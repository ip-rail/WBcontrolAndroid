package wb.control;

public class HardKey {
	
	// key-type
	public static final int HARDKEY_TYPE_KLICK			= 	0;	// Taste (Klick, nur 1x pro Tastendruck) 
	public static final int HARDKEY_TYPE_KLICK_REPEAT 	= 	1;	// Taste mit Mehrfachfunktion (Klick, wird mehrfach ausgeführt bei längerem Druck, "Dauerfeuer"))
	public static final int HARDKEY_TYPE_KLICK_LONG 	= 	2;	// Taste + Lang-Klick-Funktion (Klick, Spezialfunktion bei langem Druck)
	public static final int HARDKEY_TYPE_ON_OFF		 	= 	3;	// ON/Off Taste (unterschiedliche Funktionen für 1. und 2. Druck
	
	// key-function
	public static final int HARDKEY_FUNCTION_NO			= 0;	// keine Funktion
	public static final int HARDKEY_FUNCTION_START		= 1;	// start
	public static final int HARDKEY_FUNCTION_STOP		= 2;	// stop
	public static final int HARDKEY_FUNCTION_SPEED_ACC	= 3;	// Speed +
	public static final int HARDKEY_FUNCTION_SPEED_DEC	= 4;	// Speed -
	public static final int HARDKEY_FUNCTION_ALLPAUSE	= 5;	// alle Pause
	public static final int HARDKEY_FUNCTION_ALLSTOP	= 6;	// alle Stop
	public static final int HARDKEY_FUNCTION_CHDIR		= 7;	// Richtung umkehren
	public static final int HARDKEY_FUNCTION_MACRO		= 8;	// Macro 
	
	
	
	
		public String custom_name, macro1, macro2;
		public int keytype, function1, function2;
		
		public HardKey()
		{
			keytype = 0;	// Standard Funktion
			function1 = 0;	// 0: keine Funktion
			function2 = 0;
			custom_name = "";	//wird erst vom User vergeben (oder gar nicht)
			macro1 = "";
			macro2 = "";
		}
	

}
