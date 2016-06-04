package wb.control.dialogfragments;

import wb.control.Basis;
import wb.control.Device;
import wb.control.OnConfigChangedListener;
import wb.control.R;
import wb.control.activities.FAct_control;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class DialogFrag_ramp extends DialogFragment {

	private int speedramp;		// Wert für Beschleunigungs-Speedramp zur Zwischenspeicherung
	private int speedBarvalue;
	private int[] rampvalue = { 1, 10, 100, 500, 1000, 2000, 4000, 8000, 16000 };	// die in der seekBar auswählbaren Ramp-Werte
    Device dev;

	OnConfigChangedListener CfgChangedListener;

	
	public DialogFrag_ramp() {
        // Empty constructor required for DialogFragment
    }
	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			CfgChangedListener = (OnConfigChangedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnChangeDisplayModeListener");
		}
	}


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		//setRetainInstance(true);
		final Dialog dialog = new Dialog(getActivity());
		// speedramp = Basis.getCCDevice().getSpeedramp();
        String devname = getArguments().getString("devname");
        // TODO: if (devname == null)   // was ist, wenn der name null ist???
        dev = Basis.getDevicelistObjectByName(devname);
        speedramp = dev.getSpeedramp();

		// nach OrientationChange: gesicherten daten wieder laden
		if (savedInstanceState != null) {
			speedBarvalue = savedInstanceState.getInt("speedBarvalue", 1000);
		}
		else
		{
			for (int i=0;i<rampvalue.length;i++)
			{
				int a = 0;
				if (i > 0) { a = rampvalue[i-1]; }
				int b = rampvalue[rampvalue.length-1];
				if (i < rampvalue.length-1) { b = rampvalue[i+1]; } 

				if ((rampvalue[i] == speedramp) || ((a < speedramp) && (b > speedramp))) 
				{ speedBarvalue = i; }		
			}
		}
		
		dialog.setContentView(R.layout.dialog_speedramp);
		dialog.setTitle(R.string.control_dialog_ramp_titel);
		
		final EditText editText_dialog_ramp = (EditText) dialog.findViewById(R.id.editText_dialog_ramp);
		editText_dialog_ramp.setText(Integer.toString(speedramp), TextView.BufferType.EDITABLE);
		
		SeekBar seekBar_dialog_ramp = (SeekBar) dialog.findViewById(R.id.seekBar_dialog_ramp);
		seekBar_dialog_ramp.setMax(rampvalue.length - 1);
		seekBar_dialog_ramp.setProgress(speedBarvalue);
	
		seekBar_dialog_ramp.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

				editText_dialog_ramp.setText(Integer.toString(rampvalue[progress]), TextView.BufferType.EDITABLE);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

		});
		
		dialog.setCancelable(true);

		Button button_dialog_ramp_save = (Button) dialog.findViewById(R.id.button_dialog_ramp_save);
		button_dialog_ramp_save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				int newrampvalue;

				try {
					newrampvalue = Integer.parseInt(editText_dialog_ramp.getText().toString());
				} catch(NumberFormatException nfe) {		   
			
					newrampvalue = 1000;
				} 
				
				
				if (newrampvalue < rampvalue[0]) { newrampvalue = rampvalue[0]; }
				else if (newrampvalue > rampvalue[rampvalue.length-1]) { newrampvalue = rampvalue[rampvalue.length-1]; }

                if (dev != null)
                {
                    dev.setSpeedramp(newrampvalue);
                    //Basis.getCCDevice().setSpeedramp(newrampvalue);
                    // Anzeige auf Speedbar refreshen, damit die Änderungen sofort angezeigt werden
                    if (dev.getIscd()) { CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_SPEEDBAR, null); }
                }
				dialog.dismiss();
			}
		});

		Button button_dialog_ramp_cancel = (Button) dialog.findViewById(R.id.button_dialog_ramp_cancel);

		button_dialog_ramp_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		return dialog;
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}
	
	// speedBarvalue bei Orientationchange sichern
		@Override
		public void onSaveInstanceState(Bundle savedInstanceState) {

			super.onSaveInstanceState(savedInstanceState);	// muss als ersts aufgerufen werden!!
			savedInstanceState.putInt("speedBarvalue", speedBarvalue);
		}
		


}
