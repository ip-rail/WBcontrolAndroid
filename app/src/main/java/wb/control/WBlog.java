package wb.control;

public class WBlog {	// Logeintrag

	private String text;	// der eigentliche Meldungstext
	private String tag;		// kann zum Gruppieren verwendet werden (zB Quelle der Meldung)
	private long time;	// (in millisekunden von currentTimeMillis())
	private wblogtype type;		// Eintragsart
	
	
	public void setText(String text) {
		this.text = text;
	}
	public String getText() {
		return text;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getTag() {
		return tag;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public long getTime() {
		return time;
	}
	public void setType(wblogtype type) {
		this.type = type;
	}
	public wblogtype getType() {
		return type;
	}
	
	// Konstruktor
	
	public WBlog(String logtext, String logtag, wblogtype logtype)
	{
		text = logtext;
    	tag = logtag;
    	time = System.currentTimeMillis();
    	type = logtype;
	}
	
	public enum wblogtype
{
	Error,
	Warning,
	Info
}
	
	
}

