package wb.control.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Basis.runmodetype;
import wb.control.OnConfigChangedListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.WBnetwork;
import wb.control.activities.FAct_control;

public class Frag_cfg_level2 extends ListFragment implements WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_CFG_LEVEL2;

	// CFG Daten ID = Array index von config_name usw (siehe: Ressourcen: values)
	static final int CFGD_MANUAL_IP			= 0;	// manuelle Lok-IP
	static final int CFGD_TCP_PORT			= 1;	// tcp-Port
	static final int CFGD_UDP_PORT			= 2;	// udp-Port
	static final int CFGD_UDP_VERWENDEN		= 3;
	static final int CFGD_TCPSERVER_PORT	= 4;
	static final int CFGD_SERVER_VERWENDEN	= 5;	
	static final int CFGD_SPEEDSTUFEN		= 6;	// typ=3 group =1	CFGD_SPEEDSTUFEN	Spinner: 128 / 256 / 512 / 1024 // TODO: ist nur der Defaultwert -> muss bei den Loks eingestellt werden
	static final int CFGD_RANGIERSPEED		= 7;	// max. Rangierspeed typ=6 group =1	CFGD_RANGIERSPEED	Seekbar
	static final int CFGD_LAYOUTTYPE		= 8;	// Auswahl des Bildschirm-Layouts (Control links oder rechts)
	static final int CFGD_LAYOUT_DIV_MODE	= 9;	// Bildschirmaufteilung erzwingen (single/dual/multiview) typ=3
	static final int CFGD_NO_SCREENSAVER	= 10;	// Bildschirmschoner deaktivieren
	static final int CFGD_FONT_SCALE		= 11;	// fontScale
	static final int CFGD_RUNMODE			= 12;	// Testmodus für Programm (standard (= kein test) / test)
	static final int CFGD_THEME				= 13;	// ThemeType (standard: dark / light)
	static final int CFGD_USED_NETWORK		= 14;	// verwendetes Netzwerk
	static final int CFGD_PROGUPDATE		= 15;	// Programmupdates suchen
	static final int CFGD_STARTGUESTMODE	= 16;	// Gästemodus aktivieren
	static final int CFGD_ENDGUESTMODE		= 17;	// Gästemodus beenden: nur Button / Passworteingabe notwendig
	static final int CFGD_GUESTMODE_PWD		= 18;	// Gästemodus Passwort festlegen
	static final int CFGD_LOKSTOPMODE		= 19;	// Lokstopmodi festlegen
	
	static final int LOKSTOPMODE_CALL		= 1;	// Lokstopmode Telefonanruf 
	static final int LOKSTOPMODE_VLOW		= 2;	// Lokstopmode Schienenspannung zu niedrig
	static final int LOKSTOPMODE_NO_CONTROL	= 4;	// Lokstopmode Frag_control ist nicht aktiv
	static final int LOKSTOPMODE_NO_CONNECT	= 8;	// Lokstopmode keine Netzwerkverbindung zwischen Steuergerät und Lok
	
	final static String IPADDRESS_PATTERN =
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	OnConfigChangedListener CfgChangedListener;
	LayoutInflater layoutInflater;
	CfgAdapter cfga;
	String[] cfg_groupname;
	int cfg_selected_group;		// die übergebene Gruppe (int index), deren Elemente im ListView dargestellt werden sollen
	public ArrayList<CfgElement> CFG_Elements;
	
	EditText editText_dialog_cfg_str_data;
	TextView textView_dialog_cfg_str,textView_dialog_cfg_intseek,textView_dialog_cfg_intseek2;
	//SeekBar seekBar_dialog_cfg_intseek;
	ListView listView_dialog_cfg_list;
	Button button_dialog_cfg_list_cancel;


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
        	CfgChangedListener = (OnConfigChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnChangeDisplayModeListener");
        }

    }
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		cfg_selected_group = -1;	// leer
		
		if (savedInstanceState != null) // wenn wg. OrientationChange Daten gespeichert wurden, diese wieder holen
		{
			Basis.setConfigfragGroup(savedInstanceState.getInt("cfg_group"));	// in Basis speichern, da nur beim ersten start oder bei OrientationChange onCreate durchlaufen wird
		}
		
	} 
	
	/*
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		
	} */
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		String[] cfg_name;
		String[] cfg_description;
		int[] cfg_type;
		int[] cfg_group;

		// group // Änderung: gleich immer in Basis speichern, siehe auch frag_cfg_level1
		
		cfg_selected_group = Basis.getConfigfragGroup();
		if (cfg_selected_group < 0) { cfg_selected_group = 0; }	// falls nix gespeichert, erste Gruppe verwenden
		//Basis.setConfigfragGroup(-1);	// wieder löschen	// ist nicht notwendig

		
		// Adapter initialisiern und mit passenden Daten laden
		ArrayList<CfgElement> CFG_Elements = new ArrayList<CfgElement>();
		cfg_groupname = getResources().getStringArray(R.array.config_level1);
		cfg_name = getResources().getStringArray(R.array.config_name);
		cfg_description =  getResources().getStringArray(R.array.config_description);
		cfg_type = getResources().getIntArray(R.array.config_type);
		cfg_group = getResources().getIntArray(R.array.config_group);
		
		for (int i=0;i<cfg_name.length;i++)	// die anzuzeigende CFG-Gruppe wird zusammengesucht
    	{
			if (cfg_group[i] == cfg_selected_group)
			{
				CFG_Elements.add(new CfgElement(cfg_name[i], cfg_type[i], cfg_group[i], i, cfg_description[i]));
			}
    	}
		
		cfga=new CfgAdapter(getActivity(), android.R.layout.simple_list_item_1, CFG_Elements);
		this.setListAdapter(cfga);
		
		fcontainer = container;
		layoutInflater = inflater;
		fragview = super.onCreateView(inflater, container, savedInstanceState);
		return fragview;
	
    }	// end onCreateView
	
	/*
	@Override
	public void onResume() {
		super.onResume();
				
	} */

	
	/*
	@Override
	public void onPause()
	{
		super.onPause();
	}
	*/
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);	// muss als ersts aufgerufen werden!!
	 
	  savedInstanceState.putInt("cfg_group", cfg_selected_group);	// die darzustellende Config-Gruppe

	}
	
	
	// für ListView-Einträge außer on/off Typen
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		
		CfgElement e = (CfgElement) v.getTag();
		if (e != null)
		{
			switch (e.ID) {	// Spezialaktionen ausfiltern. default: Konfigurationsdialog starten

			case CFGD_STARTGUESTMODE:
				Basis.setUsermode(Basis.USERMODE_GUEST);
				CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_USERMODE, null);	// Anzeige auf Gästemodus umorganisieren
				break;

			default:
				ConfigDialogFragment dialog = new ConfigDialogFragment();
				Bundle args = new Bundle();
				args.putInt("type", e.Type);
				args.putInt("id", e.ID);
				args.putString("name", e.Name);
				args.putString("desc", e.Description);
				dialog.setArguments(args);
				dialog.show(getFragmentManager(), "cfgdialog");
				break;
			}
		}
	}
	
	
	public void updateList()	// Callback zum Aktualisieren des ListView für DialogFragment
	{
		if (cfga != null) { cfga.notifyDataSetChanged(); }
	}
	
	
	// für Adapter mit verschiedenden view-Typen und convertView: http://android.amberfog.com/?p=296
	
	public class CfgAdapter extends ArrayAdapter<CfgElement> implements OnCheckedChangeListener {
		 
		final ArrayList<CfgElement> cfglist;
		
	        public CfgAdapter(Context context, int textViewResourceId, ArrayList<CfgElement> elements) {
	            super(context, textViewResourceId, elements);
	            cfglist = elements;
	        }
	        
	        @Override
	        public int getItemViewType(int position) {
	        	// es werden nur 2 View-Typen unterschieden: 0: mit checkBox, 1: ohne CheckBox
	        	// alle anderen Typen werden erst im dialog benötigt!!
	        	
	        	int echter_typ;
	        	
	        	switch (cfglist.get(position).Type) {
	        	
	        	case Frag_cfg_level1.CFG_ONOFF:
	        		echter_typ = 0;
	        		break;
	        		
	        	default:
	        		echter_typ = 1;
	        		break;
	        	}
	        	
	        	return echter_typ;
	        }
	 
	        @Override
	        public int getViewTypeCount() {
	            return Frag_cfg_level1.CFG_VIEWTYPE_COUNT;
	        }

	        
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {

	        	int type = getItemViewType(position);
	        	CfgElement e = cfglist.get(position);
	        	String[] dataitems;	// für benötigte Werte-Text-Arrays
	        	String text;
	        	
	        	// if (convertView==null) { // ListView wird bei Dialog Edit durcheinandergeworfen, werte nicht aktualisiert
	        	// wenn View-Recycling aktiveert wird - da ist noch ein Fehler drin! checken!!!!
	        	
	        		switch (type) {
	        		
	        		case 0:	// CFG_ONOFF
	        			
	        			convertView=layoutInflater.inflate(R.layout.config_l2_row_2, parent, false);
	        			CheckBox checkBox_cfg_l2 = (CheckBox)convertView.findViewById(R.id.checkBox_cfg_l2_check);
	        			TextView textView_cfg_l2_name2 = (TextView)convertView.findViewById(R.id.textView_cfg_l2_check_name);
	        			TextView textView_cfg_l2_description2 = (TextView)convertView.findViewById(R.id.textView_cfg_l2_description2);
	        			textView_cfg_l2_name2.setText(cfglist.get(position).Name);
	        			textView_cfg_l2_description2.setText(cfglist.get(position).Description);
	        			checkBox_cfg_l2.setChecked(getCheckBoxState(cfglist.get(position).ID));	// darzustellenden Einstellungswert ermitteln
	        			checkBox_cfg_l2.setTag(e);	// CfgElement in Tag mitgeben
	        			checkBox_cfg_l2.setOnCheckedChangeListener(this);
	        			break;
	        			
	        		default:	// alle anderen
	        			convertView=layoutInflater.inflate(R.layout.config_l2_row_std, parent, false);
	        			TextView textView_cfg_l2_name1 = (TextView)convertView.findViewById(R.id.textView_lokdata_name);
	        			TextView textView_cfg_l2_description1 = (TextView)convertView.findViewById(R.id.textView_lokdata_value);
	        			textView_cfg_l2_name1.setText(cfglist.get(position).Name);
	        			textView_cfg_l2_description1.setText(cfglist.get(position).Description);
	        			
	        			switch (cfglist.get(position).ID) {	// bei diversen Einstellungen: derzeitige Werte anzeigen
	        			
	        			case CFGD_RANGIERSPEED:
	        				textView_cfg_l2_description1.setText(String.valueOf(Basis.getRangierMax()));
	        				break;
	        				
	        			case CFGD_FONT_SCALE:
	        				// in % anzeigen
	        				textView_cfg_l2_description1.setText(String.valueOf((int)(Basis.getFontScale()*100)) + "%");
	        				break;
	        				
	        			case CFGD_SPEEDSTUFEN:
	        				textView_cfg_l2_description1.setText(String.valueOf(Basis.getSpeedStufen()));
	        				break;
	        			
	        			case CFGD_MANUAL_IP:
	        				textView_cfg_l2_description1.setText(Basis.getManuelleIP());
	        				break;
	        				
	        			case CFGD_TCP_PORT:
	        				textView_cfg_l2_description1.setText(String.valueOf(Basis.getTcpPort()));
	        				break;
	        				
	        			case CFGD_TCPSERVER_PORT:
	        				textView_cfg_l2_description1.setText(String.valueOf(Basis.getServerTcpPort()));
	        				break;
	        				
	        			case CFGD_UDP_PORT:
	        				textView_cfg_l2_description1.setText(String.valueOf(Basis.getUdpPort()));
	        				break;
	        				
	        			case CFGD_NO_SCREENSAVER:
	        				dataitems = getResources().getStringArray(R.array.wakelock_type);
	        				textView_cfg_l2_description1.setText(dataitems[Basis.getWakelockmode()]);
	        				break;
	        				
	        			case CFGD_RUNMODE:
	        				dataitems = getResources().getStringArray(R.array.runmode_types);
	        				int runmode = Basis.getRunmode().ordinal();
	        				if (runmode < 2) { textView_cfg_l2_description1.setText(dataitems[runmode]); }	// nur standard und Test anzeigen (Master nicht!)
	        				break;

	        			case CFGD_THEME:
	        				dataitems = getResources().getStringArray(R.array.theme_types);
	        				textView_cfg_l2_description1.setText(dataitems[Basis.getThemeType()]);
	        				break;
	        				
	        			case CFGD_LAYOUTTYPE:
	        				dataitems = getResources().getStringArray(R.array.layout_types);
	        				textView_cfg_l2_description1.setText(dataitems[Basis.getLayouttype()]);
	        				break;
	        				
	        			case CFGD_LAYOUT_DIV_MODE:
	        				dataitems = getResources().getStringArray(R.array.layout_div_mode);
	        				textView_cfg_l2_description1.setText(dataitems[Basis.getForceDisplaymode()]);
	        				break;
	        				
	        			case CFGD_USED_NETWORK:
	        				textView_cfg_l2_description1.setText(Basis.getUseNetworkName());
	        				break;
	        				
	        			case CFGD_PROGUPDATE:
	        				dataitems = getResources().getStringArray(R.array.cfg_check_update);
	        				textView_cfg_l2_description1.setText(dataitems[Basis.getUpdateAllowed()]);
	        				break;
	        				
	        			case CFGD_ENDGUESTMODE:
	        				dataitems = getResources().getStringArray(R.array.cfg_guestmode_end);
	        				int gindex = 0;
	        				if (Basis.isUsermode_usepwd()) {gindex = 1; }
	        				textView_cfg_l2_description1.setText(dataitems[gindex]);
	        				break;
	        				
	        			case CFGD_LOKSTOPMODE:
							int data = Basis.getLokstopmode();
							text = "";
							dataitems = getResources().getStringArray(R.array.cfg_lokstopmodes);
							
							for (int a=0;a<dataitems.length;a++)
							{
								int flag = (int)Math.pow(2, a);
								if  ((data & flag) == flag) { text += dataitems[a] + "\r\n"; }	// TODO ändern auf Stringbuffer oä statt +=
							}
							textView_cfg_l2_description1.setText(text);
							break;
	        				
	        			}
	        			
	        			break;
	        		}

	        //	} // recyclen - hier deaktivieren
	       
	            convertView.setTag(e);	// CfgElement in Tag mitgeben
	       
	        	return convertView; 
	        }

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if (buttonView.getTag() != null)
				{
					CfgElement e = (CfgElement)buttonView.getTag();
					setCheckBoxState(e.ID, buttonView.isChecked());
				}
			}
	        
	}

	// -----------------------
	
	// Adapter für Dialog mit Check-Liste (zB. LOKSTOPMODE)
	
	public class CheckAdapter extends ArrayAdapter<String> implements OnCheckedChangeListener {
		 
		String[] checklist;
		//ArrayList<String> checklist;
		int cfg_data_id;
		
	        public CheckAdapter(Context context, int textViewResourceId, String[] elements, int cfgID) {
	            super(context, textViewResourceId, elements);
	            //checklist = getResources().getStringArray(R.array.cfg_lokstopmodes);
	            checklist = elements;
	            cfg_data_id = cfgID;	// die CFG Daten ID (siehe Definitionen ganz oben), damit das Ergebnis zugeordnet werden kann
	        }
	        
	        @Override
	        public int getItemViewType(int position) {
	        	// es gibt nur einen Typen -> 1
	        	return 1;
	        }
	 
	        @Override
	        public int getViewTypeCount() {
	            return 1;
	        }

	        
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {

	        	//int type = getItemViewType(position);
	        	if (convertView==null) { convertView=layoutInflater.inflate(R.layout.config_l2_checkrow, parent, false); }

	        	CheckBox checkBox_cfg_l2_check = (CheckBox)convertView.findViewById(R.id.checkBox_cfg_l2_check);
	        	TextView textView_cfg_l2_check_name = (TextView)convertView.findViewById(R.id.textView_cfg_l2_check_name);
	        	textView_cfg_l2_check_name.setText(checklist[position]);
	        	checkBox_cfg_l2_check.setTag(position);
	        	checkBox_cfg_l2_check.setOnCheckedChangeListener(this);
	        	
	        	boolean value = false;
	        	
	        	switch(cfg_data_id)	// darzustellenden Einstellungswert ermitteln
				{
					case CFGD_LOKSTOPMODE:
						
						int data = Basis.getLokstopmode();
						int flag = (int)Math.pow(2, position);
						value = ((data & flag) == flag);
						break;
				}
	        	
	        	checkBox_cfg_l2_check.setChecked(value);	
	        	
	        	return convertView; 
	        }

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				int index;
				int data = Basis.getLokstopmode();
				
				if (buttonView.getTag() != null)
				{
					index = (int)buttonView.getTag();
					
					
					if (isChecked) { data |= (int) Math.pow(2, index);  }	// bit setzen
					else { data &= ~(int) Math.pow(2, index); }
					
					Basis.setLokstopmode(data);
				}
			}
	        
	}
	
	

	// Dialoge
	
	
	@SuppressLint("ValidFragment")
	public class ConfigDialogFragment extends DialogFragment {
		
		CfgElement e;	// zum Zwischenspeichern des aktuellen CFG-Elements
		Fragment thisFragment;
		FragmentManager fragmentManager;

		public ConfigDialogFragment() {
	        // Empty constructor required for DialogFragment
	    }
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			fragmentManager = getFragmentManager();
			thisFragment = this;
			setRetainInstance(true);	// TODO: setRetainInstance: auswirkung checken / sichern //wg. Problemen beim Umstellen auf Single/dual/multiview vorerst lassen (error beim recreaten des dialogfragments!)
			final Dialog dialog = new Dialog(getActivity());
			Bundle dat = getArguments();
			e = new CfgElement(dat.getString("name"), dat.getInt("type"), 0, dat.getInt("id"), dat.getString("desc") );

			switch(e.Type) {

			case Frag_cfg_level1.CFG_STRING: case Frag_cfg_level1.CFG_IP: case Frag_cfg_level1.CFG_INT_FROMSTR:

				dialog.setContentView(R.layout.dialog_cfg_string);
				dialog.setTitle(e.Name); 
				editText_dialog_cfg_str_data = (EditText) dialog.findViewById(R.id.editText_dialog_cfg_str_data);
				editText_dialog_cfg_str_data.setText(getStringData(e.ID));
				textView_dialog_cfg_str = (TextView) dialog.findViewById(R.id.textView_dialog_cfg_str);
				textView_dialog_cfg_str.setVisibility(View.GONE);	// vestecken -> ist für Fehlermeldungen gedacht
				dialog.setCancelable(true);

				Button button_dialog_cfg_str_save = (Button) dialog.findViewById(R.id.button_dialog_cfg_str_save);
				button_dialog_cfg_str_save.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						
						String data = editText_dialog_cfg_str_data.getText().toString();
						
						if (e.Type == Frag_cfg_level1.CFG_IP)
						{
							if (!data.matches(IPADDRESS_PATTERN)) 
							{ 
								data = null;
								textView_dialog_cfg_str.setText(R.string.gen_not_ipv4);
								textView_dialog_cfg_str.setVisibility(View.VISIBLE);
							}
							else { textView_dialog_cfg_str.setVisibility(View.GONE); }
						}
						
						if (e.Type == Frag_cfg_level1.CFG_INT_FROMSTR)
						{
							try { int i = Integer.parseInt(data); }
							catch (NumberFormatException e)
							{
								data = null;
								textView_dialog_cfg_str.setText(R.string.gen_not_int);
								textView_dialog_cfg_str.setVisibility(View.VISIBLE);
							}
						}
						
						if (data != null) // nur speichern und Dialog beenden, wenn Daten gültig sind
						{ 
							setStringData(e.ID, data);
							Fragment f = fragmentManager.findFragmentByTag("CFG_LEVEL2");
							if (f != null) { ((Frag_cfg_level2) f).updateList(); }
							dialog.dismiss();
						}
						
					}
				});

				Button button_dialog_cfg_str_cancel = (Button) dialog.findViewById(R.id.button_dialog_cfg_str_cancel);
				button_dialog_cfg_str_cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});

				break;

			case Frag_cfg_level1.CFG_INT_SEEK:
				dialog.setContentView(R.layout.dialog_cfg_int_seek);
				dialog.setTitle(e.Name); 

				textView_dialog_cfg_intseek = (TextView) dialog.findViewById(R.id.textView_dialog_cfg_intseek);
				textView_dialog_cfg_intseek2 = (TextView) dialog.findViewById(R.id.textView_dialog_cfg_intseek2);
				textView_dialog_cfg_intseek2.setVisibility(View.GONE);
				final SeekBar seekBar_dialog_cfg_intseek = (SeekBar) dialog.findViewById(R.id.seekBar_dialog_cfg_intseek);
				
				switch(e.ID) 
				{	// SpeedBar initialisieren

				case CFGD_RANGIERSPEED:
					int value = Basis.getRangierMax();
					seekBar_dialog_cfg_intseek.setMax(Basis.getSpeedStufen() - 1);
					seekBar_dialog_cfg_intseek.setProgress(value);
					textView_dialog_cfg_intseek.setText(String.valueOf(value));
					break;
					
				case CFGD_FONT_SCALE:
					textView_dialog_cfg_intseek2.setVisibility(View.VISIBLE);
					// in der Suchleiste wird mit Werten von 0 bis 225 gearbeitet
					// was +25 dann int Werte von 25 bis 250 ergibt -> die Prozentzahl
					int fvalue = (int)(Basis.getFontScale()*100) -25;
					if (fvalue < 0) { fvalue = 0; }
					seekBar_dialog_cfg_intseek.setMax(225);
					seekBar_dialog_cfg_intseek.setProgress(fvalue);
					textView_dialog_cfg_intseek.setText(String.valueOf(fvalue+25) + "%");
					break;
	
				}
				
				seekBar_dialog_cfg_intseek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

						switch(e.ID) 
						{	

						case CFGD_RANGIERSPEED:
							textView_dialog_cfg_intseek.setText(String.valueOf(progress));
							break;

						case CFGD_FONT_SCALE:
							progress += 25;	// Wert soll nicht bei 0%, sonder bei 25% beginnen
							float fsize = getResources().getDimension(R.dimen.font_size_med);	// Achtung: der gelieferte Wert hat die aktuelle FontScale bereits eingerechnet 
							float oldFontScale = Basis.getFontScale();
							// die derzeit verwendete FontScale aus der Berechnung heraushalten!!
							float newscale = (float)progress / 100f / oldFontScale / Basis.getDisplayDensity();						
							fsize = fsize * newscale;
							textView_dialog_cfg_intseek.setText(String.valueOf(progress) + "%");
							textView_dialog_cfg_intseek2.setTextSize(TypedValue.COMPLEX_UNIT_SP, fsize);
							//TODO: error: wird zu groß angezeigt, sobald man sich von 100% wegbewegt (danach auch bei 100%)
							break;

						}


					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {}
	
				});
				
				dialog.setCancelable(true);

				Button button_dialog_cfg_intseek_save = (Button) dialog.findViewById(R.id.button_dialog_cfg_intseek_save);
				button_dialog_cfg_intseek_save.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						
						int data = seekBar_dialog_cfg_intseek.getProgress();
						
						if (e.ID == CFGD_RANGIERSPEED)	{ if (data < 10) { data = -1; } }
						
						if (e.ID == CFGD_FONT_SCALE)	{ data = data + 25; }	// Minimalwert 0 bedeutet 25%, Maximalwert 225 bedeutet 250%, daher +25
						
						if (data != -1) // nur speichern und Dialog beenden, wenn Daten gültig sind
						{ 
							setIntData(e.ID, data);
							Fragment f = fragmentManager.findFragmentByTag("CFG_LEVEL2");
							if (f != null) { ((Frag_cfg_level2) f).updateList(); }
							dialog.dismiss();
						}
					}
				});

				Button button_dialog_cfg_intseek_cancel = (Button) dialog.findViewById(R.id.button_dialog_cfg_intseek_cancel);
				button_dialog_cfg_intseek_cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});

				break;
				
			case Frag_cfg_level1.CFG_SPINNER:

				dialog.setContentView(R.layout.dialog_cfg_list);
				dialog.setTitle(e.Name);
				String[] items = null;
				listView_dialog_cfg_list = (ListView) dialog.findViewById(R.id.listView_dialog_cfg_list);
				
				int selectedposition = -1;
				
				switch(e.ID)
				{
					case CFGD_LAYOUTTYPE:
						items = getResources().getStringArray(R.array.layout_types);
						selectedposition = getIntData(e.ID);
						break;
						
					case CFGD_LAYOUT_DIV_MODE:
						items = getResources().getStringArray(R.array.layout_div_mode);
						selectedposition = getIntData(e.ID);
						break;
						
					case CFGD_NO_SCREENSAVER:
						items = getResources().getStringArray(R.array.wakelock_type);
						String ssaver = getString(R.string.cfg_screensaver) + ": ";
						for (int i=0;i<items.length;i++)	// Auswahltext zusammenstellen "Bildschirmschoner: *Modus*
				    	{
							items[i] = ssaver + items[i];
				    	}
						selectedposition = getIntData(e.ID);
						break;
						
					case CFGD_SPEEDSTUFEN:
						int[] intitems =  getResources().getIntArray(R.array.speedsteps);
						items = new String[intitems.length];
						
						for (int i=0;i<intitems.length;i++)
				    	{
							items[i] = String.valueOf(intitems[i]);
				    	}
						break;
						
					case CFGD_RUNMODE:
						items =  getResources().getStringArray(R.array.runmode_types);
						selectedposition = getIntData(e.ID);
						break;

					case CFGD_THEME:
						items =  getResources().getStringArray(R.array.theme_types);
						selectedposition = getIntData(e.ID);
						break;
						
					case CFGD_USED_NETWORK:
						int i = 0;
						items = new String[Basis.getExistingNetworks().size()];
						
						for (WBnetwork n : Basis.getExistingNetworks())
						{
							items[i++] = n.getTypName() + " ("+ n.getState() + ")";
						}
						break;
						
					case CFGD_PROGUPDATE:
						items =  getResources().getStringArray(R.array.cfg_check_update);
						selectedposition = getIntData(e.ID);
						break;
						
					case CFGD_ENDGUESTMODE:
						items =  getResources().getStringArray(R.array.cfg_guestmode_end);
						selectedposition = getIntData(e.ID);
						break;
							
				}
				
				ArrayAdapter<String> aa = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, items);
				listView_dialog_cfg_list.setAdapter(aa);
				listView_dialog_cfg_list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				
				if (selectedposition >= 0) { listView_dialog_cfg_list.setItemChecked(selectedposition, true); }		// derzeitigen Wert markieren 
				
				dialog.setCancelable(true);
				listView_dialog_cfg_list.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

						setIntData(e.ID, position);
						Fragment f = fragmentManager.findFragmentByTag("CFG_LEVEL2");
						if (f != null) { ((Frag_cfg_level2) f).updateList(); }
						dialog.dismiss();
					}
				});
				listView_dialog_cfg_list.setSelection(getIntData(e.ID));	// derzeitige Einstellung selektieren // funktioniert nicht

				button_dialog_cfg_list_cancel = (Button) dialog.findViewById(R.id.button_dialog_cfg_list_cancel);
				button_dialog_cfg_list_cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});

				break;
				
				
			case Frag_cfg_level1.CFG_MULTI_CHECK:

				dialog.setContentView(R.layout.dialog_cfg_list);
				dialog.setTitle(e.Name);
				String[] checkitems = null;
				
				listView_dialog_cfg_list = (ListView) dialog.findViewById(R.id.listView_dialog_cfg_list);
				
				switch(e.ID)
				{
					case CFGD_LOKSTOPMODE:
						checkitems = getResources().getStringArray(R.array.cfg_lokstopmodes);
						break;
				}
				
				ArrayAdapter<String> ca = new CheckAdapter(getActivity(), R.layout.config_l2_checkrow, checkitems, e.ID);
				listView_dialog_cfg_list.setAdapter(ca);
				dialog.setCancelable(true);

				listView_dialog_cfg_list.setSelection(getIntData(e.ID));	// derzeitige Einstellung selektieren // funktioniert nicht

				button_dialog_cfg_list_cancel = (Button) dialog.findViewById(R.id.button_dialog_cfg_list_cancel);
				button_dialog_cfg_list_cancel.setText(R.string.gen_ready);

				button_dialog_cfg_list_cancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment f = fragmentManager.findFragmentByTag("CFG_LEVEL2");
						if (f != null) { ((Frag_cfg_level2) f).updateList(); }
						dialog.dismiss();
					}
				});

				break;
				
			}

			return dialog;
		}
		
		@Override	// wg. Problemen bei ScreenRotation
		public void onDestroyView() {
		  if (getDialog() != null && getRetainInstance())
		    getDialog().setOnDismissListener(null);
		  super.onDestroyView();
		}

	}	// end dialoge


	
	
	class CfgElement {
		
		String Name;	// Name der angezeigt werden soll
		int Type;		// Widget-Typ - siehe: Frag_cfg_level1.java: CFG-(Daten)-Typen für ListView
		int Group;		// Index der Gruppe im Array config_level1 
		int ID;			// index des Elements in den Arrays config_name, config_type, config_group als eindeutige Kennung
		String Description;	// Beschreiungstext
		
		CfgElement(String n, int t, int g, int i, String d)
		{
			Name = n;
			Type = t;
			Group = g;
			ID = i;
			Description = d;
		}
		
	}
	
	
	// Config-Wert laden - für Checkbox/Boolean Typen
	Boolean getCheckBoxState(int idnr)
	{
		Boolean ergebnis = false;
		
		switch (idnr) {
		
		case CFGD_SERVER_VERWENDEN:
			ergebnis = Basis.getUseServer();
			break;
			
		case CFGD_UDP_VERWENDEN:
			ergebnis = Basis.Use_udp();
			break;
		}
		
		return ergebnis;
	}
	
	void setCheckBoxState(int idnr, Boolean data)
	{
		switch (idnr) {
		
		case CFGD_SERVER_VERWENDEN:
			Basis.setUseServer(data);
			break;
			
		case CFGD_UDP_VERWENDEN:
			Basis.setUse_udp(data);
			break;
			
		}
	}
	
	// Config-Wert laden - für String Typen
	String getStringData(int idnr)
	{
		String ergebnis = "";
		
		switch (idnr) {
		
		case CFGD_MANUAL_IP:
			ergebnis = Basis.getManuelleIP();
			break;
			
		case CFGD_TCP_PORT:
			ergebnis = String.valueOf(Basis.getTcpPort());
			break;
			
		case CFGD_TCPSERVER_PORT:
			ergebnis = String.valueOf(Basis.getServerTcpPort());
			break;
			
		case CFGD_UDP_PORT:
			ergebnis = String.valueOf(Basis.getUdpPort());
			break;

		}
		
		return ergebnis;
	}
	
	void setStringData(int idnr, String data)
	{
switch (idnr) {
		
		case CFGD_MANUAL_IP:
			Basis.setManuelleIP(data);
			break;
			
		case CFGD_TCPSERVER_PORT:
			Basis.setServerTcpPort(Integer.parseInt(data));
			break;
			
		case CFGD_TCP_PORT:
			Basis.setTcpPort(Integer.parseInt(data));
			break;
			
		case CFGD_UDP_PORT:
			Basis.setUdpPort(Integer.parseInt(data));
			break;
			
		case CFGD_GUESTMODE_PWD:
			Basis.setUsermodepwd(data);
			break;
			
		}
	}
	
	
	// Config-Wert laden - für int Typen
		int getIntData(int idnr)
		{
			int ergebnis = 0;
			
			switch (idnr) {
			
			case CFGD_SPEEDSTUFEN:
				ergebnis = Basis.getSpeedStufen();
				break;
				
			case CFGD_RANGIERSPEED:
				ergebnis = Basis.getRangierMax();
				break;
				
			case CFGD_LAYOUTTYPE:
				ergebnis = Basis.getLayouttype();
				break;
				
			case CFGD_LAYOUT_DIV_MODE:
				ergebnis = Basis.getForceDisplaymode();
				break;
				
			case CFGD_NO_SCREENSAVER:
				ergebnis = Basis.getWakelockmode();
				break;
				
			case CFGD_RUNMODE:
				ergebnis = Basis.getRunmode().ordinal();
				break;
				
			case CFGD_THEME:
				ergebnis = Basis.getThemeType();
				break;
				
			case CFGD_PROGUPDATE:
				ergebnis = Basis.getUpdateAllowed();
				break;
				
			case CFGD_ENDGUESTMODE:
				ergebnis = 0;
				if (Basis.isUsermode_usepwd()) {ergebnis = 1; }
				break;
				
			case CFGD_LOKSTOPMODE:
				ergebnis = Basis.getLokstopmode();
				
			}
			
			return ergebnis;
		}
		
		void setIntData(int idnr, int data)
		{
	switch (idnr) {
			
		case CFGD_SPEEDSTUFEN:	//TODO: LocalBroadcast, hier nur Defaultwert, muss beim device eingestellt werden -> dort dann LocalBroadcast
				int[] items = getResources().getIntArray(R.array.speedsteps);
				if (data <= items.length) 
				{
					Basis.setSpeedStufen(items[data]);

                    /*
					Handler uihandler = Basis.getUIhandler();
	            	if (uihandler != null) 
	        		{
	        			Message msg1 = Message.obtain(uihandler, Frag_control.MSG_SPEEDSTEPS_CHANGED);
	        			uihandler.sendMessage(msg1);
	        		} */

                    Intent dcIntent = new Intent(Basis.ACTION_SPEEDSTEPS_CHANGED);
                    Basis.getLocBcManager().sendBroadcast(dcIntent);


				}
				break;
				
			case CFGD_RANGIERSPEED:
				Basis.setRangierMax(data);
				break;
				
			case CFGD_LAYOUTTYPE:
				Basis.setLayouttype(data);
				CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_LAYOUT, getcfgGroupBundle(true));	//Änderung sofort durchführen
				break;
				
			case CFGD_LAYOUT_DIV_MODE:
				Basis.setForceDisplaymode(data);
				CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_LAYOUT, getcfgGroupBundle(true));	//Änderung sofort durchführen
				break;
				
			case CFGD_NO_SCREENSAVER:
				Basis.setWakelockmode(data, getActivity().getWindow());
				break;
				
			case CFGD_FONT_SCALE:
				float value = (float)data / 100f;
				Basis.setFontScale(value);
				CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_FONTSCALE, getcfgGroupBundle(false));	// Activity neu starten, damit Änderung in Kraft tritt
				break;
				
			case CFGD_RUNMODE:
				runmodetype runmode;
				if (data == 0) { runmode = runmodetype.standard;}
				else { runmode = runmodetype.test; }
				Basis.setRunmode(runmode);
				CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_NAVIDRAWER, getcfgGroupBundle(false));	// navication drawer commands neu aufbauen
				break;
				
			case CFGD_THEME:
				Basis.setThemeType(data);
				CfgChangedListener.OnConfigChanged(FAct_control.CID_CHANGE_THEME, getcfgGroupBundle(false));	// Theme gleich aktivieren
				break;
				
			case CFGD_USED_NETWORK:	// data = index des networks im ArrayList<WBnetwork> existingNetworks
				Basis.setUseNetwork(Basis.getExistingNetworks().get(data).getTyp());	// Netzwerk wird dabei umgestellt!!
				break;
				
			case CFGD_PROGUPDATE:
				Basis.setUpdateAllowed(data);
				break;
				
			case CFGD_ENDGUESTMODE:
				if (data == 0) { Basis.setUsermode_usepwd(false); }
				else { Basis.setUsermode_usepwd(true); }
				break;
				
			case CFGD_LOKSTOPMODE:
				Basis.setLokstopmode(data);
				break;

			}
	
	
	
		}
		
		
		
		void setFloatData(int idnr, float data)
		{
			switch (idnr) {

			/* wird als int gemacht
			case CFGD_FONT_SCALE:
				Basis.setFontScale(data);
				break;
				*/

			}
		}
		
		float getFloatData(int idnr)
		{
			float ergebnis = 0;
			
			switch (idnr) {
			
			case CFGD_FONT_SCALE:
				ergebnis = Basis.getFontScale();
				break;

			}
			
			return ergebnis;
		}
		
		Bundle getcfgGroupBundle(boolean fragcleanup)
		{
			Bundle data = new Bundle();
			data.putInt("cfggroup", cfg_selected_group);
			if (fragcleanup) { data.putBoolean("fragcleanup", true); }	// sollen die fragments des 2. containers vor dem Neustart entfernt werden? bei Umschalten auf singleview ist das notwendig!!!! 
			return data;
		}

}	// end class Frag_cfg_net
