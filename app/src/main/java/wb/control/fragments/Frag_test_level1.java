package wb.control.fragments;

import wb.control.Basis;
import wb.control.OnFragReplaceListener;
import wb.control.OnHideMnuItemListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_yes_no;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Frag_test_level1 extends Fragment implements AdapterView.OnItemClickListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_TEST_LEVEL1;

	//ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
    StableArrayAdapter aa;


    public int getFragmentID() { return FRAGMENT_ID; }


    /*
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);


	} */
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            fragReplListener = (OnFragReplaceListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragReplaceListener");
        }
        /*
        try 
    	{
    		hideMnuItemListener = (OnHideMnuItemListener) activity;
    	}
    	catch (ClassCastException e) 
    	{
    		throw new ClassCastException(activity.toString() + " must implement OnHideMnuItemListener");
    	} */
    }

    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    } */


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragview = inflater.inflate(R.layout.f_test_level1, container, false);
        final ListView listviewt1 = (ListView) fragview.findViewById(R.id.listviewtestl1);

        final String[] items = getResources().getStringArray(R.array.test_level1);
        aa = new StableArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, items);
        listviewt1.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listviewt1.setAdapter(aa);
        listviewt1.setOnItemClickListener(this);

        return fragview;
	}	// end onCreateView


    /*
	@Override
	public void onResume() {
		super.onResume();

	} */
	


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,	long id) {

        if (parent.getId() == R.id.listviewtestl1)
        {
            switch (position) {

                case 0:	// Log
                    fragReplListener.OnFragReplace(FAct_control.FRAGMENT_LOG, true, null);
                    break;

                case 1:	// CamControl
                    fragReplListener.OnFragReplace(FAct_control.FRAGMENT_CAMCONTROL, true, null);
                    break;

                case 2:	// Video Test
                    fragReplListener.OnFragReplace(FAct_control.FRAGMENT_VIDEO_TEST, true, null);
                    break;

                case 3:	// Gleisplan Test
                    fragReplListener.OnFragReplace(FAct_control.FRAGMENT_TRACKS_TEST, true, null);
                    break;

                case 4:	// Stream Test
                    fragReplListener.OnFragReplace(FAct_control.FRAGMENT_STREAM_TEST, true, null);
                    break;

                case 5:	// nach Online-Update suchen

                    if (Basis.getNetworkIsConnected())	// nur wenn Netzwerk verbunden ist
                    {
                        Basis.startCheckOnlineUpdateVersion(true);	// online verfügbare Updates checken
                    }

                    break;

                case 6:	// Hardkeys
                    fragReplListener.OnFragReplace(FAct_control.FRAGMENT_HARDKEYS, true, null);
                    break;

            }
        } //

    }



	private class StableArrayAdapter extends ArrayAdapter<String> {

        ArrayList<String> list = new ArrayList<String>();

		public StableArrayAdapter(Context context, int textViewResourceId, String[] objects) {
			super(context, textViewResourceId, objects);

            list.addAll(Arrays.asList(objects));
		}

		@Override
		public long getItemId(int position) {
			//String item = getItem(position);
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

	}


	
}
