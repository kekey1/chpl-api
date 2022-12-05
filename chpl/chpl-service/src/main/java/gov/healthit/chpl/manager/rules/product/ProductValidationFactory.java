package gov.healthit.chpl.manager.rules.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.manager.rules.ValidationRule;
import gov.healthit.chpl.permissions.ResourcePermissions;

@Component
public class ProductValidationFactory {
    public static final String NAME = "NAME";
    public static final String OWNER = "OWNER";
    public static final String OWNER_HISTORY = "OWNER_HISTORY";

    private ResourcePermissions resourcePermissions;

    @Autowired
    public ProductValidationFactory(ResourcePermissions resourcePermissions) {
        this.resourcePermissions = resourcePermissions;
    }

    public ValidationRule<ProductValidationContext> getRule(String name) {
        switch (name) {
        case NAME:
            return new ProductNameValidation();
        case OWNER:
            return new ProductOwnerValidation(resourcePermissions);
        case OWNER_HISTORY:
            return new ProductOwnerHistoryValidation();
        default:
            return null;
        }
    }
}