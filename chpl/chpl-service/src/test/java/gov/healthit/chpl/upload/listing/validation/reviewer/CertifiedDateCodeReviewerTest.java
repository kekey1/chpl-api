package gov.healthit.chpl.upload.listing.validation.reviewer;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.util.ChplProductNumberUtil;
import gov.healthit.chpl.util.ErrorMessageUtil;
import gov.healthit.chpl.util.ValidationUtils;

public class CertifiedDateCodeReviewerTest {
    private static final String MISMATCHED_CERT_DATE = "The certified date code from the listing %s does not match the certification date of the listing %s.";

    private ErrorMessageUtil errorMessageUtil;
    private CertifiedDateCodeReviewer reviewer;

    @Before
    public void setup() {
        errorMessageUtil = Mockito.mock(ErrorMessageUtil.class);
        reviewer = new CertifiedDateCodeReviewer(new ChplProductNumberUtil(), new ValidationUtils(), errorMessageUtil);
    }

    @Test
    public void review_legacyChplProductNumber_noError() {
        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .chplProductNumber("CHP-123456")
                .build();
        reviewer.review(listing);

        assertEquals(0, listing.getErrorMessages().size());
    }

    @Test
    public void review_mismatchedCertDate_errorMessage() {
        Mockito.when(errorMessageUtil.getMessage(ArgumentMatchers.eq("listing.certificationDateMismatch"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenAnswer(i -> String.format(MISMATCHED_CERT_DATE, i.getArgument(1), i.getArgument(2)));

        Calendar listingCertDate = Calendar.getInstance();
        listingCertDate.set(2021, 0, 2, 0, 0, 0);

        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationDate(listingCertDate.getTimeInMillis())
                .chplProductNumber("15.04.04.2526.WEBe.06.00.1.210101")
                .certificationDateStr(null)
                .build();

        reviewer.review(listing);

        assertEquals(1, listing.getErrorMessages().size());
        assertTrue(listing.getErrorMessages().contains(String.format(MISMATCHED_CERT_DATE, "210101", "210102")));
    }

    @Test
    public void review_mismatchedCertDateInvalidChplProductNumberFormat_noError() {
        Calendar listingCertDate = Calendar.getInstance();
        listingCertDate.set(2021, 0, 2, 0, 0, 0);

        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationDate(listingCertDate.getTimeInMillis())
                .chplProductNumber("15.04.04.2526.WEBe.06.00.1.RT789364")
                .certificationDateStr(null)
                .build();

        reviewer.review(listing);

        assertEquals(0, listing.getErrorMessages().size());
    }

    @Test
    public void review_goodCertDateCodeWithMatchingListingCertDate_noError() {
        Calendar listingCertDate = Calendar.getInstance();
        listingCertDate.set(2021, 0, 2, 0, 0, 0);

        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationDate(listingCertDate.getTimeInMillis())
                .chplProductNumber("15.04.04.2526.WEBe.06.00.1.210102")
                .certificationDateStr(null)
                .build();
        reviewer.review(listing);

        assertEquals(0, listing.getErrorMessages().size());
    }
}
