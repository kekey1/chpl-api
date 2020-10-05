package gov.healthit.chpl.web.controller.results;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import gov.healthit.chpl.certificationId.CertificationIdPercentages;
import gov.healthit.chpl.certificationId.CertificationIdValidationCounts;
import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;
import lombok.Data;

@Data
public class CertificationIdValidationResponse implements Serializable {
    private static final long serialVersionUID = 4350936762994127624L;

    private List<Product> products;
    private String ehrCertificationId;
    private CertificationIdValidationCounts metCounts;
    private CertificationIdPercentages metPercentages;
    private ArrayList<String> missingAnd = new ArrayList<String>();
    private List<ArrayList<String>> missingOr = new ArrayList<ArrayList<String>>();
    private String year;
    private boolean isValid;

    //keeping these getters and setters for backwards compatibility; lombok ones are different
    public boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    @Data
    public static class Product implements Serializable {
        private static final long serialVersionUID = 1487852426085184818L;
        private String name;
        private Long productId;
        private String version;

        public Product(CertifiedProductDetailsDTO dto) {
            this.name = dto.getProduct().getName();
            this.productId = dto.getId();
            this.version = dto.getVersion().getVersion();
        }
    }
}
