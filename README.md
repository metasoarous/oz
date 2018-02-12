# oz

Magic Visualization

## Overview

oz is a tiny client/server library meant to enable data-driven, REPL-based data visualization in the browser.

## Usage

Add oz to your leiningen project dependencies

``` clojure
[metasoarous/oz "1.0.1"]
```


In a repl:

``` clojure

    (require '[oz.core :as oz])

    (oz/start-plot-server!)

    (defn group-data [& names]
        (apply concat (for [n names]
        (map-indexed (fn [i x] {:x i :y x :col n}) (take 20 (repeatedly #(rand-int 100)))))))
```

Now send some plots off. Here is a stacked bar plot:

``` clojure
  (def stacked-bar
    {:data {:values (group-data "foo" "bar" "baz" "buh" "bunk" "dunk")}
     :mark "bar"
     :encoding {:x {:field "x"
                    :type "ordinal"}
                :y {:aggregate "sum"
                    :field "y"
                    :type "quantitative"}
                :color {:field "col"
                        :type "nominal"}}})

  (oz/p! stacked-bar)
```

Which should look something like this in when rendered in the browser:

![bar](doc/bar-lite.png)


For vega instead of vega-lite, you can also specify `:mode "vega"`:

```clojure
  (oz/p! stacked-bar :mode "vega")
```

You can specify or overide the data via `:data` key:

```clojure
  (oz/p! stacked-bar :data {:values (group-data "baz" "buh" "bunk" "dunk")})
```


## Local Development

First, start up figwheel
``` clojure
  (do-it-fools!)
```

## License

Copyright © 2018 Christopher Small

Forked from Vizard - Copyright © 2017 Yieldbot, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

