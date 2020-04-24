goog.provide("goog.dom.asserts");
goog.require("goog.asserts");
/**
 * @param {?Object} o
 * @return {!Location}
 */
goog.dom.asserts.assertIsLocation = function(o) {
  if (goog.asserts.ENABLE_ASSERTS) {
    var win = goog.dom.asserts.getWindow_(o);
    if (win) {
      if (!o || !(o instanceof win.Location) && o instanceof win.Element) {
        goog.asserts.fail("Argument is not a Location (or a non-Element mock); got: %s", goog.dom.asserts.debugStringForType_(o));
      }
    }
  }
  return (/** @type {!Location} */ (o));
};
/**
 * @private
 * @param {?Object} o
 * @param {string} typename
 * @return {!Element}
 */
goog.dom.asserts.assertIsElementType_ = function(o, typename) {
  if (goog.asserts.ENABLE_ASSERTS) {
    var win = goog.dom.asserts.getWindow_(o);
    if (win && typeof win[typename] != "undefined") {
      if (!o || !(o instanceof win[typename]) && (o instanceof win.Location || o instanceof win.Element)) {
        goog.asserts.fail("Argument is not a %s (or a non-Element, non-Location mock); " + "got: %s", typename, goog.dom.asserts.debugStringForType_(o));
      }
    }
  }
  return (/** @type {!Element} */ (o));
};
/**
 * @param {?Object} o
 * @return {!HTMLAnchorElement}
 */
goog.dom.asserts.assertIsHTMLAnchorElement = function(o) {
  return (/** @type {!HTMLAnchorElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLAnchorElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLButtonElement}
 */
goog.dom.asserts.assertIsHTMLButtonElement = function(o) {
  return (/** @type {!HTMLButtonElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLButtonElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLLinkElement}
 */
goog.dom.asserts.assertIsHTMLLinkElement = function(o) {
  return (/** @type {!HTMLLinkElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLLinkElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLImageElement}
 */
goog.dom.asserts.assertIsHTMLImageElement = function(o) {
  return (/** @type {!HTMLImageElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLImageElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLAudioElement}
 */
goog.dom.asserts.assertIsHTMLAudioElement = function(o) {
  return (/** @type {!HTMLAudioElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLAudioElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLVideoElement}
 */
goog.dom.asserts.assertIsHTMLVideoElement = function(o) {
  return (/** @type {!HTMLVideoElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLVideoElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLInputElement}
 */
goog.dom.asserts.assertIsHTMLInputElement = function(o) {
  return (/** @type {!HTMLInputElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLInputElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLTextAreaElement}
 */
goog.dom.asserts.assertIsHTMLTextAreaElement = function(o) {
  return (/** @type {!HTMLTextAreaElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLTextAreaElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLCanvasElement}
 */
goog.dom.asserts.assertIsHTMLCanvasElement = function(o) {
  return (/** @type {!HTMLCanvasElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLCanvasElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLEmbedElement}
 */
goog.dom.asserts.assertIsHTMLEmbedElement = function(o) {
  return (/** @type {!HTMLEmbedElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLEmbedElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLFormElement}
 */
goog.dom.asserts.assertIsHTMLFormElement = function(o) {
  return (/** @type {!HTMLFormElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLFormElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLFrameElement}
 */
goog.dom.asserts.assertIsHTMLFrameElement = function(o) {
  return (/** @type {!HTMLFrameElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLFrameElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLIFrameElement}
 */
goog.dom.asserts.assertIsHTMLIFrameElement = function(o) {
  return (/** @type {!HTMLIFrameElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLIFrameElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLObjectElement}
 */
goog.dom.asserts.assertIsHTMLObjectElement = function(o) {
  return (/** @type {!HTMLObjectElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLObjectElement")));
};
/**
 * @param {?Object} o
 * @return {!HTMLScriptElement}
 */
goog.dom.asserts.assertIsHTMLScriptElement = function(o) {
  return (/** @type {!HTMLScriptElement} */ (goog.dom.asserts.assertIsElementType_(o, "HTMLScriptElement")));
};
/**
 * @private
 * @param {*} value
 * @return {string}
 */
goog.dom.asserts.debugStringForType_ = function(value) {
  if (goog.isObject(value)) {
    try {
      return value.constructor.displayName || value.constructor.name || Object.prototype.toString.call(value);
    } catch (e) {
      return "\x3cobject could not be stringified\x3e";
    }
  } else {
    return value === undefined ? "undefined" : value === null ? "null" : typeof value;
  }
};
/**
 * @private
 * @param {?Object} o
 * @return {?Window}
 * @suppress {strictMissingProperties}
 */
goog.dom.asserts.getWindow_ = function(o) {
  try {
    var doc = o && o.ownerDocument;
    var win = doc && (/** @type {?Window} */ (doc.defaultView || doc.parentWindow));
    win = win || /** @type {!Window} */ (goog.global);
    if (win.Element && win.Location) {
      return win;
    }
  } catch (ex) {
  }
  return null;
};

//# sourceMappingURL=goog.dom.asserts.js.map
