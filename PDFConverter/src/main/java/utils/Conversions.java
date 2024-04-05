package utils;


public class Conversions {
    public static final String TEMPERATURE = "Temperature";
    public static final String LENGTH = "Length";
    public static final String AREA = "Area";
    public static final String VOLUME_M3 = "VolumeM3";
    public static final String THERMAL_RESISTANCE = "ThermalResistance";
    public static final String ENERGY = "Energy";
    public static final String VOLUME_FLOW_RATE = "VolumeFlowRate";
    public static final String VOLUME_LITRE = "VolumeLitre";
    public static final String POWER = "Power";

    public static float convert(String mode, float value) {
        switch (mode) {
            case "Temperature" -> {
                return (value - 32f) / 1.8f;
            }
            case "Length" -> {
                return value * 0.3048f;
            }
            case "Area" -> {
                return value * 0.092903f;
            }
            case "VolumeM3" -> {
                return value * 0.0283168466f;
            }
            case "ThermalResistance" -> {
                return value / 5.67826f;
            }
            case "Energy" -> {
                return value / 0.000948f;
            }
            case "VolumeFlowRate" -> {
                return value * 0.47194745f;
            }
            case "VolumeLitre" -> {
                return value * 4.54609f;
            }
            case "Power" -> {
                return value / 3.412141633f;
            } default -> {
                return value;
            }
        }
    }

}
