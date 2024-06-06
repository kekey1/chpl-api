package gov.healthit.chpl.manager.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.domain.auth.LoginCredentials;
import gov.healthit.chpl.user.cognito.CognitoApiWrapper;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.cognitoidentityprovider.model.PasswordResetRequiredException;

@Log4j2
@Component
public class CognitoAuthenticationManager {

    private CognitoApiWrapper cognitoApiWrapper;

    @Autowired
    public CognitoAuthenticationManager(CognitoApiWrapper cognitoApiWrapper) {
        this.cognitoApiWrapper = cognitoApiWrapper;
    }

    public String authenticate(LoginCredentials credentials) throws PasswordResetRequiredException {
        return cognitoApiWrapper.authenticate(credentials);
    }
}
