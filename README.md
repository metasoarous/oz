# vizard

Magic Visualization

## Overview

vizard is a tiny client/server library meant to enable REPL-based data visualization in the browser.

## Usage

Add vizard to your leiningen project dependencies

``` clojure
[yieldbot/vizard "1.0.1"]
```


In a repl:

``` clojure

    (require '[vizard [core :refer :all] [lite :as lite]])

    (start-plot-server!)

    (defn group-data [& names]
        (apply concat (for [n names]
        (map-indexed (fn [i x] {:x i :y x :col n}) (take 20 (repeatedly #(rand-int 100)))))))
```

Now send some plots off. Here is a stacked bar plot:

``` clojure
  (def stacked-bar (p! (lite/lite {:mark "bar"
                                   :encoding {:x {:field "x"
                                                  :type "ordinal"}
                                              :y {:aggregate "sum"
                                                  :field "y"
                                                  :type "quantitative"}
                                              :color {:field "col"
                                                      :type "nominal"}}}
                                  (group-data "foo" "bar" "baz" "buh" "bunk" "dunk"))))

```

Which should look something like this in when rendered in the browser:

![bar](doc/bar-lite.png)

Here's a multiple series line plot:

``` clojure
  (def multi-line (p! (lite/lite {:mark "line"
                                  :encoding {:x {:field "x"
                                                 :type "ordinal"}
                                             :y {:field "y"
                                                 :type "quantitative"}
                                             :color {:field "col"
                                                     :type "nominal"}}}
                                 (group-data "foo" "bar" "baz" "buh" "bunk" "dunk"))))
```

Which should look about like this:

![line](doc/line-lite.png)

## Local Development

First, start up figwheel
``` clojure
  (do-it-fools!)
```

## License

Copyright © 2017 Yieldbot, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
