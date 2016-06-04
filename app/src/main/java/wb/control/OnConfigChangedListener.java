package wb.control;

import android.os.Bundle;

public interface OnConfigChangedListener {
	
	void OnConfigChanged(int configID, Bundle data);

}

/*
Callback für's Ändern der Anzeige, Thema usw. - Dinge, die in der Activity geändert werden müssen


*/