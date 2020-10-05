package gov.healthit.chpl.certificationId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.healthit.chpl.dto.CertificationCriterionDTO;

public class Validator2015 extends Validator {

    protected static final List<String> REQUIRED_CRITERIA = new ArrayList<String>(Arrays.asList("170.315 (a)(5)",
            "170.315 (a)(9)", "170.315 (a)(14)", "170.315 (c)(1)", "170.315 (g)(7)"));

    protected static final List<String> CURES_REQUIRED_CRITERIA = new ArrayList<String>(Arrays.asList("170.315 (b)(1)",
            "170.315 (g)(9)"));

    protected static final List<String> AA_CRITERIA_OR = new ArrayList<String>(Arrays.asList("170.315 (g)(8)",
            "170.315 (g)(10)"));

    protected static final List<String> CPOE_CRITERIA_OR = new ArrayList<String>(Arrays.asList("170.315 (a)(1)",
            "170.315 (a)(2)", "170.315 (a)(3)"));

    protected static final List<String> DP_CRITERIA_OR = new ArrayList<String>(Arrays.asList("170.315 (h)(1)",
            "170.315 (h)(2)"));

    public Validator2015() {
        this.getCounts().setCriteriaRequired(REQUIRED_CRITERIA.size() + CURES_REQUIRED_CRITERIA.size());
        this.getCounts().setCriteriaRequiredMet(0);
        this.getCounts().setCriteriaAaRequired(1);
        this.getCounts().setCriteriaAaRequiredMet(0);
        this.getCounts().setCriteriaCpoeRequired(1);
        this.getCounts().setCriteriaCpoeRequiredMet(0);
        this.getCounts().setCriteriaDpRequired(1);
        this.getCounts().setCriteriaDpRequiredMet(0);
        this.getCounts().setCqmsInpatientRequired(0);
        this.getCounts().setCqmsInpatientRequiredMet(0);
        this.getCounts().setCqmsAmbulatoryRequired(0);
        this.getCounts().setCqmsAmbulatoryRequiredMet(0);
        this.getCounts().setCqmsAmbulatoryCoreRequired(0);
        this.getCounts().setCqmsAmbulatoryCoreRequiredMet(0);
        this.getCounts().setDomainsRequired(0);
        this.getCounts().setDomainsRequiredMet(0);
    }

    public boolean onValidate() {
        return isCriteriaValid();
    }

    protected boolean isCriteriaValid() {
        boolean criteriaValid = true;
        for (String crit : REQUIRED_CRITERIA) {
            if (criteriaMetContainsCriterion(crit)) {
                this.getCounts().setCriteriaRequiredMet(this.getCounts().getCriteriaRequiredMet() + 1);
            } else {
                this.getMissingAnd().add(crit);
                criteriaValid = false;
            }
        }
        for (String crit : CURES_REQUIRED_CRITERIA) {
            Boolean foundOriginal = false;
            Boolean foundRevised = false;
            for (CertificationCriterionDTO cert : criteriaMet.keySet()) {
                if (cert.getNumber().equalsIgnoreCase(crit)) {
                    if (cert.getTitle().contains("Cures Update")) {
                        foundRevised = true;
                    } else {
                        foundOriginal = true;
                    }
                }
            }
            if (foundOriginal || foundRevised) {
                this.getCounts().setCriteriaRequiredMet(this.getCounts().getCriteriaRequiredMet() + 1);
            } else {
                this.getMissingOr().add(new ArrayList<String>(new ArrayList<String>(Arrays.asList(crit,
                        crit + " (Cures Update)"))));
                criteriaValid = false;
            }
        }
        boolean aaValid = isAAValid();
        boolean cpoeValid = isCPOEValid();
        boolean dpValid = isDPValid();

        this.getCounts().setCriteriaRequired(this.getCounts().getCriteriaRequired()
                + this.getCounts().getCriteriaAaRequired()
                + this.getCounts().getCriteriaCpoeRequired()
                + this.getCounts().getCriteriaDpRequired());
        this.getCounts().setCriteriaRequiredMet(this.getCounts().getCriteriaRequiredMet()
                + this.getCounts().getCriteriaAaRequiredMet()
                + this.getCounts().getCriteriaCpoeRequiredMet()
                + this.getCounts().getCriteriaDpRequiredMet());

        return (criteriaValid && aaValid && cpoeValid && dpValid);
    }

    protected boolean isAAValid() {
        for (String crit : AA_CRITERIA_OR) {
            if (criteriaMetContainsCriterion(crit)) {
                this.getCounts().setCriteriaAaRequiredMet(1);
                return true;
            }
        }
        this.getMissingOr().add(new ArrayList<String>(AA_CRITERIA_OR));
        return false;
    }

    protected boolean isCPOEValid() {
        for (String crit : CPOE_CRITERIA_OR) {
            if (criteriaMetContainsCriterion(crit)) {
                this.getCounts().setCriteriaCpoeRequiredMet(1);
                return true;
            }
        }
        this.getMissingOr().add(new ArrayList<String>(CPOE_CRITERIA_OR));
        return false;
    }

    protected boolean isDPValid() {
        for (String crit : DP_CRITERIA_OR) {
            if (criteriaMetContainsCriterion(crit)) {
                this.getCounts().setCriteriaDpRequiredMet(1);
                return true;
            }
        }
        this.getMissingOr().add(new ArrayList<String>(DP_CRITERIA_OR));
        return false;
    }
}
