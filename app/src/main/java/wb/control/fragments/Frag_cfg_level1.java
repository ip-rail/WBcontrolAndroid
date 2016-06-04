package wb.control.fragments;

import wb.control.Basis;
import wb.control.OnFragReplaceListener;
import wb.control.OnHideMnuItemListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class Frag_cfg_level1 extends ListFragment implements WBFragID {

	private static final int FRAGMENT_ID = FAct_control.FRAGMENT_CFG_LEVEL1;

	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	private String[] items;
	ArrayAdapter<String> aa;
	
	// CFG-(Daten)-Typen für ListView
	public static final int CFG_INT			= 	0;	// noch nicht verwendet - vl. Picker? ev. auch für IP (-> derzeit alles String)
	public static final int CFG_STRING		= 	1;
	public static final int CFG_ONOFF		= 	2;	// Standarmäßig checkbox verwenden (nicht ToggleButton)
	public static final int CFG_SPINNER		= 	3;
	public static final int CFG_IP			= 	4;	// String mit Abprüfung der IPV4-Adresse
	public static final int CFG_INT_FROMSTR	= 	5;	// int als String im EditText eigeben und dann umwandeln
	public static final int CFG_INT_SEEK	= 	6;	// int per seekbar auswählen
	public static final int CFG_MULTI_CHECK	= 	7;	// mehrfach check auswahl
	
	public static final int CFG_VIEWTYPE_COUNT	= 	2;	// die anzahl der verschiedenen Typen, die im Adapter.getView von cfg_level2 unterschieden werden
														// -> nur ONOFF wird direkt im ListView angezeigt, alles andere läuft über einen Dialog

    public int getFragmentID() { return FRAGMENT_ID; }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		
		items = getResources().getStringArray(R.array.config_level1);
		aa = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, items);
		this.setListAdapter(aa);
	}
	
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

    } */
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		fcontainer = container;
		View fragview = super.onCreateView(inflater, container, savedInstanceState);
		// TODO: Problem wie Test Level1 - Problem mehrfache Views?
		return fragview;
	}	// end onCreateView
	
	
	/*
	@Override
	public void onResume() {
		super.onResume();
		
	} */
	
	/*
	@Override
	public void onDestroyView() {
		super.onDestroyView();

		//setListAdapter(null);
	} */

	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Basis.setConfigfragGroup(position);	// Auswahl in der Basis zwischenspeichern
		// test
		//Log.d ("cfg1", "cfg2-Auswahl=" + position);
		fragReplListener.OnFragReplace(FAct_control.FRAGMENT_CFG_LEVEL2, true, null);

	}
}
