package fi.vm.sade.saml.userdetails;

public class HenkiloCreateDto {

    private String etunimet;
    private String sukunimi;
    private String kutsumanimi;
    private String henkiloTyyppi;

    public String getEtunimet() {
        return etunimet;
    }

    public void setEtunimet(String etunimet) {
        this.etunimet = etunimet;
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public void setSukunimi(String sukunimi) {
        this.sukunimi = sukunimi;
    }

    public String getKutsumanimi() {
        return kutsumanimi;
    }

    public void setKutsumanimi(String kutsumanimi) {
        this.kutsumanimi = kutsumanimi;
    }

    public String getHenkiloTyyppi() {
        return henkiloTyyppi;
    }

    public void setHenkiloTyyppi(String henkiloTyyppi) {
        this.henkiloTyyppi = henkiloTyyppi;
    }

    @Override
    public String toString() {
        return "HenkiloCreateDto{" + "etunimet=" + etunimet + ", sukunimi=" + sukunimi + ", kutsumanimi=" + kutsumanimi + ", henkiloTyyppi=" + henkiloTyyppi + '}';
    }

}
