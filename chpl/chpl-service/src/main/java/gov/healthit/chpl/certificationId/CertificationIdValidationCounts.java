package gov.healthit.chpl.certificationId;

import lombok.Data;

@Data
public class CertificationIdValidationCounts {
    private int criteriaRequired;
    private int criteriaRequiredMet;
    private int criteriaAaRequired;
    private int criteriaAaRequiredMet;
    private int criteriaCpoeRequired;
    private int criteriaCpoeRequiredMet;
    private int criteriaDpRequired;
    private int criteriaDpRequiredMet;
    private int cqmsInpatientRequired;
    private int cqmsInpatientRequiredMet;
    private int cqmsAmbulatoryRequired;
    private int cqmsAmbulatoryRequiredMet;
    private int cqmsAmbulatoryCoreRequired;
    private int cqmsAmbulatoryCoreRequiredMet;
    private int domainsRequired;
    private int domainsRequiredMet;
}
