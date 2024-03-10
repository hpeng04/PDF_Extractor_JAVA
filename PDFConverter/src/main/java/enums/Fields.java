package enums;

public enum Fields {
    FILE("File", "File:", new int[]{0}),
    DATE_OF_ENTRY("Date of entry", "Date of entry:", new int[]{0}),
    COMPANY("Company", "Company:", new int[]{0}),
    FRONT_ORIENTATION("Front orientation", "Front orientation:", new int[]{0}),
    YEAR_HOUSE_BUILT("Year House Built", "Year House Built:", new int[]{0}),
    ADULTS(null, "Adults", new int[]{0, 1}),
    CHILDREN(null, "Children", new int[]{0, 1}),
    INFANTS(null, "Infants", new int[]{0, 1}),
    DAYTIME_SETPOINT("Daytime Setpoint", "Daytime Setpoint:", new int[]{1, 2}),
    NIGHTIME_SETPOINT("Nightime Setpoint", "Nightime Setpoint:", new int[]{1, 2}),
    DAYTIME_SETBACK_DURATION("Nightime Setback Duration", "Nightime Setback", new int[]{1, 2}),
    BASEMENT_SETPOINT("Basement Setpoint", "Basement Setpoint", new int[]{1, 2}),
    TEMP_RISE_FROM("TEMP. Rise from", "TEMP. Rise from", new int[]{1, 2}),
    ABOVE_GRADE_FRACTION("Above grade fraction of wall area occupied by windows", "Above grade by windows: ", new int[]{2, 3, 4}),
    INTERIOR_WALL("Interior wall type", "Interior wall", new int[]{4, 5, 6}),
    EXTERIOR_WALL("Exterior wall type", "Exterior wall type", new int[]{4, 5, 6}),
    ADDED_TO_SLAB("Added to slab type R-Value", "Added to slab", new int[]{4, 5, 6}),
    FLOORS_ABOVE_FOUND("Floors Above Found. R-Value", "Floors Above", new int[]{4, 5, 6}),
    AIR_LEAKAGE_TEST_RESULT("Air Leakage Test Results at 50 Pa.", "Test Results", new int[]{8, 9}),
    SYSTEM_TYPE("System Type:", "System Type:", new int[]{9, 10}),
    FP_POWER_0("Fan and Preheater Power at 0.0 °C", "Fan and Preheater Power", new int[]{9, 10}),
    FP_POWER_MINUS_25("Fan and Preheater Power at -25.0 °C", "Fan and Preheater Power", new int[]{9, 10}),
    HEAT_RE_EFFICIENCY_0("Sensible Heat Recovery Efficiency at 0.0 °C", "Sensible Heat Recovery Efficiency", new int[]{9, 10}),
    HEAT_RE_EFFICIENCY_MINUS_25("Sensible Heat Recovery Efficiency at -25.0 °C", "Sensible Heat Recovery Efficiency", new int[]{9, 10}),
    AIR_DC_TYPE("Air Distribution/circulation type", "Distribution/circulation Distributionlcirculation type:", new int[]{9, 10, 11}),
    AIR_DC_FAN_POWER("Air Distribution/circulation fan power", "Distribution/circulation Distributionlcirculation fan power:", new int[]{9, 10, 11}),
    OPERATION_SCHEDULE("Operation schedule", "Operation schedule:", new int[]{9, 10, 11}),
    GROSS_AIR_LEAKAGE("Gross Air Leakage and Mechanical Ventilation Energy Load", "Gross Air Leakage and Mechanical", new int[]{9, 10, 11, 12}),
    SEASONAL_HEAT_RECOVERY("Seasonal Heat Recovery Ventilator Efficiency", "Seasonal Heat Recovery Ventilator", new int[]{9, 10, 11, 12}),
    EST_VENT_LOAD_HEATING("Estimated Ventilation Electrical Load Heating Hours", "Estimated Ventilation Electrical Load", new int[]{9, 10, 11, 12}),
    EST_VENT_LOAD_NON_HEATING("Estimated Ventilation Electrical Load Non-Heating Hours", "Estimated Ventilation Electrical Load", new int[]{9, 10, 11, 12}),
    NET_AIR_LEAKAGE("Net Air Leakage and Mechanical Ventilation Load", "Net Air Leakage and Mechanical", new int[]{9, 10, 11, 12});


    private final String title;
    private final String keyword;
    private final int[] pages;

    Fields(String title, String keyword, int[] pages) {
        this.title = title;
        this.keyword = keyword;
        this.pages = pages;
    }

    public String getTitle() {
        return title;
    }

    public String getKeyword() {
        return keyword;
    }

    public int[] getPages() {
        return pages;
    }
}
