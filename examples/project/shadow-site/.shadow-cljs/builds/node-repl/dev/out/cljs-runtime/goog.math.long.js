goog.loadModule(function(exports) {
  "use strict";
  goog.module("goog.math.Long");
  goog.module.declareLegacyNamespace();
  /** @const */ var asserts = goog.require("goog.asserts");
  /** @const */ var reflect = goog.require("goog.reflect");
  /**
   * @final
   * @struct
   * @constructor
   * @param {number} low
   * @param {number} high
   */
  var Long = function(low, high) {
    /** @private @const @type {number} */ this.low_ = low | 0;
    /** @private @const @type {number} */ this.high_ = high | 0;
  };
  /**
   * @return {number}
   */
  Long.prototype.toInt = function() {
    return this.low_;
  };
  /**
   * @return {number}
   */
  Long.prototype.toNumber = function() {
    return this.high_ * TWO_PWR_32_DBL_ + this.getLowBitsUnsigned();
  };
  /**
   * @return {boolean}
   */
  Long.prototype.isSafeInteger = function() {
    var top11Bits = this.high_ >> 21;
    return top11Bits == 0 || top11Bits == -1 && !(this.low_ == 0 && this.high_ == (4292870144 | 0));
  };
  /**
   * @param {number=} opt_radix
   * @return {string}
   * @override
   */
  Long.prototype.toString = function(opt_radix) {
    var radix = opt_radix || 10;
    if (radix < 2 || 36 < radix) {
      throw new Error("radix out of range: " + radix);
    }
    if (this.isSafeInteger()) {
      var asNumber = this.toNumber();
      return radix == 10 ? "" + asNumber : asNumber.toString(radix);
    }
    var safeDigits = 14 - (radix >> 2);
    var radixPowSafeDigits = Math.pow(radix, safeDigits);
    var radixToPower = Long.fromBits(radixPowSafeDigits, radixPowSafeDigits / TWO_PWR_32_DBL_);
    var remDiv = this.div(radixToPower);
    var val = Math.abs(this.subtract(remDiv.multiply(radixToPower)).toNumber());
    var digits = radix == 10 ? "" + val : val.toString(radix);
    if (digits.length < safeDigits) {
      digits = "0000000000000".substr(digits.length - safeDigits) + digits;
    }
    val = remDiv.toNumber();
    return (radix == 10 ? val : val.toString(radix)) + digits;
  };
  /**
   * @return {number}
   */
  Long.prototype.getHighBits = function() {
    return this.high_;
  };
  /**
   * @return {number}
   */
  Long.prototype.getLowBits = function() {
    return this.low_;
  };
  /**
   * @return {number}
   */
  Long.prototype.getLowBitsUnsigned = function() {
    return this.low_ >>> 0;
  };
  /**
   * @return {number}
   */
  Long.prototype.getNumBitsAbs = function() {
    if (this.isNegative()) {
      if (this.equals(Long.getMinValue())) {
        return 64;
      } else {
        return this.negate().getNumBitsAbs();
      }
    } else {
      var val = this.high_ != 0 ? this.high_ : this.low_;
      for (var bit = 31; bit > 0; bit--) {
        if ((val & 1 << bit) != 0) {
          break;
        }
      }
      return this.high_ != 0 ? bit + 33 : bit + 1;
    }
  };
  /**
   * @return {boolean}
   */
  Long.prototype.isZero = function() {
    return this.low_ == 0 && this.high_ == 0;
  };
  /**
   * @return {boolean}
   */
  Long.prototype.isNegative = function() {
    return this.high_ < 0;
  };
  /**
   * @return {boolean}
   */
  Long.prototype.isOdd = function() {
    return (this.low_ & 1) == 1;
  };
  /**
   * @param {?Long} other
   * @return {boolean}
   */
  Long.prototype.equals = function(other) {
    return this.low_ == other.low_ && this.high_ == other.high_;
  };
  /**
   * @param {?Long} other
   * @return {boolean}
   */
  Long.prototype.notEquals = function(other) {
    return !this.equals(other);
  };
  /**
   * @param {?Long} other
   * @return {boolean}
   */
  Long.prototype.lessThan = function(other) {
    return this.compare(other) < 0;
  };
  /**
   * @param {?Long} other
   * @return {boolean}
   */
  Long.prototype.lessThanOrEqual = function(other) {
    return this.compare(other) <= 0;
  };
  /**
   * @param {?Long} other
   * @return {boolean}
   */
  Long.prototype.greaterThan = function(other) {
    return this.compare(other) > 0;
  };
  /**
   * @param {?Long} other
   * @return {boolean}
   */
  Long.prototype.greaterThanOrEqual = function(other) {
    return this.compare(other) >= 0;
  };
  /**
   * @param {?Long} other
   * @return {number}
   */
  Long.prototype.compare = function(other) {
    if (this.high_ == other.high_) {
      if (this.low_ == other.low_) {
        return 0;
      }
      return this.getLowBitsUnsigned() > other.getLowBitsUnsigned() ? 1 : -1;
    }
    return this.high_ > other.high_ ? 1 : -1;
  };
  /**
   * @return {!Long}
   */
  Long.prototype.negate = function() {
    var negLow = ~this.low_ + 1 | 0;
    var overflowFromLow = !negLow;
    var negHigh = ~this.high_ + overflowFromLow | 0;
    return Long.fromBits(negLow, negHigh);
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.add = function(other) {
    var a48 = this.high_ >>> 16;
    var a32 = this.high_ & 65535;
    var a16 = this.low_ >>> 16;
    var a00 = this.low_ & 65535;
    var b48 = other.high_ >>> 16;
    var b32 = other.high_ & 65535;
    var b16 = other.low_ >>> 16;
    var b00 = other.low_ & 65535;
    var c48 = 0, c32 = 0, c16 = 0, c00 = 0;
    c00 += a00 + b00;
    c16 += c00 >>> 16;
    c00 &= 65535;
    c16 += a16 + b16;
    c32 += c16 >>> 16;
    c16 &= 65535;
    c32 += a32 + b32;
    c48 += c32 >>> 16;
    c32 &= 65535;
    c48 += a48 + b48;
    c48 &= 65535;
    return Long.fromBits(c16 << 16 | c00, c48 << 16 | c32);
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.subtract = function(other) {
    return this.add(other.negate());
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.multiply = function(other) {
    if (this.isZero()) {
      return this;
    }
    if (other.isZero()) {
      return other;
    }
    var a48 = this.high_ >>> 16;
    var a32 = this.high_ & 65535;
    var a16 = this.low_ >>> 16;
    var a00 = this.low_ & 65535;
    var b48 = other.high_ >>> 16;
    var b32 = other.high_ & 65535;
    var b16 = other.low_ >>> 16;
    var b00 = other.low_ & 65535;
    var c48 = 0, c32 = 0, c16 = 0, c00 = 0;
    c00 += a00 * b00;
    c16 += c00 >>> 16;
    c00 &= 65535;
    c16 += a16 * b00;
    c32 += c16 >>> 16;
    c16 &= 65535;
    c16 += a00 * b16;
    c32 += c16 >>> 16;
    c16 &= 65535;
    c32 += a32 * b00;
    c48 += c32 >>> 16;
    c32 &= 65535;
    c32 += a16 * b16;
    c48 += c32 >>> 16;
    c32 &= 65535;
    c32 += a00 * b32;
    c48 += c32 >>> 16;
    c32 &= 65535;
    c48 += a48 * b00 + a32 * b16 + a16 * b32 + a00 * b48;
    c48 &= 65535;
    return Long.fromBits(c16 << 16 | c00, c48 << 16 | c32);
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.div = function(other) {
    if (other.isZero()) {
      throw new Error("division by zero");
    }
    if (this.isNegative()) {
      if (this.equals(Long.getMinValue())) {
        if (other.equals(Long.getOne()) || other.equals(Long.getNegOne())) {
          return Long.getMinValue();
        }
        if (other.equals(Long.getMinValue())) {
          return Long.getOne();
        }
        var halfThis = this.shiftRight(1);
        var approx = halfThis.div(other).shiftLeft(1);
        if (approx.equals(Long.getZero())) {
          return other.isNegative() ? Long.getOne() : Long.getNegOne();
        }
        var rem = this.subtract(other.multiply(approx));
        var result = approx.add(rem.div(other));
        return result;
      }
      if (other.isNegative()) {
        return this.negate().div(other.negate());
      }
      return this.negate().div(other).negate();
    }
    if (this.isZero()) {
      return Long.getZero();
    }
    if (other.isNegative()) {
      if (other.equals(Long.getMinValue())) {
        return Long.getZero();
      }
      return this.div(other.negate()).negate();
    }
    var res = Long.getZero();
    var rem = this;
    while (rem.greaterThanOrEqual(other)) {
      var approx = Math.max(1, Math.floor(rem.toNumber() / other.toNumber()));
      var log2 = Math.ceil(Math.log(approx) / Math.LN2);
      var delta = log2 <= 48 ? 1 : Math.pow(2, log2 - 48);
      var approxRes = Long.fromNumber(approx);
      var approxRem = approxRes.multiply(other);
      while (approxRem.isNegative() || approxRem.greaterThan(rem)) {
        approx -= delta;
        approxRes = Long.fromNumber(approx);
        approxRem = approxRes.multiply(other);
      }
      if (approxRes.isZero()) {
        approxRes = Long.getOne();
      }
      res = res.add(approxRes);
      rem = rem.subtract(approxRem);
    }
    return res;
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.modulo = function(other) {
    return this.subtract(this.div(other).multiply(other));
  };
  /**
   * @return {!Long}
   */
  Long.prototype.not = function() {
    return Long.fromBits(~this.low_, ~this.high_);
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.and = function(other) {
    return Long.fromBits(this.low_ & other.low_, this.high_ & other.high_);
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.or = function(other) {
    return Long.fromBits(this.low_ | other.low_, this.high_ | other.high_);
  };
  /**
   * @param {?Long} other
   * @return {!Long}
   */
  Long.prototype.xor = function(other) {
    return Long.fromBits(this.low_ ^ other.low_, this.high_ ^ other.high_);
  };
  /**
   * @param {number} numBits
   * @return {!Long}
   */
  Long.prototype.shiftLeft = function(numBits) {
    numBits &= 63;
    if (numBits == 0) {
      return this;
    } else {
      var low = this.low_;
      if (numBits < 32) {
        var high = this.high_;
        return Long.fromBits(low << numBits, high << numBits | low >>> 32 - numBits);
      } else {
        return Long.fromBits(0, low << numBits - 32);
      }
    }
  };
  /**
   * @param {number} numBits
   * @return {!Long}
   */
  Long.prototype.shiftRight = function(numBits) {
    numBits &= 63;
    if (numBits == 0) {
      return this;
    } else {
      var high = this.high_;
      if (numBits < 32) {
        var low = this.low_;
        return Long.fromBits(low >>> numBits | high << 32 - numBits, high >> numBits);
      } else {
        return Long.fromBits(high >> numBits - 32, high >= 0 ? 0 : -1);
      }
    }
  };
  /**
   * @param {number} numBits
   * @return {!Long}
   */
  Long.prototype.shiftRightUnsigned = function(numBits) {
    numBits &= 63;
    if (numBits == 0) {
      return this;
    } else {
      var high = this.high_;
      if (numBits < 32) {
        var low = this.low_;
        return Long.fromBits(low >>> numBits | high << 32 - numBits, high >>> numBits);
      } else {
        if (numBits == 32) {
          return Long.fromBits(high, 0);
        } else {
          return Long.fromBits(high >>> numBits - 32, 0);
        }
      }
    }
  };
  /**
   * @param {number} value
   * @return {!Long}
   */
  Long.fromInt = function(value) {
    var intValue = value | 0;
    asserts.assert(value === intValue, "value should be a 32-bit integer");
    if (-128 <= intValue && intValue < 128) {
      return getCachedIntValue_(intValue);
    } else {
      return new Long(intValue, intValue < 0 ? -1 : 0);
    }
  };
  /**
   * @param {number} value
   * @return {!Long}
   */
  Long.fromNumber = function(value) {
    if (value > 0) {
      if (value >= TWO_PWR_63_DBL_) {
        return Long.getMaxValue();
      }
      return new Long(value, value / TWO_PWR_32_DBL_);
    } else {
      if (value < 0) {
        if (value <= -TWO_PWR_63_DBL_) {
          return Long.getMinValue();
        }
        return (new Long(-value, -value / TWO_PWR_32_DBL_)).negate();
      } else {
        return Long.getZero();
      }
    }
  };
  /**
   * @param {number} lowBits
   * @param {number} highBits
   * @return {!Long}
   */
  Long.fromBits = function(lowBits, highBits) {
    return new Long(lowBits, highBits);
  };
  /**
   * @param {string} str
   * @param {number=} opt_radix
   * @return {!Long}
   */
  Long.fromString = function(str, opt_radix) {
    if (str.charAt(0) == "-") {
      return Long.fromString(str.substring(1), opt_radix).negate();
    }
    var numberValue = parseInt(str, opt_radix || 10);
    if (numberValue <= MAX_SAFE_INTEGER_) {
      return new Long(numberValue % TWO_PWR_32_DBL_ | 0, numberValue / TWO_PWR_32_DBL_ | 0);
    }
    if (str.length == 0) {
      throw new Error("number format error: empty string");
    }
    if (str.indexOf("-") >= 0) {
      throw new Error('number format error: interior "-" character: ' + str);
    }
    var radix = opt_radix || 10;
    if (radix < 2 || 36 < radix) {
      throw new Error("radix out of range: " + radix);
    }
    var radixToPower = Long.fromNumber(Math.pow(radix, 8));
    var result = Long.getZero();
    for (var i = 0; i < str.length; i += 8) {
      var size = Math.min(8, str.length - i);
      var value = parseInt(str.substring(i, i + size), radix);
      if (size < 8) {
        var power = Long.fromNumber(Math.pow(radix, size));
        result = result.multiply(power).add(Long.fromNumber(value));
      } else {
        result = result.multiply(radixToPower);
        result = result.add(Long.fromNumber(value));
      }
    }
    return result;
  };
  /**
   * @param {string} str
   * @param {number=} opt_radix
   * @return {boolean}
   */
  Long.isStringInRange = function(str, opt_radix) {
    var radix = opt_radix || 10;
    if (radix < 2 || 36 < radix) {
      throw new Error("radix out of range: " + radix);
    }
    var extremeValue = str.charAt(0) == "-" ? MIN_VALUE_FOR_RADIX_[radix] : MAX_VALUE_FOR_RADIX_[radix];
    if (str.length < extremeValue.length) {
      return true;
    } else {
      if (str.length == extremeValue.length && str <= extremeValue) {
        return true;
      } else {
        return false;
      }
    }
  };
  /**
   * @public
   * @return {!Long}
   */
  Long.getZero = function() {
    return ZERO_;
  };
  /**
   * @public
   * @return {!Long}
   */
  Long.getOne = function() {
    return ONE_;
  };
  /**
   * @public
   * @return {!Long}
   */
  Long.getNegOne = function() {
    return NEG_ONE_;
  };
  /**
   * @public
   * @return {!Long}
   */
  Long.getMaxValue = function() {
    return MAX_VALUE_;
  };
  /**
   * @public
   * @return {!Long}
   */
  Long.getMinValue = function() {
    return MIN_VALUE_;
  };
  /**
   * @public
   * @return {!Long}
   */
  Long.getTwoPwr24 = function() {
    return TWO_PWR_24_;
  };
  exports = Long;
  /** @private @const @type {!Object<number,!Long>} */ var IntCache_ = {};
  /**
   * @private
   * @param {number} value
   * @return {!Long}
   */
  function getCachedIntValue_(value) {
    return reflect.cache(IntCache_, value, function(val) {
      return new Long(val, val < 0 ? -1 : 0);
    });
  }
  /** @private @const @type {!Array<string>} */ var MAX_VALUE_FOR_RADIX_ = ["", "", "111111111111111111111111111111111111111111111111111111111111111", "2021110011022210012102010021220101220221", "13333333333333333333333333333333", "1104332401304422434310311212", "1540241003031030222122211", "22341010611245052052300", "777777777777777777777", "67404283172107811827", "9223372036854775807", "1728002635214590697", "41a792678515120367", "10b269549075433c37", "4340724c6c71dc7a7", "160e2ad3246366807", "7fffffffffffffff", 
  "33d3d8307b214008", "16agh595df825fa7", "ba643dci0ffeehh", "5cbfjia3fh26ja7", "2heiciiie82dh97", "1adaibb21dckfa7", "i6k448cf4192c2", "acd772jnc9l0l7", "64ie1focnn5g77", "3igoecjbmca687", "27c48l5b37oaop", "1bk39f3ah3dmq7", "q1se8f0m04isb", "hajppbc1fc207", "bm03i95hia437", "7vvvvvvvvvvvv", "5hg4ck9jd4u37", "3tdtk1v8j6tpp", "2pijmikexrxp7", "1y2p0ij32e8e7"];
  /** @private @const @type {!Array<string>} */ var MIN_VALUE_FOR_RADIX_ = ["", "", "-1000000000000000000000000000000000000000000000000000000000000000", "-2021110011022210012102010021220101220222", "-20000000000000000000000000000000", "-1104332401304422434310311213", "-1540241003031030222122212", "-22341010611245052052301", "-1000000000000000000000", "-67404283172107811828", "-9223372036854775808", "-1728002635214590698", "-41a792678515120368", "-10b269549075433c38", "-4340724c6c71dc7a8", "-160e2ad3246366808", 
  "-8000000000000000", "-33d3d8307b214009", "-16agh595df825fa8", "-ba643dci0ffeehi", "-5cbfjia3fh26ja8", "-2heiciiie82dh98", "-1adaibb21dckfa8", "-i6k448cf4192c3", "-acd772jnc9l0l8", "-64ie1focnn5g78", "-3igoecjbmca688", "-27c48l5b37oaoq", "-1bk39f3ah3dmq8", "-q1se8f0m04isc", "-hajppbc1fc208", "-bm03i95hia438", "-8000000000000", "-5hg4ck9jd4u38", "-3tdtk1v8j6tpq", "-2pijmikexrxp8", "-1y2p0ij32e8e8"];
  /** @private @const @type {number} */ var MAX_SAFE_INTEGER_ = 9007199254740991;
  /** @private @const @type {number} */ var TWO_PWR_32_DBL_ = 4294967296;
  /** @private @const @type {number} */ var TWO_PWR_63_DBL_ = 0x7fffffffffffffff;
  /** @private @const @type {!Long} */ var ZERO_ = Long.fromBits(0, 0);
  /** @private @const @type {!Long} */ var ONE_ = Long.fromBits(1, 0);
  /** @private @const @type {!Long} */ var NEG_ONE_ = Long.fromBits(-1, -1);
  /** @private @const @type {!Long} */ var MAX_VALUE_ = Long.fromBits(4294967295, 2147483647);
  /** @private @const @type {!Long} */ var MIN_VALUE_ = Long.fromBits(0, 2147483648);
  /** @private @const @type {!Long} */ var TWO_PWR_24_ = Long.fromBits(1 << 24, 0);
  return exports;
});

//# sourceMappingURL=goog.math.long.js.map
