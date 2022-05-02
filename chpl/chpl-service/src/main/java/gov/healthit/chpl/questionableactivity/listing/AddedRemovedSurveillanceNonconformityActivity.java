package gov.healthit.chpl.questionableactivity.listing;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.NonconformityType;
import gov.healthit.chpl.domain.concept.QuestionableActivityTriggerConcept;
import gov.healthit.chpl.domain.surveillance.SurveillanceNonconformity;
import gov.healthit.chpl.dto.questionableActivity.QuestionableActivityListingDTO;
import gov.healthit.chpl.service.CertificationCriterionService;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class AddedRemovedSurveillanceNonconformityActivity implements ListingActivity {

    private CertificationCriterionService certificationCriterionService;

    @Autowired
    public AddedRemovedSurveillanceNonconformityActivity(CertificationCriterionService certificationCriterionService) {
        this.certificationCriterionService = certificationCriterionService;
    }

    @Override
    public List<QuestionableActivityListingDTO> check(CertifiedProductSearchDetails origListing, CertifiedProductSearchDetails newListing) {
        List<SurveillanceNonconformity> origNonconformities = origListing.getSurveillance().stream()
                .flatMap(surv -> surv.getRequirements().stream())
                .flatMap(req -> req.getNonconformities().stream())
                .map(nc -> nc)
                .collect(Collectors.toList());

        List<SurveillanceNonconformity> newNonconformities = newListing.getSurveillance().stream()
                .flatMap(surv -> surv.getRequirements().stream())
                .flatMap(req -> req.getNonconformities().stream())
                .map(nc -> nc)
                .collect(Collectors.toList());

        return ListUtils.union(
                checkForRemovedNonconformityTypeAdded(origNonconformities, newNonconformities),
                checkForNonconformityTypeUpdatedwithRemoved(origNonconformities, newNonconformities));
     }

    @Override
    public QuestionableActivityTriggerConcept getTriggerType() {
        return QuestionableActivityTriggerConcept.REMOVED_NONCONFORMITY_ADDED;
    }

    private List<QuestionableActivityListingDTO> checkForRemovedNonconformityTypeAdded(List<SurveillanceNonconformity> origNonconformities, List<SurveillanceNonconformity> newNonconformities) {
        return subtractNonConformityLists(newNonconformities, origNonconformities).stream()
                .filter(nc -> isNonconformityTypeRemoved(nc.getNonconformityType()))
                .map(nc -> QuestionableActivityListingDTO.builder()
                        .after(String.format("Non-Conformity of type %s is removed and was added to surveillance.", nc.getNonconformityType()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<QuestionableActivityListingDTO> checkForNonconformityTypeUpdatedwithRemoved(List<SurveillanceNonconformity> origNonconformities, List<SurveillanceNonconformity> newNonconformities) {
        return origNonconformities.stream()
                .filter(nc -> hasNonconformityBeenUpdatedToRemovedNonconformity(nc, newNonconformities))
                .map(nc -> QuestionableActivityListingDTO.builder()
                        .after(String.format("Non-conformity of type %s is removed and was added to surveillance.", getMatchingNonconformity(nc, newNonconformities).get().getNonconformityType()))
                        .build())
                .collect(Collectors.toList());
    }

    private Boolean hasNonconformityBeenUpdatedToRemovedNonconformity(SurveillanceNonconformity origNonconformity, List<SurveillanceNonconformity> newNoncoformities) {
        Optional<SurveillanceNonconformity> updatedNonconformity = getMatchingNonconformity(origNonconformity, newNoncoformities);
        if (updatedNonconformity.isPresent()) {
            return !updatedNonconformity.get().getNonconformityType().equals(origNonconformity.getNonconformityType())
                    && isNonconformityTypeRemoved(updatedNonconformity.get().getNonconformityType());
        }
        return false;
    }

    private Boolean isNonconformityTypeRemoved(String nonconformity) {
        Optional<NonconformityType> ncType = NonconformityType.getByName(nonconformity);
        if (ncType.isPresent()) {
            return ncType.get().getRemoved();
        } else {
            return certificationCriterionService.getByNumber(nonconformity).stream()
                    .filter(crit -> crit.getRemoved()
                            && !CertificationCriterionService.hasCuresInTitle(crit))
                    .findAny()
                    .isPresent();
        }
    }

    private Optional<SurveillanceNonconformity> getMatchingNonconformity(SurveillanceNonconformity nonconformity, List<SurveillanceNonconformity> nonconformities) {
        return nonconformities.stream()
                .filter(nc -> nc.getId().equals(nonconformity.getId()))
                .findAny();
    }

    private List<SurveillanceNonconformity> subtractNonConformityLists(List<SurveillanceNonconformity> listA, List<SurveillanceNonconformity> listB) {
        Predicate<SurveillanceNonconformity> notInListB = ncFromA -> !listB.stream()
                .anyMatch(nc -> ncFromA.getId().equals(nc.getId()));

        return listA.stream()
                .filter(notInListB)
                .collect(Collectors.toList());
    }

}