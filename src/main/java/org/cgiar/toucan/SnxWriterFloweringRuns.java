package org.cgiar.toucan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

public class SnxWriterFloweringRuns
{

    static DecimalFormat dfTT  = new DecimalFormat("00");
    static DecimalFormat dfDDD  = new DecimalFormat("000");

    public static void runningTreatmentPackages(
            int threadID,
            Object[] o,
            int pd,
            Object[] cultivarOption,
            int co2,
            int firstPlantingYear
            )
    {

        // Unit information
        String soilProfileID = (String)o[2];
        int soilRootingDepth = (Integer)o[4];

        // Cultivar code
        String cropCode = (String)cultivarOption[1];
        String cultivarCode = (String)cultivarOption[2];

        // YY
        String yy = String.valueOf(firstPlantingYear).substring(2);

        // Boolean switches
        boolean isRice;
        isRice = cropCode.equals("RI");

        // Plantng density (low)
        String plantingDensity = dfDDD.format((int)cultivarOption[4]);

        // Treatments
        String snxSectionTreatments = "\n*TREATMENTS                        -------------FACTOR LEVELS------------\n" +
                "@N R O C TNAME.................... CU FL SA IC MP MI MF MR MC MT ME MH SM\n" +
                "01 1 0 0 "+cultivarCode+"-P5-FXX-WX           1  1  0  1 01  0  0  0  0  0  1  0  1\n";

        // Fields
        String snxSectionFields = "\n*FIELDS\n" +
                "@L ID_FIELD WSTA....  FLSA  FLOB  FLDT  FLDD  FLDS  FLST SLTX  SLDP  ID_SOIL    FLNAME\n" +
                " 1 TOUCAN01 WEATHERS   -99     0 IB000     0     0 00000 -99    180  "+soilProfileID+" -99\n" +
                "@L ...........XCRD ...........YCRD .....ELEV .............AREA .SLEN .FLWR .SLAS FLHST FHDUR\n" +
                " 1               0               0         0                 0     0     0     0   -99   -99\n";

        // Initial Conditions
        String icdat = yy+"001";
        String icbl = dfDDD.format(soilRootingDepth);
        String snxSectionInitialConditions = "\n*INITIAL CONDITIONS\n" +
                "@C   PCR ICDAT  ICRT  ICND  ICRN  ICRE  ICWD ICRES ICREN ICREP ICRIP ICRID ICNAME\n" +
                " 1    "+cropCode+" "+icdat+"   100     0     1     1   180  1000    .8     0   100    15 -99\n" +
                "@C  ICBL  SH2O  SNH4  SNO3\n" +
                " 1   "+icbl+"  .500    .1    .1\n";

        // Planting Details
        StringBuilder snxSectionPlantingDetails = new StringBuilder("\n*PLANTING DETAILS\n" +
                "@P PDATE EDATE  PPOP  PPOE  PLME  PLDS  PLRS  PLRD  PLDP  PLWT  PAGE  PENV  PLPH  SPRL                        PLNAME\n");
        if (isRice)
            snxSectionPlantingDetails.append(dfTT.format(1)).append(" ").append(yy).append(dfDDD.format(pd)).append("   -99   ").append(plantingDensity).append("   ").append(plantingDensity).append("     T     H    20     0     2     0    23    25     3     0                        -99\n");
        else
            snxSectionPlantingDetails.append(dfTT.format(1)).append(" ").append(yy).append(dfDDD.format(pd)).append("   -99   ").append(plantingDensity).append("   ").append(plantingDensity).append("     S     R    61     0     7   -99   -99   -99   -99     0                        -99\n");

        // Environment modifications
        String snxSectionEnvironmentModification = "\n*ENVIRONMENT MODIFICATIONS\n" +
                "@E ODATE EDAY  ERAD  EMAX  EMIN  ERAIN ECO2  EDEW  EWIND ENVNAME  \n" +
                " 1 "+icdat+" A 0.0 A 0.0 A   0 A   0 A 0.0 R "+co2+" A   0 A   0 \n";

        // Simulation controls
        String snxSectionSimulationControls = "\n*SIMULATION CONTROLS\n" +
                "@N GENERAL     NYERS NREPS START SDATE RSEED SNAME.................... SMODEL\n" +
                " 1 GE              1     1     S "+icdat+"  4573\n" +
                "@N OPTIONS     WATER NITRO SYMBI PHOSP POTAS DISES  CHEM  TILL   CO2\n" +
                " 1 OP              N     N     N     N     N     N     N     N     D\n" +
                "@N METHODS     WTHER INCON LIGHT EVAPO INFIL PHOTO HYDRO NSWIT MESOM MESEV MESOL\n" +
                " 1 ME              G     M     E     R     S     C     R     1     G     S     2\n" +
                "@N MANAGEMENT  PLANT IRRIG FERTI RESID HARVS\n" +
                " 1 MA              R     D     D     N     M\n" +
                "@N OUTPUTS     FNAME OVVEW SUMRY FROPT GROUT CAOUT WAOUT NIOUT MIOUT DIOUT VBOSE CHOUT OPOUT FMOPT\n" +
                " 1 OU              N     Y     Y     3     N     N     N     N     N     N     0     N     N     C\n" +
                "\n" +
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
                " 1 HA              0 79065   100     0";

        // SNX
        String snx = "*EXP.DETAILS: TOUCAN0"+threadID+"SN FLOWERING DATES\n" +
                "\n" +
                "*GENERAL\n" +
                "\n" +
                snxSectionTreatments +
                "\n*CULTIVARS\n" +
                "@C CR INGENO CNAME\n" +
                " 1 "+cultivarOption[1]+" "+cultivarOption[2]+" "+cultivarOption[3]+"\n" +
                snxSectionFields +
                snxSectionInitialConditions +
                snxSectionPlantingDetails +
                snxSectionEnvironmentModification +
                snxSectionSimulationControls;

        // Write
        String snxFile = App.directoryThreads+"T"+threadID+ App.d+"TOUCAN"+dfTT.format(threadID)+".SNX";
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(snxFile));
            writer.write(snx);
            writer.close();
        }
        catch (IOException ex)
        {
            System.out.println("> Skipping a file due to the locked file exception...");
        }

        // Batch file
        StringBuilder batch = new StringBuilder("$BATCH(SEASONAL)\n" +
                "\n" +
                "@FILEX                                                                                        TRTNO     RP     SQ     OP     CO\n");
        batch.append("TOUCAN").append(dfTT.format(threadID)).append(".SNX                                                                                     ").append(dfTT.format(1)).append("      1      0      0      0\n");

        // Write
        String batchFile = App.directoryThreads+"T"+threadID+App.d+"DSSBatch.v48";
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(batchFile));
            writer.write(batch.toString());
            writer.close();
        }
        catch (IOException ex)
        {
            System.out.println("> Skipping a file due to the locked file exception...");
        }

    }

}
