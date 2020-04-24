/**
 * @suppress {strictMissingProperties}
 */
goog.provide("goog.structs");
goog.require("goog.array");
goog.require("goog.object");
/**
 * @param {Object} col
 * @return {number}
 */
goog.structs.getCount = function(col) {
  if (col.getCount && typeof col.getCount == "function") {
    return col.getCount();
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    return col.length;
  }
  return goog.object.getCount(col);
};
/**
 * @param {Object} col
 * @return {!Array<?>}
 */
goog.structs.getValues = function(col) {
  if (col.getValues && typeof col.getValues == "function") {
    return col.getValues();
  }
  if (typeof col === "string") {
    return col.split("");
  }
  if (goog.isArrayLike(col)) {
    var rv = [];
    var l = col.length;
    for (var i = 0; i < l; i++) {
      rv.push(col[i]);
    }
    return rv;
  }
  return goog.object.getValues(col);
};
/**
 * @param {Object} col
 * @return {(!Array|undefined)}
 */
goog.structs.getKeys = function(col) {
  if (col.getKeys && typeof col.getKeys == "function") {
    return col.getKeys();
  }
  if (col.getValues && typeof col.getValues == "function") {
    return undefined;
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    var rv = [];
    var l = col.length;
    for (var i = 0; i < l; i++) {
      rv.push(i);
    }
    return rv;
  }
  return goog.object.getKeys(col);
};
/**
 * @param {Object} col
 * @param {*} val
 * @return {boolean}
 */
goog.structs.contains = function(col, val) {
  if (col.contains && typeof col.contains == "function") {
    return col.contains(val);
  }
  if (col.containsValue && typeof col.containsValue == "function") {
    return col.containsValue(val);
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    return goog.array.contains(/** @type {!Array<?>} */ (col), val);
  }
  return goog.object.containsValue(col, val);
};
/**
 * @param {Object} col
 * @return {boolean}
 */
goog.structs.isEmpty = function(col) {
  if (col.isEmpty && typeof col.isEmpty == "function") {
    return col.isEmpty();
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    return goog.array.isEmpty(/** @type {!Array<?>} */ (col));
  }
  return goog.object.isEmpty(col);
};
/**
 * @param {Object} col
 */
goog.structs.clear = function(col) {
  if (col.clear && typeof col.clear == "function") {
    col.clear();
  } else {
    if (goog.isArrayLike(col)) {
      goog.array.clear(/** @type {IArrayLike<?>} */ (col));
    } else {
      goog.object.clear(col);
    }
  }
};
/**
 * @param {S} col
 * @param {function(this:T,?,?,S):?} f
 * @param {T=} opt_obj
 * @template T
 * @template S
 * @deprecated Use a more specific method, e.g. goog.array.forEach, goog.object.forEach, or for-of.
 */
goog.structs.forEach = function(col, f, opt_obj) {
  if (col.forEach && typeof col.forEach == "function") {
    col.forEach(f, opt_obj);
  } else {
    if (goog.isArrayLike(col) || typeof col === "string") {
      goog.array.forEach(/** @type {!Array<?>} */ (col), f, opt_obj);
    } else {
      var keys = goog.structs.getKeys(col);
      var values = goog.structs.getValues(col);
      var l = values.length;
      for (var i = 0; i < l; i++) {
        f.call(/** @type {?} */ (opt_obj), values[i], keys && keys[i], col);
      }
    }
  }
};
/**
 * @param {S} col
 * @param {function(this:T,?,?,S):boolean} f
 * @param {T=} opt_obj
 * @return {(!Object|!Array<?>)}
 * @template T
 * @template S
 */
goog.structs.filter = function(col, f, opt_obj) {
  if (typeof col.filter == "function") {
    return col.filter(f, opt_obj);
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    return goog.array.filter(/** @type {!Array<?>} */ (col), f, opt_obj);
  }
  var rv;
  var keys = goog.structs.getKeys(col);
  var values = goog.structs.getValues(col);
  var l = values.length;
  if (keys) {
    rv = {};
    for (var i = 0; i < l; i++) {
      if (f.call(/** @type {?} */ (opt_obj), values[i], keys[i], col)) {
        rv[keys[i]] = values[i];
      }
    }
  } else {
    rv = [];
    for (var i = 0; i < l; i++) {
      if (f.call(opt_obj, values[i], undefined, col)) {
        rv.push(values[i]);
      }
    }
  }
  return rv;
};
/**
 * @param {S} col
 * @param {function(this:T,?,?,S):V} f
 * @param {T=} opt_obj
 * @return {(!Object<?,V>|!Array<V>)}
 * @template T
 * @template S
 * @template V
 */
goog.structs.map = function(col, f, opt_obj) {
  if (typeof col.map == "function") {
    return col.map(f, opt_obj);
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    return goog.array.map(/** @type {!Array<?>} */ (col), f, opt_obj);
  }
  var rv;
  var keys = goog.structs.getKeys(col);
  var values = goog.structs.getValues(col);
  var l = values.length;
  if (keys) {
    rv = {};
    for (var i = 0; i < l; i++) {
      rv[keys[i]] = f.call(/** @type {?} */ (opt_obj), values[i], keys[i], col);
    }
  } else {
    rv = [];
    for (var i = 0; i < l; i++) {
      rv[i] = f.call(/** @type {?} */ (opt_obj), values[i], undefined, col);
    }
  }
  return rv;
};
/**
 * @param {S} col
 * @param {function(this:T,?,?,S):boolean} f
 * @param {T=} opt_obj
 * @return {boolean}
 * @template T
 * @template S
 */
goog.structs.some = function(col, f, opt_obj) {
  if (typeof col.some == "function") {
    return col.some(f, opt_obj);
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    return goog.array.some(/** @type {!Array<?>} */ (col), f, opt_obj);
  }
  var keys = goog.structs.getKeys(col);
  var values = goog.structs.getValues(col);
  var l = values.length;
  for (var i = 0; i < l; i++) {
    if (f.call(/** @type {?} */ (opt_obj), values[i], keys && keys[i], col)) {
      return true;
    }
  }
  return false;
};
/**
 * @param {S} col
 * @param {function(this:T,?,?,S):boolean} f
 * @param {T=} opt_obj
 * @return {boolean}
 * @template T
 * @template S
 */
goog.structs.every = function(col, f, opt_obj) {
  if (typeof col.every == "function") {
    return col.every(f, opt_obj);
  }
  if (goog.isArrayLike(col) || typeof col === "string") {
    return goog.array.every(/** @type {!Array<?>} */ (col), f, opt_obj);
  }
  var keys = goog.structs.getKeys(col);
  var values = goog.structs.getValues(col);
  var l = values.length;
  for (var i = 0; i < l; i++) {
    if (!f.call(/** @type {?} */ (opt_obj), values[i], keys && keys[i], col)) {
      return false;
    }
  }
  return true;
};

//# sourceMappingURL=goog.structs.structs.js.map
