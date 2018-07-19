package gov.healthit.chpl.validation.pendingListing.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.dto.PendingCertificationResultDTO;
import gov.healthit.chpl.dto.PendingCertifiedProductDTO;
import gov.healthit.chpl.util.CertificationResultRules;
import gov.healthit.chpl.util.ValidationUtils;

@Component
public class RequiredTestTool2014Reviewer implements Reviewer {
    private static final String[] TEST_TOOL_CHECK_CERTS = {
            "170.314 (g)(1)", "170.314 (g)(2)", "170.314 (f)(3)"
    };

    @Autowired private CertificationResultRules certRules;
    
    @Override
    public void review(PendingCertifiedProductDTO listing) {
        //check for test tools
        for (PendingCertificationResultDTO cert : listing.getCertificationCriterion()) {
            if (cert.getMeetsCriteria() != null && cert.getMeetsCriteria() == Boolean.TRUE) {
                boolean gapEligibleAndTrue = false;
                if (certRules.hasCertOption(cert.getNumber(), CertificationResultRules.GAP)
                        && cert.getGap() == Boolean.TRUE) {
                    gapEligibleAndTrue = true;
                }

                if (!gapEligibleAndTrue
                        && certRules.hasCertOption(cert.getNumber(), CertificationResultRules.TEST_TOOLS_USED)
                        && ValidationUtils.containsCert(cert, TEST_TOOL_CHECK_CERTS)
                        && (cert.getTestTools() == null || cert.getTestTools().size() == 0)) {
                    listing.getErrorMessages()
                    .add("Test Tools are required for certification " + cert.getNumber() + ".");
                }
            }
        }
    }
}
