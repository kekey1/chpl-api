package gov.healthit.chpl.certificationId;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import gov.healthit.chpl.dto.CQMMetDTO;
import gov.healthit.chpl.dto.CertificationCriterionDTO;
import lombok.Data;

@Data
public abstract class Validator {
    private static final double PERCENTAGE_MULTIPLER = 100.0;
    private static final int PERCENTAGE_MAX = 100;

    private String attestationYear = null;

    // missing criteria where all in the set are required
    private ArrayList<String> missingAnd = new ArrayList<String>();
    // missing 1 criteria from each of the following sets
    private List<ArrayList<String>> missingOr = new ArrayList<ArrayList<String>>();

    private CertificationIdPercentages percents = new CertificationIdPercentages();
    private CertificationIdValidationCounts counts = new CertificationIdValidationCounts();
    private boolean valid = false;

    protected abstract boolean onValidate();

    protected abstract boolean isCriteriaValid();

    public boolean validate(List<CertificationCriterionDTO> certDtos, List<CQMMetDTO> cqmDtos, List<Integer> years) {
        SortedSet<Integer> editionYears = new TreeSet<Integer>();
        editionYears.addAll(years);
        this.attestationYear = Validator.calculateAttestationYear(this.editionYears);
        this.valid = this.onValidate();
        this.calculatePercentages();
        return this.isValid();
    }

    @SuppressWarnings("checkstyle:linelength")
    protected void calculatePercentages() {
        this.percents.setCriteriaMet(
                this.counts.getCriteriaRequired() == 0 ? 0 : Math.min(
                            (int) Math.floor(this.counts.getCriteriaRequiredMet() * PERCENTAGE_MULTIPLER) / this.counts.getCriteriaRequired(),
                            PERCENTAGE_MAX));
        this.percents.setCqmDomains(
                this.counts.getDomainsRequired() == 0 ? 0 : Math.min(
                            (int) Math.floor(this.counts.getDomainsRequiredMet() * PERCENTAGE_MULTIPLER) / this.counts.getDomainsRequired(),
                            PERCENTAGE_MAX));

        this.percents.setCqmsInpatient(
                this.counts.getCqmsInpatientRequired() == 0 ? 0 : Math.min(
                            (int) Math.floor(this.counts.getCqmsInpatientRequiredMet() * PERCENTAGE_MULTIPLER) / this.counts.getCqmsInpatientRequired(),
                            PERCENTAGE_MAX));

        this.percents.setCqmsAmbulatory(
                this.counts.getCqmsAmbulatoryRequired() + this.counts.getCqmsAmbulatoryCoreRequired() == 0
                    ? 0 : Math.min(
                            (int) Math.floor(
                                    ((this.counts.getCqmsAmbulatoryCoreRequiredMet()
                                            + Math.min(this.counts.getCqmsAmbulatoryRequiredMet(), this.counts.getCqmsAmbulatoryRequired()))
                                            / (double) (this.counts.getCqmsAmbulatoryRequired() + this.counts.getCqmsAmbulatoryCoreRequired())) * PERCENTAGE_MULTIPLER),
                            PERCENTAGE_MAX));
    }

    public static String calculateAttestationYear(SortedSet<Integer> editionYears) {
        String attYearString = null;

        if ((null != editionYears) && (editionYears.size() > 0)) {

            // Get the lowest year...
            attYearString = editionYears.first().toString();

            // ...if there are two years then we have a hybrid
            // so add the second year.
            if (editionYears.size() > 1) {
                attYearString += "/" + editionYears.last().toString();
            }
        }

        return attYearString;
    }

    protected Boolean criteriaMetContainsCriterion(String criterion) {
        Boolean found = false;
        for (CertificationCriterionDTO cert : criteriaMet.keySet()) {
            if (cert.getNumber().equalsIgnoreCase(criterion)) {
                found = true;
            }
        }
        return found;
    }
}
