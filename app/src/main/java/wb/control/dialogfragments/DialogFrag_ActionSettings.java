package wb.control.dialogfragments;

import wb.control.Basis;
import wb.control.OnActionGridChangeListener;
import wb.control.R;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class DialogFrag_ActionSettings extends DialogFragment {

	private static final int COLROW_MAX_VALUE = 	50;	// Maximalwert für Spalten und Reihen
	
	//Fragment thisFragment;
	//wbFragment targetFrag;			// Target-Fragment, an das das Ergebnis zurückgegeben werden soll
	//FragmentManager fragmentManager;
	
	OnActionGridChangeListener ActionGridChangeListener;
	
	Button button_dialog_action2_cs_colplus, button_dialog_action2_cs_colminus, button_dialog_action2_cs_rowplus,
			button_dialog_action2_cs_rowminus, button_dialog_dialog_action2_cs_save, button_dialog_dialog_action2_cs_cancel;
	TextView textview_dialog_action2_cs_row, textview_dialog_action2_cs_col;
	
	private int col, row;	// Anzahl der Spalten/Reihen

	
	public DialogFrag_ActionSettings() {
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

		String dTitle = getString(R.string.dialog_action2_title);
		dialog.setContentView(R.layout.dialog_action2_change_settings);
		dialog.setTitle(dTitle);
		dialog.setCancelable(true);
		
		col = Basis.getAction2_cols();
		row = Basis.getAction2_rows();
		
		
		textview_dialog_action2_cs_row = (TextView) dialog.findViewById(R.id.textview_dialog_action2_cs_row);
		textview_dialog_action2_cs_col = (TextView) dialog.findViewById(R.id.textview_dialog_action2_cs_col);
		
		updateCols();
		updateRows();
		
		button_dialog_dialog_action2_cs_cancel = (Button) dialog.findViewById(R.id.button_dialog_ramp_cancel);
		button_dialog_dialog_action2_cs_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		button_dialog_dialog_action2_cs_save = (Button) dialog.findViewById(R.id.button_dialog_ramp_save);
		button_dialog_dialog_action2_cs_save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Einstellungen übernehmen -> ActionChangeListener
				
				Basis.setAction2_cols(col);
				Basis.setAction2_rows(row);
				if (ActionGridChangeListener != null) {  ActionGridChangeListener.OnActionGridChange();  } // Grid soll geändert/neu aufgebaut werden
				dialog.dismiss();
			}
		});
		
		button_dialog_action2_cs_colplus = (Button) dialog.findViewById(R.id.button_dialog_action2_cs_colplus);
		button_dialog_action2_cs_colplus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (col < COLROW_MAX_VALUE) { col++; }
				updateCols();
			}
		});
		
		button_dialog_action2_cs_colminus = (Button) dialog.findViewById(R.id.button_dialog_action2_cs_colminus);
		button_dialog_action2_cs_colminus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (col > 1) { col--; }
				updateCols();
			}
		});
		
		button_dialog_action2_cs_rowplus = (Button) dialog.findViewById(R.id.button_dialog_action2_cs_rowplus);
		button_dialog_action2_cs_rowplus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (row < COLROW_MAX_VALUE) { row++; }
				updateRows();
			}
		});
		
		button_dialog_action2_cs_rowminus = (Button) dialog.findViewById(R.id.button_dialog_action2_cs_rowminus);
		button_dialog_action2_cs_rowminus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (row > 1) { row--; }
				
				updateRows();
			}
		});
		
		
		
		return dialog;
	}
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			ActionGridChangeListener = (OnActionGridChangeListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnActionGridChangeListener");
		}
	}
	
	

	@Override	// wg. Problemen bei ScreenRotation
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}
	
	
	private void updateCols()
	{
		textview_dialog_action2_cs_col.setText(col + " " + this.getString(R.string.dialog_action2_col));
	}
	
	private void updateRows()
	{
		textview_dialog_action2_cs_row.setText(row + " " + this.getString(R.string.dialog_action2_row));
	}
	


/*	
	public void setOnTextFromDialogListListener(OnTextFromDialogListListener listener) {
        TextFromDialogListListener = listener;
    }
	*/
	
	
}
