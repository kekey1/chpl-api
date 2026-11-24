package gov.healthit.chpl.scheduler.job.report.changeRequest.sbul;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import gov.healthit.chpl.dao.impl.BaseDAOImpl;
import gov.healthit.chpl.entity.CertificationBodyEntity;
import jakarta.persistence.Query;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "sbulChangeRequestReportCreatorJobLogger")
@Component
public class SbulChangeRequestReportDAO extends BaseDAOImpl {

    public List<SbulChangeRequestSummary> getReportByDate(LocalDate date) {
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

    public void save(List<SbulChangeRequestSummary> crReports) {
        crReports.forEach(crReport -> {
            SbulChangeRequestReportEntity entity = SbulChangeRequestReportEntity.builder()
                    .approvedCount(crReport.getApprovedCount())
                    .reportDate(crReport.getReportDate())
                    .reportingWindowStart(crReport.getReportingWindowStart())
                    .reportingWindowEnd(crReport.getReportingWindowEnd())
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

    private List<SbulChangeRequestReportEntity> getEntitiesByDate(LocalDate date) {
        Query query = entityManager.createQuery(
                "from SbulChangeRequestReportEntity where (NOT deleted = true) and reportDate = :date", SbulChangeRequestReportEntity.class);
        query.setParameter("date", date);
        return query.getResultList();
    }
}
