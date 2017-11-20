package gov.healthit.chpl.domain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.springframework.util.StringUtils;

import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;

@XmlType(namespace = "http://chpl.healthit.gov/listings")
@XmlAccessorType(XmlAccessType.FIELD)
public class CertifiedProduct implements Serializable {
    private static final long serialVersionUID = -6634520925641244762L;

    /**
     * Listing internal ID
     */
    @XmlElement(required = true)
    private Long id;

    /**
     * The unique CHPL ID of the certified product. New uploads to CHPL will use
     * the format: CertEdYr.ATL.ACB.Dev.Prod.Ver.ICS.AddS.Date
     */
    @XmlElement(required = true)
    private String chplProductNumber;

    /**
     * The last time this listing was modified in any way given in milliseconds
     * since epoch.
     */
    @XmlElement(required = false, nillable = true)
    private String lastModifiedDate;

    /**
     * Edition of the listing. Ex: 2011, 2014, or 2015
     */
    @XmlElement(required = false, nillable = true)
    private String edition;

    /**
     * The date the listing was certified given in milliseconds since epoch.
     */
    @XmlElement(required = false, nillable = true)
    private long certificationDate;

    public CertifiedProduct() {
    }

    public CertifiedProduct(CertifiedProductDetailsDTO dto) {
        this.id = dto.getId();
        if (!StringUtils.isEmpty(dto.getChplProductNumber())) {
            this.setChplProductNumber(dto.getChplProductNumber());
        } else {
            this.setChplProductNumber(dto.getYearCode() + "." + dto.getTestingLabCode() + "."
                    + dto.getCertificationBodyCode() + "." + dto.getDeveloper().getDeveloperCode() + "."
                    + dto.getProductCode() + "." + dto.getVersionCode() + "." + dto.getIcsCode() + "."
                    + dto.getAdditionalSoftwareCode() + "." + dto.getCertifiedDateCode());
        }
        this.setLastModifiedDate(dto.getLastModifiedDate() != null ? dto.getLastModifiedDate().getTime() + "" : "");
        this.edition = dto.getYear();
        this.certificationDate = (dto.getCertificationDate() != null ? dto.getCertificationDate().getTime() : -1);
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getChplProductNumber() {
        return chplProductNumber;
    }

    public void setChplProductNumber(final String chplProductNumber) {
        this.chplProductNumber = chplProductNumber;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(final String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(final String edition) {
        this.edition = edition;
    }

    public long getCertificationDate() {
        return certificationDate;
    }

    public void setCertificationDate(final long certificationDate) {
        this.certificationDate = certificationDate;
    }
}
