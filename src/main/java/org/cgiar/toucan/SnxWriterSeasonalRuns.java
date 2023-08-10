package org.cgiar.toucan;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.TreeMap;

public class SnxWriterSeasonalRuns
{

    static DecimalFormat dfTT    = new DecimalFormat("00");
    static DecimalFormat dfDDD   = new DecimalFormat("000");
    static DecimalFormat dfXYCRD = new DecimalFormat("+000.00;-000.00");

    public static void runningTreatmentPackages(
            Object[] o,
            String waterManagement,
            int nRate,
            int manureRate,
            Object[] cultivarOption,
            int daysToFlowering,
            int daysToHarvest,
            String pdensityOption,
            int residueHarvestPct,
            int co2,
            Object[] weatherAndPlantingDate,
            String label,
            int firstPlantingYear
            ) throws InterruptedException {

        // Thread ID?
        int threadID = Integer.parseInt(Thread.currentThread().getName());

        // YY
        String yy = String.valueOf(firstPlantingYear).substring(2);

        // Unit information
        String soilProfileID = (String)o[2];
        int soilRootingDepth = (Integer)o[4];
        double x = (double)o[10];
        double y = (double)o[11];

        // Cultivar code
        String cropCode = ((String)cultivarOption[1]);

        // Boolean switches
        boolean isRice = cropCode.equals("RI");
        boolean isWheat = cropCode.equals("WH");
        boolean isIrrigated = waterManagement.equals("I");

        // Treatments
        String snxSectionTreatments = "\n*TREATMENTS                        -------------FACTOR LEVELS------------\n" +
                "@N R O C TNAME.................... CU FL SA IC MP MI MF MR MC MT ME MH SM\n";
        String mi = "0", mf = "0", mr = "0", mh = "0";
        if (isIrrigated || isRice) mi = "2";
        if (nRate>0) mf = "1";
        if (manureRate>0) mr = "1";
        if (residueHarvestPct<100 || isWheat) mh = "1";

        // Fields
        String idField = cultivarOption[1]+(String)cultivarOption[2];
        String snxSectionFieldLevel1 = "\n*FIELDS\n@L ID_FIELD WSTA....  FLSA  FLOB  FLDT  FLDD  FLDS  FLST SLTX  SLDP  ID_SOIL    FLNAME\n";
        String snxSectionFieldLevel2 = "@L ...........XCRD ...........YCRD .....ELEV .............AREA .SLEN .FLWR .SLAS FLHST FHDUR\n";

        // Fertilizer
        int splitFertilizerDate = daysToFlowering; //30;
        int splitFertilizerRate = nRate/2;
        String snxSectionFertilizer = "\n*FERTILIZERS (INORGANIC)\n" +
                "@F FDATE  FMCD  FACD  FDEP  FAMN  FAMP  FAMK  FAMC  FAMO  FOCD FERNAME\n" +
                " 1     1 FE001 AP001    10   "+dfDDD.format(splitFertilizerRate)+"     0     0     0     0   -99 -99\n" +
                " 1   "+dfDDD.format(splitFertilizerDate)+" FE001 AP001    10   "+dfDDD.format(splitFertilizerRate)+"     0     0     0     0   -99 -99\n";

        // Filling the treatment and field sections
        int tn = 1;

        // Planting
        StringBuilder snxSectionPlantingDetails = new StringBuilder("\n*PLANTING DETAILS\n" +
                "@P PDATE EDATE  PPOP  PPOE  PLME  PLDS  PLRS  PLRD  PLDP  PLWT  PAGE  PENV  PLPH  SPRL                        PLNAME\n");

        // Irrigation
        StringBuilder snxSectionIrrigation = new StringBuilder("\n*IRRIGATION AND WATER MANAGEMENT\n");
        boolean irrigationSectionWritten = false;

        // Batch
        StringBuilder batch = new StringBuilder("$BATCH(SEQUENCE)\n" +
                "\n" +
                "@FILEX                                                                                        TRTNO     RP     SQ     OP     CO\n");

        // Retrieval
        int pdate = (Integer)weatherAndPlantingDate[1];
        String weatherCode = weatherAndPlantingDate[0].toString().substring(0,4);

        snxSectionTreatments +=
                dfTT.format(tn)+" 1 0 0 "+label+weatherCode+"            1 "+dfTT.format(tn)+"  0  1 "+dfTT.format(tn)+"  "+mi+"  "+mf+"  "+mr+"  0  0  1  "+mh+"  1\n";

        snxSectionFieldLevel1 +=
                dfTT.format(tn)+" "+idField+" "+"WEATHERS"+"   -99     0 IB000     0     0 00000 -99    180  "+soilProfileID+" -99\n";

        snxSectionFieldLevel2 +=
                dfTT.format(tn)+"         "+dfXYCRD.format(x)+"         "+dfXYCRD.format(y)+"         0                 0     0     0     0 FH102    30\n";

        // Planting Details
        String pdt = dfDDD.format(pdate);

        // Planting density
        String plantingDensity;

        // Low vs high density
        if (String.valueOf(pdensityOption).equals("DL"))
            plantingDensity = dfDDD.format((int)cultivarOption[4]);
        else
            plantingDensity = dfDDD.format((int)cultivarOption[5]);

        // Rice vs non-rice
        if (isRice)
            snxSectionPlantingDetails.append(dfTT.format(tn)).append(" ").append(yy).append(pdt).append("   -99   ").append(plantingDensity).append("   ").append(plantingDensity).append("     T     H    20     0     2     0    23    25     3     0                        -99\n");
        else
            snxSectionPlantingDetails.append(dfTT.format(tn)).append(" ").append(yy).append(pdt).append("   -99   ").append(plantingDensity).append("   ").append(plantingDensity).append("     S     R    61     0     7   -99   -99   -99   -99     0                        -99\n");

        // Irrigation
        if (!irrigationSectionWritten)
        {
            if (isRice)
            {
                if (daysToFlowering>10)
                {
                    snxSectionIrrigation.append("@I  EFIR  IDEP  ITHR  IEPT  IOFF  IAME  IAMT IRNAME\n" + " 1     1   -99   -99   -99   -99   -99   -99 -99\n" + "@I IDATE  IROP IRVAL\n" + " 1 ").append(yy).append(pdt).append(" IR008     2\n").append(" 1 ").append(yy).append(pdt).append(" IR010     0\n").append(" 1 ").append(yy).append(pdt).append(" IR009   150\n").append(" 1 ").append(yy).append(pdt).append(" IR003    30\n").append("@I  EFIR  IDEP  ITHR  IEPT  IOFF  IAME  IAMT IRNAME\n").append(" 2     1   -99   -99   -99   -99   -99   -99 -99\n").append("@I IDATE  IROP IRVAL\n").append(" 2 ").append(yy).append(pdt).append(" IR008     2\n").append(" 2 ").append(yy).append(pdt).append(" IR010     0\n").append(" 2 ").append(yy).append(pdt).append(" IR009   150\n").append(" 2 ").append(yy).append(pdt).append(" IR011    30\n");

                    // Paddy rice setting
                        /*
                        ! Irrigation Codes: IRRCOD
                        ! 1:  Furrow irrigation of specified amount (mm)
                        ! 2:  Alternating furrows; irrigation of specified amount (mm)
                        ! 3:  Flood irrigation of specified amount (mm)
                        ! 4:  Sprinkler irrigation of specified amount (mm)
                        ! 5:  Drip or trickle irrigation of specified amount (mm)
                        ! 6:  Single irrigation to specified total flood depth (mm)
                        ! 7:  Water table depth (cm)
                        ! 8:  Percolation rate (mm/d)
                        ! 9:  Bund height (mm)
                        ! 10: Puddling (Puddled if IRRCOD = 10 record is present)
                        ! 11: Maintain constant specified flood depth (mm)
                        */
                    int floodIrrigationDate = pdate + 5;
                    String irrigationYear = yy;
                    if (floodIrrigationDate>365)
                    {
                        floodIrrigationDate = floodIrrigationDate - 365;
                        irrigationYear = String.valueOf(Integer.parseInt(yy)+1);
                    }
                    if (isIrrigated)
                    {
                        snxSectionIrrigation.append(" 2 ").append(irrigationYear).append(dfDDD.format(floodIrrigationDate)).append(" IR011   100\n");   // Maintain constant specified flood depth (mm)
                    }
                    else
                    {
                        snxSectionIrrigation.append(" 2 ").append(irrigationYear).append(dfDDD.format(floodIrrigationDate)).append(" IR003   100\n");   // Flood irrigation of specified amount (mm)
                    }
                }
            }
            else
            {
                snxSectionIrrigation.append("@I  EFIR  IDEP  ITHR  IEPT  IOFF  IAME  IAMT IRNAME\n" + " 1     1   -99   -99   -99   -99   -99   -99 -99\n" + "@I IDATE  IROP IRVAL\n" + " 1   ").append(dfDDD.format(1)).append(" IR001   100\n").append("@I  EFIR  IDEP  ITHR  IEPT  IOFF  IAME  IAMT IRNAME\n").append(" 2     1   -99   -99   -99   -99   -99   -99 -99\n").append("@I IDATE  IROP IRVAL\n");

                // When to irrigate?
                TreeMap<Integer, Integer> irrigation = new TreeMap<>();
                irrigation.put(1, 10);   // Planting date

                if (daysToFlowering>10)
                {
                    irrigation.put(daysToFlowering-1, 15);
                    irrigation.put(daysToFlowering,   15);  // Also the split fertilizer application date
                    irrigation.put(daysToFlowering+3, 15);
                }

                // Additional supplementary irrigation after flowering
                for (int d=daysToFlowering+10; d<daysToHarvest-20; d=d+5)
                    irrigation.put(d, 10);

                for (Map.Entry<Integer, Integer> entry : irrigation.entrySet())
                    snxSectionIrrigation.append(" 2   ").append(dfDDD.format(entry.getKey())).append(" IR001   ").append(dfDDD.format(entry.getValue())).append("\n");

            }
        }

        // Batch file
        batch.append("TOUCAN").append(dfTT.format(threadID)).append(".SNX                                                                                     ").append(dfTT.format(tn)).append("      1      0      0      0\n");

        // Initial Conditions
        String icdat = yy+"001";
        String icbl = dfDDD.format(soilRootingDepth);
        String icwd = dfDDD.format(soilRootingDepth/2);
        String snxSectionInitialConditions = "\n*INITIAL CONDITIONS\n";
        if (isIrrigated || isRice)
        {
            snxSectionInitialConditions +=
                    "@C   PCR ICDAT  ICRT  ICND  ICRN  ICRE  ICWD ICRES ICREN ICREP ICRIP ICRID ICNAME\n" +
                    " 1    FA "+icdat+"   100     0     1     1   "+icwd+"  1000    .8     0   100    15 -99\n" +
                    "@C  ICBL  SH2O  SNH4  SNO3\n" +
                    " 1   " + icbl + "  .500  .001  .001\n";
        }
        else
        {
            snxSectionInitialConditions +=
                    "@C   PCR ICDAT  ICRT  ICND  ICRN  ICRE  ICWD ICRES ICREN ICREP ICRIP ICRID ICNAME\n" +
                    " 1    FA "+icdat+"   100     0     1     1   001  1000    .8     0   100    15 -99\n" +
                    "@C  ICBL  SH2O  SNH4  SNO3\n" +
                    " 1   " + icbl + "  .001  .001  .001\n";
        }

        // Environment modifications
        String snxSectionEnvironmentModification = "\n*ENVIRONMENT MODIFICATIONS\n" +
                "@E "+"ODATE "+"EDAY  ERAD  EMAX  EMIN  ERAIN ECO2 "+  "EDEW  EWIND ENVNAME  \n" +
                " 1 "+ icdat+" A 0.0 A 0.0 A   0 A   0 A 0.0 R "+co2+" A   0 A   0 \n"; //" 1 "+ icdat+ " A 0.0 A 0.0 A   0 A   0 M 0.5 R "+co2+" A   0 A   0 \n";

        // Harvest details
        int harvestDate = 365;
        if (harvestDate<1) harvestDate = 1;
        String hd = dfDDD.format(harvestDate);
        String hbpc = dfDDD.format(residueHarvestPct);
        String snxHarvest = "\n*HARVEST DETAILS\n" +
                "@H HDATE  HSTG  HCOM HSIZE   HPC  HBPC HNAME\n" +
                " 1 "+dfTT.format(Integer.parseInt(yy)+1)+hd+" GS000   -99   -99   100   "+hbpc+"\n" +
                " 2 "+dfTT.format(Integer.parseInt(yy)+1)+hd+" GS000   -99   -99   100   "+hbpc+"\n";

        // Organic amendment
        String snxSectionManure = "\n*RESIDUES AND ORGANIC FERTILIZER\n" +
                "@R RDATE  RCOD  RAMT  RESN  RESP  RESK  RINP  RDEP  RMET RENAME\n" +
                " 1 "+yy+"001 RE003  1000   1.4    .2  2.38    20    15 AP003 -99\n";

        // Simulation controls
        String irrig = "D";  if (isRice)  irrig = "R";
        String harvs = "M";  if (isWheat) harvs = "R";
        String nyers = "01"; //if (isWheat) nyers = "05";
        String snxSectionSimulationControls = "\n*SIMULATION CONTROLS\n" +
                "@N GENERAL     NYERS NREPS START SDATE RSEED SNAME.................... SMODEL\n" +
                " 1 GE             "+nyers+"     1     S "+icdat+"  4537 CROP\n" +
                "@N OPTIONS     WATER NITRO SYMBI PHOSP POTAS DISES  CHEM  TILL   CO2\n" +
                " 1 OP              Y     Y     N     N     N     N     N     N     D\n" +
                "@N METHODS     WTHER INCON LIGHT EVAPO INFIL PHOTO HYDRO NSWIT MESOM MESEV MESOL\n" +
                " 1 ME              G     M     E     R     S     C     R     1     P     S     2\n" +
                "@N MANAGEMENT  PLANT IRRIG FERTI RESID HARVS\n" +
                " 1 MA              R     "+irrig+"     D     R     "+harvs+"\n" +
                "@N OUTPUTS     FNAME OVVEW SUMRY FROPT GROUT CAOUT WAOUT NIOUT MIOUT DIOUT VBOSE CHOUT OPOUT FMOPT\n" +
                " 1 OU              N     N     Y     3     N     N     N     N     N     N     0     N     N     C\n" +
                "@  AUTOMATIC MANAGEMENT\n" +
                "@N PLANTING    PFRST PLAST PH2OL PH2OU PH2OD PSTMX PSTMN\n" +
                " 1 PL          "+icdat+" "+icdat+"    40   100    30    40    10\n" +
                "@N IRRIGATION  IMDEP ITHRL ITHRU IROFF IMETH IRAMT IREFF\n" +
                " 1 IR             30    70   100 IB001 IB001    20   .75\n" +
                "@N NITROGEN    NMDEP NMTHR NAMNT NCODE NAOFF\n" +
                " 1 NI             30    50    25 IB001 IB001\n" +
                "@N RESIDUES    RIPCN RTIME RIDEP\n" +
                " 1 RE            100     1    20\n" +
                "@N HARVEST     HFRST HLAST HPCNP HPCNR\n" +
                " 1 HA              0 79065   100   "+hbpc+"\n" +
                "@N GENERAL     NYERS NREPS START SDATE RSEED SNAME.................... SMODEL\n" +
                " 2 GE              1     1     S "+icdat+"  2150 FALLOW\n" +
                "@N OPTIONS     WATER NITRO SYMBI PHOSP POTAS DISES  CHEM  TILL   CO2\n" +
                " 2 OP              Y     Y     N     N     N     N     N     N     D\n" +
                "@N METHODS     WTHER INCON LIGHT EVAPO INFIL PHOTO HYDRO NSWIT MESOM MESEV MESOL\n" +
                " 2 ME              G     M     E     R     S     C     R     1     P     S     2\n" +
                "@N MANAGEMENT  PLANT IRRIG FERTI RESID HARVS\n" +
                " 2 MA              R     N     N     R     R\n" +
                "@N OUTPUTS     FNAME OVVEW SUMRY FROPT GROUT CAOUT WAOUT NIOUT MIOUT DIOUT VBOSE CHOUT OPOUT FMOPT\n" +
                " 2 OU              Y     N     A     5     N     N     N     N     N     N     N     N     N     A\n" +
                "@  AUTOMATIC MANAGEMENT\n" +
                "@N PLANTING    PFRST PLAST PH2OL PH2OU PH2OD PSTMX PSTMN\n" +
                " 2 PL          75169 75183    40   100    30    40    10\n" +
                "@N IRRIGATION  IMDEP ITHRL ITHRU IROFF IMETH IRAMT IREFF\n" +
                " 2 IR             30    70   100 IB001 IB001    20   .75\n" +
                "@N NITROGEN    NMDEP NMTHR NAMNT NCODE NAOFF\n" +
                " 2 NI             30    50    25 IB001 IB001\n" +
                "@N RESIDUES    RIPCN RTIME RIDEP\n" +
                " 2 RE            100     1    20\n" +
                "@N HARVEST     HFRST HLAST HPCNP HPCNR\n" +
                " 2 HA              0 79065   100   "+hbpc+"\n";

        // SNX
        String snx = "*EXP.DETAILS: TOUCAN"+dfTT.format(threadID)+"SN SEASONAL RUNS\n" +
                "\n" +
                "*GENERAL\n" +
                "\n" +
                snxSectionTreatments +
                "\n*CULTIVARS\n" +
                "@C CR INGENO CNAME\n" +
                " 1 "+cultivarOption[1]+" "+cultivarOption[2]+" "+cultivarOption[3]+"\n" +
                snxSectionFieldLevel1 +
                snxSectionFieldLevel2 +
                snxSectionInitialConditions +
                snxSectionPlantingDetails +
                snxSectionFertilizer +
                snxSectionIrrigation +
                snxSectionManure +
                snxSectionEnvironmentModification +
                snxHarvest +
                snxSectionSimulationControls;

        // Write
        String snxFile = App.directoryThreads+"T"+threadID+App.d+"TOUCAN"+dfTT.format(threadID)+".SNX";
        Utility.writeFile(snxFile, snx);

        // Write
        String batchFile = App.directoryThreads+"T"+threadID+App.d+"DSSBatch.v48";
        Utility.writeFile(batchFile, batch.toString());

    }

}
