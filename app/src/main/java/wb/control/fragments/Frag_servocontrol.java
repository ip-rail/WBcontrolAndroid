package wb.control.fragments;

//TODO: kopiert von Frag_camcontrol: noch nichts angepaßt, nur für Test!!!!!!!!

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
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

import java.util.Timer;
import java.util.TimerTask;

import wb.control.Basis;
import wb.control.Device;
import wb.control.OnConfigChangedListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.views.VerticalSeekBar;

public class Frag_servocontrol extends Fragment implements OnSeekBarChangeListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_CAMCONTROL;

	View fragview;	// Root-View für das Fragment
	OnFragReplaceListener fragReplListener;
	OnConfigChangedListener CfgChangedListener;
	SeekBar seekh, seekw;
	TextView textView_camc1;
    int[] servo_val = new int[2];
    int[] servo_val_old = new int[2];
    Device servodevice;
	Timer camctrl_Timer;	// 100ms Timer für Bewegung der Kamera (damit nicht zu viele Steuerbefehle übertragen werden)

    public static final int SERVO_MAX_VALUE		=	16000;	// Wert für Endausschlag (kann noch durch einen Korrekturwert angepasst werden)
    
    
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

    /*
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// Korrekturwerte für Servoausschlag  // TODO: checken wie das verwaltet werden soll - Std-Wert sollte nur bis 16000 gehen??


		
	}	// end onCreate */
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
		fragview = inflater.inflate(R.layout.f_camcontrol, container, false);

        textView_camc1 = (TextView)fragview.findViewById(R.id.textView_camc1);

		seekh = (SeekBar)fragview.findViewById(R.id.seekBar_camc1);
		seekw = (SeekBar)fragview.findViewById(R.id.seekBar_camc2);

		seekh.setOnSeekBarChangeListener(this);
		seekw.setOnSeekBarChangeListener(this);
		
		try { seekh.setMax(SERVO_MAX_VALUE); }
		catch (Exception e) { e.printStackTrace();	}	//TODO: warum hier npe?
		
		try { seekw.setMax(SERVO_MAX_VALUE); }
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

        servodevice = Basis.getCCDevice();

		enableServoControls(false);

        if (servodevice != null) { if (servodevice.isConnected()) { enableServoControls(true); } }



        // 100ms Timer starten (ist eigener Thread)
        camctrl_Timer = new Timer("camctrl_Timer");	// neue Instanz nach jedem cancel() notwendig
        camctrl_Timer.scheduleAtFixedRate(new TimerTask() { public void run() { camctrl_Timer_Tick();  } }, 0, 50);	//TODO: test umstellen auf 50ms statt 100

    } // end fresume


    private void fpause() {

        if (camctrl_Timer != null) { camctrl_Timer.cancel(); }

    } //end fpause





 	//SeekBar methods
 	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
	{
		switch(seekBar.getId()){

		case R.id.seekBar_camc1:
            servo_val[0] = progress;
			textView_camc1.setText("S1=" + servo_val[0] + "  S2=" + servo_val[1]);
			break;
			
		case R.id.seekBar_camc2:
            servo_val[1] = progress;
			textView_camc1.setText("S1=" + servo_val[0] + "  S2=" + servo_val[1]);
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
		// wird ebenfalls zur Freigabe der Widgets verwendet


		if (servodevice != null)
		{
			if (servodevice.isConnected())	// wenn keine Verbindung besteht, sind auch die Servo-Werte egal
			{
				if (servo_val[0] != servo_val_old[0])
				{
					sendMove(1);
                    servo_val_old[0] = servo_val[0];
				}
                else if (servo_val[1] != servo_val_old[1])
                {
                    sendMove(2);
                    servo_val_old[1] = servo_val[1];
                }

				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						
						enableServoControls(true);	// Widgets aktivieren
					}
				});
				
			}
			else
			{				
				getActivity().runOnUiThread(new Runnable() {
					public void run() {

                        enableServoControls(false);	// Widgets deaktivieren
					}
				});
			}
		}
		else
		{
            if (Basis.getCCDevice() != null)
            {
                if (Basis.getCCDevice().isConnected())  { servodevice = Basis.getCCDevice(); }
            }
		}
	}
	
	private void sendMove(int servo_nummer)	// TODO: kommt ins Device
	{
		if (servodevice != null)
		{ 
			if (servodevice.isConnected())
			{
                servodevice.Netwrite(String.format(getString(R.string.cmd_servoctrl_move), servo_nummer, servo_val[servo_nummer-1]));
				Log.d("WBcontrol", "Sende servomove: Servo" + " " + servo_val[servo_nummer-1]);  // TODO: logging
			}
		}
	}
	
	private void enableServoControls(boolean enable)
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
