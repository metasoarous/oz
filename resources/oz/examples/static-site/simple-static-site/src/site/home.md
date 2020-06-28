---
title: Yo dawg
---


# Yo dawg!

We heard you like **data vizualizationz**.

So we put data vizualizationz in yo **Markdown** so you can be like

```edn vega
{:title "Blao!"
 :data {:values [{:time 1 :dopeness 3}
                 {:time 5 :dopeness 4}
                 {:time 9 :dopeness 12}]}
 :mark :line
 :encoding {:x {:field :time} :y {:field :dopeness}}
 }
```

    ```edn vega
    {:title "Blao!"
     :data {:values [{:time 1 :dopeness 3}
                     {:time 5 :dopeness 4}
                     {:time 9 :dopeness 12}]}
     :mark :line
     :encoding {:x {:field :time} :y {:field :dopeness}}
     }
    ```

## Look ye and **behold!**

A resized image as hiccup:

```edn hiccup
[:img {:src "/img/oz.svg" :style {:width 80 :padding-top 20}}]
;[:img {:src "/img/oz.svg"}]
```

whence

    ```edn hiccup
    [:img {:src "/img/oz.svg" :style {:max-width 400 :padding-top 20}}]
    ```

## Full LaTeX support coming soon!

Right now you'll only see this render on the static export, not on the live view

$$ e ^ {i \theta} = \cos \theta + i \sin \theta $$

