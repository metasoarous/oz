![oz](resources/public/oz.svg)

Great and powerful data visualizationz

<br/>


## Overview

Oz is a library for data-driven, REPL-based data visualization in the browser, using vega and vega lite.

A fork of [vizard](https://github.com/yieldbot/vizard), oz differs from its ancestor in providing:

* both vega-lite _and_ vega support (vizard is vega-lite only)
* an API for describing dashboard-like composites of vega-based views using hiccup

It also has the following eventual goals:

* provide an API for combining vega and vega-lite into a single plot (vega for detailed control, vega-lite for the simple bits)
* higher level viz constructors, as they accrete and become useful


## Usage

Add oz to your leiningen project dependencies

[![Clojars Project](https://img.shields.io/clojars/v/metasoarous/oz.svg)](https://clojars.org/metasoarous/oz)


To get things going, require oz and start the plot server as follows:

``` clojure
(require '[oz.core :as oz])

(oz/start-plot-server!)
```

This will fire up a browser window with a websocket connection for funneling view data back and forth.

Next we'll define a function for generating some dummy data

```clojure
(defn group-data [& names]
  (apply concat (for [n names]
  (map-indexed (fn [i x] {:x i :y x :col n}) (take 20 (repeatedly #(rand-int 100)))))))
```


### `oz/p!`

The simplest function for displaying vega is `oz/p!`.
It will display a single vega or vega-lite plot in any connected browser windows.

For example, a simple line plot:

``` clojure
  (def line-plot
    {:data {:values (group-data "monkey" "slipper" "broom")}
     :encoding {:x {:field "x"}
                :y {:field "y"}
                :color {:field "col" :type "nominal"}}
     :mark "line"})

  ;; Render the plot to the 
  (oz/v! line-plot)
```

Should render something like:

![lines plot](doc/lines.png)


Another example:

```clojure
(def stacked-bar
  {:data {:values (group-data "munchkin" "witch" "dog" "lion" "tiger" "bear")}
   :mark "bar"
   :encoding {:x {:field "x"
                  :type "ordinal"}
              :y {:aggregate "sum"
                  :field "y"
                  :type "quantitative"}
              :color {:field "col"
                      :type "nominal"}}})

(oz/v! stacked-bar)
```

This should render something like:

![bars plot](doc/bars.png)


### vega support

For vega instead of vega-lite, you can also specify `:mode :vega` to `oz/v!`:

```clojure
;; load some example vega (this may only work from within a checkout of oz; haven't checked)
(def vega-data (json/parse-string (slurp (clojure.java.io/resource "example-cars-plot.vega.json")))) 
(oz/v! vega-data :mode :vega)
```

This should render like:

![cars plot](doc/car-points.png)


### `ox/view!`

This is a more powerful function which will let you compose vega and vega-lite views together with other html, using hiccup notation.
The idea is to provide some quick and dirty utilities for building composite view dashboards.

For demonstration we'll combine the three plots above into one:

```clojure
(oz/view! [:div
           [:h1 "Look ye and behold"]
           [:p "A couple of small charts"]
           [:div {:style {:display "flex" :flex-direction "row"}}
            [:vega-lite line-plot]
            [:vega vega-data]]
           [:p "A wider, more expansive chart"]
           [:vega-lite stacked-bar]
           [:h2 "If ever, oh ever a viz there was, the vizard of oz is one because, because, because..."]
           [:p "Because of the wonderful things it does"]])
```

Note that the vega and vega-lite specs are described in the output vega as using the `:vega` and `:vega-lite` keys.

You should now see something like this:

![composite view](doc/composite-view.png)


## Local Development

First, start up figwheel
``` clojure
  (do-it-fools!)
```

## License

Copyright © 2018 Christopher Small

Forked from Vizard - Copyright © 2017 Yieldbot, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

