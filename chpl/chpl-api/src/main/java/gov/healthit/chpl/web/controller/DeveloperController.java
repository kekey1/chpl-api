package gov.healthit.chpl.web.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.healthit.chpl.caching.CacheNames;
import gov.healthit.chpl.domain.Address;
import gov.healthit.chpl.domain.CertifiedProduct;
import gov.healthit.chpl.domain.Contact;
import gov.healthit.chpl.domain.Developer;
import gov.healthit.chpl.domain.DeveloperStatusEvent;
import gov.healthit.chpl.domain.Product;
import gov.healthit.chpl.domain.ProductVersion;
import gov.healthit.chpl.domain.SplitDeveloperRequest;
import gov.healthit.chpl.domain.SplitProductsRequest;
import gov.healthit.chpl.domain.TransparencyAttestationMap;
import gov.healthit.chpl.domain.UpdateDevelopersRequest;
import gov.healthit.chpl.dto.AddressDTO;
import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;
import gov.healthit.chpl.dto.ContactDTO;
import gov.healthit.chpl.dto.DeveloperACBMapDTO;
import gov.healthit.chpl.dto.DeveloperDTO;
import gov.healthit.chpl.dto.DeveloperStatusDTO;
import gov.healthit.chpl.dto.DeveloperStatusEventDTO;
import gov.healthit.chpl.dto.ProductDTO;
import gov.healthit.chpl.dto.ProductOwnerDTO;
import gov.healthit.chpl.dto.ProductVersionDTO;
import gov.healthit.chpl.exception.EntityCreationException;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.exception.InvalidArgumentsException;
import gov.healthit.chpl.exception.MissingReasonException;
import gov.healthit.chpl.exception.ValidationException;
import gov.healthit.chpl.manager.CertifiedProductManager;
import gov.healthit.chpl.manager.DeveloperManager;
import gov.healthit.chpl.manager.ProductManager;
import gov.healthit.chpl.util.ChplProductNumberUtil;
import gov.healthit.chpl.util.ErrorMessageUtil;
import gov.healthit.chpl.validation.developer.DeveloperCreationValidator;
import gov.healthit.chpl.validation.developer.DeveloperUpdateValidator;
import gov.healthit.chpl.web.controller.results.DeveloperResults;
import gov.healthit.chpl.web.controller.results.SplitDeveloperResponse;
import gov.healthit.chpl.web.controller.results.SplitProductResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "developers")
@RestController
@RequestMapping("/developers")
public class DeveloperController {

    @Autowired
    private DeveloperManager developerManager;
    @Autowired
    private CertifiedProductManager cpManager;
    @Autowired
    private DeveloperUpdateValidator updateValidator;
    @Autowired
    private DeveloperCreationValidator creationValidator;
    @Autowired
    private ErrorMessageUtil msgUtil;
    @Autowired
    private ChplProductNumberUtil chplProductNumberUtil;

    @ApiOperation(value = "List all developers in the system.",
            notes = "Security Restrictions: ROLE_ADMIN, ROLE_ONC, and ROLE_ACB can see deleted "
                    + "developers.  Everyone else can only see active developers.")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody DeveloperResults getDevelopers(
            @RequestParam(value = "showDeleted", required = false, defaultValue = "false") final boolean showDeleted) {
        List<DeveloperDTO> developerList = null;
        if (showDeleted) {
            developerList = developerManager.getAllIncludingDeleted();
        } else {
            developerList = developerManager.getAll();
        }

        List<Developer> developers = new ArrayList<Developer>();
        if (developerList != null && developerList.size() > 0) {
            for (DeveloperDTO dto : developerList) {
                Developer result = new Developer(dto);
                developers.add(result);
            }
        }

        DeveloperResults results = new DeveloperResults();
        results.setDevelopers(developers);
        return results;
    }

    @ApiOperation(value = "Get information about a specific developer.", notes = "")
    @RequestMapping(value = "/{developerId}", method = RequestMethod.GET,
    produces = "application/json; charset=utf-8")
    public @ResponseBody Developer getDeveloperById(@PathVariable("developerId") final Long developerId)
            throws EntityRetrievalException {
        DeveloperDTO developer = developerManager.getById(developerId);

        Developer result = null;
        if (developer != null) {
            result = new Developer(developer);
        }
        return result;
    }

    @ApiOperation(value = "Update a developer or merge developers.",
            notes = "This method serves two purposes: to update a single developer's information and to merge two "
                    + "developers into one.   A user of this service should pass in a single developerId to update "
                    + "just that developer.  If multiple developer IDs are passed in, the service performs a merge "
                    + "meaning that a new developer is created with all of the information provided (name, address, "
                    + "etc.) and all of the prodcuts previously assigned to the developerId's specified are "
                    + "reassigned to the newly created developer. The old developers are then deleted. "
                    + "Security Restrictions: ROLE_ADMIN, ROLE_ONC, or ROLE_ACB")
    @RequestMapping(value = "", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = "application/json; charset=utf-8")
    public ResponseEntity<Developer> updateDeveloper(
            @RequestBody(required = true) final UpdateDevelopersRequest developerInfo) throws InvalidArgumentsException,
    EntityCreationException, EntityRetrievalException, JsonProcessingException,
    ValidationException, MissingReasonException {
        return update(developerInfo);
    }

    @ApiOperation(
            value = "Split a developer - some products stay with the existing developer and some products are moved "
                    + "to a new developer.",
                    notes = "Security Restrictions: ROLE_ADMIN, ROLE_ONC, or ROLE_ACB")
    @RequestMapping(value = "/{developerId}/split", method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/json; charset=utf-8")
    public ResponseEntity<SplitDeveloperResponse> splitDeveloper(@PathVariable("developerId") final Long developerId,
            @RequestBody(required = true) final SplitDeveloperRequest splitRequest)
                    throws EntityCreationException, EntityRetrievalException, InvalidArgumentsException,
                    ValidationException, JsonProcessingException {

        //validate required fields are present in the split request
        //new developer product ids cannot be empty
        if (splitRequest.getNewDeveloperProductIds() == null || splitRequest.getNewDeveloperProductIds().size() == 0) {
            String error = msgUtil.getMessage("developer.split.missingNewDeveloperProductIds");
            throw new InvalidArgumentsException(error);
        }
        //old developer product ids cannot be empty
        if (splitRequest.getNewDeveloperProductIds() == null || splitRequest.getNewDeveloperProductIds().size() == 0) {
            String error = msgUtil.getMessage("developer.split.missingOldDeveloperProductIds");
            throw new InvalidArgumentsException(error);
        }
        //new and old developers cannot be empty
        if (splitRequest.getNewDeveloper() == null || splitRequest.getOldDeveloper() == null) {
            String error = msgUtil.getMessage("developer.split.newAndOldDeveloperRequired");
            throw new InvalidArgumentsException(error);
        }
        //make sure the developer id in the split request matches the developer id on the url path
        if (developerId != splitRequest.getOldDeveloper().getDeveloperId()) {
            throw new InvalidArgumentsException(msgUtil.getMessage("developer.split.requestMismatch"));
        }
        //check developer fields
        Set<String> devErrors = creationValidator.validate(splitRequest.getNewDeveloper());
        if (devErrors != null && devErrors.size() > 0) {
            throw new ValidationException(devErrors, null);
        }

        DeveloperDTO oldDeveloper = developerManager.getById(splitRequest.getOldDeveloper().getDeveloperId());
        DeveloperDTO newDeveloper = new DeveloperDTO();
        newDeveloper.setName(splitRequest.getNewDeveloper().getName());
        newDeveloper.setWebsite(splitRequest.getNewDeveloper().getWebsite());
        for (TransparencyAttestationMap attMap : splitRequest.getNewDeveloper().getTransparencyAttestations()) {
            DeveloperACBMapDTO devMap = new DeveloperACBMapDTO();
            devMap.setAcbId(attMap.getAcbId());
            devMap.setAcbName(attMap.getAcbName());
            devMap.setTransparencyAttestation(attMap.getAttestation());
            newDeveloper.getTransparencyAttestationMappings().add(devMap);
        }
        if (splitRequest.getNewDeveloper().getAddress() != null) {
            AddressDTO developerAddress = new AddressDTO();
            developerAddress.setStreetLineOne(splitRequest.getNewDeveloper().getAddress().getLine1());
            developerAddress.setStreetLineTwo(splitRequest.getNewDeveloper().getAddress().getLine2());
            developerAddress.setCity(splitRequest.getNewDeveloper().getAddress().getCity());
            developerAddress.setState(splitRequest.getNewDeveloper().getAddress().getState());
            developerAddress.setZipcode(splitRequest.getNewDeveloper().getAddress().getZipcode());
            developerAddress.setCountry(splitRequest.getNewDeveloper().getAddress().getCountry());
            newDeveloper.setAddress(developerAddress);
        }
        if (splitRequest.getNewDeveloper().getContact() != null) {
            ContactDTO developerContact = new ContactDTO();
            developerContact.setFullName(splitRequest.getNewDeveloper().getContact().getFullName());
            developerContact.setFriendlyName(splitRequest.getNewDeveloper().getContact().getFriendlyName());
            developerContact.setEmail(splitRequest.getNewDeveloper().getContact().getEmail());
            developerContact.setPhoneNumber(splitRequest.getNewDeveloper().getContact().getPhoneNumber());
            developerContact.setTitle(splitRequest.getNewDeveloper().getContact().getTitle());
            newDeveloper.setContact(developerContact);
        }

        DeveloperDTO createdDeveloper = developerManager.split(oldDeveloper, newDeveloper,
                splitRequest.getNewDeveloperProductIds());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Cache-cleared", CacheNames.COLLECTIONS_LISTINGS);
        DeveloperDTO originalDeveloper = developerManager.getById(oldDeveloper.getId());
        SplitDeveloperResponse response = new SplitDeveloperResponse();
        response.setNewDeveloper(new Developer(createdDeveloper));
        response.setOldDeveloper(new Developer(originalDeveloper));

        // find out which CHPL product numbers would have changed (only
        // new-style ones) and add them to the response header
        List<CertifiedProductDetailsDTO> possibleChangedChplIds = cpManager.getByDeveloperId(originalDeveloper.getId());
        if (possibleChangedChplIds != null && possibleChangedChplIds.size() > 0) {
            StringBuffer buf = new StringBuffer();
            for (CertifiedProductDetailsDTO possibleChanged : possibleChangedChplIds) {
                if (!chplProductNumberUtil.isLegacy(possibleChanged.getChplProductNumber())) {
                    if (buf.length() > 0) {
                        buf.append(",");
                    }
                    buf.append(possibleChanged.getChplProductNumber());
                }
            }
            responseHeaders.set("CHPL-Id-Changed", buf.toString());
        }
        return new ResponseEntity<SplitDeveloperResponse>(response, responseHeaders, HttpStatus.OK);
    }

    private synchronized ResponseEntity<Developer> update(final UpdateDevelopersRequest developerInfo)
            throws InvalidArgumentsException, EntityCreationException,
            EntityRetrievalException, JsonProcessingException,
            ValidationException, MissingReasonException {

        DeveloperDTO result = null;
        HttpHeaders responseHeaders = new HttpHeaders();

        // Merge these developers into one.
        // create a new developer with the rest of the passed in
        // information; first validate the new developer
        if (developerInfo.getDeveloperIds().size() > 1) {
            //merging doesn't require developer address which is why the update validator
            //is getting used here.
            Set<String> errors = updateValidator.validate(developerInfo.getDeveloper());
            if (errors != null && errors.size() > 0) {
                throw new ValidationException(errors, null);
            }
            DeveloperDTO toCreate = new DeveloperDTO();
            toCreate.setDeveloperCode(developerInfo.getDeveloper().getDeveloperCode());
            toCreate.setName(developerInfo.getDeveloper().getName());
            toCreate.setWebsite(developerInfo.getDeveloper().getWebsite());
            if (developerInfo.getDeveloper().getStatusEvents() != null
                    && developerInfo.getDeveloper().getStatusEvents().size() > 0) {
                for (DeveloperStatusEvent providedStatusHistory : developerInfo.getDeveloper().getStatusEvents()) {
                    DeveloperStatusDTO status = new DeveloperStatusDTO();
                    status.setStatusName(providedStatusHistory.getStatus().getStatus());
                    DeveloperStatusEventDTO toCreateHistory = new DeveloperStatusEventDTO();
                    toCreateHistory.setStatus(status);
                    toCreateHistory.setStatusDate(providedStatusHistory.getStatusDate());
                    toCreate.getStatusEvents().add(toCreateHistory);
                }
                // if no history is passed in, an Active status gets added in
                // the DAO when the new developer is created
            }

            Address developerAddress = developerInfo.getDeveloper().getAddress();
            if (developerAddress != null) {
                AddressDTO toCreateAddress = new AddressDTO();
                toCreateAddress.setStreetLineOne(developerAddress.getLine1());
                toCreateAddress.setStreetLineTwo(developerAddress.getLine2());
                toCreateAddress.setCity(developerAddress.getCity());
                toCreateAddress.setState(developerAddress.getState());
                toCreateAddress.setZipcode(developerAddress.getZipcode());
                toCreateAddress.setCountry(developerAddress.getCountry());
                toCreate.setAddress(toCreateAddress);
            }
            Contact developerContact = developerInfo.getDeveloper().getContact();
            if (developerContact != null) {
                ContactDTO toCreateContact = new ContactDTO();
                toCreateContact.setEmail(developerContact.getEmail());
                toCreateContact.setFullName(developerContact.getFullName());
                toCreateContact.setFriendlyName(developerContact.getFriendlyName());
                toCreateContact.setPhoneNumber(developerContact.getPhoneNumber());
                toCreateContact.setTitle(developerContact.getTitle());
                toCreate.setContact(toCreateContact);
            }
            result = developerManager.merge(developerInfo.getDeveloperIds(), toCreate);
            responseHeaders.set("Cache-cleared", CacheNames.COLLECTIONS_LISTINGS);
            // re-query because the developer code isn't filled in otherwise
            result = developerManager.getById(result.getId());
        } else if (developerInfo.getDeveloperIds().size() == 1) {
            // update the information for the developer id supplied in the
            // database. first validate the new developer info
            Set<String> errors = updateValidator.validate(developerInfo.getDeveloper());
            if (errors != null && errors.size() > 0) {
                throw new ValidationException(errors, null);
            }

            DeveloperDTO toUpdate = new DeveloperDTO();
            toUpdate.setDeveloperCode(developerInfo.getDeveloper().getDeveloperCode());
            toUpdate.setId(developerInfo.getDeveloperIds().get(0));
            toUpdate.setName(developerInfo.getDeveloper().getName());
            toUpdate.setWebsite(developerInfo.getDeveloper().getWebsite());
            for (TransparencyAttestationMap attMap : developerInfo.getDeveloper().getTransparencyAttestations()) {
                DeveloperACBMapDTO devMap = new DeveloperACBMapDTO();
                devMap.setAcbId(attMap.getAcbId());
                devMap.setAcbName(attMap.getAcbName());
                devMap.setTransparencyAttestation(attMap.getAttestation());
                toUpdate.getTransparencyAttestationMappings().add(devMap);
            }

            if (developerInfo.getDeveloper().getStatusEvents() != null
                    && developerInfo.getDeveloper().getStatusEvents().size() > 0) {
                for (DeveloperStatusEvent providedStatusHistory : developerInfo.getDeveloper().getStatusEvents()) {
                    DeveloperStatusDTO status = new DeveloperStatusDTO();
                    status.setId(providedStatusHistory.getStatus().getId());
                    status.setStatusName(providedStatusHistory.getStatus().getStatus());
                    DeveloperStatusEventDTO toCreateHistory = new DeveloperStatusEventDTO();
                    toCreateHistory.setId(providedStatusHistory.getId());
                    toCreateHistory.setDeveloperId(providedStatusHistory.getDeveloperId());
                    toCreateHistory.setStatus(status);
                    toCreateHistory.setStatusDate(providedStatusHistory.getStatusDate());
                    toCreateHistory.setReason(providedStatusHistory.getReason());
                    toUpdate.getStatusEvents().add(toCreateHistory);
                }
            }

            if (developerInfo.getDeveloper().getAddress() != null) {
                AddressDTO address = new AddressDTO();
                address.setId(developerInfo.getDeveloper().getAddress().getAddressId());
                address.setStreetLineOne(developerInfo.getDeveloper().getAddress().getLine1());
                address.setStreetLineTwo(developerInfo.getDeveloper().getAddress().getLine2());
                address.setCity(developerInfo.getDeveloper().getAddress().getCity());
                address.setState(developerInfo.getDeveloper().getAddress().getState());
                address.setZipcode(developerInfo.getDeveloper().getAddress().getZipcode());
                address.setCountry(developerInfo.getDeveloper().getAddress().getCountry());
                toUpdate.setAddress(address);
            }
            if (developerInfo.getDeveloper().getContact() != null) {
                Contact developerContact = developerInfo.getDeveloper().getContact();
                ContactDTO toUpdateContact = new ContactDTO();
                toUpdateContact.setEmail(developerContact.getEmail());
                toUpdateContact.setFullName(developerContact.getFullName());
                toUpdateContact.setFriendlyName(developerContact.getFriendlyName());
                toUpdateContact.setPhoneNumber(developerContact.getPhoneNumber());
                toUpdateContact.setTitle(developerContact.getTitle());
                toUpdate.setContact(toUpdateContact);
            }

            result = developerManager.update(toUpdate);
            responseHeaders.set("Cache-cleared", CacheNames.COLLECTIONS_LISTINGS);
        }

        if (result == null) {
            throw new EntityCreationException("There was an error inserting or updating the developer information.");
        }
        Developer restResult = new Developer(result);
        return new ResponseEntity<Developer>(restResult, responseHeaders, HttpStatus.OK);
    }
}
