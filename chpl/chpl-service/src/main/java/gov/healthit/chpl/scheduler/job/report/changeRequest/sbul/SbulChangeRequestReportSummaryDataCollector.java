package gov.healthit.chpl.scheduler.job.report.changeRequest.sbul;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.certificationCriteria.CertificationCriterion;
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
import gov.healthit.chpl.exception.ValidationException;
import gov.healthit.chpl.search.ListingSearchService;
import gov.healthit.chpl.search.domain.ListingSearchResult;
import gov.healthit.chpl.search.domain.SearchRequest;
import gov.healthit.chpl.service.CertificationCriterionService;
import gov.healthit.chpl.service.CertificationCriterionService.Criteria2015;
import gov.healthit.chpl.util.CertificationStatusUtil;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2(topic = "sbulChangeRequestReportCreatorJobLogger")
public class SbulChangeRequestReportSummaryDataCollector {
    private ChangeRequestSearchService changeRequestSearchService;
    private ListingSearchService listingSearchService;
    private ChangeRequestListingUrlDAO changeRequestListingUrlDAO;
    private CertificationBodyDAO acbDao;
    private CertificationCriterion g10;

    @Autowired
    public SbulChangeRequestReportSummaryDataCollector(ChangeRequestSearchService changeRequestSearchService,
            ListingSearchService listingSearchService,
            ChangeRequestListingUrlDAO changeRequestListingUrlDAO,
            CertificationCriterionService criteriaService,
            CertificationBodyDAO acbDao) {
        this.changeRequestSearchService = changeRequestSearchService;
        this.listingSearchService = listingSearchService;
        this.changeRequestListingUrlDAO = changeRequestListingUrlDAO;
        this.acbDao = acbDao;
        this.g10 = criteriaService.get(Criteria2015.G_10);
    }

    public List<SbulChangeRequestSummary> collect(List<Long> acbIds) {
        LocalDate today = LocalDate.now();
        List<CertificationBody> acbs = getAcbs(acbIds);
        LocalDateTime sbulReportingPeriodStart = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
        LocalDateTime sbulReportingPeriodEnd = today.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
        List<ListingSearchResult> sbulEligibleListings = getSbulEligibleListings(acbs);

        List<ChangeRequestSearchResult> sbulChangeRequestsDuringPeriod = changeRequestSearchService.getAllPagesOfSearchResults(ChangeRequestSearchRequest.builder()
                .acbIds(acbIds.stream().collect(Collectors.toSet()))
                .changeRequestTypeNames(Stream.of(ChangeRequestType.SBUL_TYPE).collect(Collectors.toSet()))
                .submittedDateTimeStart(DateTimeFormatter.ofPattern(ChangeRequestSearchRequest.TIMESTAMP_SEARCH_FORMAT_PATTERN).format(sbulReportingPeriodStart))
                .submittedDateTimeEnd(DateTimeFormatter.ofPattern(ChangeRequestSearchRequest.TIMESTAMP_SEARCH_FORMAT_PATTERN).format(sbulReportingPeriodEnd))
                .build(), LOGGER);

        return acbs.stream()
                .map(acb -> SbulChangeRequestSummary.builder()
                    .reportDate(today)
                    .reportingWindowStart(sbulReportingPeriodStart.toLocalDate())
                    .reportingWindowEnd(sbulReportingPeriodEnd.toLocalDate())
                    .certificationBody(acb)
                    .applicableListingCount(calculateApplicableListingCount(acb, sbulEligibleListings))
                    .approvedCount(calculateApprovedCount(acb, sbulChangeRequestsDuringPeriod))
                    .pendingAcbActionCount(calculatePendingAcbActionCount(acb, sbulChangeRequestsDuringPeriod))
                    .pendingDeveloperActionCount(calculatePendingDeveloperActionCount(acb, sbulChangeRequestsDuringPeriod))
                    .noSubmissionInWindowCount(calculateNoSubmissionInWindowCount(acb, sbulEligibleListings, sbulReportingPeriodStart, sbulReportingPeriodEnd))
                    .build())
                .collect(Collectors.toList());
    }

    private List<ListingSearchResult> getSbulEligibleListings(List<CertificationBody> acbs) {
        List<ListingSearchResult> sbulEligibleListings = new ArrayList<ListingSearchResult>();
        try {
            sbulEligibleListings = listingSearchService.getAllPagesOfSearchResults(SearchRequest.builder()
                .certificationBodies(acbs.stream().map(acb -> acb.getName()).collect(Collectors.toSet()))
                .certificationCriteriaIds(Stream.of(g10.getId()).collect(Collectors.toSet()))
                .certificationStatuses(CertificationStatusUtil.getActiveStatusNames().stream().collect(Collectors.toSet()))
                .build());
        } catch (ValidationException ex) {
            LOGGER.error("Cannot query for listings eligible for SBUL change requests", ex);
        }
        return sbulEligibleListings;
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

    private Long calculateApplicableListingCount(CertificationBody acb, List<ListingSearchResult> sbulEligibleListings) {
        return Long.valueOf(sbulEligibleListings.stream()
                .filter(listing -> listing.getCertificationBody().getName().equals(acb.getName()))
                .count());
    }

    private Long calculateApprovedCount(CertificationBody acb, List<ChangeRequestSearchResult> sbulChangeRequestsDuringReportingPeriod) {
        return sbulChangeRequestsDuringReportingPeriod.stream()
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

    private Long calculatePendingAcbActionCount(CertificationBody acb, List<ChangeRequestSearchResult> sbulChangeRequestsDuringReportingPeriod) {
        return sbulChangeRequestsDuringReportingPeriod.stream()
                .filter(cr -> containsAcb(acb, cr.getCertificationBodies()))
                .filter(cr -> cr.getCurrentStatus().getName().equals(ChangeRequestStatusConcept.PENDING_ONC_ACB_ACTION.getName()))
                .count();
    }

    private Long calculatePendingDeveloperActionCount(CertificationBody acb, List<ChangeRequestSearchResult> sbulChangeRequestsDuringReportingPeriod) {
        return sbulChangeRequestsDuringReportingPeriod.stream()
                .filter(cr -> containsAcb(acb, cr.getCertificationBodies()))
                .filter(cr -> cr.getCurrentStatus().getName().equals(ChangeRequestStatusConcept.PENDING_DEVELOPER_ACTION.getName()))
                .count();
    }

    private Long calculateNoSubmissionInWindowCount(CertificationBody acb, List<ListingSearchResult> sbulEligibleListings, LocalDateTime reportingPeriodStart, LocalDateTime reportingPeriodEnd) {
        List<Long> eligibleListingIdsForAcb = sbulEligibleListings.stream()
            .filter(listing -> listing.getCertificationBody().getName().equals(acb.getName()))
            .map(listing -> listing.getId())
            .collect(Collectors.toList());
        return changeRequestListingUrlDAO.getCountOfListingsWithNoChangeRequestsBetweenDates(eligibleListingIdsForAcb, reportingPeriodStart, reportingPeriodEnd);
    }
}
