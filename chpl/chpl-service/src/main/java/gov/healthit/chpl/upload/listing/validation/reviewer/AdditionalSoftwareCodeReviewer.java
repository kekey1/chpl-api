package gov.healthit.chpl.upload.listing.validation.reviewer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.util.ChplProductNumberUtil;
import gov.healthit.chpl.util.ErrorMessageUtil;
import lombok.extern.log4j.Log4j2;

@Component("listingUploadAdditionalSoftwareReviewer")
@Log4j2
public class AdditionalSoftwareCodeReviewer {
    private ChplProductNumberUtil chplProductNumberUtil;
    private ErrorMessageUtil msgUtil;

    @Autowired
    public AdditionalSoftwareCodeReviewer(ChplProductNumberUtil chplProductNumberUtil,
            ErrorMessageUtil msgUtil) {
        this.chplProductNumberUtil = chplProductNumberUtil;
        this.msgUtil = msgUtil;
    }

    public void review(CertifiedProductSearchDetails listing) {
        String chplProductNumber = listing.getChplProductNumber();
        if (StringUtils.isEmpty(chplProductNumber)) {
            return;
        }

        String[] uniqueIdParts = chplProductNumber.split("\\.");
        if (uniqueIdParts.length != ChplProductNumberUtil.CHPL_PRODUCT_ID_PARTS) {
            return;
        }

        String additionalSoftwareCode = null;
        try {
            additionalSoftwareCode = chplProductNumberUtil.getAdditionalSoftwareCode(listing.getChplProductNumber());
        } catch (Exception ex) {
            LOGGER.catching(ex);
        }

        boolean certsHaveAdditionalSoftware = listing.getCertificationResults().stream()
                .filter(certResult -> certResult.getAdditionalSoftware() != null && certResult.getAdditionalSoftware().size() > 0)
                .findAny().isPresent();
        if (additionalSoftwareCode != null && additionalSoftwareCode.equals("0")) {
            if (certsHaveAdditionalSoftware) {
                listing.getErrorMessages().add(msgUtil.getMessage("listing.additionalSoftwareCode0Mismatch"));
            }
        } else if (additionalSoftwareCode != null && additionalSoftwareCode.equals("1")) {
            if (!certsHaveAdditionalSoftware) {
                listing.getErrorMessages().add(msgUtil.getMessage("listing.additionalSoftwareCode1Mismatch"));
            }
        }
    }
}
