package wb.control.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import wb.control.Basis;
import wb.control.R;
import wb.control.WBlog.wblogtype;
import wb.control.WBupdateParser;

public class CheckOnlineUpdateTask extends AsyncTask<String, Void, Integer> {
	
	final static String LOGTAG = "CheckOnlineUpdateTask";

    protected Integer doInBackground(String... urltxt) {

        int version = 0;
        String updatetxt;
        URL updver_url = null;
        URLConnection ucon = null;
        Boolean error = false;

        Context bcontext = Basis.getBcontext();

        if (!Basis.isInternetConnected()) // check if Internet ins available (google.com)
        {
            error = true;
            Basis.AddLogLine(bcontext.getString(R.string.err_upd_no_inet), LOGTAG, wblogtype.Warning);
        }

        if (!error) {
            try {
                updver_url = new URL(urltxt[0]);
            } catch (MalformedURLException e) {
                error = true;
                Basis.AddLogLine(bcontext.getString(R.string.err_upd_ver_url) + ": " + e.toString(), LOGTAG, wblogtype.Error);
            }

            if (!error) {
                try {
                    ucon = updver_url.openConnection();
                } catch (IOException e) {
                    error = true;
                    Basis.AddLogLine(bcontext.getString(R.string.err_upd_ver_url_open) + ": " + e.toString(), LOGTAG, wblogtype.Error);
                }
                if (!error) {

                    InputStream is = null;
                    try {
                        is = ucon.getInputStream();
                    } catch (IOException e) {
                        error = true;
                        Basis.AddLogLine(bcontext.getString(R.string.err_upd_ver_instream) + ": " + e.toString(), LOGTAG, wblogtype.Error);
                    }

                    try {
                        WBupdateParser xmlParser = new WBupdateParser();
                        List<WBupdateParser.WBSoftware> updateList = xmlParser.parse(is);    // is wird vom Parser geschlossen

                        // eigene Updatemöglichkeiten auswerten
                        if (updateList != null) {
                            for (WBupdateParser.WBSoftware sw : updateList) {
                                if (sw.name.equals("wbcontrol")) {
                                    if (sw.type.equals("test")) {
                                        if (Basis.getRunmode().ordinal() == 1) {
                                            if (sw.version > version) { version = sw.version; } // im Testmode Test-Versionen melden
                                        }
                                    } else {
                                        if (sw.version > version) { version = sw.version; }   // sonst (Standardmode) nur final-Versionen melden
                                    }
                                }
                            }

                            Basis.setSoftwareUpdate_List(updateList); // updateList in Basis speichern
                        }

                    } catch (IOException e) {
                        error = true;
                        Basis.AddLogLine(bcontext.getString(R.string.err_upd_ver_instream_read) + ": " + e.toString(), LOGTAG, wblogtype.Error);
                    } catch (XmlPullParserException e) {
                        error = true;
                        Basis.AddLogLine(bcontext.getString(R.string.err_upd_ver_xml_parse) + ": " + e.toString(), LOGTAG, wblogtype.Error);
                    }
                }
            }
        }

        if (error) { version = 0; }    // im Fehlerfall 0
        return version;
    }

	protected void onPostExecute(Integer version) {

		Context bcontext = Basis.getBcontext();

        if (version > Basis.getAppversioncode()) // Version am Server ist neuer -> Update wäre möglich
        {
            Basis.setUpdateAvailable(version);
            Basis.AddLogLine(String.format(bcontext.getString(R.string.bas_upd_ver_online), version), LOGTAG, wblogtype.Info);
            Basis.getLocBcManager().sendBroadcast(new Intent(Basis.ACTION_PROGUPDATE_AVAILABLE));
        }
        else    // keine neuere Version gefunden
        {
            Basis.AddLogLine(bcontext.getString(R.string.bas_upd_nothingnew), LOGTAG, wblogtype.Info);
        }

        // StartupUpdateCheck auf erledigt setzen, wenn kein Fehler aufgetreten ist
        if (version == 0) { Basis.setStartupUpdateChecked(0);  }    // Fehler: 0 = check noch unerledigt
        else { Basis.setStartupUpdateChecked(2); }  // OK: 2 = check erledigt
	}
}
