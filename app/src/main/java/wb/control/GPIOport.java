package wb.control;

import android.content.Intent;

import java.util.ArrayList;

/**
 * Created by Michael Brunnbauer on 17.05.2016.
 */

public class GPIOport {

    /*
    GPIOPort:
    Name (A/B.. bei ATMega)
    count (anzahl der Pins am Port - alle, nicht nur usable!)
    Type (ATMega-8bit / Raspi-P1-26 / Raspi-P1-40 [unterschiedliche Belegungen??])
    usable_mask
            used_mask
    Array Pin-Funktionsbezeichnungen (frei definierbar)

    */

    public static final int GPIO_TYPE_ATMEGA_8BIT    = 1;    // unterschiedliche Port-Typen
    public static final int GPIO_TYPE_STM32_16BIT    = 2;
    public static final int GPIO_TYPE_RASPI_P1_26    = 3;
    public static final int GPIO_TYPE_RASPI_P1_40    = 4;

    private String name;        // Port Bezeichnung zB "A" für ATMega Port A
    private int pincount;       // Anzahl der Pins am Port - alle, nicht nur usable!
    private int type;           // Hardware Typ des Ports (ATMega-8bit / Raspi-P1-26 / Raspi-P1-40 [unterschiedliche Belegungen??])
    private int usable_mask;    // Bitmaske, welche Pins überhaupt verwendbar sind (und nicht schon anderweitig in Verwendung sind)
    private int used_mask;      // Bitmaske, welche Pins bereits als User-GPIOs konfiguriert wurden.
    private ArrayList<GPIOpin> pinList;     // Liste der dem Port zugehörigen Pins
    private Device dev;         // Device zu dem der Port gehört

    public GPIOport(Device d, int port_type, String port_name, int usable, int used, int values)   // Konstruktor
    {
        name = port_name;
        type = port_type;
        usable_mask = usable;
        used_mask = used;
        dev = d;

        switch (type)
        {
            case GPIO_TYPE_ATMEGA_8BIT:
                pincount = 8;
                break;
            case GPIO_TYPE_STM32_16BIT:
                pincount = 16;
                break;
            case GPIO_TYPE_RASPI_P1_26:
                pincount = 0;   // TODO: wieviele GPIO-Pins?
                break;
            case GPIO_TYPE_RASPI_P1_40:
                pincount = 0;   // TODO: wieviele GPIO-Pins?
                break;

            default:
                pincount = 0;
                usable_mask = 0;
                used_mask = 0;
                break;
        }

        // generate Pins (GPIOpin)
        pinList = new ArrayList<GPIOpin>();

        for (int i=0;i<pincount;i++)
        {
            if (((1<<i) & usable) != 0)
            {
                boolean isused = false;
                boolean output_set = false;
                if  (((1<<i) & used) > 0) { isused = true; }
                if  (((1<<i) & values) > 0) { output_set = true; }
                pinList.add(new GPIOpin(i, this, isused, output_set));
            }
        }
    }


    // ------------------------ Getter/Setter ------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPincount() {
        return pincount;
    }

    public void setPincount(int pincount) {
        this.pincount = pincount;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getUsable_mask() {
        return usable_mask;
    }

    public void setUsable_mask(int usable_mask) {
        this.usable_mask = usable_mask;
    }

    public int getUsed_mask() {
        return used_mask;
    }

    public void setUsed_mask(int used_mask) {
        this.used_mask = used_mask;
    }

    public ArrayList<GPIOpin> getPinList() {  return pinList;  }

    public Device getDevice() { return dev;  }

    private GPIOpin getPin(int pinnumber)
    {
        for (GPIOpin p : pinList)
        {
            if (p.getNumber() == pinnumber) { return p;}
        }
        return null;
    }

    private int getPinValues()
    {
        int values = 0;
        for (GPIOpin pin : pinList) { if (pin.isSet()) { values += (1<<pin.getNumber()); } }
        return values;
    }

    // zum Updaten der Port und Pin Objekte wenn eine neuer PortStatus (per <gpioi:) vom MC gemeldet wird
    public void update(int usable, int used, int values)
    {
        // wenn verwendbare pins weg oder dazugekommen sind -> Liste anpassen
        // sonst nur used/values Werte anpassen

        if ((usable_mask != usable) || (used_mask != used) || (getPinValues() != values))
        {
            for (int i=0;i<pincount;i++)
            {
                if (((1<<i) & usable) != 0) // pin ist benutzbar
                {
                    boolean isused = false;
                    boolean output_set = false;
                    if  (((1<<i) & used) > 0) { isused = true; }
                    if  (((1<<i) & values) > 0) { output_set = true; }

                    GPIOpin p = getPin(i);
                    if (p == null) { pinList.add(new GPIOpin(i, this, isused, output_set)); }   // Pin anlegen, falls er nicht existiert // TODO: Pins sortieren!!!
                    else
                    {
                        p.setUsed(isused);
                        p.setValue(output_set); // natürlich ohne Ausgabe
                    }
                }
                else    // pin darf nicht verwendet werden -> aus der Liste entfernen
                {
                    GPIOpin p = getPin(i);
                    if (p != null) { pinList.remove(p); }
                }
            }
            Basis.sendLocalBroadcast(Basis.ACTION_UPDATE_GPIO, "device", dev.getName());    // info an Frag_lokgpiocfg, dass sich die Port/Pin-Daten geändert haben
        }



    }

//------------------------------------------------------------------------------------------------------------------------------------



}


