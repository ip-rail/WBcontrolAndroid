package wb.control.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Device;
import wb.control.OnActionBarConfigListener;
import wb.control.OnActionBarItemListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_yes_no;

public class Frag_lokdetails extends Fragment implements OnItemClickListener, DialogFrag_yes_no.OnyesnoDialogListener, OnActionBarItemListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_LOKDETAILS;

	// Lok Daten ID = Array index von lokdata text (siehe: Ressourcen: values/arrays)
	static final int LOKDATA_NAME			= 0;	// Lok-Name
	static final int LOKDATA_IP				= 1;	// IP-Adresse
	static final int LOKDATA_OWNER			= 2;	// Besitzer
	static final int LOKDATA_SPEEDSTEPS		= 3;	// Speedstufen
	static final int LOKDATA_RANGIERMAX		= 4;	// Rangierspeed Maximalwert
	static final int LOKDATA_SW				= 5;	// Loksoftware
	static final int LOKDATA_PROTOCOL		= 6;	// Protokoll
	static final int LOKDATA_PWMF			= 7;	// Motor-PWM-Frequenz
	static final int LOKDATA_MACROCMD		= 8;	// Macro Konfiguration
	static final int LOKDATA_ALIVECHECK		= 9;	// Alive-Check (ein/ausschalten)
    static final int LOKDATA_GPIO_CFG		= 10;	// GPIO Konfiguration
	static final int LOKDATA_HARDWARE		= 11;	// GPIO Konfiguration

	// Lok Daten view type
	static final int LOKDATA_VTYPE_STD		= 0;	// f_loks2_row_std (lokdata_name+lokdata_value)

	static final int LOKDATA_VTYPE_COUNT	= 1;	// anzahl der view typen für Adapter

	OnFragReplaceListener fragReplListener;
	OnActionBarConfigListener aBarConfigListener;
	View fragview;	// Root-View für das Fragment
	Device showDevice;	// Das Device, von dem die Daten angezeigt werden
	LayoutInflater layoutInflater;
	LokDataAdapter loka;
	ListView listView_lokdetails;
	ImageView imageView_lokdata,imageView_lokdata_online, imageView_lokdata_man;
	TextView textView_lokdata,textView_lokdata_online, textView_lokdata_man;
	View targetView; // speichert den View, von dem aus das aktuelle ContextMenu gestartet wurde
	Uri tempUri;	// Zwischenspeicher für mit der Kamera aufzunehmende Bilder
	BroadcastReceiver locBcReceiver;
	IntentFilter devifilter;

	static final int GALLERY_IMAGE_REQUEST = 1;
	static final int CAMERA_IMAGE_REQUEST = 2;
	static final int IMAGE_EDIT_REQUEST = 3;


    public int getFragmentID() { return FRAGMENT_ID; }

    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        try {
            fragReplListener = (OnFragReplaceListener) c;
        } catch (ClassCastException e) {
            throw new ClassCastException(c.getPackageName() + " must implement OnFragReplaceListener");
        }

        try {
            aBarConfigListener = (OnActionBarConfigListener) c;
        } catch (ClassCastException e) {
            throw new ClassCastException(c.getPackageName() + " must implement OnActionBarConfigListener");
        }
    }



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // lokales Broadcasting einrichten
		devifilter = new IntentFilter();
		devifilter.addAction(Basis.ACTION_DEVICE_NAME_CHANGED);


		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

				if (intent.getAction().equals(Basis.ACTION_DEVICE_NAME_CHANGED))
				{
					String devname = intent.getStringExtra("device");
					if (showDevice != null)
					{
						if (devname.equals(showDevice.getName())) { loka.notifyDataSetChanged(); }   //TODO: testen!!!
					}
				}

			}
		};

	}	// end onCreate

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		layoutInflater = inflater;
		fragview = inflater.inflate(R.layout.f_lokdetails, container, false);
		listView_lokdetails = (ListView)fragview.findViewById(R.id.listView_lokdetails);

		loka = new LokDataAdapter(getResources().getStringArray(R.array.lokdata_name), getResources().getIntArray(R.array.lokdata_vtype), getResources().getIntArray(R.array.lokdata_order));
		listView_lokdetails.setAdapter(loka);
		listView_lokdetails.setOnItemClickListener(this);

		imageView_lokdata = (ImageView)fragview.findViewById(R.id.imageView_lokdata);
		imageView_lokdata_online = (ImageView)fragview.findViewById(R.id.imageView_lokdata_online);
		imageView_lokdata_man = (ImageView)fragview.findViewById(R.id.imageView_lokdata_man);

		textView_lokdata = (TextView)fragview.findViewById(R.id.textView_lokdata); //TODO: wird der text überhaupt gebraucht?
		textView_lokdata_online = (TextView)fragview.findViewById(R.id.textView_lokdata_online);
		textView_lokdata_man = (TextView)fragview.findViewById(R.id.textView_lokdata_man);

		registerForContextMenu(imageView_lokdata);

		return fragview;

	}	// end onCreateView


	/*
	@Override
	public void onStart() {
		super.onStart();

	} */

	@Override
	public void onResume() {

        super.onResume();
        fresume();
	}

	/*
	@Override
	public void onDestroyView() {
		super.onDestroyView();

	}  */

	@Override
	public void onPause() {

        fpause();
		super.onPause();
	}


    private void fresume() {

        if (Basis.getShowDevice() != null) { setDeviceToDisplay(Basis.getShowDevice()); }
        aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_DEL, true);	// ActionBar Button "delete Item" anzeigen
    }


    private void fpause() {

        aBarConfigListener.OnActionBarItemConfig(FAct_control.AB_ITEM_DEL, false);	// ActionBar Button "delete Item" verstecken
    }



	// ContextMenü für Foto-Management
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		//super.onCreateContextMenu(menu, v, menuInfo);
		targetView = v;
		menu.clear();	// Menu leeren!!
		MenuInflater inflater = new MenuInflater(getActivity().getApplicationContext());
		inflater.inflate(R.menu.menu_lokpic_context, menu);
		//menu.removeItem(R.id.menui_getpic_camera);	// entfernen, falls keine Kamera vorhanden ist
		// R.id.menui_lokpic_edit entfernen, wenn die Bild-uri mit "android.resource" beginnt

        // TODO: pics holen/bearbeiten ändern -> voerst gesperrt
        MenuItem item = menu.findItem( R.id.menui_lokpic_camera);
        item.setEnabled(false);
        item = menu.findItem( R.id.menui_lokpic_gallery);
        item.setEnabled(false);
        item = menu.findItem( R.id.menui_lokpic_edit);
        item.setEnabled(false);

		if (showDevice != null)
		{
			Uri lokImage = showDevice.getPicUri();
			if (lokImage != null) 
			{	
				if (lokImage.getScheme().equals("android.resource")) { menu.removeItem(R.id.menui_lokpic_edit); } 
			}
		}
	}

	public void onContextMenuClosed (Menu menu)
	{
		targetView = null;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		if (targetView.getId() != R.id.imageView_lokdata)	{ return false; }

		switch(item.getItemId())
		{
		case R.id.menui_lokpic_camera:
			Intent camPhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			tempUri = Basis.newPictureUri();
			camPhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
			startActivityForResult(camPhotoIntent, CAMERA_IMAGE_REQUEST);
			return true;

		case R.id.menui_lokpic_gallery:
			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
			photoPickerIntent.setType("image/*");
			// This flag clears the called app from the activity stack, so users arrive in the expected
			// place next time this application is restarted.
			photoPickerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivityForResult(photoPickerIntent, GALLERY_IMAGE_REQUEST);
			return true;

		case R.id.menui_lokpic_edit:
			Intent photoEditIntent = new Intent(Intent.ACTION_EDIT);	// funkt. nur mit Photoshop Express
			//photoEditIntent.setType("image/*");
			//photoEditIntent.setData(showDevice.getPicUri());
			photoEditIntent.setDataAndType(showDevice.getPicUri(), "image/jpeg");
			// This flag clears the called app from the activity stack, so users arrive in the expected
			// place next time this application is restarted.
			//photoEditIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivityForResult(photoEditIntent, IMAGE_EDIT_REQUEST);
			return true;

		default:
			return super.onContextItemSelected(item);
		}

	}	// end  onContextItemSelected


	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {

		case GALLERY_IMAGE_REQUEST:
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedImage = data.getData();
				if (selectedImage != null)
				{
					showDevice.setPicUri(selectedImage);
					//imageView_lokdata.setImageURI(selectedImage);
					loadBitmap(selectedImage, imageView_lokdata);
					showDevice.saveUriPic(selectedImage, showDevice.getName(), 3);	// in DB speichern
				}
			}
			break;

		case CAMERA_IMAGE_REQUEST:
			if (resultCode == Activity.RESULT_OK) {

				if (tempUri != null)
				{
					addFiletoGallery(tempUri);	// muss für content-uri zuerst zur Gallery hinzugefügt werden (asynchron, dauert etwas)
				}	
			}
			break;

		case IMAGE_EDIT_REQUEST:	// nach Ende von Photoshop Express oä.

			if (data != null)
			{
				Uri editedUri = data.getData();
				String scheme = data.getScheme();
				if (scheme.equals("content")) { setCameraPic(editedUri); }
				else if (scheme.equals("file")) 
				{
					tempUri = editedUri;
					if (tempUri != null) { addFiletoGallery(tempUri); }	// muss für content-uri zuerst zur Gallery hinzugefügt werden (asynchron, dauert etwas)
				}
			}
			break;
		}
	}

	public void setCameraPic(Uri contentUri)
	{
		showDevice.setPicUri(contentUri);
		//imageView_lokdata.setImageURI(contentUri);
		loadBitmap(contentUri, imageView_lokdata);
		showDevice.saveUriPic(contentUri, showDevice.getName(), 3);	// in DB speichern
		tempUri = null;	// jetzt löschen
	}


	// Lokbilder, die mit der Kamera geschossen werden, liegen nur als file-uri vor. Sie sind nicht 
	// in der Galerie vorhanden und haben daher keine content-uri. Das alles wird per MediaScan erledigt.
	public void addFiletoGallery(Uri fileUri)
	{
		new GalleryScanner(this.getActivity(), fileUri);
	}


	public class GalleryScanner implements MediaScannerConnectionClient {

		private MediaScannerConnection msc;
		private Uri fileUri;

		public GalleryScanner(Context context, Uri fu) {
			fileUri = fu;
			msc = new MediaScannerConnection(context, this);
			msc.connect();
		}

		@Override
		public void onMediaScannerConnected() {

			String fpath = fileUri.getPath();
			msc.scanFile(fpath, null);
		}

		@Override
		public void onScanCompleted(String path, Uri uri) {
			// Problem: onScanCompleted() läuft offenbar (Exception) nicht in UI-Thread -> darf nicht auf views zugreifen
			// Lösung: runOnUiThread()
			final Uri newUri = uri;

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					setCameraPic(newUri);	// Bild jetzt für aktuelles Device setzen (ACHTUNG: könnte schon geändert sein??)
				}
			});

			msc.disconnect();
		}
	}



	public void setDeviceToDisplay(Device d)
	{
		showDevice = d;
		// Lokbild laden

		Uri lokImage = showDevice.getPicUri();
		imageView_lokdata.setTag(d.getName());	// Kennzeichnung für's verzögerte Laden
		if (lokImage != null) { loadBitmap(lokImage, imageView_lokdata); }
		textView_lokdata.setText(d.getName());

		if (d.isConnected()) 
		{
			imageView_lokdata_online.setBackgroundResource(R.color.loklist_online);
			textView_lokdata_online.setText(R.string.gen_connected);
		}
		else
		{
			imageView_lokdata_online.setBackgroundResource(R.color.loklist_offline);
			textView_lokdata_online.setText(R.string.get_not_connected);
		}

		if (d.getIsUserCreated())
		{
			imageView_lokdata_man.setVisibility(View.VISIBLE);
			textView_lokdata_man.setVisibility(View.VISIBLE);
			textView_lokdata_man.setText(R.string.lokdata_man_lok);
		}
		else
		{
			imageView_lokdata_man.setVisibility(View.GONE);
			textView_lokdata_man.setVisibility(View.GONE);	// TODO: sinnvollen text ausgeben? 
		}


		loka.setStdLokItems();
	}


	private void loadBitmap(Uri uri, ImageView imageView) {

		/* TODO: komplett ändern, funkt. wg. der neuen Permissions nicht -> neues cache-system, uris können nicht mehr verwendet werden
		if (!uri.getScheme().equals("android.resource")) // nur ausführen, wenn es keine Dummy-Bild-Resource ist (sondern eine Datei)
		{
			final String imagepath = Basis.getPathFromUri(uri);

			final Bitmap bitmap = Basis.getBitmapFromMemCache(imagepath);	//Todo: läuft am Nexus7 5.1.1 nicht mehr
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


	// LokDataAdapter

	class LokDataAdapter extends BaseAdapter implements ListAdapter {
		String[] Title;
		int[] vtype;	// view type der daten
		int[] order;	// Anzeige-Reihenfolge der Standard-Daten (es könnte noch was hinzugefügt werden)
		ArrayList<Integer> lokData;	// hier ist die Lok Daten ID gespeichert
		TextView textView_lokdata_name, textView_lokdata_value;

		public LokDataAdapter(String[] text, int[] typ, int[] ord) {

			lokData = new ArrayList<Integer>();
			Title = text;
			vtype = typ;
			order = ord;
		}

		public void addDataItem(final int item) {
			lokData.add(item);
			notifyDataSetChanged();
		}

		public void setStdLokItems()
		{
			lokData.clear();
			for (int a=0;a<order.length;a++) { lokData.add(order[a]); }
			notifyDataSetChanged();	
		}


		@Override
		public int getItemViewType(int position) {
			return vtype[lokData.get(position)];
		}

		@Override
		public int getViewTypeCount() {
			return LOKDATA_VTYPE_COUNT;
		}

		public int getCount() {
			return lokData.size();
		}

		public Object getItem(int position) {
			return lokData.get(position);
		}

		public int getDataId(int position) {
			return lokData.get(position);
		}

		public long getItemId(int position) {
			return position;
		}


        public void updateView(int lokdataid) { // Daten eines Views updaten (nur für sichtbare sinnvoll und möglich)

            int index = -1;

            for (int i = 0; i < lokData.size(); i++) {
                if (lokData.get(i) == lokdataid) {
                    index = i;
                    break;
                }
            }

            if (index == -1) { return; }

            View v = listView_lokdetails.getChildAt(index - listView_lokdetails.getFirstVisiblePosition());

            if (v == null) { return; }

            setViewData(index, v);
        }



		public View getView(int position, View convertView, ViewGroup parent) {

			int type = getItemViewType(position);

			if (convertView==null) 
			{ 
				switch(type)
				{
				case LOKDATA_VTYPE_STD:
					convertView=layoutInflater.inflate(R.layout.f_lokdetails_row_std, parent, false); 
					break;
				}
			}

			setViewData(lokData.get(position), convertView);

			return convertView;
		}


		private void setViewData(int data, View v)
		{

			textView_lokdata_name  = (TextView) v.findViewById(R.id.textView_lokdata_name);
			textView_lokdata_value = (TextView) v.findViewById(R.id.textView_lokdata_value);

			textView_lokdata_name.setText(Title[data]);

			switch(data)	// Lok Daten ID == array index von lokdata_name
			{
			case LOKDATA_NAME:
				textView_lokdata_value.setText(showDevice.getName());
				// TODO größere Schriftgröße

				break;

			case LOKDATA_IP:
				textView_lokdata_value.setText(showDevice.getIP());

				break;

			case LOKDATA_OWNER:
				textView_lokdata_value.setText(showDevice.getDev_owner());

				break;

			case LOKDATA_SPEEDSTEPS:
				textView_lokdata_value.setText(String.valueOf(showDevice.getSpeedsteps()));

				break;

			case LOKDATA_RANGIERMAX:
				textView_lokdata_value.setText(String.valueOf(showDevice.getRangiermax()));

				break;

			case LOKDATA_SW:
				textView_lokdata_value.setText(showDevice.getDev_swname() + " " + showDevice.getDev_swversion());

				break;

			case LOKDATA_PROTOCOL:
				textView_lokdata_value.setText(showDevice.getDev_protocol().name());

				break;

			case LOKDATA_PWMF:
				textView_lokdata_value.setText(String.valueOf(showDevice.getLok_pwmfrequenz()));
				break;

			case LOKDATA_ALIVECHECK:
				String lifetxt;
				if (showDevice.isCheckLifesign()) {  lifetxt = getResources().getString(R.string.gen_yes); 	}
				else { lifetxt = getResources().getString(R.string.gen_no); }
				textView_lokdata_value.setText(lifetxt);
				break;

			case LOKDATA_MACROCMD:
				textView_lokdata_value.setText("");
				break;

            case LOKDATA_GPIO_CFG:
                textView_lokdata_value.setText("");
                break;

            case LOKDATA_HARDWARE:
                    textView_lokdata_value.setText(showDevice.getHardwareNames());
                    break;
			}
		}
	}



	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (parent.getId() == R.id.listView_lokdetails) {
            switch (loka.getDataId(position))    // Lok Daten ID == array index von lokdata text
            {
                case LOKDATA_ALIVECHECK:
                    DialogFrag_yes_no ynfrag = new DialogFrag_yes_no();
                    Bundle args = new Bundle();
                    args.putString("title", getString(R.string.lokdata_alivechk_title));
                    args.putString("msg", getString(R.string.lokdata_alivechk_msg));
                    args.putInt("typ", DialogFrag_yes_no.DIALOG_YN_ALIVECHECK);    // über die "Lok Daten ID" werden die in diesem Fragment verwendeten yes-no Dialoge unterschieden
                    ynfrag.setArguments(args);
                    ynfrag.setOnyesnoDialogListener(this);
                    ynfrag.show(getFragmentManager(), "alivecheckdialog");
                    break;

                case LOKDATA_GPIO_CFG:
                    Bundle args1 = new Bundle();
                    args1.putString("device", showDevice.getName());
					fragReplListener.OnFragReplace(FAct_control.FRAGMENT_LOKGPIOCFG, true, args1);
                    break;


            }
        } // end istView_lokdetails

    }



	// *************************************** Dialoge **********************************************

	// Auswertung ja/nein Dialog:

	@Override
	public void OnyesnoDialog(int typ, Boolean antwort) {

		switch(typ)
		{
		case DialogFrag_yes_no.DIALOG_YN_ALIVECHECK:
			showDevice.setCheckLifesign(antwort);
			loka.notifyDataSetChanged();
			break;
		}


	}



	@Override
	public void OnActionBarItem(int itemID) {

		// delete Lok
		if (itemID == FAct_control.AB_ITEM_DEL)
		{
			if (showDevice != null)
			{
				//TODO: Sicherheitsabfrage: Sind sie sicher?
				//TODO: aktuell gesteuertes Device nicht löschen, besonders wenn es verbunden ist, da nur Warnung ausgeben!
				Basis.RemoveDevice(showDevice);
				Basis.setShowDevice(null);
				fragReplListener.OnFragReplace(FAct_control.FRAGMENT_LOKLIST, true, null);	// zur Übersicht zurückkehren
			}
		}

	}


}	// End class Frag_loks
