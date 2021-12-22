# Build

[`build!`](#static-site-generation): generate a static website from
directories of markdown, hiccup &/or interactive Vega-Lite & Vega visualizations, while being able to see changes live (as with `live-view!`)

## Static site generation

If you've ever thought, "man, I wish there was a static site generation
toolkit which had live code reloading of whatever page you're currently
editing, and it would be great if it was in Clojure and let me embed data
visualizations and math formulas via LaTeX in Markdown & Hiccup documents",
boy, are you in for a treat!

Oz now features exactly such features in the form of the `oz/build!`.
A very simple site might be generated with:

```clojure
(build!
  [{:from "examples/static-site/src/"
    :to "examples/static-site/build/"}])
```

The input formats currently supported by `oz/build!` are

* `md`: As described above, markdown with embedded Vega-Lite or Vega visualizations, Latex, and hiccup
* `json`, `edn`: You can directly supply hiccup data for more control over layout and content
* `clj`: Will `live-reload!` Clojure files (as described above), and render the last form evaluated as hiccup

Oz should handle image and css files it comes across by simply copying them over.
However, if you have any `json` or `edn` assets (datasets perhaps) which need to pass through unchanged,
you can separate these into their own build specification, like so:

```clojure
(defn site-template
  [spec]
  [:div {:style {:max-width 900 :margin-left "auto" :margin-right "auto"}}
   spec])

(build!
  [{:from "examples/static-site/src/site/"
    :to "examples/static-site/build/"
    :template-fn site-template}
   ;; If you have static assets, like datasets or imagines which need to be simply copied over
   {:from "examples/static-site/src/assets/"
    :to "examples/static-site/build/"
    :as-assets? true}])
```

This can be a good way to separate document code from other static assets.

Specifying multiple builds like this can be used to do other things as well.
For example, if you wanted to render a particular set of pages using a different
template function (for example, so that your blog posts style differently than the main pages),
you can do that easily

```clojure
(defn blog-template
  [spec]
  (site-template
    (let [{:as spec-meta :keys [title published-at tags]} (meta spec)]
      [:div
       [:h1 {:style {:line-height 1.35}} title]
       [:p "Published on: " published-at]
       [:p "Tags: " (string/join ", " tags)]
       spec])))

(build!
  [{:from "examples/static-site/src/site/"
    :to "examples/static-site/build/"
    :template-fn site-template}
   {:from "examples/static-site/src/blog/"
    :to "examples/static-site/build/blog/"
    :template-fn blog-template}
   ;; If you have static assets, like datasets or imagines which need to be simply copied over
   {:from "examples/static-site/src/assets/"
    :to "examples/static-site/build/"
    :as-assets? true}])
```

Note that the `blog-template` above is using metadata about the spec to inform how it renders.
This metadata can be written into Markdown files using a yaml markdown metadata header (see `/examples/static-site/src/`)

```
---
title: Oz static websites rock
tags: oz, dataviz
---

# Oz static websites!

Some markdown content...
```

The title in particular here will wind it's way into the `Title` metadata tag of your output HTML
document, and thus will be visible at the top of your browser window when you view the file.
This is a pattern that Jekyll and some other blogging engines use, and `markdown-clj` now
supports extracting this data.

Again, as you edit and save these files, the outputs just automatically update for you, both as
compiled HTML files, and in the live-view window which lets you see your changes as you make em.
If you need to change a template, or some other detail of the specs, you can simply rerun `build!`
with the modified arguments, and the most recently edited page will updated before your eyes.
This provides for a lovely live-view editing experience from the comfort of your favorite editor.

### Deploy

When you're done, one of the easiest ways to deploy is with the excellent `surge.sh` toolkit,
which makes static site deployment a breeze.
You can also use GitHub Pages or S3 or really whatever if you prefer.
The great thing about static sites is that they are easy and cheap to deploy and scale, so you
have plenty of options at your disposal.
