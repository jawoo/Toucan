package org.cgiar.toucan;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class App
{

    // Some initialization and settings
    static long timeInitial = System.currentTimeMillis();
    static String tableNameUnitInformation = "unit_information";
    static String OS = System.getProperty("os.name").toLowerCase();
    static String d;
    static String filePlantingDate = "pdates";
    static String fileDaysToFlowering = "daystoflowering";
    static String directoryWorking;
    static String directoryWeather;
    static String directoryMultiplePlatingDates;
    static String directoryFloweringDates;
    static String directoryInputPlatingDates;
    static String directoryInput;
    static String directoryOutput;
    static String directoryFinal;
    static String directoryError;
    static String directorySource;
    static String dataPlantingDates;
    static String dataDaysToFlowering;
    static int numberOfThreads = 4;    // per box
    static String countryCode = "ETH";
    static int limitForDebugging = 1;
    static boolean useAvgPlantingDensity = true;
    static boolean verbose = false;
    static boolean cleanUpFirst = true;
    static int numberOfCultivars = 0;
    static boolean printDaysToFlowering = true;
    static boolean step1 = true;   // preparation
    static boolean step2 = true;   // testingMultiplePlantingDates
    static boolean step3 = true;   // retrievingDaysToFlowering
    static boolean step4 = false;   // seasonalRuns
    static boolean step5 = false;   // wrappingUp

    /*
    0: Water Management
    1: Fertilizer
    2: Manure
    3: Residue
    4: Planting Window
    5: Planting Density
    6: CO2 Fertilization
    */
    static boolean[] switchScenarios =
    {
        false,   //0
        false,   //1
        false,   //2
        false,   //3
        false,   //4
        false,   //5
        false    //6
    };
    static boolean scenarioCombinations = true;

    // To check if it's numeric
    static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
    static String nr = System.lineSeparator();

    // Main stuff
    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException
    {

        /*
        1. PREPARATION
        */
        if (step1)
        {

            // Parsing arguments
            if (args.length>0 && args[0].length()==3)
                countryCode = args[0];
            System.out.println("> ISO3: "+countryCode);
            if (args.length>0 && args[1].length()>0)
                numberOfThreads = Integer.parseInt(args[1]);
            System.out.println("> Number of threads: "+numberOfThreads);
            if (args.length>0 && args[2].length()>0)
                limitForDebugging = Integer.parseInt(args[2]);
            System.out.println("> Limit: "+limitForDebugging);

            // OS-dependent settings
            System.out.println("> OS: "+OS.toUpperCase());
            if (isWindows())
            {
                d = "\\";
                directoryWorking = "C:\\DSSAT48\\Toucan\\";
                directoryWeather = "C:\\DSSAT48\\Toucan\\_W\\2022-12_tt9106r2\\";
            }
            else if (isUnix())
            {
                d = "/";
                directoryWorking = "/home/jkoo/toucan/";
                directoryWeather = "/home/jkoo/weather/wtg/";
            }
            directoryMultiplePlatingDates = directoryWorking + "_M" + d;
            directoryFloweringDates = directoryWorking + "_F" + d;
            directoryInputPlatingDates = directoryWorking + "_P" + d;
            directoryInput = directoryWorking + "_I" + d;
            directoryOutput = directoryWorking + "_O" + d;
            directoryFinal = directoryWorking + "_X" + d;
            directoryError = directoryWorking + "_E" + d;
            directorySource = directoryWorking + "_S" + d;
            dataPlantingDates = directoryInputPlatingDates + filePlantingDate + ".csv";

            // Copying Toucan workspace files
            File toucanSource = new File(directorySource);
            try
            {

                // Making T copies
                for(int t=0; t<numberOfThreads; t++)
                {
                    String dT = directoryWorking+"T"+t;
                    File toucanDestination = new File(dT);
                    FileUtils.copyDirectory(toucanSource, toucanDestination);

                    // File permission change if Linux
                    if (isUnix())
                    {

                        // Permission 777
                        Set<PosixFilePermission> perms = new HashSet<>();
                        perms.add(PosixFilePermission.OWNER_READ);
                        perms.add(PosixFilePermission.OWNER_WRITE);
                        perms.add(PosixFilePermission.OWNER_EXECUTE);
                        perms.add(PosixFilePermission.GROUP_READ);
                        perms.add(PosixFilePermission.GROUP_WRITE);
                        perms.add(PosixFilePermission.GROUP_EXECUTE);
                        perms.add(PosixFilePermission.OTHERS_READ);
                        perms.add(PosixFilePermission.OTHERS_WRITE);
                        perms.add(PosixFilePermission.OTHERS_EXECUTE);

                        // Apply to the directory
                        Files.setPosixFilePermissions(Paths.get(dT), perms);

                        // Apply to all files
                        String[] workingFileNames = getFileNames(dT);
                        for (String workingFileName : workingFileNames)
                            Files.setPosixFilePermissions(Paths.get(dT + d + workingFileName), perms);

                    }

                }

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            // Delete files from previous runs
            if (cleanUpFirst)
            {
                FileUtils.cleanDirectory(new File(directoryOutput));
                FileUtils.cleanDirectory(new File(directoryError));
                FileUtils.cleanDirectory(new File(directoryMultiplePlatingDates));
                FileUtils.cleanDirectory(new File(directoryFloweringDates));
                FileUtils.cleanDirectory(new File(directoryInputPlatingDates));
            }

        } //if (step1)

        // CO2
        TreeMap<Integer, Integer> co2History = getCO2History();

        // Climate Options
        ArrayList<Object[]> climateOptions = new ArrayList<>();
        climateOptions.add(new Object[]{ 2011, "2011" });

        // Looping through climates
        for (Object[] climate: climateOptions)
        {
            int firstPlantingYear = (int)climate[0];
            String climateOption = (String)climate[1];
            int co2 = co2History.get(firstPlantingYear);

            // Get unit information
            Object[] unitInfo = getUnitInfo(tableNameUnitInformation);
            int numberOfUnits = unitInfo.length;
            System.out.println("> Number of units to run: "+numberOfUnits);

            // Get weather information
            String[] weatherInfo = getFileNames(directoryWeather, "WTH", 5);

            // Reduce the number of threads to match with number of units
            //if (numberOfThreads>numberOfUnits) numberOfThreads = numberOfUnits;


            /*
            2. FINDING PROMISING PLANTING DATES
            */
            System.out.println("> Climate scenario: "+climateOption);
            TreeMap<Object, Object> plantingDatesToSimulate = getPlantingDates(unitInfo, weatherInfo);


            /*
            3. ADDITIONAL RUNS FOR RETRIEVING DAYS-TO-FLOWER FOR EACH VARIETY
            */
            System.out.println("> Retrieving days to flowering...");
            TreeMap<Object, Object> daysToFloweringByCultivar = getFloweringDates(unitInfo, weatherInfo, plantingDatesToSimulate, climateOption, firstPlantingYear, co2);


            /*
            4. SEASONAL RUNS
            */
            if (step4)
            {
                System.out.println("> Running seasonal simulations...");

                // Multithreading
                ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder().setNameFormat("%d");
                ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads, threadFactoryBuilder.build());
                List<Future<Integer>> list = new ArrayList<>();

                // Looping through units
                for (int i = 0; i < numberOfUnits; i = i + 1)
                {

                    // Status
                    String progress = climateOption + ", R" + (i+1) + "/" + numberOfUnits;

                    // Subset
                    if (numberOfUnits>0)
                    {
                        try
                        {
                            if (i < numberOfUnits)
                            {

                                //Unit Information
                                Object[] ou = (Object[]) unitInfo[i];
                                String season = (String) ou[12];

                                // Construct the cultivar option
                                String cropCode = (String)ou[6];
                                String cultivarCode = (String)ou[7];
                                String cultivarName = (String)ou[8];
                                int[] cultivarInfo = (int[])ou[9];
                                Object[] cultivarOption = new Object[]{ countryCode, cropCode, cultivarCode, cultivarName, cultivarInfo[0], cultivarInfo[1], cultivarInfo[2] };
                                String cropCultivarCode = cultivarOption[1] + (String)cultivarOption[2];
                                int daysToFlowering = ((int[])daysToFloweringByCultivar.get(cropCultivarCode))[0];
                                int daysToHarvest = ((int[])daysToFloweringByCultivar.get(cropCultivarCode))[1];

                                // Multiple weather files for this unit
                                for (String weatherFileName: weatherInfo)
                                {
                                    String weatherKey = weatherFileName.split("\\.")[0] + "_" + season + "_" + cropCode;
                                    int p = (int)plantingDatesToSimulate.get(weatherKey);
                                    Object[] weatherAndPlantingDate = { weatherFileName, p };

                                    // Multiple threads
                                    Future<Integer> future = executor.submit(new ThreadSeasonalRuns(ou, weatherAndPlantingDate, cultivarOption, daysToFlowering, daysToHarvest, climateOption, progress, firstPlantingYear, co2History));
                                    list.add(future);
                                }

                            }
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }

                    }

                } // Looping through units

                // Retrieve
                for (Future<Integer> future : list)
                {
                    future.get();
                }
                executor.shutdown();

            } //if (step4)



            /*
            5. WRAPPING UP
            */
            if (step5)
            {
                boolean firstFile = true;
                String[] outputFileNames = getFileNames(directoryOutput, climateOption);

                // Write
                try
                {
                    String combinedOutput = directoryFinal+climateOption+"_combinedOutput_"+countryCode+".csv";
                    BufferedWriter writer = new BufferedWriter(new FileWriter(combinedOutput));

                    // Looping through the files
                    String header;
                    for (String outputFileName: outputFileNames)
                    {
                        BufferedReader reader = new BufferedReader(new FileReader(directoryOutput+outputFileName));
                        String line;

                        // To skip the header from the second file
                        if (firstFile)
                        {
                            header = reader.readLine()
                                    .replace("SOIL_ID","SoilProfileID")
                                    .replace("LATI","LAT")
                                    .replace("TNAM","TNAM,WeatherSequence")
                                    .replace("CR","CropCode")
                                    .replace("FNAM","CultivarCode");     // Replace SOIL_ID with SoilProfileID
                            writer.append(header).append(nr);
                            firstFile = false;
                        }

                        // Reader --> Writer
                        while ((line = reader.readLine()) != null)
                        {
                            String firstValue = line.split(",")[0];
                            if (isNumeric(firstValue))
                                writer.append(line.replace("|", ",")).append(nr);
                        }
                    }
                    writer.close();
                    System.out.println("> Output files merged for "+climateOption);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }

            } //if (step5)

        } // for (climate)

        // How long did it take?
        long runningTime = (System.currentTimeMillis() - timeInitial)/(long)1000;
        String rt = String.format("%1$02d:%2$02d:%3$02d", runningTime / (60*60), (runningTime / 60) % 60, runningTime % 60);
        System.out.println("> Done ("+rt+")");

    }



    // Find promising planting dates
    public static TreeMap<Object, Object> getPlantingDates(Object[] unitInfo, String[] weatherInfo)
    {
        TreeMap<Object, Object> plantingDatesToSimulate = new TreeMap<>();
        int numberOfUnits = unitInfo.length;

        // If step2 is false, an empty treemap will be returned.
        if (step2 && numberOfUnits>0)
        {
            try
            {
                ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
                List<Future<Object[]>> list = new ArrayList<>();

                // Execute
                for (int i=0; i<numberOfUnits; i++)
                {
                    Object[] oi = (Object[])unitInfo[i];
                    int cell5m = (Integer) oi[1];
                    int medianPlantingDate = (Integer) oi[5];
                    String cropCode = (String) oi[6];
                    String season = (String) oi[12];
                    System.out.println("> Planting date "+(i+1)+"/"+numberOfUnits+", CELL5M: "+cell5m+" for "+cropCode+", "+season+" season");

                    // Multiple weather files for this unit
                    for (String weatherFileName: weatherInfo)
                    {
                        Future<Object[]> future = executor.submit(new ScanningPlantingDates(medianPlantingDate, weatherFileName, season, cropCode));
                        list.add(future);
                    }

                }

                // Retrieve
                for (int i = 0, listSize = list.size(); i < listSize; i++)
                {
                    Future<Object[]> future = list.get(i);
                    Object[] r = future.get();
                    String weatherKey = (String) r[0];
                    int plantingDate = (Integer) r[1];
                    //int plantingDate = 135;  // <-- Per Tim Thomas' specification

                    plantingDatesToSimulate.put(weatherKey, plantingDate);
                }
                executor.shutdown();

            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        System.out.println("> Runs for scanning planting dates done.");
        return plantingDatesToSimulate;
    }



    // Find average flowering dates
    public static TreeMap<Object, Object> getFloweringDates(Object[] unitInfo, String[] weatherInfo,
                                                            TreeMap<Object, Object> plantingDatesToSimulate,
                                                            String climateOption,
                                                            int firstPlantingYear, int co2) throws IOException, ExecutionException, InterruptedException {
        TreeMap<Object, Object> daysToFloweringByCultivar = new TreeMap<>();
        String plantingDateOptionLabel = "PB";
        int numberOfUnits = unitInfo.length;

        // Distribute weather files over threads
        if (step3 && numberOfUnits>0)
        {
            int u = 0;
            int threadID = 0;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            List<Future<Integer>> list = new ArrayList<>();
            while(u < numberOfUnits)
            {
                try
                {
                    Object[] o = (Object[])unitInfo[u];
                    int pdMean = (int)o[5];
                    String season = (String) o[12];

                    // Construct the cultivar option
                    String cropCode = (String)o[6];
                    String cultivarCode = (String)o[7];
                    String cultivarName = (String)o[8];
                    int[] cultivarInfo = (int[])o[9];
                    Object[] cultivarOption = new Object[]{ countryCode, cropCode, cultivarCode, cultivarName, cultivarInfo[0], cultivarInfo[1], cultivarInfo[2] };

                    // Weather name
                    for (String weatherFileName: weatherInfo)
                    {
                        String weatherKey = weatherFileName.split("\\.")[0] + "_" + season + "_" + cropCode;

                        // To use below
                        daysToFloweringByCultivar.put(cropCode + cultivarCode, new int[]{0, 0});

                        // Planting dates to use
                        if (plantingDatesToSimulate.containsKey(weatherKey))
                        {
                            int pd = (Integer) plantingDatesToSimulate.get(weatherKey);
                            if (pd <= 0) pd = pdMean;
                            Future<Integer> future = executor.submit(new ThreadFloweringRuns(o, threadID, weatherFileName, pd, cultivarOption, plantingDateOptionLabel, co2, firstPlantingYear));
                            list.add(future);
                            threadID++;
                            if (threadID==numberOfThreads) threadID = 0;
                        }

                    }

                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                u++;

            }

            // Retrieve
            for (Future<Integer> future: list)
            {
                int exitCode = future.get();
                if (exitCode>0) System.out.println("> Failed to find good flowering dates...");
            }
            executor.shutdown();

            // Collect output files
            String[] csvFileNames = getFileNames(directoryFloweringDates, "CSV");

            // For each CSV file
            for (String csvFileName: csvFileNames)
            {

                // Status
                System.out.println("> Analyzing "+csvFileName+"...");

                // Reading in
                Reader in = new FileReader(directoryFloweringDates+csvFileName);
                Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
                for (CSVRecord record: records)
                {

                    // Parse the cultivar code from TNAM
                    String cropCultivarCode = record.get("CR") + record.get("TNAM").substring(0,6);

                    // Parse PDAT and ADAT for computing the "days to flowering"
                    int dtf, dth, pDDD = 0, aDDD = 0, hDDD = 0;
                    if (record.get("PDAT").length()>4)
                        pDDD = Integer.parseInt(record.get("PDAT").substring(4));
                    if (record.get("ADAT").length()>4)
                        aDDD = Integer.parseInt(record.get("ADAT").substring(4));
                    if (record.get("HDAT").length()>4)
                        aDDD = Integer.parseInt(record.get("HDAT").substring(4));
                    if (aDDD>0)
                    {

                        // Flowering date
                        if (aDDD > pDDD)
                            dtf = aDDD - pDDD + 1;
                        else
                            dtf = aDDD + (365 - pDDD) + 1;

                        // Harvest date
                        if (hDDD > pDDD)
                            dth = hDDD - pDDD + 1;
                        else
                            dth = hDDD + (365 - pDDD) + 1;

                        // Storing
                        try
                        {
                            int dtfPrevious = ((int[])daysToFloweringByCultivar.get(cropCultivarCode))[0];
                            int dtfNew = (dtf + dtfPrevious) / 2;

                            int dthPrevious = ((int[])daysToFloweringByCultivar.get(cropCultivarCode))[1];
                            int dthNew = (dth + dthPrevious) / 2;

                            daysToFloweringByCultivar.put(cropCultivarCode, new int[]{ dtfNew, dthNew });
                        }
                        catch(Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }

                } // For each row in the CSV file

            } // For each CSV file

            // Writing a CSV output file
            if (printDaysToFlowering)
            {
                dataDaysToFlowering = directoryFloweringDates + fileDaysToFlowering + "_" + climateOption +".csv";
                System.out.println("> Writing "+dataDaysToFlowering+"...");
                try( FileWriter writer = new FileWriter(dataDaysToFlowering);
                     CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                             .withHeader("CultivarCode","AvgDaysToFlowering","AvgDaysToHarvest")))
                {
                    for(Map.Entry<Object, Object> entry : daysToFloweringByCultivar.entrySet())
                    {
                        String key = (String)entry.getKey();
                        int[] value = (int[])entry.getValue();
                        csvPrinter.printRecord(key, value[0], value[1]);
                    }
                    csvPrinter.flush();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

        }
        return daysToFloweringByCultivar;
    }



    // List of Unit IDs
    public static Object[] getUnitInfo(String tableName)
    {
        int counter = 0;
        List unitInfo = Lists.newArrayList();
        try
        {
            Reader in = new FileReader(directoryInput + tableName + ".csv");
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records)
            {
                if (limitForDebugging==0 || counter<limitForDebugging)
                {
                    String[] crops = record.get("Crops").split(",");
                    String[] pmonths = record.get("PlantingMonths").split(",");
                    String[] areas = record.get("Areas").split(",");

                    for (int c=0; c<crops.length; c++)
                    {
                        String crop = crops[c];
                        String area = areas[c];
                        ArrayList<String> cultivarList = Utility.getCultivarCodes(crop);
                        String[] cultivarCodeAndNames = cultivarList.toArray(new String[0]);
                        int[] cultivarInfo = Utility.getCultivarManagementInformation(crop);
                        try
                        {

                            //if (String.valueOf(crop).equals("MZ"))
                            //{
                                for (String cultivarCodeAndName: cultivarCodeAndNames)
                                {
                                    String cultivarCode = cultivarCodeAndName.substring(0,6);
                                    String cultivarName = cultivarCodeAndName.substring(7);
                                    String pmonth = pmonths[c];
                                    String pdat = Utility.getPlantingDate(pmonth);

                                    // Putting all unit information in one object array
                                    Object[] o = new Object[16];
                                    o[0]  = Integer.parseInt(record.get("UnitID"));
                                    o[1]  = Integer.parseInt(record.get("CELL5M"));
                                    o[2]  = record.get("SoilProfileID");
                                    o[3]  = record.get("SoilProfile");
                                    o[4]  = Integer.parseInt(record.get("SoilRootingDepth"));
                                    o[5]  = Integer.parseInt(pdat);
                                    o[6]  = crop;
                                    o[7]  = cultivarCode;
                                    o[8]  = cultivarName;
                                    o[9]  = cultivarInfo;
                                    o[10] = Double.parseDouble(record.get("X"));
                                    o[11] = Double.parseDouble(record.get("Y"));
                                    o[12] = record.get("Season").substring(0,4).toUpperCase();
                                    o[13] = record.get("AEZ");
                                    o[14] = record.get("FPU");
                                    o[15] = area;
                                    unitInfo.add(o);
                                    counter++;
                                }
                            //}

                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        Object[] list = unitInfo.toArray(Object[]::new);
        return list;
    }



    // CO2
    public static TreeMap<Integer, Integer> getCO2History()
    {
        TreeMap<Integer, Integer> co2History = new TreeMap<>();
        try
        {
            Reader in = new FileReader(directoryInput + "CO2048.csv");
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records)
            {
                int y = Integer.parseInt(record.get("YEAR"));
                int c = (int)Double.parseDouble(record.get("CO2"));
                co2History.put(y,c);
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        return co2History;
    }



    // File names
    static String[] getFileNames(String filePath, String filteringText)
    {
        File dir = new File(filePath);
        FilenameFilter filter = (directory, name) -> (name.toUpperCase().contains(filteringText.toUpperCase()));
        String[] list = dir.list(filter);
        return list;
    }
    static String[] getFileNames(String filePath, String filteringText, int limitForDebugging)
    {
        File dir = new File(filePath);
        FilenameFilter filter = (directory, name) -> (name.toUpperCase().contains(filteringText.toUpperCase()));
        String[] list = Arrays.stream(dir.list(filter)).limit(limitForDebugging).toArray(String[]::new);
        return list;
    }
    static String[] getFileNames(String filePath)
    {
        File dir = new File(filePath);
        return dir.list();
    }



    // Numeric checker
    static boolean isNumeric(String strNum)
    {
        if (strNum == null) {
            return false;
        }
        return pattern.matcher(strNum).matches();
    }



    // OS detection
    public static boolean isWindows()
    {
        return (OS.contains("win"));
    }
    public static boolean isUnix()
    {
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0 );
    }

}
