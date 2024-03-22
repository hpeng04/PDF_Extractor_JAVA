package service;

import lombok.Getter;
import model.Fields;
import model.PdfData;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import util.TreeNode;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class TreeBuilder implements ITreeBuilder {
    @Getter
    private TreeNode<Object> tree;
    private static final Map<String, Integer> monthOrder;
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
    }

    @Override
    public void buildTreeFromPDF(List<PdfData> pdfDataList, List<String> fileType) {
        if (this.tree == null) {
            this.tree = new TreeNode<>();
        }



        for (int j = 0; j < pdfDataList.size(); j++) {
            fileIndex++;
            PdfData pdfData = pdfDataList.get(j);
            writeTree(" ", "Perimit #", " ", fileIndex);
            writeTree(" ", "File Type", fileType.get(j), fileIndex);

            TreeNode<Object> tempWindowChar = new TreeNode<>();
            TreeNode<Object> tempBuildingParam = new TreeNode<>();
            TreeNode<Object> tempBuildingAssembly = new TreeNode<>();
            TreeNode<Object> tempOccupants = new TreeNode<>("Occupants", true, -1);
            tempOccupants.addChild(new TreeNode<>(pdfData.getOccupants(), false, fileIndex));

            for (Fields field : Fields.values()) {
                switch (field) {
                    case ADULTS, CHILDREN, INFANTS -> {}
                    case INTERIOR_WALL, EXTERIOR_WALL -> {
                        String parentTitle = field.getParent();
                        String typeTitle = field.getTitle();
                        String type = get(field.getFieldName() + "Type", pdfData).toString();
                        String rValueTitle = field.getFieldName().replace("type", "") + "R-Value";
                        String rValue = get(field.getFieldName() + "RValue", pdfData).toString();

                        writeTree(parentTitle, typeTitle, type, fileIndex);
                        writeTree(parentTitle, rValueTitle, rValue, fileIndex);
                    }
                    case WINDOW_CHARACTERISTICS -> {
                        HashMap<String, List<List<String>>> windowCharacteristics = pdfData.getWindowCharacteristics();
                        windowCharacteristics.forEach((orientation, characteristics) -> {
                            String parentTitle = field.getParent() + "-" + orientation;
                            for (int i = 0; i < characteristics.size(); i++) {
                                int num = i + 1;
                                // Area
                                String areaTitle = orientation + " " + num + " Total Area (m^2)"; // e.g. South 01 Total Area (m^2)
                                String areaValue = characteristics.get(i).get(0);
                                writeTree(tempWindowChar, parentTitle, areaTitle, areaValue, fileIndex);
                                // RSI
                                String rsiTitle = orientation + " " + num + "Window RSI"; // e.g. South 01 Window RSI
                                String rsiValue = characteristics.get(i).get(1);
                                writeTree(tempWindowChar, parentTitle, rsiTitle, rsiValue, fileIndex);
                                // SHGC
                                String shgcTitle = orientation + " " + num + "SHGC"; // e.g. South 01 SHGC
                                String shgcValue = characteristics.get(i).get(2);
                                writeTree(tempWindowChar, parentTitle, shgcTitle, shgcValue, fileIndex);
                            }
                        });
                    }
                    case CEILING_COMPONENTS, MAIN_WALL_COMPONENTS, EXPOSED_FLOORS, DOORS -> {
                        List<List<String>> components = (List<List<String>>) get(field.getFieldName(), pdfData);
                        String parentTitle = field.getParent();
                        if (components != null) {
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
                    }
                    case BUILDING_ASSEMBLY_DETAILS -> {
                        HashMap<String, List<String>> assemblyDetails = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
                        assemblyDetails.forEach((component, rsis) -> {
                            String parentTitle = field.getParent() + "-" + component;
                            for (int i = 0; i < rsis.size(); i++) {
                                int num = i + 1;
                                String rsiTitle = component + " " + num + " Effective (RSI)"; // e.g. Ceiling 01 Effective (RSI)
                                String rsiValue = rsis.get(i);
                                writeTree(tempBuildingAssembly, parentTitle, rsiTitle, rsiValue, fileIndex);
                            }
                        });
                    }
                    case BUILDING_PARAMETERS_ZONE_1, BUILDING_PARAMETERS_ZONE_2 -> {
                        HashMap<String, List<String>> parameters = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
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

                    }
                    case VENTILATION_REQUIREMENTS -> {
                        HashMap<String, String> requirements = (HashMap<String, String>) get(field.getFieldName(), pdfData);
                        String parentTitle = field.getParent();
                        requirements.forEach((rooms, value) -> {
                            writeTree(parentTitle, rooms, value, fileIndex);
                        });
                    }
                    case AIR_LEAKAGE_MECHANICAL_VENTILATION -> {
                        List<String> airLeakage = (List<String>) get(field.getFieldName(), pdfData);
                        String parentTitle = field.getParent();
                        // Volume
                        String volumeTitle = "House Volume (m^3)";
                        String volumeValue = airLeakage.get(0);
                        writeTree(parentTitle, volumeTitle, volumeValue, fileIndex);
                        // MJ
                        String mjTitle = "Heat Loss (MJ)";
                        String mjValue = airLeakage.get(1);
                        writeTree(parentTitle, mjTitle, mjValue, fileIndex);
                    }
                    case SPACE_HEATING_SYSTEM_PERFORMANCE -> {
                        HashMap<String, List<String>> unsortedMap = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
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
                    }
                    case MONTHLY_ESTIMATED_ENERGY_CONSUMPTION_BY_DEVICE -> {
                        HashMap<String, List<String>> unsortedMap = (HashMap<String, List<String>>) get(field.getFieldName(), pdfData);
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
                    }
                    default -> {
                        String parentTitle = field.getParent();
                        String title = field.getTitle();
                        Object value = get(field.getFieldName(), pdfData);
                        writeTree(parentTitle, title, value, fileIndex);
                    }
                }
            }

            // add the occupants
//            if (!tree.containsData("OCCUPANTS")) {
//
//            }
            tree.getNode("GENERAL HOUSE CHARACTERISTICS").insert(tempOccupants,
                    tree.getNode("GENERAL HOUSE CHARACTERISTICS").getIndex("Year House Built") + 1);

            // add the window characteristics
            for (TreeNode<Object> child : tempWindowChar.getChildren()) {
                tree.insert(child, tree.getIndex("HOUSE TEMPERATURES") + 1);
            }
            // add the building parameters
            for (TreeNode<Object> child : tempBuildingParam.getChildren()) {
                tree.insert(child, tree.getIndex("Above grade fraction of") + 1);
            }
            // add the building assembly details
            for (TreeNode<Object> child : tempBuildingParam.getChildren()) {
                tree.insert(child, tree.getIndex("Foundations") + 1);
            }
        }



    }
//    @Override
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
