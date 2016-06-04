package wb.control.dialogfragments;

import wb.control.R;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DialogFrag_Textinput extends DialogFragment {

	//Fragment thisFragment;
	//wbFragment targetFrag;			// Target-Fragment, an das das Ergebnis zurückgegeben werden soll
	//FragmentManager fragmentManager;
	
	private TextView textView_dialog_textinput_infotxt;
	private Button button_dialog_textinput_cancel, button_dialog_textinput_save;
	private EditText editText_dialog_textinput;

	OnTextFromDialogListener TextFromDialogListener = null;
	
	public DialogFrag_Textinput() {
        // Empty constructor required for DialogFragment
    }
	
	
	// Must be implemented by activity/Fragment that uses this dialog (to receive the selected text/position)
    public interface OnTextFromDialogListener {

        void OnTextFromDialog(String text);
        // text: der eingegebene Text

    }
	
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		//fragmentManager = getSupportFragmentManager();
		//thisFragment = this;
		setRetainInstance(true);
		final Dialog dialog = new Dialog(getActivity());
		Bundle dat = getArguments();	// Daten werden per Bundle übergeben

		// String dTitle = getString(R.string.dialog_upd_title);
		String dTitle = "";
		if (dat != null) { dTitle = dat.getString("title"); }
		dialog.setContentView(R.layout.dialog_textinput);
		dialog.setTitle(dTitle);
		dialog.setCancelable(true);
		
		textView_dialog_textinput_infotxt = (TextView) dialog.findViewById(R.id.textView_dialog_textinput_infotxt);
		
		editText_dialog_textinput = (EditText) dialog.findViewById(R.id.editText_dialog_textinput);

		button_dialog_textinput_save = (Button) dialog.findViewById(R.id.button_dialog_textinput_save);
		button_dialog_textinput_save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (TextFromDialogListener != null) 
				{
		            TextFromDialogListener.OnTextFromDialog(editText_dialog_textinput.getText().toString()); 
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



	public void setOnTextFromDialogListener(OnTextFromDialogListener listener) {
        TextFromDialogListener = listener;
    }
	
	
	
}
