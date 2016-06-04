package wb.control;

public interface OnDeviceChangeListener {

    void OnDeviceChange(String devname, int changetype);

    //Callback für Fragments, wenn sich ein Device geändert hat

/*
 * devname: Devicename oder "", wenn es gelöscht wurde
 * changetype: welche aktionen sollen ausgeführt werden
 * 				0: loklist aktualisieren
 * 				1: lokdetails aktualisieren
 */


}
