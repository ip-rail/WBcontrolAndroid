package wb.control.fragments;

import java.util.ArrayList;

import wb.control.ActionElement;
import wb.control.Basis;
import wb.control.Device;
import wb.control.OnActionBarConfigListener;
import wb.control.OnActionBarItemListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_ActionSettings;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.GridLayout.Spec;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class Frag_action2 extends Fragment
implements View.OnClickListener, OnActionBarItemListener, WBFragID {

	private static final int FRAGMENT_ID = FAct_control.FRAGMENT_ACTION;

	private View fragview;	// Root-View für das Fragment
	OnFragReplaceListener fragReplListener;
	OnActionBarConfigListener aBarConfigListener;
	private GridLayout mGridLayout;
	//private GridLayout.LayoutParams mGridLayoutParams;
	private ArrayList<ActionElement> aelist;
	private Button button_action2_change_settings;
	private View menuTargetView; // speichert den View, von dem aus das aktuelle ContextMenu gestartet wurde
	BroadcastReceiver locBcReceiver;
	IntentFilter ifilter;
	
	private static final int BUTTON_CHANGE_SETTINGS_ID = 	50000;	// muss im Fragment unique sein!


    public int getFragmentID() { return FRAGMENT_ID; }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Empfang für lokales Broadcasting einrichten - für ActionElement-Aktualisierung (Typ Data)
		ifilter = new IntentFilter();
		//ifilter.addAction(Basis.ACTION_UPDATE_AE);
		ifilter.addAction(Basis.ACTION_UPDATE_AE_DATA);
		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

				if (intent.getAction().equals(Basis.ACTION_UPDATE_AE_DATA)) {
					//Extra: "datatype" (int)(siehe ae.datatype)  und "device" (String) Devicename (siehe ae.scopedata)
					int datatype = intent.getIntExtra("datatype", -1);
					String devname = intent.getStringExtra("device");

					for (ActionElement ae : aelist) {
						if ((ae.datatype == datatype) && (ae.scopedata.equals(devname))) {
							 //updateAE(ae);
                            ae.update(mGridLayout);
						}
					}
				}
			}
		};

	}    // end onCreate
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //container.removeAllViews(); //undo	// TODO: Fragment: container.removeAllViews()
		
		aelist = Basis.getActionElementListByLocation(0); // nur die AEs für den Action-Bereich holen (die für's Ctrl weglassen)
		if (aelist == null) { aelist = new ArrayList<ActionElement>(); }
				
		fragview = inflater.inflate(R.layout.f_action2, container, false);
	
		CreateGrid();

		return fragview;

	}	// end onCreateView


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
    public void onDestroyView() {


        super.onDestroyView();
    } */


    @Override
	public void onResume() {
		super.onResume();

        fresume();
		
	}	// End onResume

	
	@Override
	public void onPause() {

        fpause();
		super.onPause();
		
	} // end onPause


    private void fresume() {

        if (Basis.USERMODE_GUEST != Basis.getUsermode()) {
            aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_ADD, true);	// ActionBar Button "add Item" anzeigen, aber nicht im USERMODE_GUEST
        }

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locBcReceiver, ifilter);	// localBraodcast-Empfang aktivieren (für AE Data-Aktualisierung)
    }


    private void fpause() {

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locBcReceiver);	// localBraodcast-Empfang stoppen
        aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_ADD, false);	// ActionBar Button "add Item" verstecken
    }




	// Action-ContextMenü
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		//super.onCreateContextMenu(menu, v, menuInfo);

		if (Basis.getUsermode() == Basis.USERMODE_GUEST) { return; }	// im Guestmode nicht erlaubt
		menuTargetView = v;
		menu.clear();	// Menu leeren!!
		MenuInflater inflater = new MenuInflater(getActivity().getApplicationContext());
		inflater.inflate(R.menu.menu_action_context, menu);
	}


	public void onContextMenuClosed (Menu menu)
	{
		menuTargetView = null;
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch(item.getItemId())
		{

		case R.id.menui_act_del:

			if (menuTargetView != null)
			{ 
				menuTargetView.setVisibility(View.GONE);
				ActionElement ae = (ActionElement)menuTargetView.getTag();
				aelist.remove(ae);
				Basis.getActionElementList().remove(ae);	// in der Haupt-Liste auch entfernen!!!
				menuTargetView.setOnClickListener(null);
				mGridLayout.removeView(menuTargetView);
			}
			return true;


		case R.id.menui_act_edit:	// ausgewähltes Widget finden und editieren

			if (menuTargetView != null)
			{ 
				if (menuTargetView.getTag() != null)
				{
					ActionElement aetag = (ActionElement)menuTargetView.getTag();	// aetag ist das *echte* ae
					int aeindex = Basis.getActionElementList().indexOf(aetag);
					//ActionElement aetemp = new ActionElement();	// nur für die Bearbeitung, damit man Änderungen wieder verwerfen kann -> editierbare properties kopieren!
					//Basis.setAe_toedit(aetemp);
					((FAct_control) getActivity()).setAe_toedit(aeindex);

					fragReplListener.OnFragReplace(FAct_control.FRAGMENT_ACTION_EDIT, false, null);	// Bundle wird erst bei Antwort vom FRAGMENT_ACTION_EDIT benötigt.
					// TODO: ae edit jetzt mit dialog machen
					return true;
				}
				else { return false; }
			}
			break;
		}
		return super.onContextItemSelected(item);

	}	// end  onContextItemSelected


	
	
	public void CreateGrid()	// ActionGridLayout aufbauen
	{
		
		LinearLayout ll = (LinearLayout) fragview;
		
		if (mGridLayout != null)
		{
			mGridLayout.removeAllViews();
		}
		ll.removeAllViews();
		
		mGridLayout = new GridLayout(getActivity());		
		mGridLayout.setColumnCount(Basis.getAction2_cols());
		mGridLayout.setRowCount(Basis.getAction2_rows());
		ll.addView(mGridLayout);
		
		// add "change settings" button	
		button_action2_change_settings = new Button(getActivity());
		button_action2_change_settings.setText(R.string.action2_change_settings);
		button_action2_change_settings.setOnClickListener(this);
		button_action2_change_settings.setId(BUTTON_CHANGE_SETTINGS_ID); 	// Achtung: andere Buttons haben aelist-index als id -> hier größeren Wert verwenden!!
		mGridLayout.addView(button_action2_change_settings);
		
		for (ActionElement ae : aelist)
		{
			mGridLayout.addView(ae.createView(mGridLayout, this));
			//if (aelist.indexOf(ae) % 2 == 0) { ae.view.setBackgroundColor(Color.DKGRAY); }
			//else { ae.view.setBackgroundColor(Color.GRAY); }
		} 
	}
	
	
	@Override
	public void onClick(View v) {
		
		switch(v.getId())
		{
			case BUTTON_CHANGE_SETTINGS_ID:	// Settings von Action2 ändern
				
				DialogFrag_ActionSettings rfrag = new DialogFrag_ActionSettings();
				rfrag.show(getFragmentManager(), "actionsettingsdialog");
				break;
		}
	}
	
	//----------------------------------------------------------------
	// obsolet -> jetzt in ActionElement: createView(ViewGroup container, Fragment f)
	/*
		private View create_Actionelement(ActionElement ae) {
			
			//AEtag tag = new AEtag(ae.macro, ae.typ);
			View aeview = null;
			//GridView.LayoutParams layoutparam =  new GridView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

			// Position und Breite/Höhe im Raster auslesen und im LayoutParams definieren
			int rowsize = 1;
			if (ae.width >0) { rowsize = ae.width; }	// # rows wide
			int colsize = 1;
			if (ae.height >0) { rowsize = ae.height; }	// # columns high
			int row = ae.posX;
			int col = ae.posY;
			if (row == -1) { row = GridLayout.UNDEFINED; }	// Row position
			if (col == -1) { col = GridLayout.UNDEFINED; }	// column position
			Spec rowspec = GridLayout.spec(row, rowsize);	
			Spec colspec = GridLayout.spec(col, colsize);	
			GridLayout.LayoutParams layoutparam = new GridLayout.LayoutParams(rowspec, colspec);
			
			//layoutparam.setMargins(R.dimen.space_std_w, R.dimen.space_std_h, R.dimen.space_std_w, R.dimen.space_std_h);
			// TODO error: buttons nicht mehr sichtbar, sobald margin gesetzt ist
			final float scale = getActivity().getBaseContext().getResources().getDisplayMetrics().density;
			
			switch (ae.typ) {

			case ActionElement.AE_TYP_TEXT:
				TextView tv = new TextView(getActivity());
				ae.view = tv;
				tv.setText(ae.text);
				//tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
				tv.setTag(ae);	// ae selbst als tag setzen!
				tv.setLayoutParams(layoutparam);
				tv.setTextColor(Color.rgb(200,0,0));
				tv.setId(aelist.indexOf(ae));	// als id den aelist index setzen!!
				registerForContextMenu(tv);
				aeview = tv;
				break;
				
			case ActionElement.AE_TYP_Data:
				TextView tvd = new TextView(getActivity());
				ae.view = tvd;
				tvd.setText(ae.text);
				//tvd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
				tvd.setTag(ae);	// ae selbst als tag setzen!
				tvd.setLayoutParams(layoutparam);
				tvd.setTextColor(Color.rgb(200,0,0));
				tvd.setId(aelist.indexOf(ae));	// als id den alist index setzen!!
				registerForContextMenu(tvd);
				aeview = tvd;
				break;

			case ActionElement.AE_TYP_BUTTON:
				Button b = new Button(getActivity());
				ae.view = b;
				b.setText(ae.text);
				b.setTag(ae);
				
				//b.setGravity(Gravity.FILL_HORIZONTAL);
				
				int pixelsw = (int) (100 * scale + 0.5f);	//dp in pixel konvertieren
				int pixelsh = (int) (50 * scale + 0.5f);	//dp in pixel konvertieren
				
				//int pixelmargin = (int) (R.dimen.space_std_w * scale + 0.5f);	//dp in pixel konvertieren
				layoutparam.width = pixelsw;
				layoutparam.height = pixelsh;
				//layoutparam.topMargin = pixelmargin;
				//layoutparam.bottomMargin = pixelmargin;
				//layoutparam.leftMargin = pixelmargin;
				//layoutparam.rightMargin = pixelmargin;
				
				b.setLayoutParams(layoutparam);
				b.setId(aelist.indexOf(ae));	// als id den alist index setzen!!
				registerForContextMenu(b);
				b.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// do something when the button is clicked
						if (v.getTag() != null)
						{
							ActionElement ae = (ActionElement) v.getTag();
							if (ae.macro != null)
							{
								ae.macro.execute(ae.scope, ae.scopedata);	// Macro ausführen
							}
						}

					} } );
				aeview = b;
				break;
				
			case ActionElement.AE_TYP_ONOFF_BUTTON:
				ToggleButton tb = new ToggleButton(getActivity());
				ae.view = tb;
				tb.setText(ae.text);	// wird vor dem ersten Click angezeigt
				tb.setTextOn(ae.text + " ON");
				tb.setTextOff(ae.text + " OFF");
				tb.setTag(ae);
				tb.setChecked(ae.onoffstate);
				
				int pixelsw2 = (int) (100 * scale + 0.5f);	//dp in pixel konvertieren
				int pixelsh2 = (int) (50 * scale + 0.5f);	//dp in pixel konvertieren
				
				//int pixelmargin2 = (int) (R.dimen.space_std_w * scale + 0.5f);	//dp in pixel konvertieren
				layoutparam.width = pixelsw2;
				layoutparam.height = pixelsh2;
				//layoutparam.topMargin = pixelmargin2;
				//layoutparam.bottomMargin = pixelmargin2;
				//layoutparam.leftMargin = pixelmargin2;
				//layoutparam.rightMargin = pixelmargin2;
				
				tb.setLayoutParams(layoutparam);
				tb.setId(aelist.indexOf(ae));	// als id den alist index setzen!!
				registerForContextMenu(tb);
				tb.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// do something when the button is clicked
						if (v.getTag() != null)
						{
							ActionElement ae = (ActionElement) v.getTag();
							//if (ae.macro != null)
							if (ae.view != null)
							{
								ae.onoffstate = !ae.onoffstate;	// on/off Zustand speichern 
								ToggleButton t = (ToggleButton) ae.view;
								if (t.isChecked()) { if (ae.macro != null) { ae.macro.execute(ae.scope, ae.scopedata); } }	// Macro "on" ausführen
								else { if (ae.macro2 != null) { ae.macro2.execute(ae.scope, ae.scopedata); } }	// Macro "off" ausführen
							}
						}

					} } );
				aeview = tb;
				break;
			}
			
			if (aeview != null) { aeview.requestLayout(); }
			
			return aeview;
		}	// end create_Actionelement
		*/


		@Override
		public void OnActionBarItem(int itemID) {
	
			if (itemID == FAct_control.AB_ITEM_ADD)
			{
				ActionElement aeneu = new ActionElement();
				int aeindex = Basis.AddActionElement(aeneu);
				//Basis.setAe_toedit(aeneu);	// leeres AE mit nummer = 0 bedeutet neu anlegen // wtf: nummer gibt's da keine
				((FAct_control) getActivity()).setAe_toedit(aeindex);	// leeres AE mit nummer = 0 bedeutet neu anlegen 
				fragReplListener.OnFragReplace(FAct_control.FRAGMENT_ACTION_EDIT, false, null);	// Bundle wird erst bei Antwort vom FRAGMENT_ACTION_EDIT benötigt.
			}
		}


	
}	// end class Frag_action2
