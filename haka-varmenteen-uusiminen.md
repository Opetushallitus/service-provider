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
* services/service-provider/haka-cert-x509-secondary - käytetään XML placeholderin populointiin.
  sama arvo kuin resurssirekisterissä. *Huom: Tämä tulee päivittää jos/kun varmenne muuttuu!*
* services/service-provider/key-alias - varmenteen nimi keystoressa
* services/service-provider/key-alias-secondary - toisen varmenteen nimi keystoressa
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

Generoi uusi varmenne
--

Aliaksena käytetään muotoa `${ympäristö}_hakasp_selfsigned_${vuosi}` esim. `untuva_hakasp_selfsigned_2023`.

Huom! Varmista että dname parametrin tiedot ovat samat kuin aiemmassa varmenteessa.

Esimerkki untuva-ympäristöstä vuoden 2023 sertifikaatin luonnista:

```
keytool -keystore ${keystore} -storepass ${keystore-password} -genkey -keyalg RSA -sigalg SHA256withRSA -validity 1800 -keysize 4096 \
  -alias untuva_hakasp_selfsigned_2023 -keypass ${key-password} \
  -dname "CN=virkailija.untuvaopintopolku.fi, OU=Opetushallitus, O=Opetushallitus, L=HELSINKI, ST=UUSIMAA, C=FI"
```

Tallenna tehdyt muutokset ympäristökohtaiseen CodeCommit repositoryyn ja julkaise muuttunut konfiguraatio:

```
cloud-base/aws/config.py ${ympäristö} publish
```

Tämä on OK sillä uusi varmenne ei päädy käyttöön ennen kuin Parameter Storeen päivittää uuden varmenteen aliaksen.

Lisää uusi varmenne resurssirekisteriin
--

Toimita julkinen avain resurssirekisterin ylläpidosta vastaavalle virkamiehelle, joka lisää sertifikaatin resurssirekisteriin aiemman varmenteen rinnalle.

```
keytool -export -rfc -alias ${key-alias} -keystore ${keystore} --storepass ${keystore-password} > untuva_hakasp_selfsigned_2023.cert
```

Odota kunnes resurssirekisteri on päivittynyt ([tukee](https://wiki.eduuni.fi/display/CSCHAKA/SAML-varmenteen+vaihtaminen) useampia samanaikaisia varmenteita)

Päivitä salaisuuksienhallinta
--

Sovellukselle on määritelty kaksi varmennetta, jotta varmenne voidaan vaihtaa katkottomasti.

Kun varmenne on paikallaan keystoressa voit asettaa AWS:n Parameter Storessa seuraavat parametrit uuden sertifikaatin arvoilla:
- `services/service-provider/key-alias-secondary` (esim. `untuva_hakasp_selfsigned_2023`)
- `services/service-provider/haka-cert-x509-secondary` (sama kuin resurssirekisteriin lisätty, myös ilman `-----BEGIN/END CERTIFICATE-----` rivejä)

Esimerkki untuva -ympäristöstä:

```
aws ssm put-parameter --name /untuva/services/service-provider/key-alias-secondary --value untuva_hakasp_selfsigned_2023 --type SecureString --overwrite
aws ssm put-parameter --name /untuva/services/service-provider/haka-cert-x509-secondary --value file://./untuva_hakasp_selfsigned_2023.cert --type SecureString --overwrite
```

Uudelleenkäynnistä applikaatio, jotta uudet parametrit tulevat käyttöön: `cloud-base/aws/cloudformation.py ${ympäristö} services update -s service-provider`

Informoi tunnistuspalveluita
--

Uusi varmenne ei päivity automaattisesti kaikkiin tunnistuspalveluhin vaan se tulee tehdä niihin manuaalisesti.
Ainakin Valtorin ylläpitämä Valtti on tällainen.
Ko. tunnistuspalveluita pitää siis informoida ajoissa ja kooridinoida varmenteen vaihdon ajankohta.

Vanhan varmenteen poisto käytöstä
--

Päivitä AWS:n Parameter Storessa `key-alias` ja `haka-cert-x509` parametrit samoihin arvoihin kuin `key-alias-secondary` ja
`haka-cert-x509-secondary`, jolloin vain uusi varmenne on enää käytössä.

Esimerkki untuva -ympäristöstä:

```
aws ssm put-parameter --name /untuva/services/service-provider/key-alias --value untuva_hakasp_selfsigned_2023 --type SecureString --overwrite
aws ssm put-parameter --name /untuva/services/service-provider/haka-cert-x509 --value file://./untuva_hakasp_selfsigned_2023.cert --type SecureString --overwrite
```
