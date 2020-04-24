goog.provide("goog.functions");
/**
 * @param {T} retValue
 * @return {function():T}
 * @template T
 */
goog.functions.constant = function(retValue) {
  return function() {
    return retValue;
  };
};
/** @type {function(...):boolean} */ goog.functions.FALSE = function() {
  return false;
};
/** @type {function(...):boolean} */ goog.functions.TRUE = function() {
  return true;
};
/** @type {function(...):null} */ goog.functions.NULL = function() {
  return null;
};
/**
 * @param {T=} opt_returnValue
 * @param {...*} var_args
 * @return {T}
 * @template T
 */
goog.functions.identity = function(opt_returnValue, var_args) {
  return opt_returnValue;
};
/**
 * @param {string} message
 * @return {!Function}
 */
goog.functions.error = function(message) {
  return function() {
    throw new Error(message);
  };
};
/**
 * @param {*} err
 * @return {!Function}
 */
goog.functions.fail = function(err) {
  return function() {
    throw err;
  };
};
/**
 * @param {Function} f
 * @param {number=} opt_numArgs
 * @return {!Function}
 */
goog.functions.lock = function(f, opt_numArgs) {
  opt_numArgs = opt_numArgs || 0;
  return function() {
    /** @const */ var self = /** @type {*} */ (this);
    return f.apply(self, Array.prototype.slice.call(arguments, 0, opt_numArgs));
  };
};
/**
 * @param {number} n
 * @return {!Function}
 */
goog.functions.nth = function(n) {
  return function() {
    return arguments[n];
  };
};
/**
 * @param {!Function} fn
 * @param {...*} var_args
 * @return {!Function}
 */
goog.functions.partialRight = function(fn, var_args) {
  /** @const */ var rightArgs = Array.prototype.slice.call(arguments, 1);
  return function() {
    /** @const */ var self = /** @type {*} */ (this);
    /** @const */ var newArgs = Array.prototype.slice.call(arguments);
    newArgs.push.apply(newArgs, rightArgs);
    return fn.apply(self, newArgs);
  };
};
/**
 * @param {Function} f
 * @param {T} retValue
 * @return {function(...?):T}
 * @template T
 */
goog.functions.withReturnValue = function(f, retValue) {
  return goog.functions.sequence(f, goog.functions.constant(retValue));
};
/**
 * @param {*} value
 * @param {boolean=} opt_useLooseComparison
 * @return {function(*):boolean}
 */
goog.functions.equalTo = function(value, opt_useLooseComparison) {
  return function(other) {
    return opt_useLooseComparison ? value == other : value === other;
  };
};
/**
 * @param {function(...?):T} fn
 * @param {...Function} var_args
 * @return {function(...?):T}
 * @template T
 */
goog.functions.compose = function(fn, var_args) {
  /** @const */ var functions = arguments;
  /** @const */ var length = functions.length;
  return function() {
    /** @const */ var self = /** @type {*} */ (this);
    var result;
    if (length) {
      result = functions[length - 1].apply(self, arguments);
    }
    for (var i = length - 2; i >= 0; i--) {
      result = functions[i].call(self, result);
    }
    return result;
  };
};
/**
 * @param {...Function} var_args
 * @return {!Function}
 */
goog.functions.sequence = function(var_args) {
  /** @const */ var functions = arguments;
  /** @const */ var length = functions.length;
  return function() {
    /** @const */ var self = /** @type {*} */ (this);
    var result;
    for (var i = 0; i < length; i++) {
      result = functions[i].apply(self, arguments);
    }
    return result;
  };
};
/**
 * @param {...Function} var_args
 * @return {function(...?):boolean}
 */
goog.functions.and = function(var_args) {
  /** @const */ var functions = arguments;
  /** @const */ var length = functions.length;
  return function() {
    /** @const */ var self = /** @type {*} */ (this);
    for (var i = 0; i < length; i++) {
      if (!functions[i].apply(self, arguments)) {
        return false;
      }
    }
    return true;
  };
};
/**
 * @param {...Function} var_args
 * @return {function(...?):boolean}
 */
goog.functions.or = function(var_args) {
  /** @const */ var functions = arguments;
  /** @const */ var length = functions.length;
  return function() {
    /** @const */ var self = /** @type {*} */ (this);
    for (var i = 0; i < length; i++) {
      if (functions[i].apply(self, arguments)) {
        return true;
      }
    }
    return false;
  };
};
/**
 * @param {!Function} f
 * @return {function(...?):boolean}
 */
goog.functions.not = function(f) {
  return function() {
    /** @const */ var self = /** @type {*} */ (this);
    return !f.apply(self, arguments);
  };
};
/**
 * @param {function(new:T,...)} constructor
 * @param {...*} var_args
 * @return {T}
 * @template T
 */
goog.functions.create = function(constructor, var_args) {
  /** @const @final @constructor */ var temp = function() {
  };
  temp.prototype = constructor.prototype;
  /** @const */ var obj = new temp;
  constructor.apply(obj, Array.prototype.slice.call(arguments, 1));
  return obj;
};
/** @define {boolean} */ goog.functions.CACHE_RETURN_VALUE = goog.define("goog.functions.CACHE_RETURN_VALUE", true);
/**
 * @param {function():T} fn
 * @return {function():T}
 * @template T
 */
goog.functions.cacheReturnValue = function(fn) {
  var called = false;
  var value;
  return function() {
    if (!goog.functions.CACHE_RETURN_VALUE) {
      return fn();
    }
    if (!called) {
      value = fn();
      called = true;
    }
    return value;
  };
};
/**
 * @param {function():*} f
 * @return {function():undefined}
 */
goog.functions.once = function(f) {
  var inner = f;
  return function() {
    if (inner) {
      /** @const */ var tmp = inner;
      inner = null;
      tmp();
    }
  };
};
/**
 * @param {function(this:SCOPE,...?)} f
 * @param {number} interval
 * @param {SCOPE=} opt_scope
 * @return {function(...?):undefined}
 * @template SCOPE
 */
goog.functions.debounce = function(f, interval, opt_scope) {
  var timeout = 0;
  return (/** @type {function(...?)} */ (function(var_args) {
    goog.global.clearTimeout(timeout);
    /** @const */ var args = arguments;
    timeout = goog.global.setTimeout(function() {
      f.apply(opt_scope, args);
    }, interval);
  }));
};
/**
 * @param {function(this:SCOPE,...?)} f
 * @param {number} interval
 * @param {SCOPE=} opt_scope
 * @return {function(...?):undefined}
 * @template SCOPE
 */
goog.functions.throttle = function(f, interval, opt_scope) {
  var timeout = 0;
  var shouldFire = false;
  var args = [];
  /** @const */ var handleTimeout = function() {
    timeout = 0;
    if (shouldFire) {
      shouldFire = false;
      fire();
    }
  };
  /** @const */ var fire = function() {
    timeout = goog.global.setTimeout(handleTimeout, interval);
    f.apply(opt_scope, args);
  };
  return (/** @type {function(...?)} */ (function(var_args) {
    args = arguments;
    if (!timeout) {
      fire();
    } else {
      shouldFire = true;
    }
  }));
};
/**
 * @param {function(this:SCOPE,...?)} f
 * @param {number} interval
 * @param {SCOPE=} opt_scope
 * @return {function(...?):undefined}
 * @template SCOPE
 */
goog.functions.rateLimit = function(f, interval, opt_scope) {
  var timeout = 0;
  /** @const */ var handleTimeout = function() {
    timeout = 0;
  };
  return (/** @type {function(...?)} */ (function(var_args) {
    if (!timeout) {
      timeout = goog.global.setTimeout(handleTimeout, interval);
      f.apply(opt_scope, arguments);
    }
  }));
};

//# sourceMappingURL=goog.functions.functions.js.map
