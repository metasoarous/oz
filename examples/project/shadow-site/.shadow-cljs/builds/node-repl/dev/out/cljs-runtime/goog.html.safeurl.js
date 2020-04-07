goog.provide("goog.html.SafeUrl");
goog.require("goog.asserts");
goog.require("goog.fs.url");
goog.require("goog.html.TrustedResourceUrl");
goog.require("goog.i18n.bidi.Dir");
goog.require("goog.i18n.bidi.DirectionalString");
goog.require("goog.string.Const");
goog.require("goog.string.TypedString");
goog.require("goog.string.internal");
/**
 * @final
 * @struct
 * @constructor
 * @implements {goog.i18n.bidi.DirectionalString}
 * @implements {goog.string.TypedString}
 * @param {!Object=} opt_token
 * @param {string=} opt_content
 */
goog.html.SafeUrl = function(opt_token, opt_content) {
  /** @private @type {string} */ this.privateDoNotAccessOrElseSafeUrlWrappedValue_ = opt_token === goog.html.SafeUrl.CONSTRUCTOR_TOKEN_PRIVATE_ && opt_content || "";
  /** @private @const @type {!Object} */ this.SAFE_URL_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = goog.html.SafeUrl.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_;
};
/** @const @type {string} */ goog.html.SafeUrl.INNOCUOUS_STRING = "about:invalid#zClosurez";
/** @const @override */ goog.html.SafeUrl.prototype.implementsGoogStringTypedString = true;
/** @override */ goog.html.SafeUrl.prototype.getTypedStringValue = function() {
  return this.privateDoNotAccessOrElseSafeUrlWrappedValue_.toString();
};
/** @const @override */ goog.html.SafeUrl.prototype.implementsGoogI18nBidiDirectionalString = true;
/** @override */ goog.html.SafeUrl.prototype.getDirection = function() {
  return goog.i18n.bidi.Dir.LTR;
};
if (goog.DEBUG) {
  /** @override */ goog.html.SafeUrl.prototype.toString = function() {
    return "SafeUrl{" + this.privateDoNotAccessOrElseSafeUrlWrappedValue_ + "}";
  };
}
/**
 * @param {!goog.html.SafeUrl} safeUrl
 * @return {string}
 */
goog.html.SafeUrl.unwrap = function(safeUrl) {
  if (safeUrl instanceof goog.html.SafeUrl && safeUrl.constructor === goog.html.SafeUrl && safeUrl.SAFE_URL_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ === goog.html.SafeUrl.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_) {
    return safeUrl.privateDoNotAccessOrElseSafeUrlWrappedValue_;
  } else {
    goog.asserts.fail("expected object of type SafeUrl, got '" + safeUrl + "' of type " + goog.typeOf(safeUrl));
    return "type_error:SafeUrl";
  }
};
/**
 * @param {!goog.string.Const} url
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromConstant = function(url) {
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(goog.string.Const.unwrap(url));
};
/** @private @const */ goog.html.SAFE_MIME_TYPE_PATTERN_ = new RegExp("^(?:audio/(?:3gpp2|3gpp|aac|L16|midi|mp3|mp4|mpeg|oga|ogg|opus|x-m4a|x-wav|wav|webm)|" + "image/(?:bmp|gif|jpeg|jpg|png|tiff|webp|x-icon)|" + "text/csv|" + "video/(?:mpeg|mp4|ogg|webm|quicktime))" + '(?:;\\w+\x3d(?:\\w+|"[\\w;\x3d]+"))*$', "i");
/**
 * @param {string} mimeType
 * @return {boolean}
 */
goog.html.SafeUrl.isSafeMimeType = function(mimeType) {
  return goog.html.SAFE_MIME_TYPE_PATTERN_.test(mimeType);
};
/**
 * @param {!Blob} blob
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromBlob = function(blob) {
  var url = goog.html.SAFE_MIME_TYPE_PATTERN_.test(blob.type) ? goog.fs.url.createObjectUrl(blob) : goog.html.SafeUrl.INNOCUOUS_STRING;
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(url);
};
/** @private @const */ goog.html.DATA_URL_PATTERN_ = /^data:([^,]*);base64,[a-z0-9+\/]+=*$/i;
/**
 * @param {string} dataUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromDataUrl = function(dataUrl) {
  var filteredDataUrl = dataUrl.replace(/(%0A|%0D)/g, "");
  var match = filteredDataUrl.match(goog.html.DATA_URL_PATTERN_);
  var valid = match && goog.html.SAFE_MIME_TYPE_PATTERN_.test(match[1]);
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(valid ? filteredDataUrl : goog.html.SafeUrl.INNOCUOUS_STRING);
};
/**
 * @param {string} telUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromTelUrl = function(telUrl) {
  if (!goog.string.internal.caseInsensitiveStartsWith(telUrl, "tel:")) {
    telUrl = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(telUrl);
};
/** @private @const */ goog.html.SIP_URL_PATTERN_ = new RegExp("^sip[s]?:[+a-z0-9_.!$%\x26'*\\/\x3d^`{|}~-]+@([a-z0-9-]+\\.)+[a-z0-9]{2,63}$", "i");
/**
 * @param {string} sipUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromSipUrl = function(sipUrl) {
  if (!goog.html.SIP_URL_PATTERN_.test(decodeURIComponent(sipUrl))) {
    sipUrl = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(sipUrl);
};
/**
 * @param {string} facebookMessengerUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromFacebookMessengerUrl = function(facebookMessengerUrl) {
  if (!goog.string.internal.caseInsensitiveStartsWith(facebookMessengerUrl, "fb-messenger://share")) {
    facebookMessengerUrl = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(facebookMessengerUrl);
};
/**
 * @param {string} whatsAppUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromWhatsAppUrl = function(whatsAppUrl) {
  if (!goog.string.internal.caseInsensitiveStartsWith(whatsAppUrl, "whatsapp://send")) {
    whatsAppUrl = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(whatsAppUrl);
};
/**
 * @param {string} smsUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromSmsUrl = function(smsUrl) {
  if (!goog.string.internal.caseInsensitiveStartsWith(smsUrl, "sms:") || !goog.html.SafeUrl.isSmsUrlBodyValid_(smsUrl)) {
    smsUrl = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(smsUrl);
};
/**
 * @private
 * @param {string} smsUrl
 * @return {boolean}
 */
goog.html.SafeUrl.isSmsUrlBodyValid_ = function(smsUrl) {
  var hash = smsUrl.indexOf("#");
  if (hash > 0) {
    smsUrl = smsUrl.substring(0, hash);
  }
  var bodyParams = smsUrl.match(/[?&]body=/gi);
  if (!bodyParams) {
    return true;
  }
  if (bodyParams.length > 1) {
    return false;
  }
  var bodyValue = smsUrl.match(/[?&]body=([^&]*)/)[1];
  if (!bodyValue) {
    return true;
  }
  try {
    decodeURIComponent(bodyValue);
  } catch (error) {
    return false;
  }
  return /^(?:[a-z0-9\-_.~]|%[0-9a-f]{2})+$/i.test(bodyValue);
};
/**
 * @param {string} sshUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromSshUrl = function(sshUrl) {
  if (!goog.string.internal.caseInsensitiveStartsWith(sshUrl, "ssh://")) {
    sshUrl = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(sshUrl);
};
/**
 * @param {string} url
 * @param {(!goog.string.Const|!Array<!goog.string.Const>)} extensionId
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.sanitizeChromeExtensionUrl = function(url, extensionId) {
  return goog.html.SafeUrl.sanitizeExtensionUrl_(/^chrome-extension:\/\/([^\/]+)\//, url, extensionId);
};
/**
 * @param {string} url
 * @param {(!goog.string.Const|!Array<!goog.string.Const>)} extensionId
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.sanitizeFirefoxExtensionUrl = function(url, extensionId) {
  return goog.html.SafeUrl.sanitizeExtensionUrl_(/^moz-extension:\/\/([^\/]+)\//, url, extensionId);
};
/**
 * @param {string} url
 * @param {(!goog.string.Const|!Array<!goog.string.Const>)} extensionId
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.sanitizeEdgeExtensionUrl = function(url, extensionId) {
  return goog.html.SafeUrl.sanitizeExtensionUrl_(/^ms-browser-extension:\/\/([^\/]+)\//, url, extensionId);
};
/**
 * @private
 * @param {!RegExp} scheme
 * @param {string} url
 * @param {(!goog.string.Const|!Array<!goog.string.Const>)} extensionId
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.sanitizeExtensionUrl_ = function(scheme, url, extensionId) {
  var matches = scheme.exec(url);
  if (!matches) {
    url = goog.html.SafeUrl.INNOCUOUS_STRING;
  } else {
    var extractedExtensionId = matches[1];
    var acceptedExtensionIds;
    if (extensionId instanceof goog.string.Const) {
      acceptedExtensionIds = [goog.string.Const.unwrap(extensionId)];
    } else {
      acceptedExtensionIds = extensionId.map(function unwrap(x) {
        return goog.string.Const.unwrap(x);
      });
    }
    if (acceptedExtensionIds.indexOf(extractedExtensionId) == -1) {
      url = goog.html.SafeUrl.INNOCUOUS_STRING;
    }
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(url);
};
/**
 * @param {!goog.html.TrustedResourceUrl} trustedResourceUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.fromTrustedResourceUrl = function(trustedResourceUrl) {
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(goog.html.TrustedResourceUrl.unwrap(trustedResourceUrl));
};
/** @private @const @type {!RegExp} */ goog.html.SAFE_URL_PATTERN_ = /^(?:(?:https?|mailto|ftp):|[^:/?#]*(?:[/?#]|$))/i;
/** @const @type {!RegExp} */ goog.html.SafeUrl.SAFE_URL_PATTERN = goog.html.SAFE_URL_PATTERN_;
/**
 * @param {(string|!goog.string.TypedString)} url
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.sanitize = function(url) {
  if (url instanceof goog.html.SafeUrl) {
    return url;
  } else {
    if (typeof url == "object" && url.implementsGoogStringTypedString) {
      url = /** @type {!goog.string.TypedString} */ (url).getTypedStringValue();
    } else {
      url = String(url);
    }
  }
  if (!goog.html.SAFE_URL_PATTERN_.test(url)) {
    url = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(url);
};
/**
 * @param {(string|!goog.string.TypedString)} url
 * @param {boolean=} opt_allowDataUrl
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.sanitizeAssertUnchanged = function(url, opt_allowDataUrl) {
  if (url instanceof goog.html.SafeUrl) {
    return url;
  } else {
    if (typeof url == "object" && url.implementsGoogStringTypedString) {
      url = /** @type {!goog.string.TypedString} */ (url).getTypedStringValue();
    } else {
      url = String(url);
    }
  }
  if (opt_allowDataUrl && /^data:/i.test(url)) {
    var safeUrl = goog.html.SafeUrl.fromDataUrl(url);
    if (safeUrl.getTypedStringValue() == url) {
      return safeUrl;
    }
  }
  if (!goog.asserts.assert(goog.html.SAFE_URL_PATTERN_.test(url), "%s does not match the safe URL pattern", url)) {
    url = goog.html.SafeUrl.INNOCUOUS_STRING;
  }
  return goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse(url);
};
/** @private @const @type {!Object} */ goog.html.SafeUrl.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = {};
/**
 * @package
 * @param {string} url
 * @return {!goog.html.SafeUrl}
 */
goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse = function(url) {
  return new goog.html.SafeUrl(goog.html.SafeUrl.CONSTRUCTOR_TOKEN_PRIVATE_, url);
};
/** @const @type {!goog.html.SafeUrl} */ goog.html.SafeUrl.ABOUT_BLANK = goog.html.SafeUrl.createSafeUrlSecurityPrivateDoNotAccessOrElse("about:blank");
/** @private @const @type {!Object} */ goog.html.SafeUrl.CONSTRUCTOR_TOKEN_PRIVATE_ = {};

//# sourceMappingURL=goog.html.safeurl.js.map
