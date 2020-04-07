goog.provide("goog.labs.userAgent.util");
goog.require("goog.string.internal");
/**
 * @private
 * @return {string}
 */
goog.labs.userAgent.util.getNativeUserAgentString_ = function() {
  var navigator = goog.labs.userAgent.util.getNavigator_();
  if (navigator) {
    var userAgent = navigator.userAgent;
    if (userAgent) {
      return userAgent;
    }
  }
  return "";
};
/**
 * @private
 * @return {Navigator}
 */
goog.labs.userAgent.util.getNavigator_ = function() {
  return goog.global.navigator;
};
/** @private @type {string} */ goog.labs.userAgent.util.userAgent_ = goog.labs.userAgent.util.getNativeUserAgentString_();
/**
 * @param {?string=} opt_userAgent
 */
goog.labs.userAgent.util.setUserAgent = function(opt_userAgent) {
  goog.labs.userAgent.util.userAgent_ = opt_userAgent || goog.labs.userAgent.util.getNativeUserAgentString_();
};
/**
 * @return {string}
 */
goog.labs.userAgent.util.getUserAgent = function() {
  return goog.labs.userAgent.util.userAgent_;
};
/**
 * @param {string} str
 * @return {boolean}
 */
goog.labs.userAgent.util.matchUserAgent = function(str) {
  var userAgent = goog.labs.userAgent.util.getUserAgent();
  return goog.string.internal.contains(userAgent, str);
};
/**
 * @param {string} str
 * @return {boolean}
 */
goog.labs.userAgent.util.matchUserAgentIgnoreCase = function(str) {
  var userAgent = goog.labs.userAgent.util.getUserAgent();
  return goog.string.internal.caseInsensitiveContains(userAgent, str);
};
/**
 * @param {string} userAgent
 * @return {!Array<!Array<string>>}
 */
goog.labs.userAgent.util.extractVersionTuples = function(userAgent) {
  var versionRegExp = new RegExp("(\\w[\\w ]+)" + "/" + "([^\\s]+)" + "\\s*" + "(?:\\((.*?)\\))?", "g");
  var data = [];
  var match;
  while (match = versionRegExp.exec(userAgent)) {
    data.push([match[1], match[2], match[3] || undefined]);
  }
  return data;
};

//# sourceMappingURL=goog.labs.useragent.util.js.map
