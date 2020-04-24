goog.provide("goog.dom.safe");
goog.provide("goog.dom.safe.InsertAdjacentHtmlPosition");
goog.require("goog.asserts");
goog.require("goog.dom.asserts");
goog.require("goog.functions");
goog.require("goog.html.SafeHtml");
goog.require("goog.html.SafeScript");
goog.require("goog.html.SafeStyle");
goog.require("goog.html.SafeUrl");
goog.require("goog.html.TrustedResourceUrl");
goog.require("goog.html.uncheckedconversions");
goog.require("goog.string.Const");
goog.require("goog.string.internal");
/** @enum {string} */ goog.dom.safe.InsertAdjacentHtmlPosition = {AFTERBEGIN:"afterbegin", AFTEREND:"afterend", BEFOREBEGIN:"beforebegin", BEFOREEND:"beforeend"};
/**
 * @param {!Node} node
 * @param {!goog.dom.safe.InsertAdjacentHtmlPosition} position
 * @param {!goog.html.SafeHtml} html
 */
goog.dom.safe.insertAdjacentHtml = function(node, position, html) {
  node.insertAdjacentHTML(position, goog.html.SafeHtml.unwrapTrustedHTML(html));
};
/** @private @const @type {!Object<string,boolean>} */ goog.dom.safe.SET_INNER_HTML_DISALLOWED_TAGS_ = {"MATH":true, "SCRIPT":true, "STYLE":true, "SVG":true, "TEMPLATE":true};
/**
 * @private
 * @return {boolean}
 */
goog.dom.safe.isInnerHtmlCleanupRecursive_ = goog.functions.cacheReturnValue(function() {
  if (goog.DEBUG && typeof document === "undefined") {
    return false;
  }
  var div = document.createElement("div");
  var childDiv = document.createElement("div");
  childDiv.appendChild(document.createElement("div"));
  div.appendChild(childDiv);
  if (goog.DEBUG && !div.firstChild) {
    return false;
  }
  var innerChild = div.firstChild.firstChild;
  div.innerHTML = goog.html.SafeHtml.unwrapTrustedHTML(goog.html.SafeHtml.EMPTY);
  return !innerChild.parentElement;
});
/**
 * @param {?Element} elem
 * @param {!goog.html.SafeHtml} html
 */
goog.dom.safe.unsafeSetInnerHtmlDoNotUseOrElse = function(elem, html) {
  if (goog.dom.safe.isInnerHtmlCleanupRecursive_()) {
    while (elem.lastChild) {
      elem.removeChild(elem.lastChild);
    }
  }
  elem.innerHTML = goog.html.SafeHtml.unwrapTrustedHTML(html);
};
/**
 * @param {!Element} elem
 * @param {!goog.html.SafeHtml} html
 * @throws {Error}
 */
goog.dom.safe.setInnerHtml = function(elem, html) {
  if (goog.asserts.ENABLE_ASSERTS) {
    var tagName = elem.tagName.toUpperCase();
    if (goog.dom.safe.SET_INNER_HTML_DISALLOWED_TAGS_[tagName]) {
      throw new Error("goog.dom.safe.setInnerHtml cannot be used to set content of " + elem.tagName + ".");
    }
  }
  goog.dom.safe.unsafeSetInnerHtmlDoNotUseOrElse(elem, html);
};
/**
 * @param {!Element} elem
 * @param {!goog.html.SafeHtml} html
 */
goog.dom.safe.setOuterHtml = function(elem, html) {
  elem.outerHTML = goog.html.SafeHtml.unwrapTrustedHTML(html);
};
/**
 * @param {!Element} form
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setFormElementAction = function(form, url) {
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  goog.dom.asserts.assertIsHTMLFormElement(form).action = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!Element} button
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setButtonFormAction = function(button, url) {
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  goog.dom.asserts.assertIsHTMLButtonElement(button).formAction = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!Element} input
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setInputFormAction = function(input, url) {
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  goog.dom.asserts.assertIsHTMLInputElement(input).formAction = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!Element} elem
 * @param {!goog.html.SafeStyle} style
 */
goog.dom.safe.setStyle = function(elem, style) {
  elem.style.cssText = goog.html.SafeStyle.unwrap(style);
};
/**
 * @param {!Document} doc
 * @param {!goog.html.SafeHtml} html
 */
goog.dom.safe.documentWrite = function(doc, html) {
  doc.write(goog.html.SafeHtml.unwrapTrustedHTML(html));
};
/**
 * @param {!HTMLAnchorElement} anchor
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setAnchorHref = function(anchor, url) {
  goog.dom.asserts.assertIsHTMLAnchorElement(anchor);
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  anchor.href = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!HTMLImageElement} imageElement
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setImageSrc = function(imageElement, url) {
  goog.dom.asserts.assertIsHTMLImageElement(imageElement);
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    var allowDataUrl = /^data:image\//i.test(url);
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url, allowDataUrl);
  }
  imageElement.src = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!HTMLAudioElement} audioElement
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setAudioSrc = function(audioElement, url) {
  goog.dom.asserts.assertIsHTMLAudioElement(audioElement);
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    var allowDataUrl = /^data:audio\//i.test(url);
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url, allowDataUrl);
  }
  audioElement.src = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!HTMLVideoElement} videoElement
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setVideoSrc = function(videoElement, url) {
  goog.dom.asserts.assertIsHTMLVideoElement(videoElement);
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    var allowDataUrl = /^data:video\//i.test(url);
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url, allowDataUrl);
  }
  videoElement.src = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!HTMLEmbedElement} embed
 * @param {!goog.html.TrustedResourceUrl} url
 */
goog.dom.safe.setEmbedSrc = function(embed, url) {
  goog.dom.asserts.assertIsHTMLEmbedElement(embed);
  embed.src = goog.html.TrustedResourceUrl.unwrapTrustedScriptURL(url);
};
/**
 * @param {!HTMLFrameElement} frame
 * @param {!goog.html.TrustedResourceUrl} url
 */
goog.dom.safe.setFrameSrc = function(frame, url) {
  goog.dom.asserts.assertIsHTMLFrameElement(frame);
  frame.src = goog.html.TrustedResourceUrl.unwrap(url);
};
/**
 * @param {!HTMLIFrameElement} iframe
 * @param {!goog.html.TrustedResourceUrl} url
 */
goog.dom.safe.setIframeSrc = function(iframe, url) {
  goog.dom.asserts.assertIsHTMLIFrameElement(iframe);
  iframe.src = goog.html.TrustedResourceUrl.unwrap(url);
};
/**
 * @param {!HTMLIFrameElement} iframe
 * @param {!goog.html.SafeHtml} html
 */
goog.dom.safe.setIframeSrcdoc = function(iframe, html) {
  goog.dom.asserts.assertIsHTMLIFrameElement(iframe);
  iframe.srcdoc = goog.html.SafeHtml.unwrapTrustedHTML(html);
};
/**
 * @param {!HTMLLinkElement} link
 * @param {(string|!goog.html.SafeUrl|!goog.html.TrustedResourceUrl)} url
 * @param {string} rel
 * @throws {Error}
 */
goog.dom.safe.setLinkHrefAndRel = function(link, url, rel) {
  goog.dom.asserts.assertIsHTMLLinkElement(link);
  link.rel = rel;
  if (goog.string.internal.caseInsensitiveContains(rel, "stylesheet")) {
    goog.asserts.assert(url instanceof goog.html.TrustedResourceUrl, 'URL must be TrustedResourceUrl because "rel" contains "stylesheet"');
    link.href = goog.html.TrustedResourceUrl.unwrap(url);
  } else {
    if (url instanceof goog.html.TrustedResourceUrl) {
      link.href = goog.html.TrustedResourceUrl.unwrap(url);
    } else {
      if (url instanceof goog.html.SafeUrl) {
        link.href = goog.html.SafeUrl.unwrap(url);
      } else {
        link.href = goog.html.SafeUrl.unwrap(goog.html.SafeUrl.sanitizeAssertUnchanged(url));
      }
    }
  }
};
/**
 * @param {!HTMLObjectElement} object
 * @param {!goog.html.TrustedResourceUrl} url
 */
goog.dom.safe.setObjectData = function(object, url) {
  goog.dom.asserts.assertIsHTMLObjectElement(object);
  object.data = goog.html.TrustedResourceUrl.unwrapTrustedScriptURL(url);
};
/**
 * @param {!HTMLScriptElement} script
 * @param {!goog.html.TrustedResourceUrl} url
 */
goog.dom.safe.setScriptSrc = function(script, url) {
  goog.dom.asserts.assertIsHTMLScriptElement(script);
  script.src = goog.html.TrustedResourceUrl.unwrapTrustedScriptURL(url);
  var nonce = goog.getScriptNonce();
  if (nonce) {
    script.setAttribute("nonce", nonce);
  }
};
/**
 * @param {!HTMLScriptElement} script
 * @param {!goog.html.SafeScript} content
 */
goog.dom.safe.setScriptContent = function(script, content) {
  goog.dom.asserts.assertIsHTMLScriptElement(script);
  script.text = goog.html.SafeScript.unwrapTrustedScript(content);
  var nonce = goog.getScriptNonce();
  if (nonce) {
    script.setAttribute("nonce", nonce);
  }
};
/**
 * @param {!Location} loc
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.setLocationHref = function(loc, url) {
  goog.dom.asserts.assertIsLocation(loc);
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  loc.href = goog.html.SafeUrl.unwrap(safeUrl);
};
/**
 * @param {!Location} loc
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.assignLocation = function(loc, url) {
  goog.dom.asserts.assertIsLocation(loc);
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  loc.assign(goog.html.SafeUrl.unwrap(safeUrl));
};
/**
 * @param {!Location} loc
 * @param {(string|!goog.html.SafeUrl)} url
 */
goog.dom.safe.replaceLocation = function(loc, url) {
  goog.dom.asserts.assertIsLocation(loc);
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  loc.replace(goog.html.SafeUrl.unwrap(safeUrl));
};
/**
 * @param {(string|!goog.html.SafeUrl)} url
 * @param {Window=} opt_openerWin
 * @param {!goog.string.Const=} opt_name
 * @param {string=} opt_specs
 * @param {boolean=} opt_replace
 * @return {Window}
 */
goog.dom.safe.openInWindow = function(url, opt_openerWin, opt_name, opt_specs, opt_replace) {
  /** @type {!goog.html.SafeUrl} */ var safeUrl;
  if (url instanceof goog.html.SafeUrl) {
    safeUrl = url;
  } else {
    safeUrl = goog.html.SafeUrl.sanitizeAssertUnchanged(url);
  }
  var win = opt_openerWin || goog.global;
  return win.open(goog.html.SafeUrl.unwrap(safeUrl), opt_name ? goog.string.Const.unwrap(opt_name) : "", opt_specs, opt_replace);
};
/**
 * @param {!DOMParser} parser
 * @param {!goog.html.SafeHtml} html
 * @return {?Document}
 */
goog.dom.safe.parseFromStringHtml = function(parser, html) {
  return goog.dom.safe.parseFromString(parser, html, "text/html");
};
/**
 * @param {!DOMParser} parser
 * @param {!goog.html.SafeHtml} content
 * @param {string} type
 * @return {?Document}
 */
goog.dom.safe.parseFromString = function(parser, content, type) {
  return parser.parseFromString(goog.html.SafeHtml.unwrapTrustedHTML(content), type);
};
/**
 * @param {!Blob} blob
 * @return {!HTMLImageElement}
 * @throws {!Error}
 */
goog.dom.safe.createImageFromBlob = function(blob) {
  if (!/^image\/.*/g.test(blob.type)) {
    throw new Error("goog.dom.safe.createImageFromBlob only accepts MIME type image/.*.");
  }
  var objectUrl = goog.global.URL.createObjectURL(blob);
  var image = new goog.global.Image;
  image.onload = function() {
    goog.global.URL.revokeObjectURL(objectUrl);
  };
  goog.dom.safe.setImageSrc(image, goog.html.uncheckedconversions.safeUrlFromStringKnownToSatisfyTypeContract(goog.string.Const.from("Image blob URL."), objectUrl));
  return image;
};

//# sourceMappingURL=goog.dom.safe.js.map
