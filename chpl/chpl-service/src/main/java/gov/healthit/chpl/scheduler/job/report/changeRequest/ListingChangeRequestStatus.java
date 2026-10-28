package gov.healthit.chpl.scheduler.job.report.changeRequest;

import gov.healthit.chpl.changerequest.domain.ChangeRequestStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListingChangeRequestStatus {
    private Long listingId;
    private String chplProductNumber;
    private Long developerId;
    private String developerName;
    private ChangeRequestStatus changeRequestStatus;
}
