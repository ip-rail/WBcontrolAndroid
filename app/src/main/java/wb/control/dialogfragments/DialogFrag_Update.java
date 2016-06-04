package wb.control.dialogfragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;

import wb.control.Basis;
import wb.control.R;
import wb.control.WBlog.wblogtype;
import wb.control.WBupdateParser;

public class DialogFrag_Update extends DialogFragment {

	
	Button button_dialog_update_later, 	button_dialog_update_download, button_dialog_update_dlandinst;
	//TextView textView_dialog_update_msg;

	
	public DialogFrag_Update() {
        // Empty constructor required for DialogFragment
    }
	
/*
	OnTextFromDialogListListener TextFromDialogListListener = null;
	
	
	// Must be implemented by activity/Fragment that uses this dialog (to receive the selected text/position)
    public interface OnTextFromDialogListListener {

        public void OnTextFromDialogList(String text, int nummer, int typ);
        // text: der augewählte Text
        // nummer: die Position des ausgewählten Textes in der Liste
        // typ: der verwendete Dialog-Typ: siehe Konstanten: Dialog-Typen
    }
	*/
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		//fragmentManager = getSupportFragmentManager();
		//thisFragment = this;
		setRetainInstance(true);
		final Dialog dialog = new Dialog(getActivity());
		//Bundle dat = getArguments();	// Daten werden per Bundle übergeben

        String dTitle = String.format(getString(R.string.dialog_upd_title), Basis.getUpdateAvailable());    // Versionsnummer angeben
		dialog.setContentView(R.layout.dialog_update2);
		dialog.setTitle(dTitle);
		dialog.setCancelable(true);


		button_dialog_update_later = (Button) dialog.findViewById(R.id.button_dialog_update_later);
		button_dialog_update_later.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		button_dialog_update_download = (Button) dialog.findViewById(R.id.button_dialog_update_download);
		button_dialog_update_download.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Basis.setUpdateMode(1);	// nur Download
				startDownload();
				dialog.dismiss();
			}
		});
		
		button_dialog_update_dlandinst = (Button) dialog.findViewById(R.id.button_dialog_update_dlandinst);
		button_dialog_update_dlandinst.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Basis.setUpdateMode(2);	// Download + Install
				startDownload();
				dialog.dismiss();
			}
		});
		
		return dialog;
	}
	
	@SuppressLint("NewApi")
	private void startDownload()
	{
        WBupdateParser.WBSoftware update_sw;
        update_sw = Basis.getUpdateSoftware(Basis.getUpdateAvailable());
		String dest_filename;
		
		if (update_sw != null)
		{
            dest_filename = update_sw.filename;

			if (!dest_filename.equals(""))
			{
				// file vor dem Download löschen, wenn bereits lokal vorhanden
				//String filepath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + dest_filename;
                String filepath = Basis.getBcontext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + update_sw.filename;	//TODO: file gegen url austauschen
				File f = new File(filepath);
				if (f.exists()) { f.delete(); }
				
				//DownloadManager.Request request = new DownloadManager.Request(Uri.parse(getString(R.string.bas_upd_prog_url) + dest_filename));
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(update_sw.url));
				request.setDescription("a new program version");
				request.setTitle("WBcontrol update");
				request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);	// TODO: Download nach NW-Typ: das kann später je nach Einstellung gewählt werden
				request.setMimeType(getString(R.string.gen_apk_mime));
				// in order for this if to run, you must use the android 3.2 to compile your app
				//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {	}

				Basis.AddLogLine("Update filename: " + dest_filename, "Basis", wblogtype.Info);

				//request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, dest_filename);    // geht nicht mehr in Android 6 - wg. Berechtigungen
                request.setDestinationInExternalFilesDir (Basis.getBcontext(), Environment.DIRECTORY_DOWNLOADS, dest_filename);

				DownloadManager manager = (DownloadManager) getActivity().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
				long downloadID = manager.enqueue(request);
                update_sw.setDownloadID(downloadID);
			}
		}
	}
	
	

	@Override	// wg. Problemen bei ScreenRotation
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}


/*	
	public void setOnTextFromDialogListListener(OnTextFromDialogListListener listener) {
        TextFromDialogListListener = listener;
    }
	*/
	
	
}
