package wb.control;

/**
 * Created by Michael Brunnbauer on 27.06.2015.
 */
public class wbADCdata {
    public int index;       // ADC index - Nummer des gemeldeten ADC-Wertes (0 für ADC0...)
    public int value;       // ADC Wert 0 bis 1023. FFFFh bedeutet nicht gemessen
    public float voltage_max; // Spannung in Volt, die dem Maximalwert von 1023 entspricht. 0 bedeutet: noch nicht festgelegt - statt Volt wird dann das ADC value ausgegeben
    public String name;

    public wbADCdata (int i, int val)
    {
        index = i;
        value = val;
        voltage_max = 0;
        name = "";
    }

    public wbADCdata (int i, int val, float vmax)
    {
        index = i;
        value = val;
        voltage_max = vmax;
        name = "";
    }


    String getVoltageString()
    {
        String voltagestr = "";
        float voltage = 0;


        if (value == 0xFFFF) { voltagestr = "---"; } // ungültiger Wert
        else
        {
            if (voltage_max == 0)
            {
                voltagestr = Integer.toString(value) + "/1024";
            }
            else
            {
                voltage =  (float)value * 1023F / voltage_max;
                if (voltage > 0.1F) { voltagestr = Float.toString(voltage) + "V"; }
                else
                {
                    voltage = voltage / 1000F;
                    voltagestr = Float.toString(voltage) + "mV";
                }
            }
        }

        return voltagestr;
    }

}
