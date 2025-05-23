package service;

import app.ProgressDialog;
import lombok.Setter;
import model.Fields;
import model.PdfData;
import utils.ConversionType;
import lombok.Getter;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import readers.PdfReader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static imgprocessor.PdfImageProcessor.convert;

public class DataExtractor {
    private final float confThreshold = 80f;
    private boolean MultipleFiles;
    private int secondFileStartPage;
    private boolean SIUnit;
    @Getter
    private List<String> content1 = new ArrayList<>();

    private List<Float> content1_confidence = new ArrayList<>();
    @Getter
    private List<String> content2 = new ArrayList<>();

    private List<Float> content2_confidence = new ArrayList<>();
    @Getter @Setter
    private List<String> lowConfContent = new CopyOnWriteArrayList<>();


    public boolean isMultipleFiles() {
        return MultipleFiles;
    }

    public boolean isSIUnit() {
        return SIUnit;
    }



    public void extractData(File file, ProgressDialog dialog, AtomicInteger progress, int numFiles) {
        Map<Integer, BufferedImage> imgs;
        try {
            imgs = convert(file, dialog, progress, numFiles);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PdfReader pdfReader1 = new PdfReader();
        try {
            pdfReader1.setAllContent(imgs, dialog, progress, numFiles);
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }

        ConcurrentHashMap<Integer, List<Word>> pagesWithInfo = pdfReader1.getPagesWithInfo();

        // check if there are multiple files
        for (Word w : pagesWithInfo.get(0)) {
            String s = w.getText();
            if (s.contains("Page") || s.contains("page")) {
                Pattern pattern = Pattern.compile("\\d{2}");
                Matcher matcher = pattern.matcher(s);
                if (matcher.find()) {
                    int page = Integer.parseInt(matcher.group());
                    this.MultipleFiles = (page != imgs.size());
                    this.secondFileStartPage = page;
                    break;
                }
            }
        }

        // Clear the images from memory
        imgs = null;

        if (this.MultipleFiles) {
            for (int i = this.secondFileStartPage; i < pagesWithInfo.size(); i++) {
                List<Word> words = pagesWithInfo.get(i);
                for (Word w : words) {
                    if (!w.getText().isEmpty()) {
                        this.content2.add(w.getText());
                        this.content2_confidence.add(w.getConfidence());
                    }
                }
            }
        }

        for (int i = 0; i < this.secondFileStartPage; i++) {
            List<Word> words = pagesWithInfo.get(i);
            for (Word w : words) {
                if (!w.getText().isEmpty()) {
                    this.content1.add(w.getText());
                    this.content1_confidence.add(w.getConfidence());
                }
            }
        }

        // clean pagesWithInfo from memory
        pagesWithInfo = null;

        for (int i = this.content1.size() - 1; i >= 0; i--) {
            String s = this.content1.get(i);
            if (s.contains("MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE")) {
                Pattern pattern = Pattern.compile("MJ");
                Matcher matcher = pattern.matcher(s);
                this.SIUnit = matcher.find();
                break;
            }
        }
    }

    public PdfData processData1(DataExtractor extractor) throws Exception{
        return processData(extractor.getContent1(), this.content1_confidence);
    }

    public PdfData processData2(DataExtractor extractor) throws Exception{
        return processData(extractor.getContent2(), this.content2_confidence);
    }

    public PdfData processData(List<String> content, List<Float> content_confidence) {
        PdfData data = new PdfData();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        ConcurrentHashMap<String, String> occupants = new ConcurrentHashMap<>();

        for (Fields field : Fields.values()) {
            executor.submit(() -> {
                    switch (field) {
                        case FILE -> {
                            StringBuilder sb = new StringBuilder();
                            boolean foundSectionStart = false;

                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains("File:")) {
                                    foundSectionStart = true;
                                }
                                if (foundSectionStart) {
                                    if (line.contains("Weather Library:")) break;
                                    sb.append(line.replace("File:", "").replace("\n", " ").stripLeading());
                                }

                            }
                            data.setFile(sb.toString());
                        }
                        case OCCUPANTS -> {}
                        case ADULTS, CHILDREN, INFANTS-> {
                            for (String line : content) {
                                if (line.contains(field.getKeyword())) {
                                    String s = line.replace("Occupants :", "")
                                            .replace("\n", "").stripLeading();
                                    occupants.put(String.valueOf(field), s + "; ");
                                    break;
                                }
                            }
                        }
                        case DAYTIME_SETPOINT, NIGHTIME_SETPOINT, BASEMENT_SETPOINT -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                Pattern temp = Pattern.compile("\\d+\\.\\d+");
                                if (line.contains(field.getKeyword())) {
                                    Matcher matcher = temp.matcher(line);
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    if (matcher.find()) {
                                        try {
                                            String fieldName = field.getFieldName().substring(0, 1).toUpperCase() + field.getFieldName().substring(1);
                                            Method setter = PdfData.class.getMethod("set" + fieldName, String.class);
                                            float value = isSIUnit() ?
                                                    Float.parseFloat(matcher.group()) :
                                                    ConversionType.TEMPERATURE.convert(Float.parseFloat(matcher.group()));
                                            setter.invoke(data, String.format("%.1f", value));
                                        } catch (NoSuchMethodException | InvocationTargetException |
                                                 IllegalAccessException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }
                                }
                            }
                        }
                        case NIGHTIME_SETBACK_DURATION -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    String value = line.replace(field.getKeyword(), "").replace("\n", "").stripLeading();

                                    data.setNightimeSetbackDuration(value.replace("Duration:", "").stripLeading());
                                    break;
                                }
                            }
                        }
                        case TEMP_RISE_FROM -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                Pattern temp = Pattern.compile("\\d+(\\.\\d+?)?");
                                if (line.contains(field.getKeyword())) {
                                    Matcher matcher = temp.matcher(line);
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    if (matcher.find()) {
                                        String[] parts = line.split("\\s");

                                        for (int j = parts.length - 1; j >= 0; j--) {
                                            Matcher tempMatcher = temp.matcher(parts[j]);
                                            if (tempMatcher.find()) {
                                                float v = tempMatcher.group().contains(".") ?
                                                        Float.parseFloat(tempMatcher.group()) :
                                                        Float.parseFloat(tempMatcher.group()) / 10f;
                                                float value = isSIUnit() ?
                                                        v : ConversionType.TEMPERATURE.convert(v);
                                                data.setTempRiseFrom(String.format("%.1f", value));
                                                break;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        case WINDOW_CHARACTERISTICS -> {
                            HashMap<String, List<List<String>>> windowCharacteristics = new HashMap<>();
                            boolean foundSectionStart = false;
                            String currentOrientation = null;
                            Pattern valuePattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            Pattern orientationPattern = Pattern.compile("^(South|East|West|North|Southeast|Northeast|Northwest|Southwest)(\n)?$");

                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains("SHGC ER")) {
                                    foundSectionStart = true;
                                    continue;
                                }
                                if (foundSectionStart) {
                                    if (line.contains("ER Window Energy Rating")) break;
                                    Matcher orientationMatcher = orientationPattern.matcher(line);


                                    if (orientationMatcher.find()) {
                                        currentOrientation = orientationMatcher.group(1);
                                        windowCharacteristics.putIfAbsent(currentOrientation, new ArrayList<>());
                                    } else {
                                        List<String> dataLine = new ArrayList<>();
                                        Matcher valueMatcher = valuePattern.matcher(line);
                                        if (valueMatcher.find()) {
                                            Pattern numPattern = Pattern.compile("\\d+");
                                            String[] parts = line.split("\\s");
                                            if (parts.length < 5) continue;

                                            int length = parts.length - 1;

                                            Matcher numMatcher = numPattern.matcher(parts[length - 1]);
                                            length = numMatcher.find() ? length - 1 : length - 2;
                                            float shgc = Float.parseFloat(parts[length]);

                                            numMatcher = numPattern.matcher(parts[length - 1]);
                                            length = numMatcher.find() ? length - 1 : length - 2;
                                            float rsi = isSIUnit() ?
                                                    Float.parseFloat(parts[length]) :
                                                    ConversionType.THERMAL_RESISTANCE.convert(Float.parseFloat(parts[length]));

                                            numMatcher = numPattern.matcher(parts[length - 1]);
                                            length = numMatcher.find() ? length - 1 : length - 2;
                                            float area = isSIUnit() ?
                                                    Float.parseFloat(parts[length]) :
                                                    ConversionType.AREA.convert(Float.parseFloat(parts[length]));
                                            dataLine.add(String.format("%.2f", area));
                                            dataLine.add(String.format("%.3f", rsi));
                                            dataLine.add(String.format("%.4f", shgc));

                                        }
                                        // Only add data if we have the current orientation
                                        if (currentOrientation != null && !dataLine.isEmpty()) {
                                            windowCharacteristics.get(currentOrientation).add(dataLine);
                                        }
                                    }
                                }
                            }
                            data.setWindowCharacteristics(windowCharacteristics);
                            // clean memory
                            windowCharacteristics = null;
                        }
                        case ABOVE_GRADE_FRACTION -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s?%");
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        data.setAboveGradeFraction(matcher.group());
                                    }
                                }
                            }
                        }
                        case CEILING_COMPONENTS -> {
                            String start = "CEILING COMPONENTS";
                            String end = "MAIN WALL COMPONENTS";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern, 6);
                            data.setCeilingComponents(value);
                            // clean memory
                            value = null;
                        }
                        case MAIN_WALL_COMPONENTS -> {
                            String start = "MAIN WALL COMPONENTS";
                            String end = "EXPOSED FLOORS";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern, 7);
                            data.setMainWallComponents(value);
                            // clean memory
                            value = null;
                        }
                        case EXPOSED_FLOORS -> {
                            String start = "EXPOSED FLOORS";
                            String end = "EXPOSED FLOOR SCHEDULE";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s(\\d+(\\.\\d+)?|>)");
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern, 3);
                            data.setExposedFloors(value);
                            // clean memory
                            value = null;
                        }
                        case DOORS -> {
                            String start = "DOORS";
                            String end = "FOUNDATIONS";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern, 5);
                            data.setDoors(value);
                            // clean memory
                            value = null;
                        }
                        case INTERIOR_WALL, EXTERIOR_WALL -> processWallData(field, content, content_confidence, data);
                        case ADDED_TO_SLAB -> {
                            List<String> addToSlabRvalueList = new ArrayList<>();
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.toLowerCase().contains(field.getKeyword().toLowerCase())) {
                                    if (line.contains("FOUNDATION CODE SCHEDULE") || line.contains("BUILDING ASSEMBLY DETAILS")) break;
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
                                    findRValue(addToSlabRvalueList, line, pattern);
                                }
                            }
                            data.setAddedToSlab(addToSlabRvalueList);
                        }
                        case FLOORS_ABOVE_FOUND -> {
                            List<String> rValueList = new ArrayList<>();
                            for (int i = 1; i < content.size(); i++) {
                                String s = content.get(i);
                                if (s.contains("FOUNDATION CODE SCHEDULE") || s.contains("BUILDING ASSEMBLY DETAILS")) break;
                                if (s.matches("Found\\.:") || s.matches("Found\\.:\n")) {
                                    String line = content.get(i - 1);
                                    if (line.contains("Page") || line.contains("page")) {
                                        line = content.get(i - 2);
                                        if (content_confidence.get(i - 2) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                                    }
                                    if (content_confidence.get(i - 1) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                                    Pattern pattern = Pattern.compile("(\\d+(\\.\\d{2})?)");
                                    findRValue(rValueList, line, pattern);
                                }
                            }
                            data.setFloorsAboveFound(rValueList);
                        }
                        case BUILDING_ASSEMBLY_DETAILS -> {
                            HashMap<String, List<String>> buildingAssemblyDetails = new HashMap<>();
                            String currentComponent = null;
                            Pattern componentsPattern = Pattern.compile("^(MAIN WALL( COMPONENTS)?|CEILING COMPONENTS|EXPOSED FLOORS|FLOORS ABOVE)(\n)?$");
                            boolean foundSectionStart = false;

                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains("BUILDING ASSEMBLY DETAILS")) {
                                    foundSectionStart = true;
                                    continue;
                                }
                                if (foundSectionStart) {
                                    if (line.contains("BUILDING PARAMETERS SUMMARY")) break;
                                    Matcher componentsMatcher = componentsPattern.matcher(line);
                                    if (componentsMatcher.find()) {
                                        currentComponent = componentsMatcher.group(1);
                                        // add COMPONENTS back if pattern didn't match it
                                        if (currentComponent.equals("MAIN WALL")) currentComponent = "MAIN WALL COMPONENTS";
                                        buildingAssemblyDetails.putIfAbsent(currentComponent, new ArrayList<>());
                                    } else {
                                        Pattern valuePattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                                        Matcher valueMatcher = valuePattern.matcher(line);
                                        if (valueMatcher.find()) {
                                            if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " " + line);
                                            String[] values = line.split("\\s");
                                            float rsi = isSIUnit() ?
                                                    Float.parseFloat(values[values.length - 1]) :
                                                    ConversionType.THERMAL_RESISTANCE.convert(Float.parseFloat(values[values.length - 1]));
                                            buildingAssemblyDetails.get(currentComponent).add(String.format("%.2f", rsi));
                                        }
                                    }
                                }
                            }
                            data.setBuildingAssemblyDetails(buildingAssemblyDetails);
                            // clean memory
                            buildingAssemblyDetails = null;
                        }
                        case BUILDING_PARAMETERS_ZONE_1 -> {
                            String start = "ZONE 1 : Above Grade";
                            String end = "ZONE 1 Totals";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            HashMap<String, List<String>> buildingParametersZone = extractBuildingParameters(content, content_confidence, start, end, pattern);

                            data.setBuildingParametersZone1(buildingParametersZone);
                            // clean memory
                            buildingParametersZone = null;
                        }
                        case BUILDING_PARAMETERS_ZONE_2 -> {
                            String start = "ZONE 2 : Basement";
                            String end = "ZONE 2 Totals";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            HashMap<String, List<String>> buildingParametersZone = extractBuildingParameters(content, content_confidence, start, end, pattern);

                            data.setBuildingParametersZone2(buildingParametersZone);
                            // clean memory
                            buildingParametersZone = null;
                        }
                        case AIR_LEAKAGE_MECHANICAL_VENTILATION -> {
                            boolean foundSectionStart = false;

                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.matches("Air Leakage and Mechanical Ventilation")
                                        || line.matches("Air Leakage and Mechanical Ventilation\n") ) {
                                    foundSectionStart = true;
                                    continue;
                                }
                                if (foundSectionStart) {
                                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?(\\s)?ACH");
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " " + line);
                                        String[] values = line.split("\\s");
                                        float volume = isSIUnit() ?
                                                Float.parseFloat(values[0]) :
                                                ConversionType.VOLUME_M3.convert(Float.parseFloat(values[0]));
                                        float energy = isSIUnit() ?
                                                Float.parseFloat(values[values.length - 2]) :
                                                ConversionType.ENERGY.convert(Float.parseFloat(values[values.length - 2]));
                                        data.setAirLeakageMechanicalVentilation(Arrays.asList(
                                                String.format("%.2f", volume), String.format("%.3f", energy)));
                                        break;
                                    }
                                }
                            }
                        }
                        case VENTILATION_REQUIREMENTS -> {
                            LinkedHashMap<String, String> roomInfo = new LinkedHashMap<>();
                            boolean foundSectionStart = false;

                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains("F326 VENTILATION REQUIREMENTS")) {
                                    foundSectionStart = true;
                                    continue;
                                }
                                if (foundSectionStart) {
                                    if (line.contains("CENTRAL VENTILATION SYSTEM") || line.contains("SECONDARY FANS & OTHER EXHAUST APPLIANCES")) break;
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " " + line);
                                    Pattern pattern = Pattern.compile("^(.*?)\\s(\\d+)\\s?rooms\\s?@\\s?(\\d+\\.\\d+)");
                                    Matcher matcher = pattern.matcher(line);
                                    if (line.contains("Basement Rooms")) {
                                        Pattern pattern1 = Pattern.compile("\\d+(\\.\\d+)?");
                                        Matcher matcher1 = pattern1.matcher(line);
                                        if (matcher1.find()) {
                                            float value = isSIUnit() ?
                                                    Float.parseFloat(matcher1.group()) :
                                                    ConversionType.VOLUME_FLOW_RATE.convert(Float.parseFloat(matcher1.group()));
                                            String s1 = String.format("%.1f", value) + " L/s";
                                            roomInfo.put("Basement Rooms", s1);
                                            break;
                                        }
                                    } else {
                                        if (matcher.find()) {
                                            String roomType = matcher.group(1);
                                            String numberOfRooms = matcher.group(2);
                                            float value = isSIUnit() ?
                                                    Float.parseFloat(matcher.group(3)) :
                                                    ConversionType.VOLUME_FLOW_RATE.convert(Float.parseFloat(matcher.group(3)));
                                            float total = value * Integer.parseInt(numberOfRooms);
                                            String s = numberOfRooms + " rooms @ " + String.format("%.1f", value) + " L/s:"
                                                    + String.format("%.1f", total) + " L/s";

                                            // Check for duplicate room types and append a number if necessary
                                            if (roomInfo.containsKey(roomType)) {
                                                int duplicateCount = 2;
                                                while (roomInfo.containsKey(roomType + duplicateCount)) {
                                                    duplicateCount++;
                                                }
                                                roomType = roomType + " " +duplicateCount;
                                            }
                                            roomInfo.put(roomType, s);
                                        }
                                    }
                                }
                            }
                            data.setVentilationRequirements(roomInfo);
                            // clean memory
                            roomInfo = null;
                        }
                        case SYSTEM_TYPE -> {
                            boolean foundSectionStart = false;

                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains("CENTRAL VENTILATION SYSTEM")) {
                                    foundSectionStart = true;
                                    continue;
                                }
                                if (foundSectionStart) {
                                    if (line.contains(field.getKeyword())) {
                                        if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                        String value = line.replace(field.getKeyword(), "").replace("\n", "").stripLeading();
                                        data.setSystemType(value);
                                        break;
                                    }
                                }
                            }
                        }
                        case FP_POWER_0 -> {
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s?(Watts|watts)");
                            String keywordImperial = "Fan and Preheater Power at 32";
                            findPowerAndEfficiency(content, content_confidence, field, keywordImperial, data, pattern);
                        }
                        case FP_POWER_MINUS_25 -> {
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s?(Watts|watts)");
                            String keywordImperial = "Fan and Preheater Power at -13";
                            findPowerAndEfficiency(content, content_confidence, field, keywordImperial, data, pattern);
                        }
                        case HEAT_RE_EFFICIENCY_0 -> {
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s?%");
                            String keywordImperial = "Sensible Heat Recovery Efficiency at 32";
                            findPowerAndEfficiency(content, content_confidence, field, keywordImperial, data, pattern);
                        }
                        case HEAT_RE_EFFICIENCY_MINUS_25 -> {
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s?%");
                            String keywordImperial = "Fan and Preheater Power at -";
                            findPowerAndEfficiency(content, content_confidence, field, keywordImperial, data, pattern);
                        }
                        case EST_VENT_LOAD_HEATING -> {
                            String k1 = "Heating Hours:";
                            String value = findVentEload(content, k1);
                            data.setEstVentLoadHeating(value);
                        }
                        case EST_VENT_LOAD_NON_HEATING -> {
                            String k1 = "Non-Heating Hours:";
                            String value = findVentEload(content, k1);
                            data.setEstVentLoadNonHeating(value);
                        }
                        case GROSS_AIR_LEAKAGE, NET_AIR_LEAKAGE, ESTIMATED_DOMESTIC_WATER_HEATING_LOAD,
                                PRIMARY_DOMESTIC_WATER_HEATING_LOAD_CONSUMPTION, GROSS_SPACE_HEAT_LOSS,
                                GROSS_SPACE_HEATING_LOAD, USABLE_INTERNAL_GAINS, USABLE_SOLAR_GAINS,
                                AUXILARY_ENERGY_REQUIRED, SPACE_HEATING_SYSTEM_LOAD,
                                FURNACE_BOILER_ANNUAL_ENERGY_CONSUMPTION, HEAT_PUMP_ANN_ENERGY_CONSUMPTION-> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        float value = isSIUnit() ?
                                                Float.parseFloat(matcher.group()) :
                                                ConversionType.ENERGY.convert(Float.parseFloat(matcher.group()));
                                        try {
                                            String fieldName = field.getFieldName().substring(0, 1).toUpperCase() + field.getFieldName().substring(1);
                                            Method setter = PdfData.class.getMethod("set" + fieldName, String.class);
                                            setter.invoke(data, String.format("%.3f", value));
                                            break;
                                        } catch (NoSuchMethodException | InvocationTargetException |
                                                 IllegalAccessException e) {
                                            break;
                                        }
                                    }
                                }
                            }

                        }
                        case ANNUAL_SPACE_HETING_ENERGY -> {
                            Pattern pattern = Pattern.compile("^Annual Space Heating Energy");
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (pattern.matcher(line).find()) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    Pattern valuePattern = Pattern.compile("\\d+(\\.\\d+)?");
                                    Matcher valueMatcher = valuePattern.matcher(line);
                                    if (valueMatcher.find()) {
                                        float value = isSIUnit() ?
                                                Float.parseFloat(valueMatcher.group()) :
                                                ConversionType.ENERGY.convert(Float.parseFloat(valueMatcher.group()));
                                        data.setAnnualSpaceHeatingEnergyConsumption(String.format("%.3f", value));
                                        break;
                                    }
                                }
                            }
                        }
                        case DAILY_HOT_WATER_CONSUMPTION -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        float value = isSIUnit() ?
                                                Float.parseFloat(matcher.group()) :
                                                ConversionType.VOLUME_LITRE.convert(Float.parseFloat(matcher.group()));
                                        data.setDailyHotWaterConsumption(String.format("%.1f", value));
                                        break;
                                    }
                                }
                            }
                        }
                        case HOT_WATER_TEMPERATURE -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        float value = isSIUnit() ?
                                                Float.parseFloat(matcher.group()) :
                                                ConversionType.TEMPERATURE.convert(Float.parseFloat(matcher.group()));
                                        data.setHotWaterTemperature(String.format("%.1f",value));
                                        break;
                                    }
                                }
                            }
                        }
                        case DESIGN_HEAT_LOSS, DESIGN_COOLING_LOAD -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    String[] parts = line.split("\\s");
                                    Pattern pattern = Pattern.compile("\\d+");
                                    for (int j = parts.length - 1; j >= 0; j--) {
                                        Matcher matcher = pattern.matcher(parts[j]);
                                        if (matcher.find()) {
                                            float value = isSIUnit() ?
                                                    Float.parseFloat(matcher.group()) :
                                                    ConversionType.POWER.convert(Float.parseFloat(matcher.group()));
                                            try {
                                                String fieldName = field.getFieldName().substring(0, 1).toUpperCase() + field.getFieldName().substring(1);
                                                Method setter = PdfData.class.getMethod("set" + fieldName, String.class);
                                                setter.invoke(data, String.format("%.1f", value));
                                                break;
                                            } catch (NoSuchMethodException | InvocationTargetException |
                                                    IllegalAccessException e) {
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }

                        }
                        case WATER_HEATING_EQUIPMENT -> {
                            for (int i = 1; i < content.size(); i++) {
                                String s1 = content.get(i-1);
                                if (s1.contains("Page") || s1.contains("page")) {
                                    s1 = content.get(i-2);
                                }
                                String s2 = content.get(i);
                                if (s2.contains("Equipment:") && s1.contains("Water Heating")) {
                                    String s = s1.replace("Water Heating", "").replace("\n", "")
                                            + s2.replace("Equipment:", "").replace("\n", "");
                                    data.setWaterHeatingEquipment(s.stripLeading());
                                    break;
                                }
                            }
                        }
                        case SPACE_HEATING_SYSTEM_PERFORMANCE -> {
                            String start = "SPACE HEATING SYSTEM PERFORMANCE";
                            String end = "Ann";
                            Pattern pattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Ann)");
                            List<String> list = findAnnualTable(content, start, end, pattern);
                            HashMap<String, List<String>> monthlyData = new HashMap<>();

                            for (String line : list) {
                                String[] parts = line.split("\\s+");
                                if (parts.length < 3) continue; // Skip if there are not enough parts

                                String month = parts[0];
                                try {
                                    // check if OCR does not detect the decimal point,
                                    // if the next part is a single digit, then combine the two parts
                                    // if the next part is not a single digit, then the current part is a whole number
                                    // SI unit contains 1 decimal place, imperial unit contains 3 decimal places
                                    int decimalPlaces = isSIUnit() ? 1 : 3;
                                    int i = 1;
                                    // if current number does not contain a decimal point, then put it into lowConfContent,
                                    // and check if the next number is a single digit, if so, assume the actual number is separated to two parts, (ie. 123 4)
                                    // or, the next number could be .4 (ie. 123 .4), both cases, combine the two parts to get the actual number,
                                    // if not, assume the current number is a whole number, and divide it by 10^decimalPlaces (ie. 1234)
                                    float heatingLoadValue = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            heatingLoadValue = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            heatingLoadValue = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else {
                                            heatingLoadValue /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Heating Load: OCRed=" + original + " -> predicted=" + heatingLoadValue);
                                    }
                                    i++;
                                    float heatingLoad = isSIUnit() ?
                                            heatingLoadValue : ConversionType.ENERGY.convert(heatingLoadValue);

                                    float furnaceValue = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            furnaceValue = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            furnaceValue = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else {
                                            furnaceValue /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Furnace Input: OCRed=" + original + " -> predicted=" + furnaceValue);
                                    }
                                    float furnaceInput = isSIUnit() ?
                                            furnaceValue : ConversionType.ENERGY.convert(furnaceValue);

                                    float COP = Float.parseFloat(parts[parts.length - 1]);
                                    if (!parts[parts.length - 1].contains(".")) {
                                        String original = parts[parts.length - 1];
                                        if (!parts[parts.length - 2].contains(".") && parts[parts.length - 1].length() == decimalPlaces) {
                                            COP = Float.parseFloat(parts[parts.length - 2] + "." + parts[parts.length - 1]);
                                        } else {
                                            COP /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " COP: OCRed=" + original + " -> predicted=" + COP);
                                    } else if (parts[parts.length - 1].matches("^\\.\\d{" + decimalPlaces + "}$") && !parts[parts.length - 2].contains(".")) {
                                        COP = Float.parseFloat(parts[parts.length - 2] + parts[parts.length - 1]);
                                        lowConfContent.add(field.getTitle() + " " + month + " COP: OCRed=" + parts[parts.length - 1] + " -> predicted=" + COP);
                                    }

                                    List<String> values = List.of(String.format("%.1f", heatingLoad),
                                            String.format("%.1f", furnaceInput), String.format("%.3f", COP));
                                    monthlyData.put(month, values);
                                } catch (NumberFormatException e) {
                                    System.err.println("Error parsing numbers from line: " + line);
                                }
                                if (month.equals("Ann")) break;
                            }
                            data.setSpaceHeatingSystemPerformance(monthlyData);
                            // clean memory
                            list = null;
                            monthlyData = null;
                        }
                        case MONTHLY_ESTIMATED_ENERGY_CONSUMPTION_BY_DEVICE -> {
                            String start = "MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE";
                            String end = "Ann";
                            Pattern pattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Ann)");
                            List<String> list = findAnnualTable(content, start, end, pattern);
                            HashMap<String, List<String>> monthlyData = new HashMap<>();

                            for (String line : list) {
                                String[] parts = line.split("\\s+");
                                if (parts.length < 7) continue; // Skip if there are not enough parts

                                String month = parts[0];
                                try {
                                    // check if OCR does not detect the decimal point,
                                    // if the next part is a single digit, then combine the two parts
                                    // if the next part is not a single digit, then the current part is a whole number
                                    // SI unit contains 1 decimal place, imperial unit contains 3 decimal places
                                    int decimalPlaces = isSIUnit() ? 1 : 3;
                                    int i = 1;
                                    // if current number does not contain a decimal point, then put it into lowConfContent,
                                    // and check if the next number is a single digit, if so, assume the actual number is separated to two parts, (ie. 123 4)
                                    // or, the next number could be .4 (ie. 123 .4), both cases, combine the two parts to get the actual number,
                                    // if not, assume the current number is a whole number, and divide it by 10^decimalPlaces (ie. 1234)
                                    float value1 = Float.parseFloat(parts[i]);
                                    float conf1 = content_confidence.get(i);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) { // case 123 4
                                            value1 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) { // case 123 .4
                                            value1 = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else { // case 1234
                                            value1 /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Space Heating Primary: OCRed=" + original + " -> predicted=" + value1);
                                    }
                                    i++;
                                    float spaceHeatingPrimary = isSIUnit() ?
                                            value1 : ConversionType.ENERGY.convert(value1);

                                    float value2 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value2 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            value2 = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else {
                                            value2 /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Space Heating Secondary: OCRed=" + original + " -> predicted=" + value2);
                                    }
                                    i++;
                                    float spaceHeatingSecondary = isSIUnit() ?
                                            value2 : ConversionType.ENERGY.convert(value2);

                                    float value3 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value3 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            value3 = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else {
                                            value3 /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " DHW Primary: OCRed=" + original + " -> predicted=" + value3);
                                    }
                                    i++;
                                    float DHWPrimary = isSIUnit() ?
                                            value3 : ConversionType.ENERGY.convert(value3);

                                    float value4 = Float.parseFloat(parts[i]);
                                    if(!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value4 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            value4 = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else {
                                            value4 /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " DHW Secondary: OCRed=" + original + " -> predicted=" + value4);
                                    }
                                    i++;
                                    float DHWSecondary = isSIUnit() ?
                                            value4 : ConversionType.ENERGY.convert(value4);

                                    float value5 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value5 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            value5 = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else {
                                            value5 /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Lights: OCRed=" + original + " -> predicted=" + value5);
                                    }
                                    i++;
                                    float Lights = isSIUnit() ?
                                            value5 : ConversionType.ENERGY.convert(value5);

                                    float value6 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value6 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            value6 = Float.parseFloat(parts[i] + parts[i+1]);
                                            i++;
                                        } else {
                                            value6 /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " HRV: OCRed=" + original + " -> predicted=" + value6);
                                    }
                                    i++;
                                    float hrv = isSIUnit() ?
                                            value6 : ConversionType.ENERGY.convert(value6);

                                    float value7 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value7 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                        } else if (parts[i+1].matches("^\\.\\d{" + decimalPlaces + "}$")) {
                                            value7 = Float.parseFloat(parts[i] + parts[i+1]);
                                        } else {
                                            value7 /= (float) Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " AC: OCRed=" + original + " -> predicted=" + value7);
                                    }
                                    float ac = isSIUnit() ?
                                            value7 : ConversionType.ENERGY.convert(value7);

                                    List<String> values = List.of(String.format("%.1f", spaceHeatingPrimary), String.format("%.1f", spaceHeatingSecondary),
                                            String.format("%.1f", DHWPrimary), String.format("%.1f", DHWSecondary),
                                            String.format("%.1f", Lights), String.format("%.1f", hrv), String.format("%.1f", ac));

                                    monthlyData.put(month, values);
                                } catch (NumberFormatException e) {
                                    System.err.println("Error parsing numbers from line: " + line);
                                }
                                if (month.equals("Ann")) break;
                            }
                            data.setMonthlyEstimatedEnergyConsumptionByDevice(monthlyData);
                            // clean memory
                            monthlyData = null;
                            list = null;
                        }
                        case PRIMARY_HEATING_FUEL -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                Matcher matcher = Pattern.compile("PRIMARY Heating Fuel|PRIMARY Space Heating Fuel").matcher(line);
                                if (matcher.find()) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    Matcher matcher1 = Pattern.compile("Heating Fuel:(.*)").matcher(line);
                                    if (matcher1.find()) {
                                        String value = matcher1.group(1).replace("\n", "").stripLeading();
                                        data.setPrimaryHeatingFuel(value);
                                        break;
                                    }
                                }
                            }
                        }
                        /*
                        Air Leakage Test Results, Air Distribution/circulation type,
                        Air Distribution/circulation fan power, Operation schedule, Seasonal Heat Recovery Ventilator,
                         AFUE, High Speed Fan Power, PRIMARY Water Heating, Energy Factor, Heat Pump and Furnace Annual COP,
                        Primary System Seasonal Efficiency, Usable Internal Gains Fraction, Usable Solar Gains Fraction,
                        Furnace/Boiler Seasonal efficiency
                         */
                        default -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle());
                                    String value = line.replace(field.getKeyword(), "").replace("\n", "").stripLeading();
                                    try {
                                        String fieldName = field.getFieldName().substring(0, 1).toUpperCase() + field.getFieldName().substring(1);
                                        Method setter = PdfData.class.getMethod("set" + fieldName, String.class);
                                        setter.invoke(data, value);
                                        break;
                                    } catch (NoSuchMethodException | InvocationTargetException |
                                             IllegalAccessException e) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
            });
        }

        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // Wait for all tasks to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        data.setOccupants(occupants);
        if (occupants.size() < 3) {
            lowConfContent.add("Occupants");
        }
        return data;
    }

    private void findRValue(List<String> rValueList, String line, Pattern pattern) {

        String[] parts = line.split("\\s");
        for (int j = parts.length - 1; j >= 0; j--) {
            Matcher matcher = pattern.matcher(parts[j]);
            if (matcher.find()) {
                Matcher decimalMatcher = Pattern.compile("\\d+\\.\\d{2}").matcher(matcher.group());
                float originalRSI = decimalMatcher.find() ?
                        Float.parseFloat(matcher.group()) :
                        Float.parseFloat(matcher.group()) / 100f;
                float rsi = isSIUnit() ?
                        originalRSI :
                        ConversionType.THERMAL_RESISTANCE.convert(originalRSI);
                rValueList.add(String.format("%.2f", rsi));
                break;
            }
        }
    }

    private void processWallData(Fields field, List<String> content, List<Float> content_confidence, PdfData data) {
        List<String> typeList = new ArrayList<>();
        List<String> rValueList = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.contains(field.getKeyword())) {
                if (line.contains("FOUNDATION CODE SCHEDULE") || line.contains("BUILDING ASSEMBLY DETAILS")) break;
                if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                Pattern typePattern = Pattern.compile("type:(.*?)R-(value|Value)");
                Matcher typeMatcher = typePattern.matcher(line);
                if (typeMatcher.find()) {
                    typeList.add(typeMatcher.group(1));
                }
                Pattern rsiPattern = Pattern.compile("\\d+(\\.\\d+)?");
                // R-VALUE
                findRValue(rValueList, line, rsiPattern);
            }
        }
        if (field == Fields.INTERIOR_WALL) {
            data.setInteriorWallType(typeList);
            data.setInteriorWallRValue(rValueList);
        } else if (field == Fields.EXTERIOR_WALL) {
            data.setExteriorWallType(typeList);
            data.setExteriorWallRValue(rValueList);
        }
    }

    private  List<String> findAnnualTable(List<String> content, String startMarker, String endMarker, Pattern pattern) {
        List<String> list = new ArrayList<>();
        int start = 0;

        for (int i = content.size() - 1; i >= 0; i--) {
            String line = content.get(i);
            if (line.contains(startMarker)) {
                start = i;
                break;
            }
        }

        for (int i = start; i < content.size(); i++) {
            String line = content.get(i);

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                list.add(0, line.replace("\n", ""));
            }

            if (line.contains(endMarker)) break;
        }

        Collections.reverse(list);
        return list;
    }


    private String findVentEload(List<String> content, String k1) {
        String k2 = k1 + "\n";
        String s = "";
        float value = 0;
        for (int i = 1; i < content.size(); i++) {
            String s2 = content.get(i);
            if (s2.matches(k1) || s2.matches(k2)) {
                String s1 = content.get(i-1);
                if (s1.contains("Estimated Ventilation Electrical Load:")) {
                    s = s1;
                    break;
                }
            }
        }

        Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            value = isSIUnit() ?
                    Float.parseFloat(matcher.group()) :
                    ConversionType.ENERGY.convert(Float.parseFloat(matcher.group()));
        }
        return String.valueOf(value);
    }

    private void findPowerAndEfficiency(List<String> content, List<Float> content_confidence, Fields field, String keywordImperial, PdfData data, Pattern pattern) {
        boolean foundSectionStart = false;

        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.contains("CENTRAL VENTILATION SYSTEM")) {
                foundSectionStart = true;
                continue;
            }
            if (foundSectionStart) {
                if (line.contains("Page") || line.contains("page")) continue;
                if (line.contains(field.getKeyword()) || line.contains(keywordImperial)) {
                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(line);
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            String fieldName = field.getFieldName().substring(0, 1).toUpperCase() + field.getFieldName().substring(1);
                            Method setter = PdfData.class.getMethod("set" + fieldName, String.class);
                            setter.invoke(data, matcher.group());
                            break;
                        } catch (NoSuchMethodException | InvocationTargetException |
                                 IllegalAccessException e) {
                            break;
                        }
                    }
                }
            }

        }
    }

    private HashMap<String, List<String>> extractBuildingParameters(List<String> content, List<Float> content_confidence, String start,
                                                                    String end, Pattern pattern) {
        HashMap<String, List<String>> buildingParametersZone = new HashMap<>();

        boolean foundSectionStart = false;

        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.contains("Page") || line.contains("page")) continue;
            if (line.contains(start)) {
                foundSectionStart = true;
                continue;
            }
            if (foundSectionStart) {
                if (line.contains(end)) break;

                Matcher valueMatcher = pattern.matcher(line);
                if (valueMatcher.find()) {
                    if (content_confidence.get(i) < confThreshold) lowConfContent.add("BUILDING PARAMETERS SUMMARY " + line);
                    String[] values = line.replace("-","0").replace(":", ".").split("\\s");
                    StringBuilder name = new StringBuilder();
                    Pattern numberPattern = Pattern.compile("\\d+");
                    for (String value : values) {
                        if (!numberPattern.matcher(value).find()) {
                            name.append(value).append(" ");
                        } else break;
                    }
                    if (values.length < 3) continue; // Skip if there are not enough parts
                    float area = 0, rsi = 0, heatLoss = 0;
                    int currLocation = values.length - 1;
                    Matcher numberMatcher = numberPattern.matcher(values[currLocation - 1]);
                    currLocation = numberMatcher.find() ? currLocation - 1 : currLocation - 2;
                    heatLoss = isSIUnit() ?
                            Float.parseFloat(values[currLocation]) :
                            ConversionType.ENERGY.convert(Float.parseFloat(values[currLocation]));

                    numberMatcher = numberPattern.matcher(values[currLocation - 1]);
                    currLocation = numberMatcher.find() ? currLocation - 1 : currLocation - 2;
                    rsi = isSIUnit() ?
                            Float.parseFloat(values[currLocation]) :
                            ConversionType.THERMAL_RESISTANCE.convert(Float.parseFloat(values[currLocation]));

                    numberMatcher = numberPattern.matcher(values[currLocation - 1]);
                    currLocation = numberMatcher.find() ? currLocation - 1 : currLocation - 2;
                    area = isSIUnit() ?
                            Float.parseFloat(values[currLocation]) :
                            ConversionType.AREA.convert(Float.parseFloat(values[currLocation]));

                    buildingParametersZone.put(name.toString(), Arrays.asList(String.format("%.2f", area),
                            String.format("%.2f", rsi), String.format("%.2f", heatLoss)));
                }
            }
        }
        return buildingParametersZone;
    }

    private List<List<String>> extractTableComponents(List<String> content, List<Float> content_confidence, String sectionStart,
                                                     String end, Pattern pattern, int numColumns) {
        List<List<String>> components = new ArrayList<>();
        boolean foundStartMarker = false;

        label:
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.contains("Page") || line.contains("page")) continue;
            if (line.startsWith(sectionStart)) {
                foundStartMarker = true;
                continue;
            }
            if (foundStartMarker) {
                switch (sectionStart) {
                    case "MAIN WALL COMPONENTS" -> {
                        Pattern endPattern = Pattern.compile("(EXPOSED FLOORS|WALL CODE SCHEDULE|Indicates)");
                        Matcher endMatcher = endPattern.matcher(line);
                        if (endMatcher.find()) break label;
                    }
                    case "EXPOSED FLOORS" -> {
                        Pattern endPattern = Pattern.compile("(DOORS|EXPOSED FLOOR SCHEDULE|Indicates)");
                        Matcher endMatcher = endPattern.matcher(line);
                        if (endMatcher.find()) break label;
                    }
                    case "CEILING COMPONENTS" -> {
                        Pattern endPattern = Pattern.compile("(MAIN WALL COMPONENTS|CEILING CODE SCHEDULE|Indicates)");
                        Matcher endMatcher = endPattern.matcher(line);
                        if (endMatcher.find()) break label;
                    }
                    case "DOORS" -> {
                        Pattern endPattern = Pattern.compile("(FOUNDATIONS|DOOR CODE SCHEDULE|Indicates)");
                        Matcher endMatcher = endPattern.matcher(line);
                        if (endMatcher.find()) break label;
                    }
                    default -> {
                        if (line.contains(end)) break label;
                    }
                }

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    Pattern numberPattern1 = Pattern.compile("\\d+.\\d+");
                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(sectionStart + " " + content.get(i));

                    // check if the line contains a value at least
                    if (numberPattern1.matcher(line).find()) {
                        List<String> values = new ArrayList<>(Arrays.asList(line.split("\\s")));
                        if (values.size() < numColumns) continue;

                        // scenarios: 1. >19.1 as a whole number, 2. > and 19.1 are separated in two lists, 3. 19.1 single number
                        Pattern numberPattern = Pattern.compile("\\d+.\\d+");
                        float lastValue = 0;
                        Matcher matcher1 = numberPattern.matcher(values.get(values.size() - 1));
                        if (matcher1.find()) {
                            String lastString = matcher1.group();
                            lastValue = Float.parseFloat(lastString);
                        }

                        float secondLastValue = 0;
                        Matcher matcher2 = numberPattern.matcher(values.get(values.size() - 2));
                        if (matcher2.find()) {
                            secondLastValue = Float.parseFloat(matcher2.group());
                        } else {
                            Matcher matcher3 = numberPattern.matcher(values.get(values.size() - 3));
                            if (matcher3.find()) {
                                secondLastValue = Float.parseFloat(matcher3.group());
                            }
                        }

                        float area = isSIUnit() ?
                                secondLastValue : ConversionType.AREA.convert(secondLastValue);
                        float rsi = isSIUnit() ?
                                lastValue : ConversionType.THERMAL_RESISTANCE.convert(lastValue);

                        components.add(Arrays.asList(String.format("%.2f", area), String.format("%.2f", rsi)));
                    }
                }
            }
        }
        return components;
    }
}
