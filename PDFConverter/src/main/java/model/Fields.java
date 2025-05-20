package model;

/**
 * Enum defining various fields that can be extracted from a PDF document, typically related to building energy analysis.
 * Each enum constant represents a specific piece of data and holds metadata about it:
 * - {@code parent}: A string indicating the parent category or section this field belongs to in a hierarchical structure.
 * - {@code title}: A user-friendly title or label for this field, often used for display purposes (e.g., in an Excel sheet header).
 * - {@code keyword}: The specific keyword or phrase to search for in the PDF text to identify this field's value.
 * - {@code fieldName}: A programmatic name for the field, potentially used as a variable name or map key.
 * - {@code requiresUnitConversion}: A boolean flag indicating if the extracted value for this field needs unit conversion.
 */
public enum Fields {
    // General Information
    FILE(" ", "File", "File:", "file", false), // Represents the source file name
    DATE_OF_ENTRY(" ", "Date of entry", "Date of entry:", "dataOfEntry", false), // Date the data was entered or recorded
    COMPANY(" ", "Company", "Company:", "company", false), // Company associated with the report

    // GENERAL HOUSE CHARACTERISTICS
    FRONT_ORIENTATION("GENERAL HOUSE CHARACTERISTICS", "Front orientation", "Front orientation:", "frontOrientation", false),
    YEAR_HOUSE_BUILT("GENERAL HOUSE CHARACTERISTICS", "Year House Built", "Year House Built:", "yearHouseBuilt", false),
    OCCUPANTS("GENERAL HOUSE CHARACTERISTICS", "Occupants", "Occupants", "occupants", false), // Could be a category for Adults, Children, Infants
    ADULTS("GENERAL HOUSE CHARACTERISTICS", "Adults", "Adults", "occupants", false), // Number of adults
    CHILDREN("GENERAL HOUSE CHARACTERISTICS", "Children", "Children", "occupants", false), // Number of children
    INFANTS("GENERAL HOUSE CHARACTERISTICS", "Infants", "Infants", "occupants", false), // Number of infants

    // HOUSE TEMPERATURES (Setpoint often refers to thermostat settings)
    DAYTIME_SETPOINT("HOUSE TEMPERATURES", "Daytime Setpoint (\u00B0C)", "Daytime Setpoint:", "daytimeSetpoint", true),
    NIGHTIME_SETPOINT("HOUSE TEMPERATURES", "Nightime Setpoint (\u00B0C)", "Nightime Setpoint:", "nightimeSetpoint", true),
    NIGHTIME_SETBACK_DURATION("HOUSE TEMPERATURES", "Nightime Setback Duration", "Nightime Setback", "nightimeSetbackDuration", true), // Duration for which nighttime temperature is set back
    BASEMENT_SETPOINT("HOUSE TEMPERATURES", "Basement Setpoint (\u00B0C)", "Basement Setpoint", "basementSetpoint", true),
    TEMP_RISE_FROM("HOUSE TEMPERATURES", "TEMP. Rise (\u00B0C)", "TEMP. Rise from", "tempRiseFrom", true), // Temperature rise, possibly for heating systems

    // WINDOW CHARACTERISTICS
    WINDOW_CHARACTERISTICS("WINDOW CHARACTERISTICS", "WINDOW CHARACTERISTICS","SHGC ER", "windowCharacteristics", true), // SHGC: Solar Heat Gain Coefficient, ER: Energy Rating
    ABOVE_GRADE_FRACTION("Above grade fraction of", "wall area occupied by windows",
            "Above grade fraction of wall area occupied by windows:", "aboveGradeFraction", false), // Percentage of wall area that is windows

    // BUILDING PARAMETER DETAILS (Component-specific thermal properties, often R-values or RSI)
    CEILING_COMPONENTS("Building Parameter Details - Ceiling Components", "Ceiling",
            "CEILING COMPONENTS", "ceilingComponents", true),
    MAIN_WALL_COMPONENTS("Building Parameter Details - Main Wall Components", "Main Wall",
            "MAIN WALL COMPONENTS", "mainWallComponents", true),
    EXPOSED_FLOORS("Building Parameter Details - Exposed Floors", "Exposed Floors",
            "EXPOSED FLOORS", "exposedFloors", true),
    DOORS("Building Parameter Details - Doors", "Doors", "DOORS", "doors", true),
    INTERIOR_WALL("Foundations", "Interior wall type", "Interior wall type:", "interiorWall", true), // Foundation interior wall details
    EXTERIOR_WALL("Foundations", "Exterior wall type", "Exterior wall type:", "exteriorWall", true), // Foundation exterior wall details
    ADDED_TO_SLAB("Foundations", "Added to slab type R-Value RSI", "Added to slab", "addedToSlab", true), // Insulation added to slab
    FLOORS_ABOVE_FOUND("Foundations", "Floors Above Found. R-Value RSI", "Floors Above", "floorsAboveFound", true), // Insulation for floors above foundation

    // BUILDING ASSEMBLY DETAILS (Overall assembly properties)
    BUILDING_ASSEMBLY_DETAILS("Building Assembly Details", "Building Assembly Details",
            "BUILDING ASSEMBLY DETAILS", "buildingAssemblyDetails", true),

    // BUILDING PARAMETERS SUMMARY (Summarized values for different zones)
    BUILDING_PARAMETERS_ZONE_1("Building Parameters Summary - Zone 1: Above grade", "Zone 1: Above Grade",
            "ZONE 1: Above Grade", "buildingParametersZone1", true),
    BUILDING_PARAMETERS_ZONE_2("Building Parameters Summary - Zone 2: Basement", "ZONE 2 : Basement",
            "ZONE 2 : Basement", "buildingParametersZone2", true),

    // AIR LEAKAGE AND MECHANICAL VENTILATION
    AIR_LEAKAGE_MECHANICAL_VENTILATION("Air Leakage and Mechanical Ventilation", "Air Leakage and Mechanical Ventilation",
            "Air Leakage and Mechanical Ventilation", "airLeakageMechanicalVentilation", true), // Section header
    AIR_LEAKAGE_TEST_RESULT("Air Leakage and Mechanical Ventilation", "Air Leakage Test Results at 50 Pa.",
            "Air Leakage Test Results at 50 Pa.", // Result of a blower door test
            "airLeakageTestResult", false),

    // F326 VENTILATION REQUIREMENTS (Specific ventilation standard)
    VENTILATION_REQUIREMENTS("F326 Ventilation Requirements", "F326 VENTILATION REQUIREMENTS", "F326 VENTILATION REQUIREMENTS",
            "ventilationRequirements", true), // Section header

    // CENTRAL VENTILATION SYSTEM (Details of the main ventilation unit, e.g., HRV/ERV)
    SYSTEM_TYPE("Central Ventilation System", "System Type", "System Type:", "systemType", false),
    FP_POWER_0("Central Ventilation System", "Fan and Preheater Power at 0.0 °C", "Fan and Preheater Power at 0", "fpPower0", false), // Power consumption at 0°C
    FP_POWER_MINUS_25("Central Ventilation System", "Fan and Preheater Power at -25.0 °C", "Fan and Preheater Power at -25", "fpPowerMinus25", false), // Power consumption at -25°C
    HEAT_RE_EFFICIENCY_0("Central Ventilation System", "Sensible Heat Recovery Efficiency at 0.0 °C",
            "Sensible Heat Recovery Efficiency at 0", "sensibleHeatRecoveryEfficiency0", false), // Efficiency at 0°C
    HEAT_RE_EFFICIENCY_MINUS_25("Central Ventilation System", "Sensible Heat Recovery Efficiency at -25.0 °C",
            "Sensible Heat Recovery Efficiency at -", "sensibleHeatRecoveryEfficiencyMinus25", false), // Efficiency at -25°C

    // NEW ERS VENTILATION DATA (ERS: Energy Recovery System)
    AIR_DC_TYPE("NEW ERS VENTILATION DATA", "Air Distribution/circulation type", "Air Distribution/circulation type:", "airDcType", false),
    AIR_DC_FAN_POWER("NEW ERS VENTILATION DATA", "Air Distribution/circulation fan power", "Air Distribution/circulation fan power:", "airDcFanPower", false),
    OPERATION_SCHEDULE("NEW ERS VENTILATION DATA", "Operation schedule", "Operation schedule:", "operationSchedule", false),

    // AIR LEAKAGE AND MECHANICAL VENTILATION SUMMARY (Calculated energy loads)
    GROSS_AIR_LEAKAGE("AIR LEAKAGE AND MECHANICAL VENTILATION SUMMARY", "Gross Air Leakage and Mechanical Ventilation Energy Load (MJ)",
            "Gross Air Leakage and Mechanical", "grossAirLeakage", true),
    SEASONAL_HEAT_RECOVERY("AIR LEAKAGE AND MECHANICAL VENTILATION SUMMARY", "Seasonal Heat Recovery Ventilator Efficiency",
            "Seasonal Heat Recovery Ventilator", "seasonalHeatRecovery", false),
    EST_VENT_LOAD_HEATING("AIR LEAKAGE AND MECHANICAL VENTILATION SUMMARY", "Estimated Ventilation Electrical Load Heating Hours (MJ)",
            "Estimated Ventilation Electrical Load", "estVentLoadHeating", true),
    EST_VENT_LOAD_NON_HEATING("AIR LEAKAGE AND MECHANICAL VENTILATION SUMMARY", "Estimated Ventilation Electrical Load Non-Heating Hours (MJ)",
            "Estimated Ventilation Electrical Load", "estVentLoadNonHeating", true),
    NET_AIR_LEAKAGE("AIR LEAKAGE AND MECHANICAL VENTILATION SUMMARY", "Net Air Leakage and Mechanical Ventilation Load (MJ)",
            "Net Air Leakage and Mechanical", "netAirLeakage", true),

    // SPACE HEATING SYSTEM
    PRIMARY_HEATING_FUEL("SPACE HEATING SYSTEM", "PRIMARY Heating Fuel", "PRIMARY Heating Fuel:",
            "primaryHeatingFuel", false),
    AFUE("SPACE HEATING SYSTEM", "AFUE", "AFUE:", "afue", false), // Annual Fuel Utilization Efficiency
    HIGH_SPEED_FAN_POWER("SPACE HEATING SYSTEM", "High Speed Fan Power", "High Speed Fan Power:",
            "highSpeedFanPower", false), // Fan power for furnace/air handler

    // DOMESTIC WATER HEATING SYSTEM (DHW)
    PRIMARY_WATER_HEATING_FUEL("DOMESTIC WATER HEATING SYSTEM", "PRIMARY Water Heating Fuel", "PRIMARY Water Heating",
            "primaryWaterHeatingFuel", false),
    WATER_HEATING_EQUIPMENT("DOMESTIC WATER HEATING SYSTEM", "Water Heating Equipment", "Water Heating",
            "waterHeatingEquipment", false), // Type of water heater
    ENERGY_FACTOR("DOMESTIC WATER HEATING SYSTEM", "Energy Factor", "Energy Factor:",
            "energyFactor", false), // Efficiency metric for water heaters

    // ANNUAL DOMESTIC WATER HEATING SUMMARY
    DAILY_HOT_WATER_CONSUMPTION("ANNUAL DOMESTIC WATER HEATING SUMMARY", "Daily Hot Water Consumption (Litres)",
            "Daily Hot Water Consumption:", "dailyHotWaterConsumption", true),
    HOT_WATER_TEMPERATURE("ANNUAL DOMESTIC WATER HEATING SUMMARY", "Hot Water Temperature (\u00B0C)",
            "Hot Water Temperature:", "hotWaterTemperature", true),
    ESTIMATED_DOMESTIC_WATER_HEATING_LOAD("ANNUAL DOMESTIC WATER HEATING SUMMARY", "Estimated Domestic Water Heating Load (MJ)",
            "Estimated Domestic Water Heating Load:", "estimatedDomesticWaterHeatingLoad", true),
    PRIMARY_DOMESTIC_WATER_HEATING_LOAD_CONSUMPTION("ANNUAL DOMESTIC WATER HEATING SUMMARY", "Primary Domestic Water Heating Energy Consumption (MJ)",
            "Primary Domestic Water Heating Energy", "primaryDomesticWaterHeatingLoadConsumption", true),
    PRIMARY_SYSTEM_SEASONAL_EFFICIENCY("ANNUAL DOMESTIC WATER HEATING SUMMARY", "Primary System Seasonal Efficiency",
            "Primary System Seasonal Efficiency:", "primarySystemSeasonalEfficiency", false),

    // ANNUAL SPACE HEATING SUMMARY
    GROSS_SPACE_HEAT_LOSS("ANNUAL SPACE HEATING SUMMARY", "Gross Space Heat Loss (MJ)", "Gross Space Heat Loss:",
            "grossSpaceHeatLoss", true),
    GROSS_SPACE_HEATING_LOAD("ANNUAL SPACE HEATING SUMMARY", "Gross Space Heating Load (MJ)", "Gross Space Heating Load:", "grossSpaceHeatingLoad", true),
    USABLE_INTERNAL_GAINS("ANNUAL SPACE HEATING SUMMARY", "Usable Internal Gains (MJ)", "Usable Internal Gains:",
            "usableInternalGains", true), // Heat gains from occupants, appliances, etc.
    USABLE_INTERNAL_GAINS_FRACTION("ANNUAL SPACE HEATING SUMMARY", "Usable Internal Gains Fraction", "Usable Internal Gains Fraction:",
            "usableInternalGainsFraction", false),
    USABLE_SOLAR_GAINS("ANNUAL SPACE HEATING SUMMARY", "Usable Solar Gains (MJ)", "Usable Solar Gains:",
            "usableSolarGains", true), // Heat gains from sunlight
    USABLE_SOLAR_GAINS_FRACTION("ANNUAL SPACE HEATING SUMMARY", "Usable Solar Gains Fraction", "Usable Solar Gains Fraction:",
            "usableSolarGainsFraction", false),
    AUXILARY_ENERGY_REQUIRED("ANNUAL SPACE HEATING SUMMARY", "Auxilary Energy Required (MJ)", "Auxilary Energy Required:", // Often misspelled as "Auxiliary"
            "auxilaryEnergyRequired", true),
    SPACE_HEATING_SYSTEM_LOAD("ANNUAL SPACE HEATING SUMMARY", "Space Heating System Load (MJ)", "Space Heating System Load:",
            "spaceHeatingSystemLoad", true),
    HEAT_PUMP_FURNACE_ANN_COP("ANNUAL SPACE HEATING SUMMARY", "Heat Pump and Furnace Annual COP", "Heat Pump and Furnace Annual COP:", // COP: Coefficient of Performance
            "heatPumpFurnaceAnnualCOP", false),
    HEAT_PUMP_ANN_ENERGY_CONSUMPTION("ANNUAL SPACE HEATING SUMMARY", "Heat Pump Annual Energy Consumption MJ", "Heat Pump Annual Energy Consumption:",
            "heatPumpAnnualEnergyConsumption", true),
    FURNACE_BOILER_SEASONAL_EFFICIENCY("ANNUAL SPACE HEATING SUMMARY", "Furnace/Boiler Seasonal efficiency",
            "Furnace/Boiler Seasonal efficiency:", "furnaceBoilerSeasonalEfficiency", false),
    FURNACE_BOILER_ANNUAL_ENERGY_CONSUMPTION("ANNUAL SPACE HEATING SUMMARY", "Furnace/Boiler Annual Energy Consumption MJ",
            "Furnace/Boiler Annual Energy", "furnaceBoilerAnnualEnergyConsumption", true),
    ANNUAL_SPACE_HETING_ENERGY("ANNUAL SPACE HEATING SUMMARY", "Annual Space Heating Energy Consumption (MJ)",
            "Annual Space Heating Energy Consumption", "annualSpaceHeatingEnergyConsumption",true),

    // DESIGN SPACE HEATING AND COOLING LOADS (Calculated peak loads)
    DESIGN_HEAT_LOSS("DESIGN SPACE HEATING AND COOLING LOADS", "Design Heat Loss* (Watts)", "Design Heat Loss",
            "designHeatLoss", true),
    DESIGN_COOLING_LOAD("DESIGN SPACE HEATING AND COOLING LOADS", "Design Cooling Load* for July (Watts)", "Design Cooling Load",
            "designCoolingLoad", true),

    // Section headers for broader categories
    SPACE_HEATING_SYSTEM_PERFORMANCE("SPACE HEATING SYSTEM PERFORMANCE", "SPACE HEATING SYSTEM PERFORMANCE", "SPACE HEATING SYSTEM PERFORMANCE",
            "spaceHeatingSystemPerformance", true),
    MONTHLY_ESTIMATED_ENERGY_CONSUMPTION_BY_DEVICE("MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE (MJ)", "MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE",
            "MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE", "monthlyEstimatedEnergyConsumptionByDevice", true);

    private final String parent; // The parent category/section for this field.
    private final String title;  // User-friendly title for display (e.g., Excel header).
    private final String keyword; // Keyword to search for in the PDF text to find this field.
    private final String fieldName; // Programmatic name for the field.
    private final boolean requiresUnitConversion; // True if the extracted value needs unit conversion.

    /**
     * Constructor for the Fields enum.
     * @param parent The parent category/section.
     * @param title The display title.
     * @param keyword The keyword for PDF text search.
     * @param fieldName The programmatic field name.
     * @param requiresUnitConversion Flag for unit conversion requirement.
     */
    Fields(String parent, String title, String keyword, String fieldName, boolean requiresUnitConversion) {
        this.parent = parent;
        this.title = title;
        this.keyword = keyword;
        this.fieldName = fieldName;
        this.requiresUnitConversion = requiresUnitConversion;
    }

    /** @return The parent category/section of this field. */
    public String getParent() {
        return parent;
    }

    /** @return The user-friendly title of this field. */
    public String getTitle() {
        return title;
    }

    /** @return The keyword used to identify this field in PDF text. */
    public String getKeyword() {
        return keyword;
    }

    /** @return The programmatic name of this field. */
    public String getFieldName() {
        return fieldName;
    }

    /** @return True if this field's value typically requires unit conversion after extraction, false otherwise. */
    public boolean requiresUnitConversion() {
        return requiresUnitConversion;
    }
}
