var exec = require("cordova/exec");

var PLUGIN_NAME = "rootSafety";

var rootSafety = {
  checkGooglePlayServicesAvailability: function(cb, err) {
    exec(cb, err, PLUGIN_NAME, "checkGooglePlayServicesAvailability", []);
  },
  attest: function(nonce, api_key, cb, err) {
    exec(cb, err, PLUGIN_NAME, "attest", [nonce, api_key]);
  },
  offlineVerification: function(jws, cb, err) {
    exec(cb, err, PLUGIN_NAME, "offlineVerify", [jws]);
  },
  onlineVerification: function(api_key, jws, cb, err) {
    exec(cb, err, PLUGIN_NAME, "onlineVerify", [api_key, jws]);
  },
  extractPayload: function(jws, cb, err) {
    exec(cb, err, PLUGIN_NAME, "extractPayload", [jws]);
  },
  checkAppVerification: function(cb, err) {
    exec(cb, err, PLUGIN_NAME, "checkAppVerification", []);
  },
  listHarmfulApps: function(cb, err) {
    exec(cb, err, PLUGIN_NAME, "listHarmfulApps", []);
  },
  enableAppVerification: function(cb, err) {
    exec(cb, err, PLUGIN_NAME, "enableVerifyApps", []);
  }
};

if (typeof module != "undefined" && module.exports) {
  module.exports = rootSafety;
}
