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
};

var proxy = httpProxy.createProxyServer({
    target: 'https://localhost:8443',
    secure: false
});

https.createServer(options.https, function (req, res) {
    console.log("Got request");
    proxy.proxyRequest(req, res);
}).listen(443);
