package wb.control.fragments;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGBuilder;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.OnConfigChangedListener;
import wb.control.OnFragReplaceListener;
import wb.control.QAelement;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;



public class Frag_quickaction extends Fragment 
implements View.OnClickListener, OnItemClickListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_QUICKACTION;

	// eindeutige ID der QA-Buttons
	public static final int QA_CONTROL		= 	0;	// nur bei SINGLE_VIEW
	public static final int QA_ACTION		= 	1;
	public static final int QA_CHANGESIZE	= 	2;	// nur bei DUALVIEW, MULTIVIEW: Anzeigebereich 2 vergrößern, die anderen verschwinden lassen
	// ID der TrackEdit Tools ab 100

	private View fragview;	// Root-View für das Fragment
	private OnFragReplaceListener fragReplListener;
	private OnConfigChangedListener changeDispListener;
	private ArrayList<QAelement> qalist;
	
	private LocalBroadcastManager locBroadcastMgr;
	private BroadcastReceiver locBcReceiver;
	private IntentFilter ifilter;
	
	private LinearLayout linearLayout_qa;
	//private GridView gridView_track_tools;
	//private ToolsAdapter ta;
	
	private int[] toolpic_IDs = new int[] { R.raw.te00, R.raw.te01, R.raw.te02, R.raw.te03, 
    		R.raw.te04, R.raw.te05, R.raw.te06, R.raw.te07, R.raw.te08, R.raw.te09, 
    		R.raw.te10, R.raw.te11, R.raw.te12, R.raw.te13, R.raw.te14, R.raw.te15, 
    		R.raw.te16, R.raw.te17, R.raw.te18, R.raw.android };


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
        	changeDispListener = (OnConfigChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnChangeDisplayModeListener");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Empfang für lokales Broadcasting einrichten - für WLAN Aktivierung/Deaktivierung, ActionElement-Aktualisierung
        locBroadcastMgr = LocalBroadcastManager.getInstance(getActivity());

        ifilter = new IntentFilter();
        ifilter.addAction(Basis.ACTION_QA_TRACK_EDIT_ON);
        ifilter.addAction(Basis.ACTION_QA_TRACK_EDIT_OFF);

        locBcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Basis.ACTION_QA_TRACK_EDIT_ON)) {
                    // TODO: trackTools_on();
                }

                if (intent.getAction().equals(Basis.ACTION_QA_TRACK_EDIT_OFF)) {
                    // TODO: trackTools_off();
                }
            }
        };

    }    // end onCreate


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		qalist = Basis.getQuickActionlist();

		// nur für Version mit alten Geräten ohne Actionbar
		if (Basis.getApiLevel() < 7) // wenn Android Version kleiner ist als 2.1 (-> dann gibt es keine ActionBar mit appcompat)
		{	
			if ((Basis.getDisplaymode() == Basis.DISPLAYMODE_SINGLEVIEW) || (Basis.getForceDisplaymode() == 1))
			{
				Basis.AddQuickActionElement(new QAelement(getString(R.string.b_control), QA_CONTROL));
			}
			Basis.AddQuickActionElement(new QAelement(getString(R.string.b_action), QA_ACTION));	// nur, wenn ID noch nicht vorhanden
		}
		
		if (!((Basis.getDisplaymode() == Basis.DISPLAYMODE_SINGLEVIEW) || (Basis.getForceDisplaymode() == 1)))
		{
			Basis.AddQuickActionElement(new QAelement(getString(R.string.b_changesize), QA_CHANGESIZE));	// nur bei mehrspaltigem Layout
		}
		
		fragview = inflater.inflate(R.layout.quickaction, container, false);
		linearLayout_qa = (LinearLayout)fragview.findViewById(R.id.linearLayout_qa);
		
		for (QAelement q : qalist)
    	{
			View v = linearLayout_qa.findViewById(q.ID);			
    		if (v == null) { addButton(q.ID); }
    	}
	
		return fragview;
    }	// end onCreateView

	
	@Override
	public void onResume() {
		super.onResume();

		locBroadcastMgr.registerReceiver(locBcReceiver, ifilter); // localBraodcast-Empfang aktivieren

	}
	
	
	@Override
	public void onPause() {
		super.onPause();

		locBroadcastMgr.unregisterReceiver(locBcReceiver);	// localBraodcast-Empfang stoppen

	}
	
	// für Buttons
	@Override
	public void onClick(View v) {
		
		if ((v.getId()>100) && (v.getId()<200))	// TrackEdit-Tools
		{

		}
		else	// QAelement
		{


			if (v.getTag() != null)
			{
				QAelement qa = (QAelement) v.getTag();

				switch(qa.ID)
				{
				case QA_ACTION:	// Fragment Action starten				
					fragReplListener.OnFragReplace(FAct_control.FRAGMENT_ACTION, true, null);
					break;

				case QA_CONTROL:				
					fragReplListener.OnFragReplace(FAct_control.FRAGMENT_CONTROL, true, null);
					break;

				case QA_CHANGESIZE:		
					changeDispListener.OnConfigChanged(FAct_control.CID_DISP_FC2_CHANGESIZE, null);
					break;
				}
			}
		}
		
	}

	
	public void addQAelement(String txt, int id)
	{
		int oldindex = Basis.getQuickActionlistIndexByID(id);
		if (oldindex == -1)	// nur, wenn ID noch nicht vorhanden
		{
			//Basis.AddQuickActionElement(new QAelement(txt, id));	// da wärde doppelt abgeprüft
			qalist.add(new QAelement(txt, id));
			addButton(id);
		}
		
		
	}
	
	void addButton(int id)
	{
		int index = Basis.getQuickActionlistIndexByID(id);
		Button b = new Button(getActivity());
		QAelement q = qalist.get(index);
		b.setText(q.Name);
		b.setId(id);
		b.setTag(q);
		b.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		b.setOnClickListener(this);
		//LinearLayout container = (LinearLayout) fragview;
		//container.addView(b);
		linearLayout_qa.addView(b);
	}
	
	// Sachen für die Track-Edit-Tools
	
	private void trackTools_on()
	{
		/*
		if (gridView_track_tools == null) // neu anlegen
		{
			gridView_track_tools = new GridView(getActivity()); 
			ta = new ToolsAdapter(getActivity());
			gridView_track_tools.setAdapter(ta);
			gridView_track_tools.setOnItemClickListener(this);
			linearLayout_qa.addView(gridView_track_tools);
		}
		
		gridView_track_tools.setVisibility(View.VISIBLE);
		*/
		
		addTrackToolIcons();
	}
	
	
	/*
	private void trackTools_off()
	{
		
		// gridView_track_tools.setVisibility(View.GONE);
	} */
	
	
	private void addTrackToolIcons()
	{
		ImageView imageView;
		
		for (int i=0; i<toolpic_IDs.length;i++)
		{
			imageView = new ImageView(getActivity());
            //imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
            imageView.setAdjustViewBounds(true);
            imageView.setMaxHeight(90);
            imageView.setMaxWidth(90);
            //imageView.setImageResource(toolpic_IDs[i]);
            imageView.setId(i+100);	// IDs <= 100 sind TrackToolIcons
            imageView.setOnClickListener(this);
            //Drawable d = (SVGParser.getSVGFromResource(getActivity().getResources(), toolpic_IDs[i])).createPictureDrawable();    // old svg library

            SVG svg = new SVGBuilder()
                    .readFromResource(getActivity().getResources(), toolpic_IDs[i])
                    .build();

            Drawable d = svg.getDrawable();


            imageView.setImageDrawable(d);
            //Picture p = SVGParser.getSVGFromResource(getActivity().getResources(), toolpic_IDs[i]).getPicture();

            linearLayout_qa.addView(imageView);
		}
	}
	
	
	
	public class ToolsAdapter extends BaseAdapter {
	    private Context mContext;

	    public ToolsAdapter(Context c) {
	        mContext = c;
	    }

	    public int getCount() {
	        return toolpic_IDs.length;
	    }

	    public Object getItem(int position) {
	        return null;
	    }

	    public long getItemId(int position) {
	        return 0;
	    }

	    // create a new ImageView for each item referenced by the Adapter
	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView imageView;
	        if (convertView == null) {  // if it's not recycled, initialize some attributes
	            imageView = new ImageView(mContext);
	            //imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
	            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
	            imageView.setPadding(8, 8, 8, 8);
	            imageView.setAdjustViewBounds(true);
	            imageView.setMaxHeight(90);
	            imageView.setMaxWidth(90);
	        } else {
	            imageView = (ImageView) convertView;
	        }

	        imageView.setImageResource(toolpic_IDs[position]);
	        return imageView;
	    }

	    // references to the images
	    
	    private int[] toolpic_IDs = new int[] { R.raw.te00, R.raw.te01, R.raw.te02, R.raw.te03, 
        		R.raw.te04, R.raw.te05, R.raw.te06, R.raw.te07, R.raw.te08, R.raw.te09, 
        		R.raw.te10, R.raw.te11, R.raw.te12, R.raw.te13, R.raw.te14, R.raw.te15, 
        		R.raw.te16, R.raw.te17, R.raw.te18, R.raw.android };
	    
	}

	// für GridView-Clicks
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		// ImageView iv = (ImageView)v; TODO was soll hier gemacht werden`?? (diese eine Zeile war nicht geremmt)
		
		// TODO gewähltes über setAlpha, setBackground markieren? (für alle anderen Alpha zurücksetzen) //
	}
	
}
