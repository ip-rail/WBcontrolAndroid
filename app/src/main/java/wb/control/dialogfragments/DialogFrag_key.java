package wb.control.dialogfragments;

// Dialog zum Aufheben des Gastmodus

import wb.control.Basis;
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
import android.widget.TextView;

public class DialogFrag_key extends DialogFragment {

	
	//Dialog-Typen
    public static final int DIALOG_KEY_Save 	= 	0;	// Standard mit "Speichern"-Button
    public static final int DIALOG_KEY_OK 		= 	1;	// Standard mit "OK"-Button


    //Fragment thisFragment;
    //wbFragment targetFrag;			// Target-Fragment, an das das Ergebnis zurückgegeben werden soll
	//FragmentManager fragmentManager;
	
	private TextView textView_dialog_textinput_infotxt;
	private Button button_dialog_textinput_cancel, button_dialog_textinput_save;
	private EditText editText_dialog_textinput;
	OnConfigChangedListener CfgChangedListener;
	private int dType = DIALOG_KEY_Save;
	
	
	public DialogFrag_key() {
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

		//fragmentManager = getSupportFragmentManager();
		//thisFragment = this;
		setRetainInstance(true);
		final Dialog dialog = new Dialog(getActivity());
		dialog.setContentView(R.layout.dialog_textinput);
		dialog.setTitle(getString(R.string.dialog_key_title));
		dialog.setCancelable(true);
		
		Bundle dat = getArguments();	// Daten werden per Bundle übergeben
		dType = dat.getInt("dialogtype");
		
		textView_dialog_textinput_infotxt = (TextView) dialog.findViewById(R.id.textView_dialog_textinput_infotxt);
		textView_dialog_textinput_infotxt.setText(R.string.dialog_key_infotxt);
		editText_dialog_textinput = (EditText) dialog.findViewById(R.id.editText_dialog_textinput);

		button_dialog_textinput_save = (Button) dialog.findViewById(R.id.button_dialog_textinput_save);
		
		if (dType == DIALOG_KEY_Save) { button_dialog_textinput_save.setText(R.string.save); }
		else if (dType == DIALOG_KEY_OK) { button_dialog_textinput_save.setText(R.string.gen_ok); }
		
		button_dialog_textinput_save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (Basis.isUsermode_usepwd())
				{
					if (Basis.getUsermodepwd().equals(editText_dialog_textinput.getText().toString()))
					{
						Basis.setUsermode(Basis.USERMODE_STANDARD);	// Gästemodus ausschalten
						CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_USERMODE, null);	// Anzeige auf Gästemodus umorganisieren
					}
				}	        
				dialog.dismiss();
			}
		});
		
		button_dialog_textinput_cancel = (Button) dialog.findViewById(R.id.button_dialog_textinput_cancel);
		button_dialog_textinput_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
					dialog.dismiss();
			}

		});
				
		return dialog;
	}
	
	@Override	// wg. Problemen bei ScreenRotation
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}
	
}
