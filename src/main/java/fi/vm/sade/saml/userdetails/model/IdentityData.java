package fi.vm.sade.saml.userdetails.model;

import fi.vm.sade.authentication.service.types.AddHenkiloDataType;

public class IdentityData extends AddHenkiloDataType {

    /**
     * 
     */
    private static final long serialVersionUID = -5250521554026071402L;

    private String domainNimi;

    public String getDomainNimi() {
        return domainNimi;
    }

    public void setDomainNimi(String domainNimi) {
        this.domainNimi = domainNimi;
    }

}
