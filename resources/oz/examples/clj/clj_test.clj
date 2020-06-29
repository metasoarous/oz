(ns clj-test)


[:div
 [:h1 "yo dawgs"]
 [:markdown "what it _be_ like?"]
 [:vega-lite {:data {:values [{:a 1 :b 2} {:a 3 :b 1} {:a 8 :b 3}]}
              :mark :point
              :encoding {:x {:field :a}
                         :y {:field :b}}}]]
 
