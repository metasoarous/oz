goog.provide("goog.object");
/**
 * @param {*} v
 * @param {*} v2
 * @return {boolean}
 */
goog.object.is = function(v, v2) {
  if (v === v2) {
    return v !== 0 || 1 / v === 1 / /** @type {?} */ (v2);
  }
  return v !== v && v2 !== v2;
};
/**
 * @param {Object<K,V>} obj
 * @param {function(this:T,V,?,Object<K,V>):?} f
 * @param {T=} opt_obj
 * @template T
 * @template K
 * @template V
 */
goog.object.forEach = function(obj, f, opt_obj) {
  for (var key in obj) {
    f.call(/** @type {?} */ (opt_obj), obj[key], key, obj);
  }
};
/**
 * @param {Object<K,V>} obj
 * @param {function(this:T,V,?,Object<K,V>):boolean} f
 * @param {T=} opt_obj
 * @return {!Object<K,V>}
 * @template T
 * @template K
 * @template V
 */
goog.object.filter = function(obj, f, opt_obj) {
  /** @const */ var res = {};
  for (var key in obj) {
    if (f.call(/** @type {?} */ (opt_obj), obj[key], key, obj)) {
      res[key] = obj[key];
    }
  }
  return res;
};
/**
 * @param {Object<K,V>} obj
 * @param {function(this:T,V,?,Object<K,V>):R} f
 * @param {T=} opt_obj
 * @return {!Object<K,R>}
 * @template T
 * @template K
 * @template V
 * @template R
 */
goog.object.map = function(obj, f, opt_obj) {
  /** @const */ var res = {};
  for (var key in obj) {
    res[key] = f.call(/** @type {?} */ (opt_obj), obj[key], key, obj);
  }
  return res;
};
/**
 * @param {Object<K,V>} obj
 * @param {function(this:T,V,?,Object<K,V>):boolean} f
 * @param {T=} opt_obj
 * @return {boolean}
 * @template T
 * @template K
 * @template V
 */
goog.object.some = function(obj, f, opt_obj) {
  for (var key in obj) {
    if (f.call(/** @type {?} */ (opt_obj), obj[key], key, obj)) {
      return true;
    }
  }
  return false;
};
/**
 * @param {Object<K,V>} obj
 * @param {?function(this:T,V,?,Object<K,V>):boolean} f
 * @param {T=} opt_obj
 * @return {boolean}
 * @template T
 * @template K
 * @template V
 */
goog.object.every = function(obj, f, opt_obj) {
  for (var key in obj) {
    if (!f.call(/** @type {?} */ (opt_obj), obj[key], key, obj)) {
      return false;
    }
  }
  return true;
};
/**
 * @param {Object} obj
 * @return {number}
 */
goog.object.getCount = function(obj) {
  var rv = 0;
  for (var key in obj) {
    rv++;
  }
  return rv;
};
/**
 * @param {Object} obj
 * @return {(string|undefined)}
 */
goog.object.getAnyKey = function(obj) {
  for (var key in obj) {
    return key;
  }
};
/**
 * @param {Object<K,V>} obj
 * @return {(V|undefined)}
 * @template K
 * @template V
 */
goog.object.getAnyValue = function(obj) {
  for (var key in obj) {
    return obj[key];
  }
};
/**
 * @param {Object<K,V>} obj
 * @param {V} val
 * @return {boolean}
 * @template K
 * @template V
 */
goog.object.contains = function(obj, val) {
  return goog.object.containsValue(obj, val);
};
/**
 * @param {Object<K,V>} obj
 * @return {!Array<V>}
 * @template K
 * @template V
 */
goog.object.getValues = function(obj) {
  /** @const */ var res = [];
  var i = 0;
  for (var key in obj) {
    res[i++] = obj[key];
  }
  return res;
};
/**
 * @param {Object} obj
 * @return {!Array<string>}
 */
goog.object.getKeys = function(obj) {
  /** @const */ var res = [];
  var i = 0;
  for (var key in obj) {
    res[i++] = key;
  }
  return res;
};
/**
 * @param {!Object} obj
 * @param {...(string|number|!IArrayLike<(number|string)>)} var_args
 * @return {*}
 */
goog.object.getValueByKeys = function(obj, var_args) {
  /** @const */ var isArrayLike = goog.isArrayLike(var_args);
  /** @const */ var keys = isArrayLike ? /** @type {!IArrayLike<(number|string)>} */ (var_args) : arguments;
  for (var i = isArrayLike ? 0 : 1; i < keys.length; i++) {
    if (obj == null) {
      return undefined;
    }
    obj = obj[keys[i]];
  }
  return obj;
};
/**
 * @param {Object} obj
 * @param {?} key
 * @return {boolean}
 */
goog.object.containsKey = function(obj, key) {
  return obj !== null && key in obj;
};
/**
 * @param {Object<K,V>} obj
 * @param {V} val
 * @return {boolean}
 * @template K
 * @template V
 */
goog.object.containsValue = function(obj, val) {
  for (var key in obj) {
    if (obj[key] == val) {
      return true;
    }
  }
  return false;
};
/**
 * @param {Object<K,V>} obj
 * @param {function(this:T,V,string,Object<K,V>):boolean} f
 * @param {T=} opt_this
 * @return {(string|undefined)}
 * @template T
 * @template K
 * @template V
 */
goog.object.findKey = function(obj, f, opt_this) {
  for (var key in obj) {
    if (f.call(/** @type {?} */ (opt_this), obj[key], key, obj)) {
      return key;
    }
  }
  return undefined;
};
/**
 * @param {Object<K,V>} obj
 * @param {function(this:T,V,string,Object<K,V>):boolean} f
 * @param {T=} opt_this
 * @return {V}
 * @template T
 * @template K
 * @template V
 */
goog.object.findValue = function(obj, f, opt_this) {
  /** @const */ var key = goog.object.findKey(obj, f, opt_this);
  return key && obj[key];
};
/**
 * @param {Object} obj
 * @return {boolean}
 */
goog.object.isEmpty = function(obj) {
  for (var key in obj) {
    return false;
  }
  return true;
};
/**
 * @param {Object} obj
 */
goog.object.clear = function(obj) {
  for (var i in obj) {
    delete obj[i];
  }
};
/**
 * @param {Object} obj
 * @param {?} key
 * @return {boolean}
 */
goog.object.remove = function(obj, key) {
  var rv;
  if (rv = key in /** @type {!Object} */ (obj)) {
    delete obj[key];
  }
  return rv;
};
/**
 * @param {Object<K,V>} obj
 * @param {string} key
 * @param {V} val
 * @template K
 * @template V
 */
goog.object.add = function(obj, key, val) {
  if (obj !== null && key in obj) {
    throw new Error('The object already contains the key "' + key + '"');
  }
  goog.object.set(obj, key, val);
};
/**
 * @param {Object<K,V>} obj
 * @param {string} key
 * @param {R=} opt_val
 * @return {(V|R|undefined)}
 * @template K
 * @template V
 * @template R
 */
goog.object.get = function(obj, key, opt_val) {
  if (obj !== null && key in obj) {
    return obj[key];
  }
  return opt_val;
};
/**
 * @param {Object<K,V>} obj
 * @param {string} key
 * @param {V} value
 * @template K
 * @template V
 */
goog.object.set = function(obj, key, value) {
  obj[key] = value;
};
/**
 * @param {Object<K,V>} obj
 * @param {string} key
 * @param {V} value
 * @return {V}
 * @template K
 * @template V
 */
goog.object.setIfUndefined = function(obj, key, value) {
  return key in /** @type {!Object} */ (obj) ? obj[key] : obj[key] = value;
};
/**
 * @param {!Object<K,V>} obj
 * @param {string} key
 * @param {function():V} f
 * @return {V}
 * @template K
 * @template V
 */
goog.object.setWithReturnValueIfNotSet = function(obj, key, f) {
  if (key in obj) {
    return obj[key];
  }
  /** @const */ var val = f();
  obj[key] = val;
  return val;
};
/**
 * @param {!Object<K,V>} a
 * @param {!Object<K,V>} b
 * @return {boolean}
 * @template K
 * @template V
 */
goog.object.equals = function(a, b) {
  for (var k in a) {
    if (!(k in b) || a[k] !== b[k]) {
      return false;
    }
  }
  for (var k$1 in b) {
    if (!(k$1 in a)) {
      return false;
    }
  }
  return true;
};
/**
 * @param {Object<K,V>} obj
 * @return {!Object<K,V>}
 * @template K
 * @template V
 */
goog.object.clone = function(obj) {
  /** @const */ var res = {};
  for (var key in obj) {
    res[key] = obj[key];
  }
  return res;
};
/**
 * @param {T} obj
 * @return {T}
 * @template T
 */
goog.object.unsafeClone = function(obj) {
  /** @const */ var type = goog.typeOf(obj);
  if (type == "object" || type == "array") {
    if (goog.isFunction(obj.clone)) {
      return obj.clone();
    }
    /** @const */ var clone = type == "array" ? [] : {};
    for (var key in obj) {
      clone[key] = goog.object.unsafeClone(obj[key]);
    }
    return clone;
  }
  return obj;
};
/**
 * @param {Object} obj
 * @return {!Object}
 */
goog.object.transpose = function(obj) {
  /** @const */ var transposed = {};
  for (var key in obj) {
    transposed[obj[key]] = key;
  }
  return transposed;
};
/** @private @type {Array<string>} */ goog.object.PROTOTYPE_FIELDS_ = ["constructor", "hasOwnProperty", "isPrototypeOf", "propertyIsEnumerable", "toLocaleString", "toString", "valueOf"];
/**
 * @param {Object} target
 * @param {...(Object|null|undefined)} var_args
 * @deprecated Prefer Object.assign
 */
goog.object.extend = function(target, var_args) {
  var key;
  var source;
  for (var i = 1; i < arguments.length; i++) {
    source = arguments[i];
    for (key in source) {
      target[key] = source[key];
    }
    for (var j = 0; j < goog.object.PROTOTYPE_FIELDS_.length; j++) {
      key = goog.object.PROTOTYPE_FIELDS_[j];
      if (Object.prototype.hasOwnProperty.call(source, key)) {
        target[key] = source[key];
      }
    }
  }
};
/**
 * @param {...*} var_args
 * @return {!Object}
 * @throws {Error}
 */
goog.object.create = function(var_args) {
  /** @const */ var argLength = arguments.length;
  if (argLength == 1 && goog.isArray(arguments[0])) {
    return goog.object.create.apply(null, arguments[0]);
  }
  if (argLength % 2) {
    throw new Error("Uneven number of arguments");
  }
  /** @const */ var rv = {};
  for (var i = 0; i < argLength; i += 2) {
    rv[arguments[i]] = arguments[i + 1];
  }
  return rv;
};
/**
 * @param {...*} var_args
 * @return {!Object}
 */
goog.object.createSet = function(var_args) {
  /** @const */ var argLength = arguments.length;
  if (argLength == 1 && goog.isArray(arguments[0])) {
    return goog.object.createSet.apply(null, arguments[0]);
  }
  /** @const */ var rv = {};
  for (var i = 0; i < argLength; i++) {
    rv[arguments[i]] = true;
  }
  return rv;
};
/**
 * @param {!Object<K,V>} obj
 * @return {!Object<K,V>}
 * @template K
 * @template V
 */
goog.object.createImmutableView = function(obj) {
  var result = obj;
  if (Object.isFrozen && !Object.isFrozen(obj)) {
    result = Object.create(obj);
    Object.freeze(result);
  }
  return result;
};
/**
 * @param {!Object} obj
 * @return {boolean}
 */
goog.object.isImmutableView = function(obj) {
  return !!Object.isFrozen && Object.isFrozen(obj);
};
/**
 * @public
 * @param {?Object} obj
 * @param {boolean=} opt_includeObjectPrototype
 * @param {boolean=} opt_includeFunctionPrototype
 * @return {!Array<string>}
 */
goog.object.getAllPropertyNames = function(obj, opt_includeObjectPrototype, opt_includeFunctionPrototype) {
  if (!obj) {
    return [];
  }
  if (!Object.getOwnPropertyNames || !Object.getPrototypeOf) {
    return goog.object.getKeys(obj);
  }
  /** @const */ var visitedSet = {};
  var proto = obj;
  while (proto && (proto !== Object.prototype || !!opt_includeObjectPrototype) && (proto !== Function.prototype || !!opt_includeFunctionPrototype)) {
    /** @const */ var names = Object.getOwnPropertyNames(proto);
    for (var i = 0; i < names.length; i++) {
      visitedSet[names[i]] = true;
    }
    proto = Object.getPrototypeOf(proto);
  }
  return goog.object.getKeys(visitedSet);
};
/**
 * @param {function(new:?)} constructor
 * @return {?Object}
 */
goog.object.getSuperClass = function(constructor) {
  var proto = Object.getPrototypeOf(constructor.prototype);
  return proto && proto.constructor;
};

//# sourceMappingURL=goog.object.object.js.map
