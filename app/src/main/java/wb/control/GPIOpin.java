package wb.control;

import java.util.ArrayList;

/**
 * Created by Michael Brunnbauer on 18.05.2016.
 */

public class GPIOpin {

    private int number;         // Pin Nummer (bezogen auf den Port zu dem der Pin gehört)
    private String name;        // Pin Name (vom User vergeben)
    private GPIOport port;      // Port zu dem der Pin gehört
    private boolean used;       // wird der Pin vom User benutzt (=true) oder ist er frei (=false)
    private boolean value;      // Status den Pins: ein- (true) oder ausgeschalten (false)

    public GPIOpin(int pin_number, GPIOport portofpin, boolean pin_is_used, boolean output_val)   // Konstruktor
    {
        number = pin_number;
        name = "";
        port = portofpin;
        used = pin_is_used;
        value = output_val;
    }

    public GPIOpin(int pin_number, String pin_name, GPIOport portofpin, boolean pin_is_used, boolean output_val)   // Konstruktor
    {
        number = pin_number;
        name = pin_name;
        port = portofpin;
        used = pin_is_used;
        value = output_val;
    }


    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GPIOport getPort() {
        return port;
    }

    public void setPort(GPIOport port) {
        this.port = port;
    }

    public boolean isUsed() { return used; }

    public void setUsed(boolean used)
    {
        boolean used_old = this.used;
        this.used = used;
        Device d = port.getDevice();
        if ((used_old != used) && d.isConnected()) { d.cmdSendGPIOConfig(this); }
    }

    //public boolean getValue() { return value;  } // TODO: entfernen -> isSet() ist besser

    public boolean isSet() { return value;  }

    public void setValue(boolean value) {  this.value = value;  }   // nur Wert setzen ohne echten Output

    // Wert am Pin ausgeben
    public void write(boolean output)   // Pin-Ausgabe am MC
    {
        boolean output_old = this.value;
        this.value = output;
        Device d = port.getDevice();

        if (used && (output_old != output) && d.isConnected()) { d.cmdSendGPIOValue(this); }
    }
}


