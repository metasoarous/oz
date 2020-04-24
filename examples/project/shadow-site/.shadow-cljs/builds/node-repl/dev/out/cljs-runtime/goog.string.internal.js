goog.provide("goog.string.internal");
/**
 * @param {string} str
 * @param {string} prefix
 * @return {boolean}
 */
goog.string.internal.startsWith = function(str, prefix) {
  return str.lastIndexOf(prefix, 0) == 0;
};
/**
 * @param {string} str
 * @param {string} suffix
 * @return {boolean}
 */
goog.string.internal.endsWith = function(str, suffix) {
  /** @const */ var l = str.length - suffix.length;
  return l >= 0 && str.indexOf(suffix, l) == l;
};
/**
 * @param {string} str
 * @param {string} prefix
 * @return {boolean}
 */
goog.string.internal.caseInsensitiveStartsWith = function(str, prefix) {
  return goog.string.internal.caseInsensitiveCompare(prefix, str.substr(0, prefix.length)) == 0;
};
/**
 * @param {string} str
 * @param {string} suffix
 * @return {boolean}
 */
goog.string.internal.caseInsensitiveEndsWith = function(str, suffix) {
  return goog.string.internal.caseInsensitiveCompare(suffix, str.substr(str.length - suffix.length, suffix.length)) == 0;
};
/**
 * @param {string} str1
 * @param {string} str2
 * @return {boolean}
 */
goog.string.internal.caseInsensitiveEquals = function(str1, str2) {
  return str1.toLowerCase() == str2.toLowerCase();
};
/**
 * @param {string} str
 * @return {boolean}
 */
goog.string.internal.isEmptyOrWhitespace = function(str) {
  return /^[\s\xa0]*$/.test(str);
};
/**
 * @param {string} str
 * @return {string}
 */
goog.string.internal.trim = goog.TRUSTED_SITE && String.prototype.trim ? function(str) {
  return str.trim();
} : function(str) {
  return /^[\s\xa0]*([\s\S]*?)[\s\xa0]*$/.exec(str)[1];
};
/**
 * @param {string} str1
 * @param {string} str2
 * @return {number}
 */
goog.string.internal.caseInsensitiveCompare = function(str1, str2) {
  /** @const */ var test1 = String(str1).toLowerCase();
  /** @const */ var test2 = String(str2).toLowerCase();
  if (test1 < test2) {
    return -1;
  } else {
    if (test1 == test2) {
      return 0;
    } else {
      return 1;
    }
  }
};
/**
 * @param {string} str
 * @param {boolean=} opt_xml
 * @return {string}
 */
goog.string.internal.newLineToBr = function(str, opt_xml) {
  return str.replace(/(\r\n|\r|\n)/g, opt_xml ? "\x3cbr /\x3e" : "\x3cbr\x3e");
};
/**
 * @param {string} str
 * @param {boolean=} opt_isLikelyToContainHtmlChars
 * @return {string}
 */
goog.string.internal.htmlEscape = function(str, opt_isLikelyToContainHtmlChars) {
  if (opt_isLikelyToContainHtmlChars) {
    str = str.replace(goog.string.internal.AMP_RE_, "\x26amp;").replace(goog.string.internal.LT_RE_, "\x26lt;").replace(goog.string.internal.GT_RE_, "\x26gt;").replace(goog.string.internal.QUOT_RE_, "\x26quot;").replace(goog.string.internal.SINGLE_QUOTE_RE_, "\x26#39;").replace(goog.string.internal.NULL_RE_, "\x26#0;");
    return str;
  } else {
    if (!goog.string.internal.ALL_RE_.test(str)) {
      return str;
    }
    if (str.indexOf("\x26") != -1) {
      str = str.replace(goog.string.internal.AMP_RE_, "\x26amp;");
    }
    if (str.indexOf("\x3c") != -1) {
      str = str.replace(goog.string.internal.LT_RE_, "\x26lt;");
    }
    if (str.indexOf("\x3e") != -1) {
      str = str.replace(goog.string.internal.GT_RE_, "\x26gt;");
    }
    if (str.indexOf('"') != -1) {
      str = str.replace(goog.string.internal.QUOT_RE_, "\x26quot;");
    }
    if (str.indexOf("'") != -1) {
      str = str.replace(goog.string.internal.SINGLE_QUOTE_RE_, "\x26#39;");
    }
    if (str.indexOf("\x00") != -1) {
      str = str.replace(goog.string.internal.NULL_RE_, "\x26#0;");
    }
    return str;
  }
};
/** @private @const @type {!RegExp} */ goog.string.internal.AMP_RE_ = /&/g;
/** @private @const @type {!RegExp} */ goog.string.internal.LT_RE_ = /</g;
/** @private @const @type {!RegExp} */ goog.string.internal.GT_RE_ = />/g;
/** @private @const @type {!RegExp} */ goog.string.internal.QUOT_RE_ = /"/g;
/** @private @const @type {!RegExp} */ goog.string.internal.SINGLE_QUOTE_RE_ = /'/g;
/** @private @const @type {!RegExp} */ goog.string.internal.NULL_RE_ = /\x00/g;
/** @private @const @type {!RegExp} */ goog.string.internal.ALL_RE_ = /[\x00&<>"']/;
/**
 * @param {string} str
 * @param {boolean=} opt_xml
 * @return {string}
 */
goog.string.internal.whitespaceEscape = function(str, opt_xml) {
  return goog.string.internal.newLineToBr(str.replace(/  /g, " \x26#160;"), opt_xml);
};
/**
 * @param {string} str
 * @param {string} subString
 * @return {boolean}
 */
goog.string.internal.contains = function(str, subString) {
  return str.indexOf(subString) != -1;
};
/**
 * @param {string} str
 * @param {string} subString
 * @return {boolean}
 */
goog.string.internal.caseInsensitiveContains = function(str, subString) {
  return goog.string.internal.contains(str.toLowerCase(), subString.toLowerCase());
};
/**
 * @param {(string|number)} version1
 * @param {(string|number)} version2
 * @return {number}
 */
goog.string.internal.compareVersions = function(version1, version2) {
  var order = 0;
  /** @const */ var v1Subs = goog.string.internal.trim(String(version1)).split(".");
  /** @const */ var v2Subs = goog.string.internal.trim(String(version2)).split(".");
  /** @const */ var subCount = Math.max(v1Subs.length, v2Subs.length);
  for (var subIdx = 0; order == 0 && subIdx < subCount; subIdx++) {
    var v1Sub = v1Subs[subIdx] || "";
    var v2Sub = v2Subs[subIdx] || "";
    do {
      /** @const */ var v1Comp = /(\d*)(\D*)(.*)/.exec(v1Sub) || ["", "", "", ""];
      /** @const */ var v2Comp = /(\d*)(\D*)(.*)/.exec(v2Sub) || ["", "", "", ""];
      if (v1Comp[0].length == 0 && v2Comp[0].length == 0) {
        break;
      }
      /** @const */ var v1CompNum = v1Comp[1].length == 0 ? 0 : parseInt(v1Comp[1], 10);
      /** @const */ var v2CompNum = v2Comp[1].length == 0 ? 0 : parseInt(v2Comp[1], 10);
      order = goog.string.internal.compareElements_(v1CompNum, v2CompNum) || goog.string.internal.compareElements_(v1Comp[2].length == 0, v2Comp[2].length == 0) || goog.string.internal.compareElements_(v1Comp[2], v2Comp[2]);
      v1Sub = v1Comp[3];
      v2Sub = v2Comp[3];
    } while (order == 0);
  }
  return order;
};
/**
 * @private
 * @param {(string|number|boolean)} left
 * @param {(string|number|boolean)} right
 * @return {number}
 */
goog.string.internal.compareElements_ = function(left, right) {
  if (left < right) {
    return -1;
  } else {
    if (left > right) {
      return 1;
    }
  }
  return 0;
};

//# sourceMappingURL=goog.string.internal.js.map
