
# The beauty of oz markdown

Imagine you're writing a lovely little markdown file, when suddenly, you need a data visualization.

With oz, this magical code

    ```edn vega-lite
    {:data {:values [{:a 2.1 :b 3.4 :c "Z"} {:a 5.2 :b 2.3 :c "Z"} {:a 7.4 :b 4.8 :c "Q"} {:a 3.2 :b 3.1 :c "Q"}]}
     :mark :point
     :width 400
     :encoding {:x {:field "a"}
                :y {:field "b"}
                :color {:field "c"}}}
    ```

Turns into this

```edn vega-lite
{:data {:values [{:a 2.1 :b 3.4 :c "Z"} {:a 5.2 :b 2.3 :c "Z"} {:a 7.4 :b 4.8 :c "Q"} {:a 3.2 :b 3.1 :c "Q"}]}
 :mark :point
 :width 400
 :encoding {:x {:field "a"}
            :y {:field "b"}
            :color {:field "c"}}}
```

The real magic here is in the code class specification `edn vega-lite`.
It's possible to replace `edn` with `json`, and `vega` with `vega-lite` as appropriate.
Additionally, these classes can be hyphenated for compatibility with editors/parsers that have problems with multiple class specifications (e.g. `edn-vega-lite`)

Note that embedding all of your data into a vega/vega-lite spec directly as `:values` may be untenable for larger data sets.
In these cases, the recommended solution is to post your data to a GitHub gist, or elsewhere online where you can refer to it using the `:url` syntax (e.g. `{:data {:url "https://your.data.url/path"} ...}`).

