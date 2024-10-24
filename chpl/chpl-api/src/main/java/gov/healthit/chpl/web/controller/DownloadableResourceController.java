package gov.healthit.chpl.web.controller;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ff4j.FF4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.healthit.chpl.FeatureList;
import gov.healthit.chpl.dao.CertifiedProductDAO;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.logging.Loggable;
import gov.healthit.chpl.manager.SurveillanceManager;
import gov.healthit.chpl.svap.manager.SvapManager;
import gov.healthit.chpl.util.ErrorMessageUtil;
import gov.healthit.chpl.util.FileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;

@Api
@RestController
@Loggable
@Log4j2
public class DownloadableResourceController {
    private Environment env;
    private ErrorMessageUtil msgUtil;
    private SurveillanceManager survManager;
    private CertifiedProductDAO cpDao;
    private SvapManager svapManager;
    private FF4j ff4j;
    private FileUtils fileUtils;

    @Value("${directReviewsReportName}")
    private String directReviewsReportName;

    @Value("${schemaDirectReviewsName}")
    private String directReviewsSchemaName;

    @Value("${schemaSvapReportName}")
    private String svapReportSchemaName;

    @Autowired
    public DownloadableResourceController(Environment env,
            ErrorMessageUtil msgUtil,
            SurveillanceManager survManager,
            CertifiedProductDAO cpDao,
            SvapManager svapManager,
            FF4j ff4j,
            FileUtils fileUtils) {
        this.env = env;
        this.msgUtil = msgUtil;
        this.survManager = survManager;
        this.cpDao = cpDao;
        this.svapManager = svapManager;
        this.ff4j = ff4j;
        this.fileUtils = fileUtils;
    }

    /**
     * Streams a file back to the end user for the specified edition (2011,2014,2015,etc)
     * in the specified format (xml or csv). Optionally will send back the definition
     * files instead. The file that is sent back is generated via a quartz job on a
     * regular basis.
     * @param editionInput 2011, 2014, or 2015
     * @param formatInput csv or xml
     * @param isDefinition whether to send back the data file or definition file
     * @param request http request
     * @param response http response, used to stream back the file
     * @throws IOException if the file cannot be read
     */
    @ApiOperation(value = "Download the entire CHPL as XML.",
            notes = "Once per day, the entire certified product listing is "
                    + "written out to XML files on the CHPL servers, one for each "
                    + "certification edition. This method allows any user to download "
                    + "that XML file. It is formatted in such a way that users may import "
                    + "it into Microsoft Excel or any other XML tool of their choosing. To download "
                    + "any one of the XML files, append ‘&edition=year’ to the end of the query string "
                    + "(e.g., &edition=2015). A separate query is required to download each of the XML files.")
    @RequestMapping(value = "/download", method = RequestMethod.GET, produces = "application/xml")
    public void downloadListingDetails(@RequestParam(value = "edition", required = false) String editionInput,
            @RequestParam(value = "format", defaultValue = "xml", required = false) String formatInput,
            @RequestParam(value = "definition", defaultValue = "false", required = false) Boolean isDefinition,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        //parse inputs
        String edition = editionInput;
        String format = formatInput;
        String responseType = "text/csv";
        String filenameToStream = null;

        if (!StringUtils.isEmpty(edition)) {
            // make sure it's a 4 character year
            edition = edition.trim();
            if (!edition.startsWith("20")) {
                edition = "20" + edition;
            }
        } else {
            edition = "all";
        }

        if (!StringUtils.isEmpty(format) && format.equalsIgnoreCase("csv")) {
            format = "csv";
        } else {
            format = "xml";
            responseType = "application/xml";
        }

        File toDownload = null;
        //if the user wants a definition file, find it
        if (isDefinition != null && isDefinition.booleanValue()) {
            if (format.equals("xml")) {
                toDownload = fileUtils.getDownloadFile(env.getProperty("schemaXmlName"));
            } else if (edition.equals("2014")) {
                toDownload = fileUtils.getDownloadFile(env.getProperty("schemaCsv2014Name"));
            } else if (edition.equals("2015")) {
                if (ff4j.check(FeatureList.RWT_ENABLED)) {
                    toDownload = fileUtils.getDownloadFile(env.getProperty("schemaCsv2015NameWithRWT"));
                } else {
                    toDownload = fileUtils.getDownloadFile(env.getProperty("schemaCsv2015Name"));
                }
                filenameToStream = env.getProperty("schemaCsv2015Name");
            }

            if (!toDownload.exists()) {
                response.getWriter()
                    .write(msgUtil.getMessage("resources.schemaFileNotFound", toDownload.getAbsolutePath()));
                return;
            }
        } else {
            File newestFileWithFormat = fileUtils.getNewestFileMatchingName("^chpl-" + edition + "-.+\\." + format + "$");
            if (newestFileWithFormat != null) {
                toDownload = newestFileWithFormat;
            } else {
                response.getWriter()
                    .write(msgUtil.getMessage("resources.fileWithEditionAndFormatNotFound", edition, format));
                return;
            }
        }

        LOGGER.info("Downloading " + toDownload.getName());
        if (filenameToStream != null) {
            fileUtils.streamFileAsResponse(toDownload, responseType, response, filenameToStream);
        } else {
            fileUtils.streamFileAsResponse(toDownload, responseType, response);
        }
    }

    @ApiOperation(value = "Download a summary of SVAP activity as a CSV.",
            notes = "Once per day, a summary of SVAP activity is written out to a CSV "
                    + "file on the CHPL servers. This method allows ROLE_ADMIN, ROLE_ONC, "
                    + "and ROLE_ONC_STAFF users to download that file.")
    @RequestMapping(value = "/svap/download", method = RequestMethod.GET, produces = "text/csv")
    public void downloadSvapSummary(
            @RequestParam(value = "definition", defaultValue = "false", required = false) Boolean isDefinition,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        File downloadFile = null;
        if (isDefinition != null && isDefinition.booleanValue()) {
            try {
                downloadFile = fileUtils.getDownloadFile(svapReportSchemaName);
            } catch (IOException ex) {
                response.getWriter().append(ex.getMessage());
                return;
            }
        } else {
            try {
                downloadFile = svapManager.getSvapSummaryFile();
            } catch (IOException ex) {
                response.getWriter().append(ex.getMessage());
                return;
            }
        }

        if (downloadFile == null) {
            response.getWriter().append(msgUtil.getMessage("resources.schemaFileGeneralError"));
            return;
        }
        if (!downloadFile.exists()) {
            response.getWriter().append(msgUtil.getMessage("resources.schemaFileNotFound", downloadFile.getAbsolutePath()));
            return;
        }

        LOGGER.info("Streaming " + downloadFile.getName());
        fileUtils.streamFileAsResponse(downloadFile, "text/csv", response);
    }

    @ApiOperation(value = "Download all SED details that are certified to 170.315(g)(3).",
            notes = "Download a specific file that is generated overnight.")
    @RequestMapping(value = "/certified_products/sed_details", method = RequestMethod.GET)
    public void streamSEDDetailsDocumentContents(HttpServletResponse response)
            throws EntityRetrievalException, IOException {
        File downloadFile = fileUtils.getNewestFileMatchingName("^" + env.getProperty("SEDDownloadName") + "-.+\\.csv$");
        fileUtils.streamFileAsResponse(downloadFile, "text/csv", response);
    }

    @ApiOperation(value = "Download all direct reviews as a CSV.",
            notes = "Once per day, all direct reviews are written out to a CSV "
                    + "file on the CHPL servers. This method allows any user to download that file.")
    @RequestMapping(value = "/developers/direct-reviews/download", method = RequestMethod.GET, produces = "text/csv")
    public void downloadDirectReviews(
            @RequestParam(value = "definition", defaultValue = "false", required = false) Boolean isDefinition,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        File downloadFile = null;
        if (isDefinition != null && isDefinition.booleanValue()) {
            try {
                downloadFile = fileUtils.getDownloadFile(directReviewsSchemaName);
            } catch (IOException ex) {
                response.getWriter().append(ex.getMessage());
                return;
            }
        } else {
            try {
                downloadFile = fileUtils.getNewestFileMatchingName("^" + directReviewsReportName + "-.+\\.csv$");
            } catch (IOException ex) {
                response.getWriter().append(ex.getMessage());
                return;
            }
        }

        if (downloadFile == null) {
            response.getWriter().append(msgUtil.getMessage("resources.schemaFileGeneralError"));
            return;
        }
        if (!downloadFile.exists()) {
            response.getWriter().append(msgUtil.getMessage("resources.schemaFileNotFound", downloadFile.getAbsolutePath()));
            return;
        }

        LOGGER.info("Streaming " + downloadFile.getName());
        fileUtils.streamFileAsResponse(downloadFile, "text/csv", response);
    }

    @ApiOperation(value = "Download surveillance as CSV.",
            notes = "Once per day, all surveillance and nonconformities are written out to CSV "
                    + "files on the CHPL servers. This method allows any user to download those files.")
    @RequestMapping(value = "/surveillance/download", method = RequestMethod.GET, produces = "text/csv")
    public void downloadSurveillance(@RequestParam(value = "type", required = false, defaultValue = "") final String type,
            @RequestParam(value = "definition", defaultValue = "false", required = false) final Boolean isDefinition,
            final HttpServletRequest request, final HttpServletResponse response)
                    throws IOException, EntityRetrievalException {

        File downloadFile = null;
        if (isDefinition != null && isDefinition.booleanValue()) {
            if (type.equalsIgnoreCase("basic")) {
                downloadFile = fileUtils.getDownloadFile(env.getProperty("schemaBasicSurveillanceName"));
            } else {
                downloadFile = fileUtils.getDownloadFile(env.getProperty("schemaSurveillanceName"));
            }
        } else {
            try {
                if (type.equalsIgnoreCase("all")) {
                    downloadFile = survManager.getAllSurveillanceDownloadFile();
                } else if (type.equalsIgnoreCase("basic")) {
                    downloadFile = survManager.getBasicReportDownloadFile();
                } else {
                    downloadFile = survManager.getSurveillanceWithNonconformitiesDownloadFile();
                }
            } catch (final IOException ex) {
                response.getWriter().append(ex.getMessage());
                return;
            }
        }

        if (downloadFile == null) {
            response.getWriter()
                .append(msgUtil.getMessage("resources.schemaFileGeneralError"));
            return;
        }
        if (!downloadFile.exists()) {
            response.getWriter()
            .write(msgUtil.getMessage("resources.schemaFileNotFound", downloadFile.getAbsolutePath()));
            return;
        }

        LOGGER.info("Downloading " + downloadFile.getName());
        fileUtils.streamFileAsResponse(downloadFile, "text/csv", response);
    }
}
