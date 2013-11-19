<%--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License.  You may obtain a
    copy of the License at the following location:

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page session="true" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:theme code="mobile.custom.css.file" var="mobileCss" text="" />
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
	<head>
	    <title>Oppijan verkkopalvelu - Virkailijan palvelut - Sis&auml;&auml;nkirjautuminen</title>
        <c:if test="${not empty requestScope['isMobile'] and not empty mobileCss}">
             <meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=0;" />
             <meta name="apple-mobile-web-app-capable" content="yes" />
             <meta name="apple-mobile-web-app-status-bar-style" content="black" />


             <!--<link type="text/css" rel="stylesheet" media="screen" href="<c:url value="/css/fss-framework-1.1.2.css" />" />
             <link type="text/css" rel="stylesheet" href="<c:url value="/css/fss-mobile-${requestScope['browserType']}-layout.css" />" />

             <link type="text/css" rel="stylesheet" href="${mobileCss}" />-->
        </c:if>

        <link type="text/css" rel="stylesheet" href="css/virkailija.css" />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	    <link rel="icon" href="<c:url value="/favicon.ico" />" type="image/x-icon" />

      <link type="text/css" rel="stylesheet" href="<c:url value="/css/virkailija.css" />" />
	</head>
	<body class="fl-theme-iphone">
    <div id="wrapper" class="flc-screenNavigator-view-container">
        <div class="fl-screenNavigator-view">
            <!--
            <div id="header" class="flc-screenNavigator-navbar fl-navbar fl-table">
				        <h1 id="company-name">Jasig</h1>
                <h1 id="app-name" class="fl-table-cell">Central Authentication Service (CAS)</h1>
            </div>

          -->
            <div id="content" class="fl-screenNavigator-scroll-container">

<header id="siteheader" class="width-100">
    <div class="header-content">
        <img class="margin-left-2" src="<c:url value='/img/opintopolkufi.png' /> "/>

        <a class="float-right margin-right-2" href="#">P&aring; svenska</a>
        <span class="float-right margin-right-1">|</span>
        <a class="bold float-right margin-right-1" href="#">Suomeksi</a>

    </div>
</header>


<div class="grid16-11 offset-left-16-2 margin-vertical-5">

</div>


<div class="clear margin-bottom-3"></div>

<div class="offset-left-16-4 grid16-8">
    <h1 class="margin-bottom-3 margin-top-0">Haka-tunnistautumista ei aktivoitu</h1>
    <p>Suoritit onnistuneen Haka-tunnistautumisen, mutta Haka-tunnuksiisi ei ole liitetty
    Opintopolku.fi-käyttäjätunnusta. Mikäli haluat tunnistautua Haka-tunnuksilla, ole hyvä
    ja ota yhteyttä Opintopolku.fi-yhteyshenkilöösi.</p>
    <p><a href="<c:url value="/saml/logout?local=true"/>">Palaa alkuun</a></p>
</div>

<div class="clear margin-bottom-4"></div>

</div>

            </div>
            <footer id="footer" class="offset-left-16-1 grid16-14">
                <div class="offset-left-16-2 grid16-3 padding-vertical-5">
                    <img src="<c:url value='/img/OPH_logo.png' />" />
                </div>
                <div class="grid16-3 padding-vertical-5">
                    <address class="address">
                        Opetushallitus <br />
                        Hakaniemenranta 6 <br />
                        PL 380, 00531 Helsinki <br />
                        puhelin 029 533 1000
                    </address>
                </div>
                <div class="grid16-3 padding-vertical-5">
                    <img src="<c:url value='/img/OKM_logo.png' />" />
                </div>
                <div class="grid16-3 padding-vertical-5">
                    <address class="address">
                        Opetus- ja kulttuuriministeri&ouml; <br />
                        PL 29, 00023 valtioneuvosto <br />
                        puhelin 0295 3 30004
                    </address>
                </div>


            </footer>
            <div class="clear"></div>

        </div>
        <script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js"></script>
        <script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.5/jquery-ui.min.js"></script>
        <script type="text/javascript" src="<c:url value="/js/cas.js" />"></script>

		<!-- Piwik -->
        <script type="text/javascript">

			var siteDomain = document.domain;
			var piwikSiteId = 2;
			if(siteDomain=='opintopolku.fi'){
			   piwikSiteId = 4;
			}else if(siteDomain=='virkailija.opintopolku.fi'){
			   piwikSiteId = 3;
			}else if(siteDomain=='testi.opintopolku.fi'){
			   piwikSiteId = 1;
			}else if(siteDomain=='testi.virkailija.opintopolku.fi'){
			   piwikSiteId = 5;
			}else{
			   piwikSiteId = 2;
			}

			//console.log("siteDomain:"+siteDomain+", piwikSiteId:"+piwikSiteId);

			var _paq = _paq || [];
			_paq.push(["setDocumentTitle", document.domain + "/" + document.title]);
			_paq.push(["trackPageView"]);
			_paq.push(["enableLinkTracking"]);

			(function() {
				var u=(("https:" == document.location.protocol) ? "https" : "http") + "://nat-piwik-poc.oph.ware.fi/analytics/";
				_paq.push(["setTrackerUrl", u+"piwik.php"]);
				_paq.push(["setSiteId", piwikSiteId]);
				var d=document, g=d.createElement("script"), s=d.getElementsByTagName("script")[0]; g.type="text/javascript";
				g.defer=true; g.async=true; g.src=u+"piwik.js"; s.parentNode.insertBefore(g,s);
			})();
        </script>
        <!-- End Piwik Code -->

    </body>
</html>