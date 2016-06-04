package wb.control.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGBuilder;

import java.util.ArrayList;
import java.util.Collections;

import wb.control.Basis;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_ChooseFromList;
import wb.control.views.TrackView;
import wb.control.views.TrackView.TrackRenderThread;

public class Frag_TrackPlanTest  extends Fragment 
implements View.OnClickListener, DialogFrag_ChooseFromList.OnTextFromDialogListListener, WBFragID {

	private static final int FRAGMENT_ID = FAct_control.FRAGMENT_TRACKS_TEST;

	private View fragview;	// Root-View für das Fragment
	private TrackView trackView_test;
	private TextView textView_track_title;
	private Button button_track1, button_track_actions;
	private LinearLayout linearLayout_tools;
	
	//private SurfaceHolder sholder;
	private LocalBroadcastManager locBroadcastMgr;
	private TrackRenderThread trackThread;
	private String trackplan_name;
	private static Handler uihandler;
	private Boolean editmode = false;
	private Boolean toolpics_not_ready = true;	// tool-Icons müssen noch hinzugefügt werden
	
	private ArrayList<ImageView> iv_list;	// zum leichteren Markieren des aktuellen ToolIcons
	private int last_tool;	// das letzte markierte tool merken
	private Drawable select_drawable;
	
	public static final int MSG_TXT 	= 	1;
	
	// references to the images
    
	private int[] toolpic_IDs = new int[] { R.raw.te00, R.raw.te01, R.raw.te02, R.raw.te03, 
    		R.raw.te04, R.raw.te05, R.raw.te06, R.raw.te07, R.raw.te08, R.raw.te09, 
    		R.raw.te10, R.raw.te11, R.raw.te12, R.raw.te13, R.raw.te14, R.raw.te15, 
    		R.raw.te16, R.raw.te17, R.raw.te18, R.raw.te19, R.raw.te20, R.raw.te21, R.raw.te22, R.raw.android};


    public int getFragmentID() { return FRAGMENT_ID; }


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locBroadcastMgr = LocalBroadcastManager.getInstance(getActivity());
		
		uihandler=new Handler() { 
		    @Override 
		    public void handleMessage(Message msg) { 
		    	CheckMsg(msg);	// Message auswerten -> UI-Aktionen // TODO: handler
		    } 
		  };
			
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		fragview = inflater.inflate(R.layout.f_trackplantest, container, false);
		trackView_test = (TrackView)fragview.findViewById(R.id.trackView_test);
		trackThread = trackView_test.getThread();
		
		textView_track_title = (TextView)fragview.findViewById(R.id.textView_track_title);
		
		button_track1 = (Button)fragview.findViewById(R.id.button_track1);
		button_track1.setOnClickListener(this);
		
		button_track_actions = (Button)fragview.findViewById(R.id.button_track_actions);
		button_track_actions.setOnClickListener(this);
		
		linearLayout_tools = (LinearLayout)fragview.findViewById(R.id.linearLayout_tools);
		linearLayout_tools.setVisibility(View.GONE);

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

        trackplan_name = Basis.getTrackplanName();
        if (trackplan_name == null) { trackplan_name = ""; }
        if (trackplan_name.equals(""))
        {
            chooseTrackplan(DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLANS);
        }
        else
        {
            trackView_test.loadPlan(trackplan_name);
        }
    }


    private void fpause() {

        Basis.setTrackplanName(trackView_test.getPlanName());
        trackView_test.savePlan();
    }





	@Override
	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.button_track1) {
			if (trackThread != null)
			{
				editmode = !editmode;
				trackThread.setEditMode(editmode);
				if (editmode) 
				{ 
					if (toolpics_not_ready)
					{
						addTrackToolIcons();	// die Tool-Icons ins LinearLayout einfügen
						
					}
					linearLayout_tools.setVisibility(View.VISIBLE); 
				}
				else { linearLayout_tools.setVisibility(View.GONE); }
			}
			// Tool-Leiste sichtbar machen / verstecken
			if (editmode) { locBroadcastMgr.sendBroadcast(new Intent(Basis.ACTION_QA_TRACK_EDIT_ON)); }
			else { locBroadcastMgr.sendBroadcast(new Intent(Basis.ACTION_QA_TRACK_EDIT_OFF)); }
		} else if (id == R.id.button_track_actions) {
			ActionOptions();
		}
	}


	// ####### Auswertungsfunktion für UI - MessageHandler ##############################
	void CheckMsg(Message msg) {
		switch (msg.what) {

		case MSG_TXT:

			Bundle b = msg.getData();
			String infotxt = b.getString("infotxt");
			textView_track_title.setText(infotxt);

			break;
		}
	}

	
	
	public void chooseTrackplan(int dialogtyp)
	{
		ArrayList<String> items = new ArrayList<String>();
		//String name = "";
		
		SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
		Cursor c = wbDB.query(Basis.WB_DB_TABLE_TRACKPLANS, new String[] {"name"}, null, null, null, null, null);
		
		int count = c.getCount();
		if (count > 0)
		{
			if (c.moveToFirst()) {
				do {
					items.add(c.getString(0));	// Name des TrackPlans
					
				} while (c.moveToNext());
			}
			
			if (dialogtyp == DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLANS)
			{
				items.add(getResources().getString(R.string.trackplan_dialog_planname_new));	// neuer Plan
			}
			
			DialogFrag_ChooseFromList dialog = new DialogFrag_ChooseFromList();
			Bundle args = new Bundle();
			args.putInt("dialogtype", dialogtyp);
			
			
			switch (dialogtyp) {
			
			case DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLANS:	// Trackplan Auswahl
				args.putString("name", getResources().getString(R.string.trackplan_dialog_planname_title));
				break;
				
			case DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLAN_DEL:	// Trackplan-Löschen Auswahl
				args.putString("name", getResources().getString(R.string.trackplan_dialog_plandel_title));
				break;
				
			}
			
			args.putStringArrayList("list", items);
			dialog.setArguments(args);
			dialog.setOnTextFromDialogListListener(this);
			dialog.show(getFragmentManager(), "cfldialog");
		}
		else { trackView_test.newPlan(); }	// neuen Trackplan anlegen
		
		if ((c != null) && (!c.isClosed())) { c.close(); }
		wbDB.close();
		
	}
	
	
	public void ActionOptions()
	{
		ArrayList<String> items = new ArrayList<String>();
		String[] it = getResources().getStringArray(R.array.ftrackplan_actions);
		Collections.addAll(items, it);
		
		DialogFrag_ChooseFromList dialog = new DialogFrag_ChooseFromList();
		Bundle args = new Bundle();
		args.putInt("dialogtype", DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLAN_ACT);
		args.putString("name", getResources().getString(R.string.trackplan_dialog_actions_title));
		args.putStringArrayList("list", items);
		dialog.setArguments(args);
		dialog.setOnTextFromDialogListListener(this);
		dialog.show(getFragmentManager(), "cfldialog");
	}
	
	private void addTrackToolIcons()
	{
		ImageView imageView;
		
		iv_list = new ArrayList<ImageView>();

		for (int i=0; i<toolpic_IDs.length;i++)
		{
			imageView = new ImageView(getActivity());
			//imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(8, 8, 8, 8);
			imageView.setAdjustViewBounds(true);
			imageView.setMaxHeight(90);
			imageView.setMaxWidth(90);
			imageView.setTag(i);	// ImageID als Tag speichern, sodass jedes Bild identifiziert weden kann
			//imageView.setImageResource(toolpic_IDs[i]);
			imageView.setId(i+100);	// IDs <= 100 sind TrackToolIcons
			imageView.setOnClickListener(this);
			// Drawable d = (SVGParser.getSVGFromResource(getActivity().getResources(), toolpic_IDs[i])).createPictureDrawable();  // old svg library

            SVG svg = new SVGBuilder()
                    .readFromResource(getActivity().getResources(), toolpic_IDs[i])
                    .build();

            Drawable d = svg.getDrawable();

			imageView.setImageDrawable(d);
			//Picture p = SVGParser.getSVGFromResource(getActivity().getResources(), toolpic_IDs[i]).getPicture();

			linearLayout_tools.addView(imageView);
			iv_list.add(imageView);

			imageView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					int index = (int) v.getTag();
					trackView_test.setEditTool(index);
					selectTool(index);
					deselectTool(last_tool);
					last_tool = index;
				}
			});
		}
		
		// SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.selektor); // old svg library
		// select_drawable = svg.createPictureDrawable();

		/*
        SVG svg = new SVGBuilder()
                .readFromResource(getResources(), R.raw.selektor)
                .build(); */

        //Drawable d = svg.getDrawable();

		toolpics_not_ready = false;	//Aufgabe als erledigt markieren
	}
	
	public void selectTool(int toolnr)
	{
		ImageView iv = iv_list.get(toolnr);
		iv.setBackgroundColor(Color.RED);
		//iv.setBackgroundDrawable(select_drawable);
		iv.setAlpha(128);
		
		
	}
	
	public void deselectTool(int toolnr)
	{
		ImageView iv = iv_list.get(toolnr);
		iv.setBackgroundDrawable(null);
		iv.setAlpha(255);
	}

	@Override
	public void OnTextFromDialogList(String text, int nummer, int typ) {
		
		switch (typ) {

		case DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLANS:	// Trackplan Auswahl

			if (text.equals(getResources().getString(R.string.trackplan_dialog_planname_new)) || text.equals(""))
			{
				trackView_test.newPlan();	// neuen (leeren) Trackplan öffnen
			}
			else { trackView_test.loadPlan(text); }
			break;
			
		case DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLAN_ACT:	// TrackPlan Aktionen
			
			switch (nummer) {
			
			case 0:	// speichern
				trackView_test.savePlan();
				break;
				
			case 1:	// laden
				chooseTrackplan(DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLANS);
				break;
				
			case 2:	// neuer Plan
				trackView_test.newPlan();
				break;
				
			case 3:	// einen Plan zum Löschen auswählen
				chooseTrackplan(DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLAN_DEL);
				break;
			
			}
			break;
			
		case DialogFrag_ChooseFromList.DIALOG_CFL_TRACKPLAN_DEL:	// TrackPlan Aktionen
			trackView_test.deletePlan(text);
			break;

		}
			
	}
	

}	// end Fragment
