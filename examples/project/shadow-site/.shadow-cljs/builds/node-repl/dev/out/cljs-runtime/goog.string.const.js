goog.provide("goog.string.Const");
goog.require("goog.asserts");
goog.require("goog.string.TypedString");
/**
 * @final
 * @struct
 * @constructor
 * @implements {goog.string.TypedString}
 * @param {Object=} opt_token
 * @param {string=} opt_content
 */
goog.string.Const = function(opt_token, opt_content) {
  /** @private @type {string} */ this.stringConstValueWithSecurityContract__googStringSecurityPrivate_ = opt_token === goog.string.Const.GOOG_STRING_CONSTRUCTOR_TOKEN_PRIVATE_ && opt_content || "";
  /** @private @const @type {!Object} */ this.STRING_CONST_TYPE_MARKER__GOOG_STRING_SECURITY_PRIVATE_ = goog.string.Const.TYPE_MARKER_;
};
/** @const @override */ goog.string.Const.prototype.implementsGoogStringTypedString = true;
/** @override */ goog.string.Const.prototype.getTypedStringValue = function() {
  return this.stringConstValueWithSecurityContract__googStringSecurityPrivate_;
};
if (goog.DEBUG) {
  /** @override */ goog.string.Const.prototype.toString = function() {
    return "Const{" + this.stringConstValueWithSecurityContract__googStringSecurityPrivate_ + "}";
  };
}
/**
 * @param {!goog.string.Const} stringConst
 * @return {string}
 */
goog.string.Const.unwrap = function(stringConst) {
  if (stringConst instanceof goog.string.Const && stringConst.constructor === goog.string.Const && stringConst.STRING_CONST_TYPE_MARKER__GOOG_STRING_SECURITY_PRIVATE_ === goog.string.Const.TYPE_MARKER_) {
    return stringConst.stringConstValueWithSecurityContract__googStringSecurityPrivate_;
  } else {
    goog.asserts.fail("expected object of type Const, got '" + stringConst + "'");
    return "type_error:Const";
  }
};
/**
 * @param {string} s
 * @return {!goog.string.Const}
 */
goog.string.Const.from = function(s) {
  return new goog.string.Const(goog.string.Const.GOOG_STRING_CONSTRUCTOR_TOKEN_PRIVATE_, s);
};
/** @private @const @type {!Object} */ goog.string.Const.TYPE_MARKER_ = {};
/** @private @const @type {!Object} */ goog.string.Const.GOOG_STRING_CONSTRUCTOR_TOKEN_PRIVATE_ = {};
/** @const @type {!goog.string.Const} */ goog.string.Const.EMPTY = goog.string.Const.from("");

//# sourceMappingURL=goog.string.const.js.map
