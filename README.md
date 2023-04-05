Service Provider App
=
Toimii service providerina HAKA kirjautumisessa. Kaikki faktat HAKA-kirjautumisesta ja vähän päälle löytyy
lähtökohtaisesti OPH:n wikistä sivulta [HAKA-autentikaatio](https://wiki.eduuni.fi/display/OPHSS/HAKA-autentikaatio).
Muita lukemisen arvoisia sivuja ovat [Haka metadata](https://wiki.eduuni.fi/display/CSCHAKA/Haka+metadata),
[Testipalvelimet](https://wiki.eduuni.fi/display/CSCHAKA/Testipalvelimet),
[Usein kysytyt kysymykset](https://wiki.eduuni.fi/display/CSCHAKA/Usein+kysytyt+kysymykset)
ja [Haka](https://wiki.eduuni.fi/display/OPHPALV/Haka).

Lokaali ajaminen tapahtuu QA-ympäristöä vasten käyttäen luokalle konfiguroitua testihakaa. Tämä joudutaan tekemään näin koska haka palautuu itest-virkailija.oph.ware.fi osoitteeseen ja jotta tämä saadaan ohjattua localhostiin pitää se varata hosts-tiedostoon jolloin luokan käyttö dns-nimellä ei onnistu. 

Toistuvat ylläpidolliset toimet
-
* [HAKA varmenteen uusiminen](haka-varmenteen-uusiminen.md)

Lokaali ajamisen valmistelu
-
1) Lisää seuraavat projektin mukana tuleva tiedostot `oph-configuration`-kansioosi
   * `keystore.jks` löytyy keystore-kansiosta
   * `hakasp.xml` löytyy metadata-kansiosta
2) Hae QA:lta authentication:in `common.properties` ja uudellen nimeä se `service-provider-app.properties`:iksi. Tee seuraavat muutokset:
```
sp.host.virkailija=itest-virkailija.oph.ware.fi
keystore.url=file:///${user.home.conf}/keystore.jks
keystore.password=m4JNeOMhcI42psiRyMS5
sp.keyalias=luokka_hakasp_selfsigned
sp.keypassword=61rj9jtBPCwqe9cfbTwr
haka.metadata.url=https://haka.funet.fi/metadata/haka_test_metadata_signed.xml
haka.wayf.url=https://testsp.funet.fi/shibboleth/WAYF
authentication.hostedsp=https://itest-virkailija.oph.ware.fi/service-provider-app/saml/metadata/alias/hakasp
hakasp.metadatafile=file\://${user.home.conf}/hakasp.xml

mpassid.metadata.entityid=https://virkailija.localopintopolku.fi/service-provider-app/saml/metadata/alias/mpassidtestsp
mpassid.metadata.alias=mpassidtestsp
mpassid.metadata.url=https://mpass-proxy-test.csc.fi/idp/shibboleth
mpassid.keyalias=local_mpassidtestsp_selfsigned_2023
mpassid.metadatafile=classpath:/local-mpassidtestsp.xml
```
3) Lisää hosts-tiedostoosi rivi `127.0.0.1 itest-virkailija.oph.ware.fi` 
   * Löytyy windowsista `%SystemRoot%\System32\drivers\etc\hosts`
   
Lokaali ajaminen
-
Mene service-provider-app:in juurihakemistoon ja aja seuraavat komennot:

    mvn clean install
    node nodeproxyserver.js
    mvn jetty:run
Palvelun mukana tuleva nodeproxyn käyttämä sertifikaatti on vanhentunut joten selaimesi pitää ajaa flagilla joka jättää tämän huomioimatta. Esim. chromella tämä tapahtuu windowsissa:
* `chrome.exe --user-data-dir="C:/Chrome dev session" --ignore-certificate-errors`
 
Huom. Et halua selailla internetin pimeitä kolkkia näin ajetulla selaimella.

Palvelu pyörii oletuksen osoitteessa `https://localhost:8443/service-provider-app` 

Voit myös käyttää osoitetta `https://itest-virkailija.oph.ware.fi/service-provider-app` koska jouduit määrittämään tämän osoitteen hosts-tiedostosta localhostiin.

Huom. urin skeeman täytyy olla https

Koska aiemmin haettiin QA:n common.properties-tiedosto täytyy QA:ltä löytyä joltain tunnukselta käytetyn tunnuksen HAKA identifieri authentication-kannasta.
