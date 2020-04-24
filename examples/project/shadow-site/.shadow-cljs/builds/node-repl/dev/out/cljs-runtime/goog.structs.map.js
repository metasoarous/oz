goog.provide("goog.structs.Map");
goog.require("goog.iter.Iterator");
goog.require("goog.iter.StopIteration");
/**
 * @constructor
 * @param {*=} opt_map
 * @param {...*} var_args
 * @template K
 * @template V
 * @deprecated This type is misleading: use ES6 Map instead.
 */
goog.structs.Map = function(opt_map, var_args) {
  /** @private @type {!Object} */ this.map_ = {};
  /** @private @type {!Array<string>} */ this.keys_ = [];
  /** @private @type {number} */ this.count_ = 0;
  /** @private @type {number} */ this.version_ = 0;
  var argLength = arguments.length;
  if (argLength > 1) {
    if (argLength % 2) {
      throw new Error("Uneven number of arguments");
    }
    for (var i = 0; i < argLength; i += 2) {
      this.set(arguments[i], arguments[i + 1]);
    }
  } else {
    if (opt_map) {
      this.addAll(/** @type {!Object} */ (opt_map));
    }
  }
};
/**
 * @return {number}
 */
goog.structs.Map.prototype.getCount = function() {
  return this.count_;
};
/**
 * @return {!Array<V>}
 */
goog.structs.Map.prototype.getValues = function() {
  this.cleanupKeysArray_();
  var rv = [];
  for (var i = 0; i < this.keys_.length; i++) {
    var key = this.keys_[i];
    rv.push(this.map_[key]);
  }
  return rv;
};
/**
 * @return {!Array<string>}
 */
goog.structs.Map.prototype.getKeys = function() {
  this.cleanupKeysArray_();
  return (/** @type {!Array<string>} */ (this.keys_.concat()));
};
/**
 * @param {*} key
 * @return {boolean}
 */
goog.structs.Map.prototype.containsKey = function(key) {
  return goog.structs.Map.hasKey_(this.map_, key);
};
/**
 * @param {V} val
 * @return {boolean}
 */
goog.structs.Map.prototype.containsValue = function(val) {
  for (var i = 0; i < this.keys_.length; i++) {
    var key = this.keys_[i];
    if (goog.structs.Map.hasKey_(this.map_, key) && this.map_[key] == val) {
      return true;
    }
  }
  return false;
};
/**
 * @param {goog.structs.Map} otherMap
 * @param {function(V,V):boolean=} opt_equalityFn
 * @return {boolean}
 */
goog.structs.Map.prototype.equals = function(otherMap, opt_equalityFn) {
  if (this === otherMap) {
    return true;
  }
  if (this.count_ != otherMap.getCount()) {
    return false;
  }
  var equalityFn = opt_equalityFn || goog.structs.Map.defaultEquals;
  this.cleanupKeysArray_();
  for (var key, i = 0; key = this.keys_[i]; i++) {
    if (!equalityFn(this.get(key), otherMap.get(key))) {
      return false;
    }
  }
  return true;
};
/**
 * @param {*} a
 * @param {*} b
 * @return {boolean}
 */
goog.structs.Map.defaultEquals = function(a, b) {
  return a === b;
};
/**
 * @return {boolean}
 */
goog.structs.Map.prototype.isEmpty = function() {
  return this.count_ == 0;
};
goog.structs.Map.prototype.clear = function() {
  this.map_ = {};
  this.keys_.length = 0;
  this.count_ = 0;
  this.version_ = 0;
};
/**
 * @param {*} key
 * @return {boolean}
 */
goog.structs.Map.prototype.remove = function(key) {
  if (goog.structs.Map.hasKey_(this.map_, key)) {
    delete this.map_[key];
    this.count_--;
    this.version_++;
    if (this.keys_.length > 2 * this.count_) {
      this.cleanupKeysArray_();
    }
    return true;
  }
  return false;
};
/** @private */ goog.structs.Map.prototype.cleanupKeysArray_ = function() {
  if (this.count_ != this.keys_.length) {
    var srcIndex = 0;
    var destIndex = 0;
    while (srcIndex < this.keys_.length) {
      var key = this.keys_[srcIndex];
      if (goog.structs.Map.hasKey_(this.map_, key)) {
        this.keys_[destIndex++] = key;
      }
      srcIndex++;
    }
    this.keys_.length = destIndex;
  }
  if (this.count_ != this.keys_.length) {
    var seen = {};
    var srcIndex = 0;
    var destIndex = 0;
    while (srcIndex < this.keys_.length) {
      var key = this.keys_[srcIndex];
      if (!goog.structs.Map.hasKey_(seen, key)) {
        this.keys_[destIndex++] = key;
        seen[key] = 1;
      }
      srcIndex++;
    }
    this.keys_.length = destIndex;
  }
};
/**
 * @param {*} key
 * @param {DEFAULT=} opt_val
 * @return {(V|DEFAULT)}
 * @template DEFAULT
 */
goog.structs.Map.prototype.get = function(key, opt_val) {
  if (goog.structs.Map.hasKey_(this.map_, key)) {
    return this.map_[key];
  }
  return opt_val;
};
/**
 * @param {*} key
 * @param {V} value
 * @return {*}
 */
goog.structs.Map.prototype.set = function(key, value) {
  if (!goog.structs.Map.hasKey_(this.map_, key)) {
    this.count_++;
    this.keys_.push(/** @type {?} */ (key));
    this.version_++;
  }
  this.map_[key] = value;
};
/**
 * @param {?Object} map
 */
goog.structs.Map.prototype.addAll = function(map) {
  if (map instanceof goog.structs.Map) {
    var keys = map.getKeys();
    for (var i = 0; i < keys.length; i++) {
      this.set(keys[i], map.get(keys[i]));
    }
  } else {
    for (var key in map) {
      this.set(key, map[key]);
    }
  }
};
/**
 * @param {function(this:T,V,K,goog.structs.Map<K,V>)} f
 * @param {T=} opt_obj
 * @template T
 */
goog.structs.Map.prototype.forEach = function(f, opt_obj) {
  var keys = this.getKeys();
  for (var i = 0; i < keys.length; i++) {
    var key = keys[i];
    var value = this.get(key);
    f.call(opt_obj, value, key, this);
  }
};
/**
 * @return {!goog.structs.Map}
 */
goog.structs.Map.prototype.clone = function() {
  return new goog.structs.Map(this);
};
/**
 * @return {!goog.structs.Map}
 */
goog.structs.Map.prototype.transpose = function() {
  var transposed = new goog.structs.Map;
  for (var i = 0; i < this.keys_.length; i++) {
    var key = this.keys_[i];
    var value = this.map_[key];
    transposed.set(value, key);
  }
  return transposed;
};
/**
 * @return {!Object}
 */
goog.structs.Map.prototype.toObject = function() {
  this.cleanupKeysArray_();
  var obj = {};
  for (var i = 0; i < this.keys_.length; i++) {
    var key = this.keys_[i];
    obj[key] = this.map_[key];
  }
  return obj;
};
/**
 * @return {!goog.iter.Iterator}
 */
goog.structs.Map.prototype.getKeyIterator = function() {
  return this.__iterator__(true);
};
/**
 * @return {!goog.iter.Iterator}
 */
goog.structs.Map.prototype.getValueIterator = function() {
  return this.__iterator__(false);
};
/**
 * @param {boolean=} opt_keys
 * @return {!goog.iter.Iterator}
 */
goog.structs.Map.prototype.__iterator__ = function(opt_keys) {
  this.cleanupKeysArray_();
  var i = 0;
  var version = this.version_;
  var selfObj = this;
  var newIter = new goog.iter.Iterator;
  newIter.next = function() {
    if (version != selfObj.version_) {
      throw new Error("The map has changed since the iterator was created");
    }
    if (i >= selfObj.keys_.length) {
      throw goog.iter.StopIteration;
    }
    var key = selfObj.keys_[i++];
    return opt_keys ? key : selfObj.map_[key];
  };
  return newIter;
};
/**
 * @private
 * @param {!Object} obj
 * @param {*} key
 * @return {boolean}
 */
goog.structs.Map.hasKey_ = function(obj, key) {
  return Object.prototype.hasOwnProperty.call(obj, key);
};

//# sourceMappingURL=goog.structs.map.js.map
