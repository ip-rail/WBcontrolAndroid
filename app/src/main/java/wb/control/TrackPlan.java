package wb.control;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import wb.control.WBlog.wblogtype;
import wb.control.views.TrackView.TrackRenderThread;

public class TrackPlan {

    	public String name;		// freiwählbarer Name des Gleisplans
    	public int sizew;			// Breite des TrackPlans in Elementen
    	public int sizeh;			// Breite des TrackPlans in Elementen
    	public TrackElement trackgrid [] [];
    	
    	private TrackElement te_leer;	// das Standardelement für leere Flächen nur 1x anlegen (gibt's derzeit auch in der Thread-Class)
    	
    	public TrackPlan(String name)	// Constructor
    	{
    		this.name = name;
    		te_leer = new TrackElement(TrackRenderThread.TRACKELEMENT_TYPE_EMPTY);
    	}
    	
    	public TrackPlan(String name, int width, int height)	// Constructor
    	{
    		this.name = name;
    		sizew = width;
    		sizeh = height;
    		// trackgrid = new TrackElement [width] [height];
    		te_leer = new TrackElement(TrackRenderThread.TRACKELEMENT_TYPE_EMPTY);
    		newGrid(width, height);
    	}
    	
    	
    	// erstellt/ändert ein Array für trackgrid
        void newGrid(final int newx, final int newy)
        {
        	trackgrid = new TrackElement [newx] [newy];
        	clearGrid(newx, newy);
        	sizew = newx;
        	sizeh = newy;
        }
        
        void clearGrid(int countx, int county)
        {
        	for (int x = 0; x < countx; x++)
        	{
        		for (int y = 0; y < county; y++)
        		{
        			trackgrid[x][y] = te_leer;	// für das Leerelement immer dasselbe Objekt verwenden
        		}
        	}
        }
        
        public String gridToString()    // für DB als String ausgeben ("typ1,name1;typ2,name2;..)
        {
        	String ergebnis = "";
        	for (int y = 0; y < sizeh; y++)
        	{
        		for (int x = 0; x < sizew; x++)
        		{
        			ergebnis += trackgrid[x][y].toString();	// TODO ändern auf Stringbuffer oä statt +=
        		}
        	}
			return ergebnis;
        }
        
        public void importGridData(String data)	// zum Importieren der Daten aus der DB
        {
        	// Array ist noch nicht dimensioniert
        	trackgrid = new TrackElement [sizew] [sizeh];
        	String elements[] = data.split(";");
        	
        	int x=0;
        	int y=0;
        	int error = 0;
        	
            for(String element: elements)
        	{
        		String dat[] = element.split(",");
         		if (dat.length == 2)
        		{
        			int typ = Integer.parseInt(dat[0]);
        			if (dat[1].equals(" ")) { dat[1] = ""; } // "" wurde für's Speichern on " " umgewandelt -> jetzt wieder rückgängig machen
        			trackgrid[x][y] = new TrackElement(typ, dat[1]);
        		}
        		else  { error++; }
        		
        		x++;
        		if (x == sizew) 
        		{
        			x = 0;
        			y++;
        		}
        		
        	}



        	
        	if (error > 0)
        	{
        		Basis.AddLogLine("Fehler beim Importieren von TrackPlan mit Namen " + name, "TrackPlan", wblogtype.Error);
        	}
        }
        
        public void WriteToDB()	// einen Trackplan in der DB speichern
    	{
        	SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
    		String where = String.format("name='%s'", this.name);
    		Cursor c = wbDB.query(Basis.WB_DB_TABLE_TRACKPLANS, new String[] {"name"}, where, null, null, null, null);
    		
    		ContentValues dbValues = new ContentValues(); 
    		dbValues.put("sizew", this.sizew);
    		dbValues.put("sizeh", this.sizeh);
    		dbValues.put("data", this.gridToString());
    		
    		if (c.getCount() < 1)	// wenn der Eintrag nicht vorhanden ist -> anlegen
    		{			
    			dbValues.put("name", this.name);
    			wbDB.insert(Basis.WB_DB_TABLE_TRACKPLANS, null, dbValues);
    		}
    		else	// sonst nur updaten
    		{
    			wbDB.update(Basis.WB_DB_TABLE_TRACKPLANS, dbValues, where, null);
    		}
    		if (c != null && !c.isClosed()) { c.close(); }
    		wbDB.close();
    	}
    	
    	
    	public void ReadFromDB(String name)	// einen Trackplan aus der DB lesen
    	{
    		this.name = name;
    		int width = 0;
    		int height = 0;
    		String data = "";
    		SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
    		String where = String.format("name='%s'", name);
    		Cursor c = wbDB.query(Basis.WB_DB_TABLE_TRACKPLANS, new String[] {"sizew,sizeh,data"}, where, null, null, null, null);
    		
    		int count = c.getCount();
    		if (count == 1)
    		{
    			if (c.moveToFirst()) 
    			{ 
    				width = c.getInt(0);
        			height = c.getInt(1);
        			data = c.getString(2);
        			this.sizew = width;
        			this.sizeh = height;
        			importGridData(data);
    			}
    		}
    		else
    		{
    			if (count == 0)
    			{
    				Basis.AddLogLine("Fehler beim Landen: TrackPlan mit Namen " + name + " nicht gefunden", "Basis", wblogtype.Error);
    			}
    			else if (count >1)
    			{
    				Basis.AddLogLine("Fehler beim Landen: TrackPlan mit Namen " + name + ": " + count + "Einträge gefunden (es sollte nur 1 vorhanden sein)!", "Basis", wblogtype.Error);
    			}
    			
    		}
    		
    		if (c != null && !c.isClosed()) { c.close(); }
    		wbDB.close();
    		
    	}
    
	public void delFromDB(String name)
	{
		SQLiteDatabase wbDB = SQLiteDatabase.openOrCreateDatabase(Basis.getwbDBpath(), null);
		String where = String.format("name='%s'", name);
		wbDB.delete(Basis.WB_DB_TABLE_TRACKPLANS, where, null);
		wbDB.close();
	}
}
