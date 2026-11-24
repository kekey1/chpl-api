package gov.healthit.chpl.scheduler.job.report.changeRequest.sbul;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.scheduler.job.report.changeRequest.ListingChangeRequestStatus;
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
public class SbulChangeRequestSummary {
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

    @JsonIgnore
    @Builder.Default
    private List<ListingChangeRequestStatus> listingsWithApprovedSbulChangeRequests = new ArrayList<ListingChangeRequestStatus>();

    @JsonIgnore
    @Builder.Default
    private List<ListingChangeRequestStatus> listingsWithPendingAcbActionSbulChangeRequests = new ArrayList<ListingChangeRequestStatus>();

    @JsonIgnore
    @Builder.Default
    private List<ListingChangeRequestStatus> listingsWithPendingDeveloperActionSbulChangeRequests = new ArrayList<ListingChangeRequestStatus>();

    @JsonIgnore
    @Builder.Default
    private List<ListingChangeRequestStatus> listingsWithNoSbulChangeRequests = new ArrayList<ListingChangeRequestStatus>();
}
