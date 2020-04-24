goog.provide("goog.uri.utils");
goog.provide("goog.uri.utils.ComponentIndex");
goog.provide("goog.uri.utils.QueryArray");
goog.provide("goog.uri.utils.QueryValue");
goog.provide("goog.uri.utils.StandardQueryParam");
goog.require("goog.array");
goog.require("goog.asserts");
goog.require("goog.string");
/** @private @enum {number} */ goog.uri.utils.CharCode_ = {AMPERSAND:38, EQUAL:61, HASH:35, QUESTION:63};
/**
 * @param {?string=} opt_scheme
 * @param {?string=} opt_userInfo
 * @param {?string=} opt_domain
 * @param {(string|number|null)=} opt_port
 * @param {?string=} opt_path
 * @param {?string=} opt_queryData
 * @param {?string=} opt_fragment
 * @return {string}
 */
goog.uri.utils.buildFromEncodedParts = function(opt_scheme, opt_userInfo, opt_domain, opt_port, opt_path, opt_queryData, opt_fragment) {
  var out = "";
  if (opt_scheme) {
    out += opt_scheme + ":";
  }
  if (opt_domain) {
    out += "//";
    if (opt_userInfo) {
      out += opt_userInfo + "@";
    }
    out += opt_domain;
    if (opt_port) {
      out += ":" + opt_port;
    }
  }
  if (opt_path) {
    out += opt_path;
  }
  if (opt_queryData) {
    out += "?" + opt_queryData;
  }
  if (opt_fragment) {
    out += "#" + opt_fragment;
  }
  return out;
};
/** @private @type {!RegExp} */ goog.uri.utils.splitRe_ = new RegExp("^" + "(?:" + "([^:/?#.]+)" + ":)?" + "(?://" + "(?:([^/?#]*)@)?" + "([^/#?]*?)" + "(?::([0-9]+))?" + "(?\x3d[/#?]|$)" + ")?" + "([^?#]+)?" + "(?:\\?([^#]*))?" + "(?:#([\\s\\S]*))?" + "$");
/** @enum {number} */ goog.uri.utils.ComponentIndex = {SCHEME:1, USER_INFO:2, DOMAIN:3, PORT:4, PATH:5, QUERY_DATA:6, FRAGMENT:7};
/**
 * @param {string} uri
 * @return {!Array<(string|undefined)>}
 */
goog.uri.utils.split = function(uri) {
  return (/** @type {!Array<(string|undefined)>} */ (uri.match(goog.uri.utils.splitRe_)));
};
/**
 * @private
 * @param {?string} uri
 * @param {boolean=} opt_preserveReserved
 * @return {?string}
 */
goog.uri.utils.decodeIfPossible_ = function(uri, opt_preserveReserved) {
  if (!uri) {
    return uri;
  }
  return opt_preserveReserved ? decodeURI(uri) : decodeURIComponent(uri);
};
/**
 * @private
 * @param {goog.uri.utils.ComponentIndex} componentIndex
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getComponentByIndex_ = function(componentIndex, uri) {
  return goog.uri.utils.split(uri)[componentIndex] || null;
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getScheme = function(uri) {
  return goog.uri.utils.getComponentByIndex_(goog.uri.utils.ComponentIndex.SCHEME, uri);
};
/**
 * @param {string} uri
 * @return {string}
 */
goog.uri.utils.getEffectiveScheme = function(uri) {
  var scheme = goog.uri.utils.getScheme(uri);
  if (!scheme && goog.global.self && goog.global.self.location) {
    var protocol = goog.global.self.location.protocol;
    scheme = protocol.substr(0, protocol.length - 1);
  }
  return scheme ? scheme.toLowerCase() : "";
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getUserInfoEncoded = function(uri) {
  return goog.uri.utils.getComponentByIndex_(goog.uri.utils.ComponentIndex.USER_INFO, uri);
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getUserInfo = function(uri) {
  return goog.uri.utils.decodeIfPossible_(goog.uri.utils.getUserInfoEncoded(uri));
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getDomainEncoded = function(uri) {
  return goog.uri.utils.getComponentByIndex_(goog.uri.utils.ComponentIndex.DOMAIN, uri);
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getDomain = function(uri) {
  return goog.uri.utils.decodeIfPossible_(goog.uri.utils.getDomainEncoded(uri), true);
};
/**
 * @param {string} uri
 * @return {?number}
 */
goog.uri.utils.getPort = function(uri) {
  return Number(goog.uri.utils.getComponentByIndex_(goog.uri.utils.ComponentIndex.PORT, uri)) || null;
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getPathEncoded = function(uri) {
  return goog.uri.utils.getComponentByIndex_(goog.uri.utils.ComponentIndex.PATH, uri);
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getPath = function(uri) {
  return goog.uri.utils.decodeIfPossible_(goog.uri.utils.getPathEncoded(uri), true);
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getQueryData = function(uri) {
  return goog.uri.utils.getComponentByIndex_(goog.uri.utils.ComponentIndex.QUERY_DATA, uri);
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getFragmentEncoded = function(uri) {
  var hashIndex = uri.indexOf("#");
  return hashIndex < 0 ? null : uri.substr(hashIndex + 1);
};
/**
 * @param {string} uri
 * @param {?string} fragment
 * @return {string}
 */
goog.uri.utils.setFragmentEncoded = function(uri, fragment) {
  return goog.uri.utils.removeFragment(uri) + (fragment ? "#" + fragment : "");
};
/**
 * @param {string} uri
 * @return {?string}
 */
goog.uri.utils.getFragment = function(uri) {
  return goog.uri.utils.decodeIfPossible_(goog.uri.utils.getFragmentEncoded(uri));
};
/**
 * @param {string} uri
 * @return {string}
 */
goog.uri.utils.getHost = function(uri) {
  var pieces = goog.uri.utils.split(uri);
  return goog.uri.utils.buildFromEncodedParts(pieces[goog.uri.utils.ComponentIndex.SCHEME], pieces[goog.uri.utils.ComponentIndex.USER_INFO], pieces[goog.uri.utils.ComponentIndex.DOMAIN], pieces[goog.uri.utils.ComponentIndex.PORT]);
};
/**
 * @param {string} uri
 * @return {string}
 */
goog.uri.utils.getOrigin = function(uri) {
  var pieces = goog.uri.utils.split(uri);
  return goog.uri.utils.buildFromEncodedParts(pieces[goog.uri.utils.ComponentIndex.SCHEME], null, pieces[goog.uri.utils.ComponentIndex.DOMAIN], pieces[goog.uri.utils.ComponentIndex.PORT]);
};
/**
 * @param {string} uri
 * @return {string}
 */
goog.uri.utils.getPathAndAfter = function(uri) {
  var pieces = goog.uri.utils.split(uri);
  return goog.uri.utils.buildFromEncodedParts(null, null, null, null, pieces[goog.uri.utils.ComponentIndex.PATH], pieces[goog.uri.utils.ComponentIndex.QUERY_DATA], pieces[goog.uri.utils.ComponentIndex.FRAGMENT]);
};
/**
 * @param {string} uri
 * @return {string}
 */
goog.uri.utils.removeFragment = function(uri) {
  var hashIndex = uri.indexOf("#");
  return hashIndex < 0 ? uri : uri.substr(0, hashIndex);
};
/**
 * @param {string} uri1
 * @param {string} uri2
 * @return {boolean}
 */
goog.uri.utils.haveSameDomain = function(uri1, uri2) {
  var pieces1 = goog.uri.utils.split(uri1);
  var pieces2 = goog.uri.utils.split(uri2);
  return pieces1[goog.uri.utils.ComponentIndex.DOMAIN] == pieces2[goog.uri.utils.ComponentIndex.DOMAIN] && pieces1[goog.uri.utils.ComponentIndex.SCHEME] == pieces2[goog.uri.utils.ComponentIndex.SCHEME] && pieces1[goog.uri.utils.ComponentIndex.PORT] == pieces2[goog.uri.utils.ComponentIndex.PORT];
};
/**
 * @private
 * @param {string} uri
 */
goog.uri.utils.assertNoFragmentsOrQueries_ = function(uri) {
  goog.asserts.assert(uri.indexOf("#") < 0 && uri.indexOf("?") < 0, "goog.uri.utils: Fragment or query identifiers are not supported: [%s]", uri);
};
/** @typedef {*} */ goog.uri.utils.QueryValue;
/** @typedef {!Array<(string|goog.uri.utils.QueryValue)>} */ goog.uri.utils.QueryArray;
/**
 * @param {string} encodedQuery
 * @param {function(string,string)} callback
 */
goog.uri.utils.parseQueryData = function(encodedQuery, callback) {
  if (!encodedQuery) {
    return;
  }
  var pairs = encodedQuery.split("\x26");
  for (var i = 0; i < pairs.length; i++) {
    var indexOfEquals = pairs[i].indexOf("\x3d");
    var name = null;
    var value = null;
    if (indexOfEquals >= 0) {
      name = pairs[i].substring(0, indexOfEquals);
      value = pairs[i].substring(indexOfEquals + 1);
    } else {
      name = pairs[i];
    }
    callback(name, value ? goog.string.urlDecode(value) : "");
  }
};
/**
 * @private
 * @param {string} uri
 * @return {!Array<string>}
 */
goog.uri.utils.splitQueryData_ = function(uri) {
  var hashIndex = uri.indexOf("#");
  if (hashIndex < 0) {
    hashIndex = uri.length;
  }
  var questionIndex = uri.indexOf("?");
  var queryData;
  if (questionIndex < 0 || questionIndex > hashIndex) {
    questionIndex = hashIndex;
    queryData = "";
  } else {
    queryData = uri.substring(questionIndex + 1, hashIndex);
  }
  return [uri.substr(0, questionIndex), queryData, uri.substr(hashIndex)];
};
/**
 * @private
 * @param {!Array<string>} parts
 * @return {string}
 */
goog.uri.utils.joinQueryData_ = function(parts) {
  return parts[0] + (parts[1] ? "?" + parts[1] : "") + parts[2];
};
/**
 * @private
 * @param {string} queryData
 * @param {string} newData
 * @return {string}
 */
goog.uri.utils.appendQueryData_ = function(queryData, newData) {
  if (!newData) {
    return queryData;
  }
  return queryData ? queryData + "\x26" + newData : newData;
};
/**
 * @private
 * @param {string} uri
 * @param {string} queryData
 * @return {string}
 */
goog.uri.utils.appendQueryDataToUri_ = function(uri, queryData) {
  if (!queryData) {
    return uri;
  }
  var parts = goog.uri.utils.splitQueryData_(uri);
  parts[1] = goog.uri.utils.appendQueryData_(parts[1], queryData);
  return goog.uri.utils.joinQueryData_(parts);
};
/**
 * @private
 * @param {*} key
 * @param {goog.uri.utils.QueryValue} value
 * @param {!Array<string>} pairs
 */
goog.uri.utils.appendKeyValuePairs_ = function(key, value, pairs) {
  goog.asserts.assertString(key);
  if (goog.isArray(value)) {
    goog.asserts.assertArray(value);
    for (var j = 0; j < value.length; j++) {
      goog.uri.utils.appendKeyValuePairs_(key, String(value[j]), pairs);
    }
  } else {
    if (value != null) {
      pairs.push(key + (value === "" ? "" : "\x3d" + goog.string.urlEncode(value)));
    }
  }
};
/**
 * @param {!IArrayLike<(string|goog.uri.utils.QueryValue)>} keysAndValues
 * @param {number=} opt_startIndex
 * @return {string}
 */
goog.uri.utils.buildQueryData = function(keysAndValues, opt_startIndex) {
  goog.asserts.assert(Math.max(keysAndValues.length - (opt_startIndex || 0), 0) % 2 == 0, "goog.uri.utils: Key/value lists must be even in length.");
  var params = [];
  for (var i = opt_startIndex || 0; i < keysAndValues.length; i += 2) {
    var key = /** @type {string} */ (keysAndValues[i]);
    goog.uri.utils.appendKeyValuePairs_(key, keysAndValues[i + 1], params);
  }
  return params.join("\x26");
};
/**
 * @param {!Object<string,goog.uri.utils.QueryValue>} map
 * @return {string}
 */
goog.uri.utils.buildQueryDataFromMap = function(map) {
  var params = [];
  for (var key in map) {
    goog.uri.utils.appendKeyValuePairs_(key, map[key], params);
  }
  return params.join("\x26");
};
/**
 * @param {string} uri
 * @param {...(goog.uri.utils.QueryArray|goog.uri.utils.QueryValue)} var_args
 * @return {string}
 */
goog.uri.utils.appendParams = function(uri, var_args) {
  var queryData = arguments.length == 2 ? goog.uri.utils.buildQueryData(arguments[1], 0) : goog.uri.utils.buildQueryData(arguments, 1);
  return goog.uri.utils.appendQueryDataToUri_(uri, queryData);
};
/**
 * @param {string} uri
 * @param {!Object<?,goog.uri.utils.QueryValue>} map
 * @return {string}
 */
goog.uri.utils.appendParamsFromMap = function(uri, map) {
  var queryData = goog.uri.utils.buildQueryDataFromMap(map);
  return goog.uri.utils.appendQueryDataToUri_(uri, queryData);
};
/**
 * @param {string} uri
 * @param {string} key
 * @param {*=} opt_value
 * @return {string}
 */
goog.uri.utils.appendParam = function(uri, key, opt_value) {
  var value = opt_value != null ? "\x3d" + goog.string.urlEncode(opt_value) : "";
  return goog.uri.utils.appendQueryDataToUri_(uri, key + value);
};
/**
 * @private
 * @param {string} uri
 * @param {number} startIndex
 * @param {string} keyEncoded
 * @param {number} hashOrEndIndex
 * @return {number}
 */
goog.uri.utils.findParam_ = function(uri, startIndex, keyEncoded, hashOrEndIndex) {
  var index = startIndex;
  var keyLength = keyEncoded.length;
  while ((index = uri.indexOf(keyEncoded, index)) >= 0 && index < hashOrEndIndex) {
    var precedingChar = uri.charCodeAt(index - 1);
    if (precedingChar == goog.uri.utils.CharCode_.AMPERSAND || precedingChar == goog.uri.utils.CharCode_.QUESTION) {
      var followingChar = uri.charCodeAt(index + keyLength);
      if (!followingChar || followingChar == goog.uri.utils.CharCode_.EQUAL || followingChar == goog.uri.utils.CharCode_.AMPERSAND || followingChar == goog.uri.utils.CharCode_.HASH) {
        return index;
      }
    }
    index += keyLength + 1;
  }
  return -1;
};
/** @private @type {RegExp} */ goog.uri.utils.hashOrEndRe_ = /#|$/;
/**
 * @param {string} uri
 * @param {string} keyEncoded
 * @return {boolean}
 */
goog.uri.utils.hasParam = function(uri, keyEncoded) {
  return goog.uri.utils.findParam_(uri, 0, keyEncoded, uri.search(goog.uri.utils.hashOrEndRe_)) >= 0;
};
/**
 * @param {string} uri
 * @param {string} keyEncoded
 * @return {?string}
 */
goog.uri.utils.getParamValue = function(uri, keyEncoded) {
  var hashOrEndIndex = uri.search(goog.uri.utils.hashOrEndRe_);
  var foundIndex = goog.uri.utils.findParam_(uri, 0, keyEncoded, hashOrEndIndex);
  if (foundIndex < 0) {
    return null;
  } else {
    var endPosition = uri.indexOf("\x26", foundIndex);
    if (endPosition < 0 || endPosition > hashOrEndIndex) {
      endPosition = hashOrEndIndex;
    }
    foundIndex += keyEncoded.length + 1;
    return goog.string.urlDecode(uri.substr(foundIndex, endPosition - foundIndex));
  }
};
/**
 * @param {string} uri
 * @param {string} keyEncoded
 * @return {!Array<string>}
 */
goog.uri.utils.getParamValues = function(uri, keyEncoded) {
  var hashOrEndIndex = uri.search(goog.uri.utils.hashOrEndRe_);
  var position = 0;
  var foundIndex;
  var result = [];
  while ((foundIndex = goog.uri.utils.findParam_(uri, position, keyEncoded, hashOrEndIndex)) >= 0) {
    position = uri.indexOf("\x26", foundIndex);
    if (position < 0 || position > hashOrEndIndex) {
      position = hashOrEndIndex;
    }
    foundIndex += keyEncoded.length + 1;
    result.push(goog.string.urlDecode(uri.substr(foundIndex, position - foundIndex)));
  }
  return result;
};
/** @private @type {RegExp} */ goog.uri.utils.trailingQueryPunctuationRe_ = /[?&]($|#)/;
/**
 * @param {string} uri
 * @param {string} keyEncoded
 * @return {string}
 */
goog.uri.utils.removeParam = function(uri, keyEncoded) {
  var hashOrEndIndex = uri.search(goog.uri.utils.hashOrEndRe_);
  var position = 0;
  var foundIndex;
  var buffer = [];
  while ((foundIndex = goog.uri.utils.findParam_(uri, position, keyEncoded, hashOrEndIndex)) >= 0) {
    buffer.push(uri.substring(position, foundIndex));
    position = Math.min(uri.indexOf("\x26", foundIndex) + 1 || hashOrEndIndex, hashOrEndIndex);
  }
  buffer.push(uri.substr(position));
  return buffer.join("").replace(goog.uri.utils.trailingQueryPunctuationRe_, "$1");
};
/**
 * @param {string} uri
 * @param {string} keyEncoded
 * @param {*} value
 * @return {string}
 */
goog.uri.utils.setParam = function(uri, keyEncoded, value) {
  return goog.uri.utils.appendParam(goog.uri.utils.removeParam(uri, keyEncoded), keyEncoded, value);
};
/**
 * @param {string} uri
 * @param {!Object<string,goog.uri.utils.QueryValue>} params
 * @return {string}
 */
goog.uri.utils.setParamsFromMap = function(uri, params) {
  var parts = goog.uri.utils.splitQueryData_(uri);
  var queryData = parts[1];
  var buffer = [];
  if (queryData) {
    goog.array.forEach(queryData.split("\x26"), function(pair) {
      var indexOfEquals = pair.indexOf("\x3d");
      var name = indexOfEquals >= 0 ? pair.substr(0, indexOfEquals) : pair;
      if (!params.hasOwnProperty(name)) {
        buffer.push(pair);
      }
    });
  }
  parts[1] = goog.uri.utils.appendQueryData_(buffer.join("\x26"), goog.uri.utils.buildQueryDataFromMap(params));
  return goog.uri.utils.joinQueryData_(parts);
};
/**
 * @param {string} baseUri
 * @param {string} path
 * @return {string}
 */
goog.uri.utils.appendPath = function(baseUri, path) {
  goog.uri.utils.assertNoFragmentsOrQueries_(baseUri);
  if (goog.string.endsWith(baseUri, "/")) {
    baseUri = baseUri.substr(0, baseUri.length - 1);
  }
  if (goog.string.startsWith(path, "/")) {
    path = path.substr(1);
  }
  return goog.string.buildString(baseUri, "/", path);
};
/**
 * @param {string} uri
 * @param {string} path
 * @return {string}
 */
goog.uri.utils.setPath = function(uri, path) {
  if (!goog.string.startsWith(path, "/")) {
    path = "/" + path;
  }
  var parts = goog.uri.utils.split(uri);
  return goog.uri.utils.buildFromEncodedParts(parts[goog.uri.utils.ComponentIndex.SCHEME], parts[goog.uri.utils.ComponentIndex.USER_INFO], parts[goog.uri.utils.ComponentIndex.DOMAIN], parts[goog.uri.utils.ComponentIndex.PORT], path, parts[goog.uri.utils.ComponentIndex.QUERY_DATA], parts[goog.uri.utils.ComponentIndex.FRAGMENT]);
};
/** @enum {string} */ goog.uri.utils.StandardQueryParam = {RANDOM:"zx"};
/**
 * @param {string} uri
 * @return {string}
 */
goog.uri.utils.makeUnique = function(uri) {
  return goog.uri.utils.setParam(uri, goog.uri.utils.StandardQueryParam.RANDOM, goog.string.getRandomString());
};

//# sourceMappingURL=goog.uri.utils.js.map
