package gov.healthit.chpl.scheduler.job.report.changeRequest.rwtResults;

import java.time.LocalDate;

import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.util.LocalDateDeserializer;
import gov.healthit.chpl.util.LocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RwtResultsChangeRequestSummary {
    private Long id;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate reportDate;

    private CertificationBody certificationBody;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate reportingWindowStart;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate reportingWindowEnd;

    @Builder.Default
    private Long applicableListingCount = 0L;

    @Builder.Default
    private Long approvedCount = 0L;

    @Builder.Default
    private Long pendingAcbActionCount = 0L;

    @Builder.Default
    private Long pendingDeveloperActionCount = 0L;

    @Builder.Default
    private Long noSubmissionInWindowCount = 0L;
}
