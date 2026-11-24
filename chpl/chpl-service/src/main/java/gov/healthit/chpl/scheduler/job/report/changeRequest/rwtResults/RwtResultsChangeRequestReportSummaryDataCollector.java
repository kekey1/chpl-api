package gov.healthit.chpl.scheduler.job.report.changeRequest.rwtResults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.changerequest.dao.ChangeRequestListingUrlDAO;
import gov.healthit.chpl.changerequest.domain.ChangeRequestStatusConcept;
import gov.healthit.chpl.changerequest.domain.ChangeRequestType;
import gov.healthit.chpl.changerequest.search.ChangeRequestSearchRequest;
import gov.healthit.chpl.changerequest.search.ChangeRequestSearchResult;
import gov.healthit.chpl.changerequest.search.ChangeRequestSearchService;
import gov.healthit.chpl.dao.CertificationBodyDAO;
import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.domain.IdNamePair;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.realworldtesting.domain.RealWorldTestingReport;
import gov.healthit.chpl.realworldtesting.manager.RealWorldTestingReportService;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2(topic = "rwtResultsChangeRequestReportCreatorJobLogger")
public class RwtResultsChangeRequestReportSummaryDataCollector {
    private ChangeRequestSearchService changeRequestSearchService;
    private ChangeRequestListingUrlDAO changeRequestListingUrlDAO;
    private CertificationBodyDAO acbDao;
    private RealWorldTestingReportService rwtReportService;

    @Autowired
    public RwtResultsChangeRequestReportSummaryDataCollector(ChangeRequestSearchService changeRequestSearchService,
            ChangeRequestListingUrlDAO changeRequestListingUrlDAO,
            CertificationBodyDAO acbDao,
            RealWorldTestingReportService rwtReportService) {
        this.changeRequestSearchService = changeRequestSearchService;
        this.changeRequestListingUrlDAO = changeRequestListingUrlDAO;
        this.acbDao = acbDao;
        this.rwtReportService = rwtReportService;
    }

    public List<RwtResultsChangeRequestSummary> collect(List<Long> acbIds) {
        LocalDate today = LocalDate.now();
        List<CertificationBody> acbs = getAcbs(acbIds);
        Integer currentRwtEligibilityYear = LocalDate.now().getYear() - 1;
        LocalDateTime rwtResultsReportingPeriodStart = rwtReportService.getResultsStartDate(currentRwtEligibilityYear).atStartOfDay();
        LocalDateTime rwtResultsReportingPeriodEnd = rwtReportService.getResultsDataGatheringStopDate(currentRwtEligibilityYear).atTime(LocalTime.MAX);
        List<RealWorldTestingReport> rwtReportsForListingEligibleThisYearOrEarlier = rwtReportService.getRealWorldTestingReports(acbIds, LOGGER).stream()
                .filter(rwtReport -> rwtReport.getRwtEligibilityYear() != null && rwtReport.getRwtEligibilityYear() <= currentRwtEligibilityYear)
                .collect(Collectors.toList());

        List<ChangeRequestSearchResult> rwtResultsChangeRequestsDuringPeriod = changeRequestSearchService.getAllPagesOfSearchResults(ChangeRequestSearchRequest.builder()
                .acbIds(acbIds.stream().collect(Collectors.toSet()))
                .changeRequestTypeNames(Stream.of(ChangeRequestType.RWT_RESULTS_TYPE).collect(Collectors.toSet()))
                .submittedDateTimeStart(DateTimeFormatter.ofPattern(ChangeRequestSearchRequest.TIMESTAMP_SEARCH_FORMAT_PATTERN).format(rwtResultsReportingPeriodStart))
                .submittedDateTimeEnd(DateTimeFormatter.ofPattern(ChangeRequestSearchRequest.TIMESTAMP_SEARCH_FORMAT_PATTERN).format(rwtResultsReportingPeriodEnd))
                .build(), LOGGER);

        return acbs.stream()
                .map(acb -> RwtResultsChangeRequestSummary.builder()
                    .reportDate(today)
                    .reportingWindowStart(rwtResultsReportingPeriodStart.toLocalDate())
                    .reportingWindowEnd(rwtResultsReportingPeriodEnd.toLocalDate())
                    .certificationBody(acb)
                    .applicableListingCount(calculateApplicableListingCount(acb, rwtReportsForListingEligibleThisYearOrEarlier))
                    .approvedCount(calculateApprovedCount(acb, rwtResultsChangeRequestsDuringPeriod))
                    .pendingAcbActionCount(calculatePendingAcbActionCount(acb, rwtResultsChangeRequestsDuringPeriod))
                    .pendingDeveloperActionCount(calculatePendingDeveloperActionCount(acb, rwtResultsChangeRequestsDuringPeriod))
                    .noSubmissionInWindowCount(calculateNoSubmissionInWindowCount(acb, rwtReportsForListingEligibleThisYearOrEarlier, rwtResultsReportingPeriodStart, rwtResultsReportingPeriodEnd))
                    .build())
                .collect(Collectors.toList());
    }

    private List<CertificationBody> getAcbs(List<Long> acbIds) {
        return acbIds.stream()
                .map(acbId -> {
                    try {
                        return acbDao.getById(acbId);
                    } catch (EntityRetrievalException ex) {
                        LOGGER.error("Could not find ACB with ID " + acbId);
                        return null;
                    }
                })
                .filter(acb -> acb != null)
                .collect(Collectors.toList());
    }

    private Long calculateApplicableListingCount(CertificationBody acb, List<RealWorldTestingReport> rwtReportsForListingEligibleThisYearOrEarlier) {
        return Long.valueOf(rwtReportsForListingEligibleThisYearOrEarlier.stream()
                .filter(rwtReport -> rwtReport.getAcbName().equals(acb.getName()))
                .count());
    }

    private Long calculateApprovedCount(CertificationBody acb, List<ChangeRequestSearchResult> rwtChangeRequestsDuringReportingPeriod) {
        return rwtChangeRequestsDuringReportingPeriod.stream()
                .filter(cr -> containsAcb(acb, cr.getCertificationBodies()))
                .filter(cr -> cr.getCurrentStatus().getName().equals(ChangeRequestStatusConcept.ACCEPTED.getName()))
                .count();
    }

    private boolean containsAcb(CertificationBody acb, List<IdNamePair> searchResultsAcbs) {
        return searchResultsAcbs.stream()
                .map(sracb -> sracb.getId())
                .filter(acbId -> acbId.equals(acb.getId()))
                .findAny()
                .isPresent();
    }

    private Long calculatePendingAcbActionCount(CertificationBody acb, List<ChangeRequestSearchResult> rwtChangeRequestsDuringReportingPeriod) {
        return rwtChangeRequestsDuringReportingPeriod.stream()
                .filter(cr -> containsAcb(acb, cr.getCertificationBodies()))
                .filter(cr -> cr.getCurrentStatus().getName().equals(ChangeRequestStatusConcept.PENDING_ONC_ACB_ACTION.getName()))
                .count();
    }

    private Long calculatePendingDeveloperActionCount(CertificationBody acb, List<ChangeRequestSearchResult> rwtChangeRequestsDuringReportingPeriod) {
        return rwtChangeRequestsDuringReportingPeriod.stream()
                .filter(cr -> containsAcb(acb, cr.getCertificationBodies()))
                .filter(cr -> cr.getCurrentStatus().getName().equals(ChangeRequestStatusConcept.PENDING_DEVELOPER_ACTION.getName()))
                .count();
    }

    private Long calculateNoSubmissionInWindowCount(CertificationBody acb, List<RealWorldTestingReport> rwtReportsForListingEligibleThisYearOrEarlier, LocalDateTime reportingPeriodStart, LocalDateTime reportingPeriodEnd) {
        List<Long> eligibleListingIdsForAcb = rwtReportsForListingEligibleThisYearOrEarlier.stream()
            .filter(rwtReport -> rwtReport.getAcbName().equals(acb.getName()))
            .map(rwtReport -> rwtReport.getListingId())
            .collect(Collectors.toList());
        return changeRequestListingUrlDAO.getCountOfListingsWithNoChangeRequestsBetweenDates(eligibleListingIdsForAcb,
                ChangeRequestType.RWT_RESULTS_TYPE, reportingPeriodStart, reportingPeriodEnd);
    }
}
