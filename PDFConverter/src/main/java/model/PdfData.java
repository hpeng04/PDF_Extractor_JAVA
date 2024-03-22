package model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

@Getter @Setter
public class PdfData {
    private String file;
    private String dataOfEntry;
    private String company;
    private String frontOrientation;
    private String yearHouseBuilt;
    private String occupants;

    private String daytimeSetpoint;
    private String nightimeSetpoint;
    private String nightimeSetbackDuration;
    private String basementSetpoint;
    private String tempRiseFrom;

    private HashMap<String, List<List<String>>> windowCharacteristics; // Orientation -> [[Area1, RSI1, SHGC1], [Area2, RSI2, SHGC2],...]
    private String aboveGradeFraction;
    private List<List<String>> ceilingComponents; // [[Gross Area, RSI],...]
    private List<List<String>> mainWallComponents; // [[Gross Area, RSI],...]
    private List<List<String>> exposedFloors; // [[Area, RSI],...]
    private List<List<String>> doors; // [[Gross Area, RSI],...]
    private String interiorWallType;
    private String interiorWallRValue;
    private String exteriorWallType;
    private String exteriorWallRValue;
    private String addedToSlab;
    private String floorsAboveFound;

    private HashMap<String, List<String>> buildingAssemblyDetails; // Component Type -> [RSI1, RSI2,...]
    private HashMap<String, List<String>> buildingParametersZone1; // Component -> [Area, RSI, Heat Loss]
    private HashMap<String, List<String>> buildingParametersZone2; // Component -> [Area, RSI, Heat Loss]
    private List<String> airLeakageMechanicalVentilation; // [Volume, MJ]

    private String airLeakageTestResult;

    private HashMap<String, String> ventilationRequirements;

    private String systemType;
    private String fpPower0;
    private String fpPowerMinus25;
    private String sensibleHeatRecoveryEfficiency0;
    private String sensibleHeatRecoveryEfficiencyMinus25;

    private String airDcType;
    private String airDcFanPower;
    private String operationSchedule;

    private String grossAirLeakage;
    private String seasonalHeatRecovery;
    private String estVentLoadHeating;
    private String estVentLoadNonHeating;
    private String netAirLeakage;

    private String primaryHeatingFuel;
    private String afue;
    private String highSpeedFanPower;

    private String primaryWaterHeatingFuel;
    private String waterHeatingEquipment;
    private String energyFactor;

    private String dailyHotWaterConsumption;
    private String hotWaterTemperature;
    private String estimatedDomesticWaterHeatingLoad;
    private String primaryDomesticWaterHeatingLoadConsumption;
    private String primarySystemSeasonalEfficiency;

    private String grossSpaceHeatLoss;
    private String grossSpaceHeatingLoad;
    private String usableInternalGains;
    private String usableInternalGainsFraction;
    private String usableSolarGains;
    private String usableSolarGainsFraction;
    private String auxilaryEnergyRequired;
    private String spaceHeatingSystemLoad;
    private String furnaceBoilerSeasonalEfficiency;
    private String furnaceBoilerAnnualEnergyConsumption;

    private String designHeatLoss;
    private String designCoolingLoad;

    private HashMap<String, List<String>> spaceHeatingSystemPerformance; // Jan -> [Space Heating Load, Furnace Input, System Cop]
    private HashMap<String, List<String>> monthlyEstimatedEnergyConsumptionByDevice; // Jan -> [...]

}
