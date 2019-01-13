
# The beauty of oz markdown

Imagine writing a lovely little markdown file, when suddenly, you need a data visualization.

With oz, this magical code

    Turns into this

    ```edn-vega-lite
    {:data {:values [{:a 2 :b 3 :c "green"} {:a 5 :b 2 :c "green"} {:a 7 :b 4 :c "purple"}]}
     :mark :point
     :width 400
     :encoding {:x {:field "a"}
                :y {:field "b"}
                :color {:field "c"}}}
    ```

Turns into this

```edn-vega-lite
{:data {:values [{:a 2 :b 3 :c "green"} {:a 5 :b 2 :c "green"} {:a 7 :b 4 :c "purple"} {:a 3 :b 3 :c "purple"}]}
 :mark :point
 :width 400
 :encoding {:x {:field "a"}
            :y {:field "b"}
            :color {:field "c"}}}
```

Will eventually make this look like this for better editor and md viewer formatting:

    ```edn vega-lite
    {:data {:values [{:a 2 :b 3 :c "green"} {:a 5 :b 2 :c "green"} {:a 7 :b 4 :c "purple"}]}
     ,,,}
    ```

    

