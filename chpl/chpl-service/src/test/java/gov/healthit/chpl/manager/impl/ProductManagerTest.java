package gov.healthit.chpl.manager.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import gov.healthit.chpl.dao.DeveloperStatusDAO;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.dto.DeveloperDTO;
import gov.healthit.chpl.dto.DeveloperStatusDTO;
import gov.healthit.chpl.dto.DeveloperStatusEventDTO;
import gov.healthit.chpl.dto.ProductDTO;
import gov.healthit.chpl.dto.ProductOwnerDTO;
import gov.healthit.chpl.dto.ProductVersionDTO;
import gov.healthit.chpl.entity.DeveloperStatusType;
import gov.healthit.chpl.manager.CertifiedProductDetailsManager;
import gov.healthit.chpl.manager.DeveloperManager;
import gov.healthit.chpl.manager.ProductManager;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml")
public class ProductManagerTest extends TestCase {
	
	@Autowired private ProductManager productManager;
	@Autowired private DeveloperManager developerManager;
	@Autowired private CertifiedProductDetailsManager cpdManager;
	@Autowired private DeveloperStatusDAO devStatusDao;
	@Rule
    @Autowired
    public UnitTestRules cacheInvalidationRule;
	
	private static JWTAuthenticatedUser adminUser;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		adminUser = new JWTAuthenticatedUser();
		adminUser.setFirstName("Administrator");
		adminUser.setId(-2L);
		adminUser.setLastName("Administrator");
		adminUser.setSubjectName("admin");
		adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
	}
	
	@Test
	@Transactional(readOnly = true)
	public void getAllProducts() {
		List<ProductDTO> results = productManager.getAll();
		assertNotNull(results);
		assertEquals(7, results.size());
	}
	
	@Test
	@Transactional(readOnly = true)
	public void getProductById() {
		Long productId = -1L;
		ProductDTO product = null;
		try {
			product = productManager.getById(productId);
		} catch(EntityRetrievalException ex) {
			fail("Could not find product with id " + productId);
		}
		assertNotNull(product);
		assertEquals(-1, product.getId().longValue());
		assertNotNull(product.getOwnerHistory());
		assertEquals(1, product.getOwnerHistory().size());
		List<ProductOwnerDTO> previousOwners = product.getOwnerHistory();
		for(ProductOwnerDTO previousOwner : previousOwners) {
			assertNotNull(previousOwner.getDeveloper());
			assertEquals(-2, previousOwner.getDeveloper().getId().longValue());
			assertEquals("Test Developer 2", previousOwner.getDeveloper().getName());
		}
	}
	
	@Test
	@Transactional(readOnly = true)
	public void getProductByDeveloper() {
		Long developerId = -1L;
		List<ProductDTO> products = null;
		products = productManager.getByDeveloper(developerId);
		assertNotNull(products);
		assertEquals(3, products.size());
	}
	
	@Test
	@Transactional(readOnly = true)
	public void getProductByDevelopers() {
		List<Long> developerIds = new ArrayList<Long>();
		developerIds.add(-1L);
		developerIds.add(-2L);
		List<ProductDTO> products = null;
		products = productManager.getByDevelopers(developerIds);
		assertNotNull(products);
		assertEquals(4, products.size());
	}
	
	@Test
	@Transactional
	@Rollback(true)
	public void updateProductOwnerHistory() throws EntityRetrievalException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);

		ProductDTO product = productManager.getById(-1L);
		assertNotNull(product.getOwnerHistory());
		assertTrue(product.getOwnerHistory().size() == 1);
		
		product.setOwnerHistory(null);
		try {
			productManager.update(product, false);
		} catch(Exception ex) {
			fail("could not update product!");
			System.out.println(ex.getStackTrace());
		}
		
		try {
			ProductDTO updatedProduct = productManager.getById(product.getId());
			assertNotNull(updatedProduct);
			assertTrue(updatedProduct.getOwnerHistory() == null || updatedProduct.getOwnerHistory().size() == 0);
		} catch(Exception ex) {
			fail("could not find product!");
			System.out.println(ex.getStackTrace());
		}
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
	//i had this method in here to test for updates being allowed
	//when then developer is Active, but it fails because it triggers
	//a suspicious activity event and tries to send email. 
	//we're missing the email properties but i don't think we want to 
	//have one sent anyway.. so excluding that test.
	@Test
	@Transactional
	@Rollback
	@Ignore
	public void testAllowedToUpdateProductWithActiveDeveloper() 
			throws EntityRetrievalException, JsonProcessingException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		ProductDTO product = productManager.getById(-1L);
		assertNotNull(product);
		product.setName("new product name");
		boolean failed = false;
		try {
			product = productManager.update(product, false);
		} catch(EntityCreationException ex) {
			System.out.println(ex.getMessage());
			failed = true;
		}
		assertFalse(failed);
		assertEquals("new product name", product.getName());
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
	@Test
	@Transactional
	@Rollback
	public void testNotAllowedToUpdateProductWithInactiveDeveloper() 
			throws EntityRetrievalException, JsonProcessingException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		
		//change dev to suspended
		DeveloperDTO developer = developerManager.getById(-1L);
		assertNotNull(developer);
		DeveloperStatusDTO newStatus = devStatusDao.getById(2L);
		DeveloperStatusEventDTO newStatusHistory = new DeveloperStatusEventDTO();
		newStatusHistory.setDeveloperId(developer.getId());
		newStatusHistory.setStatus(newStatus);
		newStatusHistory.setStatusDate(new Date());
		developer.getStatusEvents().add(newStatusHistory);
		
		boolean failed = false;
		try {
			developer = developerManager.update(developer);
		} catch(EntityCreationException ex) {
			System.out.println(ex.getMessage());
			failed = true;
		}
		assertFalse(failed);
		DeveloperStatusEventDTO status = developer.getStatus();
		assertNotNull(status);
		assertNotNull(status.getId());
		assertNotNull(status.getStatus());
		assertNotNull(status.getStatus().getStatusName());
		assertEquals(DeveloperStatusType.SuspendedByOnc.toString(), status.getStatus().getStatusName());
		
		//try to update product
		ProductDTO product = productManager.getById(-1L);
		assertNotNull(product);
		product.setName("new product name");
		failed = false;
		try {
			product = productManager.update(product, false);
		} catch(EntityCreationException ex) {
			System.out.println(ex.getMessage());
			failed = true;
		}
		assertTrue(failed);
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
	@Test
	@Transactional
	@Rollback
	public void testProductSplitFailsWithoutAuthentication() throws EntityRetrievalException {
		ProductDTO origProduct = productManager.getById(-2L);
		ProductDTO newProduct = new ProductDTO();
		newProduct.setName("Split Product");
		newProduct.setDeveloperId(origProduct.getDeveloperId());
		List<ProductVersionDTO> newProductVersions = new ArrayList<ProductVersionDTO>();
		ProductVersionDTO newProductVersion = new ProductVersionDTO();
		newProductVersion.setId(5L);
		newProductVersions.add(newProductVersion);
		boolean failedAuth = false;
		try {
			productManager.split(origProduct, newProduct, "SPLIT", newProductVersions);
		} catch(AuthenticationCredentialsNotFoundException ex) {
			failedAuth = true;
		} catch(Exception ex) {
			fail(ex.getMessage());
		}
		
		assertTrue(failedAuth);
	}
	
	@Test
	@Transactional
	@Rollback
	public void testProductSplit() throws EntityRetrievalException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);

		String name = "Split Product";
		String code = "SPLIT";
		
		ProductDTO origProduct = productManager.getById(-2L);
		assertNotNull(origProduct.getProductVersions());
		assertEquals(3, origProduct.getProductVersions().size());
		CertifiedProductSearchDetails cpDetails = cpdManager.getCertifiedProductDetails(7L);
		assertFalse(cpDetails.getChplProductNumber().contains(code));
		
		ProductDTO newProduct = new ProductDTO();
		newProduct.setName(name);
		newProduct.setDeveloperId(origProduct.getDeveloperId());
		List<ProductVersionDTO> newProductVersions = new ArrayList<ProductVersionDTO>();
		ProductVersionDTO newProductVersion = new ProductVersionDTO();
		newProductVersion.setId(7L);
		newProductVersions.add(newProductVersion);
		ProductDTO updatedNewProduct = null;
		try {
			updatedNewProduct = productManager.split(origProduct, newProduct, code, newProductVersions);
		} catch(Exception ex) {
			fail(ex.getMessage());
			ex.printStackTrace();
		}
		
		ProductDTO updatedOrigProduct = productManager.getById(origProduct.getId());
		assertNotNull(updatedOrigProduct.getProductVersions());
		assertEquals(2, updatedOrigProduct.getProductVersions().size());
		
		assertNotNull(updatedNewProduct);
		assertEquals(name, updatedNewProduct.getName());
		assertNotNull(updatedNewProduct.getProductVersions());
		assertEquals(1, updatedNewProduct.getProductVersions().size());
		SecurityContextHolder.getContext().setAuthentication(null);
		cpDetails = cpdManager.getCertifiedProductDetails(7L);
		assertTrue(cpDetails.getChplProductNumber().contains(code));
	}
}
