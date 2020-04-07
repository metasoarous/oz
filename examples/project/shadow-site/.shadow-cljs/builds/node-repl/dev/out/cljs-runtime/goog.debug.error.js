goog.provide("goog.debug.Error");
/**
 * @constructor
 * @extends {Error}
 * @param {*=} opt_msg
 */
goog.debug.Error = function(opt_msg) {
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, goog.debug.Error);
  } else {
    /** @const */ var stack = (new Error).stack;
    if (stack) {
      /** @override */ this.stack = stack;
    }
  }
  if (opt_msg) {
    /** @override */ this.message = String(opt_msg);
  }
  /** @type {boolean} */ this.reportErrorToServer = true;
};
goog.inherits(goog.debug.Error, Error);
/** @override */ goog.debug.Error.prototype.name = "CustomError";

//# sourceMappingURL=goog.debug.error.js.map
