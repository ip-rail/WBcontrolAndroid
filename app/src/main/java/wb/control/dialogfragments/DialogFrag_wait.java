package wb.control.dialogfragments;

import wb.control.R;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

//Dialog, der nur einen Progress-Kreis anzeigt (für Warten auf Netzwerk-Verbindungen usw) - muss extern beendet werden!

public class DialogFrag_wait extends DialogFragment {

	public DialogFrag_wait() {
        // Empty constructor required for DialogFragment
    }
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		setRetainInstance(true);
		final Dialog dialog = new Dialog(getActivity());
		Bundle data = getArguments();
		data.getString("title");

		dialog.setContentView(R.layout.dialog_wait);
		dialog.setTitle(data.getString("title"));
		dialog.setCancelable(false);

		return dialog;
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}

}
