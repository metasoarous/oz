goog.provide("goog.html.SafeScript");
goog.require("goog.asserts");
goog.require("goog.html.trustedtypes");
goog.require("goog.string.Const");
goog.require("goog.string.TypedString");
/**
 * @final
 * @struct
 * @constructor
 * @implements {goog.string.TypedString}
 */
goog.html.SafeScript = function() {
  /** @private @type {(!TrustedScript|string)} */ this.privateDoNotAccessOrElseSafeScriptWrappedValue_ = "";
  /** @private @const @type {!Object} */ this.SAFE_SCRIPT_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = goog.html.SafeScript.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_;
};
/** @const @override */ goog.html.SafeScript.prototype.implementsGoogStringTypedString = true;
/** @private @const @type {!Object} */ goog.html.SafeScript.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = {};
/**
 * @param {!goog.string.Const} script
 * @return {!goog.html.SafeScript}
 */
goog.html.SafeScript.fromConstant = function(script) {
  var scriptString = goog.string.Const.unwrap(script);
  if (scriptString.length === 0) {
    return goog.html.SafeScript.EMPTY;
  }
  return goog.html.SafeScript.createSafeScriptSecurityPrivateDoNotAccessOrElse(scriptString);
};
/**
 * @param {!goog.string.Const} code
 * @param {...*} var_args
 * @return {!goog.html.SafeScript}
 */
goog.html.SafeScript.fromConstantAndArgs = function(code, var_args) {
  var args = [];
  for (var i = 1; i < arguments.length; i++) {
    args.push(goog.html.SafeScript.stringify_(arguments[i]));
  }
  return goog.html.SafeScript.createSafeScriptSecurityPrivateDoNotAccessOrElse("(" + goog.string.Const.unwrap(code) + ")(" + args.join(", ") + ");");
};
/**
 * @param {*} val
 * @return {!goog.html.SafeScript}
 */
goog.html.SafeScript.fromJson = function(val) {
  return goog.html.SafeScript.createSafeScriptSecurityPrivateDoNotAccessOrElse(goog.html.SafeScript.stringify_(val));
};
/** @override */ goog.html.SafeScript.prototype.getTypedStringValue = function() {
  return this.privateDoNotAccessOrElseSafeScriptWrappedValue_.toString();
};
if (goog.DEBUG) {
  /** @override */ goog.html.SafeScript.prototype.toString = function() {
    return "SafeScript{" + this.privateDoNotAccessOrElseSafeScriptWrappedValue_ + "}";
  };
}
/**
 * @param {!goog.html.SafeScript} safeScript
 * @return {string}
 */
goog.html.SafeScript.unwrap = function(safeScript) {
  return goog.html.SafeScript.unwrapTrustedScript(safeScript).toString();
};
/**
 * @param {!goog.html.SafeScript} safeScript
 * @return {(!TrustedScript|string)}
 */
goog.html.SafeScript.unwrapTrustedScript = function(safeScript) {
  if (safeScript instanceof goog.html.SafeScript && safeScript.constructor === goog.html.SafeScript && safeScript.SAFE_SCRIPT_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ === goog.html.SafeScript.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_) {
    return safeScript.privateDoNotAccessOrElseSafeScriptWrappedValue_;
  } else {
    goog.asserts.fail("expected object of type SafeScript, got '" + safeScript + "' of type " + goog.typeOf(safeScript));
    return "type_error:SafeScript";
  }
};
/**
 * @private
 * @param {*} val
 * @return {string}
 */
goog.html.SafeScript.stringify_ = function(val) {
  var json = JSON.stringify(val);
  return json.replace(/</g, "\\x3c");
};
/**
 * @package
 * @param {string} script
 * @return {!goog.html.SafeScript}
 */
goog.html.SafeScript.createSafeScriptSecurityPrivateDoNotAccessOrElse = function(script) {
  return (new goog.html.SafeScript).initSecurityPrivateDoNotAccessOrElse_(script);
};
/**
 * @private
 * @param {string} script
 * @return {!goog.html.SafeScript}
 */
goog.html.SafeScript.prototype.initSecurityPrivateDoNotAccessOrElse_ = function(script) {
  this.privateDoNotAccessOrElseSafeScriptWrappedValue_ = goog.html.trustedtypes.PRIVATE_DO_NOT_ACCESS_OR_ELSE_POLICY ? goog.html.trustedtypes.PRIVATE_DO_NOT_ACCESS_OR_ELSE_POLICY.createScript(script) : script;
  return this;
};
/** @const @type {!goog.html.SafeScript} */ goog.html.SafeScript.EMPTY = goog.html.SafeScript.createSafeScriptSecurityPrivateDoNotAccessOrElse("");

//# sourceMappingURL=goog.html.safescript.js.map
