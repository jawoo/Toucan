package org.cgiar.toucan;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Date;

public class ExeRunner
{

    static DecimalFormat dfTT  = new DecimalFormat("00");

    // Run DSSAT
    public static int dscsm048_flowering(int threadID, String runMode)
    {

        int exitCode = 0;

        try
        {

            // Execution using ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(App.directoryThreads+"T"+threadID));

            // OS dependent
            if (App.isWindows())
                pb.command("CMD.EXE", "/C", "DSCSM048.EXE "+runMode+" DSSBatch.v48 >NUL");
            else if (App.isUnix())
                pb.command("bash", "-c", "./DSCSM048.EXE "+runMode+" DSSBatch.v48 >/dev/null");

            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ( (line = r.readLine()) != null)
            {
                System.out.println(line);
            }

            // Error?
            exitCode = p.waitFor();
            File error = new File(App.directoryThreads+"T"+threadID+App.d+"ERROR.OUT");
            if (error.exists())
            {

                // Copy problematic files
                Date date = new Date();
                long timeStamp = date.getTime();
                try
                {

                    // SNX file
                    File outputSource = new File(App.directoryThreads+"T"+threadID+App.d+"TOUCAN"+dfTT.format(threadID)+".S"+runMode+"X");
                    if (outputSource.exists())
                    {
                        File outputDestination = new File(App.directoryError+"F_TOUCAN"+threadID+"_"+timeStamp+".S"+runMode+"X");
                        outputDestination.setReadable(true, false);
                        outputDestination.setExecutable(true, false);
                        outputDestination.setWritable(true, false);
                        Utility.copyFileUsingStream(outputSource, outputDestination);
                    }

                    // INP file
                    File inpSource = new File(App.directoryThreads+"T"+threadID+App.d+"DSSAT48.INP");
                    if (inpSource.exists())
                    {
                        File inpDestination = new File(App.directoryError+"F_ERROR_"+threadID+"_"+timeStamp+"_DSSAT48.INP");
                        inpDestination.setReadable(true, false);
                        inpDestination.setExecutable(true, false);
                        inpDestination.setWritable(true, false);
                        Utility.copyFileUsingStream(inpSource, inpDestination);
                    }

                    // WARNING file
                    File warningSource = new File(App.directoryThreads+"T"+threadID+App.d+"WARNING.OUT");
                    if (warningSource.exists())
                    {
                        File warningDestination = new File(App.directoryError+"F_WARNING_"+threadID+"_"+timeStamp+".OUT");
                        warningDestination.setReadable(true, false);
                        warningDestination.setExecutable(true, false);
                        warningDestination.setWritable(true, false);
                        Utility.copyFileUsingStream(warningSource, warningDestination);
                    }

                    // Error file
                    File errorSource = new File(App.directoryThreads+"T"+threadID+App.d+"ERROR.OUT");
                    if (errorSource.exists())
                    {
                        File errorDestination = new File(App.directoryError+"F_ERROR_"+threadID+"_"+timeStamp+".OUT");
                        errorDestination.setReadable(true, false);
                        errorDestination.setExecutable(true, false);
                        errorDestination.setWritable(true, false);
                        Utility.copyFileUsingStream(errorSource, errorDestination);
                    }

                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                System.out.println("> Thread " + threadID + ": Error code " + exitCode);
                //System.exit(0);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return exitCode;

    }
    public static int dscsm048_seasonal(String runMode)
    {

        int threadID = Integer.parseInt(Thread.currentThread().getName());
        int exitCode = 0;

        try
        {

            // Execution using ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(App.directoryThreads+"T"+threadID));

            // OS dependent
            if (App.isWindows())
                pb.command("CMD.EXE", "/C", "DSCSM048.EXE "+runMode+" DSSBatch.v48 >NUL");
            else if (App.isUnix())
                pb.command("bash", "-c", "./DSCSM048.EXE "+runMode+" DSSBatch.v48 >/dev/null");

            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ( (line = r.readLine()) != null)
            {
                System.out.println(line);
            }

            // Error?
            exitCode = p.waitFor();
            File error = new File(App.directoryThreads+"T"+threadID+App.d+"ERROR.OUT");
            if (error.exists())
            {

                // Copy problematic files
                try
                {

                    // Copy problematic files
                    Date date = new Date();
                    long timeStamp = date.getTime();
                    try
                    {

                        // SNX file
                        File outputSource = new File(App.directoryThreads+"T"+threadID+App.d+"TOUCAN"+dfTT.format(threadID)+".S"+runMode+"X");
                        if (outputSource.exists())
                        {
                            File outputDestination = new File(App.directoryError+"F_TOUCAN"+threadID+"_"+timeStamp+".S"+runMode+"X");
                            outputDestination.setReadable(true, false);
                            outputDestination.setExecutable(true, false);
                            outputDestination.setWritable(true, false);
                            Utility.copyFileUsingStream(outputSource, outputDestination);
                        }

                        // INP file
                        File inpSource = new File(App.directoryThreads+"T"+threadID+App.d+"DSSAT48.INP");
                        if (inpSource.exists())
                        {
                            File inpDestination = new File(App.directoryError+"F_ERROR_"+threadID+"_"+timeStamp+"_DSSAT48.INP");
                            inpDestination.setReadable(true, false);
                            inpDestination.setExecutable(true, false);
                            inpDestination.setWritable(true, false);
                            Utility.copyFileUsingStream(inpSource, inpDestination);
                        }

                        // WARNING file
                        File warningSource = new File(App.directoryThreads+"T"+threadID+App.d+"WARNING.OUT");
                        if (warningSource.exists())
                        {
                            File warningDestination = new File(App.directoryError+"F_WARNING_"+threadID+"_"+timeStamp+".OUT");
                            warningDestination.setReadable(true, false);
                            warningDestination.setExecutable(true, false);
                            warningDestination.setWritable(true, false);
                            Utility.copyFileUsingStream(warningSource, warningDestination);
                        }

                        // Error file
                        File errorSource = new File(App.directoryThreads+"T"+threadID+App.d+"ERROR.OUT");
                        if (errorSource.exists())
                        {
                            File errorDestination = new File(App.directoryError+"F_ERROR_"+threadID+"_"+timeStamp+".OUT");
                            errorDestination.setReadable(true, false);
                            errorDestination.setExecutable(true, false);
                            errorDestination.setWritable(true, false);
                            Utility.copyFileUsingStream(errorSource, errorDestination);
                        }

                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                    System.out.println("> Thread " + threadID + ": Error code " + exitCode);
                    //System.exit(0);

                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                System.out.println("> Thread " + threadID + ": Error code " + exitCode);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return exitCode;

    }

}
