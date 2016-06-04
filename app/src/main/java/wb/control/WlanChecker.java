package wb.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;

public class WlanChecker extends BroadcastReceiver {

	
	@Override
	public void onReceive(Context c, Intent intent) {

		LocalBroadcastManager LocBMan = Basis.getLocBcManager();
		NetworkInfo nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

		if(NetworkInfo.State.CONNECTED.equals(nwInfo.getState()))
		{			
			// verbunden - do something
			Basis.checkNetwork();	// die verfügbaren Netzwerke werden geckeckt
			
			int wlanerror = Basis.startNetwork();
			if (wlanerror == 0)
			{
				//Toast.makeText(c, "WLAN ist jetzt verbunden!", Toast.LENGTH_LONG).show();
				Basis.showWBtoast(c.getString(R.string.wlancheck_con));
				LocBMan.sendBroadcast(new Intent(Basis.ACTION_WLAN_CONNECTED));
                int test = Basis.getStartupUpdateChecked();  // TODO: test weg
				if (Basis.getStartupUpdateChecked() == 0) { Basis.startCheckOnlineUpdateVersion(false); }		// online verfügbare Updates checken
			}
		}
		
		if(NetworkInfo.State.DISCONNECTED.equals(nwInfo.getState()))
		{
			if (Basis.getUseNetwork() == ConnectivityManager.TYPE_WIFI) { Basis.setNetworkIsConnected(false); }
			//Toast.makeText(c, "WLAN wurde getrennt!", Toast.LENGTH_LONG).show();
			Basis.showWBtoast(c.getString(R.string.wlancheck_discon));
			Basis.stopNetwork();
			LocBMan.sendBroadcast(new Intent(Basis.ACTION_WLAN_DISCONNECTED));
		}

	}

}