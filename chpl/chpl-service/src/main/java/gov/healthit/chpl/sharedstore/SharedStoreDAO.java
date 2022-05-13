package gov.healthit.chpl.sharedstore;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import gov.healthit.chpl.dao.impl.BaseDAOImpl;

@Component
public class SharedStoreDAO extends BaseDAOImpl {

    public void add(SharedStore data) {
        SharedStoreEntity entity = SharedStoreEntity.builder()
                .primaryKey(SharedStorePrimaryKey.builder()
                        .domain(data.getDomain())
                        .key(data.getKey())
                        .build())
                .value(data.getValue())
                .putDate(LocalDateTime.now())
                .build();

        create(entity);
    }

    public SharedStore get(String type, String key) {
        SharedStoreEntity entity = getEntity(type, key);
        if (entity != null) {
            return entity.toDomain();
        } else {
            return null;
        }
    }

    public void remove(String type, String key) {
        SharedStoreEntity entity = getEntity(type, key);
        if (entity != null) {
            getEntityManager().remove(entity);
        }
    }

    private SharedStoreEntity getEntity(String domain, String key) {
        List<SharedStoreEntity> result = entityManager.createQuery(
                "FROM SharedStoreEntity sse "
                + "WHERE sse.primaryKey.domain = :domain "
                + "AND sse.primaryKey.key = :key ", SharedStoreEntity.class)
                .setParameter("domain", domain)
                .setParameter("key", key)
                .getResultList();

        if (result.size() == 0) {
            return null;
        }
        return result.get(0);
    }
}
