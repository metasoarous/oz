
# CHANGELOG


## 1.3.1

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

