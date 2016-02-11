package gov.healthit.chpl.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import gov.healthit.chpl.entity.CertifiedProductDetailsEntity;

public class CertifiedProductDetailsDTO {
	
	private Long id;
	private String productCode;
	private String versionCode;
	private String icsCode;
	private String additionalSoftwareCode;
	private String certifiedDateCode;
    private Long testingLabId;
    private String testingLabName;
    private String testingLabCode;
    private String chplProductNumber;
    private String reportFileLocation;
    private String acbCertificationId;
    private Long practiceTypeId;
    private String practiceTypeName;
    private Long productClassificationTypeId;
    private String otherAcb;
    private Long certificationStatusId;
    private String certificationStatusName;
    private Long certificationEditionId;
    private String year;
    private Long certificationBodyId;
    private String certificationBodyName;
    private String certificationBodyCode;
    private String productClassificationName;
    private Long productVersionId;
    private String productVersion;
    private Long productId;
    private String productName;
    private Long developerId;
    private String developerName;
    private String developerCode;
    private Date certificationDate;
    private Integer countCertifications;
    private Integer countCqms;
    private Integer countCorrectiveActionPlans;
    private Boolean visibleOnChpl;
    private Date lastModifiedDate;
    private Boolean privacyAttestation;
	private String termsOfUse;
	private String apiDocumentation;
	private String ics;
	private Boolean sedTesting;
	private Boolean qmsTesting;
	private Boolean transparencyAttestation;
	
    private List<CertificationResultDetailsDTO> certResults;
    private List<CQMResultDetailsDTO> cqmResults;
    private List<AdditionalSoftwareDTO> additionalSoftware;
    
    public CertifiedProductDetailsDTO(){
    	certResults = new ArrayList<CertificationResultDetailsDTO>();
    	cqmResults = new ArrayList<CQMResultDetailsDTO>();
    	additionalSoftware = new ArrayList<AdditionalSoftwareDTO>();
    }
    
    public CertifiedProductDetailsDTO(CertifiedProductDetailsEntity entity){
    	this();
    	
    	this.id = entity.getId();
    	this.testingLabCode = entity.getTestingLabCode();
    	this.productCode = entity.getProductCode();
    	this.versionCode = entity.getVersionCode();
    	this.icsCode = entity.getIcsCode();
    	this.additionalSoftwareCode = entity.getAdditionalSoftwareCode();
    	this.certifiedDateCode = entity.getCertifiedDateCode();
    	this.acbCertificationId = entity.getAcbCertificationId();
    	this.certificationBodyId = entity.getCertificationBodyId();
    	this.certificationBodyName = entity.getCertificationBodyName();
    	this.certificationBodyCode = entity.getCertificationBodyCode();
    	this.certificationEditionId = entity.getCertificationEditionId();
    	this.certificationStatusId = entity.getCertificationStatusId();
    	this.certificationStatusName = entity.getCertificationStatusName();
    	this.chplProductNumber = entity.getChplProductNumber();
    	this.otherAcb = entity.getOtherAcb();
    	this.practiceTypeId = entity.getPracticeTypeId();
       	this.practiceTypeName = entity.getPracticeTypeName();
    	this.productClassificationName = entity.getProductClassificationName();
    	this.productClassificationTypeId = entity.getProductClassificationTypeId();
    	this.productId = entity.getProductId();
    	this.productName = entity.getProductName();
    	this.productVersion = entity.getProductVersion();
    	this.productVersionId = entity.getProductVersionId();
    	this.reportFileLocation = entity.getReportFileLocation();
    	this.testingLabId = entity.getTestingLabId();
    	this.testingLabName = entity.getTestingLabName();
    	this.developerId = entity.getDeveloperId();
    	this.developerName = entity.getDeveloperName();
    	this.developerCode = entity.getDeveloperCode();
    	this.visibleOnChpl = entity.getVisibleOnChpl();
    	this.privacyAttestation = entity.getPrivacyAttestation();
    	this.termsOfUse = entity.getTermsOfUse();
    	this.apiDocumentation = entity.getApiDocumentation();
    	this.ics = entity.getIcs();
    	this.sedTesting = entity.getSedTesting();
    	this.qmsTesting = entity.getQmsTesting();
    	this.transparencyAttestation = entity.getTransparencyAttestation();
    	this.year = entity.getYear();
    	this.certificationDate = entity.getCertificationDate();
    	this.countCqms = entity.getCountCqms();
    	this.countCertifications = entity.getCountCertifications();
    	this.countCorrectiveActionPlans = entity.getCountCorrectiveActionPlans();
    	this.lastModifiedDate = entity.getLastModifiedDate();
    }
    
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getTestingLabId() {
		return testingLabId;
	}
	public void setTestingLabId(Long testingLabId) {
		this.testingLabId = testingLabId;
	}
	public String getChplProductNumber() {
		return chplProductNumber;
	}
	public void setChplProductNumber(String chplProductNumber) {
		this.chplProductNumber = chplProductNumber;
	}
	public String getReportFileLocation() {
		return reportFileLocation;
	}
	public void setReportFileLocation(String reportFileLocation) {
		this.reportFileLocation = reportFileLocation;
	}
	public String getAcbCertificationId() {
		return acbCertificationId;
	}
	public void setAcbCertificationId(String acbCertificationId) {
		this.acbCertificationId = acbCertificationId;
	}
	public Long getPracticeTypeId() {
		return practiceTypeId;
	}
	public void setPracticeTypeId(Long practiceTypeId) {
		this.practiceTypeId = practiceTypeId;
	}
	public Long getProductClassificationTypeId() {
		return productClassificationTypeId;
	}
	public void setProductClassificationTypeId(Long productClassificationTypeId) {
		this.productClassificationTypeId = productClassificationTypeId;
	}
	public String getOtherAcb() {
		return otherAcb;
	}
	public void setOtherAcb(String otherAcb) {
		this.otherAcb = otherAcb;
	}
	public Long getCertificationStatusId() {
		return certificationStatusId;
	}
	public void setCertificationStatusId(Long certificationStatusId) {
		this.certificationStatusId = certificationStatusId;
	}
	public Long getCertificationEditionId() {
		return certificationEditionId;
	}
	public void setCertificationEditionId(Long certificationEditionId) {
		this.certificationEditionId = certificationEditionId;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public Long getCertificationBodyId() {
		return certificationBodyId;
	}
	public void setCertificationBodyId(Long certificationBodyId) {
		this.certificationBodyId = certificationBodyId;
	}
	public String getCertificationBodyName() {
		return certificationBodyName;
	}
	public void setCertificationBodyName(String certificationBodyName) {
		this.certificationBodyName = certificationBodyName;
	}
	public Long getProductVersionId() {
		return productVersionId;
	}
	public void setProductVersionId(Long productVersionId) {
		this.productVersionId = productVersionId;
	}
	public String getProductVersion() {
		return productVersion;
	}
	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public Long getDeveloperId() {
		return developerId;
	}
	public void setDeveloperId(Long developerId) {
		this.developerId = developerId;
	}
	public String getDeveloperName() {
		return developerName;
	}
	public void setDeveloperName(String developerName) {
		this.developerName = developerName;
	}
	public String getPracticeTypeName() {
		return practiceTypeName;
	}
	public void setPracticeTypeName(String practiceTypeName) {
		this.practiceTypeName = practiceTypeName;
	}
	public Date getCertificationDate() {
		return certificationDate;
	}
	public void setCertificationDate(Date certificationDate) {
		this.certificationDate = certificationDate;
	}
	public String getProductClassificationName() {
		return productClassificationName;
	}
	public void setProductClassificationName(String productClassificationName) {
		this.productClassificationName = productClassificationName;
	}
	public Integer getCountCertifications() {
		return countCertifications;
	}
	public void setCountCertifications(Integer countCertifications) {
		this.countCertifications = countCertifications;
	}
	public Integer getCountCqms() {
		return countCqms;
	}
	public void setCountCqms(Integer countCqms) {
		this.countCqms = countCqms;
	}
	public Boolean getVisibleOnChpl() {
		return visibleOnChpl;
	}
	public void setVisibleOnChpl(Boolean visibleOnChpl) {
		this.visibleOnChpl = visibleOnChpl;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public String getCertificationStatusName() {
		return certificationStatusName;
	}

	public void setCertificationStatusName(String certificationStatusName) {
		this.certificationStatusName = certificationStatusName;
	}

	public Boolean getPrivacyAttestation() {
		return privacyAttestation;
	}

	public void setPrivacyAttestation(Boolean privacyAttestation) {
		this.privacyAttestation = privacyAttestation;
	}

	public Integer getCountCorrectiveActionPlans() {
		return countCorrectiveActionPlans;
	}

	public void setCountCorrectiveActionPlans(Integer countCorrectiveActionPlans) {
		this.countCorrectiveActionPlans = countCorrectiveActionPlans;
	}
	public List<CertificationResultDetailsDTO> getCertResults() {
		return certResults;
	}

	public void setCertResults(List<CertificationResultDetailsDTO> certResults) {
		this.certResults = certResults;
	}

	public List<CQMResultDetailsDTO> getCqmResults() {
		return cqmResults;
	}

	public void setCqmResults(List<CQMResultDetailsDTO> cqmResults) {
		this.cqmResults = cqmResults;
	}

	public List<AdditionalSoftwareDTO> getAdditionalSoftware() {
		return additionalSoftware;
	}

	public void setAdditionalSoftware(List<AdditionalSoftwareDTO> additionalSoftware) {
		this.additionalSoftware = additionalSoftware;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public String getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(String versionCode) {
		this.versionCode = versionCode;
	}

	public String getAdditionalSoftwareCode() {
		return additionalSoftwareCode;
	}

	public void setAdditionalSoftwareCode(String additionalSoftwareCode) {
		this.additionalSoftwareCode = additionalSoftwareCode;
	}

	public String getCertifiedDateCode() {
		return certifiedDateCode;
	}

	public void setCertifiedDateCode(String certifiedDateCode) {
		this.certifiedDateCode = certifiedDateCode;
	}

	public String getCertificationBodyCode() {
		return certificationBodyCode;
	}

	public void setCertificationBodyCode(String certificationBodyCode) {
		this.certificationBodyCode = certificationBodyCode;
	}

	public String getDeveloperCode() {
		return developerCode;
	}

	public void setDeveloperCode(String developerCode) {
		this.developerCode = developerCode;
	}

	public String getIcsCode() {
		return icsCode;
	}

	public void setIcsCode(String icsCode) {
		this.icsCode = icsCode;
	}

	public String getTermsOfUse() {
		return termsOfUse;
	}

	public void setTermsOfUse(String termsOfUse) {
		this.termsOfUse = termsOfUse;
	}

	public String getApiDocumentation() {
		return apiDocumentation;
	}

	public void setApiDocumentation(String apiDocumentation) {
		this.apiDocumentation = apiDocumentation;
	}

	public Boolean getTransparencyAttestation() {
		return transparencyAttestation;
	}

	public void setTransparencyAttestation(Boolean transparencyAttestation) {
		this.transparencyAttestation = transparencyAttestation;
	}

	public String getTestingLabName() {
		return testingLabName;
	}

	public void setTestingLabName(String testingLabName) {
		this.testingLabName = testingLabName;
	}

	public String getTestingLabCode() {
		return testingLabCode;
	}

	public void setTestingLabCode(String testingLabCode) {
		this.testingLabCode = testingLabCode;
	}

	public String getIcs() {
		return ics;
	}

	public void setIcs(String ics) {
		this.ics = ics;
	}

	public Boolean getSedTesting() {
		return sedTesting;
	}

	public void setSedTesting(Boolean sedTesting) {
		this.sedTesting = sedTesting;
	}

	public Boolean getQmsTesting() {
		return qmsTesting;
	}

	public void setQmsTesting(Boolean qmsTesting) {
		this.qmsTesting = qmsTesting;
	}
}
