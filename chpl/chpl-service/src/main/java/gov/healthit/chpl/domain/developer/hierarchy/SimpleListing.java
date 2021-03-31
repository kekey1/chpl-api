package gov.healthit.chpl.domain.developer.hierarchy;

import org.apache.commons.lang.ObjectUtils;

import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.domain.CertifiedProduct;
import lombok.Data;

@Data
public class SimpleListing extends CertifiedProduct {
    private static final long serialVersionUID = 6827193932953676099L;

    private CertificationBody acb;
    private Long openSurveillanceCount;
    private Long closedSurveillanceCount;
    private Long openSurveillanceNonConformityCount;
    private Long closedSurveillanceNonConformityCount;

    public SimpleListing() {
        super();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SimpleListing)) {
            return false;
        }
        SimpleListing otherListing = (SimpleListing) obj;
        return ObjectUtils.equals(this.getId(), otherListing.getId());
    }

    public int hashCode() {
        if (this.getId() == null) {
            return -1;
        }

        return this.getId().hashCode();
    }
}
