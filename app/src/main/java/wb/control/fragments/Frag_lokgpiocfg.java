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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SwitchCompat;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import wb.control.Basis;
import wb.control.Device;
import wb.control.GPIOpin;
import wb.control.GPIOport;
import wb.control.OnActionBarConfigListener;
import wb.control.OnActionBarItemListener;
import wb.control.OnFragReplaceListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_Textinput;
import wb.control.dialogfragments.DialogFrag_yes_no;

public class Frag_lokgpiocfg extends Fragment implements OnItemClickListener, AdapterView.OnItemSelectedListener, PopupMenu.OnMenuItemClickListener,
        DialogFrag_Textinput.OnTextFromDialogListener, WBFragID {

    private static final int FRAGMENT_ID            = FAct_control.FRAGMENT_LOKGPIOCFG;
    public static final int FILTER_TYPE_ALL         = 0;    // alle usable Pins anzeigen "alle"
    public static final int FILTER_TYPE_USED        = 1;    // alle vom User konfigurierten anzeigen "nur aktivierte"

	//OnFragReplaceListener fragReplListener;
	//OnActionBarConfigListener aBarConfigListener;
    private View fragview;	// Root-View für das Fragment
    private Device showDevice;	// Das Device, von dem die Daten angezeigt werden
    private LayoutInflater layoutInflater;
    private GPIOAdapter gpioa;

    private ListView listView_gpio;
    private TextView textView_gpio_title;
    private Spinner spinner_gpio_filter;

    private View popupTargetView; // speichert den View, von dem aus das aktuelle PopupMenu gestartet wurde

	//View targetView; // speichert den View, von dem aus das aktuelle ContextMenu gestartet wurde

    private int filterMode = FILTER_TYPE_ALL;

    private BroadcastReceiver locBcReceiver;
    private IntentFilter devifilter;



    public int getFragmentID() { return FRAGMENT_ID; }


    /*
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
    } */



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// lokales Broadcasting einrichten
		devifilter = new IntentFilter();
		//devifilter.addAction(Basis.ACTION_DEVICE_NAME_CHANGED);
        devifilter.addAction(Basis.ACTION_UPDATE_GPIO);
        devifilter.addAction(Basis.ACTION_DEVICE_DISCONNECTED);
        devifilter.addAction(Basis.ACTION_DEVICE_CONNECTED);


		locBcReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(Basis.ACTION_UPDATE_GPIO))
                {
                    String devname = intent.getStringExtra("device");
                    if (showDevice != null) { if (devname.equals(showDevice.getName())) { updateFilter(); }  }
                }

                if (intent.getAction().equals(Basis.ACTION_DEVICE_DISCONNECTED))
                {
                    String devname = intent.getStringExtra("device");
                    if (showDevice != null)
                    {
                        if (devname.equals(showDevice.getName())) { disableGPIOview(); }
                    }
                }

                if (intent.getAction().equals(Basis.ACTION_DEVICE_CONNECTED))
                {
                    String devname = intent.getStringExtra("device");
                    if (showDevice != null)
                    {
                        if (devname.equals(showDevice.getName())) { enableGPIOview(); }
                    }
                }

                /* TODO:
				else if (intent.getAction().equals(Basis.ACTION_DEVICE_NAME_CHANGED))
				{
					String devname = intent.getStringExtra("device");
					if (showDevice != null)
					{
						if (devname.equals(showDevice.getName())) { gpioa.notifyDataSetChanged(); }   //TODO: testen!!!
					}
				} */
			}
		};

        String devname = "";

        if (savedInstanceState != null) // falls das frag wg. ScreenRotation osw neu erstellt wurde
        {
            devname = savedInstanceState.getString("device");
            filterMode  = savedInstanceState.getInt("filter", FILTER_TYPE_ALL);
        }

        showDevice = Basis.getDevicelistObjectByName(devname);

    }	// end onCreate


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		layoutInflater = inflater;
		fragview = inflater.inflate(R.layout.f_lokgpiocfg, container, false);

        textView_gpio_title = (TextView)fragview.findViewById(R.id.textView_gpio_title);

        spinner_gpio_filter = (Spinner)fragview.findViewById(R.id.spinner_gpio_filter);
        ArrayAdapter<CharSequence> filteradapter = ArrayAdapter.createFromResource(getActivity(), R.array.gpio_filter, android.R.layout.simple_spinner_item);
        //filteradapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_gpio_filter.setAdapter(filteradapter);
        spinner_gpio_filter.setOnItemSelectedListener(this);

        listView_gpio = (ListView)fragview.findViewById(R.id.listView_gpio);

        gpioa = new GPIOAdapter();
        listView_gpio.setAdapter(gpioa);
        listView_gpio.setOnItemClickListener(this);

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

        String devname = "";

        Bundle b = ((FAct_control) getActivity()).getFramgentData(FRAGMENT_ID);
        if (b != null)
        {
            devname = b.getString("device"); // device wurde von Frag_lokdetails mitgegeben
            showDevice = Basis.getDevicelistObjectByName(devname);  // falls das Fragment recreated wurde, wurde showDevice schon im onCreate() gesetzt (beide Fälle treten nicht gleichzeitig auf)
        }

        textView_gpio_title.setText(getString(R.string.lokgpio_title, showDevice.getName()));
        spinner_gpio_filter.setSelection(filterMode);

        if (showDevice != null)
        {
            if (showDevice.isConnected()) { enableGPIOview(); }
            else {  disableGPIOview(); }
        }
	}



    /*
	@Override
	public void onPause() {


		super.onPause();
	} */


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (showDevice != null)
        {
            outState.putString("device", showDevice.getName());
        }
        outState.putInt("filter", spinner_gpio_filter.getSelectedItemPosition());
    }


    private void enableGPIOview()
    {
        if (listView_gpio != null) { listView_gpio.setEnabled(true); }
    }

    private void disableGPIOview()
    {
        if (listView_gpio != null) { listView_gpio.setEnabled(false); }
    }



    // Spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

		if (parent.getId() == R.id.spinner_gpio_filter) { setFilter(position); }
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        setFilter(FILTER_TYPE_ALL);
    }


    // GPIO-Filter (Spinner) setzen
    private void setFilter(int filterType)
    {
        if (showDevice != null)
        {
            switch (filterType) {

                case FILTER_TYPE_ALL:
                    gpioa.loadAllPins(showDevice);
                    break;

                case FILTER_TYPE_USED:
                    gpioa.loadUsedPins(showDevice);
                    break;
            }
        }
    }

    // nach Änderung der Daten Filter (mit selbem Typ) neu aktivieren
    private void updateFilter()
    {
        int filtertype = spinner_gpio_filter.getSelectedItemPosition();
        setFilter(filtertype);
    }



    class GPIOAdapter extends BaseAdapter implements ListAdapter {
		ArrayList<GPIOpin> pinlist;

		public GPIOAdapter() {
            pinlist = new ArrayList<GPIOpin>();
		}

        public void loadAllPins(Device dev)
        {
            pinlist.clear();
			pinlist.addAll(dev.getGPIOUsablePins());	// TODO: für alte Versionen umbauen?
            notifyDataSetChanged();
        }

        public void loadUsedPins(Device dev)
        {
            pinlist.clear();
			pinlist.addAll(dev.getGPIOUsedPins());
            notifyDataSetChanged();
        }

		@Override
		public int getItemViewType(int position) {
			return 1;
		}

		@Override
		public int getViewTypeCount() {	return 1;	}	// TODO: checken, ob ViewTypeCount passt!

		public int getCount() {
			return pinlist.size();
		}

		public Object getItem(int position) {
			return pinlist.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

        public View getView(int position, View convertView, ViewGroup parent) {

            ImageButton ib;
            //TextView textView_gpiorow_pinname, textView_gpiorow_functionname;
            GPIOViewHelper vhelper;
            GPIOpin pin = pinlist.get(position);

			if (convertView==null) 
			{
                convertView=layoutInflater.inflate(R.layout.f_lokgpiocfg_row_std, parent, false);

                vhelper = new GPIOViewHelper();
                vhelper.tv_pinname = (TextView) convertView.findViewById(R.id.textView_gpiorow_pinname);
                vhelper.tv_functionname = (TextView) convertView.findViewById(R.id.textView_gpiorow_functionname);
                vhelper.ib = (ImageButton) convertView.findViewById(R.id.imageButton_gpiorow_menu);
                vhelper.ib.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { showMenu(v); }
                });
                vhelper.sw = (SwitchCompat) convertView.findViewById(R.id.switch_gpiorow);
                vhelper.sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        switch (buttonView.getId())
                        {
                            case R.id.switch_gpiorow:
                                GPIOpin pin = (GPIOpin)buttonView.getTag();
                                if (pin != null) { pin.write(isChecked); }    // echte Ausgabe an MC

                                break;
                        }
                    }
                });
                convertView.setTag(vhelper);
			}
            else
            {
                vhelper = (GPIOViewHelper)convertView.getTag();
            }

            if (pin.isUsed()) { convertView.setBackgroundResource(R.color.grau1); }
            else { convertView.setBackgroundResource(R.color.grau2); }
			setViewData(pin, vhelper);

			return convertView;
		}


		private void setViewData(GPIOpin pin, GPIOViewHelper vh)
		{
            String pinname = "";

            vh.tv_functionname.setText(pin.getName());

            if (pin.getPort().getType() == GPIOport.GPIO_TYPE_ATMEGA_8BIT)
            {
                pinname = String.format(Basis.getBcontext().getString(R.string.lokgpio_pinname_atmega), pin.getPort().getName(), pin.getNumber());
            }
            vh.tv_pinname.setText(pinname);

            vh.sw.setChecked(pin.isSet());
            vh.sw.setTag(pin);

            if (showDevice.isConnected() && pin.isUsed()) { vh.sw.setEnabled(true); }
            else { vh.sw.setEnabled(false); }

            vh.ib.setTag(pin);  // Pin wird für Menü-Erstellung/Anpassung benötigt!
		}
	}

    private class GPIOViewHelper
    {
        public TextView tv_pinname, tv_functionname;
        ImageButton ib;
        SwitchCompat sw;

        public GPIOViewHelper() {
        }
    }

    // für PopupMenu
    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popupTargetView = v;    // den View merken, von dem aus das aktuelle PopupMenu gestartet wurde
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.menu_gpiocfg_item);
        GPIOpin pin = (GPIOpin)v.getTag();
        if (pin.isUsed()) { popup.getMenu().removeItem(R.id.menui_gpiocfg_use); }
        else
        {
            popup.getMenu().removeItem(R.id.menui_gpiocfg_release);
            popup.getMenu().removeItem(R.id.menui_gpiocfg_name);
        }
        popup.show();
    }

    // für PopupMenu
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menui_gpiocfg_name:
                if (popupTargetView != null)
                {
                    DialogFrag_Textinput dialogtxt = new DialogFrag_Textinput();
                    Bundle args = new Bundle();
                    args.putString("title", getString(R.string.lokgpio_namedialog_title));
                    dialogtxt.setArguments(args);
                    dialogtxt.setOnTextFromDialogListener(this);
                    dialogtxt.show(getFragmentManager(), "inputpinnamedialog");
                }
                return true;

            case R.id.menui_gpiocfg_use:
                if (popupTargetView != null)
                {
                    GPIOpin pin = (GPIOpin)popupTargetView.getTag();
                    pin.setUsed(true);
                    popupTargetView = null; // Menü-View zurücksetzen
                    gpioa.notifyDataSetChanged();
                }
                return true;

            case R.id.menui_gpiocfg_release:
                if (popupTargetView != null)
                {
                    GPIOpin pin = (GPIOpin)popupTargetView.getTag();
                    pin.setUsed(false);
                    popupTargetView = null; // Menü-View zurücksetzen
                    updateFilter();  // führt auch gpioa.notifyDataSetChanged() aus
                }
                return true;

            default:
                return false;
        }

    }


    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position,	long id) {

		if (parent.getId() == R.id.listView_gpio)
		{
            /*
            DialogFrag_yes_no ynfrag = new DialogFrag_yes_no();

				Bundle args = new Bundle();
				args.putString("title", getString(R.string.lokdata_alivechk_title));
				args.putString("msg", getString(R.string.lokdata_alivechk_msg));
				args.putInt("typ", DialogFrag_yes_no.DIALOG_YN_ALIVECHECK);	// über die "Lok Daten ID" werden die in diesem Fragment verwendeten yes-no Dialoge unterschieden
				ynfrag.setArguments(args);
				ynfrag.setOnyesnoDialogListener(this);
				ynfrag.show(getFragmentManager(), "alivecheckdialog");
				*/
		}
	}



    // DialogFrag_Textinput
    @Override
    public void OnTextFromDialog(String text) {

        // Text für frei wählbaren Pin-Namen

        if (popupTargetView != null)
        {
            GPIOpin pin = (GPIOpin)popupTargetView.getTag();
            pin.setName(text);
        }

        popupTargetView = null; // jetzt erst Menü-View zurücksetzen
    }




}	// End class Frag_loks
