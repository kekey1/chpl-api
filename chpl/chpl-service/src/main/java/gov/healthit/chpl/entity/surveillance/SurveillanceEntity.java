package gov.healthit.chpl.entity.surveillance;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import gov.healthit.chpl.entity.listing.CertifiedProductEntity;
import gov.healthit.chpl.util.Util;

@Entity
@Table(name = "surveillance")
public class SurveillanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "friendly_id", insertable = false, updatable = false)
    private String friendlyId;

    @Column(name = "certified_product_id")
    private Long certifiedProductId;

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "certified_product_id", insertable = false, updatable = false)
    private CertifiedProductEntity certifiedProduct;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "type_id")
    private Long surveillanceTypeId;

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", insertable = false, updatable = false)
    private SurveillanceTypeEntity surveillanceType;

    @Column(name = "randomized_sites_used")
    private Integer numRandomizedSites;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "last_modified_user")
    private Long lastModifiedUser;

    @Column(name = "creation_date", insertable = false, updatable = false)
    private Date creationDate;

    @Column(name = "last_modified_date", insertable = false, updatable = false)
    private Date lastModifiedDate;

    @Column(name = "user_permission_id")
    private Long userPermissionId;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "surveillanceId")
    @Basic(optional = false)
    @Column(name = "surveillance_id", nullable = false)
    @Where(clause = "deleted <> 'true'")
    private Set<SurveillanceRequirementEntity> surveilledRequirements = new HashSet<SurveillanceRequirementEntity>();

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Date getStartDate() {
        return Util.getNewDate(startDate);
    }

    public void setStartDate(final Date startDate) {
        this.startDate = Util.getNewDate(startDate);
    }

    public Date getEndDate() {
        return Util.getNewDate(endDate);
    }

    public void setEndDate(final Date endDate) {
        this.endDate = Util.getNewDate(endDate);
    }

    public Integer getNumRandomizedSites() {
        return numRandomizedSites;
    }

    public void setNumRandomizedSites(final Integer numRandomizedSites) {
        this.numRandomizedSites = numRandomizedSites;
    }

    public Date getCreationDate() {
        return Util.getNewDate(creationDate);
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = Util.getNewDate(creationDate);
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    public Date getLastModifiedDate() {
        return Util.getNewDate(lastModifiedDate);
    }

    public void setLastModifiedDate(final Date lastModifiedDate) {
        this.lastModifiedDate = Util.getNewDate(lastModifiedDate);
    }

    public Long getLastModifiedUser() {
        return lastModifiedUser;
    }

    public void setLastModifiedUser(final Long lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }

    public CertifiedProductEntity getCertifiedProduct() {
        return certifiedProduct;
    }

    public void setCertifiedProduct(final CertifiedProductEntity certifiedProduct) {
        this.certifiedProduct = certifiedProduct;
    }

    public Long getCertifiedProductId() {
        return certifiedProductId;
    }

    public void setCertifiedProductId(final Long certifiedProductId) {
        this.certifiedProductId = certifiedProductId;
    }

    public Long getSurveillanceTypeId() {
        return surveillanceTypeId;
    }

    public void setSurveillanceTypeId(final Long surveillanceTypeId) {
        this.surveillanceTypeId = surveillanceTypeId;
    }

    public SurveillanceTypeEntity getSurveillanceType() {
        return surveillanceType;
    }

    public void setSurveillanceType(final SurveillanceTypeEntity surveillanceType) {
        this.surveillanceType = surveillanceType;
    }

    public Set<SurveillanceRequirementEntity> getSurveilledRequirements() {
        return surveilledRequirements;
    }

    public void setSurveilledRequirements(final Set<SurveillanceRequirementEntity> surveilledRequirements) {
        this.surveilledRequirements = surveilledRequirements;
    }

    public String getFriendlyId() {
        return friendlyId;
    }

    public void setFriendlyId(final String friendlyId) {
        this.friendlyId = friendlyId;
    }

    public Long getUserPermissionId() {
        return userPermissionId;
    }

    public void setUserPermissionId(final Long userPermissionId) {
        this.userPermissionId = userPermissionId;
    }
}
