package org.cgiar.toucan;

import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;

import static java.nio.file.Files.*;

public class Utility
{

    static public DecimalFormat dfSLOC = new DecimalFormat("00.00");

    // Rooting depth adjustment
    static String updateSoilOrganicCarbonContents(String soilProfile)
    {
        String crlf	= System.getProperty("line.separator");
        String[] soilProfileByLine = soilProfile.split(crlf);
        int hc27ID = Integer.parseInt(soilProfileByLine[2].substring(55,57).trim());
        double[] slocByLayer = getSlocByLayer(hc27ID);
        StringBuilder soilProfileModified = new StringBuilder();

        for (int s=0; s<6; s++)
            soilProfileModified.append(soilProfileByLine[s]).append(crlf);

        for (int s=6; s<soilProfileByLine.length; s++)
        {
            String soilLayer = soilProfileByLine[s];
            String soilLayerBeforeSLOC = soilLayer.substring(0, 49);
            String updatedSLOC = dfSLOC.format(slocByLayer[s-6]);
            String soilLayerAfterSLOC = soilLayer.substring(54);
            soilProfileModified.append(soilLayerBeforeSLOC).append(updatedSLOC).append(soilLayerAfterSLOC).append(crlf);
        }

        return soilProfileModified.toString();
    }


    // SLOC from HC27
    static double[] getSlocByLayer(int hc27ID)
    {
        double[] slocByLayer = new double[7];
        switch (hc27ID)
        {
            case 1:
            case 2:
            case 3:
            case 10:
            case 11:
            case 12:
            case 19:
            case 20:
            case 21:
                slocByLayer[0] = 1.40;
                slocByLayer[1] = 0.87;
                slocByLayer[2] = 0.69;
                slocByLayer[3] = 0.63;
                slocByLayer[4] = 0.60;
                slocByLayer[5] = 0.53;
                slocByLayer[6] = 0.30;
                break;
            case 4:
            case 5:
            case 6:
            case 13:
            case 14:
            case 15:
            case 22:
            case 23:
            case 24:
                slocByLayer[0] = 1.00;
                slocByLayer[1] = 0.62;
                slocByLayer[2] = 0.49;
                slocByLayer[3] = 0.45;
                slocByLayer[4] = 0.43;
                slocByLayer[5] = 0.38;
                slocByLayer[6] = 0.21;
                break;
            case 7:
            case 8:
            case 9:
            case 16:
            case 17:
            case 18:
            case 25:
            case 26:
            case 27:
                slocByLayer[0] = 0.40;
                slocByLayer[1] = 0.25;
                slocByLayer[2] = 0.20;
                slocByLayer[3] = 0.18;
                slocByLayer[4] = 0.17;
                slocByLayer[5] = 0.15;
                slocByLayer[6] = 0.09;
                break;
        }
        return slocByLayer;
    }


    // Rooting depth adjustment
    static String updateSoilProfileDepth(String soilProfile, int slbMax)
    {
        DecimalFormat dfDDD = new DecimalFormat("000");
        String crlf	= System.getProperty("line.separator");
        String[] soilProfileByLine = soilProfile.split(crlf);
        StringBuilder soilProfileModified = new StringBuilder();

        // Setting the minimum depth as 40 cm, which is the median value of SLB_MAX for SSA for the shallow soils (0-90 cm; see HC.SOL)
        slbMax = Math.max(slbMax, 40);

        boolean slbMaxFound = false;
        for (int s=0; s<6; s++)
            soilProfileModified.append(soilProfileByLine[s]).append(crlf);
        for (int s=6; s<soilProfileByLine.length; s++)
        {
            if (!slbMaxFound)
            {
                int slb = Integer.parseInt(soilProfileByLine[s].substring(3,6).trim());
                if (slb<slbMax)
                    soilProfileModified.append(soilProfileByLine[s]).append(crlf);
                else
                {
                    soilProfileModified.append("   ").append(dfDDD.format(slbMax)).append(soilProfileByLine[s].substring(6)).append(crlf);
                    slbMaxFound = true;
                }
            }
        }
        return soilProfileModified.toString();
    }



    // Copying files using Stream
    static void copyFileUsingStream(File source, File dest) throws IOException
    {
        try (InputStream is = newInputStream(source.toPath()); OutputStream os = Files.newOutputStream(dest.toPath()))
        {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }



    // List of cultivar codes
    public static ArrayList<String> getCultivarCodes(String cropCode)
    {
        int counter = 1;
        ArrayList<String> cultivarList = new ArrayList<>();
        String modelNameVersion = getModelNameVersion(cropCode);
        try
        {
            File file = new File(App.directorySource + cropCode + modelNameVersion + ".CUL");
            Scanner sc = new Scanner(file);
            while(sc.hasNextLine())
            {
                String line = sc.nextLine();
                if (line.length()>70)
                {
                    String supposedlyCultivarCode = line.substring(0,6).replaceAll("\\s", "");
                    String supposedlySpace = line.substring(6,7);
                    String supposedlyCultivarName = line.substring(7,24).trim();
                    String flag = line.substring(line.length()-1).trim();

                    if (supposedlyCultivarCode.length()==6
                            && (supposedlySpace).equals(" ")
                            && flag.equals("*"))
                    {
                        String cultivarCodeAndName = supposedlyCultivarCode+" "+supposedlyCultivarName;
                        if (App.numberOfCultivars>0)
                        {
                            if (counter<=App.numberOfCultivars)
                                cultivarList.add(cultivarCodeAndName);
                        }
                        else
                        {
                            cultivarList.add(cultivarCodeAndName);
                        }
                        counter++;
                    }
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        return cultivarList;
    }



    // Look-up table for the model and version number
    public static String getModelNameVersion(String cropCode)
    {
        String model = "";
        String version = "048";
        if (String.valueOf(cropCode).equals("BA"))
            model = "CER";
        else if (String.valueOf(cropCode).equals("FB"))
            model = "GRO";
        else if (String.valueOf(cropCode).equals("CH"))
            model = "GRO";
        else if (String.valueOf(cropCode).equals("MZ"))
            model = "CER";
        else if (String.valueOf(cropCode).equals("SG"))
            model = "CER";
        else if (String.valueOf(cropCode).equals("TF"))
            model = "APS";
        else if (String.valueOf(cropCode).equals("WH"))
            model = "CER";
        return model+version;
    }



    // Cultivar-level information
    public static int[] getCultivarManagementInformation(String cropCode)
    {
        int plantingDensityHigh = 0;
        int recommendedNitrogenRate = 0;
        if (String.valueOf(cropCode).equals("BA"))
        {
            plantingDensityHigh = 200;
            recommendedNitrogenRate = 80;
        }
        else if (String.valueOf(cropCode).equals("FB"))
        {
            plantingDensityHigh = 30;
            recommendedNitrogenRate = 40;
        }
        else if (String.valueOf(cropCode).equals("CH"))
        {
            plantingDensityHigh = 32;
            recommendedNitrogenRate = 12;
        }
        else if (String.valueOf(cropCode).equals("MZ"))
        {
            plantingDensityHigh = 8;
            recommendedNitrogenRate = 60;
        }
        else if (String.valueOf(cropCode).equals("SG"))
        {
            plantingDensityHigh = 20;
            recommendedNitrogenRate = 100;
        }
        else if (String.valueOf(cropCode).equals("TF"))
        {
            plantingDensityHigh = 900;
            recommendedNitrogenRate = 60;
        }
        else if (String.valueOf(cropCode).equals("WH"))
        {
            plantingDensityHigh = 250;
            recommendedNitrogenRate = 70;
        }
        int plantingDensityLow = plantingDensityHigh/2;

        // Override the planting density with an average value
        if (App.useAvgPlantingDensity)
        {
            int avg = (int)(plantingDensityHigh*0.75);
            plantingDensityLow = avg;
            plantingDensityHigh = avg;
        }
        return new int[]{ plantingDensityHigh, plantingDensityLow, recommendedNitrogenRate };
    }



    // Convert month to the midday of the month
    public static String getPlantingDate(String plantingMonth)
    {
        int pm = Integer.parseInt(plantingMonth);
        int pd = 0;
        switch (pm)
        {
            case 1:  pd = 15; break;
            case 2:  pd = 46; break;
            case 3:  pd = 74; break;
            case 4:  pd = 105; break;
            case 5:  pd = 135; break;
            case 6:  pd = 166; break;
            case 7:  pd = 196; break;
            case 8:  pd = 227; break;
            case 9:  pd = 258; break;
            case 10: pd = 288; break;
            case 11: pd = 319; break;
            case 12: pd = 349; break;
        }
        return String.valueOf(pd);
    }

}
