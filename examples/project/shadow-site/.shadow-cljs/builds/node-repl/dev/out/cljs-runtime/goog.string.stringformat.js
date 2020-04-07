goog.provide("goog.string.format");
goog.require("goog.string");
/**
 * @param {string} formatString
 * @param {...(string|number|undefined)} var_args
 * @return {string}
 */
goog.string.format = function(formatString, var_args) {
  var args = Array.prototype.slice.call(arguments);
  var template = args.shift();
  if (typeof template == "undefined") {
    throw new Error("[goog.string.format] Template required");
  }
  var formatRe = /%([0\- \+]*)(\d+)?(\.(\d+))?([%sfdiu])/g;
  /**
   * @param {string} match
   * @param {string} flags
   * @param {string} width
   * @param {string} dotp
   * @param {string} precision
   * @param {string} type
   * @param {string} offset
   * @param {string} wholeString
   * @return {string}
   */
  function replacerDemuxer(match, flags, width, dotp, precision, type, offset, wholeString) {
    if (type == "%") {
      return "%";
    }
    var value = args.shift();
    if (typeof value == "undefined") {
      throw new Error("[goog.string.format] Not enough arguments");
    }
    arguments[0] = value;
    return goog.string.format.demuxes_[type].apply(null, arguments);
  }
  return template.replace(formatRe, replacerDemuxer);
};
/** @private @type {!Object} */ goog.string.format.demuxes_ = {};
/**
 * @param {string} value
 * @param {string} flags
 * @param {string} width
 * @param {string} dotp
 * @param {string} precision
 * @param {string} type
 * @param {string} offset
 * @param {string} wholeString
 * @return {string}
 */
goog.string.format.demuxes_["s"] = function(value, flags, width, dotp, precision, type, offset, wholeString) {
  var replacement = value;
  if (isNaN(width) || width == "" || replacement.length >= Number(width)) {
    return replacement;
  }
  if (flags.indexOf("-", 0) > -1) {
    replacement = replacement + goog.string.repeat(" ", Number(width) - replacement.length);
  } else {
    replacement = goog.string.repeat(" ", Number(width) - replacement.length) + replacement;
  }
  return replacement;
};
/**
 * @param {string} value
 * @param {string} flags
 * @param {string} width
 * @param {string} dotp
 * @param {string} precision
 * @param {string} type
 * @param {string} offset
 * @param {string} wholeString
 * @return {string}
 */
goog.string.format.demuxes_["f"] = function(value, flags, width, dotp, precision, type, offset, wholeString) {
  var replacement = value.toString();
  if (!(isNaN(precision) || precision == "")) {
    replacement = parseFloat(value).toFixed(precision);
  }
  var sign;
  if (Number(value) < 0) {
    sign = "-";
  } else {
    if (flags.indexOf("+") >= 0) {
      sign = "+";
    } else {
      if (flags.indexOf(" ") >= 0) {
        sign = " ";
      } else {
        sign = "";
      }
    }
  }
  if (Number(value) >= 0) {
    replacement = sign + replacement;
  }
  if (isNaN(width) || replacement.length >= Number(width)) {
    return replacement;
  }
  replacement = isNaN(precision) ? Math.abs(Number(value)).toString() : Math.abs(Number(value)).toFixed(precision);
  var padCount = Number(width) - replacement.length - sign.length;
  if (flags.indexOf("-", 0) >= 0) {
    replacement = sign + replacement + goog.string.repeat(" ", padCount);
  } else {
    var paddingChar = flags.indexOf("0", 0) >= 0 ? "0" : " ";
    replacement = sign + goog.string.repeat(paddingChar, padCount) + replacement;
  }
  return replacement;
};
/**
 * @param {string} value
 * @param {string} flags
 * @param {string} width
 * @param {string} dotp
 * @param {string} precision
 * @param {string} type
 * @param {string} offset
 * @param {string} wholeString
 * @return {string}
 */
goog.string.format.demuxes_["d"] = function(value, flags, width, dotp, precision, type, offset, wholeString) {
  return goog.string.format.demuxes_["f"](parseInt(value, 10), flags, width, dotp, 0, type, offset, wholeString);
};
goog.string.format.demuxes_["i"] = goog.string.format.demuxes_["d"];
goog.string.format.demuxes_["u"] = goog.string.format.demuxes_["d"];

//# sourceMappingURL=goog.string.stringformat.js.map
