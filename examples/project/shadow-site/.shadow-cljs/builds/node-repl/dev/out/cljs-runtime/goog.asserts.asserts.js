goog.provide("goog.asserts");
goog.provide("goog.asserts.AssertionError");
goog.require("goog.debug.Error");
goog.require("goog.dom.NodeType");
/** @define {boolean} */ goog.asserts.ENABLE_ASSERTS = goog.define("goog.asserts.ENABLE_ASSERTS", goog.DEBUG);
/**
 * @final
 * @constructor
 * @extends {goog.debug.Error}
 * @param {string} messagePattern
 * @param {!Array<*>} messageArgs
 */
goog.asserts.AssertionError = function(messagePattern, messageArgs) {
  goog.debug.Error.call(this, goog.asserts.subs_(messagePattern, messageArgs));
  /** @type {string} */ this.messagePattern = messagePattern;
};
goog.inherits(goog.asserts.AssertionError, goog.debug.Error);
/** @override */ goog.asserts.AssertionError.prototype.name = "AssertionError";
/**
 * @param {!goog.asserts.AssertionError} e
 */
goog.asserts.DEFAULT_ERROR_HANDLER = function(e) {
  throw e;
};
/** @private @type {function(!goog.asserts.AssertionError)} */ goog.asserts.errorHandler_ = goog.asserts.DEFAULT_ERROR_HANDLER;
/**
 * @private
 * @param {string} pattern
 * @param {!Array<*>} subs
 * @return {string}
 */
goog.asserts.subs_ = function(pattern, subs) {
  var splitParts = pattern.split("%s");
  var returnString = "";
  var subLast = splitParts.length - 1;
  for (var i = 0; i < subLast; i++) {
    var sub = i < subs.length ? subs[i] : "%s";
    returnString += splitParts[i] + sub;
  }
  return returnString + splitParts[subLast];
};
/**
 * @private
 * @param {string} defaultMessage
 * @param {Array<*>} defaultArgs
 * @param {(string|undefined)} givenMessage
 * @param {Array<*>} givenArgs
 * @throws {goog.asserts.AssertionError}
 */
goog.asserts.doAssertFailure_ = function(defaultMessage, defaultArgs, givenMessage, givenArgs) {
  var message = "Assertion failed";
  if (givenMessage) {
    message += ": " + givenMessage;
    var args = givenArgs;
  } else {
    if (defaultMessage) {
      message += ": " + defaultMessage;
      args = defaultArgs;
    }
  }
  var e = new goog.asserts.AssertionError("" + message, args || []);
  goog.asserts.errorHandler_(e);
};
/**
 * @param {function(!goog.asserts.AssertionError)} errorHandler
 */
goog.asserts.setErrorHandler = function(errorHandler) {
  if (goog.asserts.ENABLE_ASSERTS) {
    goog.asserts.errorHandler_ = errorHandler;
  }
};
/**
 * @param {T} condition
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {T}
 * @throws {goog.asserts.AssertionError}
 * @template T
 * @closurePrimitive {asserts.truthy}
 */
goog.asserts.assert = function(condition, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && !condition) {
    goog.asserts.doAssertFailure_("", null, opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return condition;
};
/**
 * @param {T} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {R}
 * @throws {!goog.asserts.AssertionError}
 * @template T
 * @template R := mapunion(T,V=>cond(eq(V,"null"),none(),cond(eq(V,"undefined"),none(),V))) =:
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertExists = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && value == null) {
    goog.asserts.doAssertFailure_("Expected to exist: %s.", [value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return value;
};
/**
 * @param {string=} opt_message
 * @param {...*} var_args
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.fail}
 */
goog.asserts.fail = function(opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS) {
    goog.asserts.errorHandler_(new goog.asserts.AssertionError("Failure" + (opt_message ? ": " + opt_message : ""), Array.prototype.slice.call(arguments, 1)));
  }
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {number}
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertNumber = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && typeof value !== "number") {
    goog.asserts.doAssertFailure_("Expected number but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {number} */ (value));
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {string}
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertString = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && typeof value !== "string") {
    goog.asserts.doAssertFailure_("Expected string but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {string} */ (value));
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {!Function}
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertFunction = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && !goog.isFunction(value)) {
    goog.asserts.doAssertFailure_("Expected function but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {!Function} */ (value));
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {!Object}
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertObject = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && !goog.isObject(value)) {
    goog.asserts.doAssertFailure_("Expected object but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {!Object} */ (value));
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {!Array<?>}
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertArray = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && !goog.isArray(value)) {
    goog.asserts.doAssertFailure_("Expected array but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {!Array<?>} */ (value));
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {boolean}
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertBoolean = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && typeof value !== "boolean") {
    goog.asserts.doAssertFailure_("Expected boolean but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {boolean} */ (value));
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {!Element}
 * @throws {goog.asserts.AssertionError}
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertElement = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && (!goog.isObject(value) || value.nodeType != goog.dom.NodeType.ELEMENT)) {
    goog.asserts.doAssertFailure_("Expected Element but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {!Element} */ (value));
};
/**
 * @param {?} value
 * @param {function(new:T,...)} type
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {T}
 * @throws {goog.asserts.AssertionError}
 * @template T
 * @closurePrimitive {asserts.matchesReturn}
 */
goog.asserts.assertInstanceof = function(value, type, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && !(value instanceof type)) {
    goog.asserts.doAssertFailure_("Expected instanceof %s but got %s.", [goog.asserts.getType_(type), goog.asserts.getType_(value)], opt_message, Array.prototype.slice.call(arguments, 3));
  }
  return value;
};
/**
 * @param {*} value
 * @param {string=} opt_message
 * @param {...*} var_args
 * @return {number}
 * @throws {goog.asserts.AssertionError}
 */
goog.asserts.assertFinite = function(value, opt_message, var_args) {
  if (goog.asserts.ENABLE_ASSERTS && (typeof value != "number" || !isFinite(value))) {
    goog.asserts.doAssertFailure_("Expected %s to be a finite number but it is not.", [value], opt_message, Array.prototype.slice.call(arguments, 2));
  }
  return (/** @type {number} */ (value));
};
goog.asserts.assertObjectPrototypeIsIntact = function() {
  for (var key in Object.prototype) {
    goog.asserts.fail(key + " should not be enumerable in Object.prototype.");
  }
};
/**
 * @private
 * @param {*} value
 * @return {string}
 */
goog.asserts.getType_ = function(value) {
  if (value instanceof Function) {
    return value.displayName || value.name || "unknown type name";
  } else {
    if (value instanceof Object) {
      return /** @type {string} */ (value.constructor.displayName) || value.constructor.name || Object.prototype.toString.call(value);
    } else {
      return value === null ? "null" : typeof value;
    }
  }
};

//# sourceMappingURL=goog.asserts.asserts.js.map
