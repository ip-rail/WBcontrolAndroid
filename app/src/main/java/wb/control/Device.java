package wb.control;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import wb.control.Basis.WBprotokoll;

public class Device {

	public static final boolean DIRECTION_FORWARD = 	true;
	public static final boolean DIRECTION_BACKWARD = 	false;

    public static final int NETWORK_TYPE_UNKNOWN    = 0;    // Netzwerktyp der Lok. ACHTUNG: bei Änderung: gültiger Wertebereich wird im NetWaiter abgecheckt -> dort anpassen
    public static final int NETWORK_TYPE_SERIAL     = 1;    // "transparentes" serielles Modul, nur 1 tcp port, zB: Wiznet W610wi
    public static final int NETWORK_TYPE_FULL       = 2;    // voller Netzwerksupport, zB. Raspi (kann alles)

	public static final int CMD_MAXSTRLEN			= 64;	// maximal erlaubte Zeichenlänge eines Befehls "<*>" incl. <>
    public static final int CMD_MAX_SENDLEN			= 200;	// maximal erlaubte Zeichenlänge einer Ansammlung von Befehlen, die auf einmal gesendet werden dürfen
                                                            // lokbasis ab v6: UART 0,1 buffer = 256 bytes. Beim raspi dürfte es mehr sein

    private final static String LOGTAG = "device";
	
	private String name;	// Devicename
    private String name_for_change;    // neuer Name, falls eine Änderung durchgeführt werden soll
	private DeviceType typ;
	private String ip;
	private int speed;        //speed vom Controller
	private int speedsteps;	// Speedsteps (128/256/512/1024)
	private int speedmode;		// 0=Speedsteps, 1=km/h
	private int trainspeed;   //aktuelle (letzte) gemeldete Zugsgeschwindigkeit
	private int rangiermax;       	// Max speed wert im Rangiermodus
	private boolean rangiermode;				// zum Speichern des Ragiermodus während anderer Activities
	private boolean dummy;
	private Uri picUri;
	private boolean sliding;	// für WLAN_DL: false: std-speedrampe verwenden. true: schnelle speedrampe verwenden (TODO: r10 - r40 oder so -> testen)
	private boolean checkLifesign;	// legt fest, ob der Alive-Check durchgeführt werden soll (=true) oder nicht (=false)
    private  Timer speedTimer;               // timer für das Intervall für die Speed-Ausgabe (in die NetwriteQueue!) (damit die Lok nicht mit speed-befehlen überflutet wird

	private int[] lastspeed = new int[5];	// die letzten an die Lok gesendeten Speedwerte
    private int[] lasttrainspeed = new int[5];	// die letzten von der Lok gemeldeten Trainspeedwerte
	private boolean richtung;      	// aktuelle Richtung 0: rückwärts, 1: vorwärts
	private boolean lastrichtung;	// Richtung beim letzten Check (für Speed/Richtungausgabe)
	private long pingstart; // Startzeit für Zeitmessung eines Pings (in millisekunden von currentTimeMillis ())
	private long lastlokstatus;	// Zeit der letzten Meldung von verbundener Lok. Zur überprüfung von ausbleibenden Meldungen (in millisekunden von currentTimeMillis ())
	private long lastspeedtime;	// Zeit der letzten Speed-Änderung im Device (nicht an die Lok gesendet!) (damit man bei schnellen Änderungen hintereinander (sliden) eine ander Rampe einstellen kann  (in millisekunden von currentTimeMillis ())
	private String statustxt;	// der komplette (rohe) letzte Statustext
	private ArrayList<wbADCdata> ulist;   // Liste der gemeldeten Spannungen des Devices

	private int motorerror;		// Fehlerstatus des Motor-Controllers (0 = ok)
	
	private Thread netwaiterthread;		// Thread für die TCP-Kommunikation mit dem Device
	private boolean connected;			// zeigt an, ob eine tcp-Verbindung besteht, oder nicht
	private boolean exitthread;			// das checken die Threads (NetWaiter + NetWriter) zyklisch um zu erfahren, ob sie sich beenden sollen
	private boolean tryreconnect;		// die threads sollen versuchen, sich neu zu verbinden (im Falle eines Verbindungsabbruchs)
	private LinkedList<String> NetwriteQueue;	// commands, die ans Device gesendet werden sollen (wird vom NetWaiterThread abgearbeitet) 
	private ReentrantLock NetwriteQueueLock;	// für NetwriteQueue

	//private Hashtable<String, String> wlanconfig;
	private boolean iscd;          // true: ist das aktuelle, zu steuernde Device (auf der Speedbalken-Seite)
	private boolean pause;         // zeigt an, ob das Gerät auf Pause gesetzt ist
	private boolean stopped;       // zeigt an, ob das Gerät gestoppt ist
	private boolean isUserCreated;	// true: das Device wurde vom User im controller angelegt (zB. Lok für Auswahlliste, wenn kein UDP möglich)

	private ArrayList<GPIOport> gpioPortList;   // Liste der GPIO Ports des Devices, die der User verwenden kann

	// WLAN-DL Variablen
    private int speedramp;
    
	// Standard Deviceproperties
	private WBprotokoll dev_protocol; 	// Name des Protokolls, mit dem das Device kommuniziert. Erweiterung: das Device beherrscht mehrere Protokolle (Anzahl, Namen)
	private String dev_swname;          // Name der Software, die auf dem Device läuft
	private String dev_swversion;       // Version der Software, die auf dem Device läuft
	private String dev_owner; 	        // Name des Besitzers (zur Identifikation von Gast-Loks und -Controllern) 
	private String dev_modelname;       // Name des Modell-Typs: bei Loks: Loktyp (zB. Taurus), bei Groundstation: HW-Typ (avr-net-io)
    private ArrayList<Integer> hardwareList;    // TODO: vorerst nur Hardware-ID (see: Ressource array "hardware_id"), später: Hardware-Objekte


	//private Hashtable<String, String> opt_deviceproperties; // für die optionalen Deviceproperties

	// Image lok_bild;
	private int lok_pwmfrequenz;    // derzeit Wert 1-9: 9 Frequenzen zur Auswahl: Frequenztabelle siehe atmega-loco-firmware-lokbasis\funktionen.c
	//List<int> lok_pwmfrange;    // Werte in [Hz] zur Auswahl oder Bereich Wert[0] = Anfangswert, Wert[1] = 0 (=Kennzeichnung), Wert[2] = Endwert
	private boolean lok_motorcontrol;  // Motor-Regelung on/off
	private boolean lok_adc;           // ADC on/off (Testfunktion)
	private int lok_nettype;        // Netzwerktyp der Lok (für Netzwerkfähigkeiten), muss von der Lok gemeldet werden
	private int lok_notstoptimeout;    // Timeout in Sekunden, wenn die TCP-Verbindung verloren geht oder kein Lebenszeichen von der Gegenstelle mehr erhalten wird -> nach x Sekunden stehenbleiben

    //WBevent
    private boolean is_stop_event;      // zeigt an, ob event ausgelöst wurde (muss auch wieder zurückgesetzt werden

    //WBevent listeners
    ArrayList<OnStopListener> stopListeners = new ArrayList<OnStopListener> ();

	
	// Message Typen (msg.what) für uihandler
    //public static final int MSG_END_THREAD 	= 	1;	// netWaiter Thread (und damit tcp-Verbindung) soll beendet werden 
    //public static final int MSG_NETWRITE 	= 	2;	// an netWaiter Thread: ein netwrite senden (data)
    
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// Konstruktor
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	public Device(String devname) {
		this(devname, DeviceType.Lok, null, false);	// in dem Fall ist es eine Lok
		
	}
	
	public Device(String devname, Boolean dummy) {
		this(devname, DeviceType.Lok, null, false);	// in dem Fall ist es eine Dummy-Lok
		
	}

	public Device(String devname, DeviceType devtype, String ipadr) {
		
		this(devname, devtype, ipadr, false);
	}

	//kompletter Konstruktor
	public Device(String devname, DeviceType devtype, String ipadr, Boolean userCreated) {
		name = devname;
        if (name.equals(""))
        {
            Random r = new Random();
            name = "Nemo" + String.valueOf(r.nextInt(200)); // Zufallsname Nemoxxx
        }
        name_for_change = "";
		typ = devtype;
		ip = ipadr;
		connected = false;
		//dev_protocol = WBprotokoll.unbekannt;	// standardmäßig unbekannt setzen!
		dev_protocol = WBprotokoll.WBcontrol;	// TODO: vorerst WBcontrol setzen!!
		richtung = DIRECTION_FORWARD;	// mit Richtung vorwärts beginnen
		lastrichtung = DIRECTION_FORWARD;
		speed = 0;
		speedsteps = Basis.getSpeedStufen();	// Standardwert laden
		rangiermax = Basis.getRangierMax();		// Standardwert laden
		speedmode = 0;
		trainspeed = 0;
		rangiermode = false;	// nicht mit Rangiermode beginnen
		checkLifesign = false;	// TODO: testweise ohne Alivecheck starten 
		// wlanconfig = new Hashtable<String, String>();
		// opt_deviceproperties  = new Hashtable<String, String>();
		this.lastspeed[4] = 0;
		this.lastspeed[3] = 0;
		this.lastspeed[2] = 0;
		this.lastspeed[1] = 0;
		this.lastspeed[0] = 0;
        this.lasttrainspeed[4] = 0;
        this.lasttrainspeed[3] = 0;
        this.lasttrainspeed[2] = 0;
        this.lasttrainspeed[1] = 0;
        this.lasttrainspeed[0] = 0;

		pause = false;
		iscd = false;
		stopped = false;
		exitthread = false;
		tryreconnect = false;
		isUserCreated = userCreated;
		lok_motorcontrol = false;  // Motor-Regelung standardmäßig deaktivieren
		lok_adc = true; 	// standardmäßig aktivieren (wird nur am Prototypen benötigt)
		lastlokstatus = 0;
		lastspeedtime = System.currentTimeMillis();	// damit ein sinnvoller Wert vorhanden ist (0 würde hier nicht passen)
		sliding = false;
		speedramp = 100;	// TODO: vernünftigen Ramp-Standardwert für Einzel-speed-Setzung wählen
        ulist = new ArrayList<wbADCdata>();
        motorerror = 0;	// kein Fehler
        gpioPortList = new ArrayList<GPIOport>();
        hardwareList = new ArrayList<Integer>();

        // Test TODO: wieder weg!!!!
        if (devname.startsWith("testi"))
        {
            gpioPortList.add(new GPIOport(this, GPIOport.GPIO_TYPE_ATMEGA_8BIT, "B", 255, 255, 0) );
            gpioPortList.add(new GPIOport(this, GPIOport.GPIO_TYPE_ATMEGA_8BIT, "C", 0xa5, 0, 0) );
        }

        lok_nettype = NETWORK_TYPE_UNKNOWN;

	    NetwriteQueue = new LinkedList<String>();
	    NetwriteQueueLock = new ReentrantLock();

		// DeviceType checks
	    if (devtype == null) {devtype = DeviceType.Unbekannt; }
	    if (devtype == DeviceType.Camera) { dev_protocol = WBprotokoll.WBcontrol; }
	    
	    setPicUri(getDevicePicUri(this));	// passendes Bild aus der DB suchen -> async machen?
	}
	
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// get/set
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

    public void setNameForChange(String name) {
        this.name_for_change = name;
    }

    public String getNameForChange() {
        return name_for_change;
    }

	public void setTyp(DeviceType t) {
		this.typ = t;
	}

	public DeviceType getTyp() {
		return typ;
	}
	
	public void setIP(String ip) {
		this.ip = ip;
	}

	public String getIP() {
		return ip;
	}
	
	public void setSpeed(int speed) {

        /*
		if ((System.currentTimeMillis() - lastspeedtime) > 200) { sliding = false; }	// wenn der letzte Speed-Wert vor mehr als 200ms gemeldet wurde, die vom User eingestelle speed-Rampe verwenden
		else { sliding = true; }	// wenn auf der Speedbar geslidet wird, dann eine schnelle Rampe verwenden!	
		// TODO: sliding: das funktioniert nicht (nie true)
		*/

		int speedmax = Basis.getSpeedStufen() - 1;
		// TODO: achtung derzeit für alle Devices gleich  - in der Basis gesetzt (wird sich ändern!!) -> ins init verlegen, dort standardwert, dann von lok auslesen bzw. gespeicherte lokdaten verwenden
		if (speed > speedmax) { speed = speedmax; }
		if (speed < 0) { speed = 0; }
		this.speed = speed;
	}

	public int getSpeed() {
		return speed;
	}

	public void setTrainspeed(int trainspeed_new) {

        setLasttrainspeed(trainspeed);

        int trainspeed_old = trainspeed;
		trainspeed = trainspeed_new;
        if (trainspeed != lasttrainspeed[0])   // TODO: eigentlich überflüssig, wird im netwaiter schon abgefangen
        {
            notifyChangedAEData(ActionElement.AE_DATATYPE_TRAINSPEED);	// Daten in Actionelement aktualisieren
            if (getIscd()) { sendStdLocBroadcast(Basis.ACTION_UPDATE_TRAINSPEED); } // wenn des das Device für die Speedbar ist
        }

        checkStopEvent();   // checken, ob die Bedingungen für den StopEvent erfüllt sind und ggf. event einmalig auslösen
	}
	
	public int getTrainspeed() {
		return trainspeed;
	}


    public int getLasttrainspeed() { return lasttrainspeed[0]; }

    public void setLasttrainspeed(int lasttspeed) 	// nur den einen letzten Wert dem Array hinzufügen (alle rücken weiter, ältester fliegt raus)
    {
        this.lasttrainspeed[4] = this.lasttrainspeed[3];
        this.lasttrainspeed[3] = this.lasttrainspeed[2];
        this.lasttrainspeed[2] = this.lasttrainspeed[1];
        this.lasttrainspeed[1] = this.lasttrainspeed[0];
        this.lasttrainspeed[0] = lasttspeed;
    }

	public Boolean getRangiermode() {
		return rangiermode;
	}

	public void setRangiermode(Boolean rangiermode) {
		this.rangiermode = rangiermode;
	}

	public boolean isCheckLifesign() {
		return checkLifesign;
	}

	public void setCheckLifesign(boolean check) {
		this.checkLifesign = check;
	}

	public void setLastspeed(int lastspeed) {	// nur den einen letzten Wert dem Array hinzufügen (alle rücken weiter, ältester fliegt raus)
		this.lastspeed[4] = this.lastspeed[3];
		this.lastspeed[3] = this.lastspeed[2];
		this.lastspeed[2] = this.lastspeed[1];
		this.lastspeed[1] = this.lastspeed[0];
		this.lastspeed[0] = lastspeed;
	}

	public int getLastspeed() {
		return lastspeed[0];
	}
	
	public int[] getAllLastspeeds() {
		return lastspeed;
	}

	public void setRichtung(boolean richtung) {
		this.richtung = richtung;
	}

	public boolean getRichtung() {
		return richtung;
	}

	public boolean getLastrichtung() {
		return lastrichtung;
	}

	public void setLastrichtung(boolean lastrichtung) {
		this.lastrichtung = lastrichtung;
	}

	public void setPingstart(long pingstart) {
		this.pingstart = pingstart;
	}

	public long getPingstart() {
		return pingstart;
	}

	public void setLastlokstatus(long lastlokstatus) {
		this.lastlokstatus = lastlokstatus;
	}

	public long getLastlokstatus() {
		return lastlokstatus;
	}

	public void setU(int index, int value)
    {
        wbADCdata adc = null;

        try
        {
            adc = ulist.get(index);
        }
        catch (IndexOutOfBoundsException e)
        {
            ulist.add(index, new wbADCdata(index, value));
        }

        if (adc != null ) { adc.value = value; }
        notifyChangedAEData(ActionElement.AE_DATATYPE_U_ADC0+index);  // datatype = 1 bis 8
    }

    public String getUString(int index)
    {
        String ustr;
        wbADCdata adc = null;

        try
        {
            adc = ulist.get(index);
        }
        catch (IndexOutOfBoundsException e)
        {
            Basis.AddLogLine(String.format(Basis.getBcontext().getString(R.string.dev_no_udata), index, this.getName()), LOGTAG, WBlog.wblogtype.Warning);
        }

        if (adc != null ) { ustr = adc.getVoltageString(); }
        else { ustr = "xxx"; }

        return ustr;
    }
	
	public void setNetwaiterThread(Thread t) {
		this.netwaiterthread = t;
	}

	public Thread getNetwaiterThread() {
		return netwaiterthread;
	}
	
	/*
	public void setWlanconfig(Hashtable<String, String> wlanconfig) {
		this.wlanconfig = wlanconfig;
	}

	public Hashtable<String, String> getWlanconfig() {
		return wlanconfig;
	} */

	public void setIscd(Boolean iscd) {
		this.iscd = iscd;
	}

	public Boolean getIscd() {
		return iscd;
	}
	
	public void setPause(Boolean pause) {
		this.pause = pause;
	}

	public Boolean getPause() {
		return pause;
	}

	public void setStopped(Boolean stopped) {
		this.stopped = stopped;
	}

	public Boolean getStopped() {
		return stopped;
	}

	public Boolean getIsUserCreated() {
		return isUserCreated;
	}

	public void setIsUserCreated(Boolean isUserCreated) {
		this.isUserCreated = isUserCreated;
	}

    public ArrayList<GPIOport> getGpioPortList() { return gpioPortList; }

    public void setDev_protocol(WBprotokoll dev_protocol) {
		this.dev_protocol = dev_protocol;
	}

	public WBprotokoll getDev_protocol() {
		return dev_protocol;
	}

	public void setDev_swname(String dev_swname) {
		this.dev_swname = dev_swname;
	}

	public String getDev_swname() {
		return dev_swname;
	}

	public void setDev_swversion(String dev_swversion) {
		this.dev_swversion = dev_swversion;
	}

	public String getDev_swversion() {
		return dev_swversion;
	}

	public void setDev_owner(String dev_owner) {
		this.dev_owner = dev_owner;
	}

	public String getDev_owner() {
		return dev_owner;
	}

	public void setDev_modelname(String dev_modelname) {
		this.dev_modelname = dev_modelname;
	}

	public String getDev_modelname() {
		return dev_modelname;
	}

	/*
	public void setOpt_deviceproperties(Hashtable<String, String> opt_deviceproperties) {
		this.opt_deviceproperties = opt_deviceproperties;
	}

	public Hashtable<String, String> getOpt_deviceproperties() {
		return opt_deviceproperties;
	} */

	public void setLok_pwmfrequenz(int lok_pwmfrequenz) {
		this.lok_pwmfrequenz = lok_pwmfrequenz;
	}

	public int getLok_pwmfrequenz() {
		return lok_pwmfrequenz;
	}

	public void setLok_motorcontrol(Boolean lok_motorcontrol) {
		this.lok_motorcontrol = lok_motorcontrol;
	}

	public Boolean getLok_motorcontrol() {
		return lok_motorcontrol;
	}

	public void setLok_adc(Boolean lok_adc) {
		this.lok_adc = lok_adc;
	}

	public Boolean getLok_adc() {
		return lok_adc;
	}

    public void setLok_nettype(int type) { lok_nettype = type;  }

    public int getLok_nettype() {
        return lok_nettype;
    }

	public void setLok_notstoptimeout(int lok_notstoptimeout) {
		this.lok_notstoptimeout = lok_notstoptimeout;
	}

	public int getLok_notstoptimeout() {
		return lok_notstoptimeout;
	}
	

	public void setSpeedramp(int ramp) {
		this.speedramp = ramp;
		cmdSendSpeedRamp();
	}

	public int getSpeedramp() {
		return this.speedramp;
	}
	
	public int getSpeedsteps() {
		return speedsteps;
	}

	public int getRangiermax() {
		return rangiermax;
	}

	public void setRangiermax(int rangiermax) {
		this.rangiermax = rangiermax;
	}

	public void setSpeedsteps(int speedsteps) {
		this.speedsteps = speedsteps;
	}

	public Uri getPicUri() {
		return picUri;
	}

	public void setPicUri(Uri picUri) {
		this.picUri = picUri;
	}

	public Boolean getDummy() {
		return dummy;
	}

	public void setDummy(Boolean dummy) {
		this.dummy = dummy;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public int getSpeedmode() {
		return speedmode;
	}

	public void setSpeedmode(int speedmode) {
		this.speedmode = speedmode;
	}
	
	
	public synchronized void setExitThread(boolean exit)
	{
		exitthread = exit;
	}
	
	public synchronized boolean getExitThread() {
		return exitthread;
	}
	
	
	public boolean isTryreconnect() {
		return tryreconnect;
	}

	public void setTryreconnect(boolean tryreconnect) {
		this.tryreconnect = tryreconnect;
	}

	
	public String getStatustxt() {
		return statustxt;
	}

	public void setStatustxt(String statustxt) {
		this.statustxt = statustxt;
	}

	public int getMotorerror() { return motorerror; }
	public void setMotorerror(int error) { this.motorerror = error; }
	
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// andere Methoden
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	

	@Override
    public String toString()
    {
		return this.name;
    }
	


	public boolean Connect()	// throws IOException 
	{
		// vom User angelegte Devices ohne IP-Adresse gleich abfangen
		if (this.ip.equals("")) {  return false; }	// TCPsocket = null; // war noch dabei

		exitthread = false;		// Kennung für Thread zum Beenden entschärfen
		netwaiterthread = new Thread(new NetWaiter(this), "NetWaiter_" + this.name);	// dem NetWaiter das aktuelle Device übergeben!
		netwaiterthread.start();
        startSpeedTimer();
		return true;
	}

	public void Disconnect() throws IOException 
	{
		//dem UI wird der Disconnect vom NetWaiter signalisiert (wird hier durch exitthread = true ausgelöst)
        stopSpeedTimer();   // es werden keine speed-befehle im Intervall mehr gesendet
		if (getDev_protocol() == WBprotokoll.WBcontrol) { Netwrite("<bye>"); }
        if (Basis.isStop_on_disconnect()) // bei disconnect speed zuerst auf 0 stellen??
        {
            speed = 0;
            cmdSendSpeed(); // und nochmal ausgeben
        }

		try { Thread.sleep(200); } // 350ms warten
		catch (InterruptedException e) { /* ignore error */ }
        iscd = false;
		exitthread = true;	// Kennung für Threads, dass sie sich beenden sollen (Msg wäre hier zu aufwendig)
	}
	
	public void Reconnect() 
	{
		tryreconnect = true;	// wird vom NetWaiter nach Start des Reconnect wieder auf false gesetzt
	}

    private void startSpeedTimer()
    {
        if (speedTimer != null) { speedTimer.cancel();  }
        speedTimer = new Timer("speedTimer_" + name);

        // 200ms Timer starten (Achtung: ist eigener Thread)
        speedTimer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() { speedTimer_Tick();  }
                }, 0, 125);     //125ms TODO: konfigurierbar machen! und dynamisch, falls sich nicht tut!!
    }

    private void stopSpeedTimer()
    {
        if (speedTimer != null) { speedTimer.cancel();  }
        speedTimer = null;
    }

    private void speedTimer_Tick()
    {
        cmdSendSpeed();
		Netwrite(Basis.getBcontext().getString(R.string.cmd_alive));    // TODO: alive-Meldung noch verbessern (zB. nur senden, wenn im CmdSendSpeed() nichts gesendet wurde)
    }



    public void Netwrite(String text) // Msg an Thread senden (mit Text). Der Rest wird jetzt im thread gemacht!!
	{
        // TODO: netwrite not connected cmds puffern? nur gewisse?

        Boolean cmdok = true;

        if (dev_protocol == WBprotokoll.WBcontrol)
        {
            cmdok = Basis.checkWBcontrolCMDtxt(text);   // zu lange oder fehlerhafte WBcontrol cmds dürfen nicht gesendet werden!
        }

		if (connected && cmdok) {
			NetwriteQueueLock.lock();
			try 
			{
				NetwriteQueue.add(text);
			}
			finally { NetwriteQueueLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall)
		}
	}
	
	public String getNextCmdtoSend()
	{
		String data = null;
		
		NetwriteQueueLock.lock();
    	try 
    	{
    		data = NetwriteQueue.poll();
    	}
    	finally { NetwriteQueueLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall
		
		return data;
	}

    // holt alle vorhandenen Befehle bis strlen = CMD_MAX_SENDLEN
	public String getNextCmdsToSend()
	{
		String data = null;

        NetwriteQueueLock.lock();
		try
		{
            if (NetwriteQueue.size() > 1)   // falls mehrere CMDs vorhanden sind
            {
                StringBuilder commands = new StringBuilder();
                String data1 = NetwriteQueue.poll();
                commands.append(data1);
                int nextlen = NetwriteQueue.getFirst().length();

                while ((commands.length()+nextlen) <= CMD_MAX_SENDLEN)
                {
                    data1 = NetwriteQueue.poll();
                    if (data1 != null)
                    {
                        commands.append(data1);
                        try { nextlen = NetwriteQueue.getFirst().length(); }
                        catch (NoSuchElementException e) { nextlen = 0; }
                    }
                    else { break; }
                }
                data = commands.toString();
            }
            else
            {
                data = NetwriteQueue.poll();
            }

		}
		finally { NetwriteQueueLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall)

		return data;
	}
	
	
	public Uri getDevicePicUri(Device d)	// liefert für jedes Device eine passende Uri aus der Datenbank (Table images)
	{		
		Uri picUri;
		SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
		picUri = getUriFromDB(wbDB, String.format("type='3' AND name='%s'", d.getName())); // uri für das Device suchen
		
		if (picUri == null)	// wenn's für das device keine Uri gibt, nach einer Uri für den Dev_modelname suchen (->Loktyp)
		{
			String modelname = d.getDev_modelname();
			if ((modelname != null) && (!modelname.equals(""))) 
			{
				picUri = getUriFromDB(wbDB, String.format("type='2' AND name='%s'", modelname));
			}
		}
		
		if (picUri == null)	// wenn's für das Dev_modelname keine Uri gibt, nach einer Uri für den DeviceType suchen (Lok/Controller..)
		{
			picUri = getUriFromDB(wbDB, String.format("type='1' AND name='%s'", d.getTyp().toString()));
		}
		
		wbDB.close();
		return picUri;
	}
	
	private Uri getUriFromDB(SQLiteDatabase db, String where)	// Unterfunktion für getDevicePicUri()
	{
		Uri picUri = null;

		Cursor c = db.query(Basis.WB_DB_TABLE_IMAGES, new String[] {"uri"}, where, null, null, null, null);

		if (c.moveToFirst()) 
		{ 
			String uristring = c.getString(0);
			picUri = Uri.parse(uristring);
		}
		if (c != null && !c.isClosed()) {	c.close();	}

		return picUri;
	}
	
	public void saveUriPic(Uri uripic, String name, int type)
	{
		SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
		ContentValues dbValues = new ContentValues(); 
		String where = String.format("type='%s' AND name='%s'", String.valueOf(type), name);	// Bild für Devicetype Lok
		Cursor c = wbDB.query(Basis.WB_DB_TABLE_IMAGES, new String[] {"name"}, where, null, null, null, null);

		if (c.getCount() < 1)	// wenn der Eintrag nicht vorhanden ist -> anlegen
		{
			dbValues.put("name", name);
			dbValues.put("type", type);
			dbValues.put("uri", uripic.toString());
			wbDB.insert(Basis.WB_DB_TABLE_IMAGES, null, dbValues);
		}
		else	// sonst nur updaten
		{
			dbValues.put("uri", uripic.toString());
			wbDB.update(Basis.WB_DB_TABLE_IMAGES, dbValues, where, null);
		}
		if (c != null && !c.isClosed()) {	c.close();	}

		wbDB.close();
	}
	
	private void notifyChangedAEData(int datatype)
	{
		Intent dcIntent = new Intent(Basis.ACTION_UPDATE_AE_DATA);
		dcIntent.putExtra("datatype", datatype);
		dcIntent.putExtra("device", this.name);
		Basis.getLocBcManager().sendBroadcast(dcIntent);
	}

    public void sendStdLocBroadcast(String msg) // (ohne extra data) msg siehe Basis: Message Typen für lokale Broadcasts
    {
        Intent dcIntent = new Intent(msg);
        dcIntent.putExtra("device", getName());
        Basis.getLocBcManager().sendBroadcast(dcIntent);
    }

    // den Namen des Devices wirklich ändern. Neuer Name name_for_change muss gesetzt sein!
    public void changeName()
    {
        if (!name_for_change.equals(""))    // in name_for_change muss eine Name gespeichert sein, sonst wird nichts gemacht
        {
            String oldname = name;
            name = name_for_change;
            name_for_change = "";

            Intent dcIntent = new Intent(Basis.ACTION_DEVICE_NAME_CHANGED); // TODO: überall einbauen control, loklist, lokdetails
            dcIntent.putExtra("device", name);
            dcIntent.putExtra("oldname", oldname);
            Basis.getLocBcManager().sendBroadcast(dcIntent);
        }
    }


    public void checkATMegaGpioPort(String port, int usable_pins, int used_pins, int values)
    {
        // checken, ob Port bereits existiert -> updaten oder addATMegaGpioPort()
        GPIOport existing_port = null;
        for (GPIOport p : gpioPortList)
        {
            if (p.getName().equals(port)) { existing_port = p; break;}
        }

        if (existing_port != null) { existing_port.update(usable_pins,used_pins, values); }
        else { addATMegaGpioPort(port, usable_pins,used_pins, values); }
    }


    public void addATMegaGpioPort(String port, int usable_pins, int used_pins, int values)
    {
        // GPIOport aus den Infos des <gpioi:> Befehls erstellen
        // <gpioi:port:mögliche:verwendete>         char:byte-mask:byte-mask
        GPIOport newport = new GPIOport(this, GPIOport.GPIO_TYPE_ATMEGA_8BIT, port, usable_pins, used_pins, values);
        gpioPortList.add(newport);
        Basis.sendLocalBroadcast(Basis.ACTION_UPDATE_GPIO, "device", this.getName());    // info an Frag_lokgpiocfg, dass sich die Port/Pin-Daten geändert haben
    }


	public ArrayList<GPIOpin> getGPIOUsedPins()
	{
		ArrayList<GPIOpin> usedlist =  new ArrayList<GPIOpin>();

		for (GPIOport port : gpioPortList)
		{
			for (GPIOpin pin : port.getPinList()) { if (pin.isUsed()) { usedlist.add(pin); } }
		}
		return usedlist;
	}

	public ArrayList<GPIOpin> getGPIOUsablePins()
	{
		ArrayList<GPIOpin> usablelist =  new ArrayList<GPIOpin>();

		for (GPIOport port : gpioPortList)
		{
			for (GPIOpin pin : port.getPinList())  { usablelist.add(pin); }
		}
		return usablelist;
	}


    public boolean addHardware(int id)
    {
        boolean error = true;

        String[] hwtype = Basis.getBcontext().getResources().getStringArray(R.array.hardware_id);

        if (id < hwtype.length) { error = false; }    // checken, ob id gültig ist

        if (!error)
        {
            boolean found = false;
            for (int i : hardwareList) { if (i == id) {found = true; break; } }
            if (!found) { hardwareList.add(id); }   // in hardwareList geben, falls noch nicht vorhanden -> es kann jede Hardware nur 1x vorkommen  TODO: checken ob ok!
        }

        return error;
    }

    public String getHardwareNames()
    {
        String hwtxt = "";

        String[] hwtype = Basis.getBcontext().getResources().getStringArray(R.array.hardware_id);

        for (int h : hardwareList)
        {
            hwtxt += hwtype[h] + "\n";      // TODO: noch ändern
        }

        return hwtxt;
    }



    public enum DeviceType {

		// TODO mit Definition abstimmen und Konstanten statt enum verwenden http://10.0.0.73/dokuwiki/doku.php?id=prog:general:devices
		Unbekannt,
		Lok,
		Controller,         // Handy, SteuerungsPC, Table usw. 
		Groundstation,       // Weichensteuerung, Beleuchtung usw..
		Server,
		Camera,				// steuerbare Kamera, zB. Kamerawagen (raspicamserver)
		Irgendeins			// alle (egal welche) Geräte - für Abfragen
	}
	
	
	//------------------------------------ Device-Command-Funktionen --------------------------------------------
	

	public void cmdSendStopall()	// <stopall>
	{
		if (this.getDev_protocol() == WBprotokoll.WBcontrol) { this.Netwrite("<stopall>"); }
		else if (this.getDev_protocol() == WBprotokoll.WLAN_DL) { this.Netwrite("v0"); }	// NOTSTOP bei WLAN_DL
		this.setSpeed(0);

	}
	

	public void cmdSendPauseall(boolean pause)	// <pauseall>
	{
		this.setPause(pause);    // status Pause für Device setzen
		
		if (pause)
		{
			if (this.getDev_protocol() == WBprotokoll.WBcontrol) {	this.Netwrite("<pauseall>"); }
			else if (this.getDev_protocol() == WBprotokoll.WLAN_DL) { this.Netwrite("v0"); }
		}
		else
		{
			if (this.getDev_protocol() == WBprotokoll.WBcontrol) {	this.Netwrite("<pauseall:aus>"); }
			else if (this.getDev_protocol() == WBprotokoll.WLAN_DL) 
			{
				setSpeed(getLastspeed());
				cmdSendSpeed();
			}
		}	
	}
	

	
	public void cmdSendStop()	//<stop>
	{
		this.setSpeed(0);
		if (this.getDev_protocol() == WBprotokoll.WBcontrol) { this.Netwrite("<stop>"); }
		else if (this.getDev_protocol() == WBprotokoll.WLAN_DL) { this.Netwrite("v0"); }
	}
	
	

	
	public void cmdSendSpeed()	// send Speed + Direction
	{
		String nettext;
		
		if (!this.getPause())	// wenn das Device nicht im Pause-Modus ist
		{
			if (this.getDev_protocol() == WBprotokoll.WBcontrol)
			{
				if ((this.getSpeed() !=  this.getLastspeed()) || (this.getRichtung() != this.getLastrichtung()))    // speed senden, wenn er sich geändert hat
				{
					//nettext = "<sd:" + speedformat.format(dev.getSpeed()) + ">";
					nettext = "<sd:" + this.getSpeed() + ">";	// TODO: Richtungsbefehle -> ressources
					this.Netwrite(nettext);
				}
				if (this.getRichtung() != this.getLastrichtung())    // Richtung senden, wenn sie sich geändert hat
				{
					if (this.getRichtung() == Device.DIRECTION_BACKWARD)	{ this.Netwrite("<richtung:rw>"); }	// Rückwärts
					else { this.Netwrite("<richtung:vw>"); }// Vorwärts
				}
			}
			else if (this.getDev_protocol() == WBprotokoll.WLAN_DL)
			{
				if ((this.getSpeed() !=  this.getLastspeed()) || (this.getRichtung() != this.getLastrichtung()))    // speed senden, wenn er sich geändert hat
				{
					if (this.getSpeed() > 0)	// bei Stop wurde das Netwrite schon im Control durchgeführt!
					{
						String dirtxt = "v";	// vorwärts
						if (this.getRichtung() == Device.DIRECTION_BACKWARD) { dirtxt = "r"; }	// rückwärts
						nettext = dirtxt + String.valueOf(this.getSpeed());
						// wenn eine schnelle Speed-Rampe wg. sliden auf der Speedbar verwendet werden soll
						if (sliding) { nettext += " r10"; }	// TODO: schnellen Rampenwert ausprobieren / ev. konfigurierbar machen
						//TODO: Test: speedrampwert immer mitgeben!
						//nettext += " r" + speedramp;
						this.Netwrite(nettext);
					}
				}
			}
			setLastspeed(this.speed); // aktuellen speed als Lastspeed sichern
			this.setLastrichtung(this.getRichtung());	// Richtung bis zum nächsten Check merken
		}
		
	}
	
	// sende Befehle zum Einstellen der Speedrampe der Lok (der zeit nur für WLAN_DL)
	public void cmdSendSpeedRamp()
	{
		if (this.getDev_protocol() == WBprotokoll.WLAN_DL)
		{
			this.Netwrite(String.format(Basis.getBcontext().getString(R.string.cmd_wlandl_ramp), speedramp));
		}
	}
	
	
	// sende Befehle zur Intitalisierung der Lok
	public void cmdSendInit()
	{

        if (this.getDev_protocol() == WBprotokoll.WBcontrol)
        {
            this.Netwrite(Basis.getBcontext().getString(R.string.prot_wbcontrol_cmd_atconnect));
        }

		else if (this.getDev_protocol() == WBprotokoll.WLAN_DL)
		{
			this.Netwrite("i19 1");         // sonst geht nichts
			//this.Netwrite("*");         // umschalten auf mehr Output
			this.Netwrite("i3");  			//TODO:  Test ausgabe
		}
		
	
	}


	// sende Befehl zum Konfigurieren eines GPIO-Pins (Pin Objekt muss schon konfiguriert sein)
	public void cmdSendGPIOConfig(GPIOpin pin)
	{
		if (pin.isUsed())
        {
            this.Netwrite(String.format(Basis.getBcontext().getString(R.string.prot_wbcontrol_cmd_gpioset), pin.getPort().getName(), pin.getNumber()));
        }
        else
        {
            this.Netwrite(String.format(Basis.getBcontext().getString(R.string.prot_wbcontrol_cmd_gpioclear), pin.getPort().getName(), pin.getNumber()));
        }
	}

    // sende Befehl zum LO/HI Setzten eines GPIO-Pins
    public void cmdSendGPIOValue(GPIOpin pin)
    {
        int value = 0;
        if (pin.isSet()) { value = 1; }
        this.Netwrite(String.format(Basis.getBcontext().getString(R.string.prot_wbcontrol_cmd_gpio), pin.getPort().getName(), pin.getNumber(), value));
    }



	// --------------- Device.WBevents ------------------------------------

    // --- interfaces----

    public interface OnStopListener {
        /**
         * Called when a device (engine) has (really) been stopped ( speed AND tainspeed = 0 for some time)
         *
         * @param d device
         */
        void onStop(Device d);
    }

    // set/remove listeners
    public void setOnStopListener(OnStopListener listener)
    {
        // Store the listener object
        this.stopListeners.add(listener);
    }

    public void removeStopListener(OnStopListener listener)
    {
        // Store the listener object
        this.stopListeners.remove(listener);
    }


    // event checks

    private void checkStopEvent()
    {
        // stop event checken
        if ((speed == 0) && (trainspeed == 0) && (lasttrainspeed[0] == 0))
        {
            if (!is_stop_event)
            {
                is_stop_event = true;
                fireOnStopListeners();
            }
        }
        else
        {
            is_stop_event = false;
        }
    }



    // ------  fire listener callbacks when it's time to --------

    private void fireOnStopListeners()
    {
        for (OnStopListener listener : stopListeners)
        {
            listener.onStop(this);
        }
        sendStdLocBroadcast(Basis.ACTION_DEVICE_EVENT_STOP);    // TODO: jetzt mal testweise im control anzeigen

    }



} // end class device
