package wb.control.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Basis.runmodetype;
import wb.control.Device;
import wb.control.Device.DeviceType;
import wb.control.OnYesNoDialogListener;
import wb.control.R;
import wb.control.WBlog.wblogtype;
import wb.control.dialogfragments.DialogFrag_TermsOfUse;
import wb.control.dialogfragments.DialogFrag_Update;

public class WBcontrolStartup extends FragmentActivity
implements OnItemClickListener, OnLongClickListener, OnYesNoDialogListener {
	
	TextView textView_startup, textView_ipconnect, textView_startup_infos;
	EditText editText_ConnectIP;
	ListView listView_startup_found;
	ArrayList<Device> devices_found;
	ArrayAdapter<Device> deva;
	Device Dummydevice_ohneVerbindungStarten;
	Boolean first;
	BroadcastReceiver locBcReceiver;
	IntentFilter ifilter;

    private final static String LOGTAG = "WBcontrolStartup";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		first = true;

		// Theme einstellen (vor super.onCreate !!)
		int themeId = getResources().getIdentifier("wbTheme", "style", getPackageName());
		setTheme(themeId);

		super.onCreate(savedInstanceState);

		// als erstes Basis-Service starten!
		startBasisService();	// Basis im Main(UI)-Thread laufen lassen

		setContentView(R.layout.startup);
		textView_ipconnect = (TextView)findViewById(R.id.textView_ipconnect);
		textView_ipconnect.setVisibility(View.GONE);
		editText_ConnectIP = (EditText)findViewById(R.id.editText_ConnectIP);	
		editText_ConnectIP.setVisibility(View.GONE);
		editText_ConnectIP.setOnLongClickListener(this);
		textView_startup = (TextView)findViewById(R.id.textView_startup);

		listView_startup_found = (ListView)findViewById(R.id.listView_startup_found);
		listView_startup_found.setOnItemClickListener(this);
		listView_startup_found.setVisibility(View.GONE);

		textView_startup_infos = (TextView)findViewById(R.id.textView_startup_infos);

        // Appversioncode von AndroidManifest auslesen, da Basis noch nicht bereit ist
        int version = 0;
        try
        {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOGTAG, e.getMessage());
        }

		textView_startup_infos.setText(String.format(getApplicationContext().getString(R.string.startup_infos), version));  //ausgelesene Versionsnummer in Text einsetzen

		devices_found = new ArrayList<Device>();
		//Adapter
		deva=new ArrayAdapter<Device>(this, android.R.layout.simple_list_item_1, devices_found);
		listView_startup_found.setAdapter(deva);

		// Empfang für lokales Broadcasting einrichten - für WLAN Aktivierung/Deaktivierung, ActionElement-Aktualisierung
		ifilter = new IntentFilter();
		ifilter.addAction(Basis.ACTION_DEVICELIST_CHANGED);
        ifilter.addAction(Basis.ACTION_BASIS_READY);
        ifilter.addAction(Basis.ACTION_PROGUPDATE_AVAILABLE);
        ifilter.addAction(Basis.ACTION_STARTUP_NETWORK_ERROR);

		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Basis.ACTION_DEVICELIST_CHANGED)) { updateFoundDevices(); }
                else if (intent.getAction().equals(Basis.ACTION_BASIS_READY)) { showConnectionList(""); }
                else if (intent.getAction().equals(Basis.ACTION_PROGUPDATE_AVAILABLE))	{ askUpdate(); }
                else if (intent.getAction().equals(Basis.ACTION_STARTUP_NETWORK_ERROR))
                {
                    int error = intent.getIntExtra("errorcode", -1);
                    String toasttxt = "";
                    switch (error)
                    {
                        case Basis.NETWORK_ERR_WLAN_NOT_ENABLED:
                            toasttxt = getString(R.string.startup_wlan_off);
                            break;

                        case Basis.NETWORK_ERR_NO_IP:
                            toasttxt = getString(R.string.startup_ip_notfound) + " " + getString(R.string.startup_check_nw);
                            break;

                        case Basis.NETWORK_ERR_INVALID_IP:
                            toasttxt = getString(R.string.startupip_invalid) + " " + getString(R.string.startup_check_nw) + " " + getString(R.string.startup_noipv6);
                            break;

                        case Basis.NETWORK_ERR_WLAN_NOT_CONNECTED:
                            toasttxt = getString(R.string.startup_wlan_ncon) + " " + getString(R.string.startup_check_nw);
                            break;
                    }
                    showConnectionList(toasttxt);
                }
			}
		};

		// ACHTUNG: Basis-Aufrufe funktionieren hier noch nicht, Basis ist noch nicht bereit (Start dauert länger)
		// weitere Aktionen werden erst nach Empfang des LocalBroadcasts ACTION_BASIS_READY durchgeführt

	}	// end onCreate

	
	@Override
	public void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(this).registerReceiver(locBcReceiver, ifilter); // localBraodcast-Empfang aktivieren
	}
	
	
	@Override
	public void onPause() {
		super.onPause();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(locBcReceiver);	// localBraodcast-Empfang stoppen (für WLAN-Status)
	}
	
	  @Override
	  public void onDestroy() {
		  //Basis.setUIhandler(null);	// nicht nullen, wurde inzwischen von control schon neu gesetzt!!
		  super.onDestroy();
	  }

    private void startBasisService() 
    {
    	try {
    		Intent svc = new Intent(this, Basis.class);
    		startService(svc);
    	}
    	catch (Exception e) {
    		Basis.AddLogLine("Basis-Service creation problem: " + e.toString(), "startup", wblogtype.Error);
    	}
    	    	
    }
    
    
  // ListView Listener   
    
    @Override
	public void onItemClick(AdapterView<?> view, View arg1, int position, long id) {
    	// ausgewähltes Device verbinden
    	Device selectedDevice = (Device)view.getItemAtPosition(position);	
    	if (selectedDevice == Dummydevice_ohneVerbindungStarten) { startDevice(null); }	// ohne Verbindung starten
    	else { startDevice(selectedDevice); }	// ein gültiges Device starten
	}
    
        
    // editText Listener
    @Override
    public boolean onLongClick(View v) {
    	int id = v.getId();
		if (id == R.id.editText_ConnectIP) {
			startManuellesDevice();
			return true;	// LongClick wurde hier behandelt
		} else {
			return false;	// LongClick muss woanders behandelt werden
		}
    } 


    
    private void updateFoundDevices()
    {
    	devices_found.clear();
    	
    	//Dummy-Devices vorbereiten
    	if (Dummydevice_ohneVerbindungStarten == null)
    	{
    		Dummydevice_ohneVerbindungStarten = new Device(getString(R.string.startup_dontconnect) , DeviceType.Lok, "");
    	}
    	
		devices_found.add(Dummydevice_ohneVerbindungStarten);
		if (Basis.getDeviceListCount(DeviceType.Lok) > 0)
		{
			devices_found.addAll(Basis.getDevicesByType(DeviceType.Lok));
		}
		deva.notifyDataSetChanged();
    }
    
    
	
    private void startManuellesDevice()
    {
    	String manip = editText_ConnectIP.getText().toString();
    	Basis.setManuelleIP(manip);

    	// Device connecten - funkt erst im Echtbetrieb // TODO: entfernen, muss auch im Testmode gehen!
    	//if (!(Basis.getRunmode() == runmodetype.test))	// im test nicht verbinden
    	//{
		Device mandev = new Device("unbekannt " + manip, DeviceType.Lok, manip);	// wir wird es bei win genannt?
		Basis.AddDevice(mandev);
		startDevice(mandev);
    	//}
    }

    private void startDevice(Device dev)	// Steuerung starten / Device = null für Starten ohne Verbindung
    {
    	// TCP-Verbindung zum Device wird erst im Frag_control hergestellt
    	Basis.setCCDevice(dev);
    	
    	// Control Activity starten
    	Intent intent_startcontrol = new Intent(WBcontrolStartup.this, FAct_control.class);
    	this.startActivity(intent_startcontrol);
    	finish();	// WBcontrol Startup Activity beenden
    }

    private void showConnectionList(String toastinfo)	// Aktionen, wenn Basis erfolgreich gestartet wurde 
    {
    	Basis.useFontScale(this);	// FontScale laut gespeicherter Konfiguration setzen
    	
    	if (!toastinfo.equals("")) { Basis.showWBtoast(toastinfo); }


        if (!Basis.getTOUaccepted()) { showTermsOfUse();  } // Terms of use already accepted?

    	updateFoundDevices();
    	
    	textView_startup.append("\n" + getString(R.string.startup_base_rdy));
    	Basis.AddLogLine(getString(R.string.startup_base_started), "Basis", wblogtype.Info);
    	String ipalt = Basis.getManuelleIP();
    	//if (ipalt == null) { textView_startup.append("\nmanuip immer noch null!"); }
    	if (!ipalt.equals("")) { editText_ConnectIP.setText(ipalt); }
    	//Lok-Auswahl-Widgets aufdrehen

    	//button_startup_retry.setVisibility(View.GONE);
    	textView_ipconnect.setVisibility(View.VISIBLE);
    	editText_ConnectIP.setVisibility(View.VISIBLE);
    	listView_startup_found.setVisibility(View.VISIBLE);
    	textView_startup.setText(R.string.startup_titel_devauswahl);
    	textView_startup.setTextAppearance(this, R.style.txt_headline_standard);
    	
    	if (first) // beim ersten durchlauf starten!!
    	{
    		checkScreen();				// Auflösung checken
    		first = false;
    	}
    	
    }

	private void showTermsOfUse()
	{
		DialogFrag_TermsOfUse toufrag = new DialogFrag_TermsOfUse();
		Bundle args = new Bundle();
		args.putString("title", getString(R.string.tou_title));
		//args.putString("msg", getString(R.string.tou_msg));
		args.putBoolean("cancel", false);
		args.putInt("yestxt", R.string.gen_yes);
		args.putInt("notxt", R.string.gen_no);
		toufrag.setArguments(args);
		toufrag.setOnYesNoDialogListener(this);
		toufrag.show(getSupportFragmentManager(), "toudialog");

	}

    @Override
	public void OnYesNoDialog(int typ, Boolean antwort) {
		// typ ist derzeit egal, dieser Dialog wird hier bisher nur 1x werwendet
		if (antwort)	// Antwort positiv
		{
			Basis.setTOUaccepted();
		}
		// sonst wirdd er dialog beim nächsten Start wieder angezeigt
	}


	private void checkScreen()
    {
    	//Configuration config = getResources().getConfiguration();
    	DisplayMetrics metrics = new DisplayMetrics();
    	getWindowManager().getDefaultDisplay().getMetrics(metrics);

    	int h = metrics.heightPixels;
    	int w = metrics.widthPixels;
    	int dpi = metrics.densityDpi;
    	float dens = metrics.density;
    	Basis.setDisplayDensity(dens);
    	    	
    	int dph = (int)(h / dens);
    	int dpw = (int)(w / dens);
    	long dpxy = dph * dpw;	// dpPixel-Anzahl
    	Basis.setDpPixels(dpxy);

    	// alte Methode: Entscheidung Display-Layout - derzeit fixer Grenzwert eingetragen
    	//if (dpxy >= Basis.getDpPixels_dualview_threshold()) { Basis.setDisplaymode(Basis.DISPLAYMODE_DUALVIEW); }
    	//else { Basis.setDisplaymode(Basis.DISPLAYMODE_SINGLEVIEW); 	}  

    	// emulator 10Zoll 1280x720 Tablet hat nur dpXY=930000 -> weniger als das GT 7.7!!! -> so läßt es sich nicht unterscheiden!!
    	
    	int displaymode = Basis.DISPLAYMODE_SINGLEVIEW;
    	String screenlayout_size = "";

    	//Determine screen size
    	if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) {     
    		screenlayout_size = "LARGE";
    		if (dpxy >= Basis.getDpPixels_dualview_threshold()) { displaymode = Basis.DISPLAYMODE_DUALVIEW; }
    	}
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL) {     
    		screenlayout_size = "NORMAL";
    	} 
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) {     
    		screenlayout_size = "SMALL";
    	}
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {     
    		screenlayout_size = "XLARGE";
    		displaymode = Basis.DISPLAYMODE_DUALVIEW;
    		// 10Zoll Tablets noch unterscheiden > 1M dpXY??
    	}
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_UNDEFINED) {     
    		screenlayout_size = "UNDEFINED";
    	}
    	else {
    		screenlayout_size = "unknown";
    	}

    	Basis.setDisplaymode(displaymode);

    	String logdata = "Screen:\r\nPixel H = " + h +
    			"\r\nPixel W = " + w +
    			"\r\nDPI = " + dpi +
    			"\r\nDensity = " + dens +
    			"\r\ndpX = " + dph +
    			"\r\ndpY = " + dpw +
    			"\r\ndpXY = " + dpxy +
    			"\r\nScreenlayout size = " + screenlayout_size + "\r\n";

    	Basis.AddLogLine(logdata, "Startup", wblogtype.Info);
    }
    
	private void askUpdate()
	{
		if (Basis.getApiLevel() >= 9)	// sollte eigentlich ab 9 gehen -> checken!!! // war: Build.VERSION_CODES.HONEYCOMB
		{
			DialogFrag_Update upfrag = new DialogFrag_Update();			
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(upfrag, "updatedialog");
            ft.commit();
        }
	}
	
    
}	// end class WBcontrolStartup