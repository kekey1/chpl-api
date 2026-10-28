package gov.healthit.chpl.scheduler.job.report.changeRequest.rwtResults;

import java.time.LocalDate;

import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.entity.CertificationBodyEntity;
import gov.healthit.chpl.entity.EntityAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "rwt_results_change_request_report")
public class RwtResultsChangeRequestReportEntity extends EntityAudit {
    private static final long serialVersionUID = -213928530211689705L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_body_id")
    private CertificationBodyEntity certificationBody;

    @Column(name = "applicable_listing_count")
    private Long applicableListingCount;

    @Column(name = "approved_count")
    private Long approvedCount;

    @Column(name = "pending_acb_action_count")
    private Long pendingAcbActionCount;

    @Column(name = "pending_developer_action_count")
    private Long pendingDeveloperActionCount;

    @Column(name = "no_submission_in_window_count")
    private Long noSubmissionInWindowCount;

    public RwtResultsChangeRequestReport toDomain() {
        return RwtResultsChangeRequestReport.builder()
                .id(id)
                .reportDate(reportDate)
                .certificationBody(certificationBody != null
                        ? certificationBody.toDomain()
                        : CertificationBody.builder().id(0L).name("All ONC-ACBs").build())
                .applicableListingCount(applicableListingCount)
                .approvedCount(approvedCount)
                .pendingAcbActionCount(pendingAcbActionCount)
                .pendingDeveloperActionCount(pendingDeveloperActionCount)
                .noSubmissionInWindowCount(noSubmissionInWindowCount)
                .build();
    }

}
