package org.cgiar.toucan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.Callable;

public class ThreadSeasonalRuns implements Callable<Integer>
{

    int exitCode = 0;
    Object[] o;
    Object[] weatherAndPlantingDate;
    Object[] cultivarOption;
    int daysToFlowering;
    int daysToHarvest;
    String climateOption;
    String progress;
    int firstPlantingYear;
    TreeMap<Integer, Integer> co2History;

    ThreadSeasonalRuns(Object[] o, Object[] weatherAndPlantingDate, Object[] cultivarOption,
                       int daysToFlowering, int daysToHarvest, String climateOption,
                       String progress, int firstPlantingYear, TreeMap<Integer, Integer> co2History)
    {
        this.o = o;
        this.weatherAndPlantingDate = weatherAndPlantingDate;
        this.cultivarOption = cultivarOption;
        this.daysToFlowering = daysToFlowering;
        this.daysToHarvest = daysToHarvest;
        this.climateOption = climateOption;
        this.progress = progress;
        this.firstPlantingYear = firstPlantingYear;
        this.co2History = co2History;
    }

    @Override
    public Integer call()
    {

        // Modeling unit information
        int unitId = (Integer) o[0];
        int cell5m = (Integer) o[1];
        String soilProfileID = (String)o[2];
        String soilProfile = (String)o[3];
        int soilRootingDepth = (Integer)o[4];
        String season = (String)o[12];
        DecimalFormat dfTT = new DecimalFormat("00");

        // Thread ID?
        int threadID = Integer.parseInt(Thread.currentThread().getName());

        // Copy weather file
        boolean weatherFound = false;
        String weatherFileName = "";
        try
        {
            weatherFileName = (String)weatherAndPlantingDate[0];
            File weatherSource = new File(App.directoryWeather+weatherFileName);
            File weatherDestination = new File(App.directoryThreads+"T"+threadID+App.d+"WEATHERS.WTG");
            Utility.copyFileUsingStream(weatherSource, weatherDestination);
            weatherFound = true;
        }
        catch (InterruptedException exception)
        {
            System.out.println("> Seasonal runs: Weather file NOT copied: "+weatherFileName);
        }

        // Proceed only when the weather file is ready
        if (weatherFound)
        {

            // Write soil profile
            // soilProfile = Utility.updateSoilOrganicCarbonContents(soilProfile);  // <-- To handle erroneous SOC values in SoilGrids...
            soilProfile = Utility.updateSoilProfileDepth(soilProfile, soilRootingDepth);

            // Write soil file
            String soilFile = App.directoryThreads+"T"+threadID+ App.d+soilProfileID.substring(0,2)+".SOL";
            try
            {
                BufferedWriter writer = new BufferedWriter(new FileWriter(soilFile));
                writer.write(soilProfile);
                writer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            // Cultivar code
            String cropCode = ((String)cultivarOption[1]);
            String cultivarCode = ((String)cultivarOption[2]);

            // Planting date option
            String pdateOption;
            String pdensityOption;
            String runLabel;
            String waterManagement;

            // Recommended N application rate
            int recommendedNitrogenRate = (int)cultivarOption[6];
            int actualNitrogenRate = recommendedNitrogenRate/2;
            int[] nitrogenFertilizerRates = new int[]{ actualNitrogenRate, recommendedNitrogenRate };

            // Organic manure application (x1000)
            int[] manureRates = new int[]{ 0, 1 };

            // Residue harvest rate
            int[] residueHarvestPcts = new int[]{ 100, 0 };

            // Combinations?
            ArrayList<Object> scenarios = new ArrayList<>();
            if (App.scenarioCombinations)
            {

                // All combinations
                int wMax = 2, fMax = 2, mMax = 2, rMax = 2, pMax = 2, dMax = 2, cMax = 2;

                // Or not
                if (!App.switchScenarios[0]) wMax = 1;
                if (!App.switchScenarios[1]) fMax = 1;
                if (!App.switchScenarios[2]) mMax = 1;
                if (!App.switchScenarios[3]) rMax = 1;
                if (!App.switchScenarios[4]) pMax = 1;
                if (!App.switchScenarios[5]) dMax = 1;
                if (!App.switchScenarios[6]) cMax = 1;

                // Looping
                for (int w=0; w<wMax; w++)
                    for (int f=0; f<fMax; f++)
                        for (int m=0; m<mMax; m++)
                            for (int r=0; r<rMax; r++)
                                for (int p=0; p<pMax; p++)
                                    for (int d=0; d<dMax; d++)
                                        for (int c=0; c<cMax; c++)
                                        {
                                            int[] scn = { w, f, m, r, p, d, c };
                                            scenarios.add(scn);
                                        }

            }
            else
            {

                // One scenario for each choice
                /*
                0: Water Management
                1: Fertilizer
                2: Manure
                3: Residue
                4: Planting Window
                5: Planting Density
                6: CO2 Fertilization
                */
                for (int s=0; s<App.switchScenarios.length; s++)
                {
                    int switchWaterManagement = 0;
                    int switchFertilizer = 0;
                    int switchManure  = 0;
                    int switchResidue = 0;
                    int switchPlantingWindow = 0;
                    int switchPlantingDensity = 0;
                    int switchCO2Fertilization = 0;

                    switch (s)
                    {
                        case 0:
                            if (App.switchScenarios[s])
                            {
                                switchWaterManagement = 1;
                                int[] scn = { switchWaterManagement, switchFertilizer, switchManure, switchResidue, switchPlantingWindow, switchPlantingDensity, switchCO2Fertilization };
                                scenarios.add(scn);
                            }
                            break;
                        case 1:
                            if (App.switchScenarios[s])
                            {
                                switchFertilizer = 1;
                                int[] scn = { switchWaterManagement, switchFertilizer, switchManure, switchResidue, switchPlantingWindow, switchPlantingDensity, switchCO2Fertilization };
                                scenarios.add(scn);
                            }
                            break;
                        case 2:
                            if (App.switchScenarios[s])
                            {
                                switchManure = 1;
                                int[] scn = { switchWaterManagement, switchFertilizer, switchManure, switchResidue, switchPlantingWindow, switchPlantingDensity, switchCO2Fertilization };
                                scenarios.add(scn);
                            }
                            break;
                        case 3:
                            if (App.switchScenarios[s])
                            {
                                switchResidue = 1;
                                int[] scn = { switchWaterManagement, switchFertilizer, switchManure, switchResidue, switchPlantingWindow, switchPlantingDensity, switchCO2Fertilization };
                                scenarios.add(scn);
                            }
                            break;
                        case 4:
                            if (App.switchScenarios[s])
                            {
                                switchPlantingWindow = 1;
                                int[] scn = { switchWaterManagement, switchFertilizer, switchManure, switchResidue, switchPlantingWindow, switchPlantingDensity, switchCO2Fertilization };
                                scenarios.add(scn);
                            }
                            break;
                        case 5:
                            if (App.switchScenarios[s])
                            {
                                switchPlantingDensity = 1;
                                int[] scn = { switchWaterManagement, switchFertilizer, switchManure, switchResidue, switchPlantingWindow, switchPlantingDensity, switchCO2Fertilization };
                                scenarios.add(scn);
                            }
                            break;
                        case 6:
                            if (App.switchScenarios[s])
                            {
                                switchCO2Fertilization = 1;
                                int[] scn = { switchWaterManagement, switchFertilizer, switchManure, switchResidue, switchPlantingWindow, switchPlantingDensity, switchCO2Fertilization };
                                scenarios.add(scn);
                            }
                            break;
                        default:
                            break;
                    }

                }

            }

            // Scenario
            int ns = scenarios.size();
            for (int s=0; s<ns; s++)
            {

                // What to simulate this time
                int[] scn = (int[])scenarios.get(s);
                int switchWaterManagement = scn[0];
                int switchFertilizer = scn[1];
                int switchManure = scn[2];
                int switchResidue = scn[3];
                int switchPlantingWindow = scn[4];
                int switchPlantingDensity = scn[5];
                int switchCO2Fertilization = scn[6];

                // Treatment label
                String labelWithAezSeason = "W"+scn[0]+"F"+scn[1]+"C"+scn[6]+"|";

                // Settings
                int nRate = nitrogenFertilizerRates[switchFertilizer];
                int manureRate = manureRates[switchManure];
                int residueHarvestPct = residueHarvestPcts[switchResidue];
                int co2 = co2History.get(2000);

                // Water management and planting window
                if (switchPlantingWindow==0)
                {

                    // Median yields
                    pdateOption = "PM";
                    if (switchWaterManagement==0)
                    {
                        // 0: pdRainfedMax, 1: pdRainfedMedian, 2: pdIrrigatedMax, 3: pdIrrigatedMedian
                        waterManagement = "R";
                        //plantingDate = plantingDatesRainfed[1];
                    }
                    else
                    {
                        // 0: pdRainfedMax, 1: pdRainfedMedian, 2: pdIrrigatedMax, 3: pdIrrigatedMedian
                        waterManagement = "I";
                        //plantingDate = plantingDatesIrrigated[1];
                    }
                }
                else
                {

                    // Best yields
                    pdateOption = "PB";
                    if (switchWaterManagement==0)
                    {
                        // 0: pdRainfedMax, 1: pdRainfedMedian, 2: pdIrrigatedMax, 3: pdIrrigatedMedian
                        waterManagement = "R";
                        //plantingDate = plantingDatesRainfed[0];
                    }
                    else
                    {
                        // 0: pdRainfedMax, 1: pdRainfedMedian, 2: pdIrrigatedMax, 3: pdIrrigatedMedian
                        waterManagement = "I";
                        //plantingDate = plantingDatesIrrigated[0];
                    }
                }

                // Planting density option
                if (switchPlantingDensity==0)
                {
                    pdensityOption = "DL";
                }
                else
                {
                    pdensityOption = "DH";
                }

                // CO2 fertilization
                if (switchCO2Fertilization==1)
                {
                    int y = Integer.parseInt(climateOption.substring(0,4));
                    co2 = co2History.get(y);
                }

                // Status
                runLabel = "W" + switchWaterManagement + "-F" + switchFertilizer + "-M" + switchManure + "-R" + switchResidue + "-" + pdateOption + "-" + pdensityOption + "-" + cropCode + cultivarCode + "-CO2" + co2;

                // Run it
                try
                {
                    String weatherSequence = weatherAndPlantingDate[0].toString().substring(0,4);
                    SnxWriterSeasonalRuns.runningTreatmentPackages(o, waterManagement, nRate, manureRate, cultivarOption, daysToFlowering, daysToHarvest, pdensityOption, residueHarvestPct, co2, weatherAndPlantingDate, labelWithAezSeason, firstPlantingYear);
                    System.out.println("> T" + dfTT.format(threadID) + ", " + progress + ", S" + (s+1) + "/" + ns + ", " + runLabel + ", SEQ: " + weatherSequence);
                    exitCode = ExeRunner.dscsm048_seasonal("N");
                    if (exitCode == 0)
                    {
                        File outputSource = new File(App.directoryThreads + "T" + threadID + App.d + "summary.csv");
                        File outputDestination = new File(App.directoryOutput + "U" + unitId + "_C" + cell5m + "_" + climateOption + "_S" + s + "_" + runLabel + "_" + weatherSequence + ".csv");
                        outputDestination.setReadable(true, false);
                        outputDestination.setExecutable(true, false);
                        outputDestination.setWritable(true, false);
                        Utility.copyFileUsingStream(outputSource, outputDestination);
                    }
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    System.out.println("> Seasonal runs: Error at T" + threadID + " for S" + s + "_" + runLabel + "_" + season);
                }

            }

        }

        // Return
        return exitCode;

    }

}
