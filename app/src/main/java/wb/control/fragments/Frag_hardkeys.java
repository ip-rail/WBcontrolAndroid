/*  
 * Frag_hardkeys: define special functions for the physical keys
 */


package wb.control.fragments;

import wb.control.Basis;
import wb.control.HardKey;
import wb.control.OnFragReplaceListener;
import wb.control.OnHardkeyChangedListener;
import wb.control.OnHideMnuItemListener;
import wb.control.R;
import wb.control.WBFragID;
import wb.control.activities.FAct_control;
import wb.control.dialogfragments.DialogFrag_Hardkeys;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Frag_hardkeys extends Fragment
        implements View.OnClickListener, WBFragID {

    private static final int FRAGMENT_ID = FAct_control.FRAGMENT_HARDKEYS;

    View fragview;    // Root-View für das Fragment
    ViewGroup fcontainer;
    OnFragReplaceListener fragReplListener;
    LayoutInflater layoutInflater;
    KeysAdapter keya;
    SparseArray<HardKey> hardkeys;    // Array for keya

    ListView listView_keys;
    View menutargetView; // speichert den View, von dem aus das aktuelle ContextMenu gestartet wurde
    TextView textView_title, TextView_info;

    OnHardkeyChangedListener HKChangedListener;
    BroadcastReceiver locBcReceiver;
    IntentFilter hkfilter;

    String[] function;


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
            HKChangedListener = (OnHardkeyChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnChangeDisplayModeListener");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hkfilter = new IntentFilter();
        hkfilter.addAction(Basis.ACTION_HKEY_UPDATE);

        locBcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(Basis.ACTION_HKEY_UPDATE))
                {
                    int ikeycode = intent.getIntExtra("keycode", -1);
                    if (ikeycode > -1) {
                        keya.reloadHardkey(ikeycode);
                    } else {
                        keya.reloadHardkeys();
                    }
                }
            }
        };
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        fcontainer = container;
        layoutInflater = inflater;
        fragview = inflater.inflate(R.layout.f_hardkeys, container, false);
        hardkeys = new SparseArray<HardKey>();
        textView_title = (TextView) fragview.findViewById(R.id.textView_title);
        TextView_info = (TextView) fragview.findViewById(R.id.TextView_info);
        listView_keys = (ListView) fragview.findViewById(R.id.listView_keys);
        keya = new KeysAdapter();
        listView_keys.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView_keys.setAdapter(keya);

        function = getResources().getStringArray(R.array.hardkey_function);

         return fragview;
    }    // end onCreateView

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


    /*
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onStop();
    } */


    private void fpause() {

        Basis.setHardKeyConfigActive(false);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locBcReceiver);    // localBraodcast-Empfang stoppen
    }

    private void fresume() {

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locBcReceiver, hkfilter);
        keya.reloadHardkeys();
        Basis.setHardKeyConfigActive(true);
    }

    // ContextMenü
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        menutargetView = v;
        menu.clear();    // Menu leeren!!
        MenuInflater inflater = new MenuInflater(getActivity().getApplicationContext());
        inflater.inflate(R.menu.menu_hardkeys, menu);
        //Bsp. für Remove
        //menu.removeItem(R.id.menui_act_add);
    }

    public void onContextMenuClosed(Menu menu) {
        menutargetView = null;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        int itemId = item.getItemId();
        if (itemId == R.id.menui_hardkeys_edit) {
            if (menutargetView != null) {
                DialogFrag_Hardkeys hkfrag = new DialogFrag_Hardkeys();
                Bundle args = new Bundle();

                int keycode = (int) menutargetView.getTag();
                args.putInt("keycode", keycode);
                hkfrag.setArguments(args);
                hkfrag.show(getFragmentManager(), "hardkeydialog");

            }
            return true;
        } else if (itemId == R.id.menui_hardkeys_del) {
            if (menutargetView != null) {
                int keycode = (int) menutargetView.getTag();
                Basis.DBdelHardkey(keycode);
                HKChangedListener.OnHardkeyChanged(keycode);    // Info an die Activity
            }
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }    // end  onContextItemSelected


    // über diese Funktion erhöt das Fragment die KeyEvents von der Activity

    public void getKeyEvent(KeyEvent kevent) {
        int keycode = 0;
        keycode = kevent.getKeyCode();
        //kevent.getDisplayLabel();

        if (!keya.keycodeExists(keycode)) {
            HardKey hknew = new HardKey();
            keya.addKey(keycode, hknew);
            Basis.DBsaveHardkey(keycode, hknew);
        }    // nur einfügen, wenn noch nicht vorhanden
        selectItem(keycode);    // die Zeile für die gedrückte Taste markieren
    }


    public void selectItem(int keycode) {
        listView_keys.setItemChecked(keya.getIndex(keycode), true);
    }


    public class KeysAdapter extends BaseAdapter implements ListAdapter {

        @Override
        public int getCount() {
            hardkeys.size();
            return hardkeys.size();
        }

        @Override
        public Object getItem(int position) {

            return hardkeys.get(hardkeys.keyAt(position));
        }

        @Override
        public long getItemId(int position) {

            return hardkeys.keyAt(position);
        }


        public void addKey(int keycode, HardKey hk) {
            hardkeys.put(keycode, hk);
            this.notifyDataSetChanged();
        }

        public void removeKey(int keycode) {
            Basis.DBdelHardkey(keycode);
            hardkeys.delete(keycode);
            this.notifyDataSetChanged();
        }


        public boolean keycodeExists(int keycode) {
            if (hardkeys.get(keycode) == null) {
                return false;
            } else {
                return true;
            }
        }

        public int getIndex(int keycode) {
            return hardkeys.indexOfKey(keycode);
        }

        public void reloadHardkeys() {
            Basis.DBloadAllHardkeys(hardkeys);    // Hardkeys neu laden
            this.notifyDataSetChanged();
        }

        // einen hardkey aktualisieren bzw. entfernen, wenn er nicht mehr in der db ist
        public void reloadHardkey(int keycode) {
            hardkeys.delete(keycode);
            HardKey hk = Basis.DBloadHardkey(keycode);
            if (hk != null) {
                hardkeys.put(keycode, Basis.DBloadHardkey(keycode));
            }
            this.notifyDataSetChanged();
        }


        @SuppressLint("NewApi")
        public void removeKeyatIndex(int position) {
            if (Basis.getApiLevel() >= 11) {
                hardkeys.removeAt(position);
            } else {
                removeKey(hardkeys.keyAt(position));
            }
            this.notifyDataSetChanged();
        }


        @SuppressLint("NewApi")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = convertView;
            String keycode_name = "?";

            if (row == null) {
                row = layoutInflater.inflate(R.layout.hardkeys_row, parent, false);
            }
            unregisterForContextMenu(row);

            int keycode = hardkeys.keyAt(position);

            HardKey hk = hardkeys.get(keycode);
            //row.setTag(hk);	// Device in Tag speichern (für ContextMenu-Bearbeitung)
            row.setTag(keycode);    // keycode in Tag speichern (für ContextMenu-Bearbeitung)

            TextView textView_key = (TextView) row.findViewById(R.id.textView_key);
            TextView textView_key2 = (TextView) row.findViewById(R.id.textView_key2);
            TextView textView_function = (TextView) row.findViewById(R.id.textView_function);
            TextView textView_function2 = (TextView) row.findViewById(R.id.textView_function2);

            textView_key2.setVisibility(View.VISIBLE);
            textView_function2.setVisibility(View.VISIBLE);

            if (Basis.getApiLevel() >= 12) {
                keycode_name = KeyEvent.keyCodeToString(keycode);
            }    // unter Apilevel 12 ist dieser Name nicht über diese Funktion verfügbar
            else {
                keycode_name = getResources().getString(R.string.hkey_key) + keycode;
            }    // für ältere Versionen


            if (hk.custom_name.equals("")) {
                textView_key.setText(keycode_name);
                textView_key2.setVisibility(View.GONE);
            } else {
                textView_key.setText(hk.custom_name);
                textView_key2.setText(keycode_name);
            }

            //textView_function.setText(String.valueOf(hk.function1));
            textView_function.setText(function[hk.function1]);

            if (hk.function2 > 0) {
                textView_function2.setText("F2: " + function[hk.function2]);
            } else {
                textView_function2.setVisibility(View.GONE);
            }

            registerForContextMenu(row);    // für user-created Devices -> ContextMenu

            return row;
        }


    } // end class KeysAdapter


    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        int i = 1;
    }


}    // end Class Frag_hardkeys
