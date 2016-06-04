package wb.control.dialogfragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import wb.control.R;

public class DialogFrag_yes_no extends DialogFragment {
	
	//Dialog-IDs, falls in einem Fragment mehrere YN-Dialoge unterschieden werden müssen
	public static final int DIALOG_YN_GENERAL 	= 	0;	// Typ ist egal
    public static final int DIALOG_YN_ALIVECHECK 	= 	1;	// lokdetails alivecheck konfig
	public static final int DIALOG_YN_DISCONNECT 	= 	2;	// control disconnect
    public static final int DIALOG_YN_NAMECHANGE 	= 	3;	// control name change

	OnyesnoDialogListener yesnoDialogListener = null;
	int dialogtyp;	// Dialog-ID merken für Antwort per OnyesnoDialog
	
	
	public DialogFrag_yes_no() {
        // Empty constructor required for DialogFragment
    }
	
	
	// Must be implemented by activity/Fragment that uses this dialog (to receive the selected text)
    public interface OnyesnoDialogListener {

        void OnyesnoDialog(int typ, Boolean antwort);	// antwort: pos: 1, neg: 0
    }


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		setRetainInstance(true);
		Bundle dat = getArguments();	// Daten werden per Bundle übergeben
		String dTitle = dat.getString("title");
		if (dTitle == null) { dTitle = "Titel";}
		String dMsg = dat.getString("msg");
		if (dMsg == null) { dMsg = "MessageText"; }
		int dYes = dat.getInt("yestxt", R.string.gen_yes);	//TODO: da stimmt was nicht!
		int dNo = dat.getInt("notxt", R.string.gen_no);
		Boolean dCancel = dat.getBoolean("cancel", true);
		dialogtyp = dat.getInt("typ", DIALOG_YN_GENERAL);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(dTitle);
		builder.setMessage(dMsg);
		builder.setCancelable(dCancel);
		
		builder.setNegativeButton(dNo, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				
				if (yesnoDialogListener != null) { yesnoDialogListener.OnyesnoDialog(dialogtyp, false); }
				dialog.dismiss();	// nix tun
			}
		});
		
		builder.setPositiveButton(dYes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				
				if (yesnoDialogListener != null) { yesnoDialogListener.OnyesnoDialog(dialogtyp, true); }
				dialog.dismiss();
			}
		});

		return builder.create();

	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}

	public void setOnyesnoDialogListener(OnyesnoDialogListener listener) {
        yesnoDialogListener = listener;
    }

}
