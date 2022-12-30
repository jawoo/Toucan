package org.cgiar.toucan;

import java.io.*;
import java.util.concurrent.Callable;

public class ThreadFloweringRuns implements Callable<Integer>
{
    int exitCode = 0;
    Object[] o;
    int threadID;
    String weatherFileName;
    int pd;
    Object[] cultivarOption;
    String pdateOption;
    int co2;
    int firstPlantingYear;

    ThreadFloweringRuns(Object[] o, int threadID, String weatherFileName, int pd, Object[] cultivarOption, String pdateOption, int co2, int firstPlantingYear)
    {
        this.o = o;
        this.threadID = threadID;
        this.weatherFileName = weatherFileName;
        this.pd = pd;
        this.cultivarOption = cultivarOption;
        this.pdateOption = pdateOption;
        this.co2 = co2;
        this.firstPlantingYear = firstPlantingYear;
    }

    public Integer call()
    {

        // To return
        int exitCode = 0;

        // Status
        System.out.println("> Flowering at T"+threadID+", "+weatherFileName+", "+pdateOption+", "+cultivarOption[1]+", "+cultivarOption[3]);

        // Modeling unit information
        String soilProfileID = (String)o[2];        // SoilProfileID
        String soilProfile = (String)o[3];          // SoilProfile
        int soilRootingDepth = (Integer)o[4];       // SoilRootingDepth

        // Copy weather file
        boolean weatherFound = false;
        try
        {

            // Delete the previous copy
            String wtgFileName = App.directoryWorking+"T"+threadID+App.d+"WEATHERS.WTG";
            File wtgFile = new File(wtgFileName);
            wtgFile.delete();

            // Get a new copy for this simulation
            File weatherSource = new File(App.directoryWeather+weatherFileName);
            File weatherDestination = new File(wtgFileName);
            Utility.copyFileUsingStream(weatherSource, weatherDestination);
            weatherFound = true;
        }
        catch (IOException e)
        {
            //e.printStackTrace();
            System.out.println("> Flowering: Weather file NOT copied at "+weatherFileName);
        }

        // Proceed only when the weather file is ready
        if (weatherFound)
        {

            // Write soil profile
            soilProfile = Utility.updateSoilProfileDepth(soilProfile, soilRootingDepth);

            // Write soil file
            String soilFile = App.directoryWorking+"T"+threadID+ App.d+soilProfileID.substring(0,2)+".SOL";
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

            // Write SNX file
            try
            {
                SnxWriterFloweringRuns.runningTreatmentPackages(threadID, o, pd, cultivarOption, co2, firstPlantingYear);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            // Run it
            exitCode = ExeRunner.dscsm048(threadID, "N");

            // Copy output file
            try
            {
                File outputSource = new File(App.directoryWorking+"T"+threadID+App.d+"summary.csv");
                File outputDestination = new File(App.directoryFloweringDates+weatherFileName.split("\\.")[0]+"_"+pdateOption+"_"+cultivarOption[1]+"_"+cultivarOption[2]+".csv");
                outputDestination.setReadable(true, false);
                outputDestination.setExecutable(true, false);
                outputDestination.setWritable(true, false);
                Utility.copyFileUsingStream(outputSource, outputDestination);
            }
            catch (FileNotFoundException e)
            {
                System.out.println("> Flowering: summary.csv file NOT found at "+weatherFileName);
            }
            catch (IOException e)
            {
                System.out.println("> Flowering simulation error occurred at "+weatherFileName);
            }

        }

        // Return
        return exitCode;

    }

}
