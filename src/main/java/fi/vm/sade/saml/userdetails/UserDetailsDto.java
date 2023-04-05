package fi.vm.sade.saml.userdetails;

public class UserDetailsDto {
    private final String identifier;
    private final String authenticationMethod;

    public UserDetailsDto(String authenticationMethod, String identifier) {
        this.authenticationMethod = authenticationMethod;
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }
}
