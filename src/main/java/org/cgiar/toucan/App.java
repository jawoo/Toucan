package org.cgiar.toucan;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Array;
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
    static String d = File.separator;
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
    static String directoryThreads;
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
    static boolean step4 = true;   // seasonalRuns
    static boolean step5 = true;   // wrappingUp

    /*
    0: Water Management
    1: Fertilizer
    2: Manure
    3: Residue
    4: Planting Window
    5: Planting Density
    6: CO2 Fertilization
    */
    static boolean[] switchScenarios = new boolean[7];
    static boolean scenarioCombinations = true;

    // To check if it's numeric
    static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
    static String nr = System.lineSeparator();

    // Main stuff
    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException
    {

        /*
        0. SETTING PARAMETERS
        */

        try
        {
            // Load the YAML file
            Yaml yaml = new Yaml();
            FileInputStream inputStream = new FileInputStream("."+d+"config.yml");
            Map<String, Object> config = yaml.load(inputStream);

            // Assign parameter values
            tableNameUnitInformation = (String)config.get("tableNameUnitInformation");
            countryCode = (String)config.get("countryCode");
            numberOfThreads = (int)config.get("numberOfThreads");
            limitForDebugging = (int)config.get("limitForDebugging");
            scenarioCombinations = (int)config.get("scenarioCombinations") > 0;

            // Access nested elements for directories
            Map<String, String> directories = (Map<String, String>)config.get("directory");
            directoryWorking = "." + App.d + directories.get("working") + d;
            directoryWeather = directoryWorking + "weather" + d + directories.get("weather") + d;
            directorySource = directoryWorking + directories.get("source") + d;
            directoryInput = directoryWorking + directories.get("input") + d;
            directoryThreads = directoryWorking + directories.get("threads") + d;
            directoryFinal = directoryWorking + directories.get("result") + d;
            directoryOutput = directoryWorking + directories.get("temp") + d + directories.get("summary") + d;
            directoryMultiplePlatingDates = directoryWorking + directories.get("temp") + d + directories.get("planting") + d;
            directoryFloweringDates = directoryWorking + directories.get("temp") + d + directories.get("flowering") + d;
            directoryInputPlatingDates = directoryWorking + directories.get("temp") + d + directories.get("plantingDates") + d;
            directoryError = directoryWorking + directories.get("temp") + d + directories.get("errors") + d;

            // Access nested elements for data files
            Map<String, String> dataFiles = (Map<String, String>)config.get("dataFile");
            dataPlantingDates = directoryInputPlatingDates + dataFiles.get("plantingDates");

            // Access nested elements for scenario switches
            Map<String, Integer> scenarioSwitches = (Map<String, Integer>)config.get("scenarioSwitch");
            switchScenarios[0] = scenarioSwitches.get("waterManagement") > 0;
            switchScenarios[1] = scenarioSwitches.get("fertilizer") > 0;
            switchScenarios[2] = scenarioSwitches.get("manure") > 0;
            switchScenarios[3] = scenarioSwitches.get("residue") > 0;
            switchScenarios[4] = scenarioSwitches.get("plantingWindow") > 0;
            switchScenarios[5] = scenarioSwitches.get("plantingDensity") > 0;
            switchScenarios[6] = scenarioSwitches.get("CO2fertilization") > 0;

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }


        /*
        1. PREPARATION
        */
        if (step1)
        {

            // Showing some parameter values
            System.out.println("> OS: "+OS.toUpperCase());
            System.out.println("> ISO3: "+countryCode);
            System.out.println("> Number of threads: "+numberOfThreads);
            System.out.println("> Limit: "+limitForDebugging);
            System.out.println("> Weather data: "+directoryWeather);
            System.out.println("> Management practice - Water: "+(switchScenarios[0] ? "ON" : "OFF"));
            System.out.println("> Management practice - Fertilizer: "+(switchScenarios[1] ? "ON" : "OFF"));
            System.out.println("> Management practice - Manure: "+(switchScenarios[2] ? "ON" : "OFF"));
            System.out.println("> Management practice - Residue: "+(switchScenarios[3] ? "ON" : "OFF"));
            System.out.println("> Management practice - Planting window: "+(switchScenarios[4] ? "ON" : "OFF"));
            System.out.println("> Management practice - Planting density: "+(switchScenarios[5] ? "ON" : "OFF"));
            System.out.println("> Management practice - CO2 fertilization: "+(switchScenarios[6] ? "ON" : "OFF"));
            System.out.println("> Management practice - Factorial combinations: "+(scenarioCombinations ? "ON" : "OFF"));

            // Copying Toucan workspace files
            File toucanSource = new File(directorySource);
            try
            {

                // Making T copies
                for(int t=0; t<numberOfThreads; t++)
                {
                    String dT = directoryThreads+"T"+t;
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
                FileUtils.cleanDirectory(new File(directoryFinal));
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
            String[] weatherInfo = getFileNames(directoryWeather, "WTH", 0);


            /*
            2. FINDING PROMISING PLANTING DATES
            */
            System.out.println("> Climate scenario: "+climateOption);
            TreeMap<Object, Object> plantingDatesToSimulate = getPlantingDates(unitInfo, weatherInfo);


            /*
            3. ADDITIONAL RUNS FOR RETRIEVING DAYS-TO-FLOWER FOR EACH VARIETY
            */
            System.out.println("> Retrieving days to flowering...");
            TreeMap<Object, Object> daysToFloweringByCultivar = new TreeMap<>();
            try
            {
                daysToFloweringByCultivar = getFloweringDates(unitInfo, weatherInfo, plantingDatesToSimulate, climateOption, firstPlantingYear, co2);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }


            /*
            4. SEASONAL RUNS
            */
            System.out.println("> Running seasonal simulations...");
            runSeasonalSimulations(unitInfo, weatherInfo, plantingDatesToSimulate, climateOption, daysToFloweringByCultivar, firstPlantingYear, co2History);


            /*
            5. WRAPPING UP
            */
            if (step5)
            {
                boolean firstFile = true;
                String[] outputFileNames = getFileNames(directoryOutput, "_"+climateOption);

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
                for (Future<Object[]> future: list)
                {
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
                                                            int firstPlantingYear, int co2) throws IOException {
        TreeMap<Object, Object> daysToFloweringByCultivar = new TreeMap<>();
        String plantingDateOptionLabel = "PB";
        int numberOfUnits = unitInfo.length;

        // Distribute weather files over threads
        if (step3 && numberOfUnits>0)
        {
            int threadID = 0;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            List<Future<Integer>> futures = new ArrayList<>();

            // Let's just pick 200 random units
            int[] subUnits = new int[numberOfUnits];
            if (numberOfUnits>20)
            {
                subUnits = new int[200];
                Random random = new Random();
                for (int i = 0; i < subUnits.length; i++)
                    subUnits[i] = random.nextInt(numberOfUnits);
            }
            else
                for (int i = 0; i < numberOfUnits; i++)
                    subUnits[i] = i;

            // Looping through subUnits
            for (int u: subUnits)
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

                    // Select a subset of weatherInfo
                    String[] weatherInfoSubset;
                    if (weatherInfo.length>10)
                    {
                        weatherInfoSubset = new String[10]; // Let's just pick 10
                        int min = 0;
                        int max = weatherInfo.length-1;
                        for (int i=0; i<10; i++)
                        {
                            int randomIndex = ThreadLocalRandom.current().nextInt(min, max);
                            weatherInfoSubset[i] = weatherInfo[randomIndex];
                        }
                    }
                    else
                        weatherInfoSubset = weatherInfo;

                    // Weather name
                    for (String weatherFileName: weatherInfoSubset)
                    {
                        String weatherKey = weatherFileName.split("\\.")[0] + "_" + season + "_" + cropCode;

                        // To use below
                        daysToFloweringByCultivar.put(cropCode + cultivarCode, new int[]{0, 0});

                        // Planting dates to use
                        if (plantingDatesToSimulate.containsKey(weatherKey))
                        {
                            int pd = (Integer) plantingDatesToSimulate.get(weatherKey);
                            if (pd <= 0) pd = pdMean;
                            int finalThreadID = threadID;
                            int finalPd = pd;

                            // Multithreading
                            Future<Integer> future = executor.submit(new ThreadFloweringRuns(o, finalThreadID, weatherFileName, finalPd, cultivarOption, plantingDateOptionLabel, co2, firstPlantingYear));
                            futures.add(future);
                            threadID++;

                            if (threadID==numberOfThreads) threadID = 0;
                        }

                    }

                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

            // Execute and retrieve the exit code
            for (Future<Integer> future: futures)
            {
                try
                {
                    int exitCode = future.get();
                    if (exitCode>0) System.out.println("> Failed to find good flowering dates...");
                }
                catch(InterruptedException | ExecutionException | NumberFormatException e)
                {
                    e.printStackTrace();
                }
            }

            // Shutdown the executor
            executor.shutdown();
            try
            {
                // Wait for all tasks to complete before continuing.
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                {
                    // Cancel currently executing tasks
                    executor.shutdownNow();
                }
            }
            catch (InterruptedException ex)
            {
                // Cancel if current thread also interrupted
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Collect output files
            String[] csvFileNames = getFileNames(directoryFloweringDates, "CSV");

            // For each CSV file
            TreeMap<Object, Object> dtfMap = new TreeMap<>();
            TreeMap<Object, Object> dthMap = new TreeMap<>();
            for (String csvFileName : csvFileNames)
            {

                // Status
                System.out.println("> Analyzing " + csvFileName + "...");

                // Reading in
                Reader in = new FileReader(directoryFloweringDates + csvFileName);
                Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
                for (CSVRecord record : records)
                {

                    // Parse the cultivar code from TNAM
                    String cropCultivarCode = record.get("CR") + record.get("TNAM").substring(0, 6);

                    // Parse PDAT and ADAT for computing the "days to flowering"
                    int dtf, dth, pDDD = 0, aDDD = 0, hDDD = 0;
                    if (record.get("PDAT").length() > 4)
                        pDDD = Integer.parseInt(record.get("PDAT").substring(4));
                    if (record.get("ADAT").length() > 4)
                        aDDD = Integer.parseInt(record.get("ADAT").substring(4));
                    if (record.get("HDAT").length() > 4)
                        hDDD = Integer.parseInt(record.get("HDAT").substring(4));
                    if (pDDD>0 && aDDD>0 && hDDD>0)
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

                        //System.out.println(dtf+", "+dth);

                        // Storing
                        try
                        {
                            if (dtf>0 && dth>0 && dtf<dth)
                            {
                                if (dtfMap.containsKey(cropCultivarCode))
                                {
                                    // DTF
                                    ArrayList<Object> dtfValues = (ArrayList<Object>)dtfMap.get(cropCultivarCode);
                                    dtfValues.add(dtf);
                                    dtfMap.put(cropCultivarCode, dtfValues);

                                    // DTH
                                    ArrayList<Object> dthValues = (ArrayList<Object>)dthMap.get(cropCultivarCode);
                                    dthValues.add(dth);
                                    dthMap.put(cropCultivarCode, dthValues);
                                }
                                else
                                {
                                    // DTF
                                    ArrayList<Object> dtfValues = new ArrayList<>();
                                    dtfValues.add(dtf);
                                    dtfMap.put(cropCultivarCode, dtfValues);

                                    // DTH
                                    ArrayList<Object> dthValues = new ArrayList<>();
                                    dthValues.add(dth);
                                    dthMap.put(cropCultivarCode, dthValues);
                                }

                            }

                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }

                    }

                } // For each row in the CSV file

            } // For each CSV file

            // List of unique cropCultivarCode
            ArrayList<Object> cropCultivarList = new ArrayList<>(dtfMap.keySet());
            int temp;
            for(int i=0; i<cropCultivarList.size(); i++)
            {
                String c = (String)cropCultivarList.get(i);
                ArrayList<Integer> dtfValues = (ArrayList<Integer>) dtfMap.get(c);
                ArrayList<Integer> dthValues = (ArrayList<Integer>) dthMap.get(c);

                // Mean of DTF values
                temp = 0;
                for (int j=0; j<dtfValues.size(); j++)
                    temp = temp + dtfValues.get(j);
                int dtfMean = temp / dtfValues.size();

                // Mean of DTH values
                temp = 0;
                for (int j=0; j<dthValues.size(); j++)
                    temp = temp + dthValues.get(j);
                int dthMean = temp / dthValues.size();

                // Storing
                daysToFloweringByCultivar.put(c, new int[]{ dtfMean, dthMean });
            }

            // Adding some default values to avoid errors
            int dtfAvg = 60, dthAvg = 120;
            daysToFloweringByCultivar.put("DEFAULT", new int[]{ dtfAvg, dthAvg });

            // Writing a CSV output file
            if (printDaysToFlowering)
            {
                dataDaysToFlowering = directoryFloweringDates + fileDaysToFlowering + "_" + climateOption + ".csv";
                System.out.println("> Writing " + dataDaysToFlowering + "...");
                try (FileWriter writer = new FileWriter(dataDaysToFlowering);
                     CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                             .withHeader("CultivarCode", "AvgDaysToFlowering", "AvgDaysToHarvest")))
                {

                    // Writing
                    for (Map.Entry<Object, Object> entry : daysToFloweringByCultivar.entrySet())
                    {
                        String key = (String) entry.getKey();
                        int[] value = (int[]) entry.getValue();
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



    // Run seasonal simulations
    public static void runSeasonalSimulations(Object[] unitInfo, String[] weatherInfo,
                                              TreeMap<Object, Object> plantingDatesToSimulate, String climateOption,
                                              TreeMap<Object, Object> daysToFloweringByCultivar,
                                              int firstPlantingYear, TreeMap<Integer, Integer> co2History)
            throws ExecutionException, InterruptedException
    {
        int numberOfUnits = unitInfo.length;

        // Multithreading
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder().setNameFormat("%d");
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads, threadFactoryBuilder.build());
        List<Future<Integer>> futures = new ArrayList<>();

        // Looping through units
        for (int i = 0; i < numberOfUnits; i = i + 1)
        {

            // Status
            String progress = climateOption + ", R" + (i+1) + "/" + numberOfUnits;

            // Subset
            if (step4 && numberOfUnits>0)
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
                        int daysToFlowering, daysToHarvest;
                        try
                        {
                            daysToFlowering = ((int[])daysToFloweringByCultivar.get(cropCultivarCode))[0];
                            daysToHarvest = ((int[])daysToFloweringByCultivar.get(cropCultivarCode))[1];
                        }
                        catch(Exception e)
                        {
                            daysToFlowering = ((int[])daysToFloweringByCultivar.get("DEFAULT"))[0];
                            daysToHarvest = ((int[])daysToFloweringByCultivar.get("DEFAULT"))[1];
                            System.out.println("> Default phenology values used for "+cropCultivarCode);
                        }

                        // Multiple weather files for this unit
                        for (String weatherFileName: weatherInfo)
                        {
                            String weatherKey = weatherFileName.split("\\.")[0] + "_" + season + "_" + cropCode;
                            int p = (int)plantingDatesToSimulate.get(weatherKey);
                            Object[] weatherAndPlantingDate = { weatherFileName, p };

                            // Multiple threads
                            Future<Integer> future = executor.submit(new ThreadSeasonalRuns(ou, weatherAndPlantingDate, cultivarOption, daysToFlowering, daysToHarvest, climateOption, progress, firstPlantingYear, co2History));
                            futures.add(future);
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
        for (Future<Integer> future: futures)
        {
            future.get();
        }

        // Shutdown the executor
        executor.shutdown();
        try
        {
            // Wait for all tasks to complete before continuing.
            if (!executor.awaitTermination(60, TimeUnit.SECONDS))
            {
                // Cancel currently executing tasks
                executor.shutdownNow();
            }
        }
        catch (InterruptedException ex)
        {
            // Cancel if current thread also interrupted
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

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
        return unitInfo.toArray(Object[]::new);
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
        return dir.list(filter);
    }
    static String[] getFileNames(String filePath, String filteringText, int limitForDebugging)
    {
        File dir = new File(filePath);
        FilenameFilter filter = (directory, name) -> (name.toUpperCase().contains(filteringText.toUpperCase()));
        String[] out;
        if (limitForDebugging>0)
            out = Arrays.stream(dir.list(filter)).limit(limitForDebugging).toArray(String[]::new);
        else
            out = Arrays.stream(dir.list(filter)).toArray(String[]::new);
        return out;
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
