package gov.healthit.chpl.validation.listing.reviewer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.util.ErrorMessageUtil;
import gov.healthit.chpl.util.ValidationUtils;

/**
 * Validate URLs have no new lines and otherwise look like URLs.
 * @author alarned
 *
 */
@Component("urlReviewer")
public class UrlReviewer implements Reviewer {

    @Autowired private ErrorMessageUtil msgUtil;

    @Override
    public void review(final CertifiedProductSearchDetails listing) {
        //check all string fields at the listing level
        addListingErrorIfNotValid(listing, listing.getReportFileLocation(),
                "Report File Location '" + listing.getReportFileLocation() + "'");
        addListingErrorIfNotValid(listing, listing.getSedReportFileLocation(),
                "SED Report File Location '" + listing.getSedReportFileLocation() + "'");
        addListingErrorIfNotValid(listing, listing.getTransparencyAttestationUrl(),
                "Transparency Attestation URL '" + listing.getTransparencyAttestationUrl() + "'");

        //check all criteria fields
        for (CertificationResult cert : listing.getCertificationResults()) {
            if (cert.isReviewable()) {
                addCriteriaErrorIfNotValid(listing, cert, cert.getApiDocumentation(), "API Documentation");
                addCriteriaErrorIfNotValid(listing, cert, cert.getExportDocumentation(), "ExportDocumentation");
                addCriteriaErrorIfNotValid(listing, cert, cert.getDocumentationUrl(), "Documentation Url");
                addCriteriaErrorIfNotValid(listing, cert, cert.getUseCases(), "Use Cases");
            }
        }
    }

    private void addListingErrorIfNotValid(final CertifiedProductSearchDetails listing,
            final String input, final String fieldName) {
        if (!StringUtils.isEmpty(input)) {
            if (ValidationUtils.hasNewline(input)) {
                listing.getErrorMessages().add(
                        msgUtil.getMessage("listing.invalidUrlFound", fieldName));
            } else if (!ValidationUtils.isWellFormedUrl(input)) {
                listing.getErrorMessages().add(
                        msgUtil.getMessage("listing.invalidUrlFound", fieldName));
            }
        }
    }

    private void addCriteriaErrorIfNotValid(final CertifiedProductSearchDetails listing,
            final CertificationResult criteria, final String input, final String fieldName) {
        if (!StringUtils.isEmpty(input)) {
            if (ValidationUtils.hasNewline(input)) {
                listing.getErrorMessages().add(
                        msgUtil.getMessage("listing.criteria.invalidUrlFound", fieldName, criteria.getNumber()));
            } else if (!ValidationUtils.isWellFormedUrl(input)) {
                listing.getErrorMessages().add(
                        msgUtil.getMessage("listing.criteria.invalidUrlFound", fieldName, criteria.getNumber()));
            }
        }
    }
}
