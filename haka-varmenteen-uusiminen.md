HAKA varmenteen uusiminen
=
SAML kirjautuminen perustuu julkisen avaimen infrastruktuuriin. HAKA luottamusverkosto
ylläpitää [resurssirekisteriä](https://haka.funet.fi/metadata/haka-metadata.xml) josta eri 
palveluntarjoajien julkiset avaimet löytyvät.

Service provider applikaation varmenteet ovat tallessa [ympäristökohtaisissa](https://github.com/Opetushallitus/cloud-base/blob/master/docs/new-developer.md#p%C3%A4%C3%A4sy-ymp%C3%A4rist%C3%B6kohtaisiin-repoihin-codecommit) 
[keystoreissa](https://github.com/Opetushallitus/service-provider/blob/1bad2d01ae2437f6da8668dd56b35d2100587fc1/src/main/resources/security/securityContext.xml#L116-L126) joista saa tarvittaessa ulos resurssirekisterin vaatiman julkisen avaimen.

Varmenteet eivät ole ikuisia vaan vanhenevat määrättynä ajankohtana. 
Varmenteen vanheneminen taroittaa että kyseinen sisäänkirjautumismenetelmä ei toimi.
Tyypillisesti tästä saa resurssirekisteristä automaattisesti varoituksen ennakkoon.

Varmenteen vanhetessa luodaan uusi vanhentuneen tilalle sekä keystoreen
että resurssirekisteriin.

Tarvittavat työkalut
-

* [keytool](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)

Konfiguraatio
-
Varmenteet [konfiguroidaan](https://github.com/Opetushallitus/cloud-base/blob/master/docs/configuring-services.md) 
applikaatioon muutaman (ympäristökohtaisen) muuttujan avulla.
* services/service-provider/haka-cert-x509 - käytetään XML placeholderin populointiin. 
  sama arvo kuin resurssirekisterissä. *Huom: Tämä tulee päivittää jos/kun varmenne muuttuu!*
* services/service-provider/key-alias - varmenteen nimi keystoressa
* services/service-provider/key-password - varmenne spesifinen salasana
* services/service-provider/keystore-password - keystoren salasana

Työvaiheet
-

Ympäristön keystore löytyy ympäristökohtaisesta CodeCommit repositorystä (cloud-environment-${ympäristö}) polusta:
`authentication/haka/keystore.jks`


Tarkista vanhan varmenteen tiedot
--

Ota talteen vanhan varmenteen tiedot: 

`keytool -list -v -alias ${key-alias} -keystore ${keystore} --storepass ${keystore-password}`

Poista vanha varmenne
--

`keytool -delete -alias ${key-alias} -keystore ${keystore} --storepass ${keystore-password}`

Generoi uusi varmenne
--

Seuraava komento käynnistää interaktiivisen kehoitteen uuden varmenteen luomiseksi. 
Huom! Tarvitset tässä ensimmäisessä vaiheessa saatuja tietoja. 

`keytool -genkey -keyalg RSA -sigalg SHA256withRSA -validity 1800 -keysize 4096 -alias ${key-alias} -keystore ${keystore} --storepass ${keystore-password}`

Kopioi seuraavat tiedot vanhasta varmenteesta
```
Owner: CN=virkailija.testiopintopolku.fi, OU=Opetushallitus, O=Opetushallitus, L=Unknown, ST=Unknown, C=FI
Issuer: CN=virkailija.testiopintopolku.fi, OU=Opetushallitus, O=Opetushallitus, L=Unknown, ST=Unknown, C=FI
```

Exporttaa julkinen avain
--

Toimita julkinen avain resurssirekisterin ylläpidosta vastaavalle virkamiehelle.

`keytool -export -rfc -alias ${key-alias} -keystore ${keystore} --storepass ${keystore-password}`

Odota kunnes resurssirekisteri on päivittynyt ([tukee](https://wiki.eduuni.fi/display/CSCHAKA/SAML-varmenteen+vaihtaminen) useampia samanaikaisia varmenteita)

Päivitä salaisuuksienhallinta
--

**services/service-provider/haka-cert-x509** konfiguraatioparametri tulee muuttaa vastaamaan keystoressa olevaa
varmennetta. *Huom: ilman `-----BEGIN/END CERTIFICATE-----` rivejä!*

Koska varmenteessa on useita rivejä lienee se helponta päivittää aws consolen kautta.

Päivitä applikaation konfiguraatio
--

* Tallenna tehdyt muutokset ympäristökohtaiseen CodeCommit repositoryyn. 
* Julkaise muuttunut konfiguraatio: `cloud-base/aws/config.py ${ympäristö} publish`
* Uudelleenkäynnistä applikaatio: `cloud-base/aws/cloudformation.py ${ympäristö} services update -s service-provider`

Informoi tunnistuspalveluita
--

Uusi varmenne ei päivity automaattisesti kaikkiin tunnistuspalveluhin vaan se tulee tehdä niihin manuaalisesti.
Ainakin Valtorin ylläpitämä Valtti on tällainen.
Ko. tunnistuspalveluita pitää siis informoida ajoissa ja kooridinoida varmenteen vaihdon ajankohta.
