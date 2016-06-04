package wb.control.dialogfragments;

import wb.control.Basis;
import wb.control.OnDeviceChangeListener;
import wb.control.R;
import wb.control.Device.DeviceType;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DialogFrag_DeviceAdd extends DialogFragment{

	Button button_dialog_tad_save, button_dialog_tad_cancel;
	EditText editText_dialog_tad_name, editText_dialog_tad_ip;
	OnDeviceChangeListener DeviceChangeListener;

	public DialogFrag_DeviceAdd() {
		// Empty constructor required for DialogFragment
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			DeviceChangeListener = (OnDeviceChangeListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnDeviceChangeListener");
		}


	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final Dialog dialog = new Dialog(getActivity());
		final int dialog_typ = getArguments().getInt("type");
		//setRetainInstance(true);	// TODO: setRetainInstance: auswirkung checken / sichern

		dialog.setContentView(R.layout.dialog_test_add_dev);
		dialog.setTitle(R.string.test_dialog_adddev_titel);    	
		editText_dialog_tad_name = (EditText) dialog.findViewById(R.id.editText_dialog_tad_name);
		editText_dialog_tad_ip = (EditText) dialog.findViewById(R.id.editText_dialog_tad_ip);
		editText_dialog_tad_name.setText("");
		editText_dialog_tad_ip.setText("");
		dialog.setCancelable(true);

		Button button_dialog_tad_save = (Button) dialog.findViewById(R.id.button_dialog_tad_save);
		button_dialog_tad_save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				String name = editText_dialog_tad_name.getText().toString();
				String ip = editText_dialog_tad_ip.getText().toString();

				final String IPADDRESS_PATTERN = 
						"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
								"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
								"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
								"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

				if ((name != null) && (!name.equals(""))) 
				{
					if (!ip.matches(IPADDRESS_PATTERN)) { ip = ""; }
					Basis.AddDevice(name, DeviceType.Lok, ip, true);	// Device anlegen
					DeviceChangeListener.OnDeviceChange(name, dialog_typ);	// Frag Loklist aktualisieren
				}

				dialog.dismiss();
			}
		});

		Button button_dialog_tad_cancel = (Button) dialog.findViewById(R.id.button_dialog_tad_cancel);

		button_dialog_tad_cancel.setOnClickListener(new OnClickListener() {
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
