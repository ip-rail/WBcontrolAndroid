package wb.control.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Act_restart extends Activity{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Control Activity starten
    	Intent intent_start = new Intent(this, WBcontrolStartup.class);
    	this.startActivity(intent_start);
    	finish();	// End Activity beenden
	}
	
}
