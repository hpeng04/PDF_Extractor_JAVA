package model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data model class representing the information extracted from a PDF document.
 * This class uses Lombok's {@code @Getter} and {@code @Setter} to reduce boilerplate code
 * for accessing and modifying its fields. Each field corresponds to a piece of data
 * typically found in building energy analysis reports.
 * The structure of this class mirrors the {@link Fields} enum to a large extent.
 */
@Getter @Setter
public class PdfData {
    // General Information
    private String file; // Name/path of the source PDF file
    private String dataOfEntry; // Date of data entry or report generation
    private String company; // Company associated with the data/report

    // General House Characteristics
    private String frontOrientation; // Compass orientation of the front of the house
    private String yearHouseBuilt; // Year the house was constructed
    private ConcurrentHashMap<String, String> occupants; // Map to store occupant types (e.g., "Adults") and their counts/details. Example: ["Adults", "2"]

    // House Temperatures (Thermostat Settings)
    private String daytimeSetpoint; // Daytime thermostat setpoint in °C
    private String nightimeSetpoint; // Nighttime thermostat setpoint in °C
    private String nightimeSetbackDuration; // Duration of the nighttime temperature setback
    private String basementSetpoint; // Thermostat setpoint for the basement in °C
    private String tempRiseFrom; // Temperature rise data, possibly related to heating system performance

    // Window Characteristics
    // Key: Orientation (e.g., "North"), Value: List of window component details (Area, RSI, SHGC)
    private HashMap<String, List<List<String>>> windowCharacteristics;
    private String aboveGradeFraction; // Fraction of above-grade wall area occupied by windows

    // Building Parameter Details (Thermal properties of building components)
    // Each List<List<String>> typically holds [Component Description, Gross Area, RSI/R-value, ...]
    private List<List<String>> ceilingComponents; // Details of ceiling construction elements
    private List<List<String>> mainWallComponents; // Details of main wall construction elements
    private List<List<String>> exposedFloors; // Details of exposed floor construction elements
    private List<List<String>> doors; // Details of door construction elements

    // Foundation Details
    // Initialized with a singleton list containing an empty string to avoid null pointers if not found.
    private List<String> interiorWallType = Collections.singletonList(""); // Type of interior foundation wall
    private List<String> interiorWallRValue = Collections.singletonList(""); // R-value/RSI of interior foundation wall
    private List<String> exteriorWallType = Collections.singletonList(""); // Type of exterior foundation wall
    private List<String> exteriorWallRValue = Collections.singletonList(""); // R-value/RSI of exterior foundation wall
    private List<String> addedToSlab; // Insulation details added to the slab
    private List<String> floorsAboveFound; // Insulation details for floors above the foundation

    // Building Assembly & Parameter Summaries
    // Key: Component Type/Name, Value: List of properties (e.g., RSI values, Area, Heat Loss)
    private HashMap<String, List<String>> buildingAssemblyDetails; // Details of building assemblies
    private HashMap<String, List<String>> buildingParametersZone1; // Summarized parameters for Zone 1 (Above Grade)
    private HashMap<String, List<String>> buildingParametersZone2; // Summarized parameters for Zone 2 (Basement)

    // Air Leakage and Mechanical Ventilation
    private List<String> airLeakageMechanicalVentilation; // General data, possibly [Volume, MJ]
    private String airLeakageTestResult; // Result of air leakage test (e.g., ACH@50Pa)
    // Key: Requirement Name, Value: Requirement Value
    private LinkedHashMap<String, String> ventilationRequirements; // Specific ventilation requirements (e.g., F326)

    // Central Ventilation System (e.g., HRV/ERV details)
    private String systemType; // Type of central ventilation system
    private String fpPower0; // Fan and preheater power at 0.0 °C
    private String fpPowerMinus25; // Fan and preheater power at -25.0 °C
    private String sensibleHeatRecoveryEfficiency0; // Sensible heat recovery efficiency at 0.0 °C
    private String sensibleHeatRecoveryEfficiencyMinus25; // Sensible heat recovery efficiency at -25.0 °C

    // New ERS (Energy Recovery System) Ventilation Data
    private String airDcType; // Air distribution/circulation type
    private String airDcFanPower; // Air distribution/circulation fan power
    private String operationSchedule; // Operation schedule of the ventilation system

    // Air Leakage and Mechanical Ventilation Summary (Energy Loads in MJ)
    private String grossAirLeakage; // Gross air leakage and mechanical ventilation energy load
    private String seasonalHeatRecovery; // Seasonal heat recovery ventilator efficiency
    private String estVentLoadHeating; // Estimated ventilation electrical load during heating hours
    private String estVentLoadNonHeating; // Estimated ventilation electrical load during non-heating hours
    private String netAirLeakage; // Net air leakage and mechanical ventilation load

    // Space Heating System
    private String primaryHeatingFuel; // Primary fuel type for space heating
    private String afue; // Annual Fuel Utilization Efficiency of the heating system
    private String highSpeedFanPower; // Power consumption of the high-speed fan (furnace/air handler)

    // Domestic Water Heating (DHW) System
    private String primaryWaterHeatingFuel; // Primary fuel type for water heating
    private String waterHeatingEquipment; // Type of water heating equipment
    private String energyFactor; // Energy Factor (EF) of the water heater

    // Annual Domestic Water Heating Summary
    private String dailyHotWaterConsumption; // Daily hot water consumption in Litres
    private String hotWaterTemperature; // Hot water supply temperature in °C
    private String estimatedDomesticWaterHeatingLoad; // Estimated DHW load in MJ
    private String primaryDomesticWaterHeatingLoadConsumption; // Primary DHW energy consumption in MJ
    private String primarySystemSeasonalEfficiency; // Seasonal efficiency of the primary DHW system

    // Annual Space Heating Summary (Energy values typically in MJ)
    private String grossSpaceHeatLoss; // Gross space heat loss
    private String grossSpaceHeatingLoad; // Gross space heating load
    private String usableInternalGains; // Usable heat gains from internal sources (occupants, appliances)
    private String usableInternalGainsFraction; // Fraction of internal gains that are usable
    private String usableSolarGains; // Usable heat gains from solar radiation
    private String usableSolarGainsFraction; // Fraction of solar gains that are usable
    private String auxilaryEnergyRequired; // Auxiliary energy required for space heating (often misspelled "auxiliary")
    private String spaceHeatingSystemLoad; // Total space heating system load
    private String heatPumpFurnaceAnnualCOP; // Annual Coefficient of Performance (COP) for heat pump/furnace
    private String heatPumpAnnualEnergyConsumption; // Annual energy consumption of the heat pump
    private String furnaceBoilerSeasonalEfficiency; // Seasonal efficiency of the furnace/boiler
    private String furnaceBoilerAnnualEnergyConsumption; // Annual energy consumption of the furnace/boiler
    private String annualSpaceHeatingEnergyConsumption; // Total annual space heating energy consumption

    // Design Space Heating and Cooling Loads
    private String designHeatLoss; // Design heat loss in Watts
    private String designCoolingLoad; // Design cooling load for July in Watts

    // Performance and Monthly Consumption Data (complex structures)
    // Key: Month (e.g., "Jan"), Value: List of performance metrics (e.g., Space Heating Load, Furnace Input, System COP)
    private HashMap<String, List<String>> spaceHeatingSystemPerformance;
    // Key: Month (e.g., "Jan"), Value: List of energy consumption values by device
    private HashMap<String, List<String>> monthlyEstimatedEnergyConsumptionByDevice;
}
