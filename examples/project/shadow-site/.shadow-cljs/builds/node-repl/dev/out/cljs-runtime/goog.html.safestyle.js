goog.provide("goog.html.SafeStyle");
goog.require("goog.array");
goog.require("goog.asserts");
goog.require("goog.html.SafeUrl");
goog.require("goog.string.Const");
goog.require("goog.string.TypedString");
goog.require("goog.string.internal");
/**
 * @final
 * @struct
 * @constructor
 * @implements {goog.string.TypedString}
 */
goog.html.SafeStyle = function() {
  /** @private @type {string} */ this.privateDoNotAccessOrElseSafeStyleWrappedValue_ = "";
  /** @private @const @type {!Object} */ this.SAFE_STYLE_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = goog.html.SafeStyle.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_;
};
/** @const @override */ goog.html.SafeStyle.prototype.implementsGoogStringTypedString = true;
/** @private @const @type {!Object} */ goog.html.SafeStyle.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = {};
/**
 * @param {!goog.string.Const} style
 * @return {!goog.html.SafeStyle}
 */
goog.html.SafeStyle.fromConstant = function(style) {
  var styleString = goog.string.Const.unwrap(style);
  if (styleString.length === 0) {
    return goog.html.SafeStyle.EMPTY;
  }
  goog.asserts.assert(goog.string.internal.endsWith(styleString, ";"), "Last character of style string is not ';': " + styleString);
  goog.asserts.assert(goog.string.internal.contains(styleString, ":"), "Style string must contain at least one ':', to " + 'specify a "name: value" pair: ' + styleString);
  return goog.html.SafeStyle.createSafeStyleSecurityPrivateDoNotAccessOrElse(styleString);
};
/** @override */ goog.html.SafeStyle.prototype.getTypedStringValue = function() {
  return this.privateDoNotAccessOrElseSafeStyleWrappedValue_;
};
if (goog.DEBUG) {
  /** @override */ goog.html.SafeStyle.prototype.toString = function() {
    return "SafeStyle{" + this.privateDoNotAccessOrElseSafeStyleWrappedValue_ + "}";
  };
}
/**
 * @param {!goog.html.SafeStyle} safeStyle
 * @return {string}
 */
goog.html.SafeStyle.unwrap = function(safeStyle) {
  if (safeStyle instanceof goog.html.SafeStyle && safeStyle.constructor === goog.html.SafeStyle && safeStyle.SAFE_STYLE_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ === goog.html.SafeStyle.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_) {
    return safeStyle.privateDoNotAccessOrElseSafeStyleWrappedValue_;
  } else {
    goog.asserts.fail("expected object of type SafeStyle, got '" + safeStyle + "' of type " + goog.typeOf(safeStyle));
    return "type_error:SafeStyle";
  }
};
/**
 * @package
 * @param {string} style
 * @return {!goog.html.SafeStyle}
 */
goog.html.SafeStyle.createSafeStyleSecurityPrivateDoNotAccessOrElse = function(style) {
  return (new goog.html.SafeStyle).initSecurityPrivateDoNotAccessOrElse_(style);
};
/**
 * @private
 * @param {string} style
 * @return {!goog.html.SafeStyle}
 */
goog.html.SafeStyle.prototype.initSecurityPrivateDoNotAccessOrElse_ = function(style) {
  this.privateDoNotAccessOrElseSafeStyleWrappedValue_ = style;
  return this;
};
/** @const @type {!goog.html.SafeStyle} */ goog.html.SafeStyle.EMPTY = goog.html.SafeStyle.createSafeStyleSecurityPrivateDoNotAccessOrElse("");
/** @const @type {string} */ goog.html.SafeStyle.INNOCUOUS_STRING = "zClosurez";
/** @typedef {(string|!goog.string.Const|!goog.html.SafeUrl)} */ goog.html.SafeStyle.PropertyValue;
/** @typedef {!Object<string,(?goog.html.SafeStyle.PropertyValue|?Array<!goog.html.SafeStyle.PropertyValue>)>} */ goog.html.SafeStyle.PropertyMap;
/**
 * @param {goog.html.SafeStyle.PropertyMap} map
 * @return {!goog.html.SafeStyle}
 * @throws {Error}
 */
goog.html.SafeStyle.create = function(map) {
  var style = "";
  for (var name in map) {
    if (!/^[-_a-zA-Z0-9]+$/.test(name)) {
      throw new Error("Name allows only [-_a-zA-Z0-9], got: " + name);
    }
    var value = map[name];
    if (value == null) {
      continue;
    }
    if (goog.isArray(value)) {
      value = goog.array.map(value, goog.html.SafeStyle.sanitizePropertyValue_).join(" ");
    } else {
      value = goog.html.SafeStyle.sanitizePropertyValue_(value);
    }
    style += name + ":" + value + ";";
  }
  if (!style) {
    return goog.html.SafeStyle.EMPTY;
  }
  return goog.html.SafeStyle.createSafeStyleSecurityPrivateDoNotAccessOrElse(style);
};
/**
 * @private
 * @param {!goog.html.SafeStyle.PropertyValue} value
 * @return {string}
 */
goog.html.SafeStyle.sanitizePropertyValue_ = function(value) {
  if (value instanceof goog.html.SafeUrl) {
    var url = goog.html.SafeUrl.unwrap(value);
    return 'url("' + url.replace(/</g, "%3c").replace(/[\\"]/g, "\\$\x26") + '")';
  }
  var result = value instanceof goog.string.Const ? goog.string.Const.unwrap(value) : goog.html.SafeStyle.sanitizePropertyValueString_(String(value));
  if (/[{;}]/.test(result)) {
    throw new goog.asserts.AssertionError("Value does not allow [{;}], got: %s.", [result]);
  }
  return result;
};
/**
 * @private
 * @param {string} value
 * @return {string}
 */
goog.html.SafeStyle.sanitizePropertyValueString_ = function(value) {
  var valueWithoutFunctions = value.replace(goog.html.SafeStyle.FUNCTIONS_RE_, "$1").replace(goog.html.SafeStyle.FUNCTIONS_RE_, "$1").replace(goog.html.SafeStyle.URL_RE_, "url");
  if (!goog.html.SafeStyle.VALUE_RE_.test(valueWithoutFunctions)) {
    goog.asserts.fail("String value allows only " + goog.html.SafeStyle.VALUE_ALLOWED_CHARS_ + " and simple functions, got: " + value);
    return goog.html.SafeStyle.INNOCUOUS_STRING;
  } else {
    if (goog.html.SafeStyle.COMMENT_RE_.test(value)) {
      goog.asserts.fail("String value disallows comments, got: " + value);
      return goog.html.SafeStyle.INNOCUOUS_STRING;
    } else {
      if (!goog.html.SafeStyle.hasBalancedQuotes_(value)) {
        goog.asserts.fail("String value requires balanced quotes, got: " + value);
        return goog.html.SafeStyle.INNOCUOUS_STRING;
      } else {
        if (!goog.html.SafeStyle.hasBalancedSquareBrackets_(value)) {
          goog.asserts.fail("String value requires balanced square brackets and one" + " identifier per pair of brackets, got: " + value);
          return goog.html.SafeStyle.INNOCUOUS_STRING;
        }
      }
    }
  }
  return goog.html.SafeStyle.sanitizeUrl_(value);
};
/**
 * @private
 * @param {string} value
 * @return {boolean}
 */
goog.html.SafeStyle.hasBalancedQuotes_ = function(value) {
  var outsideSingle = true;
  var outsideDouble = true;
  for (var i = 0; i < value.length; i++) {
    var c = value.charAt(i);
    if (c == "'" && outsideDouble) {
      outsideSingle = !outsideSingle;
    } else {
      if (c == '"' && outsideSingle) {
        outsideDouble = !outsideDouble;
      }
    }
  }
  return outsideSingle && outsideDouble;
};
/**
 * @private
 * @param {string} value
 * @return {boolean}
 */
goog.html.SafeStyle.hasBalancedSquareBrackets_ = function(value) {
  var outside = true;
  var tokenRe = /^[-_a-zA-Z0-9]$/;
  for (var i = 0; i < value.length; i++) {
    var c = value.charAt(i);
    if (c == "]") {
      if (outside) {
        return false;
      }
      outside = true;
    } else {
      if (c == "[") {
        if (!outside) {
          return false;
        }
        outside = false;
      } else {
        if (!outside && !tokenRe.test(c)) {
          return false;
        }
      }
    }
  }
  return outside;
};
/** @private @type {string} */ goog.html.SafeStyle.VALUE_ALLOWED_CHARS_ = "[-,.\"'%_!# a-zA-Z0-9\\[\\]]";
/** @private @const @type {!RegExp} */ goog.html.SafeStyle.VALUE_RE_ = new RegExp("^" + goog.html.SafeStyle.VALUE_ALLOWED_CHARS_ + "+$");
/** @private @const @type {!RegExp} */ goog.html.SafeStyle.URL_RE_ = new RegExp("\\b(url\\([ \t\n]*)(" + "'[ -\x26(-\\[\\]-~]*'" + '|"[ !#-\\[\\]-~]*"' + "|[!#-\x26*-\\[\\]-~]*" + ")([ \t\n]*\\))", "g");
/** @private @const @type {!Array<string>} */ goog.html.SafeStyle.ALLOWED_FUNCTIONS_ = ["calc", "cubic-bezier", "fit-content", "hsl", "hsla", "matrix", "minmax", "repeat", "rgb", "rgba", "(rotate|scale|translate)(X|Y|Z|3d)?"];
/** @private @const @type {!RegExp} */ goog.html.SafeStyle.FUNCTIONS_RE_ = new RegExp("\\b(" + goog.html.SafeStyle.ALLOWED_FUNCTIONS_.join("|") + ")" + "\\([-+*/0-9a-z.%\\[\\], ]+\\)", "g");
/** @private @const @type {!RegExp} */ goog.html.SafeStyle.COMMENT_RE_ = /\/\*/;
/**
 * @private
 * @param {string} value
 * @return {string}
 */
goog.html.SafeStyle.sanitizeUrl_ = function(value) {
  return value.replace(goog.html.SafeStyle.URL_RE_, function(match, before, url, after) {
    var quote = "";
    url = url.replace(/^(['"])(.*)\1$/, function(match, start, inside) {
      quote = start;
      return inside;
    });
    var sanitized = goog.html.SafeUrl.sanitize(url).getTypedStringValue();
    return before + quote + sanitized + quote + after;
  });
};
/**
 * @param {...(!goog.html.SafeStyle|!Array<!goog.html.SafeStyle>)} var_args
 * @return {!goog.html.SafeStyle}
 */
goog.html.SafeStyle.concat = function(var_args) {
  var style = "";
  /**
   * @param {(!goog.html.SafeStyle|!Array<!goog.html.SafeStyle>)} argument
   */
  var addArgument = function(argument) {
    if (goog.isArray(argument)) {
      goog.array.forEach(argument, addArgument);
    } else {
      style += goog.html.SafeStyle.unwrap(argument);
    }
  };
  goog.array.forEach(arguments, addArgument);
  if (!style) {
    return goog.html.SafeStyle.EMPTY;
  }
  return goog.html.SafeStyle.createSafeStyleSecurityPrivateDoNotAccessOrElse(style);
};

//# sourceMappingURL=goog.html.safestyle.js.map
