package service;

import lombok.Setter;
import model.Fields;
import model.PdfData;
import utils.Conversions;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.Conversions.*;
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



    public void extractData(File file) {
        Map<Integer, BufferedImage> imgs;
        try {
            imgs = convert(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PdfReader pdfReader1 = new PdfReader();
        try {
            pdfReader1.setAllContent(imgs);
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }

        ConcurrentHashMap<Integer, List<Word>> pagesWithInfo = pdfReader1.getPagesWithInfo();

        // check if there are multiple files
        for (Word w : pagesWithInfo.get(0)) {
            String s = w.getText();
            if (s.contains("Page")) {
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

        HashMap<String, String> occupants = new HashMap<>();

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
                                                    Conversions.convert(TEMPERATURE,  Float.parseFloat(matcher.group()));
                                            setter.invoke(data, String.format("%.1f", value));
                                        } catch (NoSuchMethodException | InvocationTargetException |
                                                 IllegalAccessException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }
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
                                                        v : Conversions.convert(TEMPERATURE,  v);
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
                                                    Conversions.convert(THERMAL_RESISTANCE, Float.parseFloat(parts[length]));

                                            numMatcher = numPattern.matcher(parts[length - 1]);
                                            length = numMatcher.find() ? length - 1 : length - 2;
                                            float area = isSIUnit() ?
                                                    Float.parseFloat(parts[length]) :
                                                    Conversions.convert(AREA, Float.parseFloat(parts[length]));
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
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern);
                            data.setCeilingComponents(value);
                            // clean memory
                            value = null;
                        }
                        case MAIN_WALL_COMPONENTS -> {
                            String start = "MAIN WALL COMPONENTS";
                            String end = "EXPOSED FLOORS";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern);
                            data.setMainWallComponents(value);
                            // clean memory
                            value = null;
                        }
                        case EXPOSED_FLOORS -> {
                            String start = "EXPOSED FLOORS";
                            String end = "EXPOSED FLOOR SCHEDULE";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s(\\d+(\\.\\d+)?|>)");
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern);
                            data.setExposedFloors(value);
                            // clean memory
                            value = null;
                        }
                        case DOORS -> {
                            String start = "DOORS";
                            String end = "FOUNDATIONS";
                            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                            List<List<String>> value = extractTableComponents(content, content_confidence, start, end, pattern);
                            data.setDoors(value);
                            // clean memory
                            value = null;
                        }
                        case INTERIOR_WALL -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                                    Pattern typePattern = Pattern.compile("type:(.*?)R-(value|Value)");
                                    Matcher typeMatcher = typePattern.matcher(line);
                                    if (typeMatcher.find()) {
                                        data.setInteriorWallType(typeMatcher.group(1));
                                    }
                                    String[] parts = line.split("\\s");
                                    for (int j = parts.length - 1; j >= 0; j--) {
                                        Pattern rsiPattern = Pattern.compile("\\d+(\\.\\d+)?");
                                        Matcher rsiMatcher = rsiPattern.matcher(parts[j]);

                                        if (rsiMatcher.find()) {
                                            Matcher decimalMatcher = Pattern.compile("\\d+\\.\\d{2}").matcher(rsiMatcher.group());
                                            float originalRSI = decimalMatcher.find() ?
                                                    Float.parseFloat(rsiMatcher.group()) :
                                                    Float.parseFloat(rsiMatcher.group()) / 100f;
                                            float rsi = isSIUnit() ?
                                                    originalRSI :
                                                    Conversions.convert(THERMAL_RESISTANCE,  originalRSI);
                                            data.setInteriorWallRValue(String.format("%.2f", rsi));
                                            break;
                                        }
                                    }
                                    break;

                                }
                            }
                        }
                        case EXTERIOR_WALL -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                                    Pattern typePattern = Pattern.compile("type:(.*?)R-(value|Value)");
                                    Matcher typeMatcher = typePattern.matcher(line);
                                    if (typeMatcher.find()) {
                                        data.setExteriorWallType(typeMatcher.group(1));
                                    }
                                    // R-VALUE
                                    String[] parts = line.split("\\s");
                                    for (int j = parts.length - 1; j >= 0; j--) {
                                        Pattern rsiPattern = Pattern.compile("\\d+(\\.\\d+)?");
                                        Matcher rsiMatcher = rsiPattern.matcher(parts[j]);

                                        if (rsiMatcher.find()) {
                                            Matcher decimalMatcher = Pattern.compile("\\d+\\.\\d{2}").matcher(rsiMatcher.group());
                                            float originalRSI = decimalMatcher.find() ?
                                                    Float.parseFloat(rsiMatcher.group()) :
                                                    Float.parseFloat(rsiMatcher.group()) / 100f;
                                            float rsi = isSIUnit() ?
                                                    originalRSI :
                                                    Conversions.convert(THERMAL_RESISTANCE,  originalRSI);
                                            data.setExteriorWallRValue(String.format("%.2f", rsi));
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        case ADDED_TO_SLAB -> {
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                if (line.contains(field.getKeyword())) {
                                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        Matcher decimalMatcher = Pattern.compile("\\d+\\.\\d{2}").matcher(matcher.group());
                                        float originalRSI = decimalMatcher.find() ?
                                                Float.parseFloat(matcher.group()) :
                                                Float.parseFloat(matcher.group()) / 100f;
                                        float rsi = isSIUnit() ?
                                                originalRSI :
                                                Conversions.convert(THERMAL_RESISTANCE,  originalRSI);
                                        data.setAddedToSlab(String.format("%.2f", rsi));
                                    }
                                }
                            }
                        }
                        case FLOORS_ABOVE_FOUND -> {
                            for (int i = 1; i < content.size(); i++) {
                                String s = content.get(i);
                                if (s.matches("Found\\.:") || s.matches("Found\\.:\n")) {
                                    String line = content.get(i - 1);
                                    if (content_confidence.get(i - 1) < confThreshold) lowConfContent.add(field.getTitle() + " R-Value");
                                    Pattern pattern = Pattern.compile("(\\d+(\\.\\d{2})?)");
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
                                                    Conversions.convert(THERMAL_RESISTANCE,  originalRSI);
                                            data.setFloorsAboveFound(String.format("%.2f", rsi));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        case BUILDING_ASSEMBLY_DETAILS -> {
                            HashMap<String, List<String>> buildingAssemblyDetails = new HashMap<>();
                            String currentComponent = null;
                            Pattern componentsPattern = Pattern.compile("^(MAIN WALL COMPONENTS|CEILING COMPONENTS|EXPOSED FLOORS|FLOORS ABOVE)(\n)?$");
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
                                        buildingAssemblyDetails.putIfAbsent(currentComponent, new ArrayList<>());
                                    } else {
                                        Pattern valuePattern = Pattern.compile("\\d+(\\.\\d+)?\\s\\d+(\\.\\d+)?");
                                        Matcher valueMatcher = valuePattern.matcher(line);
                                        if (valueMatcher.find()) {
                                            if (content_confidence.get(i) < confThreshold) lowConfContent.add(field.getTitle() + " " + line);
                                            String[] values = line.split("\\s");
                                            float rsi = isSIUnit() ?
                                                    Float.parseFloat(values[values.length - 1]) :
                                                    Conversions.convert(THERMAL_RESISTANCE,  Float.parseFloat(values[values.length - 1]));
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
                                                Conversions.convert(VOLUME_M3,  Float.parseFloat(values[0]));
                                        float energy = isSIUnit() ?
                                                Float.parseFloat(values[values.length - 2]) :
                                                Conversions.convert(ENERGY,  Float.parseFloat(values[values.length - 2]));
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
                                                    Conversions.convert(VOLUME_FLOW_RATE,  Float.parseFloat(matcher1.group()));
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
                                                    Conversions.convert(VOLUME_FLOW_RATE,  Float.parseFloat(matcher.group(3)));
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
                                                Conversions.convert(ENERGY,  Float.parseFloat(matcher.group()));
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
                                                Conversions.convert(ENERGY,  Float.parseFloat(valueMatcher.group()));
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
                                                Conversions.convert(VOLUME_LITRE,  Float.parseFloat(matcher.group()));
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
                                                Conversions.convert(TEMPERATURE,  Float.parseFloat(matcher.group()));
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
                                                    Conversions.convert(POWER, Float.parseFloat(matcher.group()));
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
                            String end = "SPACE HEATING SYSTEM PERFORMANCE";
                            String start = "MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE";
                            Pattern pattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Ann)");
                            List<String> list = findAnnualTable(content, content_confidence, start, end, pattern);
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
                                    float heatingLoadValue = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        // if current number does not contain a decimal point, then put it into lowConfContent,
                                        // and check if the next number is a single digit, if so, assume the actual number is separated to two parts, (ie. 123 4)
                                        // combine the two parts to get the actual number
                                        // if not, assume the current number is a whole number, and divide it by 10^decimalPlaces (ie. 1234)
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            heatingLoadValue = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else {
                                            heatingLoadValue /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Heating Load: OCRed=" + original + " -> predicted=" + heatingLoadValue);
                                    }
                                    i++;
                                    float heatingLoad = isSIUnit() ?
                                            heatingLoadValue : Conversions.convert(ENERGY, heatingLoadValue);

                                    float furnaceValue = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            furnaceValue = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                        } else {
                                            furnaceValue /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Furnace Input: OCRed=" + original + " -> predicted=" + furnaceValue);
                                    }
                                    float furnaceInput = isSIUnit() ?
                                            furnaceValue : Conversions.convert(ENERGY, furnaceValue);

                                    float COP = Float.parseFloat(parts[parts.length - 1]);
                                    if (!parts[parts.length - 1].contains(".")) {
                                        String original = parts[parts.length - 1];
                                        if (!parts[parts.length - 2].contains(".") && parts[parts.length - 1].length() == decimalPlaces) {
                                            COP = Float.parseFloat(parts[parts.length - 2] + "." + parts[parts.length - 1]);
                                        } else {
                                            COP /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " COP: OCRed=" + original + " -> predicted=" + COP);
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
                            String end = "MONTHLY ESTIMATED ENERGY CONSUMPTION BY DEVICE";
                            String start = "ESTIMATED FUEL COSTS (Dollars)";
                            Pattern pattern = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Ann)");
                            List<String> list = findAnnualTable(content, content_confidence, start, end, pattern);
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
                                    float value1 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        // if current number does not contain a decimal point, then put it into lowConfContent,
                                        // and check if the next number is a single digit, if so, assume the actual number is separated to two parts, (ie. 123 4)
                                        // combine the two parts to get the actual number
                                        // if not, assume the current number is a whole number, and divide it by 10^decimalPlaces (ie. 1234)
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value1 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else {
                                            value1 /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Space Heating Primary: OCRed=" + original + " -> predicted=" + value1);
                                    }
                                    i++;
                                    float spaceHeatingPrimary = isSIUnit() ?
                                            value1 : Conversions.convert(ENERGY, value1);

                                    float value2 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value2 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else {
                                            value2 /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Space Heating Secondary: OCRed=" + original + " -> predicted=" + value2);
                                    }
                                    i++;
                                    float spaceHeatingSecondary = isSIUnit() ?
                                            value2 : Conversions.convert(ENERGY, value2);

                                    float value3 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value3 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else {
                                            value3 /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " DHW Primary: OCRed=" + original + " -> predicted=" + value3);
                                    }
                                    i++;
                                    float DHWPrimary = isSIUnit() ?
                                            value3 : Conversions.convert(ENERGY, value3);

                                    float value4 = Float.parseFloat(parts[i]);
                                    if(!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value4 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else {
                                            value4 /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " DHW Secondary: OCRed=" + original + " -> predicted=" + value4);
                                    }
                                    i++;
                                    float DHWSecondary = isSIUnit() ?
                                            value4 : Conversions.convert(ENERGY, value4);

                                    float value5 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value5 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else {
                                            value5 /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " Lights: OCRed=" + original + " -> predicted=" + value5);
                                    }
                                    i++;
                                    float Lights = isSIUnit() ?
                                            value5 : Conversions.convert(ENERGY, value5);

                                    float value6 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value6 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                            i++;
                                        } else {
                                            value6 /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " HRV: OCRed=" + original + " -> predicted=" + value6);
                                    }
                                    i++;
                                    float hrv = isSIUnit() ?
                                            value6 : Conversions.convert(ENERGY, value6);

                                    float value7 = Float.parseFloat(parts[i]);
                                    if (!parts[i].contains(".")) {
                                        String original = parts[i];
                                        if (!parts[i+1].contains(".") && parts[i+1].length() == decimalPlaces) {
                                            value7 = Float.parseFloat((parts[i] + "." + parts[i+1]));
                                        } else {
                                            value7 /= Math.pow(10, decimalPlaces);
                                        }
                                        lowConfContent.add(field.getTitle() + " " + month + " AC: OCRed=" + original + " -> predicted=" + value7);
                                    }
                                    float ac = isSIUnit() ?
                                            value7 : Conversions.convert(ENERGY, value7);

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
                        Nightime Setback, Air Leakage Test Results, System Type, Air Distribution/circulation type,
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
        return data;
    }

    private  List<String> findAnnualTable(List<String> content, List<Float> content_confidence, String startMarker, String endMarker, Pattern pattern) {
        List<String> list = new ArrayList<>();
        boolean inSection = false;

        for (int i = content.size() - 1; i >= 0; i--) {
            String line = content.get(i);
            if (line.contains(startMarker)) {
                inSection = true;
                continue;
            }
            if (inSection && line.contains(endMarker)) break;

            if (inSection) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    list.add(0, line.replace("\n", ""));
                }
            }
        }


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
                    Conversions.convert(POWER, Float.parseFloat(matcher.group()));
        }
        return String.valueOf(value);
    }

    private void findPowerAndEfficiency(List<String> content, List<Float> content_confidence, Fields field, String keywordImperial, PdfData data, Pattern pattern) {
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.contains("Page")) continue;
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

    private HashMap<String, List<String>> extractBuildingParameters(List<String> content, List<Float> content_confidence, String start,
                                                                    String end, Pattern pattern) {
        HashMap<String, List<String>> buildingParametersZone = new HashMap<>();

        boolean foundSectionStart = false;

        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.contains("Page")) continue;
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
                            Conversions.convert(ENERGY, Float.parseFloat(values[currLocation]));

                    numberMatcher = numberPattern.matcher(values[currLocation - 1]);
                    currLocation = numberMatcher.find() ? currLocation - 1 : currLocation - 2;
                    rsi = isSIUnit() ?
                            Float.parseFloat(values[currLocation]) :
                            Conversions.convert(THERMAL_RESISTANCE, Float.parseFloat(values[currLocation]));

                    numberMatcher = numberPattern.matcher(values[currLocation - 1]);
                    currLocation = numberMatcher.find() ? currLocation - 1 : currLocation - 2;
                    area = isSIUnit() ?
                            Float.parseFloat(values[currLocation]) :
                            Conversions.convert(AREA, Float.parseFloat(values[currLocation]));

                    buildingParametersZone.put(name.toString(), Arrays.asList(String.format("%.2f", area),
                            String.format("%.2f", rsi), String.format("%.2f", heatLoss)));
                }
            }
        }
        return buildingParametersZone;
    }

    private List<List<String>> extractTableComponents(List<String> content, List<Float> content_confidence, String sectionStart,
                                                     String end, Pattern pattern) {
        List<List<String>> components = new ArrayList<>();
        boolean foundStartMarker = false;

        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.contains("Page")) continue;
            if (line.startsWith(sectionStart)) {
                foundStartMarker = true;
                continue;
            }
            if (foundStartMarker) {
                if (sectionStart.equals("MAIN WALL COMPONENTS")) {
                    Pattern endPattern = Pattern.compile("(WALL CODE SCHEDULE|EXPOSED FLOORS)");
                    Matcher endMatcher = endPattern.matcher(line);
                    if (endMatcher.find()) break;
                } else if (sectionStart.equals("EXPOSED FLOORS")) {
                    Pattern endPattern = Pattern.compile("(DOORS|EXPOSED FLOOR SCHEDULE|Indicates)");
                    Matcher endMatcher = endPattern.matcher(line);
                    if (endMatcher.find()) break;
                } else {
                    if (line.contains(end)) break;
                }

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if (content_confidence.get(i) < confThreshold) lowConfContent.add(sectionStart + " " + content.get(i));

                    List<String> values = new ArrayList<>(Arrays.asList(line.split("\\s")));
                    if (values.size() < 3) continue;
                    float lastValue = Float.parseFloat(values.get(values.size() - 1));
                    Pattern numberPattern = Pattern.compile("\\d+");
                    Matcher numberMatcher = numberPattern.matcher(values.get(values.size() - 2));
                    float secondLastValue = 0;
                    if (numberMatcher.find()) {
                        secondLastValue = Float.parseFloat(values.get(values.size() - 2));
                    } else {
                        secondLastValue = Float.parseFloat(values.get(values.size() - 3));
                    }

                    float area = isSIUnit() ?
                            secondLastValue : Conversions.convert(AREA, secondLastValue);
                    float rsi = isSIUnit() ?
                            lastValue : Conversions.convert(THERMAL_RESISTANCE, lastValue);

                    components.add(Arrays.asList(String.format("%.2f", area), String.format("%.2f", rsi)));
                }
            }
        }
        return components;
    }
}
