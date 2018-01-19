package com.pearson.eidetic.api;

import com.google.common.io.CharStreams;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import com.pearson.eidetic.globals.GlobalVariables;
import com.pearson.eidetic.utilities.StackTrace;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.threads.subthreads.SnapshotVolumeSync;
import com.pearson.eidetic.utilities.Threads;
import java.util.Arrays;
import javafx.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Judah Walker
 */
@WebServlet(name = "API_SyncSnapshot", urlPatterns = {"/api/syncsnapshot"})
public class SyncSnapshot extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SyncSnapshot.class.getName());

    public static final String PAGE_NAME = "SyncSnapshot API";

    private static final ArrayList<Pair<String, Region>> regions = new ArrayList<Pair<String, Region>>() {
        {
            add(new Pair("AP_NORTHEAST_1", Regions.AP_NORTHEAST_1));
            add(new Pair("AP_NORTHEAST_2", Regions.AP_NORTHEAST_2));
            add(new Pair("AP_SOUTHEAST_1", Regions.AP_SOUTHEAST_1));
            add(new Pair("AP_SOUTHEAST_2", Regions.AP_SOUTHEAST_2));
            add(new Pair("AP_SOUTH_1", Regions.AP_SOUTH_1));
            add(new Pair("CA_CENTRAL_1", Regions.CA_CENTRAL_1));
            add(new Pair("CN_NORTH_1", Regions.CN_NORTH_1));
            add(new Pair("EU_CENTRAL_1", Regions.EU_CENTRAL_1));
            add(new Pair("EU_WEST_1", Regions.EU_WEST_1));
            add(new Pair("EU_WEST_2", Regions.EU_WEST_2));
            add(new Pair("GovCloud", Regions.GovCloud));
            add(new Pair("SA_EAST_1", Regions.SA_EAST_1));
            add(new Pair("US_EAST_1", Regions.US_EAST_1));
            add(new Pair("US_EAST_2", Regions.US_EAST_2));
            add(new Pair("US_WEST_1", Regions.US_WEST_1));
            add(new Pair("US_WEST_2", Regions.US_WEST_2));

        }
    };

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processGetRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        processPostRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return PAGE_NAME;
    }

    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response) {

        if ((request == null) || (response == null)) {
            return;
        }

        PrintWriter out = null;

        try {
            response.setContentType("text/html");
            out = response.getWriter();
            out.println("<head>" + "</head>" + "<body><h1>" + PAGE_NAME + "</h1></body>");
        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response) {

        if ((request == null) || (response == null)) {
            return;
        }

        PrintWriter out = null;
        String ipAddress;

        try {
            response.setContentType("text/json");

            boolean containsAccountId = false, containsRegion = false, containsVolumes = false;
            if ((request.getParameterMap() != null) && (request.getParameterMap().keySet() != null) && !request.getParameterMap().keySet().isEmpty()) {
                if (request.getParameterMap().keySet().contains("accountid")) {
                    containsAccountId = true;
                }
                if (request.getParameterMap().keySet().contains("region")) {
                    containsRegion = true;
                }
                if (request.getParameterMap().keySet().contains("volumes")) {
                    containsVolumes = true;
                }
            }
            String responseMessage;
            if ((containsRegion && containsAccountId && containsVolumes)) {
                try {
                    ipAddress = request.getHeader("X-FORWARDED-FOR");
                    if (ipAddress == null) {
                        ipAddress = " \"ipaddress_0\" : \"" + request.getRemoteAddr() + "\",";  //Gets last proxy send through, should default to something amazon, such as 23.22.117.91
                    } else {
                        String[] parts = ipAddress.split(",");
                        ipAddress = "";
                        for (int i = 0; i < parts.length; i++) {
                            ipAddress = ipAddress + " \"ipaddress_" + String.valueOf(i) + "\" : \"" + parts[i] + "\",";
                        }
                    }
                } catch (Exception e) {
                    ipAddress = "";
                    logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
                }

                responseMessage = syncSansphots(request, ipAddress, containsAccountId, containsRegion);

                if (!(responseMessage == null) && !StringUtils.startsWithIgnoreCase(responseMessage, "Error")) {
                    response.setStatus(200);
                    out = response.getWriter();
                    out.println(responseMessage);
                } else {
                    response.setStatus(400);
                }

            } else {
                responseMessage = "Error: Missing required parameter";
                response.setStatus(400);
            }

            out = response.getWriter();

            out.println(responseMessage);

        } catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            try {
                response.sendError(500, "Internal Server Error");
            } catch (Exception ex) {
                logger.error(ex.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(ex));
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static String syncSansphots(HttpServletRequest request, String ipAddress, boolean containsAccountId, boolean containsRegion) {
        String responseMessage = null;
        JSONParser parser = new JSONParser();

        String accountId = "";
        try {
            accountId = request.getParameter("accountid");
        } catch (Exception e) {
            responseMessage = "Error: accountid not found in request for syncsnapshot api call.";
            logger.error("awsAccounId=\"not found\",Error=\"" + responseMessage + "\""
                    + ", ipAddress=\"" + ipAddress + "\", Stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return responseMessage;
        }

        String region = "";
        try {
            region = request.getParameter("region");
        } catch (Exception e) {
            responseMessage = "Error: region not found in request for syncsnapshot api call";
            logger.error("awsAccountId=\"" + accountId + "\",Error=\"" + responseMessage + "\""
                    + ", ipAddress=\"" + ipAddress + "\", Stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return responseMessage;
        }

        String volumeParam;
        try {
            volumeParam = request.getParameter("volumes");
        } catch (Exception e) {
            responseMessage = "Error: volumes not found in request for syncsnapshot api call.";
            logger.error("awsAccountId=\"" + accountId + "\",Error=\"" + responseMessage + "\""
                    + ", ipAddress=\"" + ipAddress + "\", Stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
            return responseMessage;
        }

        ArrayList<String> listVolumes = new ArrayList<>(Arrays.asList(volumeParam.split(",")));

        if (!listVolumes.isEmpty()) {

            AwsAccount myAccount = null;
            for (AwsAccount awsAccount : ApplicationConfiguration.getAwsAccounts()) {
                if (awsAccount.getAwsAccountId().equalsIgnoreCase(accountId)) {
                    myAccount = awsAccount;
                }
            }

            if (myAccount == null) {
                responseMessage = "Error: AWS Account Id " + accountId + " not found referenced in Eidetic. Inspect http post and Eidetic's application.conf";
                logger.error("awsAccountId=\"" + accountId + "\",Error=\"" + responseMessage + "\""
                        + ", ipAddress=\"" + ipAddress + "\"");
                return responseMessage;
            }

            Region myRegion = null;
            for (Pair pair : regions) {
                String testRegion = (String) pair.getKey();
                if (StringUtils.containsIgnoreCase(region, testRegion)) {
                    Regions vregions = (Regions) pair.getValue();
                    myRegion = Region.getRegion(vregions);
                    break;
                }
            }

            if (myRegion == null) {
                responseMessage = "Error: Region " + region + " not found in Eidetic. Inspect http post and Eidetic's documentation to ensure api coherence.";
                logger.error("awsAccountId=\"" + accountId + "\",Error=\"" + responseMessage + "\""
                        + ", ipAddress=\"" + ipAddress + "\"");
                return responseMessage;
            }

            ArrayList<Volume> myVolumes = new ArrayList<>();
            for (Volume vol : myAccount.getVolumeSync_Copy().get(myRegion)) {
                for (String vols : listVolumes) {
                    if (vol.getVolumeId().equalsIgnoreCase(vols)) {
                        myVolumes.add(vol);
                        listVolumes.remove(vols);
                        break;
                    }
                }
            }

            if (!(listVolumes.isEmpty())) {
                responseMessage = "Error: Volume(s) not found in provided region/account:";
                for (String vols : listVolumes) {
                    responseMessage = responseMessage + " " + vols;
                }
                logger.error("awsAccountId=\"" + accountId + "\",Error=\"" + responseMessage + "\""
                        + ", ipAddress=\"" + ipAddress + "\"");
                return responseMessage;
            }

            SnapshotVolumeSync thread = new SnapshotVolumeSync(
                    myAccount.getAwsAccessKeyId(),
                    myAccount.getAwsSecretKey(),
                    myAccount.getUniqueAwsAccountIdentifier(),
                    myAccount.getMaxApiRequestsPerSecond(),
                    ApplicationConfiguration.getAwsCallRetryAttempts(),
                    myRegion,
                    myVolumes,
                    false);

            try {
                thread.run();
            } catch (Exception e) {
                responseMessage = "Error: exception in snapshot sub thread action.";
                logger.error("awsAccountId=\"" + accountId + "\",Error=\"" + responseMessage + "\""
                        + ", ipAddress=\"" + ipAddress + "\", Stacktrace=\"" + e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e) + "\"");
                return responseMessage;
            }

            Integer wait = 0;
            while (!(thread.isFinished()) && (wait <= 200)) {
                Threads.sleepMilliseconds(250);
                //break after 50 seconds
                wait = wait + 1;
            }

            if (wait <= 200) {
                responseMessage = "Success";
            } else {
                responseMessage = "Error: snapshot action timeout.";
            }

            return responseMessage;
        } else {
            responseMessage = "Error: No volumes found in post.";
            logger.error("awsAccountId=\"" + accountId + "\",Error=\"" + responseMessage + "\""
                    + ", ipAddress=\"" + ipAddress + "\"\"");
            return responseMessage;
        }

    }
}
