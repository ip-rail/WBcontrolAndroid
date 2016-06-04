package wb.control.views;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGBuilder;

import wb.control.R;
import wb.control.TrackElement;
import wb.control.TrackPlan;
import wb.control.fragments.Frag_TrackPlanTest;

// Class für den Gleisbild-View "TrackView"
// Beispiel: http://www.droidnova.com/playing-with-graphics-in-android-part-ii,160.html
// checken: http://android-coding.blogspot.com/2011/05/drawing-on-surfaceview.html
// http://developer.android.com/resources/samples/LunarLander/src/com/example/android/lunarlander/LunarView.html


// drawPicture() wird von der HardwareAcceleration nicht unterstützt -> Exception wenn aktiviert.

public class TrackView extends SurfaceView implements SurfaceHolder.Callback {

    private TrackRenderThread track_thread;
    private Context mContext;    // Handle to the application context, used to e.g. fetch Drawables.

    public TrackView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public TrackView(Context context, AttributeSet attrs) {    // TrackView im xml-layout verlangt den Constructor mit attrs!!!
        super(context, attrs);
        mContext = context;
        init();
    }

    @SuppressLint("NewApi")
    private void init()    // generelle Initialisierungen
    {
        getHolder().addCallback(this);
        setFocusable(true);
        // track_thread = new TrackRenderThread(getHolder(), this);	//TODO: Test: erst im surfaceCreated() machen

        // Hardware Acceleration für diesen View deaktivieren (macht noch Probleme)
        //if (Basis.getApiLevel() >= 11) { setLayerType(View.LAYER_TYPE_SOFTWARE, null); } 

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return track_thread.doTouchEvent(event);    // wird an thread weitergegeben
    }


    public TrackRenderThread getThread() {
        return track_thread;
    }


    //Callback invoked when the surface dimensions change
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        track_thread.setSurfaceSize(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        track_thread = new TrackRenderThread(getHolder(), this);
        track_thread.setRunning(true);
        track_thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // simply copied from sample application LunarLander:
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        track_thread.setRunning(false);
        while (retry) {
            try {
                track_thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    public void loadPlan(String name) {
        track_thread.load(name);
    }

    public void savePlan() {
        track_thread.savePlan();
    }

    public void newPlan() {
        track_thread.newPlan();
    }

    public void deletePlan(String name) {
        track_thread.trackplan.delFromDB(name);
    }

    public String getPlanName() {
        return track_thread.getPlanName();
    }

    public void setEditTool(int index) {
        track_thread.setEditTool(index);
    }


    public class TrackRenderThread extends Thread {


        private final SurfaceHolder sHolder;
        private TrackView trackv;
        private boolean run_thread = false;
        private boolean surface_ok = false;
        private boolean plan_ready = false;
        private int cHeight = 5;    // Current height of the surface/canvas
        private int cWidth = 5;    // Current width of the surface/canvas

        private TrackPlan trackplan;    // neu: statt trackgrid benutzen -> noch umbauen!!!
        //private TrackElement trackgrid [] [];
        private TrackElement te_leer;    // das Standardelement für leere Flächen nur 1x anlegen
        private int tileX = 90;    // Standard-Breite eines Track-Elements
        private int tileY = 90;    // Standard-Höhe eines Track-Elements
        private int gridX = 6;    // Standard-Breite des *sichtbaren* TrackGrids (Anzahl Array-Elemente)
        private int gridY = 6;    // Standard-Höhe des *sichtbaren* TrackGrids (Anzahl Array-Elemente)
        //private int touchX = 0;	// X-Koordinate des aktuellen Touch
        //private int touchY = 0;	// Y-Koordinate des aktuellen Touch
        //private int last_touchX = 0;	// X-Koordinate des letzen Touch
        //private int last_touchY = 0;	// Y-Koordinate des letzen Touch
        //private int dX = 0;	// x offset des letzen Touch 
        //private int dY = 0;	// y offset des letzen Touch 

        //private Boolean touched = false;
        private boolean editmode = false;
        //private int drawmode = 1;	// alles neu zeichnen, 0: nix zu tun, 2: ein tile neu zeichnen
        //private int tiletodrawX = 0;
        //private int tiletodrawY = 0;

        //private Boolean moving = false;

        private Handler uihandler;

        private MoveTarget touchTarget;    // Datensatz für das Element, das getoucht (und eventuell verschoben) wird

        public static final int TRACKELEMENT_TYPE_EMPTY = 0;    // leeres Feld
        public static final int TRACKELEMENT_TYPE_LINE_H = 1;    // waagrechte Verbindung
        public static final int TRACKELEMENT_TYPE_LINE_V = 2;    // senkrechte Verbindung
        public static final int TRACKELEMENT_TYPE_LINE_vrlo = 3;    // von rechts (mitte) nach links oben
        public static final int TRACKELEMENT_TYPE_LINE_vrlu = 4;    // von rechts (mitte) nach links unten
        public static final int TRACKELEMENT_TYPE_LINE_vlro = 5;    // von links (mitte) nach rechts oben
        public static final int TRACKELEMENT_TYPE_LINE_vlru = 6;    // von links (mitte) nach rechts unten
        public static final int TRACKELEMENT_TYPE_LINE_lo = 7;    // schräge Verbindung links oben
        public static final int TRACKELEMENT_TYPE_LINE_ru = 8;    // schräge Verbindung rechts unten
        public static final int TRACKELEMENT_TYPE_LINE_ro = 9;    // schräge Verbindung rechts oben
        public static final int TRACKELEMENT_TYPE_LINE_lu = 10;    // schräge Verbindung links unten
        public static final int TRACKELEMENT_TYPE_SWITCH_ro = 11;    // Weiche von rechts nach oben
        public static final int TRACKELEMENT_TYPE_SWITCH_lu = 12;    // Weiche von links nach unten
        public static final int TRACKELEMENT_TYPE_SWITCH_ru = 13;    // Weiche von rechts nach unten
        public static final int TRACKELEMENT_TYPE_SWITCH_lo = 14;    // Weiche von links nach oben
        public static final int TRACKELEMENT_TYPE_SWITCH_mlo = 15;    // Weiche von mitte nach links oben
        public static final int TRACKELEMENT_TYPE_SWITCH_mru = 16;    // Weiche von mitte nach rechts unten
        public static final int TRACKELEMENT_TYPE_SWITCH_mro = 17;    // Weiche von mitte nach rechts oben
        public static final int TRACKELEMENT_TYPE_SWITCH_mlu = 18;    // Weiche von mitte nach links unten

        public static final int TRACKELEMENT_TYPE_END_L = 19;    // Ende links
        public static final int TRACKELEMENT_TYPE_END_R = 20;    // Ende rechts
        public static final int TRACKELEMENT_TYPE_END_U = 21;    // Ende unten
        public static final int TRACKELEMENT_TYPE_END_O = 22;    // Ende oben

        public static final int TRACKELEMENT_TEST_DROID = 23;    // zu Testzwecken
        public static final int TRACKELEMENT_COUNT = 24;    // Anzahl der Elemente


        private int[] tepic_IDs = new int[]{R.raw.te00, R.raw.te01, R.raw.te02, R.raw.te03,
                R.raw.te04, R.raw.te05, R.raw.te06, R.raw.te07, R.raw.te08, R.raw.te09,
                R.raw.te10, R.raw.te11, R.raw.te12, R.raw.te13, R.raw.te14, R.raw.te15,
                R.raw.te16, R.raw.te17, R.raw.te18, R.raw.te19, R.raw.te20, R.raw.te21, R.raw.te22, R.raw.android};


        private Paint gridPaint;    // "Farbe" für den Raster im Edit-Modus
        private Paint selectPaint;    // "Farbe" für Markier-Tests
        //private Bitmap scratch;
        //private Picture picfromsvg;
        //private Rect droidRect;
        private Picture te_pics[];
        //private Drawable te_drawable [];	// Test statt Picture
        private int actual_edit_tool;

        private Point testPoint;


        public TrackRenderThread(SurfaceHolder surfaceHolder, TrackView tv) {
            sHolder = surfaceHolder;
            trackv = tv;
            this.setName("TrackRenderThread");

            setMsgHandler(uihandler);

            te_leer = new TrackElement(TRACKELEMENT_TYPE_EMPTY);

            // Grid, grafische Ressourcen usw. vorbereiten

            //gridX = (int) cWidth / tileX;
            //gridY = (int) cHeight / tileY;
            //changeTrackGrid(gridX, gridY);	// Surfacegröße ist noch nicht bekannt -> daher später in setSurfaceSize aufrufen

            actual_edit_tool = 0;
            //Test
            testPoint = new Point();

            touchTarget = new MoveTarget();

            gridPaint = new Paint();
            gridPaint.setAntiAlias(true);
            gridPaint.setARGB(255, 180, 180, 180);

            selectPaint = new Paint();
            selectPaint.setAntiAlias(true);
            selectPaint.setARGB(128, 255, 0, 0);

            //scratch = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon);

            //SVG svg = SVGParser.getSVGFromResource(mContext.getResources(), R.raw.android);
            //picfromsvg = svg.getPicture();
            //droidRect = new Rect(0,0,200,200);

            te_pics = new Picture[TRACKELEMENT_COUNT];    // Picture-Array für die Trackelement-symbole erstellen

            //te_drawable = new Drawable[TRACKELEMENT_COUNT];	// Drawable-Array für die Trackelement-symbole erstellen	// Test

            for (int i = 0; i < TRACKELEMENT_COUNT; i++) {

                // te_pics[i] = (SVGParser.getSVGFromResource(mContext.getResources(), tepic_IDs[i])).getPicture(); // old svg-android library
                //te_drawable[i] = (SVGParser.getSVGFromResource(mContext.getResources(), tepic_IDs[i])).createPictureDrawable();	// Test

                /* new github japgolly/svg-android

                // Load and parse a SVG
                SVG svg = new SVGBuilder()
                        .readFromResource(getResources(), R.raw.someSvgResource) // if svg in res/raw
                        .readFromAsset(getAssets(), "somePicture.svg")           // if svg in assets
                                // .setWhiteMode(true) // draw fills in white, doesn't draw strokes
                                // .setColorSwap(0xFF008800, 0xFF33AAFF) // swap a single colour
                                // .setColorFilter(filter) // run through a colour filter
                                // .set[Stroke|Fill]ColorFilter(filter) // apply a colour filter to only the stroke or fill
                        .build();
                */

                SVG svg = new SVGBuilder()
                        .readFromResource(mContext.getResources(), tepic_IDs[i])
                        .build();

                te_pics[i] = svg.getPicture();

            }


        }

        public void setRunning(boolean run) {
            run_thread = run;
        }

        @Override
        public void run() {
            Canvas c;
            while (run_thread) {
                if (surface_ok && plan_ready) {
                    c = null;
                    try {
                        if (sHolder.getSurface().isValid()) {
                            c = sHolder.lockCanvas(null);
                            // AB Android 4 muss hier gecheckt werden, ob c null ist!!
                            if (c != null) {
                                synchronized (sHolder) {
                                    // ab hier kann gezeichnet werden
                                    if (trackplan != null) {
                                        updateAll(c);
                                    }
                                }    // zeichnen ende
                            }
                            //try { sleep(100); }
                            //catch (InterruptedException e) { e.printStackTrace(); }
                        }
                    } finally {
                        if (c != null) {
                            sHolder.unlockCanvasAndPost(c);
                        }
                    }
                }
            }
        }    // end run


        private void updateAll(Canvas canv) {
            canv.drawColor(Color.BLACK);    // zuerst alles löschen - schwarzer Hintergrund
            //canv.drawBitmap(scratch, (scratch.getWidth() / 2), (scratch.getHeight() / 2), null);
            drawTiles(canv);

            if (editmode) {
                drawGrid(canv);
            }

            if (touchTarget.moving)    // es wird gerade ein TrackElement verschoben
            {
                //touchTarget.moveRect.offset(touchTarget.dX, touchTarget.dX);
                touchTarget.dX = 0;    // nachdem die Verschiebung bei der Grafikausgabe berücksichtigt wurde, kann/müssen die Werte wieder auf 0 gesetzt werden
                touchTarget.dY = 0;
                touchTarget.moveRect.offsetTo(touchTarget.newMovingX, touchTarget.newMovingY);
                canv.drawPicture(te_pics[touchTarget.te.type], touchTarget.moveRect);

                //te_drawable[touchTarget.te.type].setBounds(touchTarget.moveRect);
                //te_drawable[touchTarget.te.type].draw(canv);

            }


        }

    	/*
    	private void drawDroid(Canvas canv)
    	{
    		if (moving) {
    			//droidRect.offset(dX, dY);
    			droidRect.offsetTo(touchX, touchY);
}
    		canv.drawPicture(picfromsvg, droidRect);
    		
    		picfromsvg.getWidth();
    		picfromsvg.getHeight();
    	} */

        // zeichnet das Raster für den Edit-Modus
        private void drawGrid(Canvas canv) {
            int xend = (gridX * tileX);
            int yend = (gridY * tileY);

            for (int x = 0; x <= gridX; x++) {
                canv.drawLine(x * tileX, 0, x * tileX, yend, gridPaint);
            }
            for (int y = 0; y <= gridY; y++) {
                canv.drawLine(0, y * tileY, xend, y * tileY, gridPaint);
            }
        }


        // zeichnet die Tiles
        private void drawTiles(Canvas canv) {
            Rect tilerect = new Rect(0, 0, tileX, tileY);

            for (int x = 0; x < gridX; x++) {
                for (int y = 0; y < gridY; y++) {
                    tilerect.offsetTo(tileX * x, tileY * y);
                    canv.drawPicture(te_pics[trackplan.trackgrid[x][y].type], tilerect);
                    //te_drawable[trackplan.trackgrid[x][y].type].setBounds(tilerect);
                    //te_drawable[trackplan.trackgrid[x][y].type].draw(canv);
                }
            }
        }


        // ein einziges Rasterelement zeichnen
        private void drawTile(Canvas canv, int x, int y)    // x,y: Array-Koordinaten
        {
            Rect target = getRectFromTile(x, y);
            canv.drawRect(target, selectPaint);
        }

        // ermittelt aus den Array-Koordinaten das zugehörige Rechteck im Canvas
        private Rect getRectFromTile(int x, int y) {
            int xleft, xright, ytop, ybottom;

            xleft = tileX * x;
            xright = xleft + tileX - 1;
            ytop = tileY * y;
            ybottom = ytop + tileY - 1;

            return new Rect(xleft, ytop, xright, ybottom);
        }

        // ermittelt aus den Touch-Koordinaten die zugehörigen ArrayKoordinaten
        private Point getTrackTile(int x, int y) {
            int trackx = 0;
            int tracky = 0;
            trackx = (int) (x / tileX);
            tracky = (int) (y / tileY);
            if (trackx >= gridX) {
                trackx = gridX - 1;
            }
            if (trackx < 0) {
                trackx = 0;
            }
            if (tracky >= gridY) {
                tracky = gridY - 1;
            }
            if (tracky < 0) {
                tracky = 0;
            }

            return new Point(trackx, tracky);
        }

        // ermittelt aus den Touch-Koordinaten das zugehörige Trackelement
        private TrackElement getTrackElement(int x, int y) {
            int trackx = 0;
            int tracky = 0;
            trackx = (int) (x / tileX);
            tracky = (int) (y / tileY);

            if (trackx >= gridX) {
                trackx = gridX - 1;
            }
            if (trackx < 0) {
                trackx = 0;
            }
            if (tracky >= gridY) {
                tracky = gridY - 1;
            }
            if (tracky < 0) {
                tracky = 0;
            }

            return trackplan.trackgrid[trackx][tracky];
        }

        // erstellt ein neues Array für trackgrid - nur neu anlegen! es werden keine alten Daten übernommen
        private void newTrackPlan(final int newx, final int newy) {
            if (trackplan == null) {
                trackplan = new TrackPlan(getResources().getString(R.string.trackview_new_trackplan), newx, newy);
            }

            trackplan.trackgrid = new TrackElement[newx][newy];
            //Arrays.fill(trackgrid, new TrackElement(TRACKELEMENT_TYPE_EMPTY));
            for (int x = 0; x < newx; x++) {
                for (int y = 0; y < newy; y++) {
                    trackplan.trackgrid[x][y] = te_leer;    // für das Leerelement immer dasselbe verwenden
                }
            }
        }

        // Edit-Mode ein/ausschalten
        public void setEditMode(Boolean edit) {

            synchronized (sHolder) {

                editmode = edit;
            }

        }

        // behandelt TouchEvents
        public Boolean doTouchEvent(MotionEvent event) {

            Boolean ergebnis = false;    // Rückgabewert: true: der Event wurde verwendet

            synchronized (sHolder) {

                int touchX = (int) event.getX();
                int touchY = (int) event.getY();
                if (touchX < 0) {
                    touchX = 0;
                }
                if (touchY < 0) {
                    touchY = 0;
                }
                int action = event.getAction();

                testPoint = getTrackTile(touchX, touchY);

                sendMsg("Pixel: x=" + touchX + " y=" + touchY + " Array: x=" + testPoint.x + " y=" + testPoint.y);

                switch (action) {

                    case MotionEvent.ACTION_DOWN:
                        //touched = true;
                        Log.d("wbcontrol", "Action_Down");
                        touchTarget.clear();
                        touchTarget.te = getTrackElement(touchX, touchY);
                        if (touchTarget.te != null) {
                            ergebnis = true;
                            touchTarget.touched = true;
                            touchTarget.origin_tileX = (int) (touchX / tileX);
                            touchTarget.origin_tileY = (int) (touchY / tileY);
                            touchTarget.lastMovingX = touchX;
                            touchTarget.lastMovingY = touchY;
                            touchTarget.moveRect.left = touchTarget.origin_tileX;
                            touchTarget.moveRect.top = touchTarget.origin_tileY;
                            touchTarget.moveRect.right = touchTarget.moveRect.left + tileX - 1;
                            touchTarget.moveRect.bottom = touchTarget.moveRect.top + tileY - 1;

                            if (editmode) {
                                if (touchTarget.te.type == TRACKELEMENT_TYPE_EMPTY) {
                                    //bevölkern, nicht moven
                                    int trackx = (int) (touchX / tileX);
                                    int tracky = (int) (touchY / tileY);
                                    if (trackx >= gridX) {
                                        trackx = gridX - 1;
                                    }
                                    if (trackx < 0) {
                                        trackx = 0;
                                    }
                                    if (tracky >= gridY) {
                                        tracky = gridY - 1;
                                    }
                                    if (tracky < 0) {
                                        tracky = 0;
                                    }
                                    trackplan.trackgrid[trackx][tracky] = new TrackElement(actual_edit_tool);
                                    touchTarget.clear();
                                }
                            }
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:

                        if ((touchTarget.te != null) && (touchTarget.touched) && (editmode)) {
                            ergebnis = true;
                            touchTarget.moving = true;
                            touchTarget.newMovingX = touchX;    // wird das überhaupt gebraucht?
                            touchTarget.newMovingY = touchY;
                            touchTarget.dX += touchX - touchTarget.lastMovingX;    // Veränderung aufaddieren und erst nach Bildschirmausgabe auf 0 setzen
                            touchTarget.dY += touchY - touchTarget.lastMovingX;
                        }

                        break;
                    case MotionEvent.ACTION_UP:

                        if ((touchTarget.te != null) && (touchTarget.touched) && (touchTarget.moving)) {
                            ergebnis = true;
                            int trackx = (int) (touchX / tileX);
                            int tracky = (int) (touchY / tileY);
                            if (trackx >= gridX) {
                                trackx = gridX - 1;
                            }
                            if (trackx < 0) {
                                trackx = 0;
                            }
                            if (tracky >= gridY) {
                                tracky = gridY - 1;
                            }
                            if (tracky < 0) {
                                tracky = 0;
                            }
                            trackplan.trackgrid[trackx][tracky] = touchTarget.te;    // Trackelement im Zieltile einfügen
                            if ((touchTarget.origin_tileX != trackx) || (touchTarget.origin_tileY != tracky)) {
                                trackplan.trackgrid[touchTarget.origin_tileX][touchTarget.origin_tileY] = te_leer;    // Ursprungstile leer machen, wenn wirklich verschoben
                            }

                            Log.d("wbcontrol", "Action_UP x=" + touchX + "y=" + touchY + " / Array x=" + trackx + "y=" + tracky);
                            Log.d("wbcontrol", "Action_UP: te-typ:" + trackplan.trackgrid[trackx][tracky].type);
                        }

                        touchTarget.clear();
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        ergebnis = true;
                        touchTarget.clear();
                        break;

                    case MotionEvent.ACTION_OUTSIDE:
                        ergebnis = true;
                        touchTarget.clear();
                        break;

                    default:
                }


                return ergebnis;
            }
        }


        // Callback invoked when the surface dimensions change.
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (sHolder) {
                cWidth = width;
                cHeight = height;
                gridX = (int) (cWidth / tileX);
                gridY = (int) (cHeight / tileY);
                surface_ok = true;    // erst jetzt darf gerendert werden (vorher waren die Dimensionen nicht bekannt)

                // don't forget to resize the background image
                //mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);
            }
        }

        private void populatePlanTemp() {
            // Test: ein paa TEs setzen
            trackplan.trackgrid[1][1] = new TrackElement(1);
            trackplan.trackgrid[1][2] = new TrackElement(2);
            trackplan.trackgrid[1][3] = new TrackElement(3);
            trackplan.trackgrid[1][4] = new TrackElement(4);
            trackplan.trackgrid[1][5] = new TrackElement(5);
            trackplan.trackgrid[2][1] = new TrackElement(6);
            trackplan.trackgrid[2][2] = new TrackElement(7);
            trackplan.trackgrid[2][3] = new TrackElement(8);
            trackplan.trackgrid[2][4] = new TrackElement(9);
            trackplan.trackgrid[2][5] = new TrackElement(10);
            trackplan.trackgrid[3][1] = new TrackElement(11);
            trackplan.trackgrid[3][2] = new TrackElement(12);
            trackplan.trackgrid[3][3] = new TrackElement(13);
            trackplan.trackgrid[3][4] = new TrackElement(14);
            trackplan.trackgrid[3][5] = new TrackElement(15);
            trackplan.trackgrid[4][1] = new TrackElement(16);
            trackplan.trackgrid[4][2] = new TrackElement(17);
            trackplan.trackgrid[4][3] = new TrackElement(18);
            trackplan.trackgrid[4][4] = new TrackElement(19);
            trackplan.trackgrid[4][5] = new TrackElement(20);
            trackplan.trackgrid[4][0] = new TrackElement(21);
            trackplan.trackgrid[3][0] = new TrackElement(22);
        }

        public void load(String name)    // Trackplan laden
        {
            plan_ready = false;
            if (trackplan == null) {
                trackplan = new TrackPlan(name);
            }
            trackplan.ReadFromDB(name);
            plan_ready = true;    // erst jetzt darf gerendert werden
        }

        public void savePlan()    // Trackplan in DB speichern
        {
            if (trackplan == null) {
                return;
            }
            if (trackplan.name.equals("")) {
                trackplan.name = "temp";
            }
            trackplan.WriteToDB();
        }

        public void newPlan() {
            plan_ready = false;
            newTrackPlan(15, 10);
            populatePlanTemp();
            plan_ready = true;
        }

        public String getPlanName() {
            String planname = "";
            if (trackplan != null) {
                planname = trackplan.name;
            }
            return planname;
        }

        public void setEditTool(int toolnr) {
            actual_edit_tool = toolnr;
        }


        // Handler für Statusmeldungen an Fragment
        public void setMsgHandler(Handler h) {
            uihandler = h;
        }


        // Statustext an Fragment zur Anzeige übermitteln
        public void sendMsg(String txt) {
            if (uihandler != null)
            {
                Message msg1 = Message.obtain(uihandler, Frag_TrackPlanTest.MSG_TXT);
                Bundle b = new Bundle();
                msg1.setData(b);
                b.putString("infotxt", txt);
                uihandler.sendMessage(msg1);
            }
        }


    }    // end class TrackRenderThread


    public class MoveTarget {    // Datensatz für das Verschieben eines Elements

        Boolean touched;    // wird true gesetzt, wenn ACTION_DOWN dafür empfangen wurde
        Boolean moving;        // wird true gesetzt, wenn ACTION_MOVE dafür empfangen wurde
        int origin_tileX;    // Ursprungskoordinaten im Array
        int origin_tileY;
        int lastMovingX;    // letzte Position im Canvas
        int lastMovingY;
        int newMovingX;        // neue Position im Canvas - die erst gezeichnet werden muss
        int newMovingY;
        int dX;                // Positionsänderung seit der letzten Bildschirmausgabe (muss nach ausgabe auf 0 gesetzt werden)
        int dY;
        TrackElement te;
        Rect moveRect;

        public MoveTarget() {
            moveRect = new Rect();
            clear();
        }

        public void clear()    // leeren
        {
            touched = false;
            moving = false;
            origin_tileX = 0;
            origin_tileY = 0;
            lastMovingX = 0;
            lastMovingY = 0;
            newMovingX = 0;
            newMovingY = 0;
            dX = 0;
            dY = 0;
            te = null;
            moveRect.top = 0;
            moveRect.bottom = 0;
            moveRect.left = 0;
            moveRect.right = 0;
        }
    }


}    // end class TrackView
