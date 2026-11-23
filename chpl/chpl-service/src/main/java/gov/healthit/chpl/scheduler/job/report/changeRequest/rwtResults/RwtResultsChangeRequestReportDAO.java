package gov.healthit.chpl.scheduler.job.report.changeRequest.rwtResults;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import gov.healthit.chpl.dao.impl.BaseDAOImpl;
import gov.healthit.chpl.entity.CertificationBodyEntity;
import jakarta.persistence.Query;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "rwtResultsChangeRequestReportCreatorJobLogger")
@Component
public class RwtResultsChangeRequestReportDAO extends BaseDAOImpl {

    public void insert(RwtResultsChangeRequestSummary crReport) {
        RwtResultsChangeRequestReportEntity entity = RwtResultsChangeRequestReportEntity.builder()
                .approvedCount(crReport.getApprovedCount())
                .reportDate(crReport.getReportDate())
                .reportingWindowStart(crReport.getRwtResultsReportingWindowStart())
                .reportingWindowEnd(crReport.getRwtResultsReportingWindowEnd())
                .certificationBody(crReport.getCertificationBody().getId() == 0L ? null : CertificationBodyEntity.builder()
                        .id(crReport.getCertificationBody().getId())
                        .build())
                .applicableListingCount(crReport.getApplicableListingCount())
                .noSubmissionInWindowCount(crReport.getNoSubmissionInWindowCount())
                .pendingAcbActionCount(crReport.getPendingAcbActionCount())
                .pendingDeveloperActionCount(crReport.getPendingDeveloperActionCount())
                .build();
        create(entity);
    }

    public List<RwtResultsChangeRequestSummary> getReportByDate(LocalDate date) {
        return getEntitiesByDate(date).stream()
                .map(entity -> entity.toDomain())
                .toList();
    }

    public void deleteByReportDate(LocalDate date) {
        getEntitiesByDate(date).forEach(entity -> {
            entity.setDeleted(true);
            update(entity);
        });
    }

    public void save(List<RwtResultsChangeRequestSummary> crReports) {
        crReports.forEach(crReport -> {
            RwtResultsChangeRequestReportEntity entity = RwtResultsChangeRequestReportEntity.builder()
                    .approvedCount(crReport.getApprovedCount())
                    .reportDate(crReport.getReportDate())
                    .reportingWindowStart(crReport.getRwtResultsReportingWindowStart())
                    .reportingWindowEnd(crReport.getRwtResultsReportingWindowEnd())
                    .certificationBody(crReport.getCertificationBody().getId() == 0L ? null : CertificationBodyEntity.builder()
                            .id(crReport.getCertificationBody().getId())
                            .build())
                    .applicableListingCount(crReport.getApplicableListingCount())
                    .noSubmissionInWindowCount(crReport.getNoSubmissionInWindowCount())
                    .pendingAcbActionCount(crReport.getPendingAcbActionCount())
                    .pendingDeveloperActionCount(crReport.getPendingDeveloperActionCount())
                    .build();
            create(entity);
        });
    }

    private List<RwtResultsChangeRequestReportEntity> getEntitiesByDate(LocalDate date) {
        Query query = entityManager.createQuery(
                "from RwtResultsChangeRequestReportEntity where (NOT deleted = true) and reportDate = :date", RwtResultsChangeRequestReportEntity.class);
        query.setParameter("date", date);
        return query.getResultList();
    }
}
