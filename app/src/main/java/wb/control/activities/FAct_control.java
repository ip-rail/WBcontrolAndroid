package wb.control.activities;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Basis.runmodetype;
import wb.control.HardKey;
import wb.control.Macro;
import wb.control.OnActionBarConfigListener;
import wb.control.OnActionBarItemListener;
import wb.control.OnActionGridChangeListener;
import wb.control.OnConfigChangedListener;
import wb.control.OnDeviceChangeListener;
import wb.control.OnFragReplaceListener;
import wb.control.OnHardkeyChangedListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.dialogfragments.DialogFrag_Update;
import wb.control.fragments.Frag_StreamTest;
import wb.control.fragments.Frag_TrackPlanTest;
import wb.control.fragments.Frag_VideoTest;
import wb.control.fragments.Frag_action2;
import wb.control.fragments.Frag_action_edit;
import wb.control.fragments.Frag_camcontrol;
import wb.control.fragments.Frag_cfg_level1;
import wb.control.fragments.Frag_cfg_level2;
import wb.control.fragments.Frag_control;
import wb.control.fragments.Frag_hardkeys;
import wb.control.fragments.Frag_log;
import wb.control.fragments.Frag_lokdetails;
import wb.control.fragments.Frag_lokgpiocfg;
import wb.control.fragments.Frag_loklist;
import wb.control.fragments.Frag_macro_adddevice;
import wb.control.fragments.Frag_macro_edit;
import wb.control.fragments.Frag_macro_overview;
import wb.control.fragments.Frag_quickaction;
import wb.control.fragments.Frag_servocontrol;
import wb.control.fragments.Frag_test_level1;

public class FAct_control extends AppCompatActivity
implements OnFragReplaceListener, OnConfigChangedListener, OnActionGridChangeListener, OnHardkeyChangedListener, OnActionBarConfigListener, OnDeviceChangeListener {

	//static Handler msghandler;		// für Messages von Basis TODO: weg
	ActionBar actionBar;
	//ActionElement action_ae_temp;	//* zum Zwischenspeichern des AEs, das editiert oder neu angelegt werden soll (wird in Frag_action_edit weiterbearbeitet)
	int ae_to_edit;					//** zum Zwischenspeichern des AEs, das editiert oder neu angelegt werden soll (wird in Frag_action_edit weiterbearbeitet). Index der Basis.actionelementlist des Elements
	Macro macro_to_edit;			//** zum Zwischenspeichern des Macros, das editiert werden soll (für Frag_macro_edit und Frag_macro_adddevice)
	String newdevice;				//** zum Zwischenspeichern wg. hinzufügen eines neuen Devices zum Macro (wird in Frag_macro_adddevice gesetzt und in Frag_macro_adddevice ausgewertet)
	//int cfg_group;					//** der Index der Config-Gruppe, die in Frag_cfg_level2 gestartet werden soll
	int displaymode;				// WBcontrol Layout displaymode (01=singleview, 2=dual, 3=multi)
	int displayOrientation;			// Configuration.ORIENTATION_LANDSCAPE / Configuration.ORIENTATION_PORTRAIT
	int standardcontainerID;		// die ID des Containers, in dem die Fragments ausgetauscht werden sollen (je nach Displaymode fact_control_fragcontainer1 oder 2) 
	boolean fc2big;					//** der derzeitige Status, ob Anzeigebereich 2 (fact_control_fragcontainer2) normal oder vergrößert dargestellt wird (Zustand der Variable wird erst nach der Veränderung am Container umgesetzt)
	//LinearLayout fragcontainer1;	//, fragcontainer2; 2 wird derzeit nicht benötigt
	ViewGroup fragcontainer1, standardContainer;	//, fragcontainer2; 2 wird derzeit nicht benötigt
	//*: diese Variablen werden nicht gesichert, gehen bei Orientationchange verloren -> werden nur kurzfristig zum Starten eines neuen Fragments benötigt (wäre Pech - ev. später einbauen)
	//**: diese Variablen müssen gesichert werden, damit bei Orientationchange nix verloren geht
	private String[] fragment_tags;	// res/values/arrays/fragment_tags array-index = Fragment-ID
	private String[] fragment_names;	// index = Fragment-ID
	private boolean recreated;		// wurde die Activity nach einem Orientationchange osw. recreated? dann Fragments nicht neu starten, weil sie aufgehoben wurden
	private int menuItemToHide = 0;		// welches ActionBar Item soll vesteckt werden (0 = keines)
	private int menuItemToUnHide = 0;	// welches ActionBar Item wurde versteckt und soll jetzt wieder angezeigt werden

	private boolean ab_show_add_item = false;	// ActionBar Item "add Item" anzeigen oder nicht
	private boolean ab_show_del_item = false;	// ActionBar Item "delete Item" anzeigen oder nicht
    BroadcastReceiver locBcReceiver;
    IntentFilter ifilter;

	SparseArray<HardKey> hardkeys;
	SparseBooleanArray hk_check;
    SparseArray<Bundle> fragmentData;   // Daten (in Bundle) für Fragments beim Start/resume (ohne dass das Fragment neue rstellt werden muss [setArguments()/getArguments() geht nur bei neu erstellten Instanzen]
	
	// für Navigation Drawer
	private DrawerLayout drawerLayout;
    private ListView navilistview;	
    //private int[] navitarget;			// Fragment-ID of selected navilist-entry
    private ActionBarDrawerToggle abdrawerToggle; // TODO: umbauen http://stackoverflow.com/questions/26439619/how-to-replace-deprecated-android-support-v4-app-actionbardrawertoggle
    private ArrayList<Integer> navicommands;	// Fragment-IDs
    private NaviAdapter na;
    private int exitWarning;            // counter für Warnung vor Programmausstieg per back-Taste

	// Fragment-IDs für Benachrichtigungen usw (0 freilassen!) Index der Fragment-Tags!!!
	public static final int FRAGMENT_CONTROL 		= 	1;
	public static final int FRAGMENT_ACTION 		= 	2;
	public static final int FRAGMENT_ACTION_EDIT 	= 	3;
	public static final int FRAGMENT_LOKS			= 	4;	// wird nicht mehr benötigt
	public static final int FRAGMENT_MACRO_OVERVIEW	= 	5;
	public static final int FRAGMENT_MACRO_EDIT		= 	6;
	public static final int FRAGMENT_MACRO_ADDDEV	= 	7;
	public static final int FRAGMENT_TEST_LEVEL1	= 	8;
	public static final int FRAGMENT_LOG			= 	9;
	public static final int FRAGMENT_DEVICES		= 	10;	// wird nicht mehr benötigt
	public static final int FRAGMENT_CFG_LEVEL1		= 	11;
	public static final int FRAGMENT_CFG_LEVEL2		= 	12;
	public static final int FRAGMENT_VIDEO_TEST		= 	13;
	public static final int FRAGMENT_TRACKS_TEST	= 	14;
	public static final int FRAGMENT_STREAM_TEST	= 	15;
	public static final int FRAGMENT_HARDKEYS		= 	16;
	public static final int FRAGMENT_CAMCONTROL		= 	17;
	public static final int FRAGMENT_LOKLIST		= 	18;
	public static final int FRAGMENT_LOKDETAILS		= 	19;
    public static final int FRAGMENT_LOKGPIOCFG		= 	20;
    public static final int FRAGMENT_QUICKACTION	= 	21;
	
	// OnConfigChangedListener configID
	public static final int CID_DISP_FC2_CHANGESIZE 	= 	1;	// Anzeigebereich 2 (fact_control_fragcontainer2) vergrössern, die anderen verschwinden lassen bzw. wieder zurückstellen
	public static final int CID_CHANGE_THEME		 	= 	2;	// anderes Theme einstellen
	public static final int CID_CHANGE_FONTSCALE		= 	3;	// Fontscale ändern
	public static final int CID_CHANGE_LAYOUT			= 	4;	// Layout ändern
	public static final int CID_CHANGE_USERMODE			= 	5;	// Usermode ändern (Standard / Gast)
	public static final int CID_CHANGE_SPEEDBAR			= 	6;	// Speedbar Inhalt hat sich geänder
	public static final int CID_CHANGE_NAVIDRAWER		= 	7;	// Navidrawer Commands müssen neu aufgebaut werden (zB. wg. Runmode change). CID_CHANGE_LAYOUT, THEME bewirken das auch wg. Activity restart/recreation
	
	// unique loader ids
	public static final int DEVICE_LOADER		= 	1;
	public static final int LOG_LOADER			= 	2;
	
	
	// ActionBar item IDs (für OnActionBarConfigListener, OnActionBarItemListener) zur Verwendung in den Fragments
	public static final int AB_ITEM_ADD		= 	0;
	public static final int AB_ITEM_DEL		= 	1;
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		TextView headerView;	// view für list header
        int aktuelles_fragment;			//** das derzeitige fragment (im fragcontainer2, wenn's 2 gibt) (wg. orientationchange)

		// Theme einstellen // muss vor "super.onCreate"-Aufruf gemacht werden
		String themeName = Basis.getThemeTypeName();
		int themeId = getResources().getIdentifier(themeName, "style", getPackageName());
        //int themeId = getResources().getIdentifier("AppTheme", "style", getPackageName());
		setTheme(themeId);
				
		Basis.useFontScale(this);	// FontScale laut gespeicherter Konfiguration setzen

		super.onCreate(savedInstanceState);	// wg. theme nicht am anfang machen

        fragmentData = new SparseArray<Bundle>();

		displayOrientation = getResources().getConfiguration().orientation;
		displaymode = Basis.getDisplaymode();
		fragment_tags = getResources().getStringArray(R.array.fragment_tags);
		
		fc2big = false;
		boolean fc2big_soll = false;	// Sollzustand für Anzeigebereich 2, der wiederhergestellt werden muss, falls Activity durch Orientationchange oä. neu gestartet wird
		aktuelles_fragment = FRAGMENT_ACTION;	// Standard-Anfangswertwert für Dual- und Multiview
		ae_to_edit = -1;

        //display-modus checken, ob eingestellt wurde, dass ein bestimmter Modus erzwungen werden soll
        int forceDisplayMode = Basis.getForceDisplaymode();
        if (forceDisplayMode > 0) { displaymode = forceDisplayMode; }
        if (displaymode == Basis.DISPLAYMODE_SINGLEVIEW) { aktuelles_fragment = FRAGMENT_CONTROL; }

		if (savedInstanceState != null) // falls Einstellung wiederhergestellt werden müssen
		{
			recreated = true;
			fc2big_soll = savedInstanceState.getBoolean("fc2big");	// oder false, wenn nicht im Bundle vorhanden
			newdevice = savedInstanceState.getString("newdevice");
			String macroname = savedInstanceState.getString("macro_to_edit");
			if (macroname != null) { macro_to_edit = Basis.getMacrolistObjectByName(macroname); }
			ae_to_edit = savedInstanceState.getInt("ae_to_edit", -1);
            aktuelles_fragment = savedInstanceState.getInt("frag", FRAGMENT_ACTION);
		}
		

		if (displaymode == Basis.DISPLAYMODE_SINGLEVIEW) { standardcontainerID = R.id.fact_control_fragcontainer1; }
		else { standardcontainerID = R.id.fact_control_fragcontainer2; }
		
		if (displaymode == Basis.DISPLAYMODE_SINGLEVIEW)
		{
			setContentView(R.layout.fact_control1);	// nur ein Anzeigebereich
		}
		else // 2 Anzeigebereiche neben- bzw. übereinander
		{ 
			if ((Basis.getLayouttype() == 1) && (displayOrientation == Configuration.ORIENTATION_LANDSCAPE)) {  setContentView(R.layout.fact_control_r); }
			else { setContentView(R.layout.fact_control); }
		}	
		
		standardContainer = (ViewGroup)findViewById(standardcontainerID);
		fragcontainer1  = (ViewGroup)findViewById(R.id.fact_control_fragcontainer1);
		
		// Navigation Drawer vorbereiten
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navilistview = (ListView) findViewById(R.id.navi_drawer);
		navicommands = new ArrayList<Integer>();
		fragment_names = getResources().getStringArray(R.array.fragment_names);
	
		initNavicmds();	// Navigation Drawer befüllen
		
		headerView = new TextView(this);
		headerView.setText(R.string.drawer_header_txt);
		headerView.setPadding(4, 4, 4, 2);
		navilistview.addHeaderView(headerView, null, false);	// false, damit der Header nicht selectable ist
		
		na = new NaviAdapter(this, R.layout.navilistelement, navicommands, getLayoutInflater());
		navilistview.setAdapter(na);
		// Set the list's click listener
		navilistview.setOnItemClickListener(new DrawerItemClickListener());

        abdrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            // Called when a drawer has settled in a completely closed state.
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu();
            }

            // Called when a drawer has settled in a completely open state.
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu();
            }
        }; 

        drawerLayout.addDrawerListener(abdrawerToggle);     // Set the drawer toggle as the DrawerListener
		// entsprechende fragments laden
		int f1_id = 0;
		if (displaymode == Basis.DISPLAYMODE_SINGLEVIEW) { f1_id = aktuelles_fragment; }
		else {  f1_id = FRAGMENT_CONTROL; }

        startFragment(R.id.fact_control_fragcontainer1, f1_id, false, null);  // nur createn, wenn fragment noch nicht existiert

		if (displaymode != Basis.DISPLAYMODE_SINGLEVIEW)
		{
            startFragment(standardcontainerID, aktuelles_fragment, false, null);  // nur createn, wenn fragment noch nicht existiert
			if (fc2big_soll) { displayAction(CID_DISP_FC2_CHANGESIZE, null); }	// Anzeigezustand wiederherstellen
		}

        startFragment(R.id.fact_control_fragcontainer_act, FRAGMENT_QUICKACTION, false, null);    // Fragment wird nur ne erstellt, falls es noch nicht existiert

		Basis.setWakelockmode(Basis.getWakelockmode(), this.getWindow());	// Screensaver lt. Config einstellen

        // Empfang für lokales Broadcasting einrichten
        ifilter = new IntentFilter();
		ifilter.addAction(Basis.ACTION_PROGUPDATE_AVAILABLE);
        ifilter.addAction(Basis.ACTION_PROGUPDATE_DO);

        locBcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Basis.ACTION_PROGUPDATE_AVAILABLE))   // Programmupdate ist vorhanden
                {
                    askUpdate();
                }

                else if (intent.getAction().equals(Basis.ACTION_PROGUPDATE_DO))
                {
                    Intent intent_startend = new Intent(context, End.class);
                    intent_startend.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);    // früher ging's ohne das -> jetzt absturz ohne dieses flag
                    context.startActivity(intent_startend);
                    finish();
                }
            }
        };

		  actionBar = getSupportActionBar();
		  actionBar.setDisplayHomeAsUpEnabled(true);
		  actionBar.setHomeButtonEnabled(true);
		  
		  // Hardkey Array für die Verarbeitung der Hardware-Tasten laden
		  hardkeys = new SparseArray<HardKey>();
		  hk_check = new SparseBooleanArray();
		  Basis.DBloadAllHardkeys(hardkeys);
		  hk_check_init();		//hk_check befüllen

        // BackStackChangedListener zum Aktualisieren des ausgewählten (fett dargestellten) Fragments im Drawer
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {

            @Override
            public void onBackStackChanged() {
                Fragment f = getSupportFragmentManager().findFragmentById(standardcontainerID);
                if (f != null) { updateDrawerSelected(f); }
                //logFragments();
                //healFragments();    // TODO: Test wg. Backstack Problematik
            }
        });

		  
	} // end onCreate


	@Override
	public void onResume() {
		super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(locBcReceiver, ifilter); // localBraodcast-Empfang aktivieren

		displayOrientation = getResources().getConfiguration().orientation;	// könnte sich geändert haben
			
		if (saveSpaceOnActionBar()) { actionBar.setDisplayShowTitleEnabled(false); }	// mehr Platz schaffen

        exitWarning = 0;
	}


	public boolean saveSpaceOnActionBar()
	{
		// check, um bei kleinen Displaygrößen Platz in der Actionbar zu sparen
		boolean save = false;
		boolean portrait = false;
		int size = 4;
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { portrait = true; }
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) { size = 1; }
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL) { size = 2; }
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) { size = 3; }
		
		if (portrait && (size < 4)) { save = true; }	// bei Portrait ab Large Platz sparen
		if (!portrait && (size < 3)) { save = true; }	// bei Landscape ab Normal Platz sparen
			
		return save;
		
	}


    // Back abfangen, bevor aus dem Programm ausgestiegen wird
    @Override
    public void onBackPressed(){

        if ((getSupportFragmentManager().getBackStackEntryCount() == 0) && (exitWarning < 1)){
            Basis.showWBtoast("Achtung: Ausstieg!");    // Todo: test
            exitWarning += 1;
        }
        else
        {
            exitWarning = 0;
            super.onBackPressed();
        }
    }

	
	/* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        //boolean drawerOpen = drawerLayout.isDrawerOpen(navilistview);
        //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        //TODO: was hier machen?
        return super.onPrepareOptionsMenu(menu);
    }
	

     // When using the ActionBarDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()...
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        abdrawerToggle.syncState();	// Sync the toggle state after onRestoreInstanceState has occurred.
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        abdrawerToggle.onConfigurationChanged(newConfig);	// Pass any configuration change to the drawer toggls
    }
    

	@Override
	public void onPause() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(locBcReceiver);	// localBraodcast-Empfang stoppen (für WLAN-Status)
		super.onPause();
	}


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is killed and restarted.
        super.onSaveInstanceState(savedInstanceState);    // muss als ersts aufgerufen werden!!
        savedInstanceState.putBoolean("fc2big", fc2big);
        savedInstanceState.putString("newdevice", newdevice);
        if (macro_to_edit != null) {
            savedInstanceState.putString("macro_to_edit", macro_to_edit.getName());
        }
        //savedInstanceState.putInt("cfg_group", cfg_group);
        savedInstanceState.putInt("ae_to_edit", ae_to_edit);

        Fragment f = getSupportFragmentManager().findFragmentById(standardcontainerID); // aktuelles Fragment (ID) speichern
        savedInstanceState.putInt("frag", ((WBFragID)f).getFragmentID());
    }



	/* da fc2big schon in onCreate() benötigt wird, wird sie bereits dort restored!!!!
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState. This bundle has also been passed to onCreate.
	  // This method is called after onStart
	  fc2big = savedInstanceState.getBoolean("fc2big");
	}
	*/


    public void setFragmentData(int fragmentID, Bundle b)
    {
        fragmentData.put(fragmentID, b);    // TODO: egal ob vorher eines für diese fragmentID existiert hat?
        //Adds a mapping from the specified key to the specified value, replacing the previous mapping from the specified key if there was one.
    }

    public Bundle getFramgentData(int fragmentID)
    {
        Bundle b = fragmentData.get(fragmentID);
        if (b != null) { fragmentData.remove(fragmentID); } // Eintrag entfernen, da er abgeholt wurde

        return b;    // oder null, wenn für fragmentID kein Bundle existiert
    }


	// Options Menü -------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_opt, menu);
	    
	    if (Basis.getUsermode() == Basis.USERMODE_GUEST)
	    {
	    	// "gefährliche" Fragments vor Gast verbergen
	    	//menu.removeItem(R.id.menui_loks);
	    	//menu.removeItem(R.id.menui_loks2);
	    	//menu.removeItem(R.id.menui_macro);
	    	//menu.removeItem(R.id.menui_test);
	    	menu.removeItem(R.id.menui_cfg);
	    }

	    else	// USERMODE_STANDARD
	    {
	    	if (menuItemToHide != 0)
	    	{
	    		MenuItem hideitem =  menu.findItem(menuItemToHide);
	    		hideitem.setVisible(false);

	    		if (menuItemToUnHide != 0)
	    		{
	    			MenuItem unhideitem =  menu.findItem(menuItemToUnHide);
	    			unhideitem.setVisible(true);
	    			menuItemToUnHide = 0;
	    		}

	    		menuItemToHide = 0;
	    	}
	    }

        // aus Menü/Actionbar entfernen, diese Elemente sind jetzt im Navigation Drawer
        // TODO: später komplett entfernen
        menu.removeItem(R.id.menui_action);
        menu.removeItem(R.id.menui_loks);
        menu.removeItem(R.id.menui_loks2);
        menu.removeItem(R.id.menui_macro);
        menu.removeItem(R.id.menui_test);
	    
	    // ActionBarItems der Fragments konfigurueren
	    MenuItem actionbaritem =  menu.findItem(R.id.menui_add_item);	    
		actionbaritem.setVisible(ab_show_add_item);
		actionbaritem =  menu.findItem(R.id.menui_delete_item);
		actionbaritem.setVisible(ab_show_del_item);
		
	    
	    menu.removeItem(R.id.menui_ctrl);	// ctrl ab apilevel 7 über Icon in ActionBar, darunter in Frag-quickaction
	    // TODO: wenn alles ok, ctrl komplett aus menü entfernen!

	    return super.onCreateOptionsMenu(menu);

	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {	
		
		// The action bar home/up action should open or close the drawer. ActionBarDrawerToggle will take care of this.
       if (abdrawerToggle.onOptionsItemSelected(item)) { return true; }
		
		int itemId = item.getItemId();

		//Fragment f = getSupportFragmentManager().findFragmentByTag(fragment_tags[aktuelles_fragment]); // nono!! aktuelles_fragment ist nicht immer aktuell zB. bei Fragment-Wechsel durch "back"
        Fragment f = getSupportFragmentManager().findFragmentById(standardcontainerID);
		String akt_frag_tag = f.getTag();
        int aktuelle_fragid = ((WBFragID)f).getFragmentID();
		switch(itemId)
    	{
    		case R.id.menui_cfg:
    			if (aktuelle_fragid != FRAGMENT_CFG_LEVEL1) { startFragment(standardcontainerID, FRAGMENT_CFG_LEVEL1, true, null); }
    			return true;
    		
    		case R.id.menui_exit:
    			Intent intent_startend = new Intent(this, End.class);
    			this.startActivity(intent_startend);
    			finish();
    			return true;
    		
    		case R.id.menui_loks2:
    			if (aktuelle_fragid != FRAGMENT_LOKLIST) { startFragment(standardcontainerID, FRAGMENT_LOKLIST, true, null); }
    			return true;
    		
    		case R.id.menui_macro:
    			if (aktuelle_fragid != FRAGMENT_MACRO_OVERVIEW) { startFragment(standardcontainerID, FRAGMENT_MACRO_OVERVIEW, true, null); }
    			return true;
    			
    		case R.id.menui_test:
    			if (aktuelle_fragid != FRAGMENT_TEST_LEVEL1) { startFragment(standardcontainerID, FRAGMENT_TEST_LEVEL1, true, null); }
    			return true;
    			
    		case R.id.menui_action:
    			if (aktuelle_fragid != FRAGMENT_ACTION) { startFragment(standardcontainerID, FRAGMENT_ACTION, true, null); }
    			return true;
    			
    		case R.id.menui_ctrl: case android.R.id.home:
    			if ((aktuelle_fragid != FRAGMENT_CONTROL) && (displaymode == Basis.DISPLAYMODE_SINGLEVIEW)) { startFragment(standardcontainerID, FRAGMENT_CONTROL, true, null); }
    			return true;
    		
    		case R.id.menui_add_item:
    			//Fragment addfr = getSupportFragmentManager().findFragmentByTag(fragment_tags[aktuelles_fragment]);    // TODO: das war mist
    			if (f != null) { ((OnActionBarItemListener) f).OnActionBarItem(AB_ITEM_ADD); }
    			return true;
    			
    		case R.id.menui_delete_item:
    			//Fragment delfr = getSupportFragmentManager().findFragmentByTag(fragment_tags[aktuelles_fragment]);    // TODO: das war mist
    			if (f != null) { ((OnActionBarItemListener) f).OnActionBarItem(AB_ITEM_DEL); }
    			return true;
    			
    		default:
    			return super.onOptionsItemSelected(item);
    	}
		
	}
	
	

	// Callback für's Wechseln der Fragments
	@Override
	public void OnFragReplace(int fragmentID, boolean toBackStack,  Bundle data) {
		
		startFragment(standardcontainerID, fragmentID, toBackStack, data);

	}	// End OnFragReplace
	
	
	Fragment createNewFragment(int fragmentID)
	{
		Fragment fneu = null;
		
		switch (fragmentID) {

		case FRAGMENT_CONTROL:	// standardcontainerID passt für CTRL nur im SINGLE_VIEW, wird aber sonst nie neu geladen -> daher ok
			fneu = new Frag_control();	
			break;

        case FRAGMENT_QUICKACTION:
            fneu = new Frag_quickaction();
            break;

		case FRAGMENT_ACTION:
			//fneu = new Frag_action();
			fneu = new Frag_action2();
			break;

		case FRAGMENT_ACTION_EDIT:
			fneu = new Frag_action_edit();
			break;

		case FRAGMENT_MACRO_OVERVIEW:
			macro_to_edit = null;	// Daten für zu editierendes Macro zurücksetzen
			newdevice = "";			// Daten für Device, das dem Macro hinzugefügt werden soll, zurücksetzen
			fneu = new Frag_macro_overview();
			break;

		case FRAGMENT_MACRO_EDIT:
			fneu = new Frag_macro_edit();
			break;

		case FRAGMENT_MACRO_ADDDEV:
			fneu = new Frag_macro_adddevice();
			break;

		case FRAGMENT_LOG:
			fneu = new Frag_log();
			break;
			
		case FRAGMENT_TEST_LEVEL1:
			fneu = new Frag_test_level1();
			break;

		case FRAGMENT_CFG_LEVEL1:
			fneu = new Frag_cfg_level1();
			break;

		case FRAGMENT_CFG_LEVEL2:
			fneu = new Frag_cfg_level2();
			break;
			
		case FRAGMENT_VIDEO_TEST:
			fneu = new Frag_VideoTest();
			break;
			
		case FRAGMENT_STREAM_TEST:
			fneu = new Frag_StreamTest();
			break;
			
		case FRAGMENT_TRACKS_TEST:
			fneu = new Frag_TrackPlanTest();
			break;
			
		case FRAGMENT_HARDKEYS:
			fneu = new Frag_hardkeys();
			break;
			
		case FRAGMENT_LOKLIST:
			fneu = new Frag_loklist();
			break;
			
		case FRAGMENT_LOKDETAILS:
			fneu = new Frag_lokdetails();
			break;
			
		case FRAGMENT_CAMCONTROL:
			//fneu = new Frag_camcontrol();
			fneu = new Frag_servocontrol();
			break;

        case FRAGMENT_LOKGPIOCFG:
                fneu = new Frag_lokgpiocfg();
                break;

		default:
			break;
		}

		return fneu;
	}
	


	void startFragment(int containerID, int fragmentID, boolean toBackStack, Bundle data)
	{
        //Übergabe: containerID statt ViewGroup container
		Fragment f;
        ViewGroup container = (ViewGroup)findViewById(containerID);

        //Fragment currentFragment = getSupportFragmentManager().findFragmentById(containerID);

        f = getSupportFragmentManager().findFragmentByTag(fragment_tags[fragmentID]);   // checken, ob fragment schon existiert (zB. bei Orientationchange ein Problem)
        if (f == null) { f = createNewFragment(fragmentID);  }

		if (f != null)
		{
			//if (data != null) { f.setArguments(data); }	// Bundle data mitgeben nicht mehr über setArguments()! -> wird jetzt über fragmentData gemacht!! (weil fragments nicht jedes Mal neu erstellt werden))
            if (data != null) { setFragmentData(fragmentID, data); }    // neue Methode über fragmentData
			FragmentTransaction fragmentTransaction2 = getSupportFragmentManager().beginTransaction();
            //if ((currentFragment != null) && (currentFragment != f)) { fragmentTransaction2.hide(currentFragment); }  // Test hide old fragment
			if (f.isHidden()) {fragmentTransaction2.show(f); }
			fragmentTransaction2.replace(containerID, f, fragment_tags[fragmentID]);	// tag zum Kennzeichnen und wiederfinden des Fragments (ggf. einer neuen Instanz nach OrientationChange)
			if (toBackStack) { fragmentTransaction2.addToBackStack(null); }	// true -> man kann mit BACK-Key wieder zurückspringen
			fragmentTransaction2.commit();
		}
        //logFragments(); // TODO: Test, wieder weg
	}
		
	
	public void setAe_toedit(int ae_toedit) {
		ae_to_edit = ae_toedit;
	}
	
	public int getAe_toedit() {
		return ae_to_edit;
	}
	
	public void set_macro_to_edit(Macro macro_ed) {
		macro_to_edit = macro_ed;
	}
	
	public Macro get_macro_to_edit() {
		return macro_to_edit;
	}

	public void set_macro_newdevice(String devname) {
		newdevice = devname;
	}
	
	public String get_macro_newdevice() {
		return newdevice;
	}
	

	@Override
	public void OnConfigChanged(int configID, Bundle data) {
		// Data: int "cfggroup" zum Zwischenspeichern der aktuellen Einstellungs-Gruppe (damit sie nach dem Restart wiederhergestellt wird (für Apilevel < 11)
		
		switch (configID) {

		case CID_DISP_FC2_CHANGESIZE:
			displayAction(configID, data);
			break;
			
		case CID_CHANGE_USERMODE:
			// cfg-fragment gegen control oder action austauschen	
			Basis.setConfigfragGroup(-1);
			int fragid = FRAGMENT_ACTION;
			if (displaymode == Basis.DISPLAYMODE_SINGLEVIEW) { fragid = FRAGMENT_CONTROL; }
			//Basis.setAktuellesfragment(fragid);		// passendes Fragment für den Gast setzen   // TODO: setAktuellesfragment gibt's nicht mehr -> checken auswirkung
			recreateWBactivity(false);	// Anzeigemanagement in Activity neu durchlaufen (bzw. Restart)
			break;
			
			case CID_CHANGE_LAYOUT:
				
				/*
				//TODO test
				displaymode = Basis.getDisplaymode();
				if (Basis.getDisplaymode() == Basis.DISPLAYMODE_DUALVIEW) 
				{
					//TODO: bei umschaltung von singleview auf dualview muss der control-bereich neu eigerichtet werden (2. fragment wird sonst 2x dargestellt)
					startFragment(fragcontainer1, FRAGMENT_CONTROL, false, null);
				}
				
				// TODO: CID_CHANGE_LAYOUT auftrenne: control links/rechts eingene id -> dann bei singelview nix tun!!!
			*/
			
		case CID_CHANGE_THEME: case CID_CHANGE_FONTSCALE: 
			boolean dofragcleanup = false;	// bei Wechsel von dual/multi-view auf single-view müssen die fragments des nicht mehr verwendeten containers vorher entfernt werden, sonst gibt's nachher probleme, weil die fragments weiterexistieren, der Container aber nicht!
			if (data != null)	// checken, ob eine gfg-group mitgegeben wurde
			{	
				int grp = data.getInt("cfggroup", -1);
				Basis.setConfigfragGroup(grp);	// wenn kein Wert vorhanden, dann -1 verwenden (=nix gewählt)
				dofragcleanup = data.getBoolean("fragcleanup");
			}
			recreateWBactivity(dofragcleanup);
			break;
			
		case CID_CHANGE_NAVIDRAWER:
			// navidrawer cmd list must be recreated (runmode- or layout-change)
			initNavicmds();	
			break;
			
		case CID_CHANGE_SPEEDBAR:
			// speedbar must be updated (redraw)
			refreshSpeedbar();
			break;
		}

	}	// end OnChangeDisplayMode
	
	public void refreshSpeedbar()
	{
        Frag_control fctrl = (Frag_control) getSupportFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_CONTROL]);

		if (fctrl != null)
		{
			if (fctrl.isInLayout())	{ fctrl.refreshSpeedbar(); }
		}
		
	}
	

	// Änderungen am Anzeige-Zustand
	public void displayAction(int displayActionID, Bundle data)
	{
		switch (displayActionID) {

		case CID_DISP_FC2_CHANGESIZE:

			if (fc2big)	// fc2 verkleinern -> fc1 sichtbar machen
			{
				fragcontainer1.setVisibility(View.VISIBLE);
				fc2big = false;
			}
			else	// fc2 vergrößern -> fc1 verstecken
			{
				fragcontainer1.setVisibility(View.GONE);
				fc2big = true;
			}
			break;
		}
	}
	
	@SuppressLint("NewApi")
	private void recreateWBactivity(boolean fragCleanup)    // TODO: checken, auf Verbesserung, savedInstanceState, aktuellesFragment (ersatz?, weil gibt's nicht mehr)..
	{
		Basis.clearQuickActionlist();	// soll neue aufgebaut werden
		
		if (Basis.getApiLevel() >= 11) 
		{ 
			if (fragCleanup)	// alle Fragments aus dem Standardcontainer (bei dual/mulit-view fact_control_fragcontainer2) entfernen
			{
				Fragment f_remove;
				FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
				
				for (String t : fragment_tags)	// wird für ALLE bekannten fragments durchgeführt
				{
					f_remove = getSupportFragmentManager().findFragmentByTag(t);
					if (f_remove != null) { fragmentTransaction.remove(f_remove); }
				}
				fragmentTransaction.commit();
			}
			this.recreate(); // recreate activity so theme etc. gets updated
		}	
		else	// Ersatz für ältere Versionen < API-Level 11: Activity neu starten
		{	
			Intent  intent_restart = this.getIntent(); 
	    	this.startActivity(intent_restart);
	    	finish();	// Activity beenden
		}
	}
	

	// create navigation drawer command list
	private void initNavicmds()
	{
		if (navicommands.size() > 0) { navicommands.clear(); }
		if (displaymode == Basis.DISPLAYMODE_SINGLEVIEW) { navicommands.add(FRAGMENT_CONTROL); }	// add "Control" nur im SingleView-Mode
		navicommands.add(FRAGMENT_ACTION);
		
		if (Basis.getUsermode() != Basis.USERMODE_GUEST)
		{
			navicommands.add(FRAGMENT_LOKLIST);
			navicommands.add(FRAGMENT_MACRO_OVERVIEW);
			if (Basis.getRunmode() != runmodetype.standard) { navicommands.add(FRAGMENT_TEST_LEVEL1); } // add "Test"
		}
			
		if (na != null) { na.notifyDataSetChanged(); }
	}
	
	/*
	// ####### TODO: weg -> auf LocalBroadcast umstellen: Auswertungsfunktion für UI - MessageHandler ##############################
	   
	private void CheckMsg(Message msg) {
    	    	
    	switch (msg.what) {
    	    		
    	case Basis.MSG_BASIS_PROGUPDATE_AVAILABLE:	// Programmupdate ist vorhanden
    		askUpdate();
    		break;


		// in Basis bereits erledigt: MSG_BASIS_PROGUPDATE_DO -> sendLocalBroadcast(ACTION_PROGUPDATE_DO)
    	case Basis.MSG_BASIS_PROGUPDATE_DO:	// Programmupdate durchführen
    		Intent intent_startend = new Intent(this, End.class);
			this.startActivity(intent_startend);
			finish();
    		break;


    	//default:
    	//	super.handleMessage(msg);

    	}
    }	// end CheckMsg() */
	
	
	private void askUpdate()
	{
		//textView_status.setText(getString(R.string.basis_upd_ver_online) + " = " + Basis.getUpdateAvailable());
		
		if (Basis.getApiLevel() >= 9)	// sollte eigentlich ab 9 gehen -> checken!!! // war: Build.VERSION_CODES.HONEYCOMB
		{
			DialogFrag_Update upfrag = new DialogFrag_Update();
			//upfrag.show(getFragmentManager(), "updatedialog");	// so wird's in einem Fragment gemacht
			
			FragmentManager fm = getSupportFragmentManager();	// so in der Activity
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(upfrag, "updatedialog");
            ft.commit();
        }
	}

	@Override
	public void OnActionGridChange() {
		
		// Action2 Grid Konfiguration hat sich geändert -> GridLayout neu aufbauen
		
		Frag_action2 f = (Frag_action2) getSupportFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_ACTION]);
		
		if (f != null) { f.CreateGrid(); }
		
	}

	
	// zum Weiterverteilen der Hard Keys an die Fragments

	// fängt die HardKeys am Tab!
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	
		// Hardkey-Events sollen an einen Control-"Kunden" geschickt werden, oder an Fragment_Hardkeys (die Konfiguration), falls es aktiv ist
		// TODO: es muss festgelegt werden, welchs Control die Hardkeys verwendet (sobald es mehrere gibt)
		int keyfunction;
		String macroname;
		int keyCode;
		int funktion = 1;
		boolean send_key = true;
		
		if (event.getAction()==KeyEvent.ACTION_DOWN)
		{
			keyCode = event.getKeyCode();
			if (keyCode == KeyEvent.KEYCODE_BACK) { return super.dispatchKeyEvent(event); }	 // KeyEvent wurde hier nicht verarbeitet

			if (Basis.isHardKeyConfigActive())
			{
				// an Konfigurations-Fragment weiterleiten, falls aktiv (Steuerung ist dann nicht möglich)
				Frag_hardkeys fhk =  (Frag_hardkeys) getSupportFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_HARDKEYS]);
				if (fhk != null) 
				{
					fhk.getKeyEvent(event); 
					return true;	// KeyEvent wird hier verarbeitet
				}
			}

			// Auswertung für Steuerung
			if (hardkeys.indexOfKey(keyCode) >= 0)	// key soll ausgewertet werden
			{
				HardKey hk = hardkeys.get(keyCode);
				funktion = 1;
				send_key = true;
				keyfunction = 0;
				macroname = "";
				
				switch (hk.keytype) {

				case HardKey.HARDKEY_TYPE_KLICK:
					if (event.getRepeatCount() != 0) { send_key = false; }
					break;
					
				case HardKey.HARDKEY_TYPE_KLICK_LONG:
					if (event.isLongPress()) { funktion = 2; }
					break;
					
				case HardKey.HARDKEY_TYPE_ON_OFF:
					if (!hk_check.get(keyCode)) { funktion = 2; }	// 2. (OFF) Tastendruck
					hk_check.put(keyCode, !hk_check.get(keyCode));	// abwechselnde Funktion umschalten (true:ON / false:OFF)
					break;

				// default:
				} // end switch
				
				if (send_key)  // Tastendruck weitergeben, funktion= 1|2
				{
					if (funktion == 1) 
					{ 
						keyfunction = hk.function1; 
						macroname = hk.macro1;
					}
					else if (funktion == 2) 
					{ 
						keyfunction = hk.function2;
						macroname = hk.macro2;
					}
					
					if (keyfunction > 0)
					{
						Frag_control fc =  (Frag_control) getSupportFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_CONTROL]);
						if (fc != null) { fc.getHardKeyEvent(keyfunction, macroname); }
						return true;	// KeyEvent wird hier verarbeitet
					}
				}
					
			}	// Ende Auswertung für Steuerung
			
		} // Ende if ACTION_DOWN
		

		// KeyEvent wurde hier nicht verarbeitet
		return super.dispatchKeyEvent(event);

	} 

	
	
	@Override
	public void OnHardkeyChanged(int keycode) {
		
		// Daten für hardkey function neu aus db aktualisieren.
		// keycode -1: alle aktualisieren!
		
		Intent hkintent = new Intent(Basis.ACTION_HKEY_UPDATE);
		hkintent.putExtra("keycode", keycode);
		LocalBroadcastManager.getInstance(this).sendBroadcast(hkintent);

		// das Array hier in der Activity aktualisieren
		if (keycode > -1) // nur einen ändern
		{ 
        	HardKey hk = Basis.DBloadHardkey(keycode);
        	if (hk != null) // key hinzufügen/updaten
        	{ 
        		hardkeys.put(keycode, Basis.DBloadHardkey(keycode));
        		if (hk_check.indexOfKey(keycode) < 0) { hk_check.put(keycode, true); }	// key in hk_check anlegen, falls noch nicht vorhanden
        	}
        	else // key existiert nicht mehr
        	{
        		hardkeys.delete(keycode);
        		hk_check.delete(keycode);
        	}
		}
		else // alle ändern
		{ 
			Basis.DBloadAllHardkeys(hardkeys);
			hk_check_init();	// TODO: Achtung: alle Stati gehen verloren (aber während der Konfig eher uninteressant!)
		}
		
		
	}


	// Erstbefüllung von hk_check
	private void hk_check_init()
	{
		if (hk_check.size() > 0) { hk_check.clear(); }
		
		//hk_check befüllen
		  for (int a=0;a<hardkeys.size();a++)
		  {
			  hk_check.put(hardkeys.keyAt(a), true);
		  }
	}
	
	
	// for Navigation Drawer
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	    {
            int frgmentID = (int)view.getTag();

            Fragment f = getSupportFragmentManager().findFragmentById(standardcontainerID);
            int aktuelle_fragid = ((WBFragID)f).getFragmentID();
            if (!(frgmentID == aktuelle_fragid)) { startFragment(standardcontainerID, frgmentID, true, null); }

	    	// Highlight the selected item, update the title, and close the drawer
	    	navilistview.setItemChecked(position-1, true);	// position-1, weil hier der Header mitgezählt wird (im NaviAdapter wird er nicht mitgezählt)
	        //setTitle(mPlanetTitles[position]);
	    	drawerLayout.closeDrawer(navilistview);
	    }
	}

    // angezeigtes Fragment im Drawer selektieren (für BackStack-Back nötig)
    private void updateDrawerSelected(Fragment fragment) {
        String fragClassName = fragment.getClass().getName();

		//DualView:
		// 0: Action
		// 1: Loks
		// 2: Macros
		// 3: Test
		//SingleView: 0: Control -> Rest dadurch um eins verschoben -> +1
        int position = 3;   // Test -> passt für die Mehrzahl der Fragments

        if (fragClassName.equals(Frag_action2.class.getName()) || fragClassName.equals(Frag_action_edit.class.getName())) {
            position = 0;
        } else if (fragClassName.equals(Frag_loklist.class.getName()) || fragClassName.equals(Frag_lokdetails.class.getName())||  fragClassName.equals(Frag_lokgpiocfg.class.getName())) {
            position = 1;
        } else if (fragClassName.equals(Frag_macro_overview.class.getName()) || fragClassName.equals(Frag_macro_edit.class.getName()) || fragClassName.equals(Frag_macro_adddevice.class.getName())) {
            position = 2;
        }
        if (Basis.getDisplaymode() == Basis.DISPLAYMODE_SINGLEVIEW) { position += 1; }
        navilistview.setItemChecked(position, true);
    }


	
	private class NaviAdapter extends ArrayAdapter<Integer> {
		//private Context mContext; 
		LayoutInflater inflater;
		int rowViewResourceId;

		public NaviAdapter(Context context, int textViewResourceId, ArrayList<Integer> nlist, LayoutInflater linf) {
			super(context, textViewResourceId, nlist);
			//mContext = context;
			inflater = linf;
			rowViewResourceId = textViewResourceId;
		}

		public View getView(int position, View convertView, ViewGroup parent) {  

			View row=convertView; 

			if (row==null) { row=inflater.inflate(rowViewResourceId, parent, false); } 
			TextView text_navilistelement = (TextView)row.findViewById(R.id.text_navilistelement); 
			int fragment_id = navicommands.get(position);
			
			text_navilistelement.setText(fragment_names[fragment_id]);
			row.setTag(fragment_id);	// fragment_id im Tag abspeichern

			if (navilistview.isItemChecked(position)) 
			{ text_navilistelement.setTypeface(null, Typeface.BOLD); }
			else { text_navilistelement.setTypeface(null, Typeface.NORMAL); }
			
			return row;
		}
		
	}


	// ActionBar fragment related items display/hide
	@Override
	public void OnActionBarItemConfig(int itemID, boolean visible) {

		//TODO:Achtung, falls mehrere dieser fragments angezeigt werden -> Lösung finden (getrennte ActionButtons)

		switch (itemID) {

		case AB_ITEM_ADD:
			ab_show_add_item = visible;
			break;
			
		case AB_ITEM_DEL:
			ab_show_del_item = visible;
			break;
			
		}
		
		supportInvalidateOptionsMenu();
		
	}

	@Override
	public void OnDeviceChange(String devname, int changetype) {
		
		if (changetype == 0)
		{
			Frag_loklist f =  (Frag_loklist) getSupportFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_LOKLIST]);
			if (f != null) { f.updateLoks(); }
		}
	}
	


    private void logFragments() // TODO: Testfunktion, wieder entfernen, wenn nicht mehr benötigt!
    {
        // TODO: test - wieder weg
        String fragtest_txt;


        if (getSupportFragmentManager().getFragments() != null )
        {

        int count = getSupportFragmentManager().getFragments().size();
            if (count >0)
            {
                for (Fragment fr : getSupportFragmentManager().getFragments())
                {
                    if (fr != null)
                    {
                        fragtest_txt = "Fragment ID=" + fr.getId();
                        fragtest_txt += " Tag=" + fr.getTag();

                        if (fr.isAdded()) { fragtest_txt += " isAdded";  }
                        if (fr.isDetached()) { fragtest_txt += " isDetached";  }
                        if (fr.isHidden()) { fragtest_txt += " isHidden";  }
                        if (fr.isInLayout()) { fragtest_txt += " isInLayout";  }
                        if (fr.isRemoving()) { fragtest_txt += " isRemoving";  }
                        if (fr.isResumed()) { fragtest_txt += " isResumed";  }
                        if (fr.isVisible()) { fragtest_txt += " isVisible";  }
                    }
                    else {   fragtest_txt = "Fragment: NULL";     }

                    Log.d("Frag-Check", fragtest_txt);
                }
            }

        }
    }

    private void healFragments() // TODO: Testfunktion, wieder entfernen, wenn nicht mehr benötigt!
    {
        if (getSupportFragmentManager().getFragments() != null )
        {
            int count = getSupportFragmentManager().getFragments().size();
            if (count >0)
            {
                for (Fragment fr : getSupportFragmentManager().getFragments())
                {
                    if (fr != null)
                    {
                        if (fr.isAdded() && fr.isResumed() && !fr.isVisible())  //störende Fragments nach back, die nicht auf den Backstack gelegt wurden
                        {
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.remove(fr);
                            ft.commit();
                        }
                    }

                }
            }

        }
    }
	
}	// end class FAct_control
