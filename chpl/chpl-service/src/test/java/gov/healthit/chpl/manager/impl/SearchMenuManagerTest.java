package gov.healthit.chpl.manager.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.caching.UnitTestRules;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.domain.CertificationCriterion;
import gov.healthit.chpl.domain.DescriptiveModel;
import gov.healthit.chpl.domain.KeyValueModel;
import gov.healthit.chpl.domain.KeyValueModelStatuses;
import gov.healthit.chpl.domain.Statuses;
import gov.healthit.chpl.domain.TestFunctionality;
import gov.healthit.chpl.domain.notification.NotificationType;
import gov.healthit.chpl.manager.SearchMenuManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml")
public class SearchMenuManagerTest {
@Autowired private SearchMenuManager searchMenuManager;
	
	private static JWTAuthenticatedUser adminUser;
	private static JWTAuthenticatedUser testUser3;
	@Rule
    @Autowired
    public UnitTestRules cacheInvalidationRule;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		adminUser = new JWTAuthenticatedUser();
		adminUser.setFirstName("Administrator");
		adminUser.setId(-2L);
		adminUser.setLastName("Administrator");
		adminUser.setSubjectName("admin");
		adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
		
		testUser3 = new JWTAuthenticatedUser();
		testUser3.setFirstName("Test");
		testUser3.setId(3L);
		testUser3.setLastName("User3");
		testUser3.setSubjectName("testUser3");
		testUser3.getPermissions().add(new GrantedPermission("ROLE_ACB_ADMIN"));
	}
	
	/** 
	 * Tests that the getCertBodyNames() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getCertBodyNames_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Boolean required = false;
		Set<KeyValueModel> results = searchMenuManager.getCertBodyNames(true);
		// certBodyNames should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " certBodyNames but should return more than 0", results.size() > 0);
		
		System.out.println("getCertBodyNames returned " + results.size() + " total certBodyNames.");
		System.out.println("getCertBodyNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getCertBodyNames(true);
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getCertBodyNames returned " + results.size() + " total certBodyNames.");
		System.out.println("getCertBodyNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getCertBodyNames should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getEditionNames() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getEditionNames_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<KeyValueModel> results = searchMenuManager.getEditionNames(true);
		// getEditionNames should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " EditionNames but should return more than 0", results.size() > 0);
		
		System.out.println("getEditionNames returned " + results.size() + " total editionNames.");
		System.out.println("getEditionNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getEditionNames(true);
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getEditionNames returned " + results.size() + " total editionNames.");
		System.out.println("getEditionNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getEditionNames should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getCertificationStatuses() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getCertificationStatuses_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<KeyValueModel> results = searchMenuManager.getCertificationStatuses();
		// getCertificationStatuses should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " CertificationStatuses but should return more than 0", results.size() > 0);
		
		System.out.println("getCertificationStatuses returned " + results.size() + " total certificationStatuses.");
		System.out.println("getCertificationStatuses completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getCertificationStatuses();
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getCertificationStatuses returned " + results.size() + " total certificationStatuses.");
		System.out.println("getCertificationStatuses completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getEditionNames should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getPracticeTypeNames() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getPracticeTypeNames_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<KeyValueModel> results = searchMenuManager.getPracticeTypeNames();
		// getPracticeTypeNames should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " practiceTypeNames but should return more than 0", results.size() > 0);
		
		System.out.println("getPracticeTypeNames returned " + results.size() + " total practiceTypeNames.");
		System.out.println("getPracticeTypeNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getPracticeTypeNames();
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getPracticeTypeNames returned " + results.size() + " total practiceTypeNames.");
		System.out.println("getPracticeTypeNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getPracticeTypeNames should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getClassificationNames() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getClassificationNames_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<KeyValueModel> results = searchMenuManager.getClassificationNames();
		// getClassificationNames should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " getClassificationNames but should return more than 0", results.size() > 0);
		
		System.out.println("getClassificationNames returned " + results.size() + " total classificationNames.");
		System.out.println("getClassificationNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getClassificationNames();
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getClassificationNames returned " + results.size() + " total classificationNames.");
		System.out.println("getClassificationNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getClassificationNames should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getProductNames() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getProductNames_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<KeyValueModelStatuses> results = searchMenuManager.getProductNames();
		// getProductNames should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " getProductNames but should return more than 0", results.size() > 0);
		
		System.out.println("getProductNames returned " + results.size() + " total productNames.");
		System.out.println("getProductNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getProductNames();
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getProductNames returned " + results.size() + " total productNames.");
		System.out.println("getProductNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getProductNames should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getDeveloperNames() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getDeveloperNames_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<KeyValueModelStatuses> results = searchMenuManager.getDeveloperNames();
		// getDeveloperNames should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " getDeveloperNames but should return more than 0", results.size() > 0);
		
		System.out.println("getDeveloperNames returned " + results.size() + " total developerNames.");
		System.out.println("getDeveloperNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getDeveloperNames();
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getDeveloperNames returned " + results.size() + " total developerNames.");
		System.out.println("getDeveloperNames completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getDeveloperNames should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getCQMCriterionNumbers() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getCQMCriterionNumbers_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<DescriptiveModel> results = searchMenuManager.getCQMCriterionNumbers(false);
		// getCQMCriterionNumbers should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " getCQMCriterionNumbers but should return more than 0", results.size() > 0);
		
		System.out.println("getCQMCriterionNumbers returned " + results.size() + " total cQMCriterionNumbers.");
		System.out.println("getCQMCriterionNumbers completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getCQMCriterionNumbers(false);
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getCQMCriterionNumbers returned " + results.size() + " total cQMCriterionNumbers.");
		System.out.println("getCQMCriterionNumbers completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getCQMCriterionNumbers should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	/** 
	 * Tests that the getCertificationCriterionNumbers() caches its data
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getCertificationCriterionNumbers_CachesData() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<DescriptiveModel> results = searchMenuManager.getCertificationCriterionNumbers(false);
		// getCertificationCriterionNumbers should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " getCertificationCriterionNumbers but should return more than 0", results.size() > 0);
		
		System.out.println("getCertificationCriterionNumbers returned " + results.size() + " total certificationCriterionNumbers.");
		System.out.println("getCertificationCriterionNumbers completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		results = searchMenuManager.getCertificationCriterionNumbers(false);
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("getCertificationCriterionNumbers returned " + results.size() + " total certificationCriterionNumbers.");
		System.out.println("getCertificationCriterionNumbers completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("getCertificationCriterionNumbers should complete within 100 ms but took " + timeLength
				+ " millis or " + elapsedSecs + " seconds", timeLength < 100);
	}
	
	@Transactional(readOnly = true)
	@Test
	public void testGetAllCertificationCriterionWithEditions() {
		Set<CertificationCriterion> results = searchMenuManager.getCertificationCriterion();
		assertNotNull(results);
		assertEquals(164, results.size());
		for(CertificationCriterion crit : results) {
			assertNotNull(crit.getNumber());
			assertNotNull(crit.getCertificationEdition());
			if(crit.getNumber().startsWith("170.314")) {
				assertEquals("2014", crit.getCertificationEdition());
			} else if(crit.getNumber().startsWith("170.315")) {
				assertEquals("2015", crit.getCertificationEdition());
			} else {
				assertEquals("2011", crit.getCertificationEdition());
			}
		}
		
	}
	
	/** Description: Tests the getDeveloperNames() method
	 * Verifies that getDeveloperNames() returns valid, non-null results
	 * Expected Result: At least one non-null developer name is returned
	 * Assumptions:
	 * Pre-existing data in openchpl_test DB is there per the \CHPL\chpl-api\chpl\chpl-service\src\test\resources\data\testData.xml
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getDeveloperNames_CompletesWithoutError() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		long getDeveloperNamesStartTime = System.currentTimeMillis();
		Set<KeyValueModelStatuses> results = searchMenuManager.getDeveloperNames();
		long getDeveloperNamesEndTime = System.currentTimeMillis();
		long getDeveloperNamesTimeLength = getDeveloperNamesEndTime - getDeveloperNamesStartTime;
		double getDeveloperNamesElapsedSeconds = getDeveloperNamesTimeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " developers but should return more than 0", results.size() > 0);
		
		System.out.println("getDeveloperNames returned " + results.size() + " developers.");
		System.out.println("searchMenuManager.getDeveloperNames() completed in " + getDeveloperNamesTimeLength
				+ " millis or " + getDeveloperNamesElapsedSeconds + " seconds");
//		assertTrue("searchMenuManager.getDeveloperNames() should complete within 1 second but took " + getDeveloperNamesTimeLength
//				+ " millis or " + getDeveloperNamesElapsedSeconds + " seconds", getDeveloperNamesElapsedSeconds < 1);
	}
	
	/** Description: Tests the getProductNames() method
	 * Verifies that valid productNames are returned
	 * Expected Result: One or more non-null productNames
	 * Assumptions:
	 * Pre-existing data in openchpl_test DB is there per the \CHPL\chpl-api\chpl\chpl-service\src\test\resources\data\testData.xml
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getProductNames_CompletesWithoutError() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		long getProductNamesStartTime = System.currentTimeMillis();
		Set<KeyValueModelStatuses> results = searchMenuManager.getProductNames();
		long getProductNamesEndTime = System.currentTimeMillis();
		long getProductNamesTimeLength = getProductNamesEndTime - getProductNamesStartTime;
		double getProductNamesElapsedSeconds = getProductNamesTimeLength / 1000.0;
		
		assertTrue("Returned " + results.size() + " products but should return more than 0", results.size() > 0);
		
		System.out.println("getProductNames returned " + results.size() + " products.");
		System.out.println("searchMenuManager.getProductNames() completed in " + getProductNamesTimeLength
				+ " millis or " + getProductNamesElapsedSeconds + " seconds");
//		assertTrue("searchMenuManager.getProductNames() should complete within 1 second but took " + getProductNamesTimeLength
//				+ " millis or " + getProductNamesElapsedSeconds + " seconds", getProductNamesElapsedSeconds < 1);
	}
	
	/** Description: Tests the getDeveloperNames() method
	 * Verifies that each developer has a statuses object
	 * Verifies that each property of the statuses object is non-null
	 * Expected Result: Each developer has a statuses object with a non-null value
	 * Each statuses object has a valid integer value for active, retired, withdrawn by developer, 
	 * suspended by acb, and withdrawn by acb
	 * Assumptions:
	 * Pre-existing data in openchpl_test DB is there per the \CHPL\chpl-api\chpl\chpl-service\src\test\resources\data\testData.xml
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getDeveloperNames_ReturnsValidStatusesObject() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		Set<KeyValueModelStatuses> results = searchMenuManager.getDeveloperNames();
		for(KeyValueModelStatuses result : results){
			Statuses status = result.getStatuses();
			
			assertTrue("Statuses.active should not be null", status.getActive() != null);
			assertTrue("Statuses.active should be >= 0", status.getActive() >= 0);
			
			assertTrue("Statuses.retired should not be null", status.getRetired() != null);
			assertTrue("Statuses.retired should be >= 0", status.getRetired() >= 0);
			
			assertTrue("Statuses.withdrawnByDeveloper should not be null", status.getWithdrawnByDeveloper() != null);
			assertTrue("Statuses.withdrawnByDeveloper should be >= 0", status.getWithdrawnByDeveloper() >= 0);
			
			assertTrue("Statuses.suspendedByAcb should not be null", status.getSuspendedByAcb() != null);
			assertTrue("Statuses.suspendedByAcb should be >= 0", status.getSuspendedByAcb() >= 0);
			
			assertTrue("Statuses.withdrawnByAcb should not be null", status.getWithdrawnByAcb() != null);
			assertTrue("Statuses.withdrawnByAcb should be >= 0", status.getWithdrawnByAcb() >= 0);
		}
	}
	
	/** Description: Tests the getProductNames() method
	 * Verifies that each product has a statuses object
	 * Verifies that each property of the statuses object is non-null
	 * Expected Result: Each product has a statuses object with a non-null value
	 * Each statuses object has a valid integer value for active, retired, withdrawn by developer, 
	 * suspended by acb, and withdrawn by acb
	 * Assumptions:
	 * Pre-existing data in openchpl_test DB is there per the \CHPL\chpl-api\chpl\chpl-service\src\test\resources\data\testData.xml
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_getProductNames_ReturnsValidStatusesObject() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		Set<KeyValueModelStatuses> results = searchMenuManager.getProductNames();
		for(KeyValueModelStatuses result : results){
			Statuses status = result.getStatuses();
			
			assertTrue("Statuses.active should not be null", status.getActive() != null);
			assertTrue("Statuses.active should be >= 0", status.getActive() >= 0);
			
			assertTrue("Statuses.retired should not be null", status.getRetired() != null);
			assertTrue("Statuses.retired should be >= 0", status.getRetired() >= 0);
			
			assertTrue("Statuses.withdrawnByDeveloper should not be null", status.getWithdrawnByDeveloper() != null);
			assertTrue("Statuses.withdrawnByDeveloper should be >= 0", status.getWithdrawnByDeveloper() >= 0);
			
			assertTrue("Statuses.suspendedByAcb should not be null", status.getSuspendedByAcb() != null);
			assertTrue("Statuses.suspendedByAcb should be >= 0", status.getSuspendedByAcb() >= 0);
			
			assertTrue("Statuses.withdrawnByAcb should not be null", status.getWithdrawnByAcb() != null);
			assertTrue("Statuses.withdrawnByAcb should be >= 0", status.getWithdrawnByAcb() >= 0);
		}
	}
	
	/** 
	 * Tests that getEditionNames() caches its data
	 */
	@Transactional
	@Test
	public void test_getEditionNames_CacheRefreshesWithParameter() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
		long startTime = System.currentTimeMillis();
		Set<KeyValueModel> firstResult = searchMenuManager.getEditionNames(true);
		// search should now be cached
		long endTime = System.currentTimeMillis();
		long timeLength = endTime - startTime;
		double elapsedSecs = timeLength / 1000.0;
		
		assertTrue("Returned " + firstResult + " which should have a count more than 0", firstResult.size() > 0);
		
		System.out.println("search completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		// now compare cached time vs non-cached time
		startTime = System.currentTimeMillis();
		Set<KeyValueModel> secondResult = searchMenuManager.getEditionNames(false);
		endTime = System.currentTimeMillis();
		timeLength = endTime - startTime;
		elapsedSecs = timeLength / 1000.0;
		System.out.println("search completed in  " + timeLength
				+ " millis or " + elapsedSecs + " seconds");
		
		assertTrue("firstResult should not match secondResult", firstResult.size() != secondResult.size());
	}
	
	@Transactional
	@Test
	public void testGetNotificationTypesForAdminUser() {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		Set<NotificationType> results = searchMenuManager.getNotificationTypes();
		assertNotNull(results);
		assertEquals(4, results.size());
		for(NotificationType nt : results) {
			assertNotNull(nt.getId());
			assertNotNull(nt.getName());
			assertNotNull(nt.getDescription());
			assertNotNull(nt.getRequiresAcb());
		}
	}
	
	@Transactional
	@Test
	public void testGetNotificationTypesForAcbUser() {
		SecurityContextHolder.getContext().setAuthentication(testUser3);
		Set<NotificationType> results = searchMenuManager.getNotificationTypes();
		assertNotNull(results);
		assertEquals(2, results.size());
		for(NotificationType nt : results) {
			assertNotNull(nt.getId());
			assertNotNull(nt.getName());
			assertNotNull(nt.getDescription());
			assertNotNull(nt.getRequiresAcb());
			assertTrue(nt.getRequiresAcb().booleanValue());
		}
	}
	
	@Transactional
	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testGetNotificationTypesAllowedForUnauthenticatedUser() {
		SecurityContextHolder.getContext().setAuthentication(null);
		searchMenuManager.getNotificationTypes();
	}
	
	@Transactional
	@Rollback(true)
	@Test
	public void getTestFunctionality_hasEditions() {
		Set<TestFunctionality> tfs = searchMenuManager.getTestFunctionality();
		assertNotNull(tfs);
		assertTrue(tfs.size() > 0);
		for(TestFunctionality tf : tfs) {
			assertNotNull(tf.getYear());
			assertTrue(tf.getYear().equals("2014") || tf.getYear().equals("2015"));
		}
	}
}
