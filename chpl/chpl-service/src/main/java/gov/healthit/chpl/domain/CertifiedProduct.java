package gov.healthit.chpl.domain;

import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;

public class CertifiedProduct {
	
	private Long id;
    private String chplProductNumber;
	private String lastModifiedDate;
	
	
	public CertifiedProduct(CertifiedProductDetailsDTO dto) {
		this.id = dto.getId();
		if(dto.getYear().equals("2011") || dto.getYear().equals("2014")) {
			this.chplProductNumber = dto.getChplProductNumber();
		} else {
			this.setChplProductNumber(dto.getTestingLabCode() + "." + dto.getCertificationBodyCode() + "." + 
					dto.getDeveloperCode() + "." + dto.getProductCode() + "." + dto.getVersionCode() + 
					"." + dto.getIcsCode() + "." + dto.getAdditionalSoftwareCode() + 
					"." + dto.getCertifiedDateCode());
		}
		this.setLastModifiedDate(dto.getLastModifiedDate().getTime() + "");
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getChplProductNumber() {
		return chplProductNumber;
	}
	public void setChplProductNumber(String chplProductNumber) {
		this.chplProductNumber = chplProductNumber;
	}
	public String getLastModifiedDate() {
		return lastModifiedDate;
	}
	public void setLastModifiedDate(String lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
}
