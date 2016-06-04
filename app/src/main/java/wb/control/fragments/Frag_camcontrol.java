package wb.control.fragments;

//TODO: Frag_camcontrol: noch nichts angepaßt!!!!!!!!

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import wb.control.Basis;
import wb.control.Device;
import wb.control.Device.DeviceType;
import wb.control.OnConfigChangedListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.views.MjpegView;
import wb.control.views.VerticalSeekBar;

public class Frag_camcontrol extends Fragment implements OnSeekBarChangeListener, WBFragID {

	private static final int FRAGMENT_ID = FAct_control.FRAGMENT_CAMCONTROL;

	View fragview;	// Root-View für das Fragment
	View cmtargetView; // speichert den View, von dem aus das aktuelle ContextMenu gestartet wurde
	OnFragReplaceListener fragReplListener;
	OnConfigChangedListener CfgChangedListener;
	LinearLayout linearLayout_ae_links, linearLayout_ae_rechts, linearLayout_ctrl;
	TextView textView_status, textView_trainspeed, textView_us, textView_dialog_ramp_accvalue, textView_dialog_ramp_decvalue;
	Button Button_start, Button_back, button_ctrl_ramp, Button_ctrl_disconnect;
	ImageButton imageButton_guestkey;
	ToggleButton toggleButton_pauseall, toggleButton_stopall, toggleButton_rangier;
	VerticalSeekBar vSeekBar;
	Spinner spinner_control_device;
	String stopTxt, startTxt;
	PTCameraView camview;
	SeekBar seekh, seekw;
	TextView textView_camc1;
	int camh, camv, camh_old, camv_old;	//Werte für Kamera (Servo)-Steuerung
	int servoh_corr_lo, servoh_corr_hi, servov_corr_lo, servov_corr_hi;
	int aliveTimeoutCounter;
	
	Device camdevice;	//Kameragerät (ist keine Lok!!)
	Timer camctrl_Timer;	// 100ms Timer für Bewegung der Kamera (damit nicht zu viele Steuerbefehle übertragen werden)

	BroadcastReceiver locBcReceiver;
	IntentFilter ifilter;

	//private ArrayList<AVStream> streamlist;	// Liste der verfügbaren Streams
	//private Boolean showAVstream;			// soll AV-Stream angezeigt werden?

    private static final String TAG = "MjpegStream";
    public static final int SERVO_MAX_VALUE		=	32000;	// Wert für Endausschlag (kann noch durch einen Korrekturwert angepasst werden)
    
    
    //TODO: saveinstancestate einbauen wg. fragment-wechsel, orientatinchange usw..


    public int getFragmentID() { return FRAGMENT_ID; }

    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            fragReplListener = (OnFragReplaceListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragReplaceListener");
        }
        try {
        	CfgChangedListener = (OnConfigChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnChangeDisplayModeListener");
        }
         
    }
  
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//showAVstream = false;

		// Empfang für lokales Broadcasting einrichten - für WLAN Aktivierung/Deaktivierung, ActionElement-Aktualisierung
		ifilter = new IntentFilter();
		ifilter.addAction(Basis.ACTION_WLAN_CONNECTED);
		ifilter.addAction(Basis.ACTION_WLAN_DISCONNECTED);
		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Basis.ACTION_WLAN_CONNECTED)) 
				{
					if (Basis.getUseNetwork() == ConnectivityManager.TYPE_WIFI) //nur ausführen, wenn WLAN benutzt wird
					{
						enableCamControls(true);
						if (!camdevice.isConnected()) { camdevice.Connect(); }	//auto-connect
						//textView_status.setText("WLAN-Verbindung wurde aktiviert..");
					}
				}
				else if (intent.getAction().equals(Basis.ACTION_WLAN_DISCONNECTED)) 
				{
					if (Basis.getUseNetwork() == ConnectivityManager.TYPE_WIFI)
					{
						enableCamControls(false);
						//disconnect des devices passiert automatisch, wenn nicht geschrieben werden kann
						//textView_status.setText("WLAN-Verbindung wurde getrennt..");
					}
				}
			}
		};

		// Korrekturwerte für Servoausschlag  // TODO: checken wie das verwaltet werden soll - Std-Wert sollte nur bis 16000m gehen??
		servoh_corr_lo = -2000;
		servoh_corr_hi = 21705 - SERVO_MAX_VALUE;
		servov_corr_lo = 0;
		servov_corr_hi = 22000 - SERVO_MAX_VALUE;

		camdevice = Basis.getCamDevice();

		if (camdevice == null) 
		{ 
			camdevice = new Device("Camserver", DeviceType.Camera, "10.0.0.71", false);
			Basis.setCamDevice(camdevice);
		}
		
		aliveTimeoutCounter = 0;
		
	}	// end onCreate
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
		fragview = inflater.inflate(R.layout.f_camcontrol, container, false);
		
		//camview = (PTCameraView)fragview.findViewById(R.id.pTCameraView_camc1);

		textView_camc1 = (TextView)fragview.findViewById(R.id.textView_camc1);

		seekh = (SeekBar)fragview.findViewById(R.id.seekBar_camc1);
		seekw = (SeekBar)fragview.findViewById(R.id.seekBar_camc2);

		seekh.setOnSeekBarChangeListener(this);
		seekw.setOnSeekBarChangeListener(this);
		
		try { seekh.setMax(SERVO_MAX_VALUE + servoh_corr_hi - servoh_corr_lo); } // servoh_corr_lo muss bei Weitergabe für die Steuerung wieder zum Wert addiert werden
		catch (Exception e) { e.printStackTrace();	}	//TODO: warum hier npe?
		
		try { seekw.setMax(SERVO_MAX_VALUE + servoh_corr_hi - servoh_corr_lo); } // servov_corr_lo muss bei Weitergabe für die Steuerung wieder zum Wert addiert werden
		catch (Exception e) { e.printStackTrace();	}	//TODO: warum hier npe?

		return fragview;
        
    }	// end onCreateView

	
	
	/*
	@Override
	public void onStart() {
		super.onStart();
	}
	*/

	@Override
	public void onResume() {

		super.onResume();
        fresume();
	}
	
	
	@Override
	public void onPause() {

        fpause();
		super.onPause();

	}
	
	/*
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);	// muss als ersts aufgerufen werden!!
	 
	  savedInstanceState.putInt("cfg_group", cfg_selected_group);	// die darzustellende Config-Gruppe

	} */
	
	/*
	@Override
	public void onStop() {
		super.onStop();
		
	} */


    private void fresume() {

        camh = Basis.getCamhval();
        if (camh == -32000) { camh = 0; }
        camv = Basis.getCamvval();
        if (camv == -32000) { camv = 0; }
        camh_old = camh;
        camv_old = camv;

        // checken, ob AV-Stream angezeigt werden soll
        //streamlist = Basis.getAvStreamlist();

		/*
		if (streamlist.size() < 0)	{ showAVstream = true; }
		else { showAVstream = false; }


		if (showAVstream)	// AV-Stream soll angezeigt werden
		{
			mjview.setVisibility(View.VISIBLE);
			linearLayout_ctrl.setBackgroundColor(getResources().getColor(android.R.color.transparent));
			//mjview.startAVstream(av.source); // welchen stream starten?
		} */

        // Buttons pauseall und stopall auch deaktivieren, wenn Netzwerk nicht verfügbar (eigeneip = null

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locBcReceiver, ifilter); // localBraodcast-Empfang aktivieren

        if (!camdevice.isConnected()) { camdevice.Connect(); }

        // 100ms Timer starten (ist eigener Thread)
        camctrl_Timer = new Timer("camctrl_Timer");	// neue Instanz nach jedem cancel() notwendig
        camctrl_Timer.scheduleAtFixedRate(new TimerTask() { public void run() { camctrl_Timer_Tick();  } }, 0, 50);	//TODO: test umstellen auf 50ms statt 100

    } // end fresume


    private void fpause() {

        Basis.setCamhval(camh);
        Basis.setCamvval(camv);

        if (camview != null) { camview.stopPlayback(); }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locBcReceiver);	// localBraodcast-Empfang stoppen (für WLAN-Status)

        if (camctrl_Timer != null) { camctrl_Timer.cancel(); }

        if (camdevice.isConnected())
        {
            try { camdevice.Disconnect(); } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    } //end fpause





 	// Pan & Tilt CameraView zum anzeigen und steuern der Kamera
 	
 	public class PTCameraView extends MjpegView {

 	    private Context mContext;	// Handle to the application context, used to e.g. fetch Drawables.
 	   
 	    public PTCameraView(Context context) {
 	    	super(context);
 	    	mContext = context;
 	    }
 	    
 	    
 	   public PTCameraView(Context context, AttributeSet attrs) {
	    	super(context, attrs);
	    	mContext = context;
	    }
 	    
 	    
 	   @Override
 	    public boolean onTouchEvent(MotionEvent event) {
 		   
 		   	int touchX = (int) event.getX();
			int touchY = (int) event.getY();
			if (touchX < 0) { touchX = 0;}
			if (touchY < 0) { touchY = 0;}
			int action = event.getAction();
			
			switch(action){

			case MotionEvent.ACTION_DOWN:

				Log.d("wbcontrol", "Action_Down");
				break;
				
			case MotionEvent.ACTION_MOVE:
				
				Log.d("wbcontrol", "Action_MOVE x=" + touchX + "y=" + touchY);
				break;
			case MotionEvent.ACTION_UP:
				
		        	Log.d("wbcontrol", "Action_UP x=" + touchX + "y=" + touchY);
				break;
				
			case MotionEvent.ACTION_CANCEL:

				break;
				
			case MotionEvent.ACTION_OUTSIDE:

				break;
				
			default:
			}
			

 	    	return true;	// enent wird hier behandelt		
 	    }
 	    
 	} // end PTCameraView




 	//SeekBar methods
 	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
	{
		switch(seekBar.getId()){

		case R.id.seekBar_camc1:
			camh = progress + servoh_corr_lo;
			textView_camc1.setText("h=" + camh + "  v=" + camv);
			break;
			
		case R.id.seekBar_camc2:
			camv = progress + servov_corr_lo;
			textView_camc1.setText("h=" + camh + "  v=" + camv);
			break;
			
		}
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
 	
	
	private void camctrl_Timer_Tick()
	{
		// zum Übertragen der Kameraposition (Servos), wird alle 100ms aufgerufen, damit nicht zu viele Positionsdaten übertragen werden
		// wird nur gesendet, wenn sich die Daten von den zuvor gesendeten Daten unterscheiden
		// wird ebenfalls zum check der Netzverbindung zur Kamera und Freigabe der Widgets verwendet

		aliveTimeoutCounter++;	// zum Zählen, wann eine alive-Meldung an den Camera-Server nötig ist

		if (camdevice != null)
		{
			if (camdevice.isConnected())	// wenn keine Verbindung besteht, sind auch die Servo-Werte egal
			{
				if ((camh != camh_old) || (camv != camv_old))
				{
					sendMove();
					camh_old = camh;
					camv_old = camv;
					aliveTimeoutCounter = 0;	// alive-Meldung nicht nötig
				}
				else
				{
					if (aliveTimeoutCounter > 18)		// alive-Meldung // TODO: Achtung: Wert 9 ist für 100ms Timer-Tick -> jeweils anpassen
					{ 
						camdevice.Netwrite(getString(R.string.cmd_alive));
						aliveTimeoutCounter = 0;
					} 
				}
				
				
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						
						enableCamControls(true);	// Widgets aktivieren
					}
				});
				
			}
			else
			{				
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						
						enableCamControls(false);	// Widgets deaktivieren
					}
				});
				
			}
		}
	}
	
	private void sendMove()	// TODO: kommt ins Device
	{
		if (camdevice != null) 
		{ 
			if (camdevice.isConnected())
			{
				camdevice.Netwrite(String.format(getString(R.string.cmd_camctrl_move), camh, camv));
				Log.d("WBcontrol", "Sende Move: " + camh + " " + camv);
			}
		}
	}
	
	private void enableCamControls(boolean enable)
	{
		if (enable)	// Widgets aktivieren
		{
			
			if (!seekh.isEnabled()) { seekh.setEnabled(true); }
			if (!seekw.isEnabled()) { seekw.setEnabled(true); }
		}
		else // Widgets deaktivieren
		{
			if (seekh.isEnabled()) { seekh.setEnabled(false); }
			if (seekw.isEnabled()) { seekw.setEnabled(false); }
		}
	}
 	
		
}	// end Class Frag_camcontrol
