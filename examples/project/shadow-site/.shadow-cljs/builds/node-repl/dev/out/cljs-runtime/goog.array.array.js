goog.provide("goog.array");
goog.require("goog.asserts");
/** @define {boolean} */ goog.NATIVE_ARRAY_PROTOTYPES = goog.define("goog.NATIVE_ARRAY_PROTOTYPES", goog.TRUSTED_SITE);
/** @define {boolean} */ goog.array.ASSUME_NATIVE_FUNCTIONS = goog.define("goog.array.ASSUME_NATIVE_FUNCTIONS", goog.FEATURESET_YEAR > 2012);
/**
 * @param {(IArrayLike<T>|string)} array
 * @return {T}
 * @template T
 */
goog.array.peek = function(array) {
  return array[array.length - 1];
};
/**
 * @param {(IArrayLike<T>|string)} array
 * @return {T}
 * @template T
 */
goog.array.last = goog.array.peek;
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {T} obj
 * @param {number=} opt_fromIndex
 * @return {number}
 * @template T
 */
goog.array.indexOf = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.indexOf) ? function(arr, obj, opt_fromIndex) {
  goog.asserts.assert(arr.length != null);
  return Array.prototype.indexOf.call(arr, obj, opt_fromIndex);
} : function(arr, obj, opt_fromIndex) {
  var fromIndex = opt_fromIndex == null ? 0 : opt_fromIndex < 0 ? Math.max(0, arr.length + opt_fromIndex) : opt_fromIndex;
  if (typeof arr === "string") {
    if (typeof obj !== "string" || obj.length != 1) {
      return -1;
    }
    return arr.indexOf(obj, fromIndex);
  }
  for (var i = fromIndex; i < arr.length; i++) {
    if (i in arr && arr[i] === obj) {
      return i;
    }
  }
  return -1;
};
/**
 * @param {(!IArrayLike<T>|string)} arr
 * @param {T} obj
 * @param {?number=} opt_fromIndex
 * @return {number}
 * @template T
 */
goog.array.lastIndexOf = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.lastIndexOf) ? function(arr, obj, opt_fromIndex) {
  goog.asserts.assert(arr.length != null);
  var fromIndex = opt_fromIndex == null ? arr.length - 1 : opt_fromIndex;
  return Array.prototype.lastIndexOf.call(arr, obj, fromIndex);
} : function(arr, obj, opt_fromIndex) {
  var fromIndex = opt_fromIndex == null ? arr.length - 1 : opt_fromIndex;
  if (fromIndex < 0) {
    fromIndex = Math.max(0, arr.length + fromIndex);
  }
  if (typeof arr === "string") {
    if (typeof obj !== "string" || obj.length != 1) {
      return -1;
    }
    return arr.lastIndexOf(obj, fromIndex);
  }
  for (var i = fromIndex; i >= 0; i--) {
    if (i in arr && arr[i] === obj) {
      return i;
    }
  }
  return -1;
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):?} f
 * @param {S=} opt_obj
 * @template T
 * @template S
 */
goog.array.forEach = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.forEach) ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  Array.prototype.forEach.call(arr, f, opt_obj);
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = 0; i < l; i++) {
    if (i in arr2) {
      f.call(/** @type {?} */ (opt_obj), arr2[i], i, arr);
    }
  }
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):?} f
 * @param {S=} opt_obj
 * @template T
 * @template S
 */
goog.array.forEachRight = function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = l - 1; i >= 0; --i) {
    if (i in arr2) {
      f.call(/** @type {?} */ (opt_obj), arr2[i], i, arr);
    }
  }
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {!Array<T>}
 * @template T
 * @template S
 */
goog.array.filter = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.filter) ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return Array.prototype.filter.call(arr, f, opt_obj);
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var res = [];
  var resLength = 0;
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = 0; i < l; i++) {
    if (i in arr2) {
      var val = arr2[i];
      if (f.call(/** @type {?} */ (opt_obj), val, i, arr)) {
        res[resLength++] = val;
      }
    }
  }
  return res;
};
/**
 * @param {(IArrayLike<VALUE>|string)} arr
 * @param {function(this:THIS,VALUE,number,?):RESULT} f
 * @param {THIS=} opt_obj
 * @return {!Array<RESULT>}
 * @template THIS
 * @template VALUE
 * @template RESULT
 */
goog.array.map = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.map) ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return Array.prototype.map.call(arr, f, opt_obj);
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var res = new Array(l);
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = 0; i < l; i++) {
    if (i in arr2) {
      res[i] = f.call(/** @type {?} */ (opt_obj), arr2[i], i, arr);
    }
  }
  return res;
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {function(this:S,R,T,number,?):R} f
 * @param {?} val
 * @param {S=} opt_obj
 * @return {R}
 * @template T
 * @template S
 * @template R
 */
goog.array.reduce = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.reduce) ? function(arr, f, val, opt_obj) {
  goog.asserts.assert(arr.length != null);
  if (opt_obj) {
    f = goog.bind(f, opt_obj);
  }
  return Array.prototype.reduce.call(arr, f, val);
} : function(arr, f, val, opt_obj) {
  var rval = val;
  goog.array.forEach(arr, function(val, index) {
    rval = f.call(/** @type {?} */ (opt_obj), rval, val, index, arr);
  });
  return rval;
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,R,T,number,?):R} f
 * @param {?} val
 * @param {S=} opt_obj
 * @return {R}
 * @template T
 * @template S
 * @template R
 */
goog.array.reduceRight = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.reduceRight) ? function(arr, f, val, opt_obj) {
  goog.asserts.assert(arr.length != null);
  goog.asserts.assert(f != null);
  if (opt_obj) {
    f = goog.bind(f, opt_obj);
  }
  return Array.prototype.reduceRight.call(arr, f, val);
} : function(arr, f, val, opt_obj) {
  var rval = val;
  goog.array.forEachRight(arr, function(val, index) {
    rval = f.call(/** @type {?} */ (opt_obj), rval, val, index, arr);
  });
  return rval;
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {boolean}
 * @template T
 * @template S
 */
goog.array.some = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.some) ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return Array.prototype.some.call(arr, f, opt_obj);
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = 0; i < l; i++) {
    if (i in arr2 && f.call(/** @type {?} */ (opt_obj), arr2[i], i, arr)) {
      return true;
    }
  }
  return false;
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {boolean}
 * @template T
 * @template S
 */
goog.array.every = goog.NATIVE_ARRAY_PROTOTYPES && (goog.array.ASSUME_NATIVE_FUNCTIONS || Array.prototype.every) ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return Array.prototype.every.call(arr, f, opt_obj);
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = 0; i < l; i++) {
    if (i in arr2 && !f.call(/** @type {?} */ (opt_obj), arr2[i], i, arr)) {
      return false;
    }
  }
  return true;
};
/**
 * @param {(!IArrayLike<T>|string)} arr
 * @param {function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {number}
 * @template T
 * @template S
 */
goog.array.count = function(arr, f, opt_obj) {
  var count = 0;
  goog.array.forEach(arr, function(element, index, arr) {
    if (f.call(/** @type {?} */ (opt_obj), element, index, arr)) {
      ++count;
    }
  }, opt_obj);
  return count;
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {(T|null)}
 * @template T
 * @template S
 */
goog.array.find = function(arr, f, opt_obj) {
  var i = goog.array.findIndex(arr, f, opt_obj);
  return i < 0 ? null : typeof arr === "string" ? arr.charAt(i) : arr[i];
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {number}
 * @template T
 * @template S
 */
goog.array.findIndex = function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = 0; i < l; i++) {
    if (i in arr2 && f.call(/** @type {?} */ (opt_obj), arr2[i], i, arr)) {
      return i;
    }
  }
  return -1;
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {(T|null)}
 * @template T
 * @template S
 */
goog.array.findRight = function(arr, f, opt_obj) {
  var i = goog.array.findIndexRight(arr, f, opt_obj);
  return i < 0 ? null : typeof arr === "string" ? arr.charAt(i) : arr[i];
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {number}
 * @template T
 * @template S
 */
goog.array.findIndexRight = function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = typeof arr === "string" ? arr.split("") : arr;
  for (var i = l - 1; i >= 0; i--) {
    if (i in arr2 && f.call(/** @type {?} */ (opt_obj), arr2[i], i, arr)) {
      return i;
    }
  }
  return -1;
};
/**
 * @param {(IArrayLike<?>|string)} arr
 * @param {*} obj
 * @return {boolean}
 */
goog.array.contains = function(arr, obj) {
  return goog.array.indexOf(arr, obj) >= 0;
};
/**
 * @param {(IArrayLike<?>|string)} arr
 * @return {boolean}
 */
goog.array.isEmpty = function(arr) {
  return arr.length == 0;
};
/**
 * @param {IArrayLike<?>} arr
 */
goog.array.clear = function(arr) {
  if (!goog.isArray(arr)) {
    for (var i = arr.length - 1; i >= 0; i--) {
      delete arr[i];
    }
  }
  arr.length = 0;
};
/**
 * @param {Array<T>} arr
 * @param {T} obj
 * @template T
 */
goog.array.insert = function(arr, obj) {
  if (!goog.array.contains(arr, obj)) {
    arr.push(obj);
  }
};
/**
 * @param {IArrayLike<?>} arr
 * @param {*} obj
 * @param {number=} opt_i
 */
goog.array.insertAt = function(arr, obj, opt_i) {
  goog.array.splice(arr, opt_i, 0, obj);
};
/**
 * @param {IArrayLike<?>} arr
 * @param {IArrayLike<?>} elementsToAdd
 * @param {number=} opt_i
 */
goog.array.insertArrayAt = function(arr, elementsToAdd, opt_i) {
  goog.partial(goog.array.splice, arr, opt_i, 0).apply(null, elementsToAdd);
};
/**
 * @param {Array<T>} arr
 * @param {T} obj
 * @param {T=} opt_obj2
 * @template T
 */
goog.array.insertBefore = function(arr, obj, opt_obj2) {
  var i;
  if (arguments.length == 2 || (i = goog.array.indexOf(arr, opt_obj2)) < 0) {
    arr.push(obj);
  } else {
    goog.array.insertAt(arr, obj, i);
  }
};
/**
 * @param {IArrayLike<T>} arr
 * @param {T} obj
 * @return {boolean}
 * @template T
 */
goog.array.remove = function(arr, obj) {
  var i = goog.array.indexOf(arr, obj);
  var rv;
  if (rv = i >= 0) {
    goog.array.removeAt(arr, i);
  }
  return rv;
};
/**
 * @param {!IArrayLike<T>} arr
 * @param {T} obj
 * @return {boolean}
 * @template T
 */
goog.array.removeLast = function(arr, obj) {
  var i = goog.array.lastIndexOf(arr, obj);
  if (i >= 0) {
    goog.array.removeAt(arr, i);
    return true;
  }
  return false;
};
/**
 * @param {IArrayLike<?>} arr
 * @param {number} i
 * @return {boolean}
 */
goog.array.removeAt = function(arr, i) {
  goog.asserts.assert(arr.length != null);
  return Array.prototype.splice.call(arr, i, 1).length == 1;
};
/**
 * @param {IArrayLike<T>} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {boolean}
 * @template T
 * @template S
 */
goog.array.removeIf = function(arr, f, opt_obj) {
  var i = goog.array.findIndex(arr, f, opt_obj);
  if (i >= 0) {
    goog.array.removeAt(arr, i);
    return true;
  }
  return false;
};
/**
 * @param {IArrayLike<T>} arr
 * @param {?function(this:S,T,number,?):boolean} f
 * @param {S=} opt_obj
 * @return {number}
 * @template T
 * @template S
 */
goog.array.removeAllIf = function(arr, f, opt_obj) {
  var removedCount = 0;
  goog.array.forEachRight(arr, function(val, index) {
    if (f.call(/** @type {?} */ (opt_obj), val, index, arr)) {
      if (goog.array.removeAt(arr, index)) {
        removedCount++;
      }
    }
  });
  return removedCount;
};
/**
 * @param {...*} var_args
 * @return {!Array<?>}
 */
goog.array.concat = function(var_args) {
  return Array.prototype.concat.apply([], arguments);
};
/**
 * @param {...!Array<T>} var_args
 * @return {!Array<T>}
 * @template T
 */
goog.array.join = function(var_args) {
  return Array.prototype.concat.apply([], arguments);
};
/**
 * @param {(IArrayLike<T>|string)} object
 * @return {!Array<T>}
 * @template T
 */
goog.array.toArray = function(object) {
  var length = object.length;
  if (length > 0) {
    var rv = new Array(length);
    for (var i = 0; i < length; i++) {
      rv[i] = object[i];
    }
    return rv;
  }
  return [];
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @return {!Array<T>}
 * @template T
 */
goog.array.clone = goog.array.toArray;
/**
 * @param {Array<VALUE>} arr1
 * @param {...(IArrayLike<VALUE>|VALUE)} var_args
 * @template VALUE
 */
goog.array.extend = function(arr1, var_args) {
  for (var i = 1; i < arguments.length; i++) {
    var arr2 = arguments[i];
    if (goog.isArrayLike(arr2)) {
      var len1 = arr1.length || 0;
      var len2 = arr2.length || 0;
      arr1.length = len1 + len2;
      for (var j = 0; j < len2; j++) {
        arr1[len1 + j] = arr2[j];
      }
    } else {
      arr1.push(arr2);
    }
  }
};
/**
 * @param {IArrayLike<T>} arr
 * @param {(number|undefined)} index
 * @param {number} howMany
 * @param {...T} var_args
 * @return {!Array<T>}
 * @template T
 */
goog.array.splice = function(arr, index, howMany, var_args) {
  goog.asserts.assert(arr.length != null);
  return Array.prototype.splice.apply(arr, goog.array.slice(arguments, 1));
};
/**
 * @param {(IArrayLike<T>|string)} arr
 * @param {number} start
 * @param {number=} opt_end
 * @return {!Array<T>}
 * @template T
 */
goog.array.slice = function(arr, start, opt_end) {
  goog.asserts.assert(arr.length != null);
  if (arguments.length <= 2) {
    return Array.prototype.slice.call(arr, start);
  } else {
    return Array.prototype.slice.call(arr, start, opt_end);
  }
};
/**
 * @param {IArrayLike<T>} arr
 * @param {Array=} opt_rv
 * @param {function(T):string=} opt_hashFn
 * @template T
 */
goog.array.removeDuplicates = function(arr, opt_rv, opt_hashFn) {
  var returnArray = opt_rv || arr;
  var defaultHashFn = function(item) {
    return goog.isObject(item) ? "o" + goog.getUid(item) : (typeof item).charAt(0) + item;
  };
  var hashFn = opt_hashFn || defaultHashFn;
  var seen = {}, cursorInsert = 0, cursorRead = 0;
  while (cursorRead < arr.length) {
    var current = arr[cursorRead++];
    var key = hashFn(current);
    if (!Object.prototype.hasOwnProperty.call(seen, key)) {
      seen[key] = true;
      returnArray[cursorInsert++] = current;
    }
  }
  returnArray.length = cursorInsert;
};
/**
 * @param {IArrayLike<VALUE>} arr
 * @param {TARGET} target
 * @param {function(TARGET,VALUE):number=} opt_compareFn
 * @return {number}
 * @template TARGET
 * @template VALUE
 */
goog.array.binarySearch = function(arr, target, opt_compareFn) {
  return goog.array.binarySearch_(arr, opt_compareFn || goog.array.defaultCompare, false, target);
};
/**
 * @param {IArrayLike<VALUE>} arr
 * @param {function(this:THIS,VALUE,number,?):number} evaluator
 * @param {THIS=} opt_obj
 * @return {number}
 * @template THIS
 * @template VALUE
 */
goog.array.binarySelect = function(arr, evaluator, opt_obj) {
  return goog.array.binarySearch_(arr, evaluator, true, undefined, opt_obj);
};
/**
 * @private
 * @param {IArrayLike<?>} arr
 * @param {(function(?,?,?):number|function(?,?):number)} compareFn
 * @param {boolean} isEvaluator
 * @param {?=} opt_target
 * @param {Object=} opt_selfObj
 * @return {number}
 */
goog.array.binarySearch_ = function(arr, compareFn, isEvaluator, opt_target, opt_selfObj) {
  var left = 0;
  var right = arr.length;
  var found;
  while (left < right) {
    var middle = left + (right - left >>> 1);
    var compareResult;
    if (isEvaluator) {
      compareResult = compareFn.call(opt_selfObj, arr[middle], middle, arr);
    } else {
      compareResult = /** @type {function(?,?):number} */ (compareFn)(opt_target, arr[middle]);
    }
    if (compareResult > 0) {
      left = middle + 1;
    } else {
      right = middle;
      found = !compareResult;
    }
  }
  return found ? left : -left - 1;
};
/**
 * @param {Array<T>} arr
 * @param {?function(T,T):number=} opt_compareFn
 * @template T
 */
goog.array.sort = function(arr, opt_compareFn) {
  arr.sort(opt_compareFn || goog.array.defaultCompare);
};
/**
 * @param {Array<T>} arr
 * @param {?function(T,T):number=} opt_compareFn
 * @template T
 */
goog.array.stableSort = function(arr, opt_compareFn) {
  var compArr = new Array(arr.length);
  for (var i = 0; i < arr.length; i++) {
    compArr[i] = {index:i, value:arr[i]};
  }
  var valueCompareFn = opt_compareFn || goog.array.defaultCompare;
  function stableCompareFn(obj1, obj2) {
    return valueCompareFn(obj1.value, obj2.value) || obj1.index - obj2.index;
  }
  goog.array.sort(compArr, stableCompareFn);
  for (var i = 0; i < arr.length; i++) {
    arr[i] = compArr[i].value;
  }
};
/**
 * @param {Array<T>} arr
 * @param {function(T):K} keyFn
 * @param {?function(K,K):number=} opt_compareFn
 * @template T
 * @template K
 */
goog.array.sortByKey = function(arr, keyFn, opt_compareFn) {
  var keyCompareFn = opt_compareFn || goog.array.defaultCompare;
  goog.array.sort(arr, function(a, b) {
    return keyCompareFn(keyFn(a), keyFn(b));
  });
};
/**
 * @param {Array<Object>} arr
 * @param {string} key
 * @param {Function=} opt_compareFn
 */
goog.array.sortObjectsByKey = function(arr, key, opt_compareFn) {
  goog.array.sortByKey(arr, function(obj) {
    return obj[key];
  }, opt_compareFn);
};
/**
 * @param {!IArrayLike<T>} arr
 * @param {?function(T,T):number=} opt_compareFn
 * @param {boolean=} opt_strict
 * @return {boolean}
 * @template T
 */
goog.array.isSorted = function(arr, opt_compareFn, opt_strict) {
  var compare = opt_compareFn || goog.array.defaultCompare;
  for (var i = 1; i < arr.length; i++) {
    var compareResult = compare(arr[i - 1], arr[i]);
    if (compareResult > 0 || compareResult == 0 && opt_strict) {
      return false;
    }
  }
  return true;
};
/**
 * @param {IArrayLike<?>} arr1
 * @param {IArrayLike<?>} arr2
 * @param {Function=} opt_equalsFn
 * @return {boolean}
 */
goog.array.equals = function(arr1, arr2, opt_equalsFn) {
  if (!goog.isArrayLike(arr1) || !goog.isArrayLike(arr2) || arr1.length != arr2.length) {
    return false;
  }
  var l = arr1.length;
  var equalsFn = opt_equalsFn || goog.array.defaultCompareEquality;
  for (var i = 0; i < l; i++) {
    if (!equalsFn(arr1[i], arr2[i])) {
      return false;
    }
  }
  return true;
};
/**
 * @param {!IArrayLike<VALUE>} arr1
 * @param {!IArrayLike<VALUE>} arr2
 * @param {function(VALUE,VALUE):number=} opt_compareFn
 * @return {number}
 * @template VALUE
 */
goog.array.compare3 = function(arr1, arr2, opt_compareFn) {
  var compare = opt_compareFn || goog.array.defaultCompare;
  var l = Math.min(arr1.length, arr2.length);
  for (var i = 0; i < l; i++) {
    var result = compare(arr1[i], arr2[i]);
    if (result != 0) {
      return result;
    }
  }
  return goog.array.defaultCompare(arr1.length, arr2.length);
};
/**
 * @param {VALUE} a
 * @param {VALUE} b
 * @return {number}
 * @template VALUE
 */
goog.array.defaultCompare = function(a, b) {
  return a > b ? 1 : a < b ? -1 : 0;
};
/**
 * @param {VALUE} a
 * @param {VALUE} b
 * @return {number}
 * @template VALUE
 */
goog.array.inverseDefaultCompare = function(a, b) {
  return -goog.array.defaultCompare(a, b);
};
/**
 * @param {*} a
 * @param {*} b
 * @return {boolean}
 */
goog.array.defaultCompareEquality = function(a, b) {
  return a === b;
};
/**
 * @param {IArrayLike<VALUE>} array
 * @param {VALUE} value
 * @param {function(VALUE,VALUE):number=} opt_compareFn
 * @return {boolean}
 * @template VALUE
 */
goog.array.binaryInsert = function(array, value, opt_compareFn) {
  var index = goog.array.binarySearch(array, value, opt_compareFn);
  if (index < 0) {
    goog.array.insertAt(array, value, -(index + 1));
    return true;
  }
  return false;
};
/**
 * @param {!IArrayLike<VALUE>} array
 * @param {VALUE} value
 * @param {function(VALUE,VALUE):number=} opt_compareFn
 * @return {boolean}
 * @template VALUE
 */
goog.array.binaryRemove = function(array, value, opt_compareFn) {
  var index = goog.array.binarySearch(array, value, opt_compareFn);
  return index >= 0 ? goog.array.removeAt(array, index) : false;
};
/**
 * @param {IArrayLike<T>} array
 * @param {function(this:S,T,number,!IArrayLike<T>):?} sorter
 * @param {S=} opt_obj
 * @return {!Object<?,!Array<T>>}
 * @template T
 * @template S
 */
goog.array.bucket = function(array, sorter, opt_obj) {
  var buckets = {};
  for (var i = 0; i < array.length; i++) {
    var value = array[i];
    var key = sorter.call(/** @type {?} */ (opt_obj), value, i, array);
    if (key !== undefined) {
      var bucket = buckets[key] || (buckets[key] = []);
      bucket.push(value);
    }
  }
  return buckets;
};
/**
 * @param {IArrayLike<T>} arr
 * @param {?function(this:S,T,number,?):string} keyFunc
 * @param {S=} opt_obj
 * @return {!Object<?,T>}
 * @template T
 * @template S
 */
goog.array.toObject = function(arr, keyFunc, opt_obj) {
  var ret = {};
  goog.array.forEach(arr, function(element, index) {
    ret[keyFunc.call(/** @type {?} */ (opt_obj), element, index, arr)] = element;
  });
  return ret;
};
/**
 * @param {number} startOrEnd
 * @param {number=} opt_end
 * @param {number=} opt_step
 * @return {!Array<number>}
 */
goog.array.range = function(startOrEnd, opt_end, opt_step) {
  var array = [];
  var start = 0;
  var end = startOrEnd;
  var step = opt_step || 1;
  if (opt_end !== undefined) {
    start = startOrEnd;
    end = opt_end;
  }
  if (step * (end - start) < 0) {
    return [];
  }
  if (step > 0) {
    for (var i = start; i < end; i += step) {
      array.push(i);
    }
  } else {
    for (var i = start; i > end; i += step) {
      array.push(i);
    }
  }
  return array;
};
/**
 * @param {VALUE} value
 * @param {number} n
 * @return {!Array<VALUE>}
 * @template VALUE
 */
goog.array.repeat = function(value, n) {
  var array = [];
  for (var i = 0; i < n; i++) {
    array[i] = value;
  }
  return array;
};
/**
 * @param {...*} var_args
 * @return {!Array<?>}
 */
goog.array.flatten = function(var_args) {
  var CHUNK_SIZE = 8192;
  var result = [];
  for (var i = 0; i < arguments.length; i++) {
    var element = arguments[i];
    if (goog.isArray(element)) {
      for (var c = 0; c < element.length; c += CHUNK_SIZE) {
        var chunk = goog.array.slice(element, c, c + CHUNK_SIZE);
        var recurseResult = goog.array.flatten.apply(null, chunk);
        for (var r = 0; r < recurseResult.length; r++) {
          result.push(recurseResult[r]);
        }
      }
    } else {
      result.push(element);
    }
  }
  return result;
};
/**
 * @param {!Array<T>} array
 * @param {number} n
 * @return {!Array<T>}
 * @template T
 */
goog.array.rotate = function(array, n) {
  goog.asserts.assert(array.length != null);
  if (array.length) {
    n %= array.length;
    if (n > 0) {
      Array.prototype.unshift.apply(array, array.splice(-n, n));
    } else {
      if (n < 0) {
        Array.prototype.push.apply(array, array.splice(0, -n));
      }
    }
  }
  return array;
};
/**
 * @param {!IArrayLike<?>} arr
 * @param {number} fromIndex
 * @param {number} toIndex
 */
goog.array.moveItem = function(arr, fromIndex, toIndex) {
  goog.asserts.assert(fromIndex >= 0 && fromIndex < arr.length);
  goog.asserts.assert(toIndex >= 0 && toIndex < arr.length);
  var removedItems = Array.prototype.splice.call(arr, fromIndex, 1);
  Array.prototype.splice.call(arr, toIndex, 0, removedItems[0]);
};
/**
 * @param {...!IArrayLike<?>} var_args
 * @return {!Array<!Array<?>>}
 */
goog.array.zip = function(var_args) {
  if (!arguments.length) {
    return [];
  }
  var result = [];
  var minLen = arguments[0].length;
  for (var i = 1; i < arguments.length; i++) {
    if (arguments[i].length < minLen) {
      minLen = arguments[i].length;
    }
  }
  for (var i = 0; i < minLen; i++) {
    var value = [];
    for (var j = 0; j < arguments.length; j++) {
      value.push(arguments[j][i]);
    }
    result.push(value);
  }
  return result;
};
/**
 * @param {!Array<?>} arr
 * @param {function():number=} opt_randFn
 */
goog.array.shuffle = function(arr, opt_randFn) {
  var randFn = opt_randFn || Math.random;
  for (var i = arr.length - 1; i > 0; i--) {
    var j = Math.floor(randFn() * (i + 1));
    var tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
};
/**
 * @param {!IArrayLike<T>} arr
 * @param {!IArrayLike<number>} index_arr
 * @return {!Array<T>}
 * @template T
 */
goog.array.copyByIndex = function(arr, index_arr) {
  var result = [];
  goog.array.forEach(index_arr, function(index) {
    result.push(arr[index]);
  });
  return result;
};
/**
 * @param {(!IArrayLike<VALUE>|string)} arr
 * @param {function(this:THIS,VALUE,number,?):!Array<RESULT>} f
 * @param {THIS=} opt_obj
 * @return {!Array<RESULT>}
 * @template THIS
 * @template VALUE
 * @template RESULT
 */
goog.array.concatMap = function(arr, f, opt_obj) {
  return goog.array.concat.apply([], goog.array.map(arr, f, opt_obj));
};

//# sourceMappingURL=goog.array.array.js.map
