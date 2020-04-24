goog.provide("goog.html.TrustedResourceUrl");
goog.require("goog.asserts");
goog.require("goog.html.trustedtypes");
goog.require("goog.i18n.bidi.Dir");
goog.require("goog.i18n.bidi.DirectionalString");
goog.require("goog.string.Const");
goog.require("goog.string.TypedString");
/**
 * @final
 * @struct
 * @constructor
 * @implements {goog.i18n.bidi.DirectionalString}
 * @implements {goog.string.TypedString}
 * @param {!Object=} opt_token
 * @param {(!TrustedScriptURL|string)=} opt_content
 */
goog.html.TrustedResourceUrl = function(opt_token, opt_content) {
  /** @private @const @type {(!TrustedScriptURL|string)} */ this.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue_ = opt_token === goog.html.TrustedResourceUrl.CONSTRUCTOR_TOKEN_PRIVATE_ && opt_content || "";
  /** @private @const @type {!Object} */ this.TRUSTED_RESOURCE_URL_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = goog.html.TrustedResourceUrl.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_;
};
/** @const @override */ goog.html.TrustedResourceUrl.prototype.implementsGoogStringTypedString = true;
/** @override */ goog.html.TrustedResourceUrl.prototype.getTypedStringValue = function() {
  return this.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue_.toString();
};
/** @const @override */ goog.html.TrustedResourceUrl.prototype.implementsGoogI18nBidiDirectionalString = true;
/** @override */ goog.html.TrustedResourceUrl.prototype.getDirection = function() {
  return goog.i18n.bidi.Dir.LTR;
};
/**
 * @param {(string|?Object<string,*>|undefined)} searchParams
 * @param {(string|?Object<string,*>)=} opt_hashParams
 * @return {!goog.html.TrustedResourceUrl}
 */
goog.html.TrustedResourceUrl.prototype.cloneWithParams = function(searchParams, opt_hashParams) {
  var url = goog.html.TrustedResourceUrl.unwrap(this);
  var parts = goog.html.TrustedResourceUrl.URL_PARAM_PARSER_.exec(url);
  var urlBase = parts[1];
  var urlSearch = parts[2] || "";
  var urlHash = parts[3] || "";
  return goog.html.TrustedResourceUrl.createTrustedResourceUrlSecurityPrivateDoNotAccessOrElse(urlBase + goog.html.TrustedResourceUrl.stringifyParams_("?", urlSearch, searchParams) + goog.html.TrustedResourceUrl.stringifyParams_("#", urlHash, opt_hashParams));
};
if (goog.DEBUG) {
  /** @override */ goog.html.TrustedResourceUrl.prototype.toString = function() {
    return "TrustedResourceUrl{" + this.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue_ + "}";
  };
}
/**
 * @param {!goog.html.TrustedResourceUrl} trustedResourceUrl
 * @return {string}
 */
goog.html.TrustedResourceUrl.unwrap = function(trustedResourceUrl) {
  return goog.html.TrustedResourceUrl.unwrapTrustedScriptURL(trustedResourceUrl).toString();
};
/**
 * @param {!goog.html.TrustedResourceUrl} trustedResourceUrl
 * @return {(!TrustedScriptURL|string)}
 */
goog.html.TrustedResourceUrl.unwrapTrustedScriptURL = function(trustedResourceUrl) {
  if (trustedResourceUrl instanceof goog.html.TrustedResourceUrl && trustedResourceUrl.constructor === goog.html.TrustedResourceUrl && trustedResourceUrl.TRUSTED_RESOURCE_URL_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ === goog.html.TrustedResourceUrl.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_) {
    return trustedResourceUrl.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue_;
  } else {
    goog.asserts.fail("expected object of type TrustedResourceUrl, got '" + trustedResourceUrl + "' of type " + goog.typeOf(trustedResourceUrl));
    return "type_error:TrustedResourceUrl";
  }
};
/**
 * @param {!goog.string.Const} format
 * @param {!Object<string,(string|number|!goog.string.Const)>} args
 * @return {!goog.html.TrustedResourceUrl}
 * @throws {!Error}
 */
goog.html.TrustedResourceUrl.format = function(format, args) {
  var formatStr = goog.string.Const.unwrap(format);
  if (!goog.html.TrustedResourceUrl.BASE_URL_.test(formatStr)) {
    throw new Error("Invalid TrustedResourceUrl format: " + formatStr);
  }
  var result = formatStr.replace(goog.html.TrustedResourceUrl.FORMAT_MARKER_, function(match, id) {
    if (!Object.prototype.hasOwnProperty.call(args, id)) {
      throw new Error('Found marker, "' + id + '", in format string, "' + formatStr + '", but no valid label mapping found ' + "in args: " + JSON.stringify(args));
    }
    var arg = args[id];
    if (arg instanceof goog.string.Const) {
      return goog.string.Const.unwrap(arg);
    } else {
      return encodeURIComponent(String(arg));
    }
  });
  return goog.html.TrustedResourceUrl.createTrustedResourceUrlSecurityPrivateDoNotAccessOrElse(result);
};
/** @private @const @type {!RegExp} */ goog.html.TrustedResourceUrl.FORMAT_MARKER_ = /%{(\w+)}/g;
/** @private @const @type {!RegExp} */ goog.html.TrustedResourceUrl.BASE_URL_ = new RegExp("^((https:)?//[0-9a-z.:[\\]-]+/" + "|/[^/\\\\]" + "|[^:/\\\\%]+/" + "|[^:/\\\\%]*[?#]" + "|about:blank#" + ")", "i");
/** @private @const @type {!RegExp} */ goog.html.TrustedResourceUrl.URL_PARAM_PARSER_ = /^([^?#]*)(\?[^#]*)?(#[\s\S]*)?/;
/**
 * @param {!goog.string.Const} format
 * @param {!Object<string,(string|number|!goog.string.Const)>} args
 * @param {(string|?Object<string,*>|undefined)} searchParams
 * @param {(string|?Object<string,*>)=} opt_hashParams
 * @return {!goog.html.TrustedResourceUrl}
 * @throws {!Error}
 */
goog.html.TrustedResourceUrl.formatWithParams = function(format, args, searchParams, opt_hashParams) {
  var url = goog.html.TrustedResourceUrl.format(format, args);
  return url.cloneWithParams(searchParams, opt_hashParams);
};
/**
 * @param {!goog.string.Const} url
 * @return {!goog.html.TrustedResourceUrl}
 */
goog.html.TrustedResourceUrl.fromConstant = function(url) {
  return goog.html.TrustedResourceUrl.createTrustedResourceUrlSecurityPrivateDoNotAccessOrElse(goog.string.Const.unwrap(url));
};
/**
 * @param {!Array<!goog.string.Const>} parts
 * @return {!goog.html.TrustedResourceUrl}
 */
goog.html.TrustedResourceUrl.fromConstants = function(parts) {
  var unwrapped = "";
  for (var i = 0; i < parts.length; i++) {
    unwrapped += goog.string.Const.unwrap(parts[i]);
  }
  return goog.html.TrustedResourceUrl.createTrustedResourceUrlSecurityPrivateDoNotAccessOrElse(unwrapped);
};
/** @private @const @type {!Object} */ goog.html.TrustedResourceUrl.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = {};
/**
 * @package
 * @param {string} url
 * @return {!goog.html.TrustedResourceUrl}
 */
goog.html.TrustedResourceUrl.createTrustedResourceUrlSecurityPrivateDoNotAccessOrElse = function(url) {
  var value = goog.html.trustedtypes.PRIVATE_DO_NOT_ACCESS_OR_ELSE_POLICY ? goog.html.trustedtypes.PRIVATE_DO_NOT_ACCESS_OR_ELSE_POLICY.createScriptURL(url) : url;
  return new goog.html.TrustedResourceUrl(goog.html.TrustedResourceUrl.CONSTRUCTOR_TOKEN_PRIVATE_, value);
};
/**
 * @private
 * @param {string} prefix
 * @param {string} currentString
 * @param {(string|?Object<string,*>|undefined)} params
 * @return {string}
 */
goog.html.TrustedResourceUrl.stringifyParams_ = function(prefix, currentString, params) {
  if (params == null) {
    return currentString;
  }
  if (typeof params === "string") {
    return params ? prefix + encodeURIComponent(params) : "";
  }
  for (var key in params) {
    var value = params[key];
    var outputValues = goog.isArray(value) ? value : [value];
    for (var i = 0; i < outputValues.length; i++) {
      var outputValue = outputValues[i];
      if (outputValue != null) {
        if (!currentString) {
          currentString = prefix;
        }
        currentString += (currentString.length > prefix.length ? "\x26" : "") + encodeURIComponent(key) + "\x3d" + encodeURIComponent(String(outputValue));
      }
    }
  }
  return currentString;
};
/** @private @const @type {!Object} */ goog.html.TrustedResourceUrl.CONSTRUCTOR_TOKEN_PRIVATE_ = {};

//# sourceMappingURL=goog.html.trustedresourceurl.js.map
