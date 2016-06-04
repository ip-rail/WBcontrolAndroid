package wb.control.fragments;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Commandset;
import wb.control.Macro;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_yes_no;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class Frag_macro_edit extends Fragment
implements View.OnClickListener, AdapterView.OnItemSelectedListener, DialogFrag_yes_no.OnyesnoDialogListener, WBFragID {

	
	//public static final int MACRO_EDIT		= 	1;
	//public static final int MACRO_NEW		= 	0;

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_MACRO_EDIT;

	View fragview;	// Root-View für das Fragment
	OnFragReplaceListener fragReplListener;
	//int action;
	ArrayList<String> devlist;
	String last_selected_devicename;
	ArrayAdapter<String> aa;	// für spinner
	String newdevice;	// zum hinzufügen eines neuen Devices zum Macro
	Macro tempmacro;		// zum Zwischenspeichern für Neuanlage und Änderung (damit Änderungen wieder verworfen werden können)
	// CompletionInfo[] cmdsCompletionInfos;	// Array der CompletionInfos für das editText_mace_cmds ("<" und ">")

	Button button_mace_device_add, button_mace_device_del, button_mace_save, button_mace_cancel;
	Spinner spinner_mace_device;
	TextView textView_mace_titel, textView_mace_rfid;
	EditText editText_mace_name, editText_mace_cmds, editText_mace_comment, editText_mace_rfid;


    public int getFragmentID() { return FRAGMENT_ID; }


	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            fragReplListener = (OnFragReplaceListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragReplaceListener");
        }
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        devlist = new ArrayList<String>();
		
		/*
		cmdsCompletionInfos = new CompletionInfo[2];
		cmdsCompletionInfos[0] = new CompletionInfo(1, 0, "<");
		cmdsCompletionInfos[1] = new CompletionInfo(2, 1, ">");
		*/
				
	}	// end onCreate

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		fragview = inflater.inflate(R.layout.macro_edit, container, false);
		
		button_mace_device_add = (Button)fragview.findViewById(R.id.button_mace_device_add);
		button_mace_device_add.setOnClickListener(this);
		button_mace_device_del = (Button)fragview.findViewById(R.id.button_mace_device_del);
		button_mace_device_del.setOnClickListener(this);

		button_mace_save = (Button)fragview.findViewById(R.id.button_mace_save);
		button_mace_save.setOnClickListener(this);
		button_mace_cancel = (Button)fragview.findViewById(R.id.button_mace_cancel);
		//button_mace_cancel.setOnClickListener(this);
		button_mace_cancel.setVisibility(View.GONE);	// später ganz (aus Layout) entfernen, wenn ok
		textView_mace_titel = (TextView)fragview.findViewById(R.id.textView_mace_titel);
		textView_mace_rfid = (TextView)fragview.findViewById(R.id.textView_mace_rfid);

		editText_mace_name = (EditText)fragview.findViewById(R.id.editText_mace_name);
		editText_mace_cmds = (EditText)fragview.findViewById(R.id.editText_mace_cmds);
		//editText_mace_cmds.setOnKeyListener(this);
		
		// funkt nicht
		//InputMethodManager IMM = (InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
		//IMM.displayCompletions(editText_mace_cmds, cmdsCompletionInfos);

		editText_mace_comment = (EditText)fragview.findViewById(R.id.editText_mace_comment);
		editText_mace_rfid = (EditText)fragview.findViewById(R.id.editText_mace_rfid);

		spinner_mace_device = (Spinner)fragview.findViewById(R.id.spinner_mace_device);

		aa=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, devlist); 
		//aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		spinner_mace_device.setAdapter(aa);
		spinner_mace_device.setOnItemSelectedListener(this);

		return fragview;
	}	// end onCreateView
	
	

	@Override
	public void onResume() {

		super.onResume();
        fresume();
	}
	
	
	@Override
	public void onPause() {

        fpause();
		super.onPause();
	}


    private void fresume() {

        Boolean checknewdevice = true;	// kennung, ob newdevice ausgewertet werden soll (Abfrage wg. doppelter Einträge..)
        last_selected_devicename = "";	// muss geleert werden

        devlist.clear();	// DeviceAuswahlliste leeren
        textView_mace_rfid.setVisibility(View.GONE);
        editText_mace_rfid.setVisibility(View.GONE);

        newdevice = ((FAct_control) getActivity()).get_macro_newdevice();
        if ((newdevice == null) || (newdevice.equals("")))
        {
            newdevice = "";
            checknewdevice = false;
        }

        tempmacro = ((FAct_control) getActivity()).get_macro_to_edit();

        switch (tempmacro.getType()) {

            case Macro.MACROTYP_MACRO:
                textView_mace_titel.setText(R.string.macro_edit_macro_edit);
                break;

            case Macro.MACROTYP_EVENT:
                textView_mace_titel.setText(R.string.macro_edit_event_edit);
                editText_mace_name.setEnabled(false);	// EditText sperren, darf nicht geändert werden - funkt noch nicht
                break;

            case Macro.MACROTYP_RFID:
                textView_mace_rfid.setVisibility(View.VISIBLE);
                editText_mace_rfid.setVisibility(View.VISIBLE);
                textView_mace_titel.setText(R.string.macro_edit_rfid_edit);
                if (tempmacro.getRfidcode() == null) { tempmacro.setRfidcode(""); }	// bei RFID muss rfidcode mindestens "" sein, nicht null (wäre macro)
                editText_mace_rfid.setText(tempmacro.getRfidcode());
                break;
        }


        editText_mace_name.setText(tempmacro.getName());
        editText_mace_comment.setText(tempmacro.getComment());

        String devname = "";
        Boolean newdevfound = false;

        // nur checken, wenn ein Name eingetragen ist
        for (Commandset cs : tempmacro.getCommandlist())	// Liste der vorhandenen Devices / Gruppen landen
        {
            if (cs != null)
            {
                devname = cs.getDevicename();
                devlist.add(devname);
                // checken, ob newdevice bereits vorhanden ist
                if (checknewdevice) { if (devname.equals(newdevice)) { newdevfound = true; } }
            }
        }

        if (checknewdevice && !newdevfound) // neues device hinzufügen, wenn eins angegeben wurde, aber nicht doppelt einfügen
        {
            tempmacro.AddCommandSet(newdevice);
            devlist.add(newdevice);
            ((FAct_control) getActivity()).set_macro_newdevice("");	// device wurde hinzugefügt -> also erledigt
        }
        aa.notifyDataSetChanged();

        if (!newdevice.equals(""))	 // und dieses gleich im spinner auswählen
        {
            int index = getDevlistIndexByName(newdevice);
            if (index > -1) { spinner_mace_device.setSelection(getDevlistIndexByName(newdevice)); }	// im spinner auswählen
        }

    } //end fresume


    private void fpause() {

        save();	// beim Verlassen das Macro in der Basis speichern (noch nicht in der DB!!)
    }





	// für spinner Deviceauswahl
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		
		String devicename = parent.getItemAtPosition(position).toString();
		if (!devicename.equals(last_selected_devicename)) { saveCommands(last_selected_devicename); }
		
		// Befehlstext des neuen Devices als ein String ins editText schreiben
		editText_mace_cmds.setText(tempmacro.getCommandlist().get(tempmacro.getCommandlistIndexByDeviceName(devicename)).getCommandsString());
		last_selected_devicename = devicename;	// altes Device für den nächsten Wechsel speichern
		
		// button_mace_device_del (remove Device) verstecken, wenn "Standard"-Device ausgewählt wurde (sonst anzeigen)
		if (devicename.equals(Basis.getMacroStandardDeviceName()))
		{
			button_mace_device_del.setVisibility(View.GONE);
		}
		else { button_mace_device_del.setVisibility(View.VISIBLE); }
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onClick(View v) {
		
		int id = v.getId();
		if (id == R.id.button_mace_save) {
			// save(); 	// Daten speichern -> jetzt immer in onPause
			((FAct_control) getActivity()).set_macro_to_edit(null);		// fertig, daher in Activity zwischengespeicherte Daten löschen
			((FAct_control) getActivity()).set_macro_newdevice("");
			fragReplListener.OnFragReplace(FAct_control.FRAGMENT_MACRO_OVERVIEW, true, null);
		} else if (id == R.id.button_mace_device_add) {
			((FAct_control) getActivity()).set_macro_to_edit(tempmacro);
			fragReplListener.OnFragReplace(FAct_control.FRAGMENT_MACRO_ADDDEV, true, null);
		} else if (id == R.id.button_mace_device_del) {
			String devname = spinner_mace_device.getSelectedItem().toString();
			if (!devname.equals(Basis.getMacroStandardDeviceName()))
			{
				DialogFrag_yes_no ynfrag = new DialogFrag_yes_no();
				Bundle args = new Bundle();
				args.putString("title", devname);
				args.putString("msg", getString(R.string.macro_edit_remdev_msg));
				args.putBoolean("cancel", true);
				ynfrag.setArguments(args);
				ynfrag.setOnyesnoDialogListener(this);
				ynfrag.show(getFragmentManager(), "jnremdevdialog");
			}
		}
	}

	// andere methods
	
	
	public void save()
	{
		// Commandtext des aktuell ausgewählten Devices muss noch (in Arrayform) umgewandelt werden (passiert sonst beim Devicewechsel beim spinner)
		saveCommands(spinner_mace_device.getSelectedItem().toString());
		if (!(tempmacro.getType() == Macro.MACROTYP_EVENT)) { tempmacro.setName(editText_mace_name.getText().toString()); }
		tempmacro.setComment(editText_mace_comment.getText().toString());
		if (tempmacro.getType() == Macro.MACROTYP_RFID) { tempmacro.setRfidcode(editText_mace_rfid.getText().toString()); }
	}
	
	
	private void saveCommands(String devicename)	// für Auswahl anderes Device und Save bei Exit: die commands des aktuell ausgewählten Devices speichern
	{
		if ((devicename != null) && (!devicename.equals("")))
		{
			if (!(editText_mace_cmds.getText().toString()).equals(""))
			{
				int cindex = tempmacro.getCommandlistIndexByDeviceName(devicename);
				if (cindex != -1)
				{
					Commandset cs = tempmacro.getCommandlist().get(cindex);
					cs.ImportCommandString(editText_mace_cmds.getText().toString());
				}
			}
		}
	}
	

	private int getDevlistIndexByName(String dname)	// Name suchen und Index zurückgeben
	{
		int index = -1;		// -1 = not found
		for (String s : devlist)
		{
			if (s.equals(dname)) { index = devlist.indexOf(s);  break; }
		}
		return index;
	}

	@Override
	public void OnyesnoDialog(int typ, Boolean antwort) {	// typ ist egal, wird in diesem Fragment nur 1x verwendet
		
		if (antwort) // true: device entfernen, sonst nicht!!
		{
			String devicename = spinner_mace_device.getSelectedItem().toString();	
			Commandset cs = tempmacro.getCommandSetByDeviceName(devicename);
			if (cs != null)
			{
				tempmacro.RemoveCommandSet(cs);
			}
			aa.remove(devicename);
			aa.notifyDataSetChanged();
		}	
	}

}
