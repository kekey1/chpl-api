package gov.healthit.chpl.web.controller;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.domain.CorrectiveActionPlanCertificationResult;
import gov.healthit.chpl.domain.CorrectiveActionPlanDetails;
import gov.healthit.chpl.domain.CorrectiveActionPlanResults;
import gov.healthit.chpl.dto.CertificationCriterionDTO;
import gov.healthit.chpl.dto.CertifiedProductDTO;
import gov.healthit.chpl.dto.CorrectiveActionPlanCertificationResultDTO;
import gov.healthit.chpl.dto.CorrectiveActionPlanDTO;
import gov.healthit.chpl.dto.CorrectiveActionPlanDocumentationDTO;
import gov.healthit.chpl.manager.CertifiedProductManager;
import gov.healthit.chpl.manager.CorrectiveActionPlanManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import io.swagger.annotations.ExternalDocs;

@Api(value="corrective-action-plan")
@RestController
@RequestMapping("/corrective_action_plan")
public class CorrectiveActionPlanController {
	
	private static final Logger logger = LogManager.getLogger(CorrectiveActionPlanController.class);

	@Autowired CorrectiveActionPlanManager capManager;
	@Autowired CertifiedProductManager productManager;
	
	@ApiOperation(value="List corrective action plans for a certified product.", 
			notes="List all corrective action plans, both open and resolved, for a certified product.")
	@RequestMapping(value="/", method=RequestMethod.GET,
			produces="application/json; charset=utf-8")
	public @ResponseBody CorrectiveActionPlanResults getCorrectiveActionPlansForCertifiedProduct(
			@RequestParam(value = "certifiedProductId", required=true) Long cpId) throws EntityRetrievalException {
		List<CorrectiveActionPlanDetails> plans = capManager.getPlansForCertifiedProductDetails(cpId);
		
		CorrectiveActionPlanResults results = new CorrectiveActionPlanResults();
		results.getPlans().addAll(plans);
		return results;
	}
	
	@ApiOperation(value="Get corrective action plan details.", 
			notes="Get all of the information about a specific corrective action plan. These details "
					+ " include the presence and associated id's of any uploaded supporting "
					+ " documentation but not the contents of those documents. Use /documentation/{capDocId} to "
					+ " view the files.")
	@RequestMapping(value="/{capId}", method=RequestMethod.GET,
			produces="application/json; charset=utf-8")
	public @ResponseBody CorrectiveActionPlanDetails getCorrectiveActionPlanById(@PathVariable("capId") Long capId) throws EntityRetrievalException {
		return capManager.getPlanDetails(capId);
	}
	
	@ApiOperation(value="Download CAP supporting documentation.", 
			notes="Download a specific file that was previously uploaded to a corrective action plan.")
	@RequestMapping(value="/documentation/{capDocId}", method=RequestMethod.GET)
	public void getCorrectiveActionPlanDocumentationById(
			@PathVariable("capDocId") Long capDocId, HttpServletResponse response) throws EntityRetrievalException, IOException {
		CorrectiveActionPlanDocumentationDTO doc = capManager.getDocumentationById(capDocId);
		
		if(doc != null && doc.getFileData() != null && doc.getFileData().length > 0) {
	        ByteArrayInputStream inputStream = new ByteArrayInputStream(doc.getFileData());
	        // get MIME type of the file
	        String mimeType = doc.getFileType();
	        if (mimeType == null) {
	            // set to binary type if MIME mapping not found
	            mimeType = "application/octet-stream";
	        }
	        // set content attributes for the response
	        response.setContentType(mimeType);
	        response.setContentLength((int) doc.getFileData().length);
	 
	        // set headers for the response
	        String headerKey = "Content-Disposition";
	        String headerValue = String.format("attachment; filename=\"%s\"",
	                doc.getFileName());
	        response.setHeader(headerKey, headerValue);
	 
	        // get output stream of the response
	        OutputStream outStream = response.getOutputStream();
	 
	        byte[] buffer = new byte[1024];
	        int bytesRead = -1;
	 
	        // write bytes read from the input stream into the output stream
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	            outStream.write(buffer, 0, bytesRead);
	        }
	 
	        inputStream.close();
	        outStream.close();	
		}   
	}

	@ApiOperation(value="Update a corrective action plan.", 
			notes="The logged in user must have ROLE_ADMIN or ROLE_ACB_ADMIN and administrative "
					+ "authority on the ACB associated with the corrective action plan.")
	@RequestMapping(value="/update", method=RequestMethod.POST,
			produces="application/json; charset=utf-8")
	public @ResponseBody CorrectiveActionPlanDetails update(@RequestBody(required=true) CorrectiveActionPlanDetails updateRequest) 
		throws EntityCreationException, EntityRetrievalException, JsonProcessingException,
		InvalidArgumentsException {
		
		CorrectiveActionPlanDTO toUpdate = new CorrectiveActionPlanDTO();
		toUpdate.setId(updateRequest.getId());
		toUpdate.setAcbSummary(updateRequest.getAcbSummary());
		toUpdate.setActualCompletionDate(updateRequest.getActualCompletionDate());
		toUpdate.setApprovalDate(updateRequest.getApprovalDate());
		toUpdate.setCertifiedProductId(updateRequest.getCertifiedProductId());
		toUpdate.setDeveloperSummary(updateRequest.getDeveloperSummary());
		toUpdate.setEffectiveDate(updateRequest.getEffectiveDate());
		toUpdate.setEstimatedCompletionDate(updateRequest.getEstimatedCompletionDate());
		toUpdate.setNoncomplainceDate(updateRequest.getNoncomplianceDate());
		toUpdate.setResolution(updateRequest.getResolution());
		
		//update the plan info
		Long owningAcbId = null;
		CorrectiveActionPlanDTO existingPlan = capManager.getPlanById(updateRequest.getId());
		if(existingPlan.getCertifiedProductId() != null) {
			CertifiedProductDTO certifiedProduct = productManager.getById(existingPlan.getCertifiedProductId());
			if(certifiedProduct != null) {
				owningAcbId = certifiedProduct.getCertificationBodyId();
				capManager.update(owningAcbId, toUpdate);
			} else {
				throw new InvalidArgumentsException("Could not find the certified product for this plan.");
			}
		} else {
			throw new InvalidArgumentsException("No certified product id was found for this plan.");
		}
		
		//update data for any certifications that already exist
		List<CorrectiveActionPlanCertificationResultDTO> existingCerts = capManager.getCertificationsForPlan(existingPlan.getId());
		for(int i = 0; i < existingCerts.size(); i++) {
			CorrectiveActionPlanCertificationResultDTO existingCert = existingCerts.get(i);
			for(int j = 0; j < updateRequest.getCertifications().size(); j++) {
				CorrectiveActionPlanCertificationResult updateCert = updateRequest.getCertifications().get(j);
				if(existingCert.getCertCriterion().getNumber().equals(updateCert.getCertificationCriterionNumber())) {
					existingCert.setAcbSummary(updateCert.getAcbSummary());
					existingCert.setCorrectiveActionPlanId(updateRequest.getId());
					existingCert.setDeveloperSummary(updateCert.getDeveloperSummary());
					existingCert.setResolution(updateCert.getResolution());
					capManager.updateCertification(owningAcbId, existingCert);
				}
			}
		}
		
		//remove certifications that aren't there anymore
		List<CorrectiveActionPlanCertificationResultDTO> certsToDelete = new ArrayList<CorrectiveActionPlanCertificationResultDTO>();
		existingCerts = capManager.getCertificationsForPlan(existingPlan.getId());
		for(int i = 0; i < existingCerts.size(); i++) {
			CorrectiveActionPlanCertificationResultDTO existingCert = existingCerts.get(i);
			boolean foundCert = false;
			for(int j = 0; j < updateRequest.getCertifications().size(); j++) {
				CorrectiveActionPlanCertificationResult updateCert = updateRequest.getCertifications().get(j);
				if(existingCert.getCertCriterion().getNumber().equals(updateCert.getCertificationCriterionNumber())) {
					foundCert = true;
				}
			}
			
			if(!foundCert) {
				CorrectiveActionPlanCertificationResultDTO certToDelete = new CorrectiveActionPlanCertificationResultDTO();
				certToDelete.setId(existingCert.getId());
				certToDelete.setCorrectiveActionPlanId(updateRequest.getId());
				certsToDelete.add(certToDelete);
			}
		}
		if(certsToDelete.size() > 0) {
			capManager.removeCertificationsFromPlan(owningAcbId, certsToDelete);
		}
		
		//add certifications that weren't there before
		List<CorrectiveActionPlanCertificationResultDTO> certsToAdd = new ArrayList<CorrectiveActionPlanCertificationResultDTO>();
		existingCerts = capManager.getCertificationsForPlan(existingPlan.getId());
		for(int i = 0; i < updateRequest.getCertifications().size(); i++) {
			CorrectiveActionPlanCertificationResult updateCert = updateRequest.getCertifications().get(i);
			boolean foundCert = false;
			for(int j = 0; j < existingCerts.size(); j++) {
				CorrectiveActionPlanCertificationResultDTO existingCert = existingCerts.get(j);
				if(existingCert.getCertCriterion().getNumber().equals(updateCert.getCertificationCriterionNumber())) {
					foundCert = true;
				}
			}
			
			if(!foundCert) {
				CorrectiveActionPlanCertificationResultDTO certToAdd = new CorrectiveActionPlanCertificationResultDTO();
				certToAdd.setAcbSummary(updateCert.getAcbSummary());
				certToAdd.setCorrectiveActionPlanId(updateRequest.getId());
				certToAdd.setDeveloperSummary(updateCert.getDeveloperSummary());
				certToAdd.setResolution(updateCert.getResolution());
				
				CertificationCriterionDTO criterion = new CertificationCriterionDTO();
				criterion.setNumber(updateCert.getCertificationCriterionNumber());
				certToAdd.setCertCriterion(criterion);
				certsToAdd.add(certToAdd);
			}
		}
		if(certsToAdd.size() > 0) {
			capManager.addCertificationsToPlan(owningAcbId, updateRequest.getId(), certsToAdd);
		}
		//END 
		
		return capManager.getPlanDetails(toUpdate.getId());
	}
	
	@ApiOperation(value="Add documentation to an existing CAP.", 
			notes="Upload a file of any kind (current size limit 5MB) as supporting "
					+ " documentation to an existing CAP. The logged in user uploading the file "
					+ " must have either ROLE_ADMIN or ROLE_ACB_ADMIN and administrative "
					+ " authority on the associated ACB.")
	@RequestMapping(value="/{capId}/documentation", method=RequestMethod.POST,
			produces="application/json; charset=utf-8") 
	public @ResponseBody String upload(@PathVariable("capId") Long correctiveActionPlanId,
			@RequestParam("file") MultipartFile file) throws 
			InvalidArgumentsException, MaxUploadSizeExceededException, Exception {
		if (file.isEmpty()) {
			throw new InvalidArgumentsException("You cannot upload an empty file!");
		}
		
		CorrectiveActionPlanDocumentationDTO toCreate = new CorrectiveActionPlanDocumentationDTO();
		toCreate.setFileType(file.getContentType());
		toCreate.setFileName(file.getOriginalFilename());
		toCreate.setFileData(file.getBytes());	
		toCreate.setCorrectiveActionPlanId(correctiveActionPlanId);
		
		Long owningAcbId = null;
		CorrectiveActionPlanDTO existingPlan = capManager.getPlanById(correctiveActionPlanId);
		if(existingPlan.getCertifiedProductId() != null) {
			CertifiedProductDTO certifiedProduct = productManager.getById(existingPlan.getCertifiedProductId());
			if(certifiedProduct != null) {
				owningAcbId = certifiedProduct.getCertificationBodyId();
				capManager.addDocumentationToPlan(owningAcbId, toCreate);
			} else {
				throw new InvalidArgumentsException("Could not find the certified product for this plan.");
			}
		} else {
			throw new InvalidArgumentsException("No certified product id was found for this plan.");
		}
		
		return "{\"success\": \"true\"}";
	}
	
	@ApiOperation(value="Create a new corrective action plan.", 
			notes="The logged in user"
					+ " must have either ROLE_ADMIN or ROLE_ACB_ADMIN and administrative "
					+ " authority on the associated ACB.")
	@RequestMapping(value="/create", method=RequestMethod.POST,
			produces="application/json; charset=utf-8")
	public @ResponseBody CorrectiveActionPlanDetails create(@RequestBody(required=true) CorrectiveActionPlanDetails createRequest) 
			throws EntityCreationException, EntityRetrievalException, JsonProcessingException,
			InvalidArgumentsException {
		CorrectiveActionPlanDTO toCreate = new CorrectiveActionPlanDTO();
		toCreate.setAcbSummary(createRequest.getAcbSummary());
		toCreate.setActualCompletionDate(createRequest.getActualCompletionDate());
		toCreate.setApprovalDate(createRequest.getApprovalDate());
		toCreate.setCertifiedProductId(createRequest.getCertifiedProductId());
		toCreate.setDeveloperSummary(createRequest.getDeveloperSummary());
		toCreate.setEffectiveDate(createRequest.getEffectiveDate());
		toCreate.setEstimatedCompletionDate(createRequest.getEstimatedCompletionDate());
		toCreate.setNoncomplainceDate(createRequest.getNoncomplianceDate());
		toCreate.setResolution(createRequest.getResolution());
		
		Long createdPlanId = null;
		Long acbId = null;
		
		//get the acb that owns the product to make sure we have permissions to create it		
		CertifiedProductDTO certifiedProduct = productManager.getById(toCreate.getCertifiedProductId());
		if(certifiedProduct != null) {
			acbId = certifiedProduct.getCertificationBodyId();
			CorrectiveActionPlanDetails createdPlan = capManager.create(acbId, toCreate);
			createdPlanId = createdPlan.getId();
		} else {
			throw new InvalidArgumentsException("Could not find the certified product for this plan.");
		}
		
		List<CorrectiveActionPlanCertificationResultDTO> certsToCreate = new ArrayList<CorrectiveActionPlanCertificationResultDTO>();
		if(createRequest.getCertifications() != null && createRequest.getCertifications().size() > 0) {
			for(CorrectiveActionPlanCertificationResult cert : createRequest.getCertifications()) {
				CorrectiveActionPlanCertificationResultDTO currCertToCreate = new CorrectiveActionPlanCertificationResultDTO();
				currCertToCreate.setAcbSummary(cert.getAcbSummary());
				currCertToCreate.setCorrectiveActionPlanId(createdPlanId);
				currCertToCreate.setDeveloperSummary(cert.getDeveloperSummary());
				currCertToCreate.setResolution(cert.getResolution());
				
				CertificationCriterionDTO criterion = new CertificationCriterionDTO();
				criterion.setId(cert.getCertificationCriterionId());
				criterion.setNumber(cert.getCertificationCriterionNumber());
				currCertToCreate.setCertCriterion(criterion);
				certsToCreate.add(currCertToCreate);
			}
		}
		
		CorrectiveActionPlanDetails result = capManager.addCertificationsToPlan(acbId, createdPlanId, certsToCreate);
		return result;
	}
	
	@ApiOperation(value="Delete a corrective action plan.", 
			notes="The logged in user"
					+ " must have either ROLE_ADMIN or ROLE_ACB_ADMIN and administrative "
					+ " authority on the associated ACB.")
	@RequestMapping(value="/{planId}/delete", method= RequestMethod.POST,
			produces="application/json; charset=utf-8")
	public String deleteAcb(@PathVariable("planId") Long planId) 
			throws JsonProcessingException, EntityCreationException, EntityRetrievalException,
				InvalidArgumentsException {
		
		//get the acb that owns the product to make sure we have permissions to update it
		CorrectiveActionPlanDTO existingPlan = capManager.getPlanById(planId);
		if(existingPlan.getCertifiedProductId() != null) {
			CertifiedProductDTO certifiedProduct = productManager.getById(existingPlan.getCertifiedProductId());
			if(certifiedProduct != null) {
				Long acbId = certifiedProduct.getCertificationBodyId();
				capManager.delete(acbId, planId);
			} else {
				throw new InvalidArgumentsException("Could not find the certified product for this plan.");
			}
		} else {
			throw new InvalidArgumentsException("No certified product id was found for this plan.");
		}
		return "{\"deleted\" : true }";
	}
	
	@ApiOperation(value="Remove documentation from a corrective action plan.", 
			notes="The logged in user"
					+ " must have either ROLE_ADMIN or ROLE_ACB_ADMIN and administrative "
					+ " authority on the associated ACB.")
	@RequestMapping(value="/documentation/{docId}/delete", method= RequestMethod.POST,
			produces="application/json; charset=utf-8")
	public String deleteDocumentationById(@PathVariable("docId") Long docId) 
			throws JsonProcessingException, EntityCreationException, EntityRetrievalException,
				InvalidArgumentsException {
		
		CorrectiveActionPlanDocumentationDTO doc = capManager.getDocumentationById(docId);
		
		//get the acb that owns the product to make sure we have permissions to update it
		CorrectiveActionPlanDTO existingPlan = capManager.getPlanById(doc.getCorrectiveActionPlanId());
		if(existingPlan.getCertifiedProductId() != null) {
			CertifiedProductDTO certifiedProduct = productManager.getById(existingPlan.getCertifiedProductId());
			if(certifiedProduct != null) {
				Long acbId = certifiedProduct.getCertificationBodyId();
				capManager.removeDocumentation(acbId, doc);
			} else {
				throw new InvalidArgumentsException("Could not find the certified product for this plan.");
			}
		} else {
			throw new InvalidArgumentsException("No certified product id was found for this plan.");
		}
		return "{\"deleted\" : true }";
	}
}