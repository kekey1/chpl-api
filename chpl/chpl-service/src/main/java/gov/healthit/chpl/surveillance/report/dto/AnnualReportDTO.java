package gov.healthit.chpl.surveillance.report.dto;

import gov.healthit.chpl.dto.CertificationBodyDTO;
import gov.healthit.chpl.surveillance.report.entity.AnnualReportEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnnualReportDTO {

    private Long id;
    private Integer year;
    private CertificationBodyDTO acb;
    private String obstacleSummary;
    private String findingsSummary;

    public AnnualReportDTO(AnnualReportEntity entity) {
        this.id = entity.getId();
        this.year = entity.getYear();
        this.obstacleSummary = entity.getObstacleSummary();
        this.findingsSummary = entity.getFindingsSummary();
        if (entity.getAcb() != null) {
            this.acb = new CertificationBodyDTO(entity.getAcb());
        } else {
            this.acb = new CertificationBodyDTO();
            this.acb.setId(entity.getCertificationBodyId());
        }
    }
}
