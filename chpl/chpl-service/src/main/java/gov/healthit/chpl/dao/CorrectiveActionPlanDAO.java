package gov.healthit.chpl.dao;

import java.util.List;

import gov.healthit.chpl.dto.CorrectiveActionPlanDTO;

public interface CorrectiveActionPlanDAO {
	public CorrectiveActionPlanDTO create(CorrectiveActionPlanDTO toCreate) throws EntityCreationException,
	EntityRetrievalException;
	public CorrectiveActionPlanDTO update(CorrectiveActionPlanDTO toUpdate) throws EntityRetrievalException;
	public CorrectiveActionPlanDTO getById(Long id) throws EntityRetrievalException;
	public List<CorrectiveActionPlanDTO> getAllForCertifiedProduct(Long certifiedProductId);
	public void delete(Long id) throws EntityRetrievalException;
}