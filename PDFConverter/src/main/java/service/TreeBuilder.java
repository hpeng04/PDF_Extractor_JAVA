package service;

import lombok.Getter;
import model.Fields;
import model.PdfData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import utils.TreeNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreeBuilder implements ITreeBuilder {
    @Getter
    private TreeNode<Object> tree;
    private static final Map<String, Integer> monthOrder;
    private static final Map<String, Integer> occupantsOrder;
    private int fileIndex = -1;
    static {
        monthOrder = new HashMap<>();
        monthOrder.put("Jan", 1);
        monthOrder.put("Feb", 2);
        monthOrder.put("Mar", 3);
        monthOrder.put("Apr", 4);
        monthOrder.put("May", 5);
        monthOrder.put("Jun", 6);
        monthOrder.put("Jul", 7);
        monthOrder.put("Aug", 8);
        monthOrder.put("Sep", 9);
        monthOrder.put("Oct", 10);
        monthOrder.put("Nov", 11);
        monthOrder.put("Dec", 12);
        monthOrder.put("Ann", 13);
        occupantsOrder = new HashMap<>();
        occupantsOrder.put("ADULTS", 1);
        occupantsOrder.put("CHILDREN", 2);
        occupantsOrder.put("INFANTS", 3);
    }

    @Override
    public void buildTreeFromPDF(List<PdfData> pdfDataList, List<String> fileType) throws Exception {
        if (this.tree == null) {
            this.tree = new TreeNode<>();
        }


        for (int j = 0; j < pdfDataList.size(); j++) {
            fileIndex++;
            PdfData pdfData = pdfDataList.get(j);
            writeTree(" ", "Permit #", " ", fileIndex);
            writeTree(" ", "File Type", fileType.get(j), fileIndex);

            TreeNode<Object> tempWindowChar = new TreeNode<>();
            TreeNode<Object> tempBuildingParam = new TreeNode<>();
            TreeNode<Object> tempBuildingAssembly = new TreeNode<>();
            TreeNode<Object> tempFoundations = new TreeNode<>();
            LinkedHashMap<String, List<String>> foundationsMap = new LinkedHashMap<>();


            for (Fields field : Fields.values()) {
                switch (field) {
                    case ADULTS, CHILDREN, INFANTS -> {}
                    case OCCUPANTS -> {
                        String parentTitle = field.getParent();
                        String title = field.getTitle();
                        ConcurrentHashMap<String, String> unsortedMap = (ConcurrentHashMap<String, String>) get(field.getFieldName(), pdfData);
                        if (unsortedMap == null || unsortedMap.isEmpty()) {
                            writeTree(parentTitle, title, "", fileIndex);
                        } else {
                            Map<String, String> occupants = new TreeMap<>(Comparator.comparingInt(occupantsOrder::get));
                            occupants.putAll(unsortedMap);
                            StringBuilder occupantsString = new StringBuilder();
                            occupants.forEach((type, values) -> occupantsString.append(values));


                            writeTree(parentTitle, title, occupantsString, fileIndex);
                            // clean memory
                            occupants = null;
                            unsortedMap = null;
                        }
                    }
                    case INTERIOR_WALL, EXTERIOR_WALL -> {
                        List<String> wallType = (List<String>) get(field.getFieldName() + "Type", pdfData);
                        List<String> wallRValue = (List<String>) get(field.getFieldName() + "RValue", pdfData);
                        String parentTitle = field.getParent();
                        if (wallType.size() == wallRValue.size()) {
                            for (int i = 0; i < wallType.size(); i++) {
                                int num = i + 1;

                                String typeTitle = field.getTitle() + " " + " Type" + " - " + num;
                                String type = wallType.get(i);

                                String rValueTitle = field.getTitle()  + " " + " R-Value RSI" + " - " + num;
                                String rValue = wallRValue.get(i);
                                if (foundationsMap.containsKey(String.valueOf(i))) {
                                    List<String> values = new ArrayList<>(foundationsMap.get(String.valueOf(i)));
                                    values.add(typeTitle);
                                    values.add(type);
                                    values.add(rValueTitle);
                                    values.add(rValue);
                                    foundationsMap.put(String.valueOf(i), values);
                                } else {
                                    foundationsMap.put(String.valueOf(i), Arrays.asList(typeTitle, type, rValueTitle, rValue));
                                }
                            }
                        }
                        // clean memory
                        wallType = null;
                        wallRValue = null;
                    }
                    case ADDED_TO_SLAB -> {
                        List<String> addedToSlab = (List<String>) get(field.getFieldName(), pdfData);
                        if (addedToSlab == null || addedToSlab.isEmpty()) {
                            break;
                        }
                        String parentTitle = field.getParent();
                        for (int i = 0; i < addedToSlab.size(); i++) {
                            int num = i + 1;
                            String title = field.getTitle() + " - " + num;
                            String value = addedToSlab.get(i);
                            if (foundationsMap.containsKey(String.valueOf(i))) {
                                List<String> values = new ArrayList<>(foundationsMap.get(String.valueOf(i)));
                                values.add(title);
                                values.add(value);
                                foundationsMap.put(String.valueOf(i), values);
                            } else {
                                foundationsMap.put(String.valueOf(i), Arrays.asList(title, value));
                            }
                        }
                        // clean memory
                        addedToSlab = null;
                    }
                    case FLOORS_ABOVE_FOUND -> {
                        List<String> floorsAboveFound = (List<String>) get(field.getFieldName(), pdfData);
                        if (floorsAboveFound == null || floorsAboveFound.isEmpty()) {
                            break;
                        }
                        String parentTitle = field.getParent();
                        for (int i = 0; i < floorsAboveFound.size(); i++) {
                            int num = i + 1;
                            String title = field.getTitle() + " - " + num;
                            String value = floorsAboveFound.get(i);
                            if (foundationsMap.containsKey(String.valueOf(i))) {
                                List<String> values = new ArrayList<>(foundationsMap.get(String.valueOf(i)));
                                values.add(title);
                                values.add(value);
                                foundationsMap.put(String.valueOf(i), values);
                            } else {
                                foundationsMap.put(String.valueOf(i), Arrays.asList(title, value));
                            }
                        }
                        // clean memory
                        floorsAboveFound = null;
                    }
                    case WINDOW_CHARACTERISTICS -> {
                        HashMap<String, List<List<String>>> windowCharacteristics = pdfData.getWindowCharacteristics();
                        if (windowCharacteristics == null || windowCharacteristics.isEmpty()) {
                            break;
                        }
                        windowCharacteristics.forEach((orientation, characteristics) -> {
                            String parentTitle = field.getParent() + "-" + orientation;
                            for (int i = 0; i < characteristics.size(); i++) {
                                int num = i + 1;
                                // Area
                                String areaTitle = orientation + " " + num + " Total Area (m^2)"; // e.g. South 01 Total Area (m^2)
                                String areaValue = characteristics.get(i).get(0);
                                writeTree(tempWindowChar, parentTitle, areaTitle, areaValue, fileIndex);
                                // RSI
                                String rsiTitle = orientation + " " + num + " Window RSI"; // e.g. South 01 Window RSI
                                String rsiValue = characteristics.get(i).get(1);
                                writeTree(tempWindowChar, parentTitle, rsiTitle, rsiValue, fileIndex);
                                // SHGC
                                String shgcTitle = orientation + " " + num + " SHGC"; // e.g. South 01 SHGC
                                String shgcValue = characteristics.get(i).get(2);
                                writeTree(tempWindowChar, parentTitle, shgcTitle, shgcValue, fileIndex);
                            }
                        });
                        // clean memory
                        windowCharacteristics = null;
                    }
                    case CEILING_COMPONENTS, MAIN_WALL_COMPONENTS, EXPOSED_FLOORS, DOORS -> {
                        List<List<String>> components = (List<List<String>>) get(field.getFieldName(), pdfData);
                        String parentTitle = field.getParent();
                        if (components != null && !components.isEmpty()) {
                            for (int i = 0; i < components.size(); i++) {
                                int num = i + 1;
                                // Area
                                String areaTitle = field.getTitle() + " " + num + " Area (m^2)"; // e.g. Ceiling 01 Area (m^2)
                                String areaValue = components.get(i).get(0);
                                writeTree(tempBuildingParam, parentTitle, areaTitle, areaValue, fileIndex);
                                // RSI
                                String rsiTitle = field.getTitle() + " " + num + " RSI"; // e.g. Ceiling 01 RSI
                                String rsiValue = components.get(i).get(1);
                                writeTree(tempBuildingParam, parentTitle, rsiTitle, rsiValue, fileIndex);
                            }
                        }
                        // clean memory
                        components = null;
                    }
                    case BUILDING_ASSEMBLY_DETAILS -> {
                        HashMap<String, List<String>> assemblyDetails = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
                        if (assemblyDetails == null || assemblyDetails.isEmpty()) {
                            break;
                        }
                        assemblyDetails.forEach((component, rsis) -> {
                            String parentTitle = field.getParent() + "-" + component;
                            for (int i = 0; i < rsis.size(); i++) {
                                int num = i + 1;
                                String rsiTitle = component + " " + num + " Effective (RSI)"; // e.g. Ceiling 01 Effective (RSI)
                                String rsiValue = rsis.get(i);
                                writeTree(tempBuildingAssembly, parentTitle, rsiTitle, rsiValue, fileIndex);
                            }
                        });
                        // clean memory
                        assemblyDetails = null;
                    }
                    case BUILDING_PARAMETERS_ZONE_1, BUILDING_PARAMETERS_ZONE_2 -> {
                        HashMap<String, List<String>> parameters = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
                        if (parameters == null || parameters.isEmpty()) {
                            break;
                        }
                        String parentTitle = field.getParent();
                        parameters.forEach((component, values) -> {
                            // Area Net
                            String areaTitle = component + " Area Net (m^2)"; // e.g. Ceiling Area Net (m^2)
                            String areaValue = values.get(0);
                            writeTree(parentTitle, areaTitle, areaValue, fileIndex);
                            // RSI
                            String rsiTitle = component + " Effective (RSI)"; // e.g. Ceiling RSI
                            String rsiValue = values.get(1);
                            writeTree(parentTitle, rsiTitle, rsiValue, fileIndex);
                            // Heat Loss
                            String heatLossTitle = component + " Heat Loss (MJ)"; // e.g. Ceiling Heat Loss (MJ)
                            String heatLossValue = values.get(2);
                            writeTree(parentTitle, heatLossTitle, heatLossValue, fileIndex);
                        });
                        // clean memory
                        parameters = null;
                    }
                    case VENTILATION_REQUIREMENTS -> {
                        LinkedHashMap<String, String> requirements = (LinkedHashMap<String, String>) get(field.getFieldName(), pdfData);
                        if (requirements == null || requirements.isEmpty()) {
                            break;
                        }
                        String parentTitle = field.getParent();
                        requirements.forEach((rooms, value) -> writeTree(parentTitle, rooms, value, fileIndex));
                        // clean memory
                        requirements = null;
                    }
                    case AIR_LEAKAGE_MECHANICAL_VENTILATION -> {
                        List<String> airLeakage = (List<String>) get(field.getFieldName(), pdfData);
                        if (airLeakage == null || airLeakage.isEmpty()) {
                            break;
                        }
                        String parentTitle = field.getParent();
                        // Volume
                        String volumeTitle = "House Volume (m^3)";
                        String volumeValue = airLeakage.get(0);
                        writeTree(parentTitle, volumeTitle, volumeValue, fileIndex);
                        // MJ
                        String mjTitle = "Heat Loss (MJ)";
                        String mjValue = airLeakage.get(1);
                        writeTree(parentTitle, mjTitle, mjValue, fileIndex);

                        // clean memory
                        airLeakage = null;
                    }
                    case SPACE_HEATING_SYSTEM_PERFORMANCE -> {
                        HashMap<String, List<String>> unsortedMap = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
                        if (unsortedMap == null || unsortedMap.isEmpty()) {
                            break;
                        }
                        Map<String, List<String>> systemPerformance = new TreeMap<>(Comparator.comparingInt(monthOrder::get));
                        systemPerformance.putAll(unsortedMap);
                        String parentTitle = field.getParent();
                        systemPerformance.forEach((month, value) -> {
                            // Space Heating Load
                            String spaceHeatingTitle = month + " Space Heating Load (MJ)"; // e.g. January Space Heating Load
                            String spaceHeatingValue = value.get(0);
                            writeTree(parentTitle, spaceHeatingTitle, spaceHeatingValue, fileIndex);
                            // Furnace Input
                            String furnaceInputTitle = month + " Furnace Input (MJ)"; // e.g. January Furnace Input
                            String furnaceInputValue = value.get(1);
                            writeTree(parentTitle, furnaceInputTitle, furnaceInputValue, fileIndex);
                            // System COP
                            String systemCopTitle = month + " System COP"; // e.g. January System COP
                            String systemCopValue = value.get(2);
                            writeTree(parentTitle, systemCopTitle, systemCopValue, fileIndex);
                        });

                        // clean memory
                        systemPerformance = null;
                        unsortedMap = null;
                    }
                    case MONTHLY_ESTIMATED_ENERGY_CONSUMPTION_BY_DEVICE -> {
                        HashMap<String, List<String>> unsortedMap = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
                        if (unsortedMap == null || unsortedMap.isEmpty()) {
                            break;
                        }
                        Map<String, List<String>> energyConsumption = new TreeMap<>(Comparator.comparingInt(monthOrder::get));
                        energyConsumption.putAll(unsortedMap);
                        String parentTitle = field.getParent();
                        energyConsumption.forEach((month, value) -> {
                            // Space Heating Primary
                            String spaceHeatingPrimaryTitle = month + " Space Heating Primary"; // e.g. January Space Heating Primary
                            String spaceHeatingPrimaryValue = value.get(0);
                            writeTree(parentTitle, spaceHeatingPrimaryTitle, spaceHeatingPrimaryValue, fileIndex);
                            // Space Heating Secondary
                            String spaceHeatingSecondaryTitle = month + " Space Heating Secondary"; // e.g. January Space Heating Secondary
                            String spaceHeatingSecondaryValue = value.get(1);
                            writeTree(parentTitle, spaceHeatingSecondaryTitle, spaceHeatingSecondaryValue, fileIndex);
                            // DHW Heating Primary
                            String dhwHeatingPrimaryTitle = month + " DHW Heating Primary"; // e.g. January DHW Heating Primary
                            String dhwHeatingPrimaryValue = value.get(2);
                            writeTree(parentTitle, dhwHeatingPrimaryTitle, dhwHeatingPrimaryValue, fileIndex);
                            // DHW Heating Secondary
                            String dhwHeatingSecondaryTitle = month + " DHW Heating Secondary"; // e.g. January DHW Heating Secondary
                            String dhwHeatingSecondaryValue = value.get(3);
                            writeTree(parentTitle, dhwHeatingSecondaryTitle, dhwHeatingSecondaryValue, fileIndex);
                            // Lights & Appliances
                            String lightsAppliancesTitle = month + " Lights & Appliances"; // e.g. January Lights & Appliances
                            String lightsAppliancesValue = value.get(4);
                            writeTree(parentTitle, lightsAppliancesTitle, lightsAppliancesValue, fileIndex);
                            // HRV & FANS
                            String hrvFansTitle = month + " HRV & FANS"; // e.g. January HRV & FANS
                            String hrvFansValue = value.get(5);
                            writeTree(parentTitle, hrvFansTitle, hrvFansValue, fileIndex);
                            // AC
                            String acTitle = month + " Air Conditioner"; // e.g. January Air Conditioner
                            String acValue = value.get(6);
                            writeTree(parentTitle, acTitle, acValue, fileIndex);
                        });

                        // clean memory
                        energyConsumption = null;
                        unsortedMap = null;
                    }
                    default -> {
                        String parentTitle = field.getParent();
                        String title = field.getTitle();
                        Object value = get(field.getFieldName(), pdfData) == null ? " " : get(field.getFieldName(), pdfData);
                        writeTree(parentTitle, title, value, fileIndex);
                    }
                }
            }

            // add the window characteristics
            if (tempWindowChar != null && tempWindowChar.getChildren() != null) {
                for (TreeNode<Object> child : tempWindowChar.getChildren()) {
                    this.tree.insert(child, this.tree.getIndex("HOUSE TEMPERATURES") + 1);
                }
            }
            // add the building parameters
            if (tempBuildingParam != null && tempBuildingParam.getChildren() != null) {
                for (TreeNode<Object> child : tempBuildingParam.getChildren()) {
                    this.tree.insert(child, this.tree.getIndex("Above grade fraction of") + 1);
                }
            }

            // write foundationsMap to temp tree
            if (!foundationsMap.isEmpty()) {
                foundationsMap.forEach((key, values) -> {
                    String parentTitle = "Foundations";
                    for (int i = 0; i < values.size(); i += 2) {
                        String title = values.get(i);
                        String value = values.get(i + 1);
                        writeTree(tempFoundations, parentTitle, title, value, fileIndex);
                    }
                });
            }
            //add foundations
            if (tempFoundations != null && tempFoundations.getChildren() != null) {
                int index = -1;
                index = Math.max(this.tree.getIndex("Building Parameter Details - Doors"), index);
                index = Math.max(this.tree.getIndex("Building Parameter Details - Exposed Floors"), index);
                index = Math.max(this.tree.getIndex("Building Parameter Details - Main Wall Components"), index);
                index = Math.max(this.tree.getIndex("Building Parameter Details - Ceiling Components"), index);

                for (TreeNode<Object> child : tempFoundations.getChildren()) {
                    this.tree.insert(child, index + 1);
                }
            }
            // add the building assembly details
            if (tempBuildingAssembly != null && tempBuildingAssembly.getChildren() != null) {
                for (TreeNode<Object> child : tempBuildingAssembly.getChildren()) {
                    this.tree.insert(child, this.tree.getIndex("Foundations") + 1);
                }
            }
        }

        // sort contents to desired order (below are reversed order)
        List<String> windowCharOrder = Arrays.asList("WINDOW CHARACTERISTICS-Southwest", "WINDOW CHARACTERISTICS-Northwest", "WINDOW CHARACTERISTICS-Northeast",
                "WINDOW CHARACTERISTICS-Southeast", "WINDOW CHARACTERISTICS-West", "WINDOW CHARACTERISTICS-North",
                "WINDOW CHARACTERISTICS-East", "WINDOW CHARACTERISTICS-South");

        List<String> buildingParamOrder = Arrays.asList("Building Parameter Details - Doors", "Building Parameter Details - Exposed Floors",
                "Building Parameter Details - Main Wall Components", "Building Parameter Details - Ceiling Components");

        List<String> buildingAssemblyOrder = Arrays.asList("Building Assembly Details-FLOORS ABOVE", "Building Assembly Details-EXPOSED FLOORS",
                "Building Assembly Details-MAIN WALL COMPONENTS", "Building Assembly Details-CEILING COMPONENTS");

        sortContentParent(windowCharOrder, this.tree, "HOUSE TEMPERATURES");
        sortContentParent(buildingParamOrder, this.tree, "Above grade fraction of");
        sortContentParent(buildingAssemblyOrder, this.tree, "Foundations");

        // sort contents to desired order (below are normal order)
        List<String> buildingParamZone1Order = Arrays.asList("Ceiling", "Main Walls", "Doors", "Exposed floors",
                "South Windows", "East Windows", "North Windows", "West Windows", "Southeast Windows",
                "Northeast Windows", "Northwest Windows", "Southwest Windows", "Slab on Grade");

        List<String> buildingParamZone2Order = Arrays.asList("Walls above grade", "South Windows", "East Windows", "North Windows",
                "West Windows", "Southeast Windows", "Northeast Windows", "Northwest Windows", "Southwest Windows",
                "Basement floor header", "Pony walls", "Below grade foundation");

        List<String> ventilationRequirementsOrder = Arrays.asList("Kitchen, Living Room, Dining Room", "Utility Room",
                "Bedroom", "Bathroom", "Other", "Basement Rooms");

        sortContentChild(buildingParamZone1Order, this.tree, "Building Parameters Summary - Zone 1: Above grade");
        sortContentChild(buildingParamZone2Order, this.tree, "Building Parameters Summary - Zone 2: Basement");
        sortContentChild(ventilationRequirementsOrder, this.tree, "F326 Ventilation Requirements");

        TreeNode<Object> tempZone2 = this.tree.getNode("Building Parameters Summary - Zone 2: Basement");
        this.tree.removeChild("Building Parameters Summary - Zone 2: Basement");
        if (tempZone2 != null) {
            this.tree.insert(tempZone2, this.tree.getIndex("Building Parameters Summary - Zone 1: Above grade") + 1);
        }

        // clean memory
        windowCharOrder = null;
        buildingParamOrder = null;
        buildingAssemblyOrder = null;
        buildingParamZone1Order = null;
        buildingParamZone2Order = null;
        ventilationRequirementsOrder = null;

    }


    public void buildTreeFromExcel(Sheet sheet) {
        if (this.tree == null) {
            this.tree = new TreeNode<>();
        }

        String parentTitle = "";
        for (int column = 0; column < sheet.getRow(3).getLastCellNum(); column++) {
            fileIndex = -1;
            Cell parentTitleCell = sheet.getRow(2).getCell(column);
            if (parentTitleCell != null) {
                parentTitle = parentTitleCell.getStringCellValue();
            }

            Cell subTitleCell = sheet.getRow(3).getCell(column);
            if (subTitleCell != null) {
                String subTitle = subTitleCell.getStringCellValue();
                for (int row = 4; row <= sheet.getLastRowNum(); row++) {
                    fileIndex++;
                    Cell dataCell = sheet.getRow(row).getCell(column);
                    if (dataCell != null) {
                        CellType dataCellType = dataCell.getCellType();
                        switch (dataCellType) {
                            case STRING -> {
                                String data = dataCell.getStringCellValue();
                                writeTree(parentTitle, subTitle, data, fileIndex);
                            }
                            case NUMERIC -> {
                                double data = dataCell.getNumericCellValue();
                                writeTree(parentTitle, subTitle, data, fileIndex);
                            }
                        }
                    }
                }

            }
        }
    }

    // sort content to desired order for WINDOW CHARACTERISTICS, Building Parameter Details, Building Assembly Details,
    private void sortContentParent(List<String> order, TreeNode<Object> tree, String index) {
        TreeNode<Object>[] temp = new TreeNode[order.size()];
        for (int i = 0; i < order.size(); i++) {
            if (tree.containsTitle(order.get(i))) {
                temp[i] = tree.getNode(order.get(i));
                tree.removeChild(order.get(i));
            }
        }

        for (TreeNode<Object> node : temp) {
            if (node != null) {
                tree.insert(node, tree.getIndex(index) + 1);
            }
        }
    }
    // sort content to desired order for Building Parameters Summary, and F326 Ventilation Requirements
    private void sortContentChild(List<String> order, TreeNode<Object> tree, String parentTitle) {
        TreeNode<Object> parent = tree.getNode(parentTitle);
        if (parent == null) return;

        List<TreeNode<Object>> tempList = new ArrayList<>();
        for (String s : order) {
            for (TreeNode<Object> childTitle : parent.getChildren()) {
                if (childTitle.containsPartialTitle(s)) {
                    tempList.add(childTitle);
                }
            }
        }
        parent.removeAllChildren();
        tempList.forEach(parent::addChild);
    }

    private void writeTree(String parentTitle, String title, Object value, int fileIndex) {
        writeTree(this.tree, parentTitle, title, value, fileIndex);
    }

    private void writeTree(TreeNode<Object> tree, String parentTitle, String title, Object value, int fileIndex) {
        if (!tree.containsTitle(parentTitle)) {
            tree.addChild(new TreeNode<>(parentTitle, true, fileIndex));
        }
        if (!tree.getNode(parentTitle).containsTitle(title)) {
            tree.getNode(parentTitle).addChild(new TreeNode<>(title, true, fileIndex));
        }
        tree.getNode(parentTitle).getNode(title).addChild(new TreeNode<>(value, false, fileIndex));
    }

    private Object get(String name, PdfData pdfData) {
        String fieldName = name.substring(0, 1).toUpperCase() + name.substring(1);
        try {
            Method getter = PdfData.class.getMethod("get" + fieldName);
            return getter.invoke(pdfData);
        } catch (NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }


}
