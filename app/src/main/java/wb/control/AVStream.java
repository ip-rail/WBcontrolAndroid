package wb.control;

import java.util.Locale;

public class AVStream {

	//Stream-Typen
	public static final int STREAM_HTTP_MJPG 	= 	1;	// HTTP MJPG Stream (von MJPG-Streamer, IP-Cams) -> MjpegView
	public static final int STREAM_RTSP 		= 	2;	// RTSP Stream -> MediaPlayer
	
	public String name;
	public String source;
	public int type;
	
	public AVStream(String desc, String src)
	{
		name = desc;
		source = src;
		type = 0;	// undefiniert
		if (source.toLowerCase(Locale.GERMAN).startsWith("http://")) { type = STREAM_HTTP_MJPG; }
		if (source.toLowerCase(Locale.GERMAN).startsWith("rtsp://")) { type = STREAM_RTSP; }
	}
	
	public AVStream(String desc, String src, int typ)
	{
		name = desc;
		source = src;
		type = typ; 
	}
	
}
