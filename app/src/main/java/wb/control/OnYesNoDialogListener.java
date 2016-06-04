package wb.control;

public interface OnYesNoDialogListener {
	

	void OnYesNoDialog(int typ, Boolean antwort);


}

/*

antwort: 0: negativ, 1: positiv
typ: dialogtyp zur Unterscheidung

TODO: std YesNo dialog auch auf diesen Listener umstellen!!

*/