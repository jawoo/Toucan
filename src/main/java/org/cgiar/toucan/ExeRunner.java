package org.cgiar.toucan;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Date;

public class ExeRunner
{

    static DecimalFormat dfTT  = new DecimalFormat("00");

    // Run DSSAT
    public static int dscsm048(int threadID, String runMode)
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
                    File outputDestination = new File(App.directoryError+"TOUCAN"+threadID+"_"+timeStamp+".S"+runMode+"X");
                    outputDestination.setReadable(true, false);
                    outputDestination.setExecutable(true, false);
                    outputDestination.setWritable(true, false);
                    com.google.common.io.Files.copy(outputSource, outputDestination);

                    // Error file
                    File errorSource = new File(App.directoryThreads+"T"+threadID+App.d+"ERROR.OUT");
                    File errorDestination = new File(App.directoryError+"ERROR_"+threadID+"_"+timeStamp+".S"+runMode+"X");
                    errorDestination.setReadable(true, false);
                    errorDestination.setExecutable(true, false);
                    errorDestination.setWritable(true, false);
                    com.google.common.io.Files.copy(errorSource, errorDestination);

                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                System.out.println("> Thread " + threadID + ": Error code " + exitCode);
                System.exit(0);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return exitCode;

    }
    public static int dscsm048(String runMode)
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
                    DecimalFormat dfXX = new DecimalFormat("00");
                    String xx = dfXX.format(threadID);
                    File outputSource = new File(App.directoryThreads+"T"+threadID+App.d+"TOUCAN"+xx+".S"+runMode+"X");
                    File outputDestination = new File(App.directoryError+"TOUCAN"+xx+".S"+runMode+"X");
                    com.google.common.io.Files.copy(outputSource, outputDestination);
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
