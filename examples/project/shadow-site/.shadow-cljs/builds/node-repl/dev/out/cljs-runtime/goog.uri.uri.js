goog.provide("goog.Uri");
goog.provide("goog.Uri.QueryData");
goog.require("goog.array");
goog.require("goog.asserts");
goog.require("goog.string");
goog.require("goog.structs");
goog.require("goog.structs.Map");
goog.require("goog.uri.utils");
goog.require("goog.uri.utils.ComponentIndex");
goog.require("goog.uri.utils.StandardQueryParam");
/**
 * @struct
 * @constructor
 * @param {*=} opt_uri
 * @param {boolean=} opt_ignoreCase
 */
goog.Uri = function(opt_uri, opt_ignoreCase) {
  /** @private @type {string} */ this.scheme_ = "";
  /** @private @type {string} */ this.userInfo_ = "";
  /** @private @type {string} */ this.domain_ = "";
  /** @private @type {?number} */ this.port_ = null;
  /** @private @type {string} */ this.path_ = "";
  /** @private @type {string} */ this.fragment_ = "";
  /** @private @type {boolean} */ this.isReadOnly_ = false;
  /** @private @type {boolean} */ this.ignoreCase_ = false;
  /** @private @type {!goog.Uri.QueryData} */ this.queryData_;
  var m;
  if (opt_uri instanceof goog.Uri) {
    this.ignoreCase_ = opt_ignoreCase !== undefined ? opt_ignoreCase : opt_uri.getIgnoreCase();
    this.setScheme(opt_uri.getScheme());
    this.setUserInfo(opt_uri.getUserInfo());
    this.setDomain(opt_uri.getDomain());
    this.setPort(opt_uri.getPort());
    this.setPath(opt_uri.getPath());
    this.setQueryData(opt_uri.getQueryData().clone());
    this.setFragment(opt_uri.getFragment());
  } else {
    if (opt_uri && (m = goog.uri.utils.split(String(opt_uri)))) {
      this.ignoreCase_ = !!opt_ignoreCase;
      this.setScheme(m[goog.uri.utils.ComponentIndex.SCHEME] || "", true);
      this.setUserInfo(m[goog.uri.utils.ComponentIndex.USER_INFO] || "", true);
      this.setDomain(m[goog.uri.utils.ComponentIndex.DOMAIN] || "", true);
      this.setPort(m[goog.uri.utils.ComponentIndex.PORT]);
      this.setPath(m[goog.uri.utils.ComponentIndex.PATH] || "", true);
      this.setQueryData(m[goog.uri.utils.ComponentIndex.QUERY_DATA] || "", true);
      this.setFragment(m[goog.uri.utils.ComponentIndex.FRAGMENT] || "", true);
    } else {
      this.ignoreCase_ = !!opt_ignoreCase;
      this.queryData_ = new goog.Uri.QueryData(null, null, this.ignoreCase_);
    }
  }
};
/** @type {string} */ goog.Uri.RANDOM_PARAM = goog.uri.utils.StandardQueryParam.RANDOM;
/**
 * @return {string}
 * @override
 */
goog.Uri.prototype.toString = function() {
  var out = [];
  var scheme = this.getScheme();
  if (scheme) {
    out.push(goog.Uri.encodeSpecialChars_(scheme, goog.Uri.reDisallowedInSchemeOrUserInfo_, true), ":");
  }
  var domain = this.getDomain();
  if (domain || scheme == "file") {
    out.push("//");
    var userInfo = this.getUserInfo();
    if (userInfo) {
      out.push(goog.Uri.encodeSpecialChars_(userInfo, goog.Uri.reDisallowedInSchemeOrUserInfo_, true), "@");
    }
    out.push(goog.Uri.removeDoubleEncoding_(goog.string.urlEncode(domain)));
    var port = this.getPort();
    if (port != null) {
      out.push(":", String(port));
    }
  }
  var path = this.getPath();
  if (path) {
    if (this.hasDomain() && path.charAt(0) != "/") {
      out.push("/");
    }
    out.push(goog.Uri.encodeSpecialChars_(path, path.charAt(0) == "/" ? goog.Uri.reDisallowedInAbsolutePath_ : goog.Uri.reDisallowedInRelativePath_, true));
  }
  var query = this.getEncodedQuery();
  if (query) {
    out.push("?", query);
  }
  var fragment = this.getFragment();
  if (fragment) {
    out.push("#", goog.Uri.encodeSpecialChars_(fragment, goog.Uri.reDisallowedInFragment_));
  }
  return out.join("");
};
/**
 * @param {!goog.Uri} relativeUri
 * @return {!goog.Uri}
 */
goog.Uri.prototype.resolve = function(relativeUri) {
  var absoluteUri = this.clone();
  var overridden = relativeUri.hasScheme();
  if (overridden) {
    absoluteUri.setScheme(relativeUri.getScheme());
  } else {
    overridden = relativeUri.hasUserInfo();
  }
  if (overridden) {
    absoluteUri.setUserInfo(relativeUri.getUserInfo());
  } else {
    overridden = relativeUri.hasDomain();
  }
  if (overridden) {
    absoluteUri.setDomain(relativeUri.getDomain());
  } else {
    overridden = relativeUri.hasPort();
  }
  var path = relativeUri.getPath();
  if (overridden) {
    absoluteUri.setPort(relativeUri.getPort());
  } else {
    overridden = relativeUri.hasPath();
    if (overridden) {
      if (path.charAt(0) != "/") {
        if (this.hasDomain() && !this.hasPath()) {
          path = "/" + path;
        } else {
          var lastSlashIndex = absoluteUri.getPath().lastIndexOf("/");
          if (lastSlashIndex != -1) {
            path = absoluteUri.getPath().substr(0, lastSlashIndex + 1) + path;
          }
        }
      }
      path = goog.Uri.removeDotSegments(path);
    }
  }
  if (overridden) {
    absoluteUri.setPath(path);
  } else {
    overridden = relativeUri.hasQuery();
  }
  if (overridden) {
    absoluteUri.setQueryData(relativeUri.getQueryData().clone());
  } else {
    overridden = relativeUri.hasFragment();
  }
  if (overridden) {
    absoluteUri.setFragment(relativeUri.getFragment());
  }
  return absoluteUri;
};
/**
 * @return {!goog.Uri}
 */
goog.Uri.prototype.clone = function() {
  return new goog.Uri(this);
};
/**
 * @return {string}
 */
goog.Uri.prototype.getScheme = function() {
  return this.scheme_;
};
/**
 * @param {string} newScheme
 * @param {boolean=} opt_decode
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setScheme = function(newScheme, opt_decode) {
  this.enforceReadOnly();
  this.scheme_ = opt_decode ? goog.Uri.decodeOrEmpty_(newScheme, true) : newScheme;
  if (this.scheme_) {
    this.scheme_ = this.scheme_.replace(/:$/, "");
  }
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.hasScheme = function() {
  return !!this.scheme_;
};
/**
 * @return {string}
 */
goog.Uri.prototype.getUserInfo = function() {
  return this.userInfo_;
};
/**
 * @param {string} newUserInfo
 * @param {boolean=} opt_decode
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setUserInfo = function(newUserInfo, opt_decode) {
  this.enforceReadOnly();
  this.userInfo_ = opt_decode ? goog.Uri.decodeOrEmpty_(newUserInfo) : newUserInfo;
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.hasUserInfo = function() {
  return !!this.userInfo_;
};
/**
 * @return {string}
 */
goog.Uri.prototype.getDomain = function() {
  return this.domain_;
};
/**
 * @param {string} newDomain
 * @param {boolean=} opt_decode
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setDomain = function(newDomain, opt_decode) {
  this.enforceReadOnly();
  this.domain_ = opt_decode ? goog.Uri.decodeOrEmpty_(newDomain, true) : newDomain;
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.hasDomain = function() {
  return !!this.domain_;
};
/**
 * @return {?number}
 */
goog.Uri.prototype.getPort = function() {
  return this.port_;
};
/**
 * @param {*} newPort
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setPort = function(newPort) {
  this.enforceReadOnly();
  if (newPort) {
    newPort = Number(newPort);
    if (isNaN(newPort) || newPort < 0) {
      throw new Error("Bad port number " + newPort);
    }
    this.port_ = newPort;
  } else {
    this.port_ = null;
  }
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.hasPort = function() {
  return this.port_ != null;
};
/**
 * @return {string}
 */
goog.Uri.prototype.getPath = function() {
  return this.path_;
};
/**
 * @param {string} newPath
 * @param {boolean=} opt_decode
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setPath = function(newPath, opt_decode) {
  this.enforceReadOnly();
  this.path_ = opt_decode ? goog.Uri.decodeOrEmpty_(newPath, true) : newPath;
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.hasPath = function() {
  return !!this.path_;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.hasQuery = function() {
  return this.queryData_.toString() !== "";
};
/**
 * @param {(goog.Uri.QueryData|string|undefined)} queryData
 * @param {boolean=} opt_decode
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setQueryData = function(queryData, opt_decode) {
  this.enforceReadOnly();
  if (queryData instanceof goog.Uri.QueryData) {
    this.queryData_ = queryData;
    this.queryData_.setIgnoreCase(this.ignoreCase_);
  } else {
    if (!opt_decode) {
      queryData = goog.Uri.encodeSpecialChars_(queryData, goog.Uri.reDisallowedInQuery_);
    }
    this.queryData_ = new goog.Uri.QueryData(queryData, null, this.ignoreCase_);
  }
  return this;
};
/**
 * @param {string} newQuery
 * @param {boolean=} opt_decode
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setQuery = function(newQuery, opt_decode) {
  return this.setQueryData(newQuery, opt_decode);
};
/**
 * @return {string}
 */
goog.Uri.prototype.getEncodedQuery = function() {
  return this.queryData_.toString();
};
/**
 * @return {string}
 */
goog.Uri.prototype.getDecodedQuery = function() {
  return this.queryData_.toDecodedString();
};
/**
 * @return {!goog.Uri.QueryData}
 */
goog.Uri.prototype.getQueryData = function() {
  return this.queryData_;
};
/**
 * @return {string}
 */
goog.Uri.prototype.getQuery = function() {
  return this.getEncodedQuery();
};
/**
 * @param {string} key
 * @param {*} value
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setParameterValue = function(key, value) {
  this.enforceReadOnly();
  this.queryData_.set(key, value);
  return this;
};
/**
 * @param {string} key
 * @param {*} values
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setParameterValues = function(key, values) {
  this.enforceReadOnly();
  if (!goog.isArray(values)) {
    values = [String(values)];
  }
  this.queryData_.setValues(key, values);
  return this;
};
/**
 * @param {string} name
 * @return {!Array<?>}
 */
goog.Uri.prototype.getParameterValues = function(name) {
  return this.queryData_.getValues(name);
};
/**
 * @param {string} paramName
 * @return {(string|undefined)}
 */
goog.Uri.prototype.getParameterValue = function(paramName) {
  return (/** @type {(string|undefined)} */ (this.queryData_.get(paramName)));
};
/**
 * @return {string}
 */
goog.Uri.prototype.getFragment = function() {
  return this.fragment_;
};
/**
 * @param {string} newFragment
 * @param {boolean=} opt_decode
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setFragment = function(newFragment, opt_decode) {
  this.enforceReadOnly();
  this.fragment_ = opt_decode ? goog.Uri.decodeOrEmpty_(newFragment) : newFragment;
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.hasFragment = function() {
  return !!this.fragment_;
};
/**
 * @param {!goog.Uri} uri2
 * @return {boolean}
 */
goog.Uri.prototype.hasSameDomainAs = function(uri2) {
  return (!this.hasDomain() && !uri2.hasDomain() || this.getDomain() == uri2.getDomain()) && (!this.hasPort() && !uri2.hasPort() || this.getPort() == uri2.getPort());
};
/**
 * @return {!goog.Uri}
 */
goog.Uri.prototype.makeUnique = function() {
  this.enforceReadOnly();
  this.setParameterValue(goog.Uri.RANDOM_PARAM, goog.string.getRandomString());
  return this;
};
/**
 * @param {string} key
 * @return {!goog.Uri}
 */
goog.Uri.prototype.removeParameter = function(key) {
  this.enforceReadOnly();
  this.queryData_.remove(key);
  return this;
};
/**
 * @param {boolean} isReadOnly
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setReadOnly = function(isReadOnly) {
  this.isReadOnly_ = isReadOnly;
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.isReadOnly = function() {
  return this.isReadOnly_;
};
goog.Uri.prototype.enforceReadOnly = function() {
  if (this.isReadOnly_) {
    throw new Error("Tried to modify a read-only Uri");
  }
};
/**
 * @param {boolean} ignoreCase
 * @return {!goog.Uri}
 */
goog.Uri.prototype.setIgnoreCase = function(ignoreCase) {
  this.ignoreCase_ = ignoreCase;
  if (this.queryData_) {
    this.queryData_.setIgnoreCase(ignoreCase);
  }
  return this;
};
/**
 * @return {boolean}
 */
goog.Uri.prototype.getIgnoreCase = function() {
  return this.ignoreCase_;
};
/**
 * @param {*} uri
 * @param {boolean=} opt_ignoreCase
 * @return {!goog.Uri}
 */
goog.Uri.parse = function(uri, opt_ignoreCase) {
  return uri instanceof goog.Uri ? uri.clone() : new goog.Uri(uri, opt_ignoreCase);
};
/**
 * @param {?string=} opt_scheme
 * @param {?string=} opt_userInfo
 * @param {?string=} opt_domain
 * @param {?number=} opt_port
 * @param {?string=} opt_path
 * @param {(string|goog.Uri.QueryData)=} opt_query
 * @param {?string=} opt_fragment
 * @param {boolean=} opt_ignoreCase
 * @return {!goog.Uri}
 */
goog.Uri.create = function(opt_scheme, opt_userInfo, opt_domain, opt_port, opt_path, opt_query, opt_fragment, opt_ignoreCase) {
  var uri = new goog.Uri(null, opt_ignoreCase);
  opt_scheme && uri.setScheme(opt_scheme);
  opt_userInfo && uri.setUserInfo(opt_userInfo);
  opt_domain && uri.setDomain(opt_domain);
  opt_port && uri.setPort(opt_port);
  opt_path && uri.setPath(opt_path);
  opt_query && uri.setQueryData(opt_query);
  opt_fragment && uri.setFragment(opt_fragment);
  return uri;
};
/**
 * @param {*} base
 * @param {*} rel
 * @return {!goog.Uri}
 */
goog.Uri.resolve = function(base, rel) {
  if (!(base instanceof goog.Uri)) {
    base = goog.Uri.parse(base);
  }
  if (!(rel instanceof goog.Uri)) {
    rel = goog.Uri.parse(rel);
  }
  return base.resolve(rel);
};
/**
 * @param {string} path
 * @return {string}
 */
goog.Uri.removeDotSegments = function(path) {
  if (path == ".." || path == ".") {
    return "";
  } else {
    if (!goog.string.contains(path, "./") && !goog.string.contains(path, "/.")) {
      return path;
    } else {
      var leadingSlash = goog.string.startsWith(path, "/");
      var segments = path.split("/");
      var out = [];
      for (var pos = 0; pos < segments.length;) {
        var segment = segments[pos++];
        if (segment == ".") {
          if (leadingSlash && pos == segments.length) {
            out.push("");
          }
        } else {
          if (segment == "..") {
            if (out.length > 1 || out.length == 1 && out[0] != "") {
              out.pop();
            }
            if (leadingSlash && pos == segments.length) {
              out.push("");
            }
          } else {
            out.push(segment);
            leadingSlash = true;
          }
        }
      }
      return out.join("/");
    }
  }
};
/**
 * @private
 * @param {(string|undefined)} val
 * @param {boolean=} opt_preserveReserved
 * @return {string}
 */
goog.Uri.decodeOrEmpty_ = function(val, opt_preserveReserved) {
  if (!val) {
    return "";
  }
  return opt_preserveReserved ? decodeURI(val.replace(/%25/g, "%2525")) : decodeURIComponent(val);
};
/**
 * @private
 * @param {*} unescapedPart
 * @param {RegExp} extra
 * @param {boolean=} opt_removeDoubleEncoding
 * @return {?string}
 */
goog.Uri.encodeSpecialChars_ = function(unescapedPart, extra, opt_removeDoubleEncoding) {
  if (typeof unescapedPart === "string") {
    var encoded = encodeURI(unescapedPart).replace(extra, goog.Uri.encodeChar_);
    if (opt_removeDoubleEncoding) {
      encoded = goog.Uri.removeDoubleEncoding_(encoded);
    }
    return encoded;
  }
  return null;
};
/**
 * @private
 * @param {string} ch
 * @return {string}
 */
goog.Uri.encodeChar_ = function(ch) {
  var n = ch.charCodeAt(0);
  return "%" + (n >> 4 & 15).toString(16) + (n & 15).toString(16);
};
/**
 * @private
 * @param {string} doubleEncodedString
 * @return {string}
 */
goog.Uri.removeDoubleEncoding_ = function(doubleEncodedString) {
  return doubleEncodedString.replace(/%25([0-9a-fA-F]{2})/g, "%$1");
};
/** @private @type {RegExp} */ goog.Uri.reDisallowedInSchemeOrUserInfo_ = /[#\/\?@]/g;
/** @private @type {RegExp} */ goog.Uri.reDisallowedInRelativePath_ = /[#\?:]/g;
/** @private @type {RegExp} */ goog.Uri.reDisallowedInAbsolutePath_ = /[#\?]/g;
/** @private @type {RegExp} */ goog.Uri.reDisallowedInQuery_ = /[#\?@]/g;
/** @private @type {RegExp} */ goog.Uri.reDisallowedInFragment_ = /#/g;
/**
 * @param {string} uri1String
 * @param {string} uri2String
 * @return {boolean}
 */
goog.Uri.haveSameDomain = function(uri1String, uri2String) {
  var pieces1 = goog.uri.utils.split(uri1String);
  var pieces2 = goog.uri.utils.split(uri2String);
  return pieces1[goog.uri.utils.ComponentIndex.DOMAIN] == pieces2[goog.uri.utils.ComponentIndex.DOMAIN] && pieces1[goog.uri.utils.ComponentIndex.PORT] == pieces2[goog.uri.utils.ComponentIndex.PORT];
};
/**
 * @final
 * @struct
 * @constructor
 * @param {?string=} opt_query
 * @param {goog.Uri=} opt_uri
 * @param {boolean=} opt_ignoreCase
 */
goog.Uri.QueryData = function(opt_query, opt_uri, opt_ignoreCase) {
  /** @private @type {?goog.structs.Map<string,!Array<*>>} */ this.keyMap_ = null;
  /** @private @type {?number} */ this.count_ = null;
  /** @private @type {?string} */ this.encodedQuery_ = opt_query || null;
  /** @private @type {boolean} */ this.ignoreCase_ = !!opt_ignoreCase;
};
/** @private */ goog.Uri.QueryData.prototype.ensureKeyMapInitialized_ = function() {
  if (!this.keyMap_) {
    this.keyMap_ = new goog.structs.Map;
    this.count_ = 0;
    if (this.encodedQuery_) {
      var self = this;
      goog.uri.utils.parseQueryData(this.encodedQuery_, function(name, value) {
        self.add(goog.string.urlDecode(name), value);
      });
    }
  }
};
/**
 * @param {(!goog.structs.Map<string,?>|!Object)} map
 * @param {goog.Uri=} opt_uri
 * @param {boolean=} opt_ignoreCase
 * @return {!goog.Uri.QueryData}
 */
goog.Uri.QueryData.createFromMap = function(map, opt_uri, opt_ignoreCase) {
  var keys = goog.structs.getKeys(map);
  if (typeof keys == "undefined") {
    throw new Error("Keys are undefined");
  }
  var queryData = new goog.Uri.QueryData(null, null, opt_ignoreCase);
  var values = goog.structs.getValues(map);
  for (var i = 0; i < keys.length; i++) {
    var key = keys[i];
    var value = values[i];
    if (!goog.isArray(value)) {
      queryData.add(key, value);
    } else {
      queryData.setValues(key, value);
    }
  }
  return queryData;
};
/**
 * @param {!Array<string>} keys
 * @param {!Array<?>} values
 * @param {goog.Uri=} opt_uri
 * @param {boolean=} opt_ignoreCase
 * @return {!goog.Uri.QueryData}
 */
goog.Uri.QueryData.createFromKeysValues = function(keys, values, opt_uri, opt_ignoreCase) {
  if (keys.length != values.length) {
    throw new Error("Mismatched lengths for keys/values");
  }
  var queryData = new goog.Uri.QueryData(null, null, opt_ignoreCase);
  for (var i = 0; i < keys.length; i++) {
    queryData.add(keys[i], values[i]);
  }
  return queryData;
};
/**
 * @return {?number}
 */
goog.Uri.QueryData.prototype.getCount = function() {
  this.ensureKeyMapInitialized_();
  return this.count_;
};
/**
 * @param {string} key
 * @param {*} value
 * @return {!goog.Uri.QueryData}
 */
goog.Uri.QueryData.prototype.add = function(key, value) {
  this.ensureKeyMapInitialized_();
  this.invalidateCache_();
  key = this.getKeyName_(key);
  var values = this.keyMap_.get(key);
  if (!values) {
    this.keyMap_.set(key, values = []);
  }
  values.push(value);
  this.count_ = goog.asserts.assertNumber(this.count_) + 1;
  return this;
};
/**
 * @param {string} key
 * @return {boolean}
 */
goog.Uri.QueryData.prototype.remove = function(key) {
  this.ensureKeyMapInitialized_();
  key = this.getKeyName_(key);
  if (this.keyMap_.containsKey(key)) {
    this.invalidateCache_();
    this.count_ = goog.asserts.assertNumber(this.count_) - this.keyMap_.get(key).length;
    return this.keyMap_.remove(key);
  }
  return false;
};
goog.Uri.QueryData.prototype.clear = function() {
  this.invalidateCache_();
  this.keyMap_ = null;
  this.count_ = 0;
};
/**
 * @return {boolean}
 */
goog.Uri.QueryData.prototype.isEmpty = function() {
  this.ensureKeyMapInitialized_();
  return this.count_ == 0;
};
/**
 * @param {string} key
 * @return {boolean}
 */
goog.Uri.QueryData.prototype.containsKey = function(key) {
  this.ensureKeyMapInitialized_();
  key = this.getKeyName_(key);
  return this.keyMap_.containsKey(key);
};
/**
 * @param {*} value
 * @return {boolean}
 */
goog.Uri.QueryData.prototype.containsValue = function(value) {
  var vals = this.getValues();
  return goog.array.contains(vals, value);
};
/**
 * @param {function(this:SCOPE,?,string,!goog.Uri.QueryData)} f
 * @param {SCOPE=} opt_scope
 * @template SCOPE
 */
goog.Uri.QueryData.prototype.forEach = function(f, opt_scope) {
  this.ensureKeyMapInitialized_();
  this.keyMap_.forEach(function(values, key) {
    goog.array.forEach(values, function(value) {
      f.call(opt_scope, value, key, this);
    }, this);
  }, this);
};
/**
 * @return {!Array<string>}
 */
goog.Uri.QueryData.prototype.getKeys = function() {
  this.ensureKeyMapInitialized_();
  var vals = this.keyMap_.getValues();
  var keys = this.keyMap_.getKeys();
  var rv = [];
  for (var i = 0; i < keys.length; i++) {
    var val = vals[i];
    for (var j = 0; j < val.length; j++) {
      rv.push(keys[i]);
    }
  }
  return rv;
};
/**
 * @param {string=} opt_key
 * @return {!Array<?>}
 */
goog.Uri.QueryData.prototype.getValues = function(opt_key) {
  this.ensureKeyMapInitialized_();
  var rv = [];
  if (typeof opt_key === "string") {
    if (this.containsKey(opt_key)) {
      rv = goog.array.concat(rv, this.keyMap_.get(this.getKeyName_(opt_key)));
    }
  } else {
    var values = this.keyMap_.getValues();
    for (var i = 0; i < values.length; i++) {
      rv = goog.array.concat(rv, values[i]);
    }
  }
  return rv;
};
/**
 * @param {string} key
 * @param {*} value
 * @return {!goog.Uri.QueryData}
 */
goog.Uri.QueryData.prototype.set = function(key, value) {
  this.ensureKeyMapInitialized_();
  this.invalidateCache_();
  key = this.getKeyName_(key);
  if (this.containsKey(key)) {
    this.count_ = goog.asserts.assertNumber(this.count_) - this.keyMap_.get(key).length;
  }
  this.keyMap_.set(key, [value]);
  this.count_ = goog.asserts.assertNumber(this.count_) + 1;
  return this;
};
/**
 * @param {string} key
 * @param {*=} opt_default
 * @return {*}
 */
goog.Uri.QueryData.prototype.get = function(key, opt_default) {
  if (!key) {
    return opt_default;
  }
  var values = this.getValues(key);
  return values.length > 0 ? String(values[0]) : opt_default;
};
/**
 * @param {string} key
 * @param {!Array<?>} values
 */
goog.Uri.QueryData.prototype.setValues = function(key, values) {
  this.remove(key);
  if (values.length > 0) {
    this.invalidateCache_();
    this.keyMap_.set(this.getKeyName_(key), goog.array.clone(values));
    this.count_ = goog.asserts.assertNumber(this.count_) + values.length;
  }
};
/**
 * @return {string}
 * @override
 */
goog.Uri.QueryData.prototype.toString = function() {
  if (this.encodedQuery_) {
    return this.encodedQuery_;
  }
  if (!this.keyMap_) {
    return "";
  }
  var sb = [];
  var keys = this.keyMap_.getKeys();
  for (var i = 0; i < keys.length; i++) {
    var key = keys[i];
    var encodedKey = goog.string.urlEncode(key);
    var val = this.getValues(key);
    for (var j = 0; j < val.length; j++) {
      var param = encodedKey;
      if (val[j] !== "") {
        param += "\x3d" + goog.string.urlEncode(val[j]);
      }
      sb.push(param);
    }
  }
  return this.encodedQuery_ = sb.join("\x26");
};
/**
 * @return {string}
 */
goog.Uri.QueryData.prototype.toDecodedString = function() {
  return goog.Uri.decodeOrEmpty_(this.toString());
};
/** @private */ goog.Uri.QueryData.prototype.invalidateCache_ = function() {
  this.encodedQuery_ = null;
};
/**
 * @param {Array<string>} keys
 * @return {!goog.Uri.QueryData}
 */
goog.Uri.QueryData.prototype.filterKeys = function(keys) {
  this.ensureKeyMapInitialized_();
  this.keyMap_.forEach(function(value, key) {
    if (!goog.array.contains(keys, key)) {
      this.remove(key);
    }
  }, this);
  return this;
};
/**
 * @return {!goog.Uri.QueryData}
 */
goog.Uri.QueryData.prototype.clone = function() {
  var rv = new goog.Uri.QueryData;
  rv.encodedQuery_ = this.encodedQuery_;
  if (this.keyMap_) {
    rv.keyMap_ = this.keyMap_.clone();
    rv.count_ = this.count_;
  }
  return rv;
};
/**
 * @private
 * @param {*} arg
 * @return {string}
 */
goog.Uri.QueryData.prototype.getKeyName_ = function(arg) {
  var keyName = String(arg);
  if (this.ignoreCase_) {
    keyName = keyName.toLowerCase();
  }
  return keyName;
};
/**
 * @param {boolean} ignoreCase
 */
goog.Uri.QueryData.prototype.setIgnoreCase = function(ignoreCase) {
  var resetKeys = ignoreCase && !this.ignoreCase_;
  if (resetKeys) {
    this.ensureKeyMapInitialized_();
    this.invalidateCache_();
    this.keyMap_.forEach(function(value, key) {
      var lowerCase = key.toLowerCase();
      if (key != lowerCase) {
        this.remove(key);
        this.setValues(lowerCase, value);
      }
    }, this);
  }
  this.ignoreCase_ = ignoreCase;
};
/**
 * @param {...(?goog.Uri.QueryData|?goog.structs.Map<?,?>|?Object)} var_args
 * @suppress {deprecated}
 */
goog.Uri.QueryData.prototype.extend = function(var_args) {
  for (var i = 0; i < arguments.length; i++) {
    var data = arguments[i];
    goog.structs.forEach(data, function(value, key) {
      this.add(key, value);
    }, this);
  }
};

//# sourceMappingURL=goog.uri.uri.js.map
