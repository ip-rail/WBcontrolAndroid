package wb.control;

import android.os.Bundle;

public interface OnFragReplaceListener {

	void OnFragReplace(int fragmentID, boolean toBackStack, Bundle data);
	
}

/*

 Callback für's Wechseln der Fragments
int fragmentID: ID, welches Fragment gestartet werden soll. Die IDs sind in FAct_control definiert.

boolean toBackStack: wenn true: "fragmentTransaction.addToBackStack(null);" wird ausgeführt
						man kann danach mit der BACK-Taste zum alten Fragment zurückspringen.
						Ist das nicht gewünscht, muss false übergeben werden, dannn wird kein
						.addToBackStack() ausgeführt.

Bundle data:	Im Bundle können Daten übergeben werden, wenn zB. ein startActivityForResult() ersetzt werden soll.
 				Das Bundle muss dann in der FragmentActivity ausgewertet werden.

NICHT MEHR BENÖTIGT:
int containerID: ID des containers, in dem das alte Fragment enthalten ist, das durch das neue (fragmentID) ersetzt werden soll.
					Die containerID kann im fragment folgendermaßen ermittelt werden:
					int containerID = ((ViewGroup)getView().getParent()).getId();
					oder einfach fcontainer.getId(), wenn bekannt

*/