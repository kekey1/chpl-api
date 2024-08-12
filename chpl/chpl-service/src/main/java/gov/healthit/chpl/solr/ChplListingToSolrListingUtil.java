package gov.healthit.chpl.solr;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import gov.healthit.chpl.domain.Address;
import gov.healthit.chpl.domain.CQMResultDetails;
import gov.healthit.chpl.domain.CertifiedProductChplProductNumberHistory;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.CertifiedProductTestingLab;

public class ChplListingToSolrListingUtil {

    public static SolrCertifiedProduct convert(CertifiedProductSearchDetails chplListing) {
        return SolrCertifiedProduct.builder()
                .id(chplListing.getId().toString())
                .chplProductNumber(chplListing.getChplProductNumber())
                .acb(chplListing.getCertifyingBody().get(CertifiedProductSearchDetails.ACB_NAME_KEY).toString())
                .acbCertificationId(chplListing.getAcbCertificationId())
                .accessibilityCertified(chplListing.getAccessibilityCertified())
                .atls(convertAtls(chplListing.getTestingLabs()))
                .certificationDay(buildDateRange(chplListing.getCertificationDay()))
                .decertificationDay(buildDateRange(chplListing.getDecertificationDay()))
                .developer(chplListing.getDeveloper().getName())
                .developerStreetAddress(buildStreetAddress(chplListing.getDeveloper().getAddress()))
                .developerCode(chplListing.getDeveloper().getDeveloperCode())
                .developerState(chplListing.getDeveloper().getAddress() != null ? chplListing.getDeveloper().getAddress().getState() : null)
                .developerStatus(chplListing.getDeveloper().getCurrentStatusEvent() == null ? null : chplListing.getDeveloper().getCurrentStatusEvent().getStatus().getName())
                .developerWebsite(chplListing.getDeveloper().getWebsite())
                .mandatoryDisclosures(chplListing.getMandatoryDisclosures())
                .previousChplProductNumbers(convertPreviousChplProductNumbers(chplListing.getChplProductNumberHistory()))
                .product(chplListing.getProduct().getName())
                .reportFileLocation(chplListing.getReportFileLocation())
                .rwtPlansCheckDate(buildDateRange(chplListing.getRwtPlansCheckDate()))
                .rwtPlansUrl(chplListing.getRwtPlansUrl())
                .rwtResultsCheckDate(buildDateRange(chplListing.getRwtResultsCheckDate()))
                .rwtResultsUrl(chplListing.getRwtResultsUrl())
                .sedIntendedUserDescription(chplListing.getSedIntendedUserDescription())
                .sedReportFileLocation(chplListing.getSedReportFileLocation())
                .sedTestingEndDay(buildDateRange(chplListing.getSedTestingEndDay()))
                .svapNoticeUrl(chplListing.getSvapNoticeUrl())
                .version(chplListing.getVersion().getVersion())
                .cqms(convertCqms(chplListing.getCqmResults()))
                .build();
    }

    private static List<String> convertAtls(List<CertifiedProductTestingLab> testingLabs) {
        if (CollectionUtils.isEmpty(testingLabs)) {
            return null;
        }

        return testingLabs.stream()
                .map(atl -> atl.getTestingLab().getName())
                .toList();
    }

    private static List<String> convertPreviousChplProductNumbers(List<CertifiedProductChplProductNumberHistory> histories) {
        if (CollectionUtils.isEmpty(histories)) {
            return null;
        }
        return histories.stream()
                .map(history -> history.getChplProductNumber())
                .toList();
    }

    private static List<String> convertCqms(List<CQMResultDetails> cqms) {
        if (CollectionUtils.isEmpty(cqms)) {
            return null;
        }
        List<CQMResultDetails> attestedCqms = cqms.stream()
                .filter(cqm -> cqm.getSuccess() != null && cqm.getSuccess())
                .toList();
        if(CollectionUtils.isEmpty(attestedCqms)) {
            return null;
        }
        return attestedCqms.stream()
                .map(cqm -> cqm.getCmsId())
                .toList();
    }

    private static String buildStreetAddress(Address address) {
        if (address == null) {
            return null;
        }
        String allAddressLines = "";
        if (!StringUtils.isEmpty(address.getLine1())) {
            allAddressLines += address.getLine1();
        }
        if(!StringUtils.isEmpty(address.getLine2())) {
            if (!StringUtils.isEmpty(allAddressLines)) {
                allAddressLines += " ";
            }
            allAddressLines += address.getLine2();
        }
        return allAddressLines;
    }

    private static String buildDateRange(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return "[" + localDate + " TO " + localDate + "]";
    }
}
