package fi.vm.sade.saml.userdetails;

public class UserDetailsDto {
    private HenkiloCreateDto henkiloCreateDto;

    private KayttajatiedotCreateDto kayttajatiedotCreateDto;

    private String identifier;

    public void setHenkiloCreateDto(HenkiloCreateDto henkiloCreateDto) {
        this.henkiloCreateDto = henkiloCreateDto;
    }

    public HenkiloCreateDto getHenkiloCreateDto() {
        return henkiloCreateDto;
    }

    public KayttajatiedotCreateDto getKayttajatiedotCreateDto() {
        return kayttajatiedotCreateDto;
    }

    public void setKayttajatiedotCreateDto(KayttajatiedotCreateDto kayttajatiedotCreateDto) {
        this.kayttajatiedotCreateDto = kayttajatiedotCreateDto;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
