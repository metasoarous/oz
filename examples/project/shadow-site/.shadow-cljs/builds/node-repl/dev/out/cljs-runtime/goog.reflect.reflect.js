goog.provide("goog.reflect");
/**
 * @param {!Function} type
 * @param {Object} object
 * @return {Object}
 */
goog.reflect.object = function(type, object) {
  return object;
};
/**
 * @param {string} prop
 * @param {!Object} object
 * @return {string}
 */
goog.reflect.objectProperty = function(prop, object) {
  return prop;
};
/**
 * @param {T} x
 * @return {T}
 * @template T
 */
goog.reflect.sinkValue = function(x) {
  goog.reflect.sinkValue[" "](x);
  return x;
};
goog.reflect.sinkValue[" "] = goog.nullFunction;
/**
 * @param {Object} obj
 * @param {string} prop
 * @return {boolean}
 */
goog.reflect.canAccessProperty = function(obj, prop) {
  try {
    goog.reflect.sinkValue(obj[prop]);
    return true;
  } catch (e) {
  }
  return false;
};
/**
 * @param {!Object<K,V>} cacheObj
 * @param {?} key
 * @param {function(?):V} valueFn
 * @param {function(?):K=} opt_keyFn
 * @return {V}
 * @template K
 * @template V
 */
goog.reflect.cache = function(cacheObj, key, valueFn, opt_keyFn) {
  /** @const */ var storedKey = opt_keyFn ? opt_keyFn(key) : key;
  if (Object.prototype.hasOwnProperty.call(cacheObj, storedKey)) {
    return cacheObj[storedKey];
  }
  return cacheObj[storedKey] = valueFn(key);
};

//# sourceMappingURL=goog.reflect.reflect.js.map
