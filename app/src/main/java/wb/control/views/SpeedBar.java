package wb.control.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class SpeedBar extends View {

	private int sliderValue;		// eingestellter Geschwindigkeitswert, entspricht Progress
	private int vergleichsValue;	// der Wert für die Anzeige der echten Geschwindigkeit
	private int max;

	private Color speedColor;       // Farbe Speedbalken
	private Color vergleichColor;   // Farbe Vergleichsbalken
	private Color backColor;       // Farbe Hintergrund

	private int MinWidth;
	private int MaxWidth;
	private int MinHeight;
	private int MaxHeight;

	private int vergleichsWidth;	// Breite des Vergleichs-Balkens

	// Konstruktoren
	
	public SpeedBar(Context context) {
		//super(context);
		this(context, null);
	}

	public SpeedBar(Context context, AttributeSet attrs) {
		//super(context, attrs);
		this(context, attrs, android.R.attr.progressBarStyle);
	}

	public SpeedBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		
	}
}
