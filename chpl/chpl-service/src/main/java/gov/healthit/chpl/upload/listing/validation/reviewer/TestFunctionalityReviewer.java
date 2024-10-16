package gov.healthit.chpl.upload.listing.validation.reviewer;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gov.healthit.chpl.dao.TestFunctionalityDAO;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertificationResultTestFunctionality;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.dto.TestFunctionalityCriteriaMapDTO;
import gov.healthit.chpl.dto.TestFunctionalityDTO;
import gov.healthit.chpl.permissions.ResourcePermissions;
import gov.healthit.chpl.util.CertificationResultRules;
import gov.healthit.chpl.util.ErrorMessageUtil;
import gov.healthit.chpl.util.Util;
import gov.healthit.chpl.validation.listing.reviewer.PermissionBasedReviewer;

@Component("listingUploadTestFunctionalityReviewer")
public class TestFunctionalityReviewer extends PermissionBasedReviewer {
    private CertificationResultRules certResultRules;
    private TestFunctionalityDAO testFunctionalityDao;

    @Autowired
    public TestFunctionalityReviewer(CertificationResultRules certResultRules,
            TestFunctionalityDAO testFunctionalityDao,
            ErrorMessageUtil msgUtil, ResourcePermissions resourcePermissions) {
        super(msgUtil, resourcePermissions);
        this.certResultRules = certResultRules;
        this.testFunctionalityDao = testFunctionalityDao;
    }

    @Override
    public void review(CertifiedProductSearchDetails listing) {
        listing.getCertificationResults().stream()
            .filter(certResult -> BooleanUtils.isTrue(certResult.isSuccess()))
            .forEach(certResult -> review(listing, certResult));
    }

    public void review(CertifiedProductSearchDetails listing, CertificationResult certResult) {
        reviewCriteriaCanHaveTestFunctionalityData(listing, certResult);
        removeTestFunctionalityWithoutIds(listing, certResult);
        removeTestFunctionalityMismatchedToCriteria(listing, certResult);
        if (certResult.getTestFunctionality() != null && certResult.getTestFunctionality().size() > 0) {
            certResult.getTestFunctionality().stream()
                .forEach(testFunc -> reviewTestFunctionalityFields(listing, certResult, testFunc));
        }
    }

    private void reviewCriteriaCanHaveTestFunctionalityData(CertifiedProductSearchDetails listing, CertificationResult certResult) {
        if (!certResultRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.FUNCTIONALITY_TESTED)
                && certResult.getTestFunctionality() != null && certResult.getTestFunctionality().size() > 0) {
            listing.getErrorMessages().add(msgUtil.getMessage(
                    "listing.criteria.testFunctionalityNotApplicable", Util.formatCriteriaNumber(certResult.getCriterion())));
        }
    }

    private void removeTestFunctionalityWithoutIds(CertifiedProductSearchDetails listing, CertificationResult certResult) {
        if (certResult.getTestFunctionality() == null || certResult.getTestFunctionality().size() == 0) {
            return;
        }
        Iterator<CertificationResultTestFunctionality> testFunctionalityIter = certResult.getTestFunctionality().iterator();
        while (testFunctionalityIter.hasNext()) {
            CertificationResultTestFunctionality testFunctionality = testFunctionalityIter.next();
            if (testFunctionality.getTestFunctionalityId() == null) {
                testFunctionalityIter.remove();
                addCriterionErrorOrWarningByPermission(listing, certResult, "listing.criteria.testFunctionalityNotFoundAndRemoved",
                        Util.formatCriteriaNumber(certResult.getCriterion()), testFunctionality.getName());
            }
        }
    }

    private void removeTestFunctionalityMismatchedToCriteria(CertifiedProductSearchDetails listing, CertificationResult certResult) {
        if (certResult.getTestFunctionality() == null || certResult.getTestFunctionality().size() == 0) {
            return;
        }
        String year = MapUtils.getString(listing.getCertificationEdition(), CertifiedProductSearchDetails.EDITION_NAME_KEY);
        Iterator<CertificationResultTestFunctionality> testFunctionalityIter = certResult.getTestFunctionality().iterator();
        while (testFunctionalityIter.hasNext()) {
            CertificationResultTestFunctionality testFunctionality = testFunctionalityIter.next();
            if (!isTestFunctionalityCritierionValid(certResult.getCriterion().getId(),
                    testFunctionality.getTestFunctionalityId(), year)) {
                testFunctionalityIter.remove();

                addCriterionErrorOrWarningByPermission(listing, certResult,
                        msgUtil.getMessage("listing.criteria.testFunctionalityCriterionMismatch",
                            Util.formatCriteriaNumber(certResult.getCriterion()),
                            testFunctionality.getName(),
                            getDelimitedListOfValidCriteriaNumbers(testFunctionality.getTestFunctionalityId(), year),
                            Util.formatCriteriaNumber(certResult.getCriterion())));
            }
        }
    }

    private boolean isTestFunctionalityCritierionValid(Long criteriaId, Long testFunctionalityId, String year) {
        List<TestFunctionalityDTO> validTestFunctionalityForCriteria =
                testFunctionalityDao.getTestFunctionalityCriteriaMaps(year).get(criteriaId);
        if (validTestFunctionalityForCriteria == null) {
            return false;
        } else {
            return validTestFunctionalityForCriteria.stream().filter(validTf -> validTf.getId().equals(testFunctionalityId)).count() > 0;
        }
    }

    private String getDelimitedListOfValidCriteriaNumbers(Long testFunctionalityId, String year) {
        List<TestFunctionalityCriteriaMapDTO> testFunctionalityMaps = testFunctionalityDao.getTestFunctionalityCritieriaMaps();
        return testFunctionalityMaps.stream().
            filter(testFunctionalityMap -> testFunctionalityMap.getCriteria().getCertificationEdition().equals(year)
                    && testFunctionalityId.equals(testFunctionalityMap.getTestFunctionality().getId()))
            .map(testFunctionalityMap -> Util.formatCriteriaNumber(testFunctionalityMap.getCriteria()))
            .collect(Collectors.joining(","));
    }

    private void reviewTestFunctionalityFields(CertifiedProductSearchDetails listing,
            CertificationResult certResult, CertificationResultTestFunctionality testFunctionality) {
        reviewTestFunctionalityName(listing, certResult, testFunctionality);
    }

    private void reviewTestFunctionalityName(CertifiedProductSearchDetails listing,
            CertificationResult certResult, CertificationResultTestFunctionality testFunctionality) {
        if (StringUtils.isEmpty(testFunctionality.getName())) {
            listing.getErrorMessages().add(msgUtil.getMessage("listing.criteria.missingTestFunctionalityName",
                    Util.formatCriteriaNumber(certResult.getCriterion())));
        }
    }
}
