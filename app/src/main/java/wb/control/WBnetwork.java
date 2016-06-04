package wb.control;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;

public class WBnetwork {
	
	// ein am Adroid-Gerät verfügbares Netzwerk, über das die WBcontrol-Verbindung laufen kann
	// also primär WLAN (=WIFI), es soll aber auch die Mobiltelefon-Verbindung genutzt werden können (und ev. andere)
	
	private int typ;	//one of TYPE_MOBILE, TYPE_WIFI, TYPE_WIMAX, TYPE_ETHERNET, TYPE_BLUETOOTH, or other types defined by ConnectivityManager 
	private String typname;

	// Konstruktor
	public WBnetwork(int ntyp, String ntypname)
	{
		typ = ntyp;
		typname = ntypname;
	}
	
	public int getTyp() {
		return typ;
	}
	
	public String getTypName() {
		return typname;
	}
	
	public State getState() {	// DISCONNECTED / UNKNOWN / CONNECTED
		State state = null;
		ConnectivityManager cm = (ConnectivityManager) Basis.getBcontext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getNetworkInfo(typ);
		if (ni != null) { state = ni.getState(); }
		
		return state;
	}
	
	/* TODO
	public String getIP() {
		String ip = "";
		ConnectivityManager cm = (ConnectivityManager) Basis.getBcontext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getNetworkInfo(typ);
		if (ni != null) 
		{ ip = ni.; 
		}
		
		return state;
	} */
}
