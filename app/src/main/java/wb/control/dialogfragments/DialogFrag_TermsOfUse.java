package wb.control.dialogfragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import wb.control.OnYesNoDialogListener;
import wb.control.R;

public class DialogFrag_TermsOfUse extends DialogFragment {

	//Fragment thisFragment;
	//wbFragment targetFrag;			// Target-Fragment, an das das Ergebnis zurückgegeben werden soll
	//FragmentManager fragmentManager;

	private TextView textView_tou_lic, textView_tou_lic_title, 	textView_tou_msg;
	private Button button_tou_no, button_tou_yes;

    OnYesNoDialogListener yesnoDialogListener = null;
    int dialogtyp;	// Dialog-ID merken für Antwort per OnyesnoDialog


	public DialogFrag_TermsOfUse() {
        // Empty constructor required for DialogFragment
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
		dialog.setContentView(R.layout.dialog_terms_of_use);
		dialog.setTitle(dTitle);
		dialog.setCancelable(false);
        dialogtyp = dat.getInt("typ", 0);

        /* TODO: alle eigenschaften zur Übergabe einbauen, damit der Dialog flexibel verwendet werden kann!!,
        "yestxt"
        "notxt"
        "cancel"
        "msg"
         und hidden-überschrift, hidden-txt
         Texte derzeit teilweise in Layout vorbelegt!!
        */

          // Button button_tou_no, button_tou_yes;


        //textView_tou_msg = (TextView) dialog.findViewById(R.id.textView_tou_msg);
        textView_tou_lic_title = (TextView) dialog.findViewById(R.id.textView_tou_lic_title);

        textView_tou_lic_title.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Text auf- oder zuklappen
                if (textView_tou_lic.getVisibility() == View.GONE)
                {
                    textView_tou_lic.setVisibility(View.VISIBLE);
                    textView_tou_lic_title.setText(R.string.tou_lic_title_visible);
                }
                else
                {
                    textView_tou_lic.setVisibility(View.GONE);
                    textView_tou_lic_title.setText(R.string.tou_lic_title_hidden);
                }
            }
        });

        textView_tou_lic = (TextView) dialog.findViewById(R.id.textView_tou_lic);


        button_tou_yes = (Button) dialog.findViewById(R.id.button_tou_yes);
        button_tou_yes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

                if (yesnoDialogListener != null) { yesnoDialogListener.OnYesNoDialog(dialogtyp, true); }
				dialog.dismiss();
			}
		});

        button_tou_no = (Button) dialog.findViewById(R.id.button_tou_no);
        button_tou_no.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

                if (yesnoDialogListener != null) { yesnoDialogListener.OnYesNoDialog(dialogtyp, false); }
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



    public void setOnYesNoDialogListener(OnYesNoDialogListener listener) {
        yesnoDialogListener = listener;
    }
	
	
	
}
