package wb.control;

public interface OnHardkeyChangedListener {
	
	void OnHardkeyChanged(int keycode);

}

/*
Callback für Änderungen bei den Hardware-Tasten-Funktionen -> Daten für den keycode neu aus der DB laden (bei -1 alle neu laden)


*/