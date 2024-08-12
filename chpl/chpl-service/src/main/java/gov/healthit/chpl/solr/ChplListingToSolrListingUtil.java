package gov.healthit.chpl.solr;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.domain.Address;
import gov.healthit.chpl.domain.CQMResultDetails;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductAccessibilityStandard;
import gov.healthit.chpl.domain.CertifiedProductChplProductNumberHistory;
import gov.healthit.chpl.domain.CertifiedProductQmsStandard;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.CertifiedProductTargetedUser;
import gov.healthit.chpl.domain.CertifiedProductTestingLab;
import gov.healthit.chpl.domain.InheritedCertificationStatus;
import gov.healthit.chpl.domain.ListingMeasure;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.listing.ics.IcsManager;
import gov.healthit.chpl.listing.ics.ListingIcsNode;
import gov.healthit.chpl.util.Util;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ChplListingToSolrListingUtil {

    private IcsManager icsManager;

    @Autowired
    public ChplListingToSolrListingUtil(IcsManager icsManager) {
        this.icsManager = icsManager;
    }

    public SolrCertifiedProduct convert(CertifiedProductSearchDetails chplListing) {
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
                .accessibilityStandardNames(convertAccessibilityStandards(chplListing.getAccessibilityStandards()))
                .targetedUserNames(convertTargetedUsers(chplListing.getTargetedUsers()))
                .qmsStandardNames(convertQms(chplListing.getQmsStandards()))
                .measures(convertMeasures(chplListing.getMeasures()))
                .listingsRelatedViaInheritance(convertIcs(chplListing.getId(), chplListing.getIcs()))
                .attestedCqms(convertCqms(chplListing.getCqmResults()))
                .attestedCriteria(convertCriteria(chplListing.getCertificationResults()))
                .build();
    }

    private List<String> convertAtls(List<CertifiedProductTestingLab> testingLabs) {
        if (CollectionUtils.isEmpty(testingLabs)) {
            return null;
        }

        return testingLabs.stream()
                .map(atl -> atl.getTestingLab().getName())
                .toList();
    }

    private List<String> convertPreviousChplProductNumbers(List<CertifiedProductChplProductNumberHistory> histories) {
        if (CollectionUtils.isEmpty(histories)) {
            return null;
        }
        return histories.stream()
                .map(history -> history.getChplProductNumber())
                .toList();
    }

    private List<String> convertIcs(Long chplListingId, InheritedCertificationStatus ics) {
        if (ics == null || BooleanUtils.isFalse(ics.getInherits())) {
            return null;
        }

        List<ListingIcsNode> icsFamily = null;
        try {
            icsFamily = icsManager.getIcsFamilyTree(chplListingId);
        } catch (EntityRetrievalException ex) {
            LOGGER.error("Could not get ICS for listing " + chplListingId, ex);
        }

        if (CollectionUtils.isEmpty(icsFamily)) {
            return null;
        }
        return icsFamily.stream()
                .map(icsNode -> icsNode.getChplProductNumber())
                .toList();
    }

    private List<String> convertAccessibilityStandards(List<CertifiedProductAccessibilityStandard> accStds) {
        if (CollectionUtils.isEmpty(accStds)) {
            return null;
        }
        return accStds.stream()
                .map(accStd -> accStd.getAccessibilityStandardName())
                .toList();
    }

    private List<String> convertTargetedUsers(List<CertifiedProductTargetedUser> targetedUsers) {
        if (CollectionUtils.isEmpty(targetedUsers)) {
            return null;
        }
        return targetedUsers.stream()
                .map(targetedUser -> targetedUser.getTargetedUserName())
                .toList();
    }

    private List<String> convertQms(List<CertifiedProductQmsStandard> qmsStandards) {
        if (CollectionUtils.isEmpty(qmsStandards)) {
            return null;
        }
        return qmsStandards.stream()
                .map(qmsStandard -> qmsStandard.getQmsStandardName())
                .toList();
    }

    private List<String> convertMeasures(List<ListingMeasure> measures) {
        if (CollectionUtils.isEmpty(measures)) {
            return null;
        }
        return measures.stream()
                .map(measure -> measure.getMeasure().getAbbreviation())
                .toList();
    }

    private List<String> convertCqms(List<CQMResultDetails> cqms) {
        if (CollectionUtils.isEmpty(cqms)) {
            return null;
        }
        List<CQMResultDetails> attestedCqms = cqms.stream()
                .filter(cqm -> cqm.getSuccess() != null && cqm.getSuccess())
                .toList();
        if (CollectionUtils.isEmpty(attestedCqms)) {
            return null;
        }
        return attestedCqms.stream()
                .map(cqm -> cqm.getCmsId())
                .toList();
    }

    private List<String> convertCriteria(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        return certResults.stream()
                .map(certResult -> Util.formatCriteriaNumber(certResult.getCriterion()))
                .toList();
    }

    private String buildStreetAddress(Address address) {
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

    private String buildDateRange(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return "[" + localDate + " TO " + localDate + "]";
    }
}
