package wb.control.dialogfragments;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Device;
import wb.control.R;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;


public class DialogFrag_ChooseFromList extends DialogFragment implements OnItemSelectedListener {

	Fragment thisFragment;
	//wbFragment targetFrag;			// Target-Fragment, an das das Ergebnis zurückgegeben werden soll
	FragmentManager fragmentManager;
	
	Button button_dialog_cfl_cancel;
	ListView listView_dialog_cfl;
	TextView textView_dialog_cfl;
	Spinner spinner_dialog_cfl;
	
	ArrayList<String> items;			// die zur Auswahl stehende Liste
	ArrayAdapter<String> aa;			// für Liste: ACHTUNG: ArrayAdapter kann nur mit eigenen Funktionen
										// (add,clear..)  verändert werden, sonst greift notifyDataSetChanged() nicht. Alternative: ArrayAdapter bei jeder Änderung neu erzeugen
	
	String[] devtypes;					// Devicetypen-Auswahl
	ArrayAdapter<String> ta;			// für Devicetypen-Spinner
	int dType = 0;						// Diaog-Type: siehe DIALOG_CFL_* Konstanten
	int selectedIndex = -1;				// welches element der Liste ausgewählt sein soll (1-: keines)
	Boolean use_spinner = false;		// Kennung, ob der Spinner verwendet werden soll - muss je Dialog-Typ gesetz werden
	
	OnTextFromDialogListListener TextFromDialogListListener = null;
	
	//Dialog-Typen
	public static final int DIALOG_CFL_DEVICENAMES 		= 	1;	// Devicenamen (nach Typen) auswählbar
	public static final int DIALOG_CFL_TRACKPLANS 		= 	2;	// in der DB vorhandene TrackPlans nach Namen auswählbar
	public static final int DIALOG_CFL_TRACKPLAN_ACT 	= 	3;	// TrackPlan Aktionen
	public static final int DIALOG_CFL_TRACKPLAN_DEL 	= 	4;	// in der DB vorhandene TrackPlans nach Namen zum Löschen auswählbar
	public static final int DIALOG_CFL_AV_STREAMS		= 	5;	// Auswahl des anzuzeigenden Video-Streams
	public static final int DIALOG_CFL_PWMF				= 	6;	// Auswahl der PWM Frequenz des Motors
	
	public DialogFrag_ChooseFromList() {
        // Empty constructor required for DialogFragment
    }

	
	
	// Must be implemented by activity/Fragment that uses this dialog (to receive the selected text/position)
    public interface OnTextFromDialogListListener {

        void OnTextFromDialogList(String text, int nummer, int typ);
        // text: der augewählte Text
        // nummer: die Position des ausgewählten Textes in der Liste
        // typ: der verwendete Dialog-Typ: siehe Konstanten: Dialog-Typen
    }
	
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		fragmentManager = getFragmentManager();
		//fragmentManager = getSupportFragmentManager();
		thisFragment = this;
		setRetainInstance(true);
		//targetFrag = (wbFragment) thisFragment.getTargetFragment();	// wird wg. interface nicht mehr benötigt
		final Dialog dialog = new Dialog(getActivity());
		Bundle dat = getArguments();	// Daten werden per Bundle übergeben
		// String "title"	Dialog Titel
		// Int "dialogtype"	Unterscheidung, welche Daten zur Auswahl stehen usw. (für flexible Verwendung des Dialogs)
		dType = dat.getInt("dialogtype");
		selectedIndex = dat.getInt("selectedindex");
		String dTitle = dat.getString("name");
		if (dTitle == null) { dTitle= getString(R.string.dialog_cfl_title); }
		
		items = dat.getStringArrayList("list");

		dialog.setContentView(R.layout.dialog_choosefromlist);
		dialog.setTitle(dTitle);
		
		switch(dType)	// zur Auswahl stehende Daten nach dialog-Typ setzen  
		{
		case DIALOG_CFL_DEVICENAMES:	// Device-Namen
			
			devtypes = getResources().getStringArray(R.array.dialog_cfl_devicetypes);
			items = Basis.getListofNames(Basis.getDevicelist());
			items.add(0, this.getString(R.string.dialog_cfl_ccd));	// CCD-Dummy als ersten Eintrag
			use_spinner = true;	// Spinner verwenden		
			break;
		}
		
		listView_dialog_cfl = (ListView) dialog.findViewById(R.id.listView_dialog_cfl);
		textView_dialog_cfl = (TextView) dialog.findViewById(R.id.textView_dialog_cfl);
		if (!use_spinner) { textView_dialog_cfl.setVisibility(View.GONE); }	// Beschriftung für Spinner verstecken

		aa = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, items);
		listView_dialog_cfl.setAdapter(aa);
		listView_dialog_cfl.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView_dialog_cfl.setSelection(selectedIndex);	//TODO: checken, ob das funktioniert
		
		dialog.setCancelable(true);
		listView_dialog_cfl.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				//((FAct_control) getActivity()).set_ae_addevice(items.get(position));	// gewählten Devicenamen in Activity speichern
				//((Frag_action_edit) f).AddScopeDevicename(items.get(position));
				
				//targetFrag.OnTextFromDialogList(items.get(position));
				
				if (TextFromDialogListListener != null) 
				{
		            TextFromDialogListListener.OnTextFromDialogList(items.get(position), position, dType); 
		        }

				dialog.dismiss();
			}
		});

		button_dialog_cfl_cancel = (Button) dialog.findViewById(R.id.button_dialog_cfl_cancel);
		button_dialog_cfl_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//((FAct_control) getActivity()).set_ae_addevice(null);	// null -> kein gültiges Ergebnis

				switch(dType)	// je nach dialog-Typ Rückmeldung für Button "Abbrechen" setzen
				{
				case DIALOG_CFL_AV_STREAMS:	// Device-Namen

					if (TextFromDialogListListener != null) { TextFromDialogListListener.OnTextFromDialogList(null, -1, dType); }	 // -1: kein Stream asgewählt
					break;
				}


				dialog.dismiss();
			}
		});
		
		spinner_dialog_cfl = (Spinner) dialog.findViewById(R.id.spinner_dialog_cfl);
		if (use_spinner)	// Spinner wenn benötigt konfigurieren, sonst verstecken
		{
			ta=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, devtypes); 
			//ta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
			spinner_dialog_cfl.setAdapter(ta);
			spinner_dialog_cfl.setOnItemSelectedListener(this);
		}
		else { spinner_dialog_cfl.setVisibility(View.GONE); }

		return dialog;
	}

	@Override	// wg. Problemen bei ScreenRotation
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}

	// für spinner
	@SuppressLint("NewApi")
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long itemid) {
		
		int id = parent.getId();
		if (id == R.id.spinner_dialog_cfl) {
			ArrayList<String> tempitems = null;
			aa.clear();
			switch (position) {
			
			case 0:	// alle
				tempitems = Basis.getListofNames(Basis.getDevicelist());
				aa.add(getString(R.string.dialog_cfl_ccd));	// als erstes Device: Dummyeintrag für aktuell gesteuerte Lok
				break;
				
			case 1:	// Loks
				tempitems = Basis.getListofNames(Basis.getDevicesByType(Device.DeviceType.Lok));
				aa.add(getString(R.string.dialog_cfl_ccd));	// als erstes Device
				break;
				
			case 2:	// Controller
				tempitems = Basis.getListofNames(Basis.getDevicesByType(Device.DeviceType.Controller));
				break;
			}
			if (Basis.getApiLevel() >= 11) { aa.addAll(tempitems); }
			else { for (String s : tempitems) { aa.add(s); } }
			aa.notifyDataSetChanged();
		}
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		
	}


	
	public void setOnTextFromDialogListListener(OnTextFromDialogListListener listener) {
        TextFromDialogListListener = listener;
    }
	

}
