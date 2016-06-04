package wb.control.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import wb.control.Basis;
import wb.control.Device;
import wb.control.R;
import wb.control.UDPWaiter;
import wb.control.WBlog.wblogtype;
import wb.control.WBupdateParser;

public class End extends Activity 
 implements View.OnClickListener {
		
	// Button Button_startup_connect, Button_startup_dont_connect;
	ProgressBar progressBar_end;
	TextView textView_end_info;
	Button button_end_restart;
	
	Intent svc, udp;
	
	Boolean doUpdate;	// Update ausführen?
	String dest_filename;	// Programmfile für Update
	


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		// Theme einstellen
		String themeName = Basis.getThemeTypeName();
		int themeId = getResources().getIdentifier(themeName, "style", getPackageName());
		setTheme(themeId);
			    
		Basis.useFontScale(this);	// FontScale laut gespeicherter Konfiguration setzen
		
		setContentView(R.layout.end);

		textView_end_info = (TextView)findViewById(R.id.textView_end_info);
		
		progressBar_end = (ProgressBar)findViewById(R.id.progressBar_end);
		progressBar_end.setProgress(0);
		
		button_end_restart = (Button)findViewById(R.id.button_end_restart);
		button_end_restart.setOnClickListener(this);
		button_end_restart.setVisibility(View.INVISIBLE);
		


	}	// end onCreate

	@Override
	public void onStart() {
		super.onStart();

		Basis.setWakelockmode(Basis.SSAVER_ACTIVE, this.getWindow());	// Screensaver einschalten wg. Stromsparen im Shutdown-Zustand

		doUpdate = Basis.getDoUpdateAtEnd();

		if (doUpdate)	
		{
			//dest_filename = Basis.getUpdateFile(Basis.getUpdateAvailable());
            dest_filename  = Basis.getUpdateSoftware(Basis.getUpdateAvailable()).filename;
			Basis.AddLogLine(getString(R.string.inf_progupd_inst) + " (" + dest_filename + ")", "Basis", wblogtype.Info);
			textView_end_info.setText(getString(R.string.end_info_upd));
		}
		
		Basis.DisconnectAll();
		
		udp = new Intent(this, UDPWaiter.class);
		svc = new Intent(this, Basis.class);
		
		new ShutdownCheck().execute();


	}	// end onStart
	
	
public void onClick(View view) {	// für Restart Button click
    	
    	int id = view.getId();
		if (id == R.id.button_end_restart) {
			// Control Activity starten
			Intent intent_start = new Intent(this, WBcontrolStartup.class);
			this.startActivity(intent_start);
			finish();	// End Activity beenden
		}

    }	// end onClick
	
	private class ShutdownCheck extends AsyncTask<Void, Integer, Void>
	{

		@Override
		protected void onPreExecute()
		{
			progressBar_end.setProgress(0);
			progressBar_end.setMax(100);
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			Boolean done = false;
			Boolean udp_finished_old = false;
			Boolean basis_finished_old = false;
			Boolean devices_finished_old = false;
			
			int goal = 0;
			
			while (!done)
			{
					
				if (this.isCancelled()) { done = true; }
				
				if (!devices_finished_old && !DevicesConnected()) // sind noch Devices verbunden?
				{
					devices_finished_old = true;
					goal += 1;
					publishProgress(20*goal);
					stopService(udp);	// UDPWaiter beenden
					SystemClock.sleep(500);
				}
				
				if (!isServiceRunning("wb.control.UDPWaiter") && !udp_finished_old) 
				{ 
					goal += 1;
					publishProgress(20*goal);
					// als letztes Basis beenden!
					stopService(svc);	// Basis beenden
					
					udp_finished_old = true;
					SystemClock.sleep(500);
					}
				
				if (!isServiceRunning("wb.control.Basis") && !basis_finished_old)
				{
					goal += 1;
					publishProgress(20*goal);
					basis_finished_old = true;
				}
				
				if (goal == 3) { done = true; } 
				
				SystemClock.sleep(200);
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			//animate a progress bar 
			progressBar_end.incrementProgressBy(progress[0]);
		}
		
		protected void onPostExecute(Void result) 
		{         
			if (doUpdate & (Basis.getApiLevel()>= 8))	// Update ausführen
			{
				Intent updintent = new Intent(Intent.ACTION_VIEW);
				//updintent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Download/" + dest_filename)), getString(R.string.gen_apk_mime));
                updintent.setDataAndType(Uri.fromFile(new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + dest_filename)), getString(R.string.gen_apk_mime));
				startActivity(updintent); 
			}
			
			
			Basis.setDoUpdateAtEnd(false);	// Updatekennung wieder löschen
			//Abschluss der Aktion anzeigen
			progressBar_end.setVisibility(View.INVISIBLE);
			button_end_restart.setVisibility(View.VISIBLE);
			textView_end_info.setText(getString(R.string.end_info2));
		}

	}	// end class ShutdownTask




    private boolean isServiceRunning(String servicename) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (servicename.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean DevicesConnected()
    {
    	int count = 0;
    	
    	for (Device d : Basis.getDevicelist())
		{	
    		if (d.isConnected())  {  count++; 	}
		}
    	
    	return (count > 0);
    }

    
    
}