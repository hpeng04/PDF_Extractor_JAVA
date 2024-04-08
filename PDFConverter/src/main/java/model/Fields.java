package model;

public enum Fields {
    FILE(" ", "File", "File:", "file", false),
    DATE_OF_ENTRY(" ", "Date of entry", "Date of entry:", "dataOfEntry", false),
    COMPANY(" ", "Company", "Company:", "company", false),
    FRONT_ORIENTATION("GENERAL HOUSE CHARACTERISTICS", "Front orientation", "Front orientation:", "frontOrientation", false),
    YEAR_HOUSE_BUILT("GENERAL HOUSE CHARACTERISTICS", "Year House Built", "Year House Built:", "yearHouseBuilt", false),
    OCCUPANTS("GENERAL HOUSE CHARACTERISTICS", "Occupants", "Occupants", "occupants", false),
    ADULTS("GENERAL HOUSE CHARACTERISTICS", "Adults", "Adults", "occupants", false),
    CHILDREN("GENERAL HOUSE CHARACTERISTICS", "Children", "Children", "occupants", false),
    INFANTS("GENERAL HOUSE CHARACTERISTICS", "Infants", "Infants", "occupants", false),
    // HOUSE TEMPERATURES
    DAYTIME_SETPOINT("HOUSE TEMPERATURES", "Daytime Setpoint (\u00B0C)", "Daytime Setpoint:", "daytimeSetpoint", true),
    NIGHTIME_SETPOINT("HOUSE TEMPERATURES", "Nightime Setpoint (\u00B0C)", "Nightime Setpoint:", "nightimeSetpoint", true),
    NIGHTIME_SETBACK_DURATION("HOUSE TEMPERATURES", "Nightime Setback Duration", "Nightime Setback", "nightimeSetbackDuration", true),
    BASEMENT_SETPOINT("HOUSE TEMPERATURES", "Basement Setpoint (\u00B0C)", "Basement Setpoint", "basementSetpoint", true),
    TEMP_RISE_FROM("HOUSE TEMPERATURES", "TEMP. Rise (\u00B0C)", "TEMP. Rise from", "tempRiseFrom", true),
    // WINDOW CHARACTERISTICS
    WINDOW_CHARACTERISTICS("WINDOW CHARACTERISTICS", "WINDOW CHARACTERISTICS","SHGC ER", "windowCharacteristics", true),
    ABOVE_GRADE_FRACTION("Above grade fraction of", "wall area occupied by windows",
            "Above grade fraction of wall area occupied by windows:", "aboveGradeFraction", false),
    // BUILDING PARAMETER DETAILS
    CEILING_COMPONENTS("Building Parameter Details - Ceiling Components", "Ceiling",
            "CEILING COMPONENTS", "ceilingComponents", true),
    MAIN_WALL_COMPONENTS("Building Parameter Details - Main Wall Components", "Main Wall",
            "MAIN WALL COMPONENTS", "mainWallComponents", true),
    EXPOSED_FLOORS("Building Parameter Details - Exposed Floors", "Exposed Floors",
            "EXPOSED FLOORS", "exposedFloors", true),
    DOORS("Building Parameter Details - Doors", "Doors", "DOORS", "doors", true),
    INTERIOR_WALL("Foundations", "Interior wall type", "Interior wall type:", "interiorWall", true), //TODO
    EXTERIOR_WALL("Foundations", "Exterior wall type", "Exterior wall type:", "exteriorWall", true), //TODO
    ADDED_TO_SLAB("Foundations", "Added to slab type R-Value RSI", "Added to slab", "addedToSlab", true), //TODO
    FLOORS_ABOVE_FOUND("Foundations", "Floors Above Found. R-Value RSI", "Floors Above", "floorsAboveFound", true), //TODO
    // BUILDING ASSEMBLY DETAILS
    BUILDING_ASSEMBLY_DETAILS("Building Assembly Details", "Building Assembly Details",
            "BUILDING ASSEMBLY DETAILS", "buildingAssemblyDetails", true),
    // BUILDING PARAMETERS SUMMARY
    BUILDING_PARAMETERS_ZONE_1("Building Parameters Summary - Zone 1: Above grade", "Zone 1: Above Grade",
            "ZONE 1: Above Grade", "buildingParametersZone1", true),
    BUILDING_PARAMETERS_ZONE_2("Building Parameters Summary - Zone 2: Basement", "ZONE 2 : Basement",
            "ZONE 2 : Basement", "buildingParametersZone2", true),
    AIR_LEAKAGE_MECHANICAL_VENTILATION("Air Leakage and Mechanical Ventilation", "Air Leakage and Mechanical Ventilation",
            "Air Leakage and Mechanical Ventilation", "airLeakageMechanicalVentilation", true),
    // AIR LEAKAGE AND MECHANICAL VENTILATION
    AIR_LEAKAGE_TEST_RESULT("Air Leakage and Mechanical Ventilation", "Air Leakage Test Results at 50 Pa.",
            "Air Leakage Test Results at 50 Pa.",
            "airLeakageTestResult", false),
    //  VENTILATION REQUIREMENTS
    VENTILATION_REQUIREMENTS("F326 Ventilation Requirements", "F326 VENTILATION REQUIREMENTS", "F326 VENTILATION REQUIREMENTS",
            "ventilationRequirements", true),
    // CENTRAL VENTILATION SYSTEM
    SYSTEM_TYPE("Central Ventilation System", "System Type", "System Type:", "systemType", false),
    FP_POWER_0("Central Ventilation System", "Fan and Preheater Power at 0.0 °C", "Fan and Preheater Power at 0", "fpPower0", false),
    FP_POWER_MINUS_25("Central Ventilation System", "Fan and Preheater Power at -25.0 °C", "Fan and Preheater Power at -25", "fpPowerMinus25", false),
    HEAT_RE_EFFICIENCY_0("Central Ventilation System", "Sensible Heat Recovery Efficiency at 0.0 °C",
            "Sensible Heat Recovery Efficiency at 0", "sensibleHeatRecoveryEfficiency0", false),
    HEAT_RE_EFFICIENCY_MINUS_25("Central Ventilation System", "Sensible Heat Recovery Efficiency at -25.0 °C",
            "Sensible Heat Recovery Efficiency at -", "sensibleHeatRecoveryEfficiencyMinus25", false),
    // NEW ERS VENTILATION DATA
    AIR_DC_TYPE("NEW ERS VENTILATION DATA", "Air Distribution/circulation type", "Air Distribution/circulation type:", "airDcType", false),
    AIR_DC_FAN_POWER("NEW ERS VENTILATION DATA", "Air Distribution/circulation fan power", "Air Distribution/circulation fan power:", "airDcFanPower", false),
    OPERATION_SCHEDULE("NEW ERS VENTILATION DATA", "Operation schedule", "Operation schedule:", "operationSchedule", false),
    // AIR LEAKAGE AND MECHANICAL VENTILATION SUMMARY
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
    AFUE("SPACE HEATING SYSTEM", "AFUE", "AFUE:", "afue", false),
    HIGH_SPEED_FAN_POWER("SPACE HEATING SYSTEM", "High Speed Fan Power", "High Speed Fan Power:",
            "highSpeedFanPower", false),
    // DOMESTIC WATER HEATING SYSTEM
    PRIMARY_WATER_HEATING_FUEL("DOMESTIC WATER HEATING SYSTEM", "PRIMARY Water Heating Fuel", "PRIMARY Water Heating",
            "primaryWaterHeatingFuel", false),
    WATER_HEATING_EQUIPMENT("DOMESTIC WATER HEATING SYSTEM", "Water Heating Equipment", "Water Heating",
            "waterHeatingEquipment", false),
    ENERGY_FACTOR("DOMESTIC WATER HEATING SYSTEM", "Energy Factor", "Energy Factor:",
            "energyFactor", false),
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
            "usableInternalGains", true),
    USABLE_INTERNAL_GAINS_FRACTION("ANNUAL SPACE HEATING SUMMARY", "Usable Internal Gains Fraction", "Usable Internal Gains Fraction:",
            "usableInternalGainsFraction", false),
    USABLE_SOLAR_GAINS("ANNUAL SPACE HEATING SUMMARY", "Usable Solar Gains (MJ)", "Usable Solar Gains:",
            "usableSolarGains", true),
    USABLE_SOLAR_GAINS_FRACTION("ANNUAL SPACE HEATING SUMMARY", "Usable Solar Gains Fraction", "Usable Solar Gains Fraction:",
            "usableSolarGainsFraction", false),
    AUXILARY_ENERGY_REQUIRED("ANNUAL SPACE HEATING SUMMARY", "Auxilary Energy Required (MJ)", "Auxilary Energy Required:",
            "auxilaryEnergyRequired", true),
    SPACE_HEATING_SYSTEM_LOAD("ANNUAL SPACE HEATING SUMMARY", "Space Heating System Load (MJ)", "Space Heating System Load:",
            "spaceHeatingSystemLoad", true),
    HEAT_PUMP_FURNACE_ANN_COP("ANNUAL SPACE HEATING SUMMARY", "Heat Pump and Furnace Annual COP", "Heat Pump and Furnace Annual COP:",
            "heatPumpFurnaceAnnualCOP", false),
    HEAT_PUMP_ANN_ENERGY_CONSUMPTION("ANNUAL SPACE HEATING SUMMARY", "Heat Pump Annual Energy Consumption MJ", "Heat Pump Annual Energy Consumption:",
            "heatPumpAnnualEnergyConsumption", true),
    FURNACE_BOILER_SEASONAL_EFFICIENCY("ANNUAL SPACE HEATING SUMMARY", "Furnace/Boiler Seasonal efficiency",
            "Furnace/Boiler Seasonal efficiency:", "furnaceBoilerSeasonalEfficiency", false),
    FURNACE_BOILER_ANNUAL_ENERGY_CONSUMPTION("ANNUAL SPACE HEATING SUMMARY", "Furnace/Boiler Annual Energy Consumption MJ",
            "Furnace/Boiler Annual Energy", "furnaceBoilerAnnualEnergyConsumption", true),
    ANNUAL_SPACE_HETING_ENERGY("ANNUAL SPACE HEATING SUMMARY", "Annual Space Heating Energy Consumption (MJ)",
            "Annual Space Heating Energy Consumption", "annualSpaceHeatingEnergyConsumption",true),
    // DESIGN SPACE HEATING AND COOLING LOADS
    DESIGN_HEAT_LOSS("DESIGN SPACE HEATING AND COOLING LOADS", "Design Heat Loss* (Watts)", "Design Heat Loss",
            "designHeatLoss", true),
    DESIGN_COOLING_LOAD("DESIGN SPACE HEATING AND COOLING LOADS", "Design Cooling Load* for July (Watts)", "Design Cooling Load",
            "designCoolingLoad", true),

    SPACE_HEATING_SYSTEM_PERFORMANCE("SPACE HEATING SYSTEM PERFORMANCE", "SPACE HEATING SYSTEM PERFORMANCE", "SPACE HEATING SYSTEM PERFORMANCE",
            "spaceHeatingSystemPerformance", true),
    MONTHLY_ESTIMATED_ENERGY_CONSUMPTION_BY_DEVICE("MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE (MJ)", "MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE",
            "MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE", "monthlyEstimatedEnergyConsumptionByDevice", true);



    private final String parent;
    private final String title;
    private final String keyword;
    private final String fieldName;
    private final boolean requiresUnitConversion;


    Fields(String parent, String title, String keyword, String fieldName, boolean requiresUnitConversion) {
        this.parent = parent;
        this.title = title;
        this.keyword = keyword;
        this.fieldName = fieldName;
        this.requiresUnitConversion = requiresUnitConversion;
    }

    public String getParent() {
        return parent;
    }
    public String getTitle() {
        return title;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean requiresUnitConversion() {
        return requiresUnitConversion;
    }

}
