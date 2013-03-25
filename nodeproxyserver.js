/*
 * Tällä voi pistää pystyyn oman https proxy serverin. Node.js pitää olla asennettuna.
 * Käynnistetään komennolla node nodeproxyservice.js
 */

var fs = require('fs'),
    http = require('http'),
    https = require('https'),
    httpProxy = require('http-proxy');

var options = {
    https: {
        key: fs.readFileSync('cert/privatekey.pem'),
        cert: fs.readFileSync('cert/certificate.pem')
    }
}

var proxy = new httpProxy.HttpProxy({
    target: {
        host: 'itest-virkailija.oph.ware.fi',
        port: 8443,
        https: true
    }
});

https.createServer(options.https, function (req, res) {
    proxy.proxyRequest(req, res);
}).listen(443);