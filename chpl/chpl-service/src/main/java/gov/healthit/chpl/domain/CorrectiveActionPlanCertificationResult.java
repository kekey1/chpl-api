package gov.healthit.chpl.domain;

import gov.healthit.chpl.dto.CorrectiveActionPlanCertificationResultDTO;

public class CorrectiveActionPlanCertificationResult {
	
	private Long id;
	private Long certificationCriterionId;
	private String certificationCriterionNumber;
	private String certificationCriterionTitle;
	private String acbSummary;
	private String developerSummary;
	private String resolution;
	
	public CorrectiveActionPlanCertificationResult() {}
	public CorrectiveActionPlanCertificationResult(CorrectiveActionPlanCertificationResultDTO dto) {
		this.id = dto.getId();
		if(dto.getCertCriterion() != null) {
			this.certificationCriterionId = dto.getCertCriterion().getId();
			this.certificationCriterionNumber = dto.getCertCriterion().getNumber();
			this.certificationCriterionTitle = dto.getCertCriterion().getTitle();
		}
		this.acbSummary = dto.getAcbSummary();
		this.developerSummary = dto.getDeveloperSummary();
		this.resolution = dto.getResolution();
	}
		
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getCertificationCriterionId() {
		return certificationCriterionId;
	}
	public void setCertificationCriterionId(Long certificationCriterionId) {
		this.certificationCriterionId = certificationCriterionId;
	}
	public String getCertificationCriterionNumber() {
		return certificationCriterionNumber;
	}
	public void setCertificationCriterionNumber(String certificationCriterionNumber) {
		this.certificationCriterionNumber = certificationCriterionNumber;
	}
	public String getCertificationCriterionTitle() {
		return certificationCriterionTitle;
	}
	public void setCertificationCriterionTitle(String certificationCriterionTitle) {
		this.certificationCriterionTitle = certificationCriterionTitle;
	}
	public String getAcbSummary() {
		return acbSummary;
	}
	public void setAcbSummary(String acbSummary) {
		this.acbSummary = acbSummary;
	}
	public String getDeveloperSummary() {
		return developerSummary;
	}
	public void setDeveloperSummary(String developerSummary) {
		this.developerSummary = developerSummary;
	}
	public String getResolution() {
		return resolution;
	}
	public void setResolution(String resolution) {
		this.resolution = resolution;
	}
	
}
