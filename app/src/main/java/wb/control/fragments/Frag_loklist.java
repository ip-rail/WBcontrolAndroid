package wb.control.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Device;
import wb.control.Device.DeviceType;
import wb.control.OnActionBarConfigListener;
import wb.control.OnActionBarItemListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_DeviceAdd;

public class Frag_loklist extends Fragment implements OnItemClickListener, OnActionBarItemListener, WBFragID
{

	private static final int FRAGMENT_ID = FAct_control.FRAGMENT_LOKLIST;

    OnActionBarConfigListener aBarConfigListener;
    View fragview;	// Root-View für das Fragment
	LayoutInflater layoutInflater;
	OnFragReplaceListener fragReplListener;
	GridView gridView_loks;
	LokAdapter la;
	BroadcastReceiver locBcReceiver;
	IntentFilter devifilter;


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

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // lokales Broadcasting einrichten
		devifilter = new IntentFilter();
		devifilter.addAction(Basis.ACTION_DEVICELIST_CHANGED);
		devifilter.addAction(Basis.ACTION_DEVICE_NAME_CHANGED);


		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Basis.ACTION_DEVICELIST_CHANGED)) 
				{
					updateLoks();
				}

				else if (intent.getAction().equals(Basis.ACTION_DEVICE_NAME_CHANGED))
				{
                    la.notifyDataSetChanged();
				}

			}
		};

	}	// end onCreate


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		layoutInflater = inflater;
		fragview = inflater.inflate(R.layout.f_loklist, container, false);

		gridView_loks  = (GridView)fragview.findViewById(R.id.gridView_loklist);

		la = new LokAdapter(getActivity());
		gridView_loks.setAdapter(la);
		gridView_loks.setOnItemClickListener(this);

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


    private void fresume()
    {
        aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_ADD, true);	// ActionBar Button "add Item" anzeigen
        la.loadLokList();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locBcReceiver, devifilter);
    }

    private void fpause()
    {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locBcReceiver);    // localBraodcast-Empfang stoppen
        aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_ADD, false);	// ActionBar Button "add Item" verstecken
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,	long id) {

		Basis.setShowDevice(la.getItem(position));
		fragReplListener.OnFragReplace(FAct_control.FRAGMENT_LOKDETAILS, true, null);

	}


	public class LokAdapter extends BaseAdapter {
		private Context mContext;
		ArrayList<Device> loklist;

		public LokAdapter(Context c) {
			mContext = c;
			loklist = new ArrayList<Device>();
		}

		public void addLok(Device lok)
		{
			loklist.add(lok);
			notifyDataSetChanged();
		}

		public void loadLokList()
		{
			loklist.clear();

			if (Basis.getDeviceListCount(DeviceType.Lok) > 0)	// wenn Loks vorhanden sind
			{
				for (Device ding : Basis.getDevicesByType(DeviceType.Lok)) { loklist.add(ding);	} // Loks suchen
			}

			notifyDataSetChanged();
		}

		public int getCount() {
			return loklist.size();
		}

		public Device getItem(int position) {
			return loklist.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}


		public void loadBitmap(Uri uri, ImageView imageView) {

			/* TODO: komplett ändern, funkt. wg. der neuen Permissions nicht -> neues cache-system, uris können nicht mehr verwendet werden
			if (!uri.getScheme().equals("android.resource")) // nur ausführen, wenn es keine Dummy-Bild-Resource ist (sondern eine Datei)
			{
				final String imagepath = Basis.getPathFromUri(uri);

				final Bitmap bitmap = Basis.getBitmapFromMemCache(imagepath);
				if (bitmap != null) {  imageView.setImageBitmap(bitmap);   } 
				else 
				{
					imageView.setImageBitmap(Basis.getStandardlokpic());
					BitmapWorkerTask task = new BitmapWorkerTask(imageView, getActivity());
					task.execute(imagepath);
				}
			}
			else	// derzeit gibt es nur das Standard-Lok-Bild als Ressource
			{
				imageView.setImageBitmap(Basis.getStandardlokpic());
			} */

            imageView.setImageBitmap(Basis.getStandardlokpic());
		}



		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView_loklist, imageView_loklist_online, imageView_loklist_man;
            TextView textView_loklist_name, textView_loklist_owner;
            ViewHelper vhelper;

			if (convertView == null) 	// if it's not recycled
			{
				convertView=layoutInflater.inflate(R.layout.f_loklist_element, parent, false); 

				imageView_loklist = (ImageView) convertView.findViewById(R.id.imageView_loklist);
				imageView_loklist_online = (ImageView) convertView.findViewById(R.id.imageView_loklist_online);
				imageView_loklist_man = (ImageView) convertView.findViewById(R.id.imageView_loklist_man);
				textView_loklist_name = (TextView) convertView.findViewById(R.id.textView_loklist_name);
				textView_loklist_owner = (TextView) convertView.findViewById(R.id.textView_loklist_owner);

				vhelper = new ViewHelper();
				vhelper.tvname = textView_loklist_name;
				vhelper.tvowner = textView_loklist_owner;
				vhelper.ivlok = imageView_loklist;
				vhelper.ivonline = imageView_loklist_online;
				vhelper.ivman = imageView_loklist_man;
				convertView.setTag(vhelper);

			} 
			else
			{
				vhelper = (ViewHelper)convertView.getTag();
			}

			Device d = loklist.get(position);

			if (d != null)
			{
				vhelper.tvname.setText(d.getName());
				vhelper.tvowner.setText(d.getDev_owner());
				vhelper.ivlok.setTag(d.getName());	// imageView mit lok-Namen kennzeichnen (wegen Asynctask und den wiederverwendeten Views)
				Uri lokImage = d.getPicUri();
				if (lokImage != null) { loadBitmap(lokImage, vhelper.ivlok); }
				if (d.isConnected()) { vhelper.ivonline.setBackgroundResource(R.color.loklist_online); }
				else { vhelper.ivonline.setBackgroundResource(R.color.loklist_offline); }
				if (d.getIsUserCreated()) { vhelper.ivman.setVisibility(View.VISIBLE); }
				else { vhelper.ivman.setVisibility(View.GONE); }
			}

			return convertView;
		}
	}

	
	public class ViewHelper
	{
		public TextView tvname, tvowner;
		public ImageView ivlok, ivonline, ivman;

		public ViewHelper() {

		}
	}

	public void updateLoks()
	{
		la.loadLokList();
	}
	

	@Override
	public void OnActionBarItem(int itemID) {

		if (itemID == FAct_control.AB_ITEM_ADD)
		{
			DialogFrag_DeviceAdd devfrag = new DialogFrag_DeviceAdd();
			Bundle args = new Bundle();
			args.putInt("type", 0);
			devfrag.setArguments(args);
			devfrag.show(getFragmentManager(), "devicedialog");
		}
	}


}	// end class
