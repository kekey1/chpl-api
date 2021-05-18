package gov.healthit.chpl.scheduler.job.activity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MongoListingActivity extends CertifiedProductSearchDetails {
    private Date historicalListingDate;
}
