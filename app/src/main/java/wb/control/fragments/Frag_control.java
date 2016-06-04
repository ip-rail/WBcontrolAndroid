package wb.control.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;

import wb.control.ActionElement;
import wb.control.Basis;
import wb.control.Device;
import wb.control.Device.DeviceType;
import wb.control.HardKey;
import wb.control.Macro;
import wb.control.OnConfigChangedListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.WBlog.wblogtype;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_key;
import wb.control.dialogfragments.DialogFrag_ramp;
import wb.control.dialogfragments.DialogFrag_wait;
import wb.control.dialogfragments.DialogFrag_yes_no;
import wb.control.views.VerticalSeekBar;

public class Frag_control extends Fragment
implements View.OnClickListener, AdapterView.OnItemSelectedListener, DialogFrag_yes_no.OnyesnoDialogListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_CONTROL;

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
	ArrayList<Device> loklist;
	DevSelAdapter da;
	Device Dummydevice_noneSelected;
	Device ccDevice;        // von diesem Fragment zu steuerndes Device
	BroadcastReceiver locBcReceiver;
	IntentFilter ifilter;
	DialogFrag_wait waitDialog;	// Wartedialog bei tcp-Verbindungen usw. -> wird im Msg-Handler beendet (Msg vom zuständigen Netwaiter)
	ArrayList<ActionElement> aelist_links, aelist_rechts;	// für Actionelement-Leisten
	Boolean hardKeys_allowed;

	public static final int HKEY_SPEEDSTEPS				=	16;	// bei Speed-Änderung über Hardwaretasten: 16 Speed-Stufen verwenden


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

		// Empfang für lokales Broadcasting einrichten - für WLAN Aktivierung/Deaktivierung, ActionElement-Aktualisierung, Device Statusänderungen..
		ifilter = new IntentFilter();
		ifilter.addAction(Basis.ACTION_WLAN_CONNECTED);
		ifilter.addAction(Basis.ACTION_WLAN_DISCONNECTED);
		ifilter.addAction(Basis.ACTION_UPDATE_AE);
		ifilter.addAction(Basis.ACTION_UPDATE_AE_DATA);
        ifilter.addAction(Basis.ACTION_UPDATE_TRAINSPEED);
        ifilter.addAction(Basis.ACTION_DEVICE_DISCONNECTED);
        ifilter.addAction(Basis.ACTION_DEVICE_CONNECTED);
        ifilter.addAction(Basis.ACTION_DEVICE_CONNECING_FAILED);
        ifilter.addAction(Basis.ACTION_SPEEDSTEPS_CHANGED);
        ifilter.addAction(Basis.ACTION_DEVICELIST_CHANGED);
        ifilter.addAction(Basis.ACTION_UPDATE_STATUSTXT);
        ifilter.addAction(Basis.ACTION_DEVICE_TRY_RECONNECT);
        ifilter.addAction(Basis.ACTION_DEVICE_MOTORERROR);
        ifilter.addAction(Basis.ACTION_CMD_STOPPALL);
		ifilter.addAction(Basis.ACTION_DEVICE_NAME_CHANGED);
        ifilter.addAction(Basis.ACTION_DEVICE_NEW_NAME);
		ifilter.addAction(Basis.ACTION_DEVICE_EVENT_STOP);


		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Basis.ACTION_WLAN_CONNECTED)) 
				{
					if (Basis.getUseNetwork() == ConnectivityManager.TYPE_WIFI) //nur ausführen, wenn WLAN benutzt wird
					{
						enableAllPauseStop(true);
						//textView_status.setText("WLAN-Verbindung wurde aktiviert..");
					}
				}

				else if (intent.getAction().equals(Basis.ACTION_WLAN_DISCONNECTED)) 
				{
					if (Basis.getUseNetwork() == ConnectivityManager.TYPE_WIFI)
					{
						Controls_enable(false);
						enableAllPauseStop(false);
						//textView_status.setText("WLAN-Verbindung wurde getrennt..");
					}
				} 
				
				else if (intent.getAction().equals(Basis.ACTION_UPDATE_AE)) 
				{
					updateAeDisplay(aelist_links, linearLayout_ae_links);	// links
					updateAeDisplay(aelist_rechts, linearLayout_ae_rechts);	// rechts
				} 
				
				else if (intent.getAction().equals(Basis.ACTION_UPDATE_AE_DATA)) 
				{
					//Extra: "datatype" (int)(siehe ae.datatype)  und "device" (String) Devicename (siehe ae.scopedata)
				    
					int datatype = intent.getIntExtra("datatype", -1);
					String devname = intent.getStringExtra("device");

					for (ActionElement ae : aelist_links)
					{
						if ((ae.datatype == datatype) && (ae.scopedata.equals(devname))) { ae.update(linearLayout_ae_links); }
						if (devname.equals(ccDevice.getName()))	// scopedata="" bedeutet aktuell gesteuertes Device
						{
							if ((ae.datatype == datatype) && (ae.scopedata.equals(""))) { ae.update(linearLayout_ae_links); }
						}
					}
					for (ActionElement ae : aelist_rechts)
					{
						if ((ae.datatype == datatype) && (ae.scopedata.equals(devname))) { ae.update(linearLayout_ae_rechts); }
					}

                    // schienenspannung hier noch reinschmuggeln TODO: auf Actionelement umbauen!
                    if (ccDevice != null)
                    {
                        if ((datatype == ActionElement.AE_DATATYPE_U_ADC0) && (devname.equals(ccDevice.getName())))
                        {
                            textView_us.setText(String.format(Basis.getBcontext().getString(R.string.ctrl_us), ccDevice.getUString(datatype-1)));
                        }
                    }
				}
                else if (intent.getAction().equals(Basis.ACTION_UPDATE_TRAINSPEED)) // SpeedBar: tsProgress (echte Lokgeschwindigkeit) soll upgedatet werden (von netWaiter)
                {
                    // String devname = intent.getStringExtra("device");
                    // if (DeviceIsCCD(devname)) { updateTrainSpeed(); }
                    updateTrainSpeed();

                }
                else if (intent.getAction().equals(Basis.ACTION_DEVICE_MOTORERROR))
                {
                    String devname = intent.getStringExtra("device");
                    int error  = intent.getIntExtra("error", 0);
                    if (DeviceIsCCD(devname)) { displayMotorError(error); }
                }
                else if (intent.getAction().equals(Basis.ACTION_DEVICE_DISCONNECTED))   // bestehende Verbindung wurde getrennt
                {
                    String devname = intent.getStringExtra("device");
                    if (DeviceIsCCD(devname)) { onccDeviceDisconnected(); }
                }
                else if (intent.getAction().equals(Basis.ACTION_DEVICE_CONNECTED))  // neue Verbindung ist zustande gekommen
                {
                    String devname = intent.getStringExtra("device");
                    if (DeviceIsCCD(devname))
                    {
                        setControlsForDevice(ccDevice);
                        ccDevice.setIscd(true);
						Basis.setCCDevice(ccDevice);
                        checkCloseWaitDialog();   // falls ein Verbindungsdialog aktiv ist
                    }
                }
                else if (intent.getAction().equals(Basis.ACTION_DEVICE_CONNECING_FAILED))
                {
                    String devname = intent.getStringExtra("device");
                    if (DeviceIsCCD(devname)) { onDeviceConnectingFailed(); }
                }
                else if (intent.getAction().equals(Basis.ACTION_DEVICE_TRY_RECONNECT))  // Meldung von netWaiter, dass die Verbindung zum Device unterbrochen wurde, aber versucht wird, neu zu verbinden
                {
                    String devname = intent.getStringExtra("device");
                    if (DeviceIsCCD(devname)) { Controls_enable(false); }
                }
                else if (intent.getAction().equals(Basis.ACTION_SPEEDSTEPS_CHANGED))
                {
                    // String devname = intent.getStringExtra("device");    // TODO: später für Device, jetzt wird generell geändert
                    //if (DeviceIsCCD(devname)) { changeSpeedsteps(); }
                    changeSpeedsteps();
                }
                else if (intent.getAction().equals(Basis.ACTION_DEVICELIST_CHANGED))  { updateLoklist(); }
                else if (intent.getAction().equals(Basis.ACTION_SPEEDSTEPS_CHANGED))
                {
                    String txt = intent.getStringExtra("text");
                    textView_status.setText(txt);
                }

                else if (intent.getAction().equals(Basis.ACTION_UPDATE_STATUSTXT))
                {
                    if (ccDevice != null) { textView_status.setText(ccDevice.getStatustxt()); }
                }

                else if (intent.getAction().equals(Basis.ACTION_CMD_STOPPALL))
                {
                    stopall(); // Start/Stop Taste auf Stop setzen
                }
				else if (intent.getAction().equals(Basis.ACTION_DEVICE_NAME_CHANGED))
				{
                    String devname = intent.getStringExtra("device");
                    if (ccDevice != null)
                    {
                        if (devname.equals(ccDevice.getName())) { da.notifyDataSetChanged(); }   //TODO: testen!!!
                    }
				}
                else if (intent.getAction().equals(Basis.ACTION_DEVICE_NEW_NAME))
                {
                    String devname = intent.getStringExtra("device");

                    if (ccDevice != null)
                    {
                        if (devname.equals(ccDevice.getName())) { setupNameChangeDialog(); }
                    }
                }
                else if (intent.getAction().equals(Basis.ACTION_DEVICE_EVENT_STOP))
                {
                    String devname = intent.getStringExtra("device");
                    if (ccDevice != null)
                    {
                        if (devname.equals(ccDevice.getName())) { textView_status.setText("Stop-Event wurde ausgelöst!");	 } // TODO: testweise - wieder weg oder string in Ressources geben
                    }
                }

            }
		};
			
		aelist_links = Basis.getActionElementListByLocation(1);	// 1= ctrl-links, 2= ctrl-rechts (0= Action-Bereich)
		aelist_rechts = Basis.getActionElementListByLocation(2);
		if (aelist_links == null) { aelist_links = new ArrayList<ActionElement>(); }	//Basis.getActionElementListByLocation() liefert null, wenn keine passenden aes gefunden wurden, daher in diesem Fall eine leere Liste anlegen
		if (aelist_rechts == null) { aelist_rechts = new ArrayList<ActionElement>(); }
		
		stopTxt = getString(R.string.stop);
		startTxt = getString(R.string.start);
		
		hardKeys_allowed = false;
		
	}	// end onCreate
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
		fragview = inflater.inflate(R.layout.f_control, container, false);
		
		linearLayout_ctrl  = (LinearLayout)fragview.findViewById(R.id.linearLayout_ctrl);
		linearLayout_ae_links = (LinearLayout)fragview.findViewById(R.id.linearLayout_ae_links);
		linearLayout_ae_rechts = (LinearLayout)fragview.findViewById(R.id.linearLayout_ae_rechts);
		
		textView_status = (TextView)fragview.findViewById(R.id.textView_status);
		textView_status.setText("los geht's..");	// TODO: soll noch weg
		textView_trainspeed = (TextView)fragview.findViewById(R.id.textView_trainspeed);
		textView_trainspeed.setText(R.string.ctrl_ts);
		textView_us = (TextView)fragview.findViewById(R.id.textView_us);
        textView_us.setText(String.format(Basis.getBcontext().getString(R.string.ctrl_us), "0"));
		//textView_speed = (TextView)findViewById(R.id.textView_speed);

		button_ctrl_ramp = (Button)fragview.findViewById(R.id.button_ctrl_ramp);
		button_ctrl_ramp.setOnClickListener(this);
		Button_ctrl_disconnect = (Button)fragview.findViewById(R.id.Button_ctrl_disconnect);
		Button_ctrl_disconnect.setOnClickListener(this);
		imageButton_guestkey = (ImageButton)fragview.findViewById(R.id.imageButton_guestkey);
		imageButton_guestkey.setOnClickListener(this);

		Button_start = (Button)fragview.findViewById(R.id.Button_start);
		Button_start.setOnClickListener(this);
		Button_back = (Button)fragview.findViewById(R.id.Button_back);
		Button_back.setOnClickListener(this);
		
		toggleButton_rangier  = (ToggleButton)fragview.findViewById(R.id.toggleButton_rangier);
		toggleButton_rangier.setOnClickListener(this);
		toggleButton_pauseall  = (ToggleButton)fragview.findViewById(R.id.toggleButton_pauseall);
		toggleButton_pauseall.setOnClickListener(this);
		toggleButton_stopall  = (ToggleButton)fragview.findViewById(R.id.toggleButton_stopall);
		toggleButton_stopall.setOnClickListener(this);

		spinner_control_device = (Spinner)fragview.findViewById(R.id.spinner_control_device);
		spinner_control_device.setOnItemSelectedListener(this);
		loklist = new ArrayList<Device>();
		da=new DevSelAdapter(getActivity(), android.R.layout.simple_spinner_item, loklist); 
		//aa.setDropDownViewResource(R.layout.spinner_devsel_item); 
		spinner_control_device.setAdapter(da);
		
		
		vSeekBar = (VerticalSeekBar)fragview.findViewById(R.id.verticalSeekBar1);
		vSeekBar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(VerticalSeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(VerticalSeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {

				if ((ccDevice != null) && fromUser)	// nur machen bei Fingereingabe & wenn ein zu steuerndes Device ausgewählt ist
	            {					
	                int oldspeed = ccDevice.getSpeed();
                    ccDevice.setSpeed(progress);
	               
	                if ((progress > 0) && (oldspeed == 0)) { Button_start.setText(stopTxt); }
	                else if (progress == 0)
	                {
	                    Button_start.setText(startTxt);
	                    if (progress != oldspeed) { ccDevice.cmdSendStop(); }	 // nur stop dirket ausgeben!! (Rest wird im fastTimer gesendet. In dem Fall wird der Speed nur im device gespeichert)
	                }
	            }
			}
		});
		vSeekBar.setThumb(null);	// SeekBar-Thumb verstecken
		vSeekBar.setProgress(0);
		vSeekBar.setMax(Basis.getSpeedStufen()-1);
		Button_back.setText(R.string.vorwaerts);
		
		setupAeDisplay(aelist_links, linearLayout_ae_links);
		setupAeDisplay(aelist_rechts, linearLayout_ae_rechts);
		
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
	public void onStop() {
		super.onStop();
		
	} */


	private void fresume() {

        ccDevice  = Basis.getCCDevice();

        if (Basis.getUsermode() == Basis.USERMODE_GUEST) { imageButton_guestkey.setVisibility(View.VISIBLE); }
        else { imageButton_guestkey.setVisibility(View.GONE);}


        // Buttons pauseall und stopall auch deaktivieren, wenn Netzwerk nicht verfügbar (eigeneip = null
        Boolean enablepausestop = false;
        if (Basis.getEigeneIP() != null) { enablepausestop = true; }
        enableAllPauseStop(enablepausestop);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locBcReceiver, ifilter); // localBraodcast-Empfang aktivieren
        updateLoklist();

        if (ccDevice != null)	// aktuelle Lok auswählen und Controls nach aktuellem Status setzen
        {
            spinner_control_device.setSelection(loklist.indexOf(ccDevice));	// im spinner auswählen
        }
        else	// kein Device ausgewählt
        {
            spinner_control_device.setSelection(loklist.indexOf(Dummydevice_noneSelected));	// im spinner auswählen
        }

        //if (!Basis.getUpdateAvailable().equals("")) { askUpdate(); }
	}


	private void fpause() {

        Basis.setCCDevice(ccDevice);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locBcReceiver);	// localBraodcast-Empfang stoppen (für WLAN-Status)
	}



	@Override
	public void onClick(View v) {
		
		int id = v.getId();
		if (id == R.id.button_ctrl_ramp) {
			DialogFrag_ramp rfrag = new DialogFrag_ramp();
            Bundle args = new Bundle();
            if (ccDevice != null) { args.putString("devname", ccDevice.getName());  }
            rfrag.setArguments(args);
			rfrag.show(getFragmentManager(), "rampdialog");
		} else if (id == R.id.Button_ctrl_disconnect) {
			DialogFrag_yes_no ynfrag = new DialogFrag_yes_no();
			Bundle args = new Bundle();
			args.putString("title", getString(R.string.control_dialog_disconnect_titel));
			args.putString("msg", getString(R.string.control_dialog_disconnect_txt));
			args.putBoolean("cancel", true);
			args.putInt("yestxt", R.string.gen_yes);
			args.putInt("notxt", R.string.gen_no);
            args.putInt("typ", DialogFrag_yes_no.DIALOG_YN_DISCONNECT);
			ynfrag.setArguments(args);
			ynfrag.setOnyesnoDialogListener(this);
			ynfrag.show(getFragmentManager(), "disconnectdialog");
		} else if (id == R.id.imageButton_guestkey) {
			if (Basis.isUsermode_usepwd())
			{
				DialogFrag_key keyfrag = new DialogFrag_key();
				Bundle keyargs = new Bundle();
				keyargs.putInt("dialogtype", DialogFrag_key.DIALOG_KEY_OK);
				keyfrag.setArguments(keyargs);
				keyfrag.show(getFragmentManager(), "keydialog");	
			}
			else
			{
				Basis.setUsermode(Basis.USERMODE_STANDARD);	// Gästemodus ausschalten
				CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_USERMODE, null);	// Anzeige auf Gästemodus umorganisieren
			}
		} else if (id == R.id.Button_start) { Start(); }
		
		else if (id == R.id.Button_back) {
			if (((Button) v).getText().equals(this.getString(R.string.vorwaerts)))	{ Richtung_vor(true); }
			else	{ Richtung_vor(false); }
		} else if (id == R.id.toggleButton_pauseall) {
			PauseAll(((ToggleButton) v).isChecked());
			textView_status.setText(R.string.ctrl_pauseall_active);
		} else if (id == R.id.toggleButton_stopall) {
			StopAll(((ToggleButton) v).isChecked());
			textView_status.setText(R.string.ctrl_stopall_active);
        } else if (id == R.id.toggleButton_rangier) {
            //textView_status.setText("Rangier..");

            if (ccDevice != null) {
                if (((ToggleButton) v).isChecked()) {
                    vSeekBar.setMax(Basis.getRangierMax());        // Speed-Maximum im Rangiermodus setzen
                    ccDevice.setRangiermode(true);
                    // Wenn speed zu hoch -> auf Rangiermax setzen
                    if (ccDevice.getSpeed() > Basis.getRangierMax()) {
                        ccDevice.setSpeed(Basis.getRangierMax());
                    }
                } else {
                    vSeekBar.setMax(Basis.getSpeedStufen() - 1);    // wieder auf normalen max. Speed setzen
                    ccDevice.setRangiermode(false);
                }
            }
        }
    }
	
	// Spinner Listeners
	
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) { 
		Device cdev = (Device)parent.getItemAtPosition(position);

		if (cdev.getName().equals(getString(R.string.device_none_selected)))
		{
			Controls_enable(false);
            if (ccDevice != null)
            {
                if (ccDevice.getIscd()) { ccDevice.setIscd(false); }
                ccDevice = null;
                Basis.setCCDevice(null);
            }

			if (Basis.getEigeneIP() != null) { enableAllPauseStop(true);}
			else { enableAllPauseStop(false);}
		}
		else	// ein echtes Device wurde angewählt
		{
            ccDevice = cdev;	// schon eintragen, auch wenn Device noch nicht verbunden ist! wenn die Verbindung scheitert, wird es wieder rausgekickt! (unten beim MSG_DEVICE_CONNECING_FAILED)
			
			if (!ccDevice.isConnected())	// zuerst noch tcp-Verbindung herstellen
			{ 
				Controls_enable(false);
				waitDialog = new DialogFrag_wait();
				Bundle args = new Bundle();
				args.putString("title", getString(R.string.gen_connecting));
				waitDialog.setArguments(args);
                // waitDialog.setCancelable(false); besser cancelable, falls man was anderes machen will/muss
                waitDialog.show(getFragmentManager(), "waitdialog");
				boolean started = ccDevice.Connect();
				// started gibt nur an, ob der Startvorgang erfolgreich "gestartet" wurde
                // ccDevice wurde schon gesetzt, aber setIscd(true) noch nicht!
                // die echte Rückmeldung "erfolgreich verbunden( oder nicht)"  liefern erst die locBroadcasts Basis.ACTION_DEVICE_CONNECTED oder Basis.ACTION_DEVICE_CONNECING_FAILED
                // wenn Basis.ACTION_DEVICE_CONNECTED für das Device kommt (Namen checken), das in ccDevice eingetragen ist, dann erst setIscd(true) und Controls_enable(true) machen!

				if ((!started) || (!Basis.getNetworkIsConnected()))	// wenn die Verbindung gar nicht versucht wurde, weil die Voraussetzungen nicht passen (IP leer usw)
				{
					// Controls_enable(false);  // kann man sich sparen, da anschließend das dummy-Device ausgewählt wird, da wird das gleich wieder gesetzt!
                    ccDevice.setIscd(false);    // zur Sicherheit
					spinner_control_device.setSelection(loklist.indexOf(Dummydevice_noneSelected));	// im spinner auswählen
                    waitDialog.dismiss();
                    waitDialog = null;
				}

			}
			else	// Device ist bereits verbunden
			{
                ccDevice = cdev;    // setzt aktuell zu steuerndes Device
                Basis.setCCDevice(ccDevice);
				setControlsForDevice(ccDevice);	// aktuelle Daten eintragen, Controls enablen
			}
		}
	} 

	public void onNothingSelected(AdapterView<?> parent)
	{
        ccDevice = null;
        Basis.setCCDevice(null);
		Controls_enable(false);
        // TODO: sinnvoll? -> spinner_control_device.setSelection(loklist.indexOf(Dummydevice_noneSelected));	// im spinner auswählen
	} 
	
	
	public class DevSelAdapter extends ArrayAdapter<Device> {
	 
		ArrayList<Device> listdevs;
		
	        public DevSelAdapter(Context c, int textViewResourceId, ArrayList<Device> devs) {
	            super(c, textViewResourceId, devs);
	            listdevs = devs;
	        }
	        
	        @Override
	        public View getDropDownView(int position, View convertView, ViewGroup parent) {

	        	LayoutInflater inflater=getLayoutInflater(null);
	        	View row=inflater.inflate(R.layout.spinner_devsel_item, parent, false);
	        	
	        	TextView textView_spinner_devsel = (TextView)row.findViewById(R.id.textView_spinner_devsel);
	        	Device d = listdevs.get(position);
	        	textView_spinner_devsel.setText(d.getName());
	        	TextView textView_spinner_devsel_status = (TextView)row.findViewById(R.id.textView_spinner_devsel_status);

	        	if (d.isConnected()) { textView_spinner_devsel_status.setText(R.string.gen_connected); } //Device "verbunden" anzeigen
	        	else {  textView_spinner_devsel_status.setText(""); }	
	        		 
	        	return row; 
	        }
	}
	
	
	// Dialoge
	
	// ja/nein Dialog für Disconnect:
	
	@Override
	public void OnyesnoDialog(int typ, Boolean antwort) {

        switch (typ) {

            case DialogFrag_yes_no.DIALOG_YN_DISCONNECT:

                if (antwort) { doDisconnect(); }    // bei negativer Antwort nix tun
                break;

            case DialogFrag_yes_no.DIALOG_YN_NAMECHANGE:

                if (antwort)
                {
                    if (ccDevice != null) { ccDevice.changeName();  }
                }
                break;

        }
    }

    // Device disconnect
    private void doDisconnect() {

        if (ccDevice != null) {
            try {
                if (ccDevice.getNetwaiterThread() != null) {

                    Controls_enable(false);
                    ccDevice.Disconnect();
                    // ccDevice = null; // wird im nächsten aufruf mitgemacht
                    spinner_control_device.setSelection(loklist.indexOf(Dummydevice_noneSelected));     // "keine Lok ausgewählt" im spinner auswählen
                }
            } catch (IOException e) {
                Basis.AddLogLine(getResources().getString(R.string.ctrl_dev_disconnect_error) + e.toString(), "Control", wblogtype.Error);
            }
        }


    }


    //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// andere Methoden
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	
	private void updateLoklist()
	{
		loklist.clear();

		if (Dummydevice_noneSelected == null)
		{
			Dummydevice_noneSelected = new Device(getString(R.string.device_none_selected) , DeviceType.Lok, "");
		}
		loklist.add(Dummydevice_noneSelected);

		if (Basis.getDeviceListCount(DeviceType.Lok) > 0)	// wenn Loks vorhanden sind
		{
			for (Device ding : Basis.getDevicesByType(DeviceType.Lok))  // Loks suchen
			{
				loklist.add(ding);
			}
		}
		da.notifyDataSetChanged();	// spinnerliste aktualisieren
	}
	
	
	public void StopAll(Boolean stop)	// StopAll Taste
	{
		Basis.setStopAll(stop);

		if (stop)		// stopall setzen
		{
			Basis.setStopAll(true);
			vSeekBar.setProgress(0);

			if (Basis.getDeviceListCount(DeviceType.Lok) > 0)
			{
				for (Device d : Basis.getDevicesByType(DeviceType.Lok))	{ d.cmdSendStopall(); } // an alle TCP-verbundenen Loks senden
			}
			Basis.SendUDP("<stopall>");   // und über UDP
		}
		else	// wieder aufheben
		{
			// per UDP an die controller usw senden // TODO: stoppall wieder aufheben (was tun?)
		}
		
	}
	
	public void PauseAll(Boolean pause) 	// PauseAll Taste
	{
		Basis.setPauseAll(pause);
		if (Basis.getDeviceListCount(DeviceType.Lok) > 0)
			{
				for (Device d : Basis.getDevicesByType(DeviceType.Lok))	// an alle TCP-verbundenen Loks senden
				{				
					d.cmdSendPauseall(pause);
				}
			}
		if (pause)	{ Basis.SendUDP("<pauseall>"); } // und über UDP Pause setzen
		else { Basis.SendUDP("<pauseall:aus>"); }	// Pause aufheben

	}
	

    private void stopall()
    {
        Button_start.setText(startTxt); // wenn STOPALL Befehl empfangen wurde, stoppt das Device, jetzt mus nochd er Button aktualisiert werden
    }

	public void Start()	// Start/Stop Taste
	{
		boolean start;

        if (ccDevice != null) {
            if (ccDevice.getSpeed() == 0) { start = true; } // start
            else { start = false;  }
            Start(start);
        }
	}
	
	
	public void Start(boolean start)	// Start/Stop Taste
	{
        if (ccDevice != null) {
            if (start) {
                ccDevice.setSpeed((int) ((float) Basis.getSpeedStufen() / (float) 3));    // mit 1/3 speed starten
                //speed will be sent ONLY in Basis/fastTimer
                Button_start.setText(stopTxt);
            } else   // stop
            {
                ccDevice.cmdSendStop();
                Button_start.setText(startTxt);
            }
            vSeekBar.setProgress(ccDevice.getSpeed());    // speed am Balken setzen
        }
	}
	
	
	public void Speed_acc()
	{
        if (ccDevice != null) {
            int oldspeed = vSeekBar.getProgress();
            int max = vSeekBar.getMax();
            int diff = (int) ((float) max / (float) HKEY_SPEEDSTEPS);    //speed um ein Zehntel vom Maximalwert erhöhen
            int newspeed = oldspeed + diff;
            if (newspeed > max) { newspeed = max;   }
            ccDevice.setSpeed(newspeed);
            vSeekBar.setProgress(newspeed);
            if (oldspeed == 0) { Button_start.setText(stopTxt);  }
        }
	}
	
	public void Speed_dec()
	{
        if (ccDevice != null) {
            int oldspeed = vSeekBar.getProgress();
            int diff = (int) ((float) vSeekBar.getMax() / (float) HKEY_SPEEDSTEPS);    //speed um ein Zehntel vom Maximalwert dezimieren
            int newspeed = oldspeed - diff;
            if (newspeed < 0) { newspeed = 0;  }
            ccDevice.setSpeed(newspeed);
            vSeekBar.setProgress(newspeed);
            if (newspeed == 0) { Button_start.setText(startTxt);  }
        }
	}
	
	
	public void changeDirection()
	{
        if (ccDevice != null) { Richtung_vor(ccDevice.getRichtung());  }
	}
	
	public void Richtung_vor(Boolean vor)	// </> (Vorwärts/Rückwärts)-Taste
	{
		if (ccDevice != null) {
			if (vor)
			{
                ccDevice.setRichtung(Device.DIRECTION_BACKWARD);
				Button_back.setText(R.string.rueckwaerts);
			}
			else
			{
                ccDevice.setRichtung(Device.DIRECTION_FORWARD);
				Button_back.setText(R.string.vorwaerts);
			}
		} 
	}
	
	private void Controls_enable(Boolean enable)	// Steuer-Widgets müssen deaktiviert werden, wenn kein zu steuerndes Gerät ausgewählt ist
	{		
		vSeekBar.setEnabled(enable);
		button_ctrl_ramp.setEnabled(enable);
		Button_ctrl_disconnect.setEnabled(enable);
		Button_start.setEnabled(enable);
		Button_back.setEnabled(enable);
		toggleButton_rangier.setEnabled(enable);
		if (!enable) 
		{ 
			vSeekBar.setProgress(0);
			vSeekBar.setTsProgress(0);
		}
		
		hardKeys_allowed = enable;		// keine Hardkeys annehmen, wenn controls nicht enabled sind!! 
	}
	
	private void enableAllPauseStop(Boolean enable)
	{
		toggleButton_pauseall.setEnabled(enable);
		toggleButton_stopall.setEnabled(enable);
	}
	
	private void setControlsForDevice(Device dev)	// Controls für das angegebene Device einstellen
	{
		// Widgets nach status des aktuellen devices setzen! (alle außer Device-Spinner!)
		if (Basis.getEigeneIP() != null) { enableAllPauseStop(true);}
		else { enableAllPauseStop(false);}

		if (dev != null)	// aktuelle Lok auswählen und Controls nach aktuellem Status setzen
		{
			if (dev.isConnected())
			{
				toggleButton_pauseall.setChecked(Basis.getPauseAll());
				toggleButton_stopall.setChecked(Basis.getStopAll());
				toggleButton_rangier.setChecked(dev.getRangiermode());
				vSeekBar.setProgress(dev.getSpeed()); 
				vSeekBar.setTsProgress(dev.getTrainspeed());

				if (dev.getSpeed() > 0) 
				{
					Button_start.setText(stopTxt);

					if (dev.getRangiermode())	// wenn Rangiermode aktiviert ist
					{
						vSeekBar.setMax(Basis.getRangierMax());		// Speed-Maximum im Rangiermodus setzen
						// Wenn speed zu hoch -> auf Rangiermax setzen
						if (dev.getSpeed() > Basis.getRangierMax()) { dev.setSpeed(Basis.getRangierMax()); }
					}
					else { vSeekBar.setMax(Basis.getSpeedStufen() - 1); }	// wieder auf normalen max. Speed setzen
				}
				else { Button_start.setText(startTxt); }

				if (dev.getRichtung() == Device.DIRECTION_FORWARD)	{ Button_back.setText(R.string.vorwaerts); } 
				else { Button_back.setText(R.string.rueckwaerts); }

				Controls_enable(true);	//Steuercontrols freigeben
			}
			else { Controls_enable(false); }	// Device ist nicht verbunden

		}
	}	// end setControlsForDevice

	
	public void refreshSpeedbar()	// Speedbar neu aufbauen (für Aufrufe von andern Fragments zB Dialogen)
	{
		vSeekBar.refreshAll();
	}
	
	

	// Funktionen für LocalBroadcasts


    private Boolean DeviceIsCCD(String devicename)
    {
        Boolean state = false;
        if (ccDevice != null)
        {
            if (devicename.equals(ccDevice.getName())) { state = true; }
        }
        return state;
    }


    private void changeSpeedsteps() // Speedbar anpassen, falls sich die Speedsteps geändert haben (vorerst Defaultwert, später pro Device)
    {
        if (ccDevice != null)
        {
            int newsteps = Basis.getSpeedStufen();
            int oldsteps = vSeekBar.getMax();
            int oldspeed = ccDevice.getSpeed();
            int newspeed = (int) ((float) newsteps / (float) oldsteps * (float) oldspeed);
            ccDevice.setSpeed(newspeed);
            vSeekBar.setMax(newsteps - 1);
            vSeekBar.refreshAll();
        }
    }


    private void updateTrainSpeed()
    {
        if (ccDevice != null) {
            int tspeed = ccDevice.getTrainspeed();
            textView_trainspeed.setText(this.getString(R.string.ctrl_ts) + tspeed);
            vSeekBar.setTsProgress(tspeed);
        }
    }

    private void displayMotorError(int error)
    {
        int error_m1,error_m2;
        error_m1 = error & 3;	// bit 1,2
        error_m2 = error & 12;	// bit 3,4
        String msgtxt = String.format(Basis.getBcontext().getString(R.string.netw_msg_motorerror), error, error_m1, error_m2);
        if (error > 0) { Start(false); } // Controls auf "stop" setzen
        textView_status.setText(msgtxt);
        //TODO: Errorcode Auswertung, DEUTLICHE Anzeige!!! (auch wenn's ein anderes Device ist!!)
    }


    private void checkCloseWaitDialog()
    {
        // falls ein WaitDialog existiert -> ihn beenden
        if (waitDialog != null)
        {
            waitDialog.dismiss();
            waitDialog = null;
        }
    }


    private void onccDeviceDisconnected() {   // Reaktion auf die Meldung, dass das ccDevice getrennt wurde

        if (ccDevice != null)
        {
            Button_back.setText(R.string.vorwaerts);
            // Controls_enable(false);        //Steuercontrols sperren -< wird im setSelection auch gemacht
            spinner_control_device.setSelection(loklist.indexOf(Dummydevice_noneSelected));    // "keine lok ausgewählt" im spinner auswählen
        }

        checkCloseWaitDialog(); // falls ein WaitDialog existiert -> ihn beenden
    }

    private void onDeviceConnectingFailed()
    {
        //ccDevice = null;	// CCDevice löschen, da keine Verbindung zustandekam -> jetzt wird wieder auf "keine Lok ausgewählt" zurückgesellt
        //Controls_enable(false);	// war bisher aktiv
        // die vorherigen aktionen werden im nachfolgenden aufruf michtgemacht
        spinner_control_device.setSelection(loklist.indexOf(Dummydevice_noneSelected));	// im spinner auswählen

        checkCloseWaitDialog(); // falls ein WaitDialog existiert -> ihn beenden
    }



    private void setupNameChangeDialog()
    {
        DialogFrag_yes_no ynfrag = new DialogFrag_yes_no();
        Bundle args = new Bundle();
        args.putString("title", getString(R.string.dialog_devnamechange_titel));
        args.putString("msg", String.format(Basis.getBcontext().getString(R.string.dialog_devnamechange_txt), ccDevice.getName(),ccDevice.getNameForChange()));
        args.putBoolean("cancel", true);
        args.putInt("yestxt", R.string.gen_yes);
        args.putInt("notxt", R.string.gen_no);
        args.putInt("typ", DialogFrag_yes_no.DIALOG_YN_NAMECHANGE);
        ynfrag.setArguments(args);
        ynfrag.setOnyesnoDialogListener(this);
        ynfrag.show(getFragmentManager(), "disconnectdialog");
    }



    // ------------------- Actionelement-Funktionen ----------------------------------------------
    
	// aelist in layout anzeigen
	private void setupAeDisplay(ArrayList<ActionElement> aelist, LinearLayout layout)
	{
		for (ActionElement ae : aelist)
    	{
			layout.addView(ae.createView(layout, this));
    	}
	}
	
	// aelist-views aus layout entfernen
		private void clearAeDisplay(ArrayList<ActionElement> aelist, LinearLayout layout)
		{
			for (ActionElement ae : aelist)
	    	{
				View v = layout.findViewWithTag(ae);
				layout.removeView(v);
	    	}
		}
	
	// aelist in layout anzeigen
		private void updateAeDisplay(ArrayList<ActionElement> aelist, LinearLayout layout)
		{
			clearAeDisplay(aelist, layout);
			if (layout == linearLayout_ae_links) { Basis.updateActionElementListByLocation(aelist, 1); }
			else if (layout == linearLayout_ae_rechts) { Basis.updateActionElementListByLocation(aelist, 2); }
			setupAeDisplay(aelist, layout);
		}


    
 // Action-ContextMenü
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
 		//super.onCreateContextMenu(menu, v, menuInfo);
 		
 		if (Basis.getUsermode() == Basis.USERMODE_GUEST) { return; }	// im Guestmode nicht erlaubt
 		
 		cmtargetView = v;
 		menu.clear();	// Menu leeren!!
 		MenuInflater inflater = new MenuInflater(getActivity().getApplicationContext());
 		inflater.inflate(R.menu.menu_control_context, menu);
 		// ContextMenu derzeit anpassen
 		menu.removeItem(R.id.menui_ctrl_add);
 		menu.removeItem(R.id.menui_ctrl_exit);
 		menu.removeItem(R.id.menui_ctrl_move);
 		
 	}
 	
 	public void onContextMenuClosed (Menu menu)
 	{
 		cmtargetView = null;
 	}
 	
 	@Override
 	public boolean onContextItemSelected(MenuItem item) {
 		//AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

 		
 		int id = item.getItemId();
 		
 		//switch (item.getItemId()) {

 		/*	// neue AEs werden nur über den Action-Bereich angelegt
 		case R.id.menui_act_add:

 			*/

 		if (id == R.id.menui_ctrl_del)	//case R.id.menui_ctrl_del:
 		{
 			if (cmtargetView != null)
 			{ 
 				cmtargetView.setVisibility(View.GONE);
 				ActionElement ae = (ActionElement)cmtargetView.getTag();
 				if (ae.ort == ActionElement.AE_LOCATION_CTRL_L) 	// links
 				{
 					linearLayout_ae_links.removeView(cmtargetView);
 					aelist_links.remove(ae); 
 				}
 				else if (ae.ort == ActionElement.AE_LOCATION_CTRL_R) 	// rechts
 				{ 
 					linearLayout_ae_rechts.removeView(cmtargetView);
 					aelist_rechts.remove(ae); 
 				}
 				Basis.getActionElementList().remove(ae);	// in der Haupt-Liste auch entfernen!!!
 			}

 			return true;
 		}

 		if (id == R.id.menui_ctrl_edit)	//case R.id.menui_ctrl_edit:	// ausgewähltes Widget finden und editieren

 		{
 			if (cmtargetView != null)
 			{ 
 				if (cmtargetView.getTag() != null)
 				{
 					ActionElement aetag = (ActionElement)cmtargetView.getTag();	// aetag ist das *echte* ae
 					int aeindex = Basis.getActionElementList().indexOf(aetag);
 					((FAct_control) getActivity()).setAe_toedit(aeindex);
 					fragReplListener.OnFragReplace(FAct_control.FRAGMENT_ACTION_EDIT, true, null);	// Bundle wird erst bei Antwort vom FRAGMENT_ACTION_EDIT benötigt.

 					return true;
 				}
 				else
 				{
 					return false;
 				}
 			}
 		}
 			
 		if (id == R.id.menui_ctrl_exit)	//case R.id.menui_ctrl_exit:
 		{
 			return true;
 		}

 		if (id == R.id.menui_ctrl_move)	//case R.id.menui_ctrl_move:
 		{
 			return true;
 		}
 		else
 		{
 			return super.onContextItemSelected(item);
 		}
 			
 		
 	}	// end  onContextItemSelected
 	
 	
 	
	// über diese Funktion erhöt das Fragment die KeyEvents für die Hardware-Tasten von der Activity

 	public void getHardKeyEvent(int funktion, String macroname)
 	{

 		if (hardKeys_allowed)	// wird durch Controls_enable gesteuert -> keine Controls -> auch keine Hardkeys!!!
 		{

 			switch (funktion) {

 			case HardKey.HARDKEY_FUNCTION_START:
 				Start(true);
 				break;

 			case HardKey.HARDKEY_FUNCTION_STOP:
 				Start(false);
 				break;

 			case HardKey.HARDKEY_FUNCTION_SPEED_ACC:
 				Speed_acc();
 				break;

 			case HardKey.HARDKEY_FUNCTION_SPEED_DEC:
 				Speed_dec();
 				break;

 			case HardKey.HARDKEY_FUNCTION_ALLPAUSE:
 				PauseAll(true);
 				break;

 			case HardKey.HARDKEY_FUNCTION_ALLSTOP:
 				StopAll(true);
 				break;

 			case HardKey.HARDKEY_FUNCTION_CHDIR:
 				changeDirection();
 				break;

 			case HardKey.HARDKEY_FUNCTION_MACRO:
 				Macro m = Basis.getMacrolistObjectByName(macroname);
 				if (m != null) { m.execute(macroname); }
 				break;

 			} // end switch function
 		}

	}
 	


 	
		
}	// end Class Frag_control
