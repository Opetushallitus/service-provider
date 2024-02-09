<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page session="true" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
	<head>
	    <title>Oppijan verkkopalvelu - Virkailijan palvelut - Sis&auml;&auml;nkirjautuminen</title>
        <c:if test="${not empty requestScope['isMobile'] and not empty mobileCss}">
             <meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=0;" />
             <meta name="apple-mobile-web-app-capable" content="yes" />
             <meta name="apple-mobile-web-app-status-bar-style" content="black" />
        </c:if>

        <link type="text/css" rel="stylesheet" href="css/virkailija.css" />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	    <link rel="icon" href="<c:url value="/favicon.ico" />" type="image/x-icon" />

      <link type="text/css" rel="stylesheet" href="<c:url value="/css/virkailija.css" />" />
	</head>
	<body class="fl-theme-iphone">
    <div id="wrapper" class="flc-screenNavigator-view-container">
        <div class="fl-screenNavigator-view">
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
    <h1 class="margin-bottom-3 margin-top-0">${error.title}</h1>
    <p>${error.description}</p>
    <h1 class="margin-bottom-3 margin-top-0">${error.title_sv}</h1>
    <p>${error.description_sv}</p>
    <p><a href="<c:url value="/saml/logout?local=true"/>">Palaa alkuun</a></p>
</div>

<div class="clear margin-bottom-4"></div>

</div>

            </div>
            <footer id="footer" class="offset-left-16-1 grid16-14">
                <div class="offset-left-16-2 grid16-3 padding-vertical-5">
                    <img src="<c:url value='/img/OPH_logo.svg' />" />
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
    </body>
</html>