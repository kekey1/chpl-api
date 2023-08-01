package gov.healthit.chpl.dao;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import gov.healthit.chpl.dao.impl.BaseDAOImpl;
import gov.healthit.chpl.domain.CertificationCriterion;
import gov.healthit.chpl.entity.CertificationCriterionEntity;
import gov.healthit.chpl.exception.EntityCreationException;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.util.AuthUtil;

@Repository("certificationCriterionDAO")
public class CertificationCriterionDAO extends BaseDAOImpl {

    @Transactional
    public CertificationCriterion update(CertificationCriterion criterion)
            throws EntityRetrievalException, EntityCreationException {
        CertificationCriterionEntity entity = this.getEntityById(criterion.getId());
        entity.setCertificationEditionId(criterion.getCertificationEditionId());
        entity.setDescription(criterion.getDescription());
        entity.setId(criterion.getId());
        entity.setLastModifiedUser(AuthUtil.getAuditId());
        entity.setNumber(criterion.getNumber());
        entity.setTitle(criterion.getTitle());
        entity.setRemoved(criterion.getRemoved());
        update(entity);

        return entity.toDomain();
    }

    public List<CertificationCriterion> findAll() {
        List<CertificationCriterionEntity> entities = getAllEntities();
        return entities.stream()
                .map(entity -> entity.toDomain())
                .collect(Collectors.toList());
    }

    public List<CertificationCriterion> findByCertificationEditionYear(String year) {
        List<CertificationCriterionEntity> entities = getEntitiesByCertificationEditionYear(year);
        return entities.stream()
                .map(entity -> entity.toDomain())
                .collect(Collectors.toList());
    }

    public List<CertificationCriterion> getAllByNumber(String criterionName) {
        List<CertificationCriterionEntity> entities = getEntitiesByNumber(criterionName);
        return entities.stream()
                .map(entity -> entity.toDomain())
                .collect(Collectors.toList());
    }

    public CertificationCriterion getById(Long criterionId) throws EntityRetrievalException {
        CertificationCriterionEntity entity = getEntityById(criterionId);
        return entity.toDomain();
    }

    @Transactional
    public CertificationCriterion getByNumberAndTitle(String criterionNumber, String criterionTitle) {
        CertificationCriterionEntity entity = getEntityByNumberAndTitle(criterionNumber, criterionTitle);
        return entity.toDomain();
    }

    private List<CertificationCriterionEntity> getAllEntities() {
        Query query = entityManager
                .createQuery(
                        "SELECT cce "
                                + "FROM CertificationCriterionEntity cce "
                                + "JOIN FETCH cce.certificationEdition "
                                + "WHERE cce.deleted = false",
                                CertificationCriterionEntity.class);
        @SuppressWarnings("unchecked") List<CertificationCriterionEntity> result = query.getResultList();

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<CertificationCriterionEntity> getEntitiesByCertificationEditionYear(String year) {
        Query query = entityManager.createQuery("SELECT cce "
                + "FROM CertificationCriterionEntity cce "
                + "JOIN FETCH cce.certificationEdition edition "
                + "WHERE (NOT cce.deleted = true) "
                + "AND (edition.year = :year)", CertificationCriterionEntity.class);
        query.setParameter("year", year);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<CertificationCriterionEntity> getEntitiesByNumber(String number) {
        Query query = entityManager.createQuery("SELECT cce "
                + "FROM CertificationCriterionEntity cce "
                + "JOIN FETCH cce.certificationEdition "
                + "WHERE (NOT cce.deleted = true) "
                + "AND (cce.number = :number)", CertificationCriterionEntity.class);
        query.setParameter("number", number);
        return query.getResultList();
    }

    public CertificationCriterionEntity getEntityById(Long id) throws EntityRetrievalException {
        CertificationCriterionEntity entity = null;
        if (id != null) {
            Query query = entityManager.createQuery(
                    "SELECT cce "
                            + "FROM CertificationCriterionEntity cce "
                            + "JOIN FETCH cce.certificationEdition "
                            + "WHERE (cce.deleted <> true) AND (cce.id = :entityid) ",
                            CertificationCriterionEntity.class);
            query.setParameter("entityid", id);
            @SuppressWarnings("unchecked") List<CertificationCriterionEntity> result = query.getResultList();

            if (result.size() > 1) {
                throw new EntityRetrievalException("Data error. Duplicate criterion id in database.");
            }

            if (result.size() > 0) {
                entity = result.get(0);
            }
        }

        return entity;
    }

    public CertificationCriterionEntity getEntityByNumberAndTitle(String criterionNumber, String criterionTitle) {
        Query query = entityManager
                .createQuery(
                        "SELECT cce " + "FROM CertificationCriterionEntity cce "
                                + "JOIN FETCH cce.certificationEdition "
                                + "WHERE (NOT cce.deleted = true) "
                                + "AND (cce.number = :number) "
                                + "AND (cce.title = :title) ",
                                CertificationCriterionEntity.class);
        query.setParameter("number", criterionNumber);
        query.setParameter("title", criterionTitle);
        @SuppressWarnings("unchecked") List<CertificationCriterionEntity> results = query.getResultList();

        CertificationCriterionEntity entity = null;
        if (results.size() > 0) {
            entity = results.get(0);
        }
        return entity;
    }
}
