package wb.control.fragments;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Device;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class Frag_macro_adddevice extends Fragment
implements AdapterView.OnItemClickListener, OnLongClickListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_MACRO_ADDDEV;

	View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	EditText editText_adddev_newname;
	ListView listView_adddev_devices;
	
	ArrayList<Device> devlist;
	ArrayAdapter<Device> lvaa;	// Liste darf nur über Adapter geändert werden, wirkt sonst nicht!!


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

		devlist = new ArrayList<Device>();
				
	}	// end onCreate
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		fcontainer = container;
		fragview = inflater.inflate(R.layout.macro_adddevice, container, false);
		
		editText_adddev_newname = (EditText)fragview.findViewById(R.id.editText_adddev_newname);
		//editText_adddev_newname.setText(macroname);
		editText_adddev_newname.setOnLongClickListener(this);
		
		listView_adddev_devices = (ListView)fragview.findViewById(R.id.listView_adddev_devices);
		listView_adddev_devices.setOnItemClickListener(this);
		
		//Achtung: derzeit: alle devices - bereits enthaltene sollten entfernt werden!!!!
		lvaa=new ArrayAdapter<Device>(getActivity(), android.R.layout.simple_list_item_1, devlist);
		listView_adddev_devices.setAdapter(lvaa);
		
		return fragview;
	}	// end onCreateView

	
	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		super.onResume();
		
		lvaa.clear();
			// TODO 
		if (Basis.getApiLevel() >= 11) { lvaa.addAll(Basis.getDevicelist()); }
		else { for (Device d : Basis.getDevicelist()) { lvaa.add(d); } }
		editText_adddev_newname.setText("");
	}
	

	@Override
	public void onItemClick(AdapterView<?> view, View arg1, int position, long id) {
		// Name des ausgewählten Devices zurückmelden
		((FAct_control) getActivity()).set_macro_newdevice(view.getItemAtPosition(position).toString());
		fragReplListener.OnFragReplace(FAct_control.FRAGMENT_MACRO_EDIT, false, null);
	}


	@Override
	public boolean onLongClick(View v) {

		int id = v.getId();
		if (id == R.id.editText_adddev_newname) {
			((FAct_control) getActivity()).set_macro_newdevice(editText_adddev_newname.getText().toString());
			fragReplListener.OnFragReplace(FAct_control.FRAGMENT_MACRO_EDIT, true, null);
			return true;
		} else {
			return false;
		}
	}
}
