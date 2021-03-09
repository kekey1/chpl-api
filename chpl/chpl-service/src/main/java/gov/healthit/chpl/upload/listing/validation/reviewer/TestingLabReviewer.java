package gov.healthit.chpl.upload.listing.validation.reviewer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.CertifiedProductTestingLab;
import gov.healthit.chpl.util.ErrorMessageUtil;

@Component("testingLabReviewer")
public class TestingLabReviewer  {
    private ErrorMessageUtil msgUtil;

    @Autowired
    public TestingLabReviewer(ErrorMessageUtil msgUtil) {
        this.msgUtil = msgUtil;
    }

    public void review(CertifiedProductSearchDetails listing) {
        List<CertifiedProductTestingLab> atls = listing.getTestingLabs();
        if (atls == null || atls.size() == 0) {
            listing.getErrorMessages().add(msgUtil.getMessage("listing.missingTestingLab"));
            return;
        }

        listing.getTestingLabs().stream()
            .forEach(atl -> reviewValidTestingLab(listing, atl));
    }

    private void reviewValidTestingLab(CertifiedProductSearchDetails listing, CertifiedProductTestingLab atl) {
        if (StringUtils.isEmpty(atl.getTestingLabName())) {
            listing.getErrorMessages().add(msgUtil.getMessage("listing.missingTestingLabName"));
        }

        if (StringUtils.isEmpty(atl.getTestingLabCode())) {
            listing.getErrorMessages().add(msgUtil.getMessage("listing.missingTestingLabCode"));
        }

        if (atl.getTestingLabId() == null && !StringUtils.isEmpty(atl.getTestingLabName())) {
            listing.getErrorMessages().add(msgUtil.getMessage("listing.invalidTestingLab", atl.getTestingLabName()));
        }
    }
}
