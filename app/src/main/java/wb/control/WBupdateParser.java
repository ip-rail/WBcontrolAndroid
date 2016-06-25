package wb.control;

import android.os.Build;
import android.os.Environment;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael Brunnbauer on 26.03.2016.
 */
public class WBupdateParser {

    // We don't use namespaces
    private static final String ns = null;

    public List parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }


    private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List entries = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "updateinfo");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("software")) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }


    public static class WBSoftware {

        public static final String WBSOFTWARE_PLATFORM_UC02     = "uc02";       // UC02-Board mit ATMega2561
        public static final String WBSOFTWARE_PLATFORM_328      = "328";        // ATMega328
        public static final String WBSOFTWARE_PLATFORM_RASPI    = "raspi";      // Raspberry Pi
        public static final String WBSOFTWARE_PLATFORM_ANDROID  = "android";    // Android

        public static final String WBSOFTWARE_TYPE_FINAL        = "final";      // final-Version
        public static final String WBSOFTWARE_TYPE_TEST         = "test";       // test-Version der Software - nur im Testmodus einspielen!

        public final String name;           // Softwarename: zB: lokbasis | wbcontrol | raspilokserver
        public final String platform;
        public final String type;
        public final int version;           // fortlaufende Zahl als Versionsnummer, keine Punkte, keine Revisionen und so Scheiß!
        public String filename;   // TODO: wird durch url ersetzt // filename im lokalen download-verzeichnis (der app)
        public final String url;        // download-url
        private long downloadID;                // ID zum Verwalten aktueller Downloads (wurde kein Download angestoßen, ist die ID=0. Ist der download abgeschlossen und behandelt, muss die iD wieder auf 0 gesetzt werden!)
        public boolean downloaded;      // true, falls das file bereits erfolgreich downgeloadet wurde

        private WBSoftware(String name, String platform, String type, int version, String url) {
            this.name = name;
            this.platform = platform;
            this.type = type;
            this.version = version;
            this.url = url;
            filename = "";
            downloadID = 0;
            downloaded = false;


            String[] parts = this.url.split("/"); // wenn kein : enthalten ist, dann ist cmdparts[0] = command, cmdparts.length=1
            if (parts.length >= 4) { filename = parts[parts.length - 1]; }  // filename sollte der Teil der url nach dem letzten '/' sein
        }


        public boolean isDownloaded(String filename) {
            String filepath = "";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) // Pfad für APIlevel >= 8
            {
                filepath = Basis.getBcontext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + filename;
            }
            else   { filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "wbcontrol" + File.separator + filename; }

            File f = new File(filepath);    // da wird im Dateisystem kein File angelegt!!!
            return f.exists();
        }

        public long getDownloadID() {  return downloadID; }

        public void setDownloadID(long id)
        {
            downloadID = id;
        }


        public void clearDownloadID()
        {
            downloadID = 0;
        }

    }

    // Parses the contents of an "software". If it encounters a name, platform, type, version, or filename tag, hands them off
// to their respective "read" methods for processing. Otherwise, skips the tag.
    private WBSoftware readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "software");

        String name = null;
        String platform = null;
        String type = null;
        int version = 0;
        String url = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagname = parser.getName();
            if (tagname.equals("name")) {
                name = readTag(parser, "name");
            } else if (tagname.equals("platform")) {
                platform = readTag(parser, "platform");
            } else if (tagname.equals("type")) {
                type = readTag(parser, "type");
            } else if (tagname.equals("version")) {
                version = readVersion(parser);
            } else if (tagname.equals("url")) {
                url = readTag(parser, "url");

            } else {
                skip(parser);
            }
        }
        return new WBSoftware(name, platform, type, version, url);
    }

    /*
    // Processes name tags in the feed.
    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return title;
    } */

    // Processes tags in "software"
    private String readTag(XmlPullParser parser, String tagname) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tagname);
        String tagdata = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tagname);
        return tagdata;
    }

    // For the tags name.., extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Processes "version" tag
    private int readVersion(XmlPullParser parser) throws IOException, XmlPullParserException {
        int ver;
        parser.require(XmlPullParser.START_TAG, ns, "version");
        String tagdata = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "version");

        try	{ ver = Integer.parseInt(tagdata); }
        catch (NumberFormatException e) { ver = 0; }    // TODO: log Error

        return ver;
    }


    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
