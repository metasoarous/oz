goog.provide("goog.iter");
goog.provide("goog.iter.Iterable");
goog.provide("goog.iter.Iterator");
goog.provide("goog.iter.StopIteration");
goog.require("goog.array");
goog.require("goog.asserts");
goog.require("goog.functions");
goog.require("goog.math");
/** @typedef {({length:number}|{__iterator__})} */ goog.iter.Iterable;
/** @const @type {!Error} */ goog.iter.StopIteration = "StopIteration" in goog.global ? goog.global["StopIteration"] : {message:"StopIteration", stack:""};
/**
 * @constructor
 * @template VALUE
 */
goog.iter.Iterator = function() {
};
/**
 * @return {VALUE}
 */
goog.iter.Iterator.prototype.next = function() {
  throw goog.iter.StopIteration;
};
/**
 * @param {boolean=} opt_keys
 * @return {!goog.iter.Iterator<VALUE>}
 */
goog.iter.Iterator.prototype.__iterator__ = function(opt_keys) {
  return this;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.toIterator = function(iterable) {
  if (iterable instanceof goog.iter.Iterator) {
    return iterable;
  }
  if (typeof iterable.__iterator__ == "function") {
    return /** @type {{__iterator__:function(this:?,boolean=)}} */ (iterable).__iterator__(false);
  }
  if (goog.isArrayLike(iterable)) {
    var like = /** @type {!IArrayLike<(number|string)>} */ (iterable);
    var i = 0;
    var newIter = new goog.iter.Iterator;
    newIter.next = function() {
      while (true) {
        if (i >= like.length) {
          throw goog.iter.StopIteration;
        }
        if (!(i in like)) {
          i++;
          continue;
        }
        return like[i++];
      }
    };
    return newIter;
  }
  throw new Error("Not implemented");
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,?,!goog.iter.Iterator<VALUE>)} f
 * @param {THIS=} opt_obj
 * @template THIS
 * @template VALUE
 */
goog.iter.forEach = function(iterable, f, opt_obj) {
  if (goog.isArrayLike(iterable)) {
    try {
      goog.array.forEach(/** @type {IArrayLike<?>} */ (iterable), f, opt_obj);
    } catch (ex) {
      if (ex !== goog.iter.StopIteration) {
        throw ex;
      }
    }
  } else {
    iterable = goog.iter.toIterator(iterable);
    try {
      while (true) {
        f.call(opt_obj, iterable.next(), undefined, iterable);
      }
    } catch (ex$2) {
      if (ex$2 !== goog.iter.StopIteration) {
        throw ex$2;
      }
    }
  }
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,undefined,!goog.iter.Iterator<VALUE>):boolean} f
 * @param {THIS=} opt_obj
 * @return {!goog.iter.Iterator<VALUE>}
 * @template THIS
 * @template VALUE
 */
goog.iter.filter = function(iterable, f, opt_obj) {
  var iterator = goog.iter.toIterator(iterable);
  var newIter = new goog.iter.Iterator;
  newIter.next = function() {
    while (true) {
      var val = iterator.next();
      if (f.call(opt_obj, val, undefined, iterator)) {
        return val;
      }
    }
  };
  return newIter;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,undefined,!goog.iter.Iterator<VALUE>):boolean} f
 * @param {THIS=} opt_obj
 * @return {!goog.iter.Iterator<VALUE>}
 * @template THIS
 * @template VALUE
 */
goog.iter.filterFalse = function(iterable, f, opt_obj) {
  return goog.iter.filter(iterable, goog.functions.not(f), opt_obj);
};
/**
 * @param {number} startOrStop
 * @param {number=} opt_stop
 * @param {number=} opt_step
 * @return {!goog.iter.Iterator<number>}
 */
goog.iter.range = function(startOrStop, opt_stop, opt_step) {
  var start = 0;
  var stop = startOrStop;
  var step = opt_step || 1;
  if (arguments.length > 1) {
    start = startOrStop;
    stop = +opt_stop;
  }
  if (step == 0) {
    throw new Error("Range step argument must not be zero");
  }
  var newIter = new goog.iter.Iterator;
  newIter.next = function() {
    if (step > 0 && start >= stop || step < 0 && start <= stop) {
      throw goog.iter.StopIteration;
    }
    var rv = start;
    start += step;
    return rv;
  };
  return newIter;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {string} deliminator
 * @return {string}
 * @template VALUE
 */
goog.iter.join = function(iterable, deliminator) {
  return goog.iter.toArray(iterable).join(deliminator);
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,undefined,!goog.iter.Iterator<VALUE>):RESULT} f
 * @param {THIS=} opt_obj
 * @return {!goog.iter.Iterator<RESULT>}
 * @template THIS
 * @template VALUE
 * @template RESULT
 */
goog.iter.map = function(iterable, f, opt_obj) {
  var iterator = goog.iter.toIterator(iterable);
  var newIter = new goog.iter.Iterator;
  newIter.next = function() {
    var val = iterator.next();
    return f.call(opt_obj, val, undefined, iterator);
  };
  return newIter;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,VALUE):VALUE} f
 * @param {VALUE} val
 * @param {THIS=} opt_obj
 * @return {VALUE}
 * @template THIS
 * @template VALUE
 */
goog.iter.reduce = function(iterable, f, val, opt_obj) {
  var rval = val;
  goog.iter.forEach(iterable, function(val) {
    rval = f.call(opt_obj, rval, val);
  });
  return rval;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,undefined,!goog.iter.Iterator<VALUE>):boolean} f
 * @param {THIS=} opt_obj
 * @return {boolean}
 * @template THIS
 * @template VALUE
 */
goog.iter.some = function(iterable, f, opt_obj) {
  iterable = goog.iter.toIterator(iterable);
  try {
    while (true) {
      if (f.call(opt_obj, iterable.next(), undefined, iterable)) {
        return true;
      }
    }
  } catch (ex) {
    if (ex !== goog.iter.StopIteration) {
      throw ex;
    }
  }
  return false;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,undefined,!goog.iter.Iterator<VALUE>):boolean} f
 * @param {THIS=} opt_obj
 * @return {boolean}
 * @template THIS
 * @template VALUE
 */
goog.iter.every = function(iterable, f, opt_obj) {
  iterable = goog.iter.toIterator(iterable);
  try {
    while (true) {
      if (!f.call(opt_obj, iterable.next(), undefined, iterable)) {
        return false;
      }
    }
  } catch (ex) {
    if (ex !== goog.iter.StopIteration) {
      throw ex;
    }
  }
  return true;
};
/**
 * @param {...(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} var_args
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.chain = function(var_args) {
  return goog.iter.chainFromIterable(arguments);
};
/**
 * @param {(goog.iter.Iterator<?>|goog.iter.Iterable)} iterable
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.chainFromIterable = function(iterable) {
  var iterator = goog.iter.toIterator(iterable);
  var iter = new goog.iter.Iterator;
  var current = null;
  iter.next = function() {
    while (true) {
      if (current == null) {
        var it = iterator.next();
        current = goog.iter.toIterator(it);
      }
      try {
        return current.next();
      } catch (ex) {
        if (ex !== goog.iter.StopIteration) {
          throw ex;
        }
        current = null;
      }
    }
  };
  return iter;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,undefined,!goog.iter.Iterator<VALUE>):boolean} f
 * @param {THIS=} opt_obj
 * @return {!goog.iter.Iterator<VALUE>}
 * @template THIS
 * @template VALUE
 */
goog.iter.dropWhile = function(iterable, f, opt_obj) {
  var iterator = goog.iter.toIterator(iterable);
  var newIter = new goog.iter.Iterator;
  var dropping = true;
  newIter.next = function() {
    while (true) {
      var val = iterator.next();
      if (dropping && f.call(opt_obj, val, undefined, iterator)) {
        continue;
      } else {
        dropping = false;
      }
      return val;
    }
  };
  return newIter;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {function(this:THIS,VALUE,undefined,!goog.iter.Iterator<VALUE>):boolean} f
 * @param {THIS=} opt_obj
 * @return {!goog.iter.Iterator<VALUE>}
 * @template THIS
 * @template VALUE
 */
goog.iter.takeWhile = function(iterable, f, opt_obj) {
  var iterator = goog.iter.toIterator(iterable);
  var iter = new goog.iter.Iterator;
  iter.next = function() {
    var val = iterator.next();
    if (f.call(opt_obj, val, undefined, iterator)) {
      return val;
    }
    throw goog.iter.StopIteration;
  };
  return iter;
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @return {!Array<VALUE>}
 * @template VALUE
 */
goog.iter.toArray = function(iterable) {
  if (goog.isArrayLike(iterable)) {
    return goog.array.toArray(/** @type {!IArrayLike<?>} */ (iterable));
  }
  iterable = goog.iter.toIterator(iterable);
  var array = [];
  goog.iter.forEach(iterable, function(val) {
    array.push(val);
  });
  return array;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable1
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable2
 * @param {function(VALUE,VALUE):boolean=} opt_equalsFn
 * @return {boolean}
 * @template VALUE
 */
goog.iter.equals = function(iterable1, iterable2, opt_equalsFn) {
  var fillValue = {};
  var pairs = goog.iter.zipLongest(fillValue, iterable1, iterable2);
  var equalsFn = opt_equalsFn || goog.array.defaultCompareEquality;
  return goog.iter.every(pairs, function(pair) {
    return equalsFn(pair[0], pair[1]);
  });
};
/**
 * @param {(goog.iter.Iterator<VALUE>|goog.iter.Iterable)} iterable
 * @param {VALUE} defaultValue
 * @return {VALUE}
 * @template VALUE
 */
goog.iter.nextOrValue = function(iterable, defaultValue) {
  try {
    return goog.iter.toIterator(iterable).next();
  } catch (e) {
    if (e != goog.iter.StopIteration) {
      throw e;
    }
    return defaultValue;
  }
};
/**
 * @param {...!IArrayLike<VALUE>} var_args
 * @return {!goog.iter.Iterator<!Array<VALUE>>}
 * @template VALUE
 */
goog.iter.product = function(var_args) {
  var someArrayEmpty = goog.array.some(arguments, function(arr) {
    return !arr.length;
  });
  if (someArrayEmpty || !arguments.length) {
    return new goog.iter.Iterator;
  }
  var iter = new goog.iter.Iterator;
  var arrays = arguments;
  /** @type {?Array<number>} */ var indicies = goog.array.repeat(0, arrays.length);
  iter.next = function() {
    if (indicies) {
      var retVal = goog.array.map(indicies, function(valueIndex, arrayIndex) {
        return arrays[arrayIndex][valueIndex];
      });
      for (var i = indicies.length - 1; i >= 0; i--) {
        goog.asserts.assert(indicies);
        if (indicies[i] < arrays[i].length - 1) {
          indicies[i]++;
          break;
        }
        if (i == 0) {
          indicies = null;
          break;
        }
        indicies[i] = 0;
      }
      return retVal;
    }
    throw goog.iter.StopIteration;
  };
  return iter;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.cycle = function(iterable) {
  var baseIterator = goog.iter.toIterator(iterable);
  var cache = [];
  var cacheIndex = 0;
  var iter = new goog.iter.Iterator;
  var useCache = false;
  iter.next = function() {
    var returnElement = null;
    if (!useCache) {
      try {
        returnElement = baseIterator.next();
        cache.push(returnElement);
        return returnElement;
      } catch (e) {
        if (e != goog.iter.StopIteration || goog.array.isEmpty(cache)) {
          throw e;
        }
        useCache = true;
      }
    }
    returnElement = cache[cacheIndex];
    cacheIndex = (cacheIndex + 1) % cache.length;
    return returnElement;
  };
  return iter;
};
/**
 * @param {number=} opt_start
 * @param {number=} opt_step
 * @return {!goog.iter.Iterator<number>}
 */
goog.iter.count = function(opt_start, opt_step) {
  var counter = opt_start || 0;
  var step = opt_step !== undefined ? opt_step : 1;
  var iter = new goog.iter.Iterator;
  iter.next = function() {
    var returnValue = counter;
    counter += step;
    return returnValue;
  };
  return iter;
};
/**
 * @param {VALUE} value
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.repeat = function(value) {
  var iter = new goog.iter.Iterator;
  iter.next = goog.functions.constant(value);
  return iter;
};
/**
 * @param {(!goog.iter.Iterator<number>|!goog.iter.Iterable)} iterable
 * @return {!goog.iter.Iterator<number>}
 */
goog.iter.accumulate = function(iterable) {
  var iterator = goog.iter.toIterator(iterable);
  var total = 0;
  var iter = new goog.iter.Iterator;
  iter.next = function() {
    total += iterator.next();
    return total;
  };
  return iter;
};
/**
 * @param {...(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} var_args
 * @return {!goog.iter.Iterator<!Array<VALUE>>}
 * @template VALUE
 */
goog.iter.zip = function(var_args) {
  var args = arguments;
  var iter = new goog.iter.Iterator;
  if (args.length > 0) {
    var iterators = goog.array.map(args, goog.iter.toIterator);
    iter.next = function() {
      var arr = goog.array.map(iterators, function(it) {
        return it.next();
      });
      return arr;
    };
  }
  return iter;
};
/**
 * @param {VALUE} fillValue
 * @param {...(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} var_args
 * @return {!goog.iter.Iterator<!Array<VALUE>>}
 * @template VALUE
 */
goog.iter.zipLongest = function(fillValue, var_args) {
  var args = goog.array.slice(arguments, 1);
  var iter = new goog.iter.Iterator;
  if (args.length > 0) {
    var iterators = goog.array.map(args, goog.iter.toIterator);
    iter.next = function() {
      var iteratorsHaveValues = false;
      var arr = goog.array.map(iterators, function(it) {
        var returnValue;
        try {
          returnValue = it.next();
          iteratorsHaveValues = true;
        } catch (ex) {
          if (ex !== goog.iter.StopIteration) {
            throw ex;
          }
          returnValue = fillValue;
        }
        return returnValue;
      });
      if (!iteratorsHaveValues) {
        throw goog.iter.StopIteration;
      }
      return arr;
    };
  }
  return iter;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} selectors
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.compress = function(iterable, selectors) {
  var selectorIterator = goog.iter.toIterator(selectors);
  return goog.iter.filter(iterable, function() {
    return !!selectorIterator.next();
  });
};
/**
 * @private
 * @constructor
 * @extends {goog.iter.Iterator<!Array<?>>}
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {function(VALUE):KEY=} opt_keyFunc
 * @template KEY
 * @template VALUE
 */
goog.iter.GroupByIterator_ = function(iterable, opt_keyFunc) {
  /** @type {!goog.iter.Iterator} */ this.iterator = goog.iter.toIterator(iterable);
  /** @type {function(VALUE):KEY} */ this.keyFunc = opt_keyFunc || goog.functions.identity;
  /** @type {KEY} */ this.targetKey;
  /** @type {KEY} */ this.currentKey;
  /** @type {VALUE} */ this.currentValue;
};
goog.inherits(goog.iter.GroupByIterator_, goog.iter.Iterator);
/** @override */ goog.iter.GroupByIterator_.prototype.next = function() {
  while (this.currentKey == this.targetKey) {
    this.currentValue = this.iterator.next();
    this.currentKey = this.keyFunc(this.currentValue);
  }
  this.targetKey = this.currentKey;
  return [this.currentKey, this.groupItems_(this.targetKey)];
};
/**
 * @private
 * @param {KEY} targetKey
 * @return {!Array<VALUE>}
 */
goog.iter.GroupByIterator_.prototype.groupItems_ = function(targetKey) {
  var arr = [];
  while (this.currentKey == targetKey) {
    arr.push(this.currentValue);
    try {
      this.currentValue = this.iterator.next();
    } catch (ex) {
      if (ex !== goog.iter.StopIteration) {
        throw ex;
      }
      break;
    }
    this.currentKey = this.keyFunc(this.currentValue);
  }
  return arr;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {function(VALUE):KEY=} opt_keyFunc
 * @return {!goog.iter.Iterator<!Array<?>>}
 * @template KEY
 * @template VALUE
 */
goog.iter.groupBy = function(iterable, opt_keyFunc) {
  return new goog.iter.GroupByIterator_(iterable, opt_keyFunc);
};
/**
 * @param {(!goog.iter.Iterator<?>|!goog.iter.Iterable)} iterable
 * @param {function(this:THIS,...*):RESULT} f
 * @param {THIS=} opt_obj
 * @return {!goog.iter.Iterator<RESULT>}
 * @template THIS
 * @template RESULT
 */
goog.iter.starMap = function(iterable, f, opt_obj) {
  var iterator = goog.iter.toIterator(iterable);
  var iter = new goog.iter.Iterator;
  iter.next = function() {
    var args = goog.iter.toArray(iterator.next());
    return f.apply(opt_obj, goog.array.concat(args, undefined, iterator));
  };
  return iter;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number=} opt_num
 * @return {!Array<goog.iter.Iterator<VALUE>>}
 * @template VALUE
 */
goog.iter.tee = function(iterable, opt_num) {
  var iterator = goog.iter.toIterator(iterable);
  var num = typeof opt_num === "number" ? opt_num : 2;
  var buffers = goog.array.map(goog.array.range(num), function() {
    return [];
  });
  var addNextIteratorValueToBuffers = function() {
    var val = iterator.next();
    goog.array.forEach(buffers, function(buffer) {
      buffer.push(val);
    });
  };
  var createIterator = function(buffer) {
    var iter = new goog.iter.Iterator;
    iter.next = function() {
      if (goog.array.isEmpty(buffer)) {
        addNextIteratorValueToBuffers();
      }
      goog.asserts.assert(!goog.array.isEmpty(buffer));
      return buffer.shift();
    };
    return iter;
  };
  return goog.array.map(buffers, createIterator);
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number=} opt_start
 * @return {!goog.iter.Iterator<!Array<?>>}
 * @template VALUE
 */
goog.iter.enumerate = function(iterable, opt_start) {
  return goog.iter.zip(goog.iter.count(opt_start), iterable);
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number} limitSize
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.limit = function(iterable, limitSize) {
  goog.asserts.assert(goog.math.isInt(limitSize) && limitSize >= 0);
  var iterator = goog.iter.toIterator(iterable);
  var iter = new goog.iter.Iterator;
  var remaining = limitSize;
  iter.next = function() {
    if (remaining-- > 0) {
      return iterator.next();
    }
    throw goog.iter.StopIteration;
  };
  return iter;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number} count
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.consume = function(iterable, count) {
  goog.asserts.assert(goog.math.isInt(count) && count >= 0);
  var iterator = goog.iter.toIterator(iterable);
  while (count-- > 0) {
    goog.iter.nextOrValue(iterator, null);
  }
  return iterator;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number} start
 * @param {number=} opt_end
 * @return {!goog.iter.Iterator<VALUE>}
 * @template VALUE
 */
goog.iter.slice = function(iterable, start, opt_end) {
  goog.asserts.assert(goog.math.isInt(start) && start >= 0);
  var iterator = goog.iter.consume(iterable, start);
  if (typeof opt_end === "number") {
    goog.asserts.assert(goog.math.isInt(opt_end) && opt_end >= start);
    iterator = goog.iter.limit(iterator, opt_end - start);
  }
  return iterator;
};
/**
 * @private
 * @param {?IArrayLike<VALUE>} arr
 * @return {boolean}
 * @template VALUE
 */
goog.iter.hasDuplicates_ = function(arr) {
  var deduped = [];
  goog.array.removeDuplicates(arr, deduped);
  return arr.length != deduped.length;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number=} opt_length
 * @return {!goog.iter.Iterator<!Array<VALUE>>}
 * @template VALUE
 */
goog.iter.permutations = function(iterable, opt_length) {
  var elements = goog.iter.toArray(iterable);
  var length = typeof opt_length === "number" ? opt_length : elements.length;
  var sets = goog.array.repeat(elements, length);
  var product = goog.iter.product.apply(undefined, sets);
  return goog.iter.filter(product, function(arr) {
    return !goog.iter.hasDuplicates_(arr);
  });
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number} length
 * @return {!goog.iter.Iterator<!Array<VALUE>>}
 * @template VALUE
 */
goog.iter.combinations = function(iterable, length) {
  var elements = goog.iter.toArray(iterable);
  var indexes = goog.iter.range(elements.length);
  var indexIterator = goog.iter.permutations(indexes, length);
  var sortedIndexIterator = goog.iter.filter(indexIterator, function(arr) {
    return goog.array.isSorted(arr);
  });
  var iter = new goog.iter.Iterator;
  function getIndexFromElements(index) {
    return elements[index];
  }
  iter.next = function() {
    return goog.array.map(sortedIndexIterator.next(), getIndexFromElements);
  };
  return iter;
};
/**
 * @param {(!goog.iter.Iterator<VALUE>|!goog.iter.Iterable)} iterable
 * @param {number} length
 * @return {!goog.iter.Iterator<!Array<VALUE>>}
 * @template VALUE
 */
goog.iter.combinationsWithReplacement = function(iterable, length) {
  var elements = goog.iter.toArray(iterable);
  var indexes = goog.array.range(elements.length);
  var sets = goog.array.repeat(indexes, length);
  var indexIterator = goog.iter.product.apply(undefined, sets);
  var sortedIndexIterator = goog.iter.filter(indexIterator, function(arr) {
    return goog.array.isSorted(arr);
  });
  var iter = new goog.iter.Iterator;
  function getIndexFromElements(index) {
    return elements[index];
  }
  iter.next = function() {
    return goog.array.map(/** @type {!Array<number>} */ (sortedIndexIterator.next()), getIndexFromElements);
  };
  return iter;
};

//# sourceMappingURL=goog.iter.iter.js.map
