package wb.control.fragments;

import java.util.ArrayList;

import wb.control.ActionElement;
import wb.control.Basis;
import wb.control.Macro;
import wb.control.OnFragReplaceListener;
import wb.control.OnHideMnuItemListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.WBlog.wblogtype;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_ChooseFromList;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
//wbFragment

public class Frag_action_edit extends Fragment
implements View.OnClickListener, AdapterView.OnItemSelectedListener, DialogFrag_ChooseFromList.OnTextFromDialogListListener, WBFragID {

	private static final int FRAGMENT_ID = FAct_control.FRAGMENT_ACTION_EDIT;

    private int leermacro_index = -1;
	OnFragReplaceListener fragReplListener;
    private View fragview;	// Root-View für das Fragment
    private Button button_ae_edit_add, button_ae_edit_cancel, button_ae_edit_adddev;
    private TextView textView_ae_edit_titel, textView_ae_edit_macro, textView_ae_edit_macro2, textView_ae_edit_datatype,
		textView_ae_edit_text, textView_ae_edit_scope, textView_ae_edit_scopedata, textView_ae_edit_location;
    private EditText editText_ae_edit_text, editText_ae_edit_scopedata;
    private Spinner spinner_ae_edit_typ, spinner_ae_edit_macro, spinner_ae_edit_macro2, spinner_ae_edit_scope, spinner_ae_edit_location, spinner_ae_edit_datatype;


    private ActionElement ae;		// für new/edit Daten
    private int aeindex;			// Index des ae in der Basis.actionelementlist
    private ArrayAdapter<Macro> aa;	// für spinner
    private ArrayList<Macro> mlist;
    private String[] scope;					// für Scope-Auswahl statt ArrayList<String> scopelist;
    private ArrayAdapter<String> sa;		// für Scope-Spinner

    private String[] ActionElement_Typ;			// für Typ-Auswahl
    private ArrayAdapter<String> ta;			// für Typ-Spinner
    private String[] ActionElement_Locations;	// für Location-Auswahl
    private ArrayAdapter<String> la;			// für Location-Spinner
    private String[] datatypes;					// für Datatype-Auswahl
    private String[] datatype_pretxt;			// für Datatype: Text vor den eigentlichen Daten
    private ArrayAdapter<String> da;			// für Datatype-Spinner


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
    
    /*
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}	// end onCreate */

	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		fragview = inflater.inflate(R.layout.action_ae_edit, container, false);

		button_ae_edit_add = (Button)fragview.findViewById(R.id.button_ae_edit_add);
		button_ae_edit_add.setOnClickListener(this);
		button_ae_edit_cancel = (Button)fragview.findViewById(R.id.button_ae_edit_cancel);
		button_ae_edit_cancel.setOnClickListener(this);
		button_ae_edit_adddev  = (Button)fragview.findViewById(R.id.button_ae_edit_adddev);
		button_ae_edit_adddev.setOnClickListener(this);
		
		textView_ae_edit_titel = (TextView)fragview.findViewById(R.id.textView_ae_edit_titel);
		textView_ae_edit_text = (TextView)fragview.findViewById(R.id.textView_ae_edit_text);
		textView_ae_edit_macro = (TextView)fragview.findViewById(R.id.textView_ae_edit_macro);
		textView_ae_edit_macro2 = (TextView)fragview.findViewById(R.id.textView_ae_edit_macro2);
		textView_ae_edit_scope = (TextView)fragview.findViewById(R.id.textView_ae_edit_scope);
		textView_ae_edit_scopedata = (TextView)fragview.findViewById(R.id.textView_ae_edit_scopedata);
		textView_ae_edit_location = (TextView)fragview.findViewById(R.id.textView_ae_edit_location); 
		textView_ae_edit_datatype = (TextView)fragview.findViewById(R.id.textView_ae_edit_datatype);
		
		editText_ae_edit_text  = (EditText)fragview.findViewById(R.id.editText_ae_edit_text);
		editText_ae_edit_scopedata  = (EditText)fragview.findViewById(R.id.editText_ae_edit_scopedata);
		// muss wg. Devicename-hinzufügen immer aktuell sein, auch nach händischem Editieren
		//editText_ae_edit_scopedata.setOnKeyListener(this);
		//editText_ae_edit_scopedata.setOnFocusChangeListener(this);
		
		spinner_ae_edit_typ = (Spinner)fragview.findViewById(R.id.spinner_ae_edit_typ);
		ActionElement_Typ = getResources().getStringArray(R.array.actionelement_typ);
		ta=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, ActionElement_Typ); 
		//ta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		spinner_ae_edit_typ.setAdapter(ta);
		spinner_ae_edit_typ.setOnItemSelectedListener(this);
		
		spinner_ae_edit_macro = (Spinner)fragview.findViewById(R.id.spinner_ae_edit_macro);
		spinner_ae_edit_macro2 = (Spinner)fragview.findViewById(R.id.spinner_ae_edit_macro2);
		mlist = new ArrayList<Macro>();
		aa=new ArrayAdapter<Macro>(getActivity(), android.R.layout.simple_spinner_item, mlist); 
		//aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		spinner_ae_edit_macro.setAdapter(aa);
		spinner_ae_edit_macro.setOnItemSelectedListener(this);
		spinner_ae_edit_macro2.setAdapter(aa);
		spinner_ae_edit_macro2.setOnItemSelectedListener(this);
				
		scope = getResources().getStringArray(R.array.actionelement_macroscope_typ);
		spinner_ae_edit_scope = (Spinner)fragview.findViewById(R.id.spinner_ae_edit_scope);
		//sa=new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, scopelist); 
		sa=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, scope); 
		//sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		spinner_ae_edit_scope.setAdapter(sa);
		spinner_ae_edit_scope.setOnItemSelectedListener(this);
		
		ActionElement_Locations = getResources().getStringArray(R.array.actionelement_locations);
		spinner_ae_edit_location  = (Spinner)fragview.findViewById(R.id.spinner_ae_edit_location);
		la=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, ActionElement_Locations);
		spinner_ae_edit_location.setAdapter(la);
		spinner_ae_edit_location.setOnItemSelectedListener(this);
		
		datatypes = getResources().getStringArray(R.array.actionelement_datatype);
		datatype_pretxt = getResources().getStringArray(R.array.actionelement_datatype_pretxt);
		spinner_ae_edit_datatype = (Spinner)fragview.findViewById(R.id.spinner_ae_edit_datatype);
		da=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, datatypes);
		spinner_ae_edit_datatype.setAdapter(da);
		spinner_ae_edit_datatype.setOnItemSelectedListener(this);
		
		return fragview;
	}
	
	@Override
	public void onResume() {

		super.onResume();
        fresume();
	}	// end onResume
	
	
	@Override
	public void onPause() {

        fpause();
		super.onPause();
	}



    private void fresume() {

        ae = null;
        aeindex = ((FAct_control) getActivity()).getAe_toedit();

        if (aeindex != -1) { ae = Basis.getActionElementList().get(((FAct_control) getActivity()).getAe_toedit()); }

        if (ae == null) // wenn kein ae vorhanden ist, gleich wieder zurück zu Frag_action
        {
            fragReplListener.OnFragReplace(FAct_control.FRAGMENT_ACTION, false, null);
            ((FAct_control) getActivity()).setAe_toedit(-1); // Ae_toedit wieder löschen
        }

        mlist.addAll(Basis.getMacrolist());
        aa.notifyDataSetChanged();

        // index von leer-Macro suchen und speichern
        if (mlist.size() > 0 )
        {
            leermacro_index = -1;		// -1 = not found
            for (Macro m : mlist)
            {
                if (m.getName().equals("leer")) { leermacro_index = mlist.indexOf(m); break; }
            }
            if (leermacro_index == -1) // Warnung im log speichern, wenn leermakro nicht gefunden wurde
            {
                leermacro_index = 0;	// in diesem Fall auf erstes Macro setzen
                Basis.AddLogLine("ActionEdit: leer-Macro wurde nicht gefunden ", "action", wblogtype.Warning);
            }
        }

        if (ae != null)	// wurde verher schon abgeprüft, wird hier nicht mehr benötigt
        {

            // Widgets setzen, die für neu und edit gleich sind


            // Widgets einrichten, die bei neu/edit unterschiedlich sind
            if (ae.view == null)	// neu
            {
                textView_ae_edit_titel.setText(R.string.action_ae_edit_titel_new);
                editText_ae_edit_text.setText("");
                if (ae.typ == ActionElement.AE_TYP_Data) { editText_ae_edit_text.setText(datatype_pretxt[ae.datatype]); }	// nur bei neuen ae setzen (User kkann es nachher beliebig ändern)
                spinner_ae_edit_typ.setSelection(ActionElement.AE_TYP_BUTTON);	// Typ: Button selektieren
                spinner_ae_edit_typ.setVisibility(View.VISIBLE);	// Typ-spinner anzeigen

            }
            else	// edit
            {
                textView_ae_edit_titel.setText(R.string.action_ae_edit_titel_edit);
                spinner_ae_edit_typ.setSelection(ae.typ);	// wg. verstecken der entsprechenden Widgets trotzdem setzen
                spinner_ae_edit_typ.setVisibility(View.GONE);	// Typ-spinner verstecken
                editText_ae_edit_text.setText(ae.text);


                // Macro im spinner auswählen

                if ((mlist.size() > 0 ) && (ae.macro != null))
                {
                    int mindex = -1;		// -1 = not found
                    for (Macro m : mlist)
                    {
                        if (m.getName().equals(ae.macro.getName())) { mindex = mlist.indexOf(m); break; }
                    }
                    if (mindex > -1) { spinner_ae_edit_macro.setSelection(mindex); }	// im spinner auswählen
                    else { spinner_ae_edit_macro.setSelection(leermacro_index); }	// leer-Macro auswählen
                }
                else { spinner_ae_edit_macro.setSelection(leermacro_index); }

                // off-Macro für ON/OFF-Buttons in Spinner einstellen, für alle anderen TextView und Spinner verstecken

                if (ae.typ == ActionElement.AE_TYP_ONOFF_BUTTON)
                {
                    spinner_ae_edit_macro2.setVisibility(View.VISIBLE);
                    textView_ae_edit_macro2.setVisibility(View.VISIBLE);

                    if ((mlist.size() > 0 ) && (ae.macro2 != null))
                    {
                        int mindex = -1;		// -1 = not found
                        for (Macro m : mlist)
                        {
                            if (m.getName().equals(ae.macro2.getName())) { mindex = mlist.indexOf(m); break; }
                        }
                        if (mindex > -1) { spinner_ae_edit_macro2.setSelection(mindex); }	// im spinner auswählen
                        else { spinner_ae_edit_macro2.setSelection(leermacro_index); }	//leer-Macro auswählen
                    }
                    else { spinner_ae_edit_macro2.setSelection(leermacro_index); }

                }
                else	// Macro2-Widgets verstecken
                {
                    spinner_ae_edit_macro2.setVisibility(View.GONE);
                    textView_ae_edit_macro2.setVisibility(View.GONE);
                }

                spinner_ae_edit_scope.setSelection(ae.scope);	// scope-Spinner einstellen

            }

            spinner_ae_edit_datatype.setSelection(ae.datatype);	// Datatype-Spinner einstellen (nur bei Typ=Data relevant, aber trotzdem immer machen)
            spinner_ae_edit_location.setSelection(ae.ort);	// Location-Spinner einstellen

            setWidgets(ae);	// je nach ae-Typ die entsprechenden Widgets aktivieren bzw. verstecken
        }

    } // end fresume


    private void fpause() {

        if (ae != null)	// Daten für AE nur sichern, wenn nicht mit "Cancel"-Button abgebrochen wurde
        {
            // restliche Daten nach ae sichern
            ae.text = editText_ae_edit_text.getText().toString();
            ae.scopedata = editText_ae_edit_scopedata.getText().toString();
            if (ae.scopedata.equals(getString(R.string.dialog_cfl_ccd))) { ae.scopedata = ""; }	// Dummyeintrag für CCD korrigieren -> ""
            //Nicht-Macro elemente säubern
            if ((ae.typ == ActionElement.AE_TYP_TEXT) || (ae.typ == ActionElement.AE_TYP_BILD))
            {
                ae.macro = null;
                ae.scope = ActionElement.AE_MACROSCOPE_TYP_NONE;
                ae.scopedata = "";
            }

            // zur Sicherheit AEs im Frag_control nochmals aktualisieren
            Intent aeintent = new Intent(Basis.ACTION_UPDATE_AE);
            aeintent.putExtra("aeindex", Basis.getActionElementList().indexOf(ae));
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(aeintent);

        }
    } // end fpause


	
// Buttons
	
	@Override
	public void onClick(View v) {
	
		int id = v.getId();
		if (id == R.id.button_ae_edit_add) {
			//data.putInt("result", Activity.RESULT_OK);
			//((FAct_control) getActivity()).setAe_toedit_mode(true);	// AE soll gespeichert werden
			fragReplListener.OnFragReplace(FAct_control.FRAGMENT_ACTION, false, null);
		} else if (id == R.id.button_ae_edit_cancel) {
			//data.putInt("result", Activity.RESULT_CANCELED);
			Basis.getActionElementList().remove(ae);	// Actionelement löschen
			ae = null;
			((FAct_control) getActivity()).setAe_toedit(-1);	// AE verwerfen, damit es nicht gespeichert wird
			fragReplListener.OnFragReplace(FAct_control.FRAGMENT_ACTION, false, null);
		} else if (id == R.id.button_ae_edit_adddev) {
			// aktuelles editText_ae_edit_scopedata vorher unbedingt speichern (damit händische Änderungen nicht verlorengehen)
			ae.scopedata = editText_ae_edit_scopedata.getText().toString();
			DialogFrag_ChooseFromList dialog = new DialogFrag_ChooseFromList();
			Bundle args = new Bundle();
			args.putInt("dialogtype", DialogFrag_ChooseFromList.DIALOG_CFL_DEVICENAMES);	// Devicenamen
			dialog.setArguments(args);
			// dialog.setTargetFragment(this, 0);	// Fragment für Ergebnisrückmeldung mitgeben -> wg. Listener nicht mehr notwendig
			dialog.setOnTextFromDialogListListener(this);
			dialog.show(getFragmentManager(), "cfldialog");		// TODO muss der tag noch geändert werden???
		}
	}	// end onClick
	
	
	// für spinner
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long itemid) {

		int id = parent.getId();
		if (id == R.id.spinner_ae_edit_typ) {
			ae.typ = position;
			setWidgets(ae);		// entsprechende Widgets anzeigen/verstecken
		} else if (id == R.id.spinner_ae_edit_macro) {
			ae.macro = (Macro)parent.getItemAtPosition(position);
		} else if (id == R.id.spinner_ae_edit_macro2) {
			ae.macro2 = (Macro)parent.getItemAtPosition(position);
		} else if (id == R.id.spinner_ae_edit_scope) {
			ae.scope = position;
			setScopeDataVisibility(position);
		} else if (id == R.id.spinner_ae_edit_location) {
			int ortalt = ae.ort;
			ae.ort = position;
			if (ortalt !=position)
			{
				Intent aeintent = new Intent(Basis.ACTION_UPDATE_AE);
				aeintent.putExtra("aeindex", Basis.getActionElementList().indexOf(ae));
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(aeintent);
			}
		} else if (id == R.id.spinner_ae_edit_datatype) {
			ae.datatype = position;
			editText_ae_edit_text.setText(datatype_pretxt[ae.datatype]);	// pre-text setzen (usertext wird wieder überschrieben)
		}
		
	}	// end spinner onItemSelected

	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
	void setWidgets(ActionElement ae)	// entsprechend dem ae die grafischen elemente anpassen
	{
		spinner_ae_edit_datatype.setVisibility(View.GONE);
		textView_ae_edit_datatype.setVisibility(View.GONE);
		button_ae_edit_adddev.setVisibility(View.GONE);
		
		switch (ae.typ) {	//  Macro-Widgets je nach AE-Typ verstecken oder anzeigen

		case ActionElement.AE_TYP_TEXT:
			setMacroWidgetVisibility(View.GONE, 0);
			if (ae.scope != ActionElement.AE_MACROSCOPE_TYP_NONE) { ae.scope = ActionElement.AE_MACROSCOPE_TYP_NONE; }
			break;

		case ActionElement.AE_TYP_BUTTON:
			setMacroWidgetVisibility(View.VISIBLE, 1);
			if (ae.scope == ActionElement.AE_MACROSCOPE_TYP_NONE) { ae.scope = ActionElement.AE_MACROSCOPE_TYP_CCDEVICE; }
			break;

		case ActionElement.AE_TYP_BILD:
			setMacroWidgetVisibility(View.GONE, 0);
			if (ae.scope != ActionElement.AE_MACROSCOPE_TYP_NONE) { ae.scope = ActionElement.AE_MACROSCOPE_TYP_NONE; }
			break;

		case ActionElement.AE_TYP_BILDBUTTON:
			setMacroWidgetVisibility(View.VISIBLE, 1);
			if (ae.scope == ActionElement.AE_MACROSCOPE_TYP_NONE) { ae.scope = ActionElement.AE_MACROSCOPE_TYP_CCDEVICE; }
			break;
			
		case ActionElement.AE_TYP_ONOFF_BUTTON:
			setMacroWidgetVisibility(View.VISIBLE, 2);
			if (ae.scope == ActionElement.AE_MACROSCOPE_TYP_NONE) { ae.scope = ActionElement.AE_MACROSCOPE_TYP_CCDEVICE; }
			break;
			
		case ActionElement.AE_TYP_Data:
			spinner_ae_edit_datatype.setVisibility(View.VISIBLE);
			textView_ae_edit_datatype.setVisibility(View.VISIBLE);
			setMacroWidgetVisibility(View.GONE, 0);
			ae.scope = ActionElement.AE_MACROSCOPE_USED_AS_DATA_SOURCE;		// wichtig: Kennung dafür, dass Datenquelle in scopedata gespeichert wird
			// Scopedata anpassen ("" bedeutet CCD (aktuell gesteuertes Device))
			if (ae.scopedata.equals("")) { editText_ae_edit_scopedata.setText(getString(R.string.dialog_cfl_ccd)); }
			else { editText_ae_edit_scopedata.setText(ae.scopedata); }
			break;
		}
		
		setScopeDataVisibility(ae.scope);
	}
	
	
	
	void setScopeDataVisibility(int scopetype)	// Widgets, die nur für ScopeData benötigt werden
	{
		int visibility = View.GONE;
		
		switch (scopetype) {	// ScopeData je nach Typ verstecken oder anzeigen/befüllen

		/* case ActionElement.AE_MACROSCOPE_TYP_NONE:
			visibility = View.GONE;
			break; */
			
		/* case ActionElement.AE_MACROSCOPE_TYP_CCDEVICE:
			visibility = View.GONE;
			break; */

		case ActionElement.AE_MACROSCOPE_TYP_NAMES:
			textView_ae_edit_scopedata.setText(R.string.action_ae_edit_scopedata_name);
			visibility = View.VISIBLE;
			break;

		/* case ActionElement.AE_MACROSCOPE_TYP_ALLNAMES:
			visibility = View.GONE;
			break; */

		/* case ActionElement.AE_MACROSCOPE_TYP_ALL:
			visibility = View.GONE;
			break; */

		/* case ActionElement.AE_MACROSCOPE_TYP_ALLCON:
			visibility = View.GONE;
			break; */

		case ActionElement.AE_MACROSCOPE_TYP_ALLFROMTYPE:
			textView_ae_edit_scopedata.setText(R.string.action_ae_edit_scopedata_type);
			visibility = View.VISIBLE;
			break;

		case ActionElement.AE_MACROSCOPE_TYP_ALLCONTYPE:
			textView_ae_edit_scopedata.setText(R.string.action_ae_edit_scopedata_type);
			visibility = View.VISIBLE;
			break;
			
		case ActionElement.AE_MACROSCOPE_USED_AS_DATA_SOURCE:
			textView_ae_edit_scopedata.setText(R.string.action_ae_edit_scopedata_datasource);
			visibility = View.VISIBLE;
			break;
		}
		
		textView_ae_edit_scopedata.setVisibility(visibility);
		editText_ae_edit_scopedata.setVisibility(visibility);
		button_ae_edit_adddev.setVisibility(visibility);
	}
	
	void setMacroWidgetVisibility(int visibility, int macrocount)	// Widgets, die für tex/bild nicht benötigt werden
	{
		int vis2 = View.GONE;	// visibility für off-Macro
		textView_ae_edit_macro.setVisibility(visibility);
		spinner_ae_edit_macro.setVisibility(visibility);
		textView_ae_edit_scope.setVisibility(visibility);
		spinner_ae_edit_scope.setVisibility(visibility);
		
		if (macrocount == 2)	// für on/off Button werden 2 Macros benötigt
		{
			// 2. Macro anzeigen und Texte korrekt setzen ("on-Macro", "off-Macro")
			textView_ae_edit_macro.setText(R.string.action_macro_on);
			vis2 = View.VISIBLE;
		}
		else
		{
			textView_ae_edit_macro.setText(R.string.action_macro);	// nur "Macro"
		}
		textView_ae_edit_macro2.setVisibility(vis2);
		spinner_ae_edit_macro2.setVisibility(vis2);
	}
	


	@Override
	public void OnTextFromDialogList(String text, int nummer, int typ) {
		// Übernahme von Daten aus dem DialogFrag_ChooseFromList (Devicenamen von scopedata)
		if (text != null)
		{
			if (ae.typ == ActionElement.AE_TYP_Data) { ae.scopedata = text; }	// nur 1 Devicename möglich -> immer ersetzen
			else	// sonst mehrere Namen zulassen
			{
				if (ae.scopedata.equals("")) { ae.scopedata = text; }
				else 
				{ 
					// nur einfügen, wenn der Name noch nicht vorhanden ist (Groß/Kleinschreibung wird ignoriert)
					String[] checknames = ae.scopedata.split(",");

					Boolean found = false;
					for (String s : checknames)
					{
						if (s.toUpperCase().equals(text.toUpperCase())) { found = true; }
					}
					if (!found) { ae.scopedata += "," + text; }
				}
			}
			editText_ae_edit_scopedata.setText(ae.scopedata);
		}
	}

	
	
	
} // end class Frag_action_edit
