# Styling options

## html

### from-format or mode

Used interchangeably, where from-format takes precedent. Possible values are:

:vega :vega-lite :markdown :md :pprint :print

Passes along any options to html-head.

## html-head

### description

Used as the meta "description" element. Otherwise set to "Oz document".

### title

Used as the "title" element. Otherwise set to "Oz document".

### author

Used as the meta "author" element. Otherwise empty.

### keywords

Used as the meta "keywords" element concatenated with ",". Should be a vector of strings.
If (:tag (meta doc)) it is merged on top.
Otherwise empty.

### omit-shortcut-icon? and shortcut-icon-url

If not true, a link shortcut-icon-url is used as the "link"
(displays as a little icon in the browser title bar) element. If empty "http://ozviz.io/oz.svg"
gets used.

### omit-styles?

If not true, "oz/public/css/style.css", "http://ozviz.io/fonts/lmroman12-regular.woff" and
"https://fonts.googleapis.com/css?family=Open+Sans" are included.

### omit-vega-libs?

If not true, the cdn versions of vega, vega-lite, and vega-embed are included.

### omit-highlightjs?

If not true, "metasoarous/highlight.js" is included per cdn.

### omit-mathjax?

If not true, "mathjax" is included per cdn.

### omit-charset?

If not true, "UTF-8" is set to as the default

### header-extras

Is passed along without modification at the end of the header. Must be valid hiccup.
