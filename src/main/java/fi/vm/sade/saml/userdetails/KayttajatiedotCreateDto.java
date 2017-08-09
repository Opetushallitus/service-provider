package fi.vm.sade.saml.userdetails;

public class KayttajatiedotCreateDto {

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "KayttajatiedotCreateDto{" + "username=" + username + '}';
    }

}
