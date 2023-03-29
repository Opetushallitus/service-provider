package fi.vm.sade.saml.userdetails;

import fi.vm.sade.saml.exception.RequiredSamlAttributeNotProvidedException;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.springframework.security.saml.SAMLCredential;

import java.util.ArrayList;
import java.util.List;

import static fi.vm.sade.saml.userdetails.DelegatingUserDetailsService.IDENTIFIER_ATTRIBUTE_HAKA;
import static org.junit.Assert.assertEquals;

public class DelegatingUserDetailsServiceTest {
    private DelegatingUserDetailsService delegatingUserDetailsService;

    @Before
    public void setup() {
        this.delegatingUserDetailsService = new DelegatingUserDetailsService();
        delegatingUserDetailsService.setMpassidEntityId("https://virkailija.localopintopolku.fi/service-provider-app/saml/metadata/alias/mpassidtestsp");
    }

    @Test(expected = RequiredSamlAttributeNotProvidedException.class)
    public void requiredAttributeNotFound() {
        NameID nameID = new NameIDBuilder().buildObject();
        Assertion assertion = new AssertionBuilder().buildObject();
        SAMLCredential samlCredential = new SAMLCredential(nameID, assertion, null, null);
        this.delegatingUserDetailsService.loadUserBySAML(samlCredential);
    }
    @Test
    public void ePnnAttributeReadSuccessfully() {
        Attribute attribute = new AttributeBuilder().buildObject();
        attribute.setName(IDENTIFIER_ATTRIBUTE_HAKA);
        XSString xmlObject = new XSStringBuilder().buildObject(null, "localname", null);
        xmlObject.setValue("value");
        attribute.getAttributeValues().add(xmlObject);

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(attribute);

        NameID nameID = new NameIDBuilder().buildObject();
        Assertion assertion = new AssertionBuilder().buildObject();
        SAMLCredential samlCredential = new SAMLCredential(nameID, assertion, null, attributes, null);
        UserDetailsDto userDetailsDto = (UserDetailsDto) this.delegatingUserDetailsService.loadUserBySAML(samlCredential);

        assertEquals("haka", userDetailsDto.getAuthenticationMethod());
        assertEquals("value", userDetailsDto.getIdentifier());
    }
}

