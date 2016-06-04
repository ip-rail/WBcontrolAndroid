package wb.control.fragments;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Macro;
import wb.control.OnActionBarConfigListener;
import wb.control.OnActionBarItemListener;
import wb.control.OnFragReplaceListener;
import wb.control.OnHideMnuItemListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Frag_macro_overview extends Fragment
implements AdapterView.OnItemSelectedListener, OnActionBarItemListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_MACRO_OVERVIEW;

    View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	OnActionBarConfigListener aBarConfigListener;
	Button Button_maco_ctrl, Button_maco_act;
	Spinner spinner_maco_typ;
	ListView listView_maco;
	String[] macrotyp; // für Typ-Auswahl, bei den Ressources (values/arrays) gespeichert
	View CMtargetView; // speichert den View, von dem aus das aktuelle ContextMenu gestartet wurde
	//ArrayAdapter<Macro> lvaa;	// Adapter für Macroliste
	MacroAdapter lvaa;	// Adapter für Macroliste
	ArrayList<Macro> macrolist;


    public int getFragmentID() { return FRAGMENT_ID; }

	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            fragReplListener = (OnFragReplaceListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragReplaceListener");
        }
        
        try {
			aBarConfigListener = (OnActionBarConfigListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnActionBarConfigListener");
		}

    }
	
    /*
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	} */

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		fcontainer = container;
		fragview = inflater.inflate(R.layout.f_macro_overview, container, false);

		spinner_maco_typ = (Spinner)fragview.findViewById(R.id.spinner_maco_typ);
		macrotyp = getResources().getStringArray(R.array.macro_typ);
		ArrayAdapter<String> aa=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, macrotyp); 
		//aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		spinner_maco_typ.setAdapter(aa);
		spinner_maco_typ.setOnItemSelectedListener(this);

		listView_maco = (ListView)fragview.findViewById(R.id.listView_maco);
		//listView_maco.setOnItemClickListener(this);
		//registerForContextMenu(listView_maco);
		macrolist = new ArrayList<Macro>();
		lvaa = new MacroAdapter(getActivity(), android.R.layout.simple_list_item_1, macrolist, inflater);
		listView_maco.setAdapter(lvaa);

		return fragview;
	}	// end onCreateView


	/*
	@Override
	public void onDestroyView() {
		super.onDestroyView();

	} */


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

        aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_ADD, true);	// ActionBar Button "add Item" anzeigen
        spinner_maco_typ.setSelection(Basis.getActualMacrotyp());	// Macrotyp-auswahl setzen

	}

	private void fpause() {

        Basis.setActualMacrotyp(spinner_maco_typ.getSelectedItemPosition());	// aktuelle Macrotyp-auswahl sichern
        aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_ADD, false);	// ActionBar Button "add Item" verstecken
	}


	// Spinner Listeners

	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) 
	{ 
		macrolist.clear();

		switch (position) {

		case 0:		// 0: Macros
			macrolist.addAll(Basis.getMacrolist());
			break;

		case 1:		// 1: Ereignisse
			macrolist.addAll(Basis.getWbeventlistOnlyEvents());
			break;

		case 2:		// 2: RFIDs
			macrolist.addAll(Basis.getWbeventlistOnlyRFIDs());
			break;
		}

		lvaa.notifyDataSetChanged();
	}

	public void onNothingSelected(AdapterView<?> parent) 
	{ 
		// selection.setText(""); 
	} 



	// ContextMenu
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		//super.onCreateContextMenu(menu, v, menuInfo);

		int mtyp = spinner_maco_typ.getSelectedItemPosition();	// Index des ausgewählten Macro-Typ ermitteln (0: Macros / 1: Ereignisse / 2: RFIDs)

		if (mtyp != 1)	//  bei Typ 1: Ereignisse kein ContextMenu aufbauen
		{
			CMtargetView = v;
			//Macro m = (Macro) CMtargetView.getTag();

			menu.clear();	// Menu leeren!!
			MenuInflater inflater = new MenuInflater(getActivity().getApplicationContext());
			inflater.inflate(R.menu.menu_macro_context, menu);
			// falls was angepasst werden muss:
			//menu.removeItem(R.id.menui_macro_del);	// menui_macro_del / menui_macro_new / menui_macro_copy
			menu.removeItem(R.id.menui_macro_new);
		}
	}

	public void onContextMenuClosed (Menu menu)
	{
		CMtargetView = null;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int mtyp = spinner_maco_typ.getSelectedItemPosition();	// Index des ausgewählten Macro-Typ ermitteln (0: Macros / 1: Ereignisse / 2: RFIDs)

		int itemId = item.getItemId();
		if (itemId == R.id.menui_macro_del) {
			if (CMtargetView != null)
			{
				Macro m = (Macro) CMtargetView.getTag();

				if (m != null)
				{
					if (!m.getName().equals("leer"))	// das leer-Macro nicht löschen lassen
					{

						switch (mtyp) {

						case Macro.MACROTYP_MACRO:		// 0: Macros
							Basis.RemoveMacro(m.getName());
							break;

						case Macro.MACROTYP_EVENT:		// 1: Ereignisse
							Basis.RemoveWbevent(m.getName(), false);	// sollen nicht händisch angelegt werden
							break;

						case Macro.MACROTYP_RFID:		// 2: RFIDs
							Basis.RemoveWbevent(m.getName(), true);
							break;
						}

						macrolist.remove(m);	// jetzt noch in der macrolist entfernen
						lvaa.notifyDataSetChanged();
					}
				}
			}
			return true;
			
		} else if (itemId == R.id.menui_macro_new) {
			//macrolist.clear();
			addNewMacro(mtyp);
			return true;
			
		} else if (itemId == R.id.menui_macro_copy) {
			if (CMtargetView != null)
			{
				macrolist.clear();
				//Macro m = new Macro();
				Macro m_alt = (Macro) CMtargetView.getTag();
				if (m_alt != null)
				{						
					Macro m_neu = new Macro();
					m_neu.copyFrom(m_alt);	// Daten aus dem bestehenden Macro übernehmen
					m_neu.setName(m_neu.getName() + "_Kopie");

					switch (mtyp) {

					case 0:		// 0: Macros
						Basis.AddMacro(m_neu);
						macrolist.addAll(Basis.getMacrolist());
						break;

						/*
					case 1:		// 1: Ereignisse	// sollen nicht händisch angelegt werden
						break;
						 */

					case 2:		// 2: RFIDs
						Basis.AddWBevent(m_neu);
						macrolist.addAll(Basis.getWbeventlistOnlyRFIDs());
						break;
					}

					lvaa.notifyDataSetChanged();
				}
			}
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}	// end  onContextItemSelected


	public class MacroAdapter extends ArrayAdapter<Macro> implements OnClickListener {
		private Context mContext; 
		LayoutInflater inflater;

		public MacroAdapter(Context context, int textViewResourceId, ArrayList<Macro> macrolist, LayoutInflater linf) {
			super(context, textViewResourceId, macrolist);
			mContext = context;
			inflater = linf;
		}

		public View getView(int position, View convertView, ViewGroup parent) {  

			View row=convertView; 

			if (row==null) { 
				row=inflater.inflate(R.layout.macro_overview_row, parent, false);
			} 
			unregisterForContextMenu(row);
			TextView textView_macro_ov_rowname = (TextView)row.findViewById(R.id.textView_macro_ov_rowname); 

			Macro m = macrolist.get(position);
			textView_macro_ov_rowname.setText(m.getName());
			//tv.setTextAppearance(parent.getContext(), R.style.txt_listrow_big);
			row.setTag(m);	// Macro im Tag abspeichern
			registerForContextMenu(row);
			row.setOnClickListener(this);

			return row;
		}


		@Override
		public void onClick(View view) {

			Macro m = (Macro) view.getTag();

			if (m != null)
			{
				String name = m.getName();

				if (!name.equals("leer"))		// das Macro "leer" darf nicht editiert werden - muss leer bleiben
				{
					((FAct_control) getActivity()).set_macro_to_edit(m);
					fragReplListener.OnFragReplace(FAct_control.FRAGMENT_MACRO_EDIT, true, null);
				}
			}
		}
	}

	
	// neues Macro hinzufügen
	private void addNewMacro(int mtyp)
	{
		Macro m = null;
		
		switch (mtyp) {

		case Macro.MACROTYP_MACRO:		// 0: Macros
			m = new Macro("neues Macro", mtyp);
			Basis.AddMacro(m);
			break;

		case Macro.MACROTYP_EVENT:		// 1: Ereignisse	// sollen nicht händisch angelegt werden
			break;

		case Macro.MACROTYP_RFID:		// 2: RFIDs
			m = new Macro("neues RFID", mtyp);
			Basis.AddRFID(m);
			break;
		}

		if (m != null)
		{
			((FAct_control) getActivity()).set_macro_to_edit(m);
			fragReplListener.OnFragReplace(FAct_control.FRAGMENT_MACRO_EDIT, true, null);
		}

	}

	@Override
	public void OnActionBarItem(int itemID) {
		
		addNewMacro(spinner_maco_typ.getSelectedItemPosition());
	}
}
