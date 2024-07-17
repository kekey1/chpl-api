package gov.healthit.chpl.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.healthit.chpl.domain.statistics.CuresCriterionChartStatistic;
import gov.healthit.chpl.report.ReportDataManager;
import gov.healthit.chpl.report.criteriamigrationreport.CriteriaMigrationReport;
import gov.healthit.chpl.util.SwaggerSecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "report-data", description = "Allows retrieval of data used by reports.")
@RestController
@RequestMapping("/report-data")
public class ReportDataController {
    private ReportDataManager reportDataManager;

    @Autowired
    public ReportDataController(ReportDataManager reportDataManager) {
        this.reportDataManager = reportDataManager;
    }

    @Operation(summary = "Retrieves the data used to generate the Cures Update Report.",
            description = "Retrieves the data used to generate the Cures Update Report.",
            security = {
                    @SecurityRequirement(name = SwaggerSecurityRequirement.API_KEY)
            })
    @RequestMapping(value = "/cures-update-report", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody List<CuresCriterionChartStatistic> getCuresUpdateReportData() {
        return reportDataManager.getCuresUpdateReportData();
    }

    @Operation(summary = "Retrieves the data used to generate the HTI-1 Criteria Migration Report.",
            description = "Retrieves the data used to generate the HTI-1 Criteria Migration Report.",
            security = {
                    @SecurityRequirement(name = SwaggerSecurityRequirement.API_KEY)
            })
    @RequestMapping(value = "/hti-1-criteria-migration-report", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody CriteriaMigrationReport getHti1CriteriaMigrationReport() {
        return reportDataManager.getHti1CriteriaMigrationReport();
    }
}