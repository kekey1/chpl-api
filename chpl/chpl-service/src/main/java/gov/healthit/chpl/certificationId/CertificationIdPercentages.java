package gov.healthit.chpl.certificationId;

import lombok.Data;

@Data
public class CertificationIdPercentages {
    private Integer criteriaMet;
    private Integer cqmDomains;
    private Integer cqmsInpatient;
    private Integer cqmsAmbulatory;
}
