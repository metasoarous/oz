
# CHANGELOG


## 1.6.0

* add `live-reload!` and `live-view!` functions for figwheel like coding experience
* greatly improved embed performance via vega-embed update
* improve documentation on `start-plot-server!` by properly cloning var
* nil port to `view!` handled more elegantly
* fix oz logo in exported html
* dependency updates which seem to fix figwheel compatibility
* moved delay after starting server from specific setup step on view! to start-plot-server! to avoid vizs not showing up in scripts
* new general purpose `compile` function for arbitrary conversions
* more general purpose `export!` function for compiling to other formats
* more flexibility in html output (header, styles, favicons, etc)
* static compilation of vega(-lite) -> svg/png
* embedded visualizations by default include both a statically compiled view and live/interactive view (& either can be turned off)
* move to shadow-cljs for cljs compilation
* move to deps.edn for simpler dependency management and build tooling
* add react-error-boundary so that broken plots don't glob up the view! machinery
* add main function for CLI goodness
* update js libs:
  * vega -> "5.3.1-0"
  * vega-lite -> "3.0.0-rc16-0"
  * vega-tooltip -> "0.16.0-0"
  * vega-embed -> "4.0.0-rc1-0"


## 1.5.6

* fix Java version errors with IClojure


## 1.5.4, 1.5.5

* documentation improvements


## 1.5.3

* documentation improvements
* namespace resources/public as resources/oz/public to avoid resource clashes with Clojure CLI class path resolution


## 1.5.1, 1.5.2

* documentation improvements


## 1.5.0

* add preliminary `oz.core/export!` functionality (live vega output in html file)
* Jupyter notebook integration via clojupyter & iclojure kernels
* add `oz.core/read` function, which parses markdown with a notation for embedding vega(lite) visualizations


## 1.4.1

* fix bad default home path for osx github cred file


## 1.4.0

* improved styling
* fixed gist publishing by adding authorization
* deprecate `publish-plot!` in favor of more general `publish!`, which loads via http://ozviz.io for hiccup forms
* split reagent api from main app namespace to avoid bugs using oz as a reagent lib
* update aleph to avoid java 9 issues
* call start-plot-server! automatically on `v!` or `view!` if it hasn't been already
* update js libs:
  * cljsjs/vega -> "4.4.0-0"
  * cljsjs/vega-lite -> "3.0.0-rc10-0"
  * cljsjs/vega-embed -> "3.26.0-0"
  * cljsjs/vega-tooltip -> "0.14.0-0"


## 1.3.2

* update js libs:
  * vega 3.0.7 -> 3.2.1
  * vega-lite 2.0.0 -> 2.2.0
  * vega-tooltip 0.4.4 -> 0.6.1
  * vega-embed ? -> 3.1.1
* log translated vega-lite -> vega for inspection and debugging
* `[:vega {...}]` and `[:vega-lite {...}]` blocks now accept multiple spec maps, which get merged together on client
* add `:width` and `:height` opts to `v!` and `publish-plot!`


## 1.2.2

* bug fix for viz not rendering sometimes


## 1.2.1

* generalized hiccup support


## 1.1.1

Initial release; forked vizard and added following features:

* vega support
* simple hiccup composite support

