goog.provide("goog.html.SafeHtml");
goog.require("goog.array");
goog.require("goog.asserts");
goog.require("goog.dom.TagName");
goog.require("goog.dom.tags");
goog.require("goog.html.SafeScript");
goog.require("goog.html.SafeStyle");
goog.require("goog.html.SafeStyleSheet");
goog.require("goog.html.SafeUrl");
goog.require("goog.html.TrustedResourceUrl");
goog.require("goog.html.trustedtypes");
goog.require("goog.i18n.bidi.Dir");
goog.require("goog.i18n.bidi.DirectionalString");
goog.require("goog.labs.userAgent.browser");
goog.require("goog.object");
goog.require("goog.string.Const");
goog.require("goog.string.TypedString");
goog.require("goog.string.internal");
/**
 * @final
 * @struct
 * @constructor
 * @implements {goog.i18n.bidi.DirectionalString}
 * @implements {goog.string.TypedString}
 */
goog.html.SafeHtml = function() {
  /** @private @type {(!TrustedHTML|string)} */ this.privateDoNotAccessOrElseSafeHtmlWrappedValue_ = "";
  /** @private @const @type {!Object} */ this.SAFE_HTML_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = goog.html.SafeHtml.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_;
  /** @private @type {?goog.i18n.bidi.Dir} */ this.dir_ = null;
};
/** @define {boolean} */ goog.html.SafeHtml.ENABLE_ERROR_MESSAGES = goog.define("goog.html.SafeHtml.ENABLE_ERROR_MESSAGES", goog.DEBUG);
/** @define {boolean} */ goog.html.SafeHtml.SUPPORT_STYLE_ATTRIBUTE = goog.define("goog.html.SafeHtml.SUPPORT_STYLE_ATTRIBUTE", true);
/** @const @override */ goog.html.SafeHtml.prototype.implementsGoogI18nBidiDirectionalString = true;
/** @override */ goog.html.SafeHtml.prototype.getDirection = function() {
  return this.dir_;
};
/** @const @override */ goog.html.SafeHtml.prototype.implementsGoogStringTypedString = true;
/** @override */ goog.html.SafeHtml.prototype.getTypedStringValue = function() {
  return this.privateDoNotAccessOrElseSafeHtmlWrappedValue_.toString();
};
if (goog.DEBUG) {
  /** @override */ goog.html.SafeHtml.prototype.toString = function() {
    return "SafeHtml{" + this.privateDoNotAccessOrElseSafeHtmlWrappedValue_ + "}";
  };
}
/**
 * @param {!goog.html.SafeHtml} safeHtml
 * @return {string}
 */
goog.html.SafeHtml.unwrap = function(safeHtml) {
  return goog.html.SafeHtml.unwrapTrustedHTML(safeHtml).toString();
};
/**
 * @param {!goog.html.SafeHtml} safeHtml
 * @return {(!TrustedHTML|string)}
 */
goog.html.SafeHtml.unwrapTrustedHTML = function(safeHtml) {
  if (safeHtml instanceof goog.html.SafeHtml && safeHtml.constructor === goog.html.SafeHtml && safeHtml.SAFE_HTML_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ === goog.html.SafeHtml.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_) {
    return safeHtml.privateDoNotAccessOrElseSafeHtmlWrappedValue_;
  } else {
    goog.asserts.fail("expected object of type SafeHtml, got '" + safeHtml + "' of type " + goog.typeOf(safeHtml));
    return "type_error:SafeHtml";
  }
};
/** @private @typedef {(string|number|boolean|!goog.string.TypedString|!goog.i18n.bidi.DirectionalString)} */ goog.html.SafeHtml.TextOrHtml_;
/**
 * @param {!goog.html.SafeHtml.TextOrHtml_} textOrHtml
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.htmlEscape = function(textOrHtml) {
  if (textOrHtml instanceof goog.html.SafeHtml) {
    return textOrHtml;
  }
  var textIsObject = typeof textOrHtml == "object";
  var dir = null;
  if (textIsObject && textOrHtml.implementsGoogI18nBidiDirectionalString) {
    dir = /** @type {!goog.i18n.bidi.DirectionalString} */ (textOrHtml).getDirection();
  }
  var textAsString;
  if (textIsObject && textOrHtml.implementsGoogStringTypedString) {
    textAsString = /** @type {!goog.string.TypedString} */ (textOrHtml).getTypedStringValue();
  } else {
    textAsString = String(textOrHtml);
  }
  return goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse(goog.string.internal.htmlEscape(textAsString), dir);
};
/**
 * @param {!goog.html.SafeHtml.TextOrHtml_} textOrHtml
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.htmlEscapePreservingNewlines = function(textOrHtml) {
  if (textOrHtml instanceof goog.html.SafeHtml) {
    return textOrHtml;
  }
  var html = goog.html.SafeHtml.htmlEscape(textOrHtml);
  return goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse(goog.string.internal.newLineToBr(goog.html.SafeHtml.unwrap(html)), html.getDirection());
};
/**
 * @param {!goog.html.SafeHtml.TextOrHtml_} textOrHtml
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.htmlEscapePreservingNewlinesAndSpaces = function(textOrHtml) {
  if (textOrHtml instanceof goog.html.SafeHtml) {
    return textOrHtml;
  }
  var html = goog.html.SafeHtml.htmlEscape(textOrHtml);
  return goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse(goog.string.internal.whitespaceEscape(goog.html.SafeHtml.unwrap(html)), html.getDirection());
};
/**
 * @param {!goog.html.SafeHtml.TextOrHtml_} textOrHtml
 * @return {!goog.html.SafeHtml}
 * @deprecated Use goog.html.SafeHtml.htmlEscape.
 */
goog.html.SafeHtml.from = goog.html.SafeHtml.htmlEscape;
/** @private @const */ goog.html.SafeHtml.VALID_NAMES_IN_TAG_ = /^[a-zA-Z0-9-]+$/;
/** @private @const @type {!Object<string,boolean>} */ goog.html.SafeHtml.URL_ATTRIBUTES_ = goog.object.createSet("action", "cite", "data", "formaction", "href", "manifest", "poster", "src");
/** @private @const @type {!Object<string,boolean>} */ goog.html.SafeHtml.NOT_ALLOWED_TAG_NAMES_ = goog.object.createSet(goog.dom.TagName.APPLET, goog.dom.TagName.BASE, goog.dom.TagName.EMBED, goog.dom.TagName.IFRAME, goog.dom.TagName.LINK, goog.dom.TagName.MATH, goog.dom.TagName.META, goog.dom.TagName.OBJECT, goog.dom.TagName.SCRIPT, goog.dom.TagName.STYLE, goog.dom.TagName.SVG, goog.dom.TagName.TEMPLATE);
/** @typedef {(string|number|goog.string.TypedString|goog.html.SafeStyle.PropertyMap|undefined)} */ goog.html.SafeHtml.AttributeValue;
/**
 * @param {(!goog.dom.TagName|string)} tagName
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @param {(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)=} opt_content
 * @return {!goog.html.SafeHtml}
 * @throws {Error}
 */
goog.html.SafeHtml.create = function(tagName, opt_attributes, opt_content) {
  goog.html.SafeHtml.verifyTagName(String(tagName));
  return goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse(String(tagName), opt_attributes, opt_content);
};
/**
 * @package
 * @param {string} tagName
 * @throws {Error}
 */
goog.html.SafeHtml.verifyTagName = function(tagName) {
  if (!goog.html.SafeHtml.VALID_NAMES_IN_TAG_.test(tagName)) {
    throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? "Invalid tag name \x3c" + tagName + "\x3e." : "");
  }
  if (tagName.toUpperCase() in goog.html.SafeHtml.NOT_ALLOWED_TAG_NAMES_) {
    throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? "Tag name \x3c" + tagName + "\x3e is not allowed for SafeHtml." : "");
  }
};
/**
 * @param {?goog.html.TrustedResourceUrl=} opt_src
 * @param {?goog.html.SafeHtml=} opt_srcdoc
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @param {(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)=} opt_content
 * @return {!goog.html.SafeHtml}
 * @throws {Error}
 */
goog.html.SafeHtml.createIframe = function(opt_src, opt_srcdoc, opt_attributes, opt_content) {
  if (opt_src) {
    goog.html.TrustedResourceUrl.unwrap(opt_src);
  }
  var fixedAttributes = {};
  fixedAttributes["src"] = opt_src || null;
  fixedAttributes["srcdoc"] = opt_srcdoc && goog.html.SafeHtml.unwrap(opt_srcdoc);
  var defaultAttributes = {"sandbox":""};
  var attributes = goog.html.SafeHtml.combineAttributes(fixedAttributes, defaultAttributes, opt_attributes);
  return goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse("iframe", attributes, opt_content);
};
/**
 * @param {(string|!goog.html.SafeUrl)=} opt_src
 * @param {string=} opt_srcdoc
 * @param {!Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @param {(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)=} opt_content
 * @return {!goog.html.SafeHtml}
 * @throws {Error}
 */
goog.html.SafeHtml.createSandboxIframe = function(opt_src, opt_srcdoc, opt_attributes, opt_content) {
  if (!goog.html.SafeHtml.canUseSandboxIframe()) {
    throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? "The browser does not support sandboxed iframes." : "");
  }
  var fixedAttributes = {};
  if (opt_src) {
    fixedAttributes["src"] = goog.html.SafeUrl.unwrap(goog.html.SafeUrl.sanitize(opt_src));
  } else {
    fixedAttributes["src"] = null;
  }
  fixedAttributes["srcdoc"] = opt_srcdoc || null;
  fixedAttributes["sandbox"] = "";
  var attributes = goog.html.SafeHtml.combineAttributes(fixedAttributes, {}, opt_attributes);
  return goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse("iframe", attributes, opt_content);
};
/**
 * @return {boolean}
 */
goog.html.SafeHtml.canUseSandboxIframe = function() {
  return goog.global["HTMLIFrameElement"] && "sandbox" in goog.global["HTMLIFrameElement"].prototype;
};
/**
 * @param {!goog.html.TrustedResourceUrl} src
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @return {!goog.html.SafeHtml}
 * @throws {Error}
 */
goog.html.SafeHtml.createScriptSrc = function(src, opt_attributes) {
  goog.html.TrustedResourceUrl.unwrap(src);
  var fixedAttributes = {"src":src};
  var defaultAttributes = {};
  var attributes = goog.html.SafeHtml.combineAttributes(fixedAttributes, defaultAttributes, opt_attributes);
  return goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse("script", attributes);
};
/**
 * @param {(!goog.html.SafeScript|!Array<!goog.html.SafeScript>)} script
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @return {!goog.html.SafeHtml}
 * @throws {Error}
 */
goog.html.SafeHtml.createScript = function(script, opt_attributes) {
  for (var attr in opt_attributes) {
    var attrLower = attr.toLowerCase();
    if (attrLower == "language" || attrLower == "src" || attrLower == "text" || attrLower == "type") {
      throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? 'Cannot set "' + attrLower + '" attribute' : "");
    }
  }
  var content = "";
  script = goog.array.concat(script);
  for (var i = 0; i < script.length; i++) {
    content += goog.html.SafeScript.unwrap(script[i]);
  }
  var htmlContent = goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse(content, goog.i18n.bidi.Dir.NEUTRAL);
  return goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse("script", opt_attributes, htmlContent);
};
/**
 * @param {(!goog.html.SafeStyleSheet|!Array<!goog.html.SafeStyleSheet>)} styleSheet
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @return {!goog.html.SafeHtml}
 * @throws {Error}
 */
goog.html.SafeHtml.createStyle = function(styleSheet, opt_attributes) {
  var fixedAttributes = {"type":"text/css"};
  var defaultAttributes = {};
  var attributes = goog.html.SafeHtml.combineAttributes(fixedAttributes, defaultAttributes, opt_attributes);
  var content = "";
  styleSheet = goog.array.concat(styleSheet);
  for (var i = 0; i < styleSheet.length; i++) {
    content += goog.html.SafeStyleSheet.unwrap(styleSheet[i]);
  }
  var htmlContent = goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse(content, goog.i18n.bidi.Dir.NEUTRAL);
  return goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse("style", attributes, htmlContent);
};
/**
 * @param {(!goog.html.SafeUrl|string)} url
 * @param {number=} opt_secs
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.createMetaRefresh = function(url, opt_secs) {
  var unwrappedUrl = goog.html.SafeUrl.unwrap(goog.html.SafeUrl.sanitize(url));
  if (goog.labs.userAgent.browser.isIE() || goog.labs.userAgent.browser.isEdge()) {
    if (goog.string.internal.contains(unwrappedUrl, ";")) {
      unwrappedUrl = "'" + unwrappedUrl.replace(/'/g, "%27") + "'";
    }
  }
  var attributes = {"http-equiv":"refresh", "content":(opt_secs || 0) + "; url\x3d" + unwrappedUrl};
  return goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse("meta", attributes);
};
/**
 * @private
 * @param {string} tagName
 * @param {string} name
 * @param {!goog.html.SafeHtml.AttributeValue} value
 * @return {string}
 * @throws {Error}
 */
goog.html.SafeHtml.getAttrNameAndValue_ = function(tagName, name, value) {
  if (value instanceof goog.string.Const) {
    value = goog.string.Const.unwrap(value);
  } else {
    if (name.toLowerCase() == "style") {
      if (goog.html.SafeHtml.SUPPORT_STYLE_ATTRIBUTE) {
        value = goog.html.SafeHtml.getStyleValue_(value);
      } else {
        throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? 'Attribute "style" not supported.' : "");
      }
    } else {
      if (/^on/i.test(name)) {
        throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? 'Attribute "' + name + '" requires goog.string.Const value, "' + value + '" given.' : "");
      } else {
        if (name.toLowerCase() in goog.html.SafeHtml.URL_ATTRIBUTES_) {
          if (value instanceof goog.html.TrustedResourceUrl) {
            value = goog.html.TrustedResourceUrl.unwrap(value);
          } else {
            if (value instanceof goog.html.SafeUrl) {
              value = goog.html.SafeUrl.unwrap(value);
            } else {
              if (typeof value === "string") {
                value = goog.html.SafeUrl.sanitize(value).getTypedStringValue();
              } else {
                throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? 'Attribute "' + name + '" on tag "' + tagName + '" requires goog.html.SafeUrl, goog.string.Const, or' + ' string, value "' + value + '" given.' : "");
              }
            }
          }
        }
      }
    }
  }
  if (value.implementsGoogStringTypedString) {
    value = /** @type {!goog.string.TypedString} */ (value).getTypedStringValue();
  }
  goog.asserts.assert(typeof value === "string" || typeof value === "number", "String or number value expected, got " + typeof value + " with value: " + value);
  return name + '\x3d"' + goog.string.internal.htmlEscape(String(value)) + '"';
};
/**
 * @private
 * @param {!goog.html.SafeHtml.AttributeValue} value
 * @return {string}
 * @throws {Error}
 */
goog.html.SafeHtml.getStyleValue_ = function(value) {
  if (!goog.isObject(value)) {
    throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? 'The "style" attribute requires goog.html.SafeStyle or map ' + "of style properties, " + typeof value + " given: " + value : "");
  }
  if (!(value instanceof goog.html.SafeStyle)) {
    value = goog.html.SafeStyle.create(value);
  }
  return goog.html.SafeStyle.unwrap(value);
};
/**
 * @param {!goog.i18n.bidi.Dir} dir
 * @param {string} tagName
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @param {(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)=} opt_content
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.createWithDir = function(dir, tagName, opt_attributes, opt_content) {
  var html = goog.html.SafeHtml.create(tagName, opt_attributes, opt_content);
  html.dir_ = dir;
  return html;
};
/**
 * @param {!goog.html.SafeHtml.TextOrHtml_} separator
 * @param {!Array<(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)>} parts
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.join = function(separator, parts) {
  var separatorHtml = goog.html.SafeHtml.htmlEscape(separator);
  var dir = separatorHtml.getDirection();
  var content = [];
  /**
   * @param {(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)} argument
   */
  var addArgument = function(argument) {
    if (goog.isArray(argument)) {
      goog.array.forEach(argument, addArgument);
    } else {
      var html = goog.html.SafeHtml.htmlEscape(argument);
      content.push(goog.html.SafeHtml.unwrap(html));
      var htmlDir = html.getDirection();
      if (dir == goog.i18n.bidi.Dir.NEUTRAL) {
        dir = htmlDir;
      } else {
        if (htmlDir != goog.i18n.bidi.Dir.NEUTRAL && dir != htmlDir) {
          dir = null;
        }
      }
    }
  };
  goog.array.forEach(parts, addArgument);
  return goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse(content.join(goog.html.SafeHtml.unwrap(separatorHtml)), dir);
};
/**
 * @param {...(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)} var_args
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.concat = function(var_args) {
  return goog.html.SafeHtml.join(goog.html.SafeHtml.EMPTY, Array.prototype.slice.call(arguments));
};
/**
 * @param {!goog.i18n.bidi.Dir} dir
 * @param {...(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)} var_args
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.concatWithDir = function(dir, var_args) {
  var html = goog.html.SafeHtml.concat(goog.array.slice(arguments, 1));
  html.dir_ = dir;
  return html;
};
/** @private @const @type {!Object} */ goog.html.SafeHtml.TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_ = {};
/**
 * @package
 * @param {string} html
 * @param {?goog.i18n.bidi.Dir} dir
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse = function(html, dir) {
  return (new goog.html.SafeHtml).initSecurityPrivateDoNotAccessOrElse_(html, dir);
};
/**
 * @private
 * @param {string} html
 * @param {?goog.i18n.bidi.Dir} dir
 * @return {!goog.html.SafeHtml}
 */
goog.html.SafeHtml.prototype.initSecurityPrivateDoNotAccessOrElse_ = function(html, dir) {
  this.privateDoNotAccessOrElseSafeHtmlWrappedValue_ = goog.html.trustedtypes.PRIVATE_DO_NOT_ACCESS_OR_ELSE_POLICY ? goog.html.trustedtypes.PRIVATE_DO_NOT_ACCESS_OR_ELSE_POLICY.createHTML(html) : html;
  this.dir_ = dir;
  return this;
};
/**
 * @package
 * @param {string} tagName
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @param {(!goog.html.SafeHtml.TextOrHtml_|!Array<!goog.html.SafeHtml.TextOrHtml_>)=} opt_content
 * @return {!goog.html.SafeHtml}
 * @throws {Error}
 */
goog.html.SafeHtml.createSafeHtmlTagSecurityPrivateDoNotAccessOrElse = function(tagName, opt_attributes, opt_content) {
  var dir = null;
  var result = "\x3c" + tagName;
  result += goog.html.SafeHtml.stringifyAttributes(tagName, opt_attributes);
  var content = opt_content;
  if (content == null) {
    content = [];
  } else {
    if (!goog.isArray(content)) {
      content = [content];
    }
  }
  if (goog.dom.tags.isVoidTag(tagName.toLowerCase())) {
    goog.asserts.assert(!content.length, "Void tag \x3c" + tagName + "\x3e does not allow content.");
    result += "\x3e";
  } else {
    var html = goog.html.SafeHtml.concat(content);
    result += "\x3e" + goog.html.SafeHtml.unwrap(html) + "\x3c/" + tagName + "\x3e";
    dir = html.getDirection();
  }
  var dirAttribute = opt_attributes && opt_attributes["dir"];
  if (dirAttribute) {
    if (/^(ltr|rtl|auto)$/i.test(dirAttribute)) {
      dir = goog.i18n.bidi.Dir.NEUTRAL;
    } else {
      dir = null;
    }
  }
  return goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse(result, dir);
};
/**
 * @package
 * @param {string} tagName
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @return {string}
 * @throws {Error}
 */
goog.html.SafeHtml.stringifyAttributes = function(tagName, opt_attributes) {
  var result = "";
  if (opt_attributes) {
    for (var name in opt_attributes) {
      if (!goog.html.SafeHtml.VALID_NAMES_IN_TAG_.test(name)) {
        throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? 'Invalid attribute name "' + name + '".' : "");
      }
      var value = opt_attributes[name];
      if (value == null) {
        continue;
      }
      result += " " + goog.html.SafeHtml.getAttrNameAndValue_(tagName, name, value);
    }
  }
  return result;
};
/**
 * @package
 * @param {!Object<string,?goog.html.SafeHtml.AttributeValue>} fixedAttributes
 * @param {!Object<string,string>} defaultAttributes
 * @param {?Object<string,?goog.html.SafeHtml.AttributeValue>=} opt_attributes
 * @return {!Object<string,?goog.html.SafeHtml.AttributeValue>}
 * @throws {Error}
 */
goog.html.SafeHtml.combineAttributes = function(fixedAttributes, defaultAttributes, opt_attributes) {
  var combinedAttributes = {};
  var name;
  for (name in fixedAttributes) {
    goog.asserts.assert(name.toLowerCase() == name, "Must be lower case");
    combinedAttributes[name] = fixedAttributes[name];
  }
  for (name in defaultAttributes) {
    goog.asserts.assert(name.toLowerCase() == name, "Must be lower case");
    combinedAttributes[name] = defaultAttributes[name];
  }
  if (opt_attributes) {
    for (name in opt_attributes) {
      var nameLower = name.toLowerCase();
      if (nameLower in fixedAttributes) {
        throw new Error(goog.html.SafeHtml.ENABLE_ERROR_MESSAGES ? 'Cannot override "' + nameLower + '" attribute, got "' + name + '" with value "' + opt_attributes[name] + '"' : "");
      }
      if (nameLower in defaultAttributes) {
        delete combinedAttributes[nameLower];
      }
      combinedAttributes[name] = opt_attributes[name];
    }
  }
  return combinedAttributes;
};
/** @const @type {!goog.html.SafeHtml} */ goog.html.SafeHtml.DOCTYPE_HTML = goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse("\x3c!DOCTYPE html\x3e", goog.i18n.bidi.Dir.NEUTRAL);
/** @const @type {!goog.html.SafeHtml} */ goog.html.SafeHtml.EMPTY = goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse("", goog.i18n.bidi.Dir.NEUTRAL);
/** @const @type {!goog.html.SafeHtml} */ goog.html.SafeHtml.BR = goog.html.SafeHtml.createSafeHtmlSecurityPrivateDoNotAccessOrElse("\x3cbr\x3e", goog.i18n.bidi.Dir.NEUTRAL);

//# sourceMappingURL=goog.html.safehtml.js.map
