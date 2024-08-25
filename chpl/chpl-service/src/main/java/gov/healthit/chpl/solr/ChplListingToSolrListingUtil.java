package gov.healthit.chpl.solr;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.conformanceMethod.domain.CertificationResultConformanceMethod;
import gov.healthit.chpl.domain.Address;
import gov.healthit.chpl.domain.CQMResultDetails;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertificationResultAdditionalSoftware;
import gov.healthit.chpl.domain.CertificationResultTestData;
import gov.healthit.chpl.domain.CertifiedProductAccessibilityStandard;
import gov.healthit.chpl.domain.CertifiedProductChplProductNumberHistory;
import gov.healthit.chpl.domain.CertifiedProductQmsStandard;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.CertifiedProductSed;
import gov.healthit.chpl.domain.CertifiedProductTargetedUser;
import gov.healthit.chpl.domain.CertifiedProductTestingLab;
import gov.healthit.chpl.domain.InheritedCertificationStatus;
import gov.healthit.chpl.domain.ListingMeasure;
import gov.healthit.chpl.domain.surveillance.Surveillance;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.functionalitytested.CertificationResultFunctionalityTested;
import gov.healthit.chpl.listing.ics.IcsManager;
import gov.healthit.chpl.listing.ics.ListingIcsNode;
import gov.healthit.chpl.optionalStandard.domain.CertificationResultOptionalStandard;
import gov.healthit.chpl.standard.CertificationResultStandard;
import gov.healthit.chpl.svap.domain.CertificationResultSvap;
import gov.healthit.chpl.testtool.CertificationResultTestTool;
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
                .certificationStatus(chplListing.getCurrentStatus().getStatus().getName())
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
                .qmsStandardModifications(convertQmsModifications(chplListing.getQmsStandards()))
                .measures(convertMeasures(chplListing.getMeasures()))
                .listingsRelatedViaInheritance(convertIcs(chplListing.getId(), chplListing.getIcs()))
                .attestedCqms(convertCqms(chplListing.getCqmResults()))
                .attestedCriteria(convertCriteria(chplListing.getCertificationResults()))
                .additionalSoftwareNames(convertToAdditionalSoftwareNames(chplListing.getCertificationResults()))
                .conformanceMethodNames(convertToConformanceMethodNames(chplListing.getCertificationResults()))
                .documentationUrls(convertToDocumentationUrls(chplListing.getCertificationResults()))
                .exportDocumentation(convertToExportDocumentation(chplListing.getCertificationResults()))
                .functionalitiesTestedNames(convertToFunctionalitiesTested(chplListing.getCertificationResults()))
                .optionalStandardNames(convertToOptionalStandards(chplListing.getCertificationResults()))
                .privacySecurityFramework(convertToPandS(chplListing.getCertificationResults()))
                .riskManagementSummaryInformation(convertToRiskManagementSummaryInfo(chplListing.getCertificationResults()))
                .serviceBaseUrlList(convertToServiceBaseUrlList(chplListing.getCertificationResults()))
                .standardNames(convertToStandards(chplListing.getCertificationResults()))
                .svaps(convertToSvaps(chplListing.getCertificationResults()))
                .testDataNames(convertToTestData(chplListing.getCertificationResults()))
                .testToolNames(convertToTestTools(chplListing.getCertificationResults()))
                .testTaskDescriptions(convertToTestTaskDescriptions(chplListing.getSed()))
                .testParticipantEducations(convertToParticipantEducations(chplListing.getSed()))
                .testParticipantOccupations(convertToParticipantOccupations(chplListing.getSed()))
                .nonconformitySummary(convertToNonconformitySummary(chplListing.getSurveillance()))
                .nonconformityFindings(convertToNonconformityFindings(chplListing.getSurveillance()))
                .nonconformityResolution(convertToNonconformityResolution(chplListing.getSurveillance()))
                .nonconformityDeveloperExplanations(convertToNonconformityDeveloperExplanations(chplListing.getSurveillance()))
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

    private List<String> convertQmsModifications(List<CertifiedProductQmsStandard> qmsStandards) {
        if (CollectionUtils.isEmpty(qmsStandards)) {
            return null;
        }
        return qmsStandards.stream()
                .filter(qmsStandard -> !StringUtils.isEmpty(qmsStandard.getQmsModification()))
                .map(qmsStandard -> qmsStandard.getQmsModification())
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

    private Set<String> convertToAdditionalSoftwareNames(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultAdditionalSoftware> allAdditionalSoftware = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getAdditionalSoftware()))
                .flatMap(certResult -> certResult.getAdditionalSoftware().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allAdditionalSoftware)) {
            return null;
        }
        return allAdditionalSoftware.stream()
                .map(addSoft -> !StringUtils.isEmpty(addSoft.getName()) ? addSoft.getName() : addSoft.getCertifiedProductNumber())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToConformanceMethodNames(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultConformanceMethod> allConformanceMethods = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getConformanceMethods()))
                .flatMap(certResult -> certResult.getConformanceMethods().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allConformanceMethods)) {
            return null;
        }
        return allConformanceMethods.stream()
                .map(cm -> cm.getConformanceMethod().getName())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToDocumentationUrls(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }

        List<String> allDocumentationUrls = certResults.stream()
                .filter(certResult -> !StringUtils.isEmpty(certResult.getDocumentationUrl()))
                .map(certResult -> certResult.getDocumentationUrl())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allDocumentationUrls)) {
            return null;
        }

        return allDocumentationUrls.stream()
                .collect(Collectors.toSet());
    }

    private Set<String> convertToExportDocumentation(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }

        List<String> allDocumentationUrls = certResults.stream()
                .filter(certResult -> !StringUtils.isEmpty(certResult.getExportDocumentation()))
                .map(certResult -> certResult.getExportDocumentation())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allDocumentationUrls)) {
            return null;
        }

        return allDocumentationUrls.stream()
                .collect(Collectors.toSet());
    }

    private Set<String> convertToFunctionalitiesTested(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultFunctionalityTested> allFuncTested = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getFunctionalitiesTested()))
                .flatMap(certResult -> certResult.getFunctionalitiesTested().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allFuncTested)) {
            return null;
        }
        return allFuncTested.stream()
                .map(ft -> ft.getFunctionalityTested().getRegulatoryTextCitation())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToOptionalStandards(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultOptionalStandard> allOptStandards = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getOptionalStandards()))
                .flatMap(certResult -> certResult.getOptionalStandards().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allOptStandards)) {
            return null;
        }
        return allOptStandards.stream()
                .map(os -> os.getOptionalStandard().getCitation())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToPandS(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<String> allPandS = certResults.stream()
                .filter(certResult -> !StringUtils.isEmpty(certResult.getPrivacySecurityFramework()))
                .map(certResult -> certResult.getPrivacySecurityFramework())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allPandS)) {
            return null;
        }
        return allPandS.stream()
                .collect(Collectors.toSet());
    }

    private Set<String> convertToRiskManagementSummaryInfo(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<String> riskManagementSummaries = certResults.stream()
                .filter(certResult -> !StringUtils.isEmpty(certResult.getRiskManagementSummaryInformation()))
                .map(certResult -> certResult.getRiskManagementSummaryInformation())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(riskManagementSummaries)) {
            return null;
        }
        return riskManagementSummaries.stream()
                .collect(Collectors.toSet());
    }

    private Set<String> convertToServiceBaseUrlList(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<String> allServiceBaseUrlLists = certResults.stream()
                .filter(certResult -> !StringUtils.isEmpty(certResult.getServiceBaseUrlList()))
                .map(certResult -> certResult.getServiceBaseUrlList())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allServiceBaseUrlLists)) {
            return null;
        }
        return allServiceBaseUrlLists.stream()
                .collect(Collectors.toSet());
    }

    private Set<String> convertToStandards(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultStandard> allStandards = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getStandards()))
                .flatMap(certResult -> certResult.getStandards().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allStandards)) {
            return null;
        }
        return allStandards.stream()
                .map(os -> os.getStandard().getRegulatoryTextCitation())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToSvaps(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultSvap> allSvaps = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getSvaps()))
                .flatMap(certResult -> certResult.getSvaps().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allSvaps)) {
            return null;
        }
        return allSvaps.stream()
                .map(svap -> svap.getRegulatoryTextCitation())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToTestData(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultTestData> allTestData = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getTestDataUsed()))
                .flatMap(certResult -> certResult.getTestDataUsed().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allTestData)) {
            return null;
        }
        return allTestData.stream()
                .map(td -> td.getTestData().getName())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToTestTools(List<CertificationResult> certResults) {
        if (CollectionUtils.isEmpty(certResults)) {
            return null;
        }
        List<CertificationResultTestTool> allTestTools = certResults.stream()
                .filter(certResult -> !CollectionUtils.isEmpty(certResult.getTestToolsUsed()))
                .flatMap(certResult -> certResult.getTestToolsUsed().stream())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allTestTools)) {
            return null;
        }
        return allTestTools.stream()
                .map(tt -> tt.getTestTool().getValue())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToTestTaskDescriptions(CertifiedProductSed sed) {
        if (sed == null || CollectionUtils.isEmpty(sed.getTestTasks())) {
            return null;
        }
        return sed.getTestTasks().stream()
            .map(testTask -> testTask.getDescription())
            .collect(Collectors.toSet());
    }

    private Set<String> convertToParticipantOccupations(CertifiedProductSed sed) {
        if (sed == null || CollectionUtils.isEmpty(sed.getTestTasks())) {
            return null;
        }
        return sed.getTestTasks().stream()
                .flatMap(testTask -> testTask.getTestParticipants().stream())
                .map(participant -> participant.getOccupation())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToParticipantEducations(CertifiedProductSed sed) {
        if (sed == null || CollectionUtils.isEmpty(sed.getTestTasks())) {
            return null;
        }
        return sed.getTestTasks().stream()
                .flatMap(testTask -> testTask.getTestParticipants().stream())
                .map(participant -> participant.getEducationType().getName())
                .collect(Collectors.toSet());
    }

    private Set<String> convertToNonconformitySummary(List<Surveillance> survs) {
        if (CollectionUtils.isEmpty(survs)) {
            return null;
        }
        return survs.stream()
            .flatMap(surv -> surv.getRequirements().stream())
            .flatMap(req -> req.getNonconformities().stream())
            .filter(nc -> !StringUtils.isEmpty(nc.getSummary()))
            .map(nc -> nc.getSummary())
            .collect(Collectors.toSet());
    }

    private Set<String> convertToNonconformityFindings(List<Surveillance> survs) {
        if (CollectionUtils.isEmpty(survs)) {
            return null;
        }
        return survs.stream()
            .flatMap(surv -> surv.getRequirements().stream())
            .flatMap(req -> req.getNonconformities().stream())
            .filter(nc -> !StringUtils.isEmpty(nc.getFindings()))
            .map(nc -> nc.getFindings())
            .collect(Collectors.toSet());
    }

    private Set<String> convertToNonconformityResolution(List<Surveillance> survs) {
        if (CollectionUtils.isEmpty(survs)) {
            return null;
        }
        return survs.stream()
            .flatMap(surv -> surv.getRequirements().stream())
            .flatMap(req -> req.getNonconformities().stream())
            .filter(nc -> !StringUtils.isEmpty(nc.getResolution()))
            .map(nc -> nc.getResolution())
            .collect(Collectors.toSet());
    }

    private Set<String> convertToNonconformityDeveloperExplanations(List<Surveillance> survs) {
        if (CollectionUtils.isEmpty(survs)) {
            return null;
        }
        return survs.stream()
            .flatMap(surv -> surv.getRequirements().stream())
            .flatMap(req -> req.getNonconformities().stream())
            .filter(nc -> !StringUtils.isEmpty(nc.getDeveloperExplanation()))
            .map(nc -> nc.getDeveloperExplanation())
            .collect(Collectors.toSet());
    }

    private String buildStreetAddress(Address address) {
        if (address == null) {
            return null;
        }
        String allAddressLines = "";
        if (!StringUtils.isEmpty(address.getLine1())) {
            allAddressLines += address.getLine1();
        }
        if (!StringUtils.isEmpty(address.getLine2())) {
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
