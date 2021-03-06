package gov.healthit.chpl.changerequest.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import gov.healthit.chpl.changerequest.domain.ChangeRequest;
import gov.healthit.chpl.changerequest.domain.ChangeRequestConverter;
import gov.healthit.chpl.changerequest.domain.ChangeRequestStatus;
import gov.healthit.chpl.changerequest.domain.service.ChangeRequestDetailsFactory;
import gov.healthit.chpl.changerequest.entity.ChangeRequestEntity;
import gov.healthit.chpl.changerequest.entity.ChangeRequestStatusEntity;
import gov.healthit.chpl.changerequest.entity.ChangeRequestTypeEntity;
import gov.healthit.chpl.dao.impl.BaseDAOImpl;
import gov.healthit.chpl.entity.developer.DeveloperEntity;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.util.AuthUtil;

@Repository("changeRequestDAO")
public class ChangeRequestDAO extends BaseDAOImpl {

    private DeveloperCertificationBodyMapDAO developerCertificationBodyMapDAO;
    private ChangeRequestStatusDAO changeRequestStatusDAO;
    private ChangeRequestDetailsFactory changeRequestDetailsFactory;

    @Value("${changerequest.status.pendingacbaction}")
    private Long pendingAcbAction;

    @Value("${changerequest.status.pendingdeveloperaction}")
    private Long pendingDeveloperAction;

    @Autowired
    public ChangeRequestDAO(DeveloperCertificationBodyMapDAO developerCertificationBodyMapDAO,
            ChangeRequestStatusDAO changeRequestStatusDAO,
            @Lazy ChangeRequestDetailsFactory changeRequestDetailsFactory) {
        this.developerCertificationBodyMapDAO = developerCertificationBodyMapDAO;
        this.changeRequestStatusDAO = changeRequestStatusDAO;
        this.changeRequestDetailsFactory = changeRequestDetailsFactory;
    }

    public ChangeRequest create(ChangeRequest cr) throws EntityRetrievalException {
        ChangeRequestEntity entity = getNewEntity(cr);
        create(entity);
        return ChangeRequestConverter.convert(getEntityById(entity.getId()));
    }

    public ChangeRequest get(Long changeRequestId) throws EntityRetrievalException {
        ChangeRequest cr = ChangeRequestConverter.convert(getEntityById(changeRequestId));
        return populateDependentObjects(cr);
    }

    public List<ChangeRequest> getAll() throws EntityRetrievalException {
        return getEntities().stream()
                .map(entity -> ChangeRequestConverter.convert(entity))
                .map(cr -> populateDependentObjects(cr))
                .collect(Collectors.<ChangeRequest>toList());
    }

    public List<ChangeRequest> getAllPending() throws EntityRetrievalException {
        return getAll().stream()
                .filter(cr -> getUpdatableStatuses().contains(cr.getCurrentStatus().getChangeRequestStatusType().getId()))
                .collect(Collectors.<ChangeRequest>toList());
    }


    public List<ChangeRequest> getByDeveloper(Long developerId) throws EntityRetrievalException {
        List<Long> developers = new ArrayList<Long>(Arrays.asList(developerId));

        return getEntitiesByDevelopers(developers).stream()
                .map(entity -> ChangeRequestConverter.convert(entity))
                .map(cr -> populateDependentObjects(cr))
                .collect(Collectors.<ChangeRequest>toList());
    }

    private ChangeRequestEntity getEntityById(Long id) throws EntityRetrievalException {
        String hql = "SELECT DISTINCT cr "
                + "FROM ChangeRequestEntity cr "
                + "JOIN FETCH cr.changeRequestType "
                + "JOIN FETCH cr.developer "
                + "WHERE cr.deleted = false "
                + "AND cr.id = :changeRequestId";

        ChangeRequestEntity entity = null;
        List<ChangeRequestEntity> result = entityManager
                .createQuery(hql, ChangeRequestEntity.class)
                .setParameter("changeRequestId", id)
                .getResultList();

        if (result.size() == 0) {
            throw new EntityRetrievalException("Data error. Duplicate change request id in database.");
        } else {
            entity = result.get(0);
        }
        return entity;
    }

    private List<ChangeRequestEntity> getEntitiesByDevelopers(List<Long> developerIds)
            throws EntityRetrievalException {
        String hql = "SELECT DISTINCT cr "
                + "FROM ChangeRequestEntity cr "
                + "JOIN FETCH cr.changeRequestType "
                + "JOIN FETCH cr.developer "
                + "WHERE cr.deleted = false "
                + "AND cr.developer.id IN (:developerIds)";

        List<ChangeRequestEntity> results = entityManager
                .createQuery(hql, ChangeRequestEntity.class)
                .setParameter("developerIds", developerIds)
                .getResultList();

        return results;
    }

    private List<ChangeRequestEntity> getEntities()
            throws EntityRetrievalException {

        String hql = "SELECT DISTINCT cr "
                + "FROM ChangeRequestEntity cr  "
                + "JOIN FETCH cr.changeRequestType crt "
                + "JOIN FETCH cr.developer dev "
                + "JOIN FETCH dev.address "
                + "JOIN FETCH dev.contact "
                + "JOIN FETCH dev.statusEvents statuses "
                + "JOIN FETCH statuses.developerStatus "
                + "JOIN FETCH dev.developerCertificationStatuses "
                + "WHERE cr.deleted = false ";

        List<ChangeRequestEntity> results = entityManager
                .createQuery(hql, ChangeRequestEntity.class)
                .getResultList();

        return results;
    }

    private ChangeRequestStatus getCurrentStatus(Long changeRequestId) {
        String hql = "SELECT crStatus "
                + "FROM ChangeRequestStatusEntity crStatus "
                + "JOIN FETCH crStatus.changeRequestStatusType "
                + "LEFT JOIN FETCH crStatus.certificationBody acb "
                + "LEFT JOIN FETCH acb.address "
                + "JOIN FETCH crStatus.userPermission "
                + "WHERE crStatus.deleted = false "
                + "AND crStatus.changeRequest.id = :changeRequestId "
                + "ORDER BY crStatus.statusChangeDate DESC";

        List<ChangeRequestStatus> statuses = entityManager
                .createQuery(hql, ChangeRequestStatusEntity.class)
                .setParameter("changeRequestId", changeRequestId)
                .getResultList().stream()
                .map(ChangeRequestConverter::convert)
                .collect(Collectors.<ChangeRequestStatus>toList());

        if (statuses.size() > 0) {
            return statuses.get(0);
        } else {
            return null;
        }
    }

    private ChangeRequestEntity getNewEntity(ChangeRequest cr) {
        ChangeRequestEntity entity = new ChangeRequestEntity();
        entity.setChangeRequestType(
                getSession().load(ChangeRequestTypeEntity.class, cr.getChangeRequestType().getId()));
        entity.setDeveloper(getSession().load(DeveloperEntity.class, cr.getDeveloper().getDeveloperId()));
        entity.setDeleted(false);
        entity.setLastModifiedUser(AuthUtil.getAuditId());
        entity.setCreationDate(new Date());
        entity.setLastModifiedDate(new Date());
        return entity;
    }

    private List<Long> getUpdatableStatuses() {
        List<Long> statuses = new ArrayList<Long>();
        statuses.add(pendingAcbAction);
        statuses.add(pendingDeveloperAction);
        return statuses;
    }

    private ChangeRequest populateDependentObjects(ChangeRequest cr) {
        try {
            cr.setCurrentStatus(getCurrentStatus(cr.getId()));
            cr.setStatuses(changeRequestStatusDAO.getByChangeRequestId(cr.getId()));
            cr.setCertificationBodies(developerCertificationBodyMapDAO
                    .getCertificationBodiesForDeveloper(cr.getDeveloper().getDeveloperId()));
            cr.setDetails(
                    changeRequestDetailsFactory.get(cr.getChangeRequestType().getId())
                            .getByChangeRequestId(cr.getId()));
            return cr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
