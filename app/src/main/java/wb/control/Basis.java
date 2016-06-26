package wb.control;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LruCache;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import wb.control.Device.DeviceType;
import wb.control.WBlog.wblogtype;
import wb.control.activities.End;
import wb.control.tasks.CheckOnlineUpdateTask;
import wb.control.tasks.UdpSenderTask;

//local service 

public class Basis extends Service {

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// data
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


    private static String appversionname;				// Versionsname aus dem Manifest
	private static int appversioncode;					// Versionscode aus dem Manifest
	private static Context bc;
	private static BroadcastReceiver WLANreceiver;		// BroadcastReceiver für WLAN-Statusänderungen
	private static BroadcastReceiver BcReceiver;		// BroadcastReceiver für die globalen Broadcasts (für DownloadManager - meldet die fertigen Downloads)
	private static LocalBroadcastManager LocBcManager;	// für locale Broadcasts in wb.control
	private static Handler loghandler;					// Handler für Logeinträge, damit diese an den UI-Thread gepostet werden können
	private static Timer fastTimer;	// 200ms Timer für diverse Aufgaben
	private static int configfraggroup;					// zum Zwischenspeichern der aktuellen configGroup des cfg_level2-Fragments beim Restart der Activity wg. Einstellungsänderung bei Apilevel < 11
	private static Thread UDPwaiterThread;
	private static DatagramSocket UDPsocket = null;
	private static Device CCDevice;			// das aktuell zu steuernde Device bzw. null / statt cdindex
	private static Device showDevice;		// das Device, das aktuell unter "Loks" angezeigt werden soll
	private static Device server;			// das server device
	private static Device camDevice;		// aktives KameraDevice (Kamera-Wagen oä)
	private static int speedstufen;			// speedstufen für Speed-Balken - // TODO: nur Voreinstellung für neue Loks. der Wert ist pro Device (Lok)!!
	private static int rangiermax;       	// Maxwert im Rangiermodus
	private static int lokstopmode;			// wann manuell gesteuerte Loks gestoppt werden sollen: Wert 0: nie. Flags: 1: bei Anruf, 2: Schienenspannung zu niedrig
	private static String manuelle_ip;     	// für manuell eingegebene IP (wenn man Gerätesuche nicht verwenden will)
	private static String name;             // der Name des Controllers (auf dem WBcontrol läuft)
	private static ArrayList<WBlog> log;   // der zur Laufzeit anfallende log
	private static int loglevel;			// 0: Loglevel=Normal, 1: Loglevel=Test 	// muss nicht threadsafe sein 
	private static String statustext;       // aktuelle Statusmeldung die ausgegeben werden soll
	private static int tcpport;             // Port für TCP Kommunikation
	private static int udpport;             // Port für UDP Kommunikation
	private static boolean use_udp;			// true: udp wird verwendet, false: kein udp (kein senden, kein udpwaiter wird gestartet
	private static int servertcpport;       // Port für TCP Kommunikation mit dem Server
	private static String eigeneip;      	// die IP des Controllers
	private static String broadcastip;      	// die Broadcst IP-Adresse für udp
	private static Boolean networkIsConnected;	// true, wenn das Steuerungsgerät mit einem Netzwerk verbunden ist (IP kann es auch ohne Verbindung haben)
	private static ArrayList<Device> devicelist; // Liste aller bekannten Devices
	private static ArrayList<Macro> wbeventlist;  			// Liste aller bekannten WBevents (incl. RFID-Tags)! (Event-Macros, RFID-Macros) für Macro-Verarbeitung
	private static ArrayList<Macro> macrolist;   			// Liste aller bekannten Macros
    private static Boolean useserver;          			// soll ein vorhandener server benutzt werden?
    private static Boolean ismastercontroller;    	// nur der Mastercontroller einer Analge kann Gastloks zuweisen/verwalten usw
    private static Boolean ismasterathome;        	// für check Mastercontroller auf Heimanlage oder nicht
    private static Boolean stopall;            	// stopall Befehl wurde ausgelöst,
    private static Boolean pauseall;            	// pauseall Befehl wurde ausgelöst
    private static String pauseallsender;   // Name des Devices, das die Pause ausgelöst hat -> nur der darf sie beenden!!
    private static runmodetype runmode;     				// Betriebs(Start)-Modus des Programms
    private static runmodetype startmode;     			// Startzustand des Betriebs(Start)-Modus des Programms (muss wg. master/standard memoriert werden!! )
    private static int usermode;					// Normal/Gastbetrieb: siehe UserMode Typen
    private static boolean usermode_usepwd;			// zum aufheben des Gästemodus Passwort notwendig?
    private static String usermodepwd;				// Passwort für usermode
    private static int actualmacrotyp;			// aktuelle auswahl macrotyp in Macroübersicht nur zur Laufzeit	// muss nicht threadsafe sein
    private static ArrayList<ActionElement> actionelementlist; // Liste der action elemente (nach start aus der db gelesenen) - wird bei Android dauerhaft gebraucht (Win nur für Programmstert)!
    private static int ae_nextfreenumber;			// nächste freie nummer für ein ae-element. Startet mit 1
    private static int apiLevel;					// Android API level
    private static int displaymode;					// Bildschirmaufteilung: Anzeigemodus für Fragment-Anordnung (siehe: Display-Mode Typen weiter unten)
    private static int layouttype;					// ausgewähltes Layout
    private static boolean stop_on_disconnect;		// soll bei Device.Disconnect() immer ein <stop> ausgeführt werden
    private static int forceDisplaymode;			// Bildschirmaufteilung erzwingen (single/dual/multiview) 0 für nix erzwingen
    private static int screensavermode;				// Modus für "Bildschirmschoner deaktivieren" Typen siehe weiter unten
    private static float displayDensity = 1f;					// DisplayDensity merken für font-scale-berechnungen
    
    private static PowerManager.WakeLock wlock;		// WakeLock für Bildschirmschoner-Einstellungen
    private static ArrayList<QAelement> QuickActionlist;		// Liste der elemente für das QuickAction-Fragment
    private static long dpPixels;					// dpPixel-Anzahl als Maß zum Abschätzen der Layoutgröße
    private static long dpPixels_dualview_threshold;	// dpPixel-Anzahl, ab der DUAL_VIEW verwendet wird
    private static float fontScale;					// zum ändern der Textgröße
    private static int themeType;					// zu verwendendes Theme (siehe Theme Auswahl Konstanten weiter unten)
    private static String trackplanName;			// name des aktuellen TrackPlans;
    
    private static ArrayList<WBnetwork> existingNetworks;	// die verwendbaren Netzwerke (mobile, WLAN..)
    private static int useNetwork;					// unbekannt=-1 und die typen aus dem ConnectivityManager (Mobile=0, WLAN=1, Ethernet=9)
    
    private static ArrayList<AVStream> avStreamlist;	// Liste der verfügbaren AV-Streams (Video-Kamera..)
    private static int selectedAVstream;					// aktueller AV-Stream -> index von avStreamlist (-1: keiner ausgewählt)
    private static List <WBupdateParser.WBSoftware> SoftwareUpdate_List;        // Liste aller verfügbaren software-Updates (aller Softwar-Komponenten!)
    private static int updateAvailable;				// Versionscode einer verfügbaren neueren Programmversion (siehe appversioncode). 0 wenn keine neuere Programmversion verfügbar ist
    // private static String updateFile;				// Filename der verfügbaren neueren Programmversion (updateAvailable und updateFile gehören zusammen) TODO: entfernen
    private static int updateAllowed;				// Einstellung: Programmupdates suchen (0: nicht automatisch, 1: bei WLAN, 2: immer
    private static int updateMode;					// 1: nur Download, 2: Download und automatisch installieren
    private static Boolean doUpdateAtEnd;			// true: wenn der Shutdown ausgeführt wird, soll ein Programmupdate durchgeführt werden
    private static int startupUpdateChecked;    // gibt an, ob Programm-Update Check (und damit Dialog-Anzeige) schon erfolgreich durchgeführt wurde erfolgreich = 2 (Internetverbindung notwenidg)
                                                    // wird bei Start des Checks auf 1 gesetzt und falls der check nicht geklappt hat wieder auf 0 (weil der Check länger dauert, damit er nicht mehrfach ausgeführt wird)
    private static int action2_rows;				// Action2 Raster - Anzahl der Reihen
    private static int action2_cols;				// Action2 Raster - Anzahl der Spalten
        
    // Locks
    private static ReentrantLock logLock;			// für AddLogLine()
    
    // WLAN-DL Variablen
    private static int speedramp_min;				// Minimalwert der Speedrampen-Nummer: von <speedramp_min> bis <speedramp_max>	// Stdwert 0
    private static int speedramp_max;				// Maximalwert der Speedrampen-Nummer: von <speedramp_min> bis <speedramp_max>

    private static String macroStandardDeviceName;	// Name des Macro-"Standard"-Devices (damit die Macro-Klasse darauf zugreifen kann)
    private static boolean hardKeyConfigActive;		// true: HardKey events an das (aktive) HK-Konfig Fragment senden, false: Hardkeys zur Steuerung verwenden
    
    //Bitmap caching
    private static Bitmap standardlokpic;
	private static LruCache<String, Bitmap> mMemoryCache;
    
	private static int camhval,camvval;				// Servo-Values für Kamera. wird nur zur Laufzeit gespeichert
    
    
    private static final String PREFS_NAME = "wbcontrol.config";
    private static final String LOGFILE_NAME = "wbcontrol.log.txt";

    private final static String LOGTAG = "Basis";

	private static String wbDBpath;						// Datenbankpfad
	public final static String WB_DB_NAME = "wbcontrol";
	public final static int WB_DB_VERSION = 1;					// Version der DB
	public final static String WB_DB_TABLE_CFG = "config";		// zum Speichern diverser Einstellungen (Ersatz für Preferences)
	public final static String WB_DB_TABLE_MACRO = "macro";	// zum Speichern der Macros
	public final static String WB_DB_TABLE_MACRO_CMDS = "macro_commands";	// zum Speichern der Macros (Befehle)
	public final static String WB_DB_TABLE_WBEVENT = "wbevent";	// zum Speichern der WBevents/RFIDs
	public final static String WB_DB_TABLE_WBEVENT_CMDS = "wbevent_commands";	// zum Speichern der WBevents/RFIDs (Befehle)
	public final static String WB_DB_TABLE_ACTION = "action";	// zum Speichern der Actionelemente
	public final static String WB_DB_TABLE_DEVICES = "devices";	// zum Speichern der manuell angelegten (isUserCreated) Devices
	public final static String WB_DB_TABLE_IMAGES = "images";	// zum Speichern der Bilder für Devices
	public final static String WB_DB_TABLE_TRACKPLANS = "trackplans";	// zum Speichern der TrackPlans (Gleispläne)
	public final static String WB_DB_TABLE_AVSTREAMS = "avstreams";	// zum Speichern der AV-streams
	public final static String WB_DB_TABLE_HARDKEYS = "hardkeys";	// zum Speichern der Hardware-Key Funktionen

    //Message Typen für lokale Broadcasts
    public static final String ACTION_WLAN_DISCONNECTED = "wb.control.WLAN_DISCONNECTED";		// WLAN geht nicht mehr
    public static final String ACTION_WLAN_CONNECTED = "wb.control.WLAN_CONNECTED";				// WLAN funktioniert jetzt
    public static final String ACTION_DEVICELIST_CHANGED = "wb.control.DEVICELIST_CHANGED";		// Devicelist hat sich geändert -> Info an Fragments, falls Listen geündert werden müssen
    public static final String ACTION_NEW_LOGDATA = "wb.control.NEW_LOGDATA";					// neue Logenträge vorhanden -> Info an Fragments, falls Log angezeigt werden soll
    public static final String ACTION_UPDATE_AE = "wb.control.UPDATE_AE";						// eine ActionElement hat sich geändert. Extra: "aeindex" (int) = index in der Basis.actionelementlist
    public static final String ACTION_UPDATE_AE_DATA = "wb.control.UPDATE_AE_DATA";				// Daten haben sich geändert, die ein ActionElement vom Typ AE_TYP_Data verwenden könnten. Extra: "datatype" (int)(siehe ae.datatype)  und "device" (String) Devicename (siehe ae.scopedata)
    public static final String ACTION_QA_TRACK_EDIT_ON = "wb.control.QA_TACK_EDIT_ON";			// QuickAction: Track-Edit Tools aktivieren
    public static final String ACTION_QA_TRACK_EDIT_OFF = "wb.control.QA_TACK_EDIT_OFF";		// QuickAction: Track-Edit Tools deaktivieren
    public static final String ACTION_HKEY_UPDATE = "wb.control.HKEY_UPDATE";					//  Änderung bei den Hardkeys, Extra (int) "keycode" -> -1 bedeutet alle keycodes aktualisieren
        // Ersatz für: Message Typen (msg.what) für datahandler	- jetzt für lokale Broadcasts
    public static final String ACTION_BASIS_READY = "wb.control.BASIS_READY";		            // Basis-Service is started and ready for work
    public static final String ACTION_STARTUP_NETWORK_ERROR = "wb.control.STARTUP_NETWORK_ERROR";	// Fehler von start Network(). "errorcode" (int) mitgeben (siehe Startup Network Errors)
    public static final String ACTION_PROGUPDATE_AVAILABLE = "wb.control.PROGUPDATE_AVAILABLE";     // neues Programmupdate ist verfügbar
    public static final String ACTION_PROGUPDATE_DO = "wb.control.PROGUPDATE_DO";                  // neues Programmupdate soll durchgeführt werden
        // Ersatz für frag_control messages (devices)
    public static final String ACTION_UPDATE_TRAINSPEED 	= 	"wb.control.UPDATE_TRAINSPEED";	// SpeedBar: tsProgress (echte Lokgeschwindigkeit) soll updaedatet werden (von netWaiter)
    public static final String ACTION_DEVICE_DISCONNECTED 	=	"wb.control.DEVICE_DISCONNECTED";	// Msg von netWaiter, dass das Device getrennt wurde (Name im Bundle) (für den Fall, dass die Verbindung beendet / unterbrochen oder die Lok ausgeschalten wurde)
    public static final String ACTION_UPDATE_U_LOK			= 	"wb.control.UPDATE_U_LOK";	// VersorgungsSpannung (der Lok) soll upfadatet werden (von netWaiter)
    public static final String ACTION_DEVICE_CONNECTED 		=	"wb.control.DEVICE_CONNECTED";	// Msg von netWaiter, dass das Device verbunden wurde (Name im Bundle)
    public static final String ACTION_DEVICE_CONNECING_FAILED =	"wb.control.DEVICE_CONNECING_FAILED";	// Msg von netWaiter, dass das Device verbunden werden sollte, aber fehlschlug (Name im Bundle)
    public static final String ACTION_SPEEDSTEPS_CHANGED 	=	"wb.control.SPEEDSTEPS_CHANGED";	// Msg, dass sich in den Einstellungen die Speedsteps geändert haben (-> Speedbar aktualisieren)
    public static final String ACTION_UPDATE_STATUSTXT		=	"wb.control.UPDATE_STATUSTXT";	// Msg, dass das Device einen neuen Text zum anzeigen in der Statuszeile hat
    public static final String ACTION_DEVICE_TRY_RECONNECT 	=	"wb.control.DEVICE_TRY_RECONNECT";	// Msg von netWaiter, dass die Verbindung zum Device unterbrochen wurde, aber versucht wird, neu zu verbinden
    public static final String ACTION_DEVICE_MOTORERROR	 	=	"wb.control.DEVICE_MOTORERROR";	// Msg von netWaiter, dass das Device einen motorerror gemeldet hat
    public static final String ACTION_CMD_STOPPALL          =   "wb.control.CMD_STOPPALL";      // Msg dass ein STOPPALL-Befehl empfangen wurde (für die Fragments (und sonstige Anzeigeelemente), die devices bekommen den befehl ohnehin direkt)
    public static final String ACTION_DEVICE_NEW_NAME       =   "wb.control.DEVICE_NEW_NAME";      // Msg, dass das Device einen anderen Namen (als den gespeicherten) meldet
    public static final String ACTION_DEVICE_NAME_CHANGED   =   "wb.control.DEVICE_NAME_CHANGED";      // Msg, dass ein Devicename geändert wurde Extra: device = neuer Name, oldname = alter Name
    public static final String ACTION_DEVICE_EVENT_STOP     =   "wb.control.DEVICE_EVENT_STOP";      // Msg, dass das Device den Event Stop ausgelöst hat
    public static final String ACTION_UPDATE_GPIO           =   "wb.control.UPDATE_GPIO";           // Msg, dass sich ein GPIOport-Status geändert hat. Extra: device, für das sich die Daten geändert haben

    // Startup Network Errors
    public static final int NETWORK_ERR_WLAN_NOT_ENABLED = 1;   // WLAN nicht aktiv (wifistate != 3)
    public static final int NETWORK_ERR_NO_IP = 2;              // IP konnte nicht ermittelt werden,
    public static final int NETWORK_ERR_INVALID_IP = 4;         // IP ist keine gültige IPv4-Adresse
    public static final int NETWORK_ERR_WLAN_NOT_CONNECTED = 8; // WLAN aktiv, aber kein Netzwerk verbunden

    // Display-Mode Typen
    public static final int DISPLAYMODE_SINGLEVIEW = 	1;	// immer nur 1 Anzeigebereich am Display (Handy-Modus)
    public static final int DISPLAYMODE_DUALVIEW = 		2;		// 2 Anzeigebereiche nebeneinander (Querformat) oder untereinander (Hochformat) - für kleine Tablets mit geringer Auflösung (7")
    public static final int DISPLAYMODE_MULTIVIEW = 	3;		// mehr als 2 Anzeigebereiche sind müglich (10" Tablet mit ausreichender Auflösung)
    public static final int DISPLAYMODE_DUALVIEW_DPXY_THRESHOLD = 	288000;	// DEFAULTWERT dpXY für Verwendung von DUALVIEW (darunter SINGLEVIEW)
    
    // UserMode Typen
    public static final int USERMODE_STANDARD 	= 	0;		// Normal
    public static final int USERMODE_GUEST 		= 	1;		// Betreib als "Gast"-Gerät -> Einstellungen und gefährliche Sachen verstecken
    
    // Layout Typen (Namen sind definiert in: values:arrays:layout_types)
    public static final int LAYOUT_CONTROL_L = 0;			// Control links
    public static final int LAYOUT_CONTROL_R = 1;			// Control rechts
    
    // Screensaver Mode Typen (siehe: values:arrays:wakelock_type)
    public static final int SSAVER_ACTIVE	= 0;	// Bildschirmschoner aktiv
    public static final int SSAVER_DIM		= 1;	// nur Helligkeit reduzieren (nicht ganz finster)
    public static final int SSAVER_INACTIVE	= 2;	// Bildschirmschoner deaktiviert
    
    // Theme Auswahl
    public static final int THEME_DARK	= 0;	// Dunkles Theme (=Standard)
    public static final int THEME_LIGHT	= 1;	// Helles Theme
    
    //Netzwerktypen
    public static final int NETWORK_WLAN = 1;	// Netzwerktyp WLAN
    
    public static final int BASIS_SERVICE_NOTIFICATION_ID = 1;	// eindeutige ID in der application
    
    
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// Getter/Setter: "Properties" für Data
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    public static Context getBcontext() {
		return bc;
	} 
           
    public static LocalBroadcastManager getLocBcManager() {
		return LocBcManager;
	}

    /*  // TODO: setAktuellesfragment -> entfernen
    public static int getAktuellesfragment() {
		return aktuellesfragment;
	}

	public static void setAktuellesfragment(int af) {
		aktuellesfragment = af;
	} */

	public static void setUDPwaiterThread(Thread uDPwaiterThread) 
    {
    	UDPwaiterThread = uDPwaiterThread;
    }
    
	public static Thread getUDPwaiterThread() 
    {
    	return UDPwaiterThread;
    }
	
    public static void setUDPsocket(DatagramSocket uDPsocket) 
    {
		UDPsocket = uDPsocket;
	}
	public static DatagramSocket getUDPsocket() 
	{
		return UDPsocket;
	}
	   
    public static void setCCDevice(Device ccd) 
    {	
    	if (CCDevice != null) { CCDevice.setIscd(false); }	// für altes CCDevice abdrehen TODO: setIscd wird hier nicht mehr gebraucht (gesetzt im frag_control bei der auswahl des devices, gelöscht beim device.disconnect()
    	CCDevice = ccd;
    	if (CCDevice != null) { CCDevice.setIscd(true); }
	}
	public static Device getCCDevice() {
		return CCDevice;
	}
	
    public static Device getShowDevice() {
		return showDevice;
	}

	public static void setShowDevice(Device showDevice) {
		Basis.showDevice = showDevice;
	}
	
	public static void setSpeedStufen(int stufen)
    {
    	speedstufen = stufen;
    }
	
    public static int getSpeedStufen()
    {
        return speedstufen;
    }
    
    public static void setRangierMax(int max)
    {
    	rangiermax = max;
    }
    public static int getRangierMax()
    {
        return rangiermax;
    }
    
    public static void setManuelleIP(String ip)
    {
        manuelle_ip = ip;
    }
    public static String getManuelleIP()
    {
        return manuelle_ip;
    }
    
    public static void setName(String n)
    {
    	name = n;
    }
    
    public static String getName()
    {
    	return name;
    } 

    public static List<WBlog> getLogSublist(int start, int end)	// mit Lock
    {
    	List<WBlog> sublist;
    	
    	logLock.lock();
    	try 
    	{
    		sublist = log.subList(start, end);
    	}
    	finally { logLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall)
    	
    	return sublist;
    }
    
    public static WBlog getLogEntry(int index)	// mit Lock
    {
    	WBlog wblogitem;
    	
    	logLock.lock();
    	try 
    	{
    		wblogitem = log.get(index);
    	}
    	finally { logLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall)
    	
    	return wblogitem;
    }
    
    
    public static int getLoglevel() {
		return loglevel;
	}
	public static void setLoglevel(int loglevel) {
		Basis.loglevel = loglevel;
	}
	public static void setStatusText(String txt)
    {
    	statustext = txt;
    }
    
    public static String getStatusText()
    {
    	return statustext;
    }    
    
    public static void setTcpPort(int port)
    {
    	tcpport = port;
    }
    
    public static int getTcpPort()
    {
    	return tcpport;
    }
    
    public static void setUdpPort(int port)
    {
    	udpport = port;
    }
    
    public static int getUdpPort()
    {
    	return udpport;
    }
    
    public static boolean Use_udp() 
    {
		return use_udp;
	}

	public static void setUse_udp(boolean use_udp) 
	{
		boolean old_use_udp = Basis.use_udp;
		Basis.use_udp = use_udp;
		if (!old_use_udp &&  use_udp) { startUDPService(); }	// falls es bisher nicht gestartet war und jetzt erlaubt wurde -> starten
	}

	public static void setServerTcpPort(int port)
    {
    	servertcpport = port;
    }
    
    public static int getServerTcpPort()
    {
    	return servertcpport;
    }
	
    public static void setEigeneIP(String ip)
    {
    	eigeneip = ip;
    }
    
    public static String getEigeneIP()
    {
    	return eigeneip;
    }
    
    public static void setBroadcastip(String broadcastip) {
		Basis.broadcastip = broadcastip;
	}
	public static String getBroadcastip() {
		return broadcastip;
	}
	public static void setDevicelist(ArrayList<Device> devlist) 
    {
		Basis.devicelist = devlist;
		sendLocalBroadcast(ACTION_DEVICELIST_CHANGED);
	}
    
    public static ArrayList<Device> getDevicelist()
    {
    	return devicelist;
    }
    
    public static void setWbeventlist(ArrayList<Macro> wbevlist) 
    {
		Basis.wbeventlist = wbevlist;
	}
    
	public static ArrayList<Macro> getWbeventlist() 
	{
		return wbeventlist;
	}
    
	public static void setMacrolist(ArrayList<Macro> macrolist) 
    {
		Basis.macrolist = macrolist;
	}
    
	public static ArrayList<Macro> getMacrolist() 
	{
		return macrolist;
	}
	
	
    public static void setUseServer(Boolean use)
    {
    	useserver = use;
    }
    
    public static Boolean getUseServer()
    {
    	return useserver;
    }
    
    public static void setIsMasterController(Boolean is)
    {
    	ismastercontroller = is;
    }
    
    public static Boolean getIsMasterController()
    {
    	return ismastercontroller;
    }
    
    public static void setIsMasterAtHome(Boolean is)
    {
    	ismasterathome = is;
    }
    
    public static Boolean getIsMasterAtHome()
    {
    	return ismasterathome;
    }
            
    public static void setStopAll(Boolean stop)
    {
        Boolean oldstopall = stopall;
    	stopall = stop;
        if ((!oldstopall) && stopall) {  sendLocalBroadcast(ACTION_CMD_STOPPALL);  }    // für das UI
    }
    
    public static Boolean getStopAll()
    {
    	return stopall;
    }
    
    public static void setPauseAll(Boolean pause)
    {
    	pauseall  = pause;
    }
    
    public static Boolean getPauseAll()
    {
    	return pauseall ;
    }
    
    public static void setPauseAllSender(String sender)
    {
    	pauseallsender  = sender;
    }
    
    public static String getPauseAllSender()
    {
    	return pauseallsender ;
    }
       
    public static void setRunmode(runmodetype m)
    {
    	runmode  = m;
    	SharedPreferences settings = bc.getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString("runmode", runmode.toString());
    	editor.commit();
    	if (m == runmodetype.standard) { loglevel = 0; }    // loglevel mitsetzen
        else { loglevel = 1; }
    }
    
    public static runmodetype getRunmode() 
    {
    	return runmode;
    }

    public static void setStartmode(runmodetype startmode) 
    {
    	Basis.startmode = startmode;
    }
    public static runmodetype getStartmode() 
    {
    	return startmode;
    }
    
	public static int getUsermode() {
		return usermode;
	}

	public static void setUsermode(int usermode) {
		Basis.usermode = usermode;
	}

	public static boolean isUsermode_usepwd() {
		return usermode_usepwd;
	}

	public static void setUsermode_usepwd(boolean usermode_usepwd) {
		Basis.usermode_usepwd = usermode_usepwd;
	}

	public static String getUsermodepwd() {
		return usermodepwd;
	}

	public static void setUsermodepwd(String usermodepwd) {
		Basis.usermodepwd = usermodepwd;
	}

	public static void setActualMacrotyp(int actualmacrotyp) {
		Basis.actualmacrotyp = actualmacrotyp;
	}
	public static int getActualMacrotyp() {
		return actualmacrotyp;
	}
	
	public static void setAe_nextfreenumber(int ae_nextfreenumber) {
		Basis.ae_nextfreenumber = ae_nextfreenumber;
	}
	public static int getAe_nextfreenumber() {
		return ae_nextfreenumber;
	}
	
	public static int getLayouttype() {
		return layouttype;
	}

	public static void setLayouttype(int layouttype) {
		Basis.layouttype = layouttype;
	}

	public static int getWakelockmode() {
		return screensavermode;
	}

	public static void setWakelockmode(int smode, Window w) {
		Basis.screensavermode = smode;
		
		switch(screensavermode)
		{
			case SSAVER_ACTIVE:	// Bildschirmschoner aktiv
				
				if (wlock != null) { wlock.release(); wlock = null; }
				w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
				
            case SSAVER_DIM:	// Helligkeit reduzieren //TODO: SCREEN_DIM_WAKE_LOCK entfernen
				PowerManager pm = (PowerManager) bc.getSystemService(Context.POWER_SERVICE);
				if (wlock == null)
				{
					wlock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "screendim");
					wlock.acquire();
				}
				break;
				
			case SSAVER_INACTIVE:	// Bildschirmschoner deaktiviert
				if (wlock != null) { wlock.release(); wlock = null; }
				w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
		}
		
	}
	
	public static ArrayList<QAelement> getQuickActionlist() {
		return QuickActionlist;
	}

	public static void setQuickActionlist(ArrayList<QAelement> qalist) {
		Basis.QuickActionlist = qalist;
	}

	public static ArrayList<ActionElement> getActionElementList() {
		return actionelementlist;
	}
	
	/*
	public static void setAe_toedit(ActionElement ae_toedit) {
		Basis.ae_toedit = ae_toedit;
	}
	public static ActionElement getAe_toedit() {
		return ae_toedit;
	}
	*/
	
	public static int getDisplaymode() {
		return displaymode;
	}

	public static void setDisplaymode(int displaymode) {
		Basis.displaymode = displaymode;
	}

	public static int getForceDisplaymode() {
		return forceDisplaymode;
	}

	public static void setForceDisplaymode(int forceDisplaymode) {
		Basis.forceDisplaymode = forceDisplaymode;
	}

	public static float getFontScale() {
		return fontScale;
	}

	public static void setFontScale(float fontScale) {
		Basis.fontScale = fontScale;
	}

	public static int getThemeType() {
		return themeType;
	}

	public static void setThemeType(int themeType) {
		Basis.themeType = themeType;
	}

	public static long getDpPixels_dualview_threshold() {
		return dpPixels_dualview_threshold;
	}

	public static void setDpPixels_dualview_threshold(
			long dpPixels_dualview_threshold) {
		Basis.dpPixels_dualview_threshold = dpPixels_dualview_threshold;
	}

	public static boolean isStop_on_disconnect() {
		return stop_on_disconnect;
	}

	public static void setStop_on_disconnect(boolean stop_on_disconnect) {
		Basis.stop_on_disconnect = stop_on_disconnect;
	}

	public static void setSpeedramp_max(int speedramp_max) {
		Basis.speedramp_max = speedramp_max;
	}
	public static int getSpeedramp_max() {
		return speedramp_max;
	}
	public static int getSpeedramp_min() {
		return speedramp_min;
	}
	public static void setSpeedramp_min(int speedramp_min) {
		Basis.speedramp_min = speedramp_min;
	}
	
	public static int getApiLevel() {
		return apiLevel;
	}

	public static long getDpPixels() {
		return dpPixels;
	}

	public static void setDpPixels(long dpPixels) {
		Basis.dpPixels = dpPixels;
	}
	
	//wbDBpath
	
	public static String getwbDBpath() {
		return wbDBpath;
	}
	
	public static void setwbDBpath(String path) {
		Basis.wbDBpath = path;
	}
	
	public static int getConfigfragGroup() {
		return configfraggroup;
	}

	public static void setConfigfragGroup(int configfraggroup) {
		Basis.configfraggroup = configfraggroup;
	}
	
	public static String getTrackplanName() {
		return trackplanName;
	}

	public static void setTrackplanName(String name) {
		Basis.trackplanName = name;
	}
	
	public static ArrayList<WBnetwork> getExistingNetworks() {
		return existingNetworks;
	}
	
	public static int getUseNetwork() {
		return useNetwork;
	}

	public static void setUseNetwork(int newNetwork) {
		
		if (useNetwork != newNetwork) { changeUsedNetwork(newNetwork); }
		Basis.useNetwork = newNetwork;
	}

	public static ArrayList<AVStream> getAvStreamlist() {
		return avStreamlist;
	}
	
	public static int getSelectedAVstream() {
		return selectedAVstream;
	}

	public static void setSelectedAVstream(int selectedAVstream) {
		Basis.selectedAVstream = selectedAVstream;
	}

	public static Boolean getNetworkIsConnected() {
		return networkIsConnected;
	}

	public static void setNetworkIsConnected(Boolean networkIsConnected) {
		Basis.networkIsConnected = networkIsConnected;
	}

    public static List getSoftwareUpdate_List() { return SoftwareUpdate_List; }

    public static void setSoftwareUpdate_List(List upateList)
    {
        SoftwareUpdate_List = upateList;

        // TODO: wenn eine neue Update-Liste gespeichert wird -> Liste auswerten (falls auch andere Updates verwaltet werden sollen)
    }


	public static int getUpdateAvailable() {
		return updateAvailable;
	}

	public static void setUpdateAvailable(int updateAvailable) {
		Basis.updateAvailable = updateAvailable;
	}

	public static int getUpdateAllowed() {
		return updateAllowed;
	}

	public static void setUpdateAllowed(int updateAllowed) {
		Basis.updateAllowed = updateAllowed;
	}

	public static int getUpdateMode() {
		return updateMode;
	}

	public static void setUpdateMode(int updateMode) {
		Basis.updateMode = updateMode;
	}

	public static Boolean getDoUpdateAtEnd() {
		return doUpdateAtEnd;
	}

	public static void setDoUpdateAtEnd(Boolean doUpdateAtEnd) {
		Basis.doUpdateAtEnd = doUpdateAtEnd;
	}

    public synchronized static void setStartupUpdateChecked(int checked)
    {
        startupUpdateChecked = checked;
    }

    public static int getStartupUpdateChecked()
    {
        return startupUpdateChecked;
    }

	public static String getMacroStandardDeviceName() {
		return macroStandardDeviceName;
	}	
	
	public static int getAction2_rows() {
		return action2_rows;
	}

	public static void setAction2_rows(int action2_rows) {
		Basis.action2_rows = action2_rows;
	}

	public static int getAction2_cols() {
		return action2_cols;
	}

	public static void setAction2_cols(int action2_cols) {
		Basis.action2_cols = action2_cols;
	}

	public static int getLokstopmode() {
		return lokstopmode;
	}

	public static void setLokstopmode(int lokstopmode) {
		Basis.lokstopmode = lokstopmode;
	}


	public static boolean isHardKeyConfigActive() {
		return hardKeyConfigActive;
	}

	public static void setHardKeyConfigActive(boolean hardKeyConfigActive) {
		Basis.hardKeyConfigActive = hardKeyConfigActive;
	}

	public static Bitmap getStandardlokpic() {
		return standardlokpic;
	}

	public static LruCache<String, Bitmap> getBitmapMemCache() {
		return mMemoryCache;
	}
	
	
	public static float getDisplayDensity() {
		return displayDensity;
	}

	public static void setDisplayDensity(float dispDensity) {
		displayDensity = dispDensity;
	}

	public static Device getServer() {
		return server;
	}

	public static void setServer(Device server) {
		Basis.server = server;
	}

	public static Device getCamDevice() {
		return camDevice;
	}

	public static void setCamDevice(Device camDevice) {
		Basis.camDevice = camDevice;
	}

	public static int getCamhval() {
		return camhval;
	}

	public static void setCamhval(int camhval) {
		Basis.camhval = camhval;
	}

	public static int getCamvval() {
		return camvval;
	}

	public static void setCamvval(int camvval) {
		Basis.camvval = camvval;
	}

	public static String getAppversionname() {
		return appversionname;
	}

	public static int getAppversioncode() {
		return appversioncode;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// lifecycle methods
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    @TargetApi(8)   // TODO: Problem -> sollte weg -> in eigene funktion auslagern und dort absichern!!
	@Override
	public void onCreate() {
		super.onCreate();
				
		
		bc = getApplication().getApplicationContext();
        LocBcManager = LocalBroadcastManager.getInstance(this);
		loghandler = new Handler();	// nur für die logeinträge, keine Messages
		// Starteinstellungen vornehmen
		macroStandardDeviceName = bc.getString(R.string.macro_name_stddevice);
		CCDevice = null; // noch kein gültiges Device verbunden
		//TODO: entfernen //aktuellesfragment = -1;	// -1 bedeutet leer
		configfraggroup = -1;	// -1 bedeutet leer / nicht gesetzt
		UDPsocket = null;
		networkIsConnected = false;	// es ist noch kein Netzwerk verbunden
		devicelist = new ArrayList<Device>();
		wbeventlist = new ArrayList<Macro>();
		macrolist = new ArrayList<Macro>();
		actionelementlist = new ArrayList<ActionElement>();
		logLock = new ReentrantLock();
		log = new ArrayList<WBlog>();
		loglevel = 0;	// TODO: derzeit noch Test als Standardwert, später 0 (normal -> weniger wird geloggt)
		fastTimer = new Timer("fastTimer");
		//speedformat = new DecimalFormat("0000");	// oder "###"
		displaymode = DISPLAYMODE_SINGLEVIEW;	// Displaymode wird in WBcontrolStartup bestimmt - hier Standardwert (Handy) voreinstellen
		dpPixels_dualview_threshold = DISPLAYMODE_DUALVIEW_DPXY_THRESHOLD;	// Defaultwert einstellen
		dpPixels = 0;
		layouttype = LAYOUT_CONTROL_L;
		forceDisplaymode = 0;	//nix forcen!
		screensavermode = 0;	// Bildschirmschoner aktiv
		fontScale = 1;
		themeType = THEME_DARK;	// Standard-Theme voreinstellen
		wlock = null;
		stop_on_disconnect = true;
		stopall = false;
		pauseall = false;
		QuickActionlist = new ArrayList<QAelement>();
		
		action2_rows = 3;
		action2_cols = 3;
		
		actualmacrotyp = 0;	// beim Programmstart auf's erste Element "Macros" setzen
		ae_nextfreenumber = 1;	// erkennungsnummer für erstes action-element. Startet mit 1 (0 kennzeichnet ein neues, noch anzulegendes element)
        trackplanName = "";
		
		updateAvailable = 0;
		//updateFile = ""; // TODO: entfernen
		updateMode = 0;
		updateAllowed = 1;	// Standardeinstellung: nur bei WLAN automatisch nach Updates suchen
		doUpdateAtEnd = false;	// es soll in der Activity End im Normalfall KEIN Update durchgeführt werden (nur wenn es explizit angeschafft wurde)
        startupUpdateChecked = 0;
		existingNetworks = new ArrayList<WBnetwork>();
		useNetwork = -1;	// noch unbekannt, wird erst ermittelt
		
		hardKeyConfigActive = false;
		
		camhval = -32000; // -32000 = Wert für *undefiniert*
		camvval = -32000; // -32000 = Wert für *undefiniert*
		
		//Version aus dem AndroidManifest auslesen
		appversionname = "0";
		appversioncode = 0;
		
		try 
		{
		    appversionname = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		    appversioncode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} 
		catch (NameNotFoundException e) 
		{
			Log.e(LOGTAG, e.getMessage());
		}
		

		// OS check
		try
		{
			apiLevel = android.os.Build.VERSION.SDK_INT;
		}
		catch (Exception e)
		{
			apiLevel = 0;	// bei API-Level < 4 (also Android 1.5 und niedriger) (Manifest: MinSDKversion ist auf 4 eingestellt)
		}
		
		String logstart_txt = "\r\n\r\n" + bc.getString(R.string.log_progstart) + " " + bc.getString(R.string.app_name) + " v" + appversionname + " (" + appversioncode + ")";
		logstart_txt += "\r\n" + bc.getString(R.string.log_api) + " = " + apiLevel;
		logstart_txt += "\r\n" + bc.getString(R.string.log_extstate) + " = " +  Environment.getExternalStorageState();
		//logstart_txt += "\r\n" + bc.getString(R.string.log_extpath) + " = " + Environment.getExternalStorageDirectory().getAbsolutePath();
		logstart_txt += "\r\n" + bc.getString(R.string.log_intpath) + " = " + bc.getFilesDir().getPath();
        if (apiLevel >= 8) { logstart_txt += "\r\n" + bc.getString(R.string.log_extpath) + " = " + bc.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(); } // DIRECTORY_DOCUMENTS ist APIlevel 19)

    	AddLogLine(logstart_txt, LOGTAG, wblogtype.Info);
    	
		// WLAN-DL Startwerte
		speedramp_min = 0;
	    speedramp_max = 9;
		
		// Restore preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		speedstufen = settings.getInt("speedstufen", 1024);	// wenn nicht vorhanden, dann Stdwert 1024 (WBcontrol)
		rangiermax = settings.getInt("rangiermax", 200);
		manuelle_ip = settings.getString("manuelle_ip", "10.0.0.1");
		name = settings.getString("name", "myWBController");
		tcpport = settings.getInt("tcpport", 8000);
		udpport = settings.getInt("udpport", 8002);
		use_udp = settings.getBoolean("use_udp", false);
		servertcpport = settings.getInt("servertcpport", 8001);
		loglevel = settings.getInt("loglevel", 1);	// im Problemfall 1: Loglevel=Test 
		useserver = settings.getBoolean("useserver", false);
		ismastercontroller = settings.getBoolean("ismastercontroller", false);
		stop_on_disconnect = settings.getBoolean("stop_on_disconnect", true);
		// String runmode_str = settings.getString("runmode", "standard").toLowerCase(Locale.GERMAN);	// TODO für Echtbetrieb
		String runmode_str = settings.getString("runmode", "test").toLowerCase(Locale.GERMAN);		// für Testphase
		runmode = runmodetype.standard;		// Defaultwert
		if (runmode_str.equals("test")) 	{ runmode = runmodetype.test;}
		else if (runmode_str.equals("master")) 	{ runmode = runmodetype.master;}
		setStartmode(runmode);	// Startwert des runmode sichern
		usermode = settings.getInt("usermode", USERMODE_STANDARD);
		usermode_usepwd = settings.getBoolean("usermode_usepwd", false);
		usermodepwd = settings.getString("usermodepwd", "");
		layouttype = settings.getInt("layouttypel", LAYOUT_CONTROL_L);
		forceDisplaymode = settings.getInt("forceDisplaymode", 0);
		screensavermode = settings.getInt("screensavermode", 0);
		fontScale = settings.getFloat("fontScale", 1);
		themeType = settings.getInt("themeType", THEME_DARK);
		trackplanName = settings.getString("trackplanname", "");
		updateAllowed = settings.getInt("updateAllowed", 1);
		action2_rows = settings.getInt("action2_rows", 3);
		action2_cols = settings.getInt("action2_cols", 3);
		lokstopmode = settings.getInt("lokstopmode", 0);
		
		checkDB();	// DB checken, bei Bedarf anlegen
		
		//Macros und WBevents-Macros müssen noch eingelesen werden
		ReadMacrosFromDB();
		ReadWBeventsFromDB();
		ReadAEfromDB();	// erst nach den Macros geladen werden!!
		ReadDevicesfromDB(); // User created Devices laden

		
		// Diverse Dummies anlegen, wenn keine Echtdaten vorhanden sind
		
		AddMacro(new Macro("leer"), false);	// leeres Macro anlegen, falls nicht vorhanden - für ActionElemente, wenn keine Macros zugeordnet sind (in ActAction_Edit), wird dieses angezeigt (darf nicht verändert werden).

		avStreamlist = ReadAVstreamsfromDB();
		
		
		// TODO (wieder entfernen) Einstellungen/Dummies für Testzwecke - Testmodus
		//Device Taurus = new Device("Taurus", DeviceType.Lok, "10.0.0.1");
		//devicelist.add(Taurus);
		//cdindex = devicelist.indexOf(Taurus);

		
		if (macrolist.size() < 2)
		{
			AddMacro(new Macro("Macro1", Macro.MACROTYP_MACRO));
			AddMacro(new Macro("Macro2", Macro.MACROTYP_MACRO));
			AddMacro(new Macro("Macro3", Macro.MACROTYP_MACRO));
		}

		if (wbeventlist.size() < 2)
		{
			AddWBevent(new Macro("Event 1", Macro.MACROTYP_EVENT));
			AddWBevent(new Macro("Event 2", Macro.MACROTYP_EVENT));
			AddWBevent(new Macro("Event RFID 1", "1234567"));
			AddWBevent(new Macro("Event RFID 2", "12345689"));
		}
		
		if (actionelementlist.size() == 0)
		{
			for (int a=1;a<16;a++)
			{
				int typ;
				if ((a % 2)== 0) { typ = ActionElement.AE_TYP_BUTTON; }
				else  { typ = ActionElement.AE_TYP_ONOFF_BUTTON; }
				actionelementlist.add(new ActionElement("Test" + a, typ));
			}
		}
		
		if ((avStreamlist.size() == 0)|| (runmode == runmodetype.test))	// testweise AV-Streams aus dem Internet // testweise immer neu in DB schreiben
		{
			String[] streamsources = getResources().getStringArray(R.array.streamsources);
			
			int counter=1;
			AVStream avs;
			
			avStreamlist.clear();	// für test
			
			for (String str : streamsources)
			{			
				avs = new AVStream("Teststream " + counter, str);
				avStreamlist.add(avs);
				DBsaveAVstream(avs);
				counter++;
			}
		}
		
		// Testmodus Ende
		
		
		avStreamlist = ReadAVstreamsfromDB();
		
		initBitmapMemCache();
		standardlokpic = BitmapFactory.decodeResource(getResources(), R.drawable.lok_unknown);	// Lok Standardbild vorladen
		// TODO: initBitmap.. sollte eigentlich nicht im UI-Thread passieren
		
		
		// 200ms Timer starten (Achtung: ist eigener Thread)
		fastTimer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() { fastTimer_Tick();  }
				}, 0, 200);
		Basis.AddLogLine(bc.getString(R.string.bas_ftimer_started), LOGTAG, wblogtype.Info);
		
		AddLogLine(checkNetwork(), LOGTAG, wblogtype.Info);	// Info über die verschiedenen Netzwerktypen finden (in Basis speichern) & im Log ausgeben
		
		// Register Broadcast Receiver zur Überwachung des WLAN-Zustandes (mit AP verbunden oder nicht)
		if (WLANreceiver == null) { WLANreceiver = new WlanChecker(); }
		registerReceiver(WLANreceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		
		// für die globalen Broadcasts

		if (Basis.getApiLevel() >= 9)	// DownloadManager erst ab API-Level 9
		{
			BcReceiver = new BroadcastReceiver() {
				@SuppressLint("NewApi")
				@Override
				public void onReceive(Context context, Intent intent) {

					String action = intent.getAction();

					if (Basis.getApiLevel() >= 9)	// DownloadManager erst ab API-Level 9
					{
						if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) 
						{
							long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                            WBupdateParser.WBSoftware software = getSoftware(downloadId);   // checken, ob unter dieser ID ein Download für eine Software angefordert wurde

							if (software != null)   // falls die ID nicht in der SoftwareUpdate_List vermerkt ist, interessiert der Download nicht
							{
								// checken, ob der Download erfolgreich war
								DownloadManager dmanager = (DownloadManager) bc.getSystemService(Context.DOWNLOAD_SERVICE);
								Query query = new Query();
								query.setFilterById(downloadId);
								Cursor c = dmanager.query(query);
								if (c.moveToFirst()) 
								{
									int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) // Download war erfolgreich
                                    {
                                        Basis.AddLogLine(bc.getString(R.string.bas_upd_download_ok), LOGTAG, wblogtype.Info);
                                        //String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                                        software.clearDownloadID(); // downloadID muss zurückgesetzt werden
                                        software.downloaded = true;
                                        //TODO: localBroadcast, falls nötig (Updates-Management-Anzeige aktualisieren, falls offen)

                                        if (software.name.equals("wbcontrol")) {
                                            if (updateMode == 1)    // nur Download -> fertig
                                            {
                                                //textView_status.setText(getString(R.string.basis_upd_download_ready));
                                            } else if (updateMode == 2)  // Download + Installieren
                                            {
                                                Basis.setDoUpdateAtEnd(true);
                                                sendLocalBroadcast(ACTION_PROGUPDATE_DO);
                                            }
                                        }
                                    } else    // Download war fehlerhaft
                                    {
                                        Basis.AddLogLine(bc.getString(R.string.err_upd_download), LOGTAG, wblogtype.Error);
                                        // TODO: DownloadID ebenfalls löschen oder was anderes machen??
                                    }
                                }
							}
						}
					}

				}
			};

			if (Basis.getApiLevel() >= 9)
			{
				getApplication().registerReceiver(BcReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
			}
		}
		
	}	// end onCreate();
	
	
	public int onStartCommand(Intent intent, int flags, int startId)
	{    
		// wird nach onCreate ausgeführt UND bei jedem startService()-Aufruf (auch wenn der Basis-Service bereits läuft)
		startCheck();
		
		
		// Notification für Basis-Service starten
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setSmallIcon(getNotificationIcon());
		mBuilder.setContentTitle(getResources().getString(R.string.bas_svc_notification_title));
		mBuilder.setContentText(getResources().getString(R.string.bas_svc_notification_info_running));
		mBuilder.setOngoing(true);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, End.class);  // damit man den Basis-Service aus der notification beenden kann
		// The stack builder object will contain an artificial back stack for the started Activity.
		// This ensures that navigating backward from the Activity leads out of your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(End.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);	
		mNotificationManager.notify(BASIS_SERVICE_NOTIFICATION_ID, mBuilder.build());

		// We want this service to continue running until it is explicitly stopped, so return sticky (=1).
		return 1;
	}

    // in Android Lollipop hat sich der Style der Notification icons geändert, daher hier jeweils das passende drawable auswählen
    private int getNotificationIcon() {
        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.drawable.lokw : R.drawable.icon;
    }
	
	private void startCheck()
	{
		// Funktionen ausgelagert aus onStartCommand(), weil dieser bei Android 1.6 noch nicht existiert
		// ist wichtig für Wiedereinstieg ins Programm, wenn Netzwerk noch nicht läuft

		int neterror = startNetwork();	// WLAN checken, UDPWaiter und fastTimer starten 
		//	1: WLAN nicht aktiv (wifistate != 3), 2: IP konnte nicht ermittelt werden
		// 4: IP ist keine gültige IPv4-Adresse
		/* folgende Fehler-Kombinationen können auftreten:
				0 - ok
				1
				2
				1+2 -> 1
				4
				1+4 -> 1
		 */

		// als letzte Aktion im onCreate: LocalBroadcast an Startup-Activity senden, damit weitergemacht werden kann


			if (neterror == 0 ) { sendLocalBroadcast(ACTION_BASIS_READY); }

			else if ((neterror & NETWORK_ERR_WLAN_NOT_ENABLED) == NETWORK_ERR_WLAN_NOT_ENABLED )	// auch bei 1+2 oder 1+4
			{
				AddLogLine(bc.getString(R.string.bas_wlan_not_active), LOGTAG, wblogtype.Error);
                sendLocalBroadcast(ACTION_STARTUP_NETWORK_ERROR , "errorcode", NETWORK_ERR_WLAN_NOT_ENABLED);
			}
			else if ((neterror & NETWORK_ERR_NO_IP) == NETWORK_ERR_NO_IP )
            {
                AddLogLine(bc.getString(R.string.bas_no_ip), LOGTAG, wblogtype.Error);
                sendLocalBroadcast(ACTION_STARTUP_NETWORK_ERROR , "errorcode", NETWORK_ERR_NO_IP);
            }
            else if ((neterror & NETWORK_ERR_INVALID_IP) == NETWORK_ERR_INVALID_IP )
            {
                AddLogLine(bc.getString(R.string.bas_ip_no_v4), LOGTAG, wblogtype.Error);
                sendLocalBroadcast(ACTION_STARTUP_NETWORK_ERROR , "errorcode", NETWORK_ERR_INVALID_IP);
            }
            else if ((neterror & NETWORK_ERR_WLAN_NOT_CONNECTED) == NETWORK_ERR_WLAN_NOT_CONNECTED)
            {
                AddLogLine(bc.getString(R.string.bas_wlan_not_connected), LOGTAG, wblogtype.Error);
                sendLocalBroadcast(ACTION_STARTUP_NETWORK_ERROR , "errorcode", NETWORK_ERR_WLAN_NOT_CONNECTED);
            }

	}


	@Override
	public void onDestroy() {
		
		
		if (BcReceiver != null) { try { unregisterReceiver(BcReceiver); } catch (Exception e) { } }	// TODO: Exception abfangen, falls Receiver nicht registriert ist!!
		if (WLANreceiver != null) { try { unregisterReceiver(WLANreceiver); }  catch (Exception e) { } }	// WLAN-Status-Überwachung deaktivieren TODO: catch exception
		
		// fastTimer stoppen
		if (fastTimer != null) fastTimer.cancel();
		
		if (UDPwaiterThread != null) // UDP-Service beenden
    	{
    		Intent svc = new Intent(bc, UDPWaiter.class);
    		bc.stopService(svc);
    		UDPwaiterThread = null;
    	}
		
		// SharedPreferences abspeichern!		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("speedstufen", speedstufen);
		editor.putInt("rangiermax", rangiermax);
		editor.putString("manuelle_ip", manuelle_ip);
		editor.putString("name", name);
		editor.putInt("tcpport", tcpport);
		editor.putInt("udpport", udpport);
		editor.putBoolean("use_udp", use_udp);
		editor.putInt("servertcpport", servertcpport);
		editor.putInt("loglevel", loglevel);
		editor.putBoolean("useserver", useserver);
		editor.putBoolean("ismastercontroller", ismastercontroller);
		editor.putString("runmode", runmode.toString());
		editor.putInt("usermode", usermode);
		editor.putBoolean("usermode_usepwd", usermode_usepwd);
		if (usermodepwd == null) { usermodepwd = ""; }
		editor.putString("usermodepwd", usermodepwd);
		editor.putBoolean("stop_on_disconnect", stop_on_disconnect);
		editor.putInt("layouttypel", layouttype);
		editor.putInt("forceDisplaymode", forceDisplaymode);
		editor.putInt("screensavermode", screensavermode);
		editor.putFloat("fontScale", fontScale);
		editor.putInt("themeType", themeType);
		editor.putString("trackplanname", trackplanName);
		editor.putInt("updateAllowed", updateAllowed);
		editor.putInt("action2_rows", action2_rows);
		editor.putInt("action2_cols", action2_cols);
		editor.putInt("lokstopmode", lokstopmode);
		editor.commit();	// Commit the edits!
		
		// userCreated devices, ActionElemente, WBevents und Macros müssen noch gespeichert werden
		WriteDevicestoDB(); 
		WriteAEtoDB();
		WriteMacrosToDB();
		WriteWBeventsToDB();
		
		saveLog();	// log abspeichern
		
		// Notification entfernen
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(BASIS_SERVICE_NOTIFICATION_ID);

		super.onDestroy();
	}
  
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// andere Methoden
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


	// existierende Netzwerke (verbunden oder nicht) werden ermittelt - und welches verbunden ist
	public static String checkNetwork()
	{
		String data = bc.getString(R.string.bas_avail_netw);	//Available Networks:\r\n
		Boolean wlanIsConnected = false;
		NetworkInfo[] nws;

		ConnectivityManager cm = (ConnectivityManager) bc.getSystemService(Context.CONNECTIVITY_SERVICE);
		nws = cm.getAllNetworkInfo();

		for (NetworkInfo n : nws)
		{
			int ntype = n.getType();
			String typename = n.getTypeName();
			String subtypename = n.getSubtypeName();
			String netavail;
			if (n.isAvailable()) { netavail = bc.getString(R.string.bas_net_available); } // network connectivity is possible
			else { netavail = bc.getString(R.string.bas_net_not_available); }
			String state = n.getState().toString();

			data += "Network " + typename + " (Subtyp " + subtypename + ")\r\n";
			data += "State = " + state + ". " + netavail + "\r\n\r\n";

			if (n.getState() != NetworkInfo.State.UNKNOWN)	// wenn es das Netzwerk wirklich gibt -> speichern
			{
				Boolean alreadyAdded = false;
				for (WBnetwork wbn : existingNetworks)	// checken, ob Network bereits vermerkt
				{
					if (wbn.getTyp() == ntype) { alreadyAdded = true; }
				}

				if (!alreadyAdded) 
				{
					existingNetworks.add( new WBnetwork(ntype, typename));
					//if (ntype == ConnectivityManager.TYPE_WIFI) { wlannet = newnetwork; }	// merken, wenn WLAN vorhanden ist -> standardmäßig dann immer WLAN verwenden (wenn was anderes verwendet werden soll, muß umgestellt werden)
				}

			}
		}

		if (existingNetworks.size() == 1) 
		{
			useNetwork = existingNetworks.get(0).getTyp(); 
			if (existingNetworks.get(0).getState() == NetworkInfo.State.CONNECTED) { networkIsConnected = true; }
		} 
		else // wenn mehrere vorhanden sind, checken, ob wlan verfügbar ist (sonst das erste verbundene verwenden)
		{ 
			WBnetwork firstconnected_network = null;
			WBnetwork wlan_network = null;
			Boolean firstfound = false;

			for (WBnetwork wbn : existingNetworks)
			{
				if (wbn.getTyp() == ConnectivityManager.TYPE_WIFI) { wlan_network = wbn; }
				else if (!firstfound)
				{
					if (wbn.getState() == NetworkInfo.State.CONNECTED) 
					{ 
						firstconnected_network = wbn; 
						firstfound = true;	
					}
				}
			}

			if (wlan_network != null)
			{
				useNetwork = ConnectivityManager.TYPE_WIFI; // standardmäßig immer WLAN verwenden
				if (wlan_network.getState() == NetworkInfo.State.CONNECTED) 
				{ 
					networkIsConnected = true;
					wlanIsConnected = true;
				}
			}

			if (wlan_network == null)// falls WLAN nicht vorhanden ist
			{
				if (firstconnected_network != null)
				{
					if (firstconnected_network.getState() == NetworkInfo.State.CONNECTED) 
					{
						useNetwork = firstconnected_network.getTyp();
						networkIsConnected = true;
					}
				}
			}
			else	// WLAN vorhanden, aber nicht verbunden, dafür ein anderes
			{
				if (!wlanIsConnected)
				{

					if (firstconnected_network != null)
					{
						if (firstconnected_network.getState() == NetworkInfo.State.CONNECTED) 
						{
							useNetwork = firstconnected_network.getTyp();
							networkIsConnected = true;
						}
					}
				}
			}

		}
		return data;	
	}
	
	public static void changeUsedNetwork(int NetworkTyp)
	{
		// TODO: Netzwerk auf den neuen Typ umstellen (WLAN/Mobile/Ethernet)
		
		 //ConnectivityManager.requestRouteToHost
	}
	
	public static String getUseNetworkName()	//Klartextname des Network-Typs aus useNetwork
	{
		String nwname = "";
		
		for (WBnetwork n : existingNetworks)
		{
			if (n.getTyp() == useNetwork) { nwname = n.getTypName(); }
		}
		
		return nwname;

	}


    // startNetwork(): netzwerk-relevanter Teil von onCreate(), wiederholbar, falls WLAN nicht aktiv ist
    // ip-Adresse und broadcast Adresse werden ermittelt, udpservice (udpwaiter) wird gestartet, um udp-messages zu empfangen
	public static int startNetwork()
	{
		eigeneip = null;
		int wifistate = 99;
		String bssid;
		int ipAddress = 0;
		int error = 0;			// 1: WLAN nicht aktiv (wifistate != 3), 2: IP konnte nicht ermittelt werden, 4: IP ist keine gültige IPv4-Adresse
		// 8: WLAN aktiv, aber kein Netzwerk verbunden

		if (useNetwork == ConnectivityManager.TYPE_WIFI)
		{

			try
			{
				// IP-Adresse ermitteln
				WifiManager wifiManager = (WifiManager) bc.getSystemService(WIFI_SERVICE);
				wifistate = wifiManager.getWifiState();
				if (wifistate != 3) { error += NETWORK_ERR_WLAN_NOT_ENABLED; }
				AddLogLine("WIFI-State = " + wifistate , LOGTAG, wblogtype.Info);
				//if (!wifiManager.isWifiEnabled()) { return false; }	// mit Fehler beenden, wenn WLAN disabled -> nicht machen, weil's auch im Test ohne echtes WLAN funktionieren soll

				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				ipAddress = wifiInfo.getIpAddress();
				bssid = wifiInfo.getBSSID();


				if (runmode == runmodetype.standard)	// nur im Testmodus weitermachen, falls kein WLAN aktiv ist
				{
					if (error == NETWORK_ERR_WLAN_NOT_ENABLED) { return error; }
					if (bssid == null) { return NETWORK_ERR_WLAN_NOT_CONNECTED; }
				}
			}
			catch (Exception e)
			{
				AddLogLine("ERROR: bei WLAN-Check: " + e.toString(), LOGTAG, wblogtype.Error);
			}
		}
		if (ipAddress != 0)	{ eigeneip = Formatter.formatIpAddress(ipAddress); }	// TODO: ACHTUNG: funkt nur bei IPv4 !!!!!
		else { eigeneip = getLocalIpAddressNew(); /*getLocalIpAddress();*/ }	// für Tests ohne echtes WLAN (oder bei mobile-Betrieb): irgendeine lokale IP-Adresse ermitteln (-> per config deaktivierbar machen)

		if (eigeneip == null) { error += NETWORK_ERR_NO_IP; return error; }	// mit Fehler beenden, wenn lokale IP-Adresse nicht ermittelt werden konnte


		AddLogLine("eigene IP = " + eigeneip, LOGTAG, wblogtype.Info);
		String[] ipcheck = eigeneip.split("\\.");
		if (ipcheck.length != 4) { error += NETWORK_ERR_INVALID_IP; return error; }	// mit Fehler beenden, lokale IP-Adresse passt nicht
		broadcastip = ipcheck[0] + "." + ipcheck[1] + "." + ipcheck[2] + ".255";
		AddLogLine("Broadcast-IP = " + broadcastip, LOGTAG, wblogtype.Info);

		// TODO: fehlt: wenn nötig einen WifiLock einrichten

		if (error == 0)	{ startUDPService(); }
			
		return error;
	}
	
	private static void startUDPService() 	// UDP-Waiter starten
	{
		if ((UDPwaiterThread == null) && use_udp && networkIsConnected) {	// UDP-Service starten, falls noch nicht gestartet, eine Netzwerkverbindung besteht und udp erlaubt ist
			try {
				AddLogLine("UDPWaiter-Service wird gestartet..", "UDPWaiter", wblogtype.Info);
				Intent svc = new Intent(bc, UDPWaiter.class);
				bc.startService(svc); // war original: startService(svc, Bundle.EMPTY);
				//textView_startup.append("\r\nUDPWaiter-Service wurde gestartet..");
			}
			catch (Exception e) {
				AddLogLine("ERROR: UDPWaiter-Service konnte nicht gestartet werden!", "UDPWaiter", wblogtype.Error);
			}
		}
	}
    
    public static void stopNetwork()	// alles Nötige stoppen, wenn WLAN-Verbindung ausfällt
    {
    	DisconnectAll();	// alle Device-Verbindungen trennen
    	   			
    	if (UDPwaiterThread != null) 
    	{
    		Intent svc = new Intent(bc, UDPWaiter.class);
    		bc.stopService(svc);
    		UDPwaiterThread = null;
    	}
    	
    	eigeneip = null;
    }
    
    
    
    public static void DisconnectAll()
    {
    	if (devicelist != null)
    	{
    		if (devicelist.size()> 0)
    		{
    			for (Device d : devicelist)	// alle TCP-Verbindungen trennen
    			{
    				if (d.isConnected())
    				{
    					try {
    						d.Disconnect();
    					} catch (IOException e) {
    						AddLogLine("Fehler bei DisconnectAll (Devices): " + e.toString(), "Device", wblogtype.Error);
    					}
    				}
    			}
    		}
    		//devicelist.clear();	// nicht clearen -> wg. der vom User angelegten Devices
    		Basis.setCCDevice(null);
            //TODO: locBroadcasts für die Devices, damit die Fragments bescheid wissen (vor allem control)
    	}
    }
    
    // der devicelist ein Device hinzufügen
    public static void AddDevice(Device dev)
    {
    	devicelist.add(dev);
    	sendLocalBroadcast(ACTION_DEVICELIST_CHANGED);
    }

    // der devicelist ein Device hinzufügen, statt Config.DeviceList.Add();
    public static void AddDevice(String devname, DeviceType devtype, String ipadr)
    {
    	devicelist.add(new Device(devname, devtype, ipadr));
    	sendLocalBroadcast(ACTION_DEVICELIST_CHANGED);
    }
    
    public static void AddDevice(String devname, DeviceType devtype, String ipadr, Boolean userCreated)
    {
    	devicelist.add(new Device(devname, devtype, ipadr, userCreated));
    	sendLocalBroadcast(ACTION_DEVICELIST_CHANGED);
    }
    
    public static void RemoveDevice(Device dev)
    {
    	devicelist.remove(dev);
    	sendLocalBroadcast(ACTION_DEVICELIST_CHANGED);
    }
    
    public static int getDeviceListCount(DeviceType typ)
    {
    	int dlistcount = devicelist.size();
    	
    	if (typ == DeviceType.Irgendeins || (dlistcount == 0))
    	{
    		return dlistcount;
    	}
    	else
    	{
    		int count = 0;
    		for (Device d : devicelist)
        	{
        		if (d.getTyp() == typ) { count++; }
        	}
    		return count;
    	}
    }
    
    
    public static void AddLogLine(final String line, final String tag, final wblogtype type)
    {   
    	
    	Runnable r = new Runnable() {
            public void run() {
            	AddLogLineUI(line, tag, type);
            }
    	};

    	if (loghandler != null) { loghandler.post(r); }
    	// im UI-Thread ausführen (nur dort darf der log verändert werden!!) // TODO: checken: Aktion im UI-Thread nicht mehr notwendig?
    }
    
    
    public static void AddLogLineUI(String line, String tag, wblogtype type)
    {      	
    	// im Normalfall (loglevel = 0), werden nur Error und Warnings gespeichert
    	logLock.lock();
    	try {
    		if (type == wblogtype.Info)	// Infos nur bei Loglevel 1 (=Test) speichern
    		{
    			if (loglevel == 1)
    			{ 
    				log.add(new WBlog(line, tag, type));
    				Log.d("WBcontrol", line);
    			}
    		}
    		else
    		{
    			log.add(new WBlog(line, tag, type));
    			Log.d("WBcontrol", line);
    		}

    		if (type == wblogtype.Error)
    		{ 
    			if ((Basis.getRunmode() != runmodetype.test) || (!line.equals("Netwrite war nicht möglich!")))
    			{
    				saveLog();
    			}	// bei einem Error logfile speichern
    		}
    	}
    	finally { logLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall)
    	
    	sendLocalBroadcast(ACTION_NEW_LOGDATA);
    }


    private static String getLogFilePath_old()  // API < 8
    {
        String extpath = Environment.getExternalStorageDirectory().getAbsolutePath();
        return extpath + File.separator + "wbcontrol";
    }

    @TargetApi(8)
    private static String getLogFilePath_new()  // API >= 8
    {
        return  bc.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
    }


    public static void saveLog()    // Logfile im Filesystem speichern
    {
        // Arbeit sollte in eigenem Thread gemacht werden!!

        // checken ob ExternalStorage derzeit beschreibbar ist
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }

        String extpath;
        if (apiLevel >= 8) { extpath = getLogFilePath_new(); }
        else { extpath = getLogFilePath_old(); }

            //String extpath = Environment.getExternalStorageDirectory().getAbsolutePath();    //
        //String extpath = bc.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();
        File f = new File(extpath + File.separator + LOGFILE_NAME);
        File dir = new File(extpath);
        if (!dir.exists()) { dir.mkdirs(); }    // Verzeichnis(se) anlegen

        BufferedOutputStream buffos = null;
        String line;
        Calendar c = new GregorianCalendar();  // This creates a Calendar instance with the current time
        //DecimalFormat zweistellig = new DecimalFormat("00");    // format für Datum: zweistelliger Tag, Monat

        try {
            buffos = new BufferedOutputStream(new FileOutputStream(f, true));
        }    // true für append
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (buffos != null) {
            for (WBlog l : log) {
                c.setTimeInMillis(l.getTime());
                // String date = zweistellig.format(c.get(Calendar.DAY_OF_MONTH)) + "." + zweistellig.format(c.get(Calendar.MONTH) + 1) + ".";
                // String time = zweistellig.format(c.get(Calendar.HOUR_OF_DAY)) + ":" + zweistellig.format(c.get(Calendar.MINUTE)) + ":" + zweistellig.format(c.get(Calendar.SECOND));
                // line = date + " " + time + "\t " + l.getType().toString() + "\t " + l.getTag() + "\t " + l.getText() + "\r\n";

                line = String.format("%1$d.%2$d. %3$d:%4$d:%5$d\t %6$s\t %7$s\t %8$s%n", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH), c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE), c.get(Calendar.SECOND), l.getType().toString(), l.getTag(), l.getText());
                // TODO: Datum universell machen (locale)
                try {
                    buffos.write(line.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                buffos.flush();
                buffos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.clear();    // log nach dem speichern löschen, damit er nicht mehrfach abgespeichert wird.
        AddLogLine(bc.getString(R.string.log_saved), LOGTAG, wblogtype.Info);
    }


    public static void clearLog()		// leert den Log
    {
    	logLock.lock();
    	try { log.clear(); }
    	finally { logLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall)
    	sendLocalBroadcast(ACTION_NEW_LOGDATA);	// Log wird derzeit nur von dort aus gelöscht, wo dieser Broadcast ausgewertet wird TODO: haha, ausser bei einem Error
    }
    
    public static int getLogSize()
    {
    	return log.size();
    }
    
      
    
    public static void SortLogNewestFirst(ArrayList<WBlog> list)		// WBlog-Objekte nach Zeit sortieren
    {
    	Comparator<WBlog> comperator = new Comparator<WBlog>() {

    		@Override
    		public int compare(WBlog object1, WBlog object2) {
    			
    			int result = 0;
    			if (object1.getTime() < object2.getTime()) { result = 1; }
    			else if (object1.getTime() == object2.getTime()) { result = 0; }
    			else if (object1.getTime() > object2.getTime()) { result = -1; }   
    			return result;
    		}
    	};

    	Collections.sort(list, comperator);
    }
    

    public static List<WBlog> getLogNewerThan(int lastold)
    {
        //ArrayList<WBlog> loglist = new ArrayList<WBlog>();
        List<WBlog> loglist = new ArrayList<WBlog>();
        int logsize = log.size();

        if (lastold < logsize)  // log könnte inzwischen geleert worden sein (zB. bei Programmausstieg)
        {
            logLock.lock();
            try
            {
                loglist = log.subList(lastold+1, logsize);
            }
            finally { logLock.unlock(); }
        }

    	return loglist; // im Fehlerfall ist es eine leere Liste!
    }
    

    

    
    public static void AddRFID(String eventname, String rfidcode)   // neues RFID-WBevent hinzufügen (für RFID muss RFIDCode eingetragen werden!)
    {
    	int evindex = getWbeventlistIndexByName(eventname);
    	if (evindex == -1) // checken, ob Name in der List existiert
    	{
    		// Macro neuerevent = new Macro(eventname);
    		if (rfidcode == null) { rfidcode = ""; }	// darf nicht null sein
    		Macro neuerevent =new Macro(eventname, rfidcode);
    		wbeventlist.add(neuerevent);
    	}
    	else
    	{
    		eventname += "_2";
    		AddRFID(eventname, rfidcode);
    	}
    }
    
    public static void AddRFID(Macro rf)   // neues RFID-WBevent hinzufügen (für RFID muss RFIDCode eingetragen werden!)
    {
    	rf.setType(Macro.MACROTYP_RFID);
    	int evindex = getWbeventlistIndexByName(rf.getName());
    	if (evindex == -1) // checken, ob Name in der List existiert
    	{
    		if (rf.getRfidcode() == null) { rf.setRfidcode(""); }	// darf nicht null sein
    		wbeventlist.add(rf);
    	}
    	else
    	{
    		rf.setName(rf.getName() + "_2");
    		AddRFID(rf);
    	}	
    }
    
    public static void AddWBevent(Macro wbe)   // neues WBevent hinzufügen
    {
    	int evindex = getWbeventlistIndexByName(wbe.getName());
    	if (evindex == -1) // checken, ob Name in der List existiert
    	{
    		wbeventlist.add(wbe);
    	}
    	else
    	{
    		wbe.setName(wbe.getName() + "_2");
    		AddMacro(wbe);
    	}
    }
    
    public static int AddActionElement(ActionElement ae)   // neues ActionElement hinzufügen, liefert als Ergebnis den Listenindex zurück
    {
    	actionelementlist.add(ae);
    	return actionelementlist.indexOf(ae);
    }
    
    public static void AddMacro(Macro m, boolean ChangeNameIfAlreadyExists)   // neues Macro hinzufügen
    {
    	int mindex = getMacrolistIndexByName(m.getName());
    	if (mindex == -1) // checken, ob Name in der List existiert
    	{
    		macrolist.add(m);
    	}
    	else
    	{
    		if (ChangeNameIfAlreadyExists)
    		{
    			m.setName(m.getName() + "_2");
    		AddMacro(m);
    		}
    	}
    }
    
    public static void AddMacro(Macro m)   // neues Macro hinzufügen
    {
    	AddMacro(m, true);	// true -> "_2" an den Macro-Namen anhängen, wenn der Name schon existiert
    }
    
    public static int getDevicelistIndexByName(String dname)	// Objekt nach Name suchen und Index zurückgeben
    {
    	int index = -1;		// -1 = not found
    	for (Device d : devicelist)
    	{
    		if (d.getName().equals(dname)) { index = devicelist.indexOf(d);  break; }
    	}
		return index;
    }
    
    public static ArrayList<ActionElement> getActionElementListByLocation(int location) 
    {
		
    	ArrayList<ActionElement> found = new ArrayList<ActionElement>();
    	for (ActionElement ae : actionelementlist)
    	{
    		if (ae.ort == location) { found.add(ae); }
    	}
    	
    	if (found.size() > 0) {	return found; }
    	else { return null; }
    	
	}
    
    public static void updateActionElementListByLocation(ArrayList<ActionElement> aelist_dest, int location) 
    {
    	// aelist_dest leeren und alle passenden AEs aus Basis.actionelementlist in aelist_dest stecken
    	aelist_dest.clear();
    	
    	for (ActionElement ae : actionelementlist)
    	{
    		if (ae.ort == location) { aelist_dest.add(ae); }
    	}
    }
    
    
    public static Device getDevicelistObjectByName(String dname)	// Objekt nach Name suchen und zurückgeben
    {
    	Device founddev = null;		// null = not found
    	for (Device d : devicelist)
    	{
    		if (d.getName().equals(dname)) { founddev = d;  break; }
    	}
		return founddev;
    }
    
    public static Device getDevicelistObjectByName(String dname, DeviceType typ)	// Objekt nach Name suchen und zurückgeben
    {
    	Device founddev = null;		// null = not found
    	for (Device d : devicelist)
    	{
    		if (d.getName().equals(dname)) 
    		{ 
    			if (d.getTyp() == typ) { founddev = d;  break; } 
    		}
    	}
		return founddev;
    }
    
    public static ArrayList<Device> getDevicesByType(DeviceType typ)
    {
    	ArrayList<Device> found = new ArrayList<Device>();
    	for (Device d : devicelist)
    	{
    		if (d.getTyp() == typ) { found.add(d); }
    	}
    	
    	if (found.size() > 0) {	return found; }
    	else { return null; }
    }
    
    public static ArrayList<String> getListofNames(ArrayList<Device> devlist)
    {
    	ArrayList<String> namelist = new ArrayList<String>();

    	if (devlist != null)
    	{
    		for (Device d : devlist)
    		{
    			namelist.add(d.toString()); 
    		}
    	}
    	return namelist;
    }
    
    public static int getWbeventlistIndexByName(String wname)	// Objekt nach Name suchen und Index zurückgeben
    {
    	int index = -1;		// -1 = not found
    	for (Macro w : wbeventlist)
    	{
    		if (w.getName().equals(wname)) { index = wbeventlist.indexOf(w); break; }
    	}
		return index;
    }
    
    public static Macro getWbeventlistObjectByName(String wname, boolean lookingforRFID)	// Objekt nach Name suchen und zurückgeben
    {																				// rfid = true, wenn es ein rFIDs ein soll, false für normalen wbevent
    	Macro foundw = null; //null = not found
    	for (Macro w : wbeventlist)
    	{
    		if (w.getName().equals(wname)) 
    		{
    			if (w.getRfidcode() == null) 
    			{
    				if (!lookingforRFID) {foundw = w; break; } 
    			}
    			else { if (lookingforRFID) {foundw = w; break; } }
    				
    		}
    	}
		return foundw;
    }
    
    public static void RemoveWbevent(String wname, boolean lookingforRFID)
    {
    	Macro m = getWbeventlistObjectByName(wname, lookingforRFID);
    	if (m != null) { wbeventlist.remove(m); }
    }
    
    public static ArrayList<Macro> getWbeventlistOnlyEvents()	// List nur mit Events ohne RFIDs
    {
    	
    	ArrayList<Macro> evlist = new ArrayList<Macro>();
    	
    	for (Macro w : wbeventlist)
    	{
    		if (w.getType() == Macro.MACROTYP_EVENT) { evlist.add(w); }
    	}
		return evlist;
    }
    
    public static ArrayList<Macro> getWbeventlistOnlyRFIDs()	// List nur mit RFID-Events
    {
    	
    	ArrayList<Macro> rfidlist = new ArrayList<Macro>();
    	
    	for (Macro w : wbeventlist)
    	{
    		if (w.getType() == Macro.MACROTYP_RFID) { rfidlist.add(w); }
    	}
		return rfidlist;
    }
    
    public static int getMacrolistIndexByName(String mname)	// Objekt nach Name suchen und Index zurückgeben
    {
    	int index = -1;		// -1 = not found
    	for (Macro m : macrolist)
    	{
    		if (m.getName().equals(mname)) { index = macrolist.indexOf(m); break; }
    	}
		return index;
    }
       
    public static Macro getMacrolistObjectByName(String mname)	// Objekt nach Name suchen und zurückgeben
    {
    	Macro foundm = null; //null = not found
    	for (Macro m : macrolist)
    	{
    		if (m.getName().equals(mname)) { foundm = m; break; }
    	}
		return foundm;
    }
    

	public static void RemoveMacro(String mname)
    {
    	Macro m = getMacrolistObjectByName(mname);
    	if (m != null) { macrolist.remove(m); }
    }
    
    public static int getQuickActionlistIndexByID(int id)	// Objekt nach Name suchen und Index zurückgeben
    {
    	int index = -1;		// -1 = not found
    	for (QAelement q : QuickActionlist)
    	{
    		if (q.ID == id) { index = QuickActionlist.indexOf(q);  break; }
    	}
    	return index;
    }


    public static void AddQuickActionElement(QAelement q)   // neues QuickActionElement hinzufügen
    {
    	int index = getQuickActionlistIndexByID(q.ID);
    	if (index == -1) // checken, ob Name in der List existiert
    	{
    		QuickActionlist.add(q);
    	}
    }
    
    public static void clearQuickActionlist()   // QuickAction-List leeren
    {
    	QuickActionlist.clear();
    }

 
    public static void SendUDP(String data) {	// UDP-Broadcast an alle
        // use_udp = true -> udp darf verwendet werden (siehe Einstellungen: "UDP verwenden")
        if ((use_udp) && (checkWBcontrolCMDtxt(data)))
        {
            new UdpSenderTask().execute(data);
        }
    }

    //WBcontrol-Protokoll-Command auf max. erlaubte Länge checken (Device.CMD_MAXSTRLEN)
    // mehrere cmds auf einmal sind kein Problem. ein einzelner Befehl darf die max. Länge nicht überschreiten, damit die Mikrocontroller nicht überfordert werden
    public static boolean checkWBcontrolCMDtxt(String cmdtxt)
    {
        Boolean CMDisOK = false;


        int firststartpos = cmdtxt.indexOf("<");
        int firstendpos = cmdtxt.indexOf(">");

        //TODO: Achtung: firstendpos könnte nach 2. startpos sein - wird nicht abgecheckt
        if (!((firststartpos >= 0) && (firstendpos > 0) && (firststartpos < firstendpos)))  // gültiger WBcontrol-Befehl enthalten?
        {
            Log.d(LOGTAG, "starting or closing char error!"); // TODO: richties logging
            return false;
        }

        if (firststartpos != cmdtxt.lastIndexOf("<"))     // es sind mehrere Befehle enthalten
        {
            String[] cmds = cmdtxt.split("<");
            Boolean partserror = false;

            for (String cmd : cmds)
            {
                if ((cmd.length()+1) > Device.CMD_MAXSTRLEN)
                {
                    partserror = true;
                    Log.d(LOGTAG, "Command too long! " + cmdtxt); // TODO: richties logging
                }
                if (!cmd.endsWith(">") && (!cmd.equals("")))    // TODO: erster String ist "" -> beachten & checken, ob damit nirgends ein Problem auftritt
                {
                    partserror = true;
                    Log.d(LOGTAG, "Missing closing char! Text: " + cmdtxt); // TODO: richtiges logging
                }
            }

            if (!partserror) { CMDisOK = true; }

        } else    // einfacher check für einen cmd
        {
            if (cmdtxt.length() > Device.CMD_MAXSTRLEN) {
                Log.d(LOGTAG, "Command too long!");
            }  //TODO: Fehler/Warnung loggen, checken, ob mehrere cmds drin sind -> dann einzeln checken -> auch für Basis.SendUDP() -> gemeinsame Basis-fuktion für check
            else { CMDisOK = true; }
        }



        return CMDisOK;
    }

	private static void fastTimer_Tick()    // wird alle 200ms in einem eigenen thread ausgeführt!!
	{
		//AddLogLine("Tick!", "fastTimer", wblogtype.Info);	// für Test

        // keine Speed-ausgabe mehr hier - wird jetzt im Device gemacht
		//Device dev = Basis.getCCDevice();
		// if (dev != null) {	dev.CmdSendSpeed();	}	// send speed and direction to device (loco)
			

		// für alle verbunden Loks die Zeit seit der letzen <sd:nnn> Meldung überprüfen
		// die Zeitspanne darf nie > 2s sein, sonst Alarmmeldung!!
		//getLastlokstatus()
		// TODO: netwaiter setzt bei Datenempfang Lastlokstatus im Device (threadsafe machen!)
		//       und hier wird alle 200ms gecheckt, ob alle Verbindungen aktuell sind. Es kann je nach Geschwindigkeit 
		//       unterschieden werden (stop: 5-10s, bei längerem stop noch länger, bei schneller Fahrt 500ms
		//       check pro Device deaktivierbar machen
		//TODO: das sollte im Device gemacht werde!!!!
		
		if (devicelist.size() > 0)
		{
			for (Device d : devicelist)
			{
				if ((d.getTyp() == DeviceType.Lok) && (d.isConnected()) && (d.isCheckLifesign()))
				{
					if (System.currentTimeMillis() - d.getLastlokstatus() > 2000)
					{
						d.Reconnect();
						// TODO: Alarm - Meldung an Benutzer
						AddLogLine(String.format(bc.getString(R.string.dev_no_response), d.getTyp().toString(), d.getName()), "fastTimer", wblogtype.Warning);
					}
				}
			}
		} 
		
		// TODO: eigene Meldung auch regelmäßig an verbundene Loks senden - fehlt noch


		/*

        if (startmode)     // Aktionen nur während des Search-Mode
        {

            if (tcount >= 25) // bei 200ms -> alle 5s
            {
                tcount = 0;
                try
                {
                    WBbasis.SendUDP("<iamcontrol:" + WBbasis.Name + ">");   // i-am-new Medung an alle, dass der Server seinen Betrieb aufgenommen hat
                }
                catch (Exception err)
                {
                    WBbasis.WriteLogfileLine(err.ToString());
                }
            }
        }

        if (!logpause)  // textbox nur aktualisieren, wenn logpause nicht gesetzt
        {
            textBox_log.Text = WBbasis.GetLogText();
        }


		 */
		
	}
	
	private void checkDB()	// legt DB und Tabellen an, wenn noch nicht vorhanden
	{
        SQLiteDatabase wbDB = this.openOrCreateDatabase(WB_DB_NAME, MODE_PRIVATE, null);
		Basis.wbDBpath = wbDB.getPath();	// für spätere Verwendung speichern (zB. im Device)
		
	    wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_CFG
	                + " (_id integer primary key autoincrement, name TEXT, touok INTEGER);");

		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_MACRO
				+ " (_id integer primary key autoincrement, name TEXT, comment TEXT);");
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_MACRO_CMDS
				+ " (_id integer primary key autoincrement, macroname TEXT, devicename TEXT, command TEXT, ord INTEGER);");
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_WBEVENT
                + " (_id integer primary key autoincrement, name TEXT, rfidcode TEXT, comment TEXT, typ TEXT);");
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_WBEVENT_CMDS
                + " (_id integer primary key autoincrement, eventname TEXT, devicename TEXT, command TEXT, ord INTEGER);");
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_ACTION
                + " (_id integer primary key autoincrement, typ INTEGER, aetext TEXT, macroname TEXT, macroname2 TEXT, scope INTEGER, scopedata TEXT, ort INTEGER, dattype INTEGER);");
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_DEVICES
                + " (_id integer primary key autoincrement, devicename TEXT, typ TEXT, ip TEXT);");
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_IMAGES
                + " (_id integer primary key autoincrement, name TEXT, type INTEGER, uri TEXT);");
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_TRACKPLANS
                + " (_id integer primary key autoincrement, name TEXT, sizew INTEGER, sizeh INTEGER, data TEXT);");		
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_AVSTREAMS
                + " (_id integer primary key autoincrement, name TEXT, source TEXT, type INTEGER);");		
		
		wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_HARDKEYS
                + " (_id integer primary key autoincrement, keycode INTEGER, cname TEXT, keytype INTEGER, function1 INTEGER, function2 INTEGER, macro1 TEXT, macro2 TEXT);");	
		
		// Dummybild-Eintrag für Loks checken/eintragen
		String typname = DeviceType.Lok.toString();
		String where = String.format("type='1' AND name='%s'", typname);	// Bild für Devicetype Lok
		Cursor c = wbDB.query(WB_DB_TABLE_IMAGES, new String[] {"name"}, where, null, null, null, null);
		if (c.getCount() < 1)	// wenn der Eintrag nicht vorhanden ist -> anlegen
		{
			Uri uripic = Uri.parse("android.resource://wb.control/" + R.drawable.lok_unknown);
			String action_sql = "INSERT INTO " + Basis.WB_DB_TABLE_IMAGES + " (name, type, uri)"
					+ " VALUES ('" + typname + "', '1', '" + uripic.toString() + "');";
			wbDB.execSQL(action_sql); 
		}
		if (c != null && !c.isClosed()) {	c.close();	}

        // "terms of use" checken
        c = wbDB.query(WB_DB_TABLE_CFG, new String[] {"name"}, "name='default'", null, null, null, null); // Standard-Einstellungen holen
        if (c.getCount() < 1)	// wenn der Eintrag nicht vorhanden ist -> anlegen
        {
            String action_sql = "INSERT INTO " + Basis.WB_DB_TABLE_CFG + " (name, touok)"
                    + " VALUES ('default', '0');";
            wbDB.execSQL(action_sql);
        }
        if (c != null && !c.isClosed()) {	c.close();	}

		wbDB.close();
	}

    public static boolean getTOUaccepted()
    {
        boolean accepted = false;
        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);    // TODO: die andern openOrCreateDatabase -> openDatabase in Basis
        Cursor c = wbDB.query(WB_DB_TABLE_CFG, new String[] {"touok"}, "name='default'", null, null, null, null); // Standard-Einstellungen holen

        if (c.moveToFirst())	// wenn ein Datensatz vorhanden ist
        {
            if (c.getInt(0) == 1) { accepted = true; }
        }
        else    // TODO: for testing only
        {
            Log.d(LOGTAG, "No default-config in DB");
        }

        c.close();
        wbDB.close();
        return accepted;
    }

    public static void setTOUaccepted()
    {
        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READWRITE);
        ContentValues val = new ContentValues();
        val.put("touok", 1);
        int updated = wbDB.update(WB_DB_TABLE_CFG, val, "name=?", new String[]{"default"});   // UPDATE config SET touok=1 WHERE name='default';
        if (updated != 1) { Log.d(LOGTAG, "touok: es hätte 1 Eintrag in der DB upgedatet werden sollen, es wurden aber " + updated); } // TODO: nach Test entfernen
    }


	
	public void WriteMacrosToDB()	// Macros in der DB speichern   //TODO: Macros usw. endlich bei Bedarf in der DB updaten/löschen!
	{
        SQLiteDatabase wbDB = this.openOrCreateDatabase(WB_DB_NAME, MODE_PRIVATE, null);
		
		// Macro tables leeren
		wbDB.delete(WB_DB_TABLE_MACRO, null, null);
		wbDB.delete(WB_DB_TABLE_MACRO_CMDS, null, null);	

		// Macros speichern
		String macro_sql, cmd_sql;
		wbDB.beginTransaction();
		try {
			for (Macro m : macrolist)
			{	
				String macroname = m.getName();
				String comment = m.getComment();

				if (!macroname.equals("")) 
				{ 
					macro_sql = "INSERT INTO " + WB_DB_TABLE_MACRO + " (name, comment)"
					+ " VALUES ('" + macroname + "', '" + comment + "');";
					wbDB.execSQL(macro_sql); 
				}

				for (Commandset cs : m.getCommandlist())
				{
					String devname = cs.getDevicename();
					int cmdorder = 0;	// Reihenfolge der Befehle pro Device
					for (String befehl : cs.getCommandlist())
					{
						if (!macroname.equals("") && !devname.equals("") && !befehl.equals(""))
						{
							cmd_sql = "INSERT INTO " + WB_DB_TABLE_MACRO_CMDS
							+ " (macroname,devicename,command,ord)"
							+ " VALUES ('" + macroname + "','" + devname +  "','" + befehl +  "','" + cmdorder +  "');";
							cmdorder++;
							wbDB.execSQL(cmd_sql);
						}
					}
				}
			}
			wbDB.setTransactionSuccessful();
		} finally {
			wbDB.endTransaction();
		}
		wbDB.close();
	}
	
	
	
	public static void ReadMacrosFromDB()	// Macros aus der DB laden
	{
        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);
		Cursor c = wbDB.query(WB_DB_TABLE_MACRO, new String[] {"name", "comment"}, null, null, null, null, null);
		
		if (c.moveToFirst()) {
			do {
			
				String macroname = c.getString(0);	// 0: name, 1: comment
				Macro m = new Macro(macroname);
				m.setType(Macro.MACROTYP_MACRO);
				m.setComment(c.getString(1));
				Cursor c2 = wbDB.query(WB_DB_TABLE_MACRO_CMDS, new String[] {"devicename","command"},
						"macroname='" + macroname + "'",	// WHERE
						null, null, null,
						"devicename,ord");	// ORDER BY
				if (c2.moveToFirst()) 
				{
					do {						
						m.AddCommand(c2.getString(0), c2.getString(1));	// devicename: 0, command: 1
		
					} while (c2.moveToNext());
				}
				if (c2 != null && !c2.isClosed()) {	c2.close();	}
				AddMacro(m);
			
			} while (c.moveToNext());
		}
		if (c != null && !c.isClosed()) {	c.close();	}
		
		wbDB.close();
	}
	
	public void WriteWBeventsToDB()	// WBevents in der DB speichern
	{
        SQLiteDatabase wbDB = this.openOrCreateDatabase(WB_DB_NAME, MODE_PRIVATE, null);
		
		// Macro tables leeren
		wbDB.delete(WB_DB_TABLE_WBEVENT, null, null);
		wbDB.delete(WB_DB_TABLE_WBEVENT_CMDS, null, null);	

		// Macros speichern
		String macro_sql, cmd_sql;
		wbDB.beginTransaction();
		try {
			for (Macro w : wbeventlist)
			{	
				String evname = w.getName();
				String comment = w.getComment();
				String rfidcode = w.getRfidcode();
				String typ = "rfid";	// für RFID
				if (rfidcode == null) { typ = "ev"; }	// für wbevent

				if (!evname.equals("")) 
				{ 
					macro_sql = "INSERT INTO " + WB_DB_TABLE_WBEVENT + " (name, comment, rfidcode, typ)"
					+ " VALUES ('" + evname + "', '" + comment + "', '" + rfidcode + "', '" + typ + "');";
					wbDB.execSQL(macro_sql); 
				}

				for (Commandset cs : w.getCommandlist())
				{
					String devname = cs.getDevicename();
					int cmdorder = 0;	// Reihenfolge der Befehle pro Device
					for (String befehl : cs.getCommandlist())
					{
						if (!evname.equals("") && !devname.equals("") && !befehl.equals(""))
						{
							cmd_sql = "INSERT INTO " + WB_DB_TABLE_WBEVENT_CMDS
							+ " (eventname,devicename,command,ord)"
							+ " VALUES ('" + evname + "','" + devname +  "','" + befehl +  "','" + cmdorder +  "');";
							cmdorder++;
							wbDB.execSQL(cmd_sql);
						}
					}
				}
			}
			wbDB.setTransactionSuccessful();
		} finally {
			wbDB.endTransaction();
		}
		wbDB.close();
	}
	
	public static void ReadWBeventsFromDB()	// WBevents aus der DB laden
	{
        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);
		Cursor c = wbDB.query(WB_DB_TABLE_WBEVENT, new String[] {"name", "comment", "rfidcode", "typ"}, null, null, null, null, null);
		
		if (c.moveToFirst()) {
			do {
			
				String evname = c.getString(0);	// 0: name, 1: comment
				Macro w = new Macro(evname);
				w.setComment(c.getString(1));
				w.setRfidcode(c.getString(2));
				if (c.getString(3).equals("ev")) // wbevent kennzeichnen
				{
					w.setRfidcode(null);
					w.setType(Macro.MACROTYP_EVENT);
				}
				else
				{
					w.setType(Macro.MACROTYP_RFID);
				}
				
				Cursor c2 = wbDB.query(WB_DB_TABLE_WBEVENT_CMDS, new String[] {"devicename","command"},
						"eventname='" + evname + "'",	// WHERE
						null, null, null,
						"devicename,ord");	// ORDER BY
				if (c2.moveToFirst()) 
				{
					do {						
						w.AddCommand(c2.getString(0), c2.getString(1));	// devicename: 0, command: 1
		
					} while (c2.moveToNext());
				}
				if (c2 != null && !c2.isClosed()) {	c2.close();	}
				AddWBevent(w);
			
			} while (c.moveToNext());
		}
		if (c != null && !c.isClosed()) {	c.close();	}
		
		wbDB.close();
	}
	
	public void WriteAEtoDB()	// ActionElemente in der DB speichern
	{
        SQLiteDatabase wbDB = this.openOrCreateDatabase(WB_DB_NAME, MODE_PRIVATE, null);
		wbDB.delete(WB_DB_TABLE_ACTION, null, null);	// table leeren

		// Actionelemente speichern
		wbDB.beginTransaction();
		try {
			for (ActionElement ae : actionelementlist)
			{	
				String mname1 = "";
				String mname2 = "";
				if (ae.macro != null) { mname1 = ae.macro.getName(); }
				if (ae.macro2 != null) { mname2 = ae.macro2.getName(); }
					String  action_sql = "INSERT INTO " + WB_DB_TABLE_ACTION + " (typ, aetext, macroname, macroname2, scope, scopedata, ort, dattype)"
					+ " VALUES ('" + ae.typ + "', '" + ae.text +  "', '" + mname1 + "', '" + mname2 + "', '" + ae.scope + "', '" + ae.scopedata +  "', '" + ae.ort  + "', '" + ae.datatype + "');";
					wbDB.execSQL(action_sql); 
			}
			wbDB.setTransactionSuccessful();
		} finally {
			wbDB.endTransaction();
		}
		wbDB.close();
	}	// end WriteAEToDB
	
	public static void ReadAEfromDB()	// ActionElemente aus der DB laden. Darf beim Programmstart erst nach den Macros geladen werden!!
	{
        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);
		Cursor c = wbDB.query(WB_DB_TABLE_ACTION, new String[] {"typ", "aetext", "macroname", "macroname2", "scope", "scopedata", "ort", "dattype"}, null, null, null, null, null);
		
		if (c.moveToFirst()) {
			do {
				ActionElement ae = new ActionElement();	// 0: typ, 1: aetext, 2: macroname, 3: macroname2, 4: scope, 5: scopedata, 6: ort, 7: dattype
				ae.typ = c.getInt(0);
				ae.text = c.getString(1);
				String mname1 = c.getString(2);
				String mname2 = c.getString(3);
				if (!mname1.equals("")) { ae.macro = getMacrolistObjectByName(mname1); }
				if (!mname2.equals("")) { ae.macro2 = getMacrolistObjectByName(mname2); }
				ae.scope = c.getInt(4);
				ae.scopedata = c.getString(5);
				ae.ort = c.getInt(6);
				ae.datatype = c.getInt(7);
						
				AddActionElement(ae);
			} while (c.moveToNext());
		}
		if (c != null && !c.isClosed()) {	c.close();	}
		
		wbDB.close();
	}
	
	public void WriteDevicestoDB()	// manuell angelegte Devices (isUserCreated) in der DB speichern
	{
        SQLiteDatabase wbDB = this.openOrCreateDatabase(WB_DB_NAME, MODE_PRIVATE, null);
		wbDB.delete(WB_DB_TABLE_DEVICES, null, null);	// table leeren

		wbDB.beginTransaction();
		try {
			for (Device d : devicelist)
			{	
				if (d.getIsUserCreated())	// nur UserCreated Devices speichern
				{
					String  action_sql = "INSERT INTO " + WB_DB_TABLE_DEVICES + " (devicename, typ, ip)"
					+ " VALUES ('" + d.getName() + "', '" + d.getTyp().toString() +  "', '" + d.getIP() + "');";
					wbDB.execSQL(action_sql); 
				}
			}
			wbDB.setTransactionSuccessful();
		} finally {
			wbDB.endTransaction();
		}
		wbDB.close();
	}	// end WriteDevicestoDB
	
	
	public static void ReadDevicesfromDB()	//  manuell angelegte Devices (isUserCreated) aus der DB laden.
	{
        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);

		Cursor c = wbDB.query(WB_DB_TABLE_DEVICES, new String[] {"devicename", "typ", "ip"}, null, null, null, null, null);
		
		if (c.moveToFirst()) {
			do {
				String dname = c.getString(0);	// 0: devicename, 1: typ, 2: ip
				DeviceType dtyp = DeviceType.valueOf(c.getString(1));
				String ip = c.getString(2);
				Basis.AddDevice(dname, dtyp, ip, true);
				
			} while (c.moveToNext());
		}
		if (c != null && !c.isClosed()) {	c.close();	}
		
		wbDB.close();
	}
	
	// AV-stream in DB speichern
	
	//wbDB.execSQL("CREATE TABLE IF NOT EXISTS " + WB_DB_TABLE_AVSTREAMS
    //      + " (_id integer primary key autoincrement, name TEXT, source TEXT, type INTEGER);");
	
	public static void DBsaveAVstream(AVStream av)
	{
        SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
		ContentValues dbValues = new ContentValues(); 
		String where = String.format("source='%s'", av.source);	// checken ob bereits ein Sttream mit dieser source gespeichert ist
		Cursor c = wbDB.query(WB_DB_TABLE_AVSTREAMS, new String[] {"source"}, where, null, null, null, null);

		dbValues.put("name", av.name);
		dbValues.put("source", av.source);
		dbValues.put("type", av.type);
		
		if (c.getCount() < 1)	// wenn der Eintrag nicht vorhanden ist -> anlegen
		{
			wbDB.insert(Basis.WB_DB_TABLE_AVSTREAMS, null, dbValues);
		}
		else	// sonst nur updaten
		{
			wbDB.update(Basis.WB_DB_TABLE_AVSTREAMS, dbValues, where, null);
		}
				
		if (c != null && !c.isClosed()) {	c.close();	}
		wbDB.close();
	}
	
	// alle gespeicherten AV-streams aus DB lesen
	ArrayList<AVStream> ReadAVstreamsfromDB()
	{
		ArrayList<AVStream> streamlist = new ArrayList<AVStream>();

        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);
		Cursor c = wbDB.query(WB_DB_TABLE_AVSTREAMS, new String[] {"name", "source", "type"}, null, null, null, null, null);
		
		if (c.moveToFirst()) {
			do {
				// 0: name, 1: source, 2: type
				streamlist.add(new AVStream(c.getString(0), c.getString(1), c.getInt(2)));
	
			} while (c.moveToNext());
		}
		if (c != null && !c.isClosed()) { c.close(); }
		wbDB.close();
		
		return streamlist;
	}
	
	
	
	// einen Hardkey-Datensatz laden
	
	public static HardKey DBloadHardkey(int keycode)
	{
		HardKey hk = null;
        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);

		String where = String.format(Locale.GERMAN,"keycode='%d'", keycode);	
		Cursor c = wbDB.query(WB_DB_TABLE_HARDKEYS, new String[] {"cname", "keytype", "function1", "function2", "macro1", "macro2"}, where, null, null, null, null);
		
		if (c.moveToFirst())	// wenn ein Datensatz vorhanden ist
		{ 
			hk = new HardKey();
			hk.custom_name = c.getString(0);
			hk.keytype = c.getInt(1);
			hk.function1 = c.getInt(2);
			hk.function2 = c.getInt(3);
			hk.macro1 = c.getString(4);
			hk.macro2 = c.getString(5);
		}		
		if (c != null && !c.isClosed()) {	c.close();	}
		wbDB.close();
		return hk;	// returns null when keycode is not found
	}
	
	
	// das komplette HardKey Array aus der DB laden -> ins hardkeys

	public static void DBloadAllHardkeys(SparseArray<HardKey> hardkeys)
	{
		HardKey hk;
		int keycode;
		
		if (hardkeys == null) { hardkeys = new SparseArray<HardKey>(); }
		else { hardkeys.clear(); }

        SQLiteDatabase wbDB = SQLiteDatabase.openDatabase(wbDBpath, null, SQLiteDatabase.OPEN_READONLY);
		Cursor c = wbDB.query(WB_DB_TABLE_HARDKEYS, new String[] {"keycode", "cname", "keytype", "function1", "function2", "macro1", "macro2"}, null, null, null, null, null);

		if (c.moveToFirst())	// wenn ein Datensatz vorhanden ist
		{ 
			do {
				hk = new HardKey();
				keycode = c.getInt(0);
				hk.custom_name = c.getString(1);
				hk.keytype = c.getInt(2);
				hk.function1 = c.getInt(3);
				hk.function2 = c.getInt(4);
				hk.macro1 = c.getString(5);
				hk.macro2 = c.getString(6);
				hardkeys.put(keycode, hk);	// Hardkey ins SparseArray einfügen
			} while (c.moveToNext());
		}	// TODO: Log output falls keine Datensätze vorhanden sind

		if (c != null && !c.isClosed()) {	c.close();	}
		wbDB.close();
	}

	
	
	// einen Hardkey-Datensatz speichern (neu oder ändern)
	
	public static void DBsaveHardkey(int keycode, HardKey hk)
	{
        SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
		ContentValues dbValues = new ContentValues(); 
		String where = String.format(Locale.GERMAN,"keycode='%d'", keycode);	// checken ob bereits ein Datensatz mit diesem keycode gespeichert ist
		Cursor c = wbDB.query(WB_DB_TABLE_HARDKEYS, new String[] {"keycode"}, where, null, null, null, null);

		dbValues.put("keycode", keycode);
		dbValues.put("cname", hk.custom_name);
		dbValues.put("keytype", hk.keytype);
		dbValues.put("function1", hk.function1);
		dbValues.put("function2", hk.function2);
		dbValues.put("macro1", hk.macro1);
		dbValues.put("macro2", hk.macro2);
		
		if (c.getCount() < 1)	// wenn der Eintrag nicht vorhanden ist -> anlegen
		{
			wbDB.insert(Basis.WB_DB_TABLE_HARDKEYS, null, dbValues);
		}
		else	// sonst nur updaten
		{
			wbDB.update(Basis.WB_DB_TABLE_HARDKEYS, dbValues, where, null);
		}
				
		if (c != null && !c.isClosed()) {	c.close();	}
		wbDB.close();
	}
	
	
	// einen Hardkey-Datensatz löschen
	
	public static void DBdelHardkey(int keycode)
	{
        SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
		String where = String.format(Locale.GERMAN,"keycode='%d'", keycode);	
		wbDB.delete(WB_DB_TABLE_HARDKEYS, where, null);
		wbDB.close();
	}

	/*
	public static String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Basis.AddLogLine("Fehler beim Ermitteln der lokalen IP-Adresse: " + ex.toString(), LOGTAG, wblogtype.Error);
	    }
	    return null;
	} */
	
	
	public static String getLocalIpAddressNew() {	// neue Version -> checken TODO
    	try {
    		String ipaddr;
    		ArrayList<NetworkInterface>  nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
    		for (NetworkInterface ni: nilist) 
    		{
    			Basis.AddLogLine("Network: " + ni.getDisplayName(), LOGTAG, wblogtype.Info);
    			ArrayList<InetAddress>  ialist = Collections.list(ni.getInetAddresses());
    			for (InetAddress address: ialist)
    			{
                    ipaddr=address.getHostAddress();

    				Basis.AddLogLine("IP: " + ipaddr, LOGTAG, wblogtype.Info);
    				
    				//if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipaddr))   // InetAddressUtils now deprecated
                     if (!address.isLoopbackAddress() && address instanceof Inet4Address)
    				{ 
    					return ipaddr;
    				}
    			}
    		}
 
    	} catch (SocketException ex) {
    		Basis.AddLogLine(ex.toString(), LOGTAG, wblogtype.Error);
    	}
    	return null;
    }
	
	
	public static void sendLocalBroadcast(String msg)
	{
		if (LocBcManager != null)
		{
            LocBcManager.sendBroadcast(new Intent(msg));
		}
	}

    public static void sendLocalBroadcast(String msg, String extraname, int extravalue)
    {
        if (LocBcManager != null)
        {
            Intent dcIntent = new Intent(msg);
            dcIntent.putExtra(extraname, extravalue);
            LocBcManager.sendBroadcast(dcIntent);
        }
    }

    public static void sendLocalBroadcast(String msg, String extraname, String extravalue)
    {
        if (LocBcManager != null)
        {
            Intent dcIntent = new Intent(msg);
            dcIntent.putExtra(extraname, extravalue);
            LocBcManager.sendBroadcast(dcIntent);
        }
    }
	
	public static void useFontScale(Activity act)	// setzt die in der Basis gespeicherte fontScale für die übergebene Activity
	{
		// wirkt sich aber NUR auf das aktuelle Fragment sofort aus!!
		DisplayMetrics metrics = new DisplayMetrics();
		act.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		Resources res = act.getResources();
		Configuration config = res.getConfiguration();
		config.fontScale = Basis.fontScale;
		res.updateConfiguration(config, metrics);
	}
	
	public static String getThemeTypeName()	// liefert den Namen des gespeicherten Themes
	{
		String tname = "Theme";
		
		switch (Basis.themeType) {
		
		case THEME_DARK:
			//tname = "wbTheme"; // TODO funktioniert nicht
			tname = "Theme.AppCompat";
            tname = "AppTheme";
			break;
			
		case THEME_LIGHT:
			//tname = "wbThemeLight";
			//tname = "Theme.AppCompat.Light";
            tname = "AppTheme.Light";
			break;	
		}
		
		return tname;
	}
	
	
	public static Uri newPictureUri()	// liefert eine File-Uri im "picture"-Verzeichnis (um fotografierte Lokbilder darin zu speichern)
    {
		String picpath;

    	// checken ob ExternalStorage derzeit beschreibbar ist
    	if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) { return null; }

    	picpath = Environment.getExternalStorageDirectory().getAbsolutePath();
    	picpath += File.separator + "Pictures" + File.separator;
        Calendar rightNow = Calendar.getInstance();
    	String filename = "bahnpic" + String.valueOf(rightNow.getTimeInMillis()) + ".jpg";
    	File f = new File(picpath + filename);

    	try {
            if(!f.exists()) // file anlegen
            {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }

        } catch (IOException e) {
            Basis.AddLogLine("Fehler beim Anlegen eines Fotos: " + e.toString(), LOGTAG, wblogtype.Error);
        }

    	return Uri.fromFile(f);
    }
	
	
	public static String getPathFromUri(Uri uri) {
		
		String[] projection = { MediaStore.Images.Media.DATA };
		// Cursor cursor = managedQuery(uri, projection, null, null, null);
		// startManagingCursor(cursor);
		CursorLoader loader = new CursorLoader(bc, uri, projection, null, null, null);
		Cursor cursor = loader.loadInBackground();
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		String path = cursor.getString(column_index);
		cursor.close();
		return path;
		}
	
	
	public static void startCheckOnlineUpdateVersion(boolean manualCheck)
	{
        // manualCheck: true, falls check manuell aufgerufen wurde -> relevant, falls updateAllowed = 0 (bedeutet: nicht automatisch -> nur bei manuellem Aufruf checken )
		if (networkIsConnected)
		{
			if (((updateAllowed > 0) && (useNetwork > 0)) || (updateAllowed == 2) || manualCheck)
			{
                setStartupUpdateChecked(1);  // vorläufig auf 1 -> wenn check fehlschlägt im Task wieder auf 0 setzen!
				String urltxt = bc.getString(R.string.bas_upd_ver_url); 
				new CheckOnlineUpdateTask().execute(urltxt);
			}
			else { Basis.AddLogLine(bc.getString(R.string.bas_upd_not_allowed), LOGTAG, wblogtype.Info); }
		}
		else { Basis.AddLogLine(bc.getString(R.string.bas_upd_no_nw), LOGTAG, wblogtype.Info); }
	}
	
	
	// check whether or not internet (google.com) is available - // TODO: should be back after 2s but needs >30s 
	public static boolean isInternetConnected() {
	    final int CONNECTION_TIMEOUT = 2000;
	        try {
	            HttpURLConnection mURLConnection = (HttpURLConnection) (new URL(bc.getString(R.string.bas_inet_avail_testurl)).openConnection());
	            mURLConnection.setRequestProperty("User-Agent", "ConnectionTest");
	            mURLConnection.setRequestProperty("Connection", "close");
	            mURLConnection.setConnectTimeout(CONNECTION_TIMEOUT);
	            mURLConnection.setReadTimeout(CONNECTION_TIMEOUT);
	            mURLConnection.connect();
	            return (mURLConnection.getResponseCode() == 200);
	        } catch (IOException e) {
	            Basis.AddLogLine(bc.getString(R.string.bas_inet_avail_error) + ": " + e.toString(), "Basis/CheckOnlineUpdateTask", wblogtype.Error);
	        }

	    return false;
	}

	
	public static void showWBtoast(String text)	// Toast mit eigenem Layout anzeigen (wg. zu kleiner Schrift)
	{
		if (bc != null)
		{
			LayoutInflater inflater = (LayoutInflater) bc.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		//getLayoutInflater()
			View layout = inflater.inflate(R.layout.toast, null);

			TextView textView_toast = (TextView) layout.findViewById(R.id.textView_toast);
			textView_toast.setText(text);

			Toast toast = new Toast(bc);
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(layout);
			toast.show();
		}
		
	}

    /*
    // Name des Update-Files ermitteln (falls die softwareUpdate-Liste eingelsen werden konnte (Internetverbindung benötigt!)
    public static String getUpdateFile(int version) {

        String updateFile = "";

        if (SoftwareUpdate_List != null)
        {
            for (WBupdateParser.WBSoftware sw : SoftwareUpdate_List) {

                if (sw.name.equals("wbcontrol") && sw.version == version) {

                    updateFile = sw.filename;
                }
            }
        }

        return updateFile;
    } */

    // gibt die WBSoftware für eine bestimmte version von wbcontrol zurück (die version wurde vorher über den CheckOnlineUpdateTask ermittelt
    // (die neueste verfügbare version kann - nachdem CheckOnlineUpdateTask gelaufen ist - mit getUpdateAvailable() abgefragt werden
    // (version=0, wenn keine neuere verfügbar ist))
    public static WBupdateParser.WBSoftware getUpdateSoftware(int version)
    {
        WBupdateParser.WBSoftware update_sw = null;

        if (SoftwareUpdate_List != null)
        {
            for (WBupdateParser.WBSoftware sw : SoftwareUpdate_List) {

                if (sw.name.equals("wbcontrol") && sw.version == version) { update_sw = sw; }
            }
        }
        return update_sw;
    }

    public static WBupdateParser.WBSoftware getSoftware(long downloadID)
    {
        if (SoftwareUpdate_List != null)
        {
            for (WBupdateParser.WBSoftware sw : SoftwareUpdate_List) {

                if (sw.getDownloadID() == downloadID) {return sw; }
            }
        }
        return null;
    }



	// Bitmap caching funktionen

	private static void initBitmapMemCache()
	{

		// Get max available VM memory, exceeding this amount will throw an OutOfMemory exception. 
		// Stored in kilobytes as LruCache takes an int in its constructor.
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;	// Use 1/8th of the available memory for this memory cache.

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {

				// bitmap.getAllocationByteCount() ab KitKat

				return bitmap.getByteCount() / 1024; // TODO erst ab APILEVEL 12 The cache size will be measured in kilobytes rather than number of items. 
			}
		};

	}

	
	public static synchronized void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public static Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}
	


	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// enums TODO: change enums to int
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


	public enum WBprotokoll   // die möglichen Kommunikationsprotokolle zwischen Devices und Controller
    {
    unbekannt,
    WLAN_DL,
    WBcontrol
    }
	
	
    public enum runmodetype	// für Betriebs(Start)-Modus des Programms
    {
    standard,   // für die User
    test,   // "DrySwim", bei Fehlersuche
    master  // mein Modus
    }
    
    	
    

}	// end class Basis

