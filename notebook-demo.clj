
;; # Welcome to Oz 2.0!

;; This is the next iteration of the ideas started under Oz `1.6.0-alpha*`:

;; * Overall
;;   * greatly improved default fonts and styles
;;   * `compile` multimethod system for general purpose (recursive) transformations
;; * Namespace as a notebook (`live-watch!`, `build!`)
;;   * process `;; ` comments as markdown
;;   * renders code, stdout and stderr
;;     * but can be turned off for reporting
;;   * **parallel** evaluation of independent code blocks
;;   * adds an `:oz.doc/async-block` model
;;     * you see updates as they happen
;;     * no more waiting till completion
;;   * failing blocks present stacktraces
;;     * downstream blocks show
;;   * faster file watching on mac (ht `beholder`)
;; * Reagent components
;;   * added API hooks to access the full vega view API
;;     * use `:view-callback` parameter
;;     * stream data into a viz
;;     * update attributes of individual datums in a viz

;; Please check out `2.0.0-alpha2`

;; # Out of the hammock

;; I've spent the last almost couple of years thinking about how these relate:
;; * static compilation
;;   * pdf (goal)
;;   * static but interactive html
;; * "live" view updates as code re-evaluates
;;   * partial page updates
;; * repl tooling
;; * reagent components

;; The difference between redesigning a Swiss Army Knife versus redesigning a steak knife.


;; ## Show me


;; Markdown comments look like this:

;;     ;; Markdown comments look like this

;; A comment line that does not start precisely with `;;<space>` will _not_ be rendered as markdown.

;; Rationale:

;; * Need to be able to comment out code
;; * Markdown is indentation sensitive so avoid possible confusion
;; * Follows standard style convention, and matches Marginalia functionality


;; ## Markdown comments can have metadata

;;     ;; ^{:style {:color :red}}
;;     ;; Some lofty prose

;;  ⇨ 

;; ^{:style {:color :red}}
;; Some lofty prose


;; We take care to unwrap single `:div` or `:p` tags that have only a single item, such as an image, so that properties can be embedded directly on the image.


;;     ;; ^{:style {:max-width 300} :align :center}
;;     ;; ![](https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Balaeniceps_rex_-Ueno_Zoo%2C_Tokyo%2C_Japan_-upper_body-8a.jpg/1920px-Balaeniceps_rex_-Ueno_Zoo%2C_Tokyo%2C_Japan_-upper_body-8a.jpg)

;;  ⇨ 

;; ^{:style {:max-width 300} :align :center}
;; ![](https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Balaeniceps_rex_-Ueno_Zoo%2C_Tokyo%2C_Japan_-upper_body-8a.jpg/1920px-Balaeniceps_rex_-Ueno_Zoo%2C_Tokyo%2C_Japan_-upper_body-8a.jpg)

;; BTW, this ^ is a shoebill.
;; They are fucking awesome and prove that birds are in fact dinosaurs.



;; ## Code!

(ns notebook-demo
  (:require [clojure.set :as set]
            [clojure.string :as string]))

;; Note the status indicators on the bottom right of the code?

;; ### stdout and stderr

;; Oz has to consider both the interactive notebooks _and_ static output, so more explicitly opting into display is preferable as a default (for now, though this is up for debate).

;; To opt into output, simply `print`

(do
  (Thread/sleep 5001)
  (println "this is fancy, isn't it?"))

;; Note that if we change the code, we get a pending status till the result comes in.


;; ### Dependencies

(do
  (Thread/sleep 2000)
  (def a-funny-thing "I heard today..."))

(do
  (Thread/sleep 2000)
  (println "Let me tell you:" a-funny-thing))


;; Note the dependencies on the bottom form ^


;; ### Parallel execution!

(do
  (Thread/sleep 2000)
  (def thing-1 "jack"))

(do
  (Thread/sleep 2000)
  (def thing-2 "jill"))

(println thing-1 "and" thing-2)


;; ### Error handling

;; Errors are caught and you get a stacktrace

(do
  (Thread/sleep 1000)
  (/ 3 0)
  (println "you won't see me..."))

;; Downstream dependencies are stopped as well

(defn a-broken-function
  [x]
  (/ 14 x))

(def something-broken
  (a-broken-function 0))

(def something-implicitly-broken
  (println "do something cool with" something-broken))

;; Note that we can follow the dependency chain here to track where the error was


(defmulti f first)

(defmethod f :fire
  ([[_ message]]
   (str message " is HAWT!")))

(defmethod f :default
  ([[k message]]
   [:default [k message]]))

(print (f [:fire "global warming"]))

(print (f ["I'm on" :fire]))


;; ### Old hiccup notation

;; Together with some new utility helpers:

;[:data-table
 ;[{:a 5 :b 7} {:a 8 :b -9} {:a 8 :b 8}]]

;; (slideshow view coming soon)


[:vega-lite
 {:data {:url "https://vega.github.io/vega-lite/data/cars.json"}
  :description "A scatterplot showing horsepower and miles per gallon for various cars."
  :encoding {:x {:field "Horsepower" :type "quantitative"}
             :y {:field "Miles_per_Gallon" :type "quantitative"}}
  :mark {:type "point"
         :tooltip {:content :data}}}
 {:renderer :svg}]


[:vega-lite
  {;:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:url "https://vega.github.io/vega-lite/data/sp500.csv"}
   :encoding {:x {:field "date"
                  :type "temporal"}
              :y {:field "price"
                  :type "quantitative"}}
   :mark "point"
   :width 480}
  {:renderer :svg}]

[:vega-lite
  {;:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:url "https://vega.github.io/vega-lite/data/sp500.csv"}
   :vconcat [{:encoding {:x {:axis {:title ""}
                             :field "date"
                             :scale {:domain {:param "brush"}}
                             :type "temporal"}
                         :y {:field "price" :type "quantitative"}}
              :mark "area"
              :width 480}
             {:encoding {:x {:field "date" :type "temporal"}
                         :y {:axis {:grid false :tickCount 3}
                             :field "price"
                             :type "quantitative"}}
              :height 60
              :mark "area"
              :params [{:name "brush" :select {:encodings ["x"] :type "interval"}}]
              :width 480}]}
  {:renderer :svg}]


;; <br/>

;; # Computer Modern FTW

;; The following shows how we can take a chunk of text and apply a particular set of styles to them

;; You can change these by adding metadata annotations (e.g. `;; ^{:class :bright}`)

;; ^{:class :fancy}
;; ### Computer Modern Serif

;; `^{:class :serif}` or `^{:class :fancy}`

;; ^{:class :fancy}
;; Hey there. This is what your text _could_ look like if you switched to **Computer Modern Serif**.
;; It really does have a nice feel to it for a serif font.
;; Very pleasing to look at for extended periods of time.
;; Knuth was a _real_ polymath.

;; ^{:class :sans}
;; ### Computer Modern Sans

;; `^{:class :sans}`

;; ^{:class :sans}
;; Hey there. This is what your text _could_ look like if you switched to **Computer Modern Sans**.
;; It really does have a nice feel to it for a sans font.
;; Very pleasing to look at for extended periods of time.
;; Knuth was a _real_ polymath.

;; ^{:class :typewriter}
;; ### Computer Modern Typewriter

;; `^{:class :typewriter}`

;; ^{:class :typewriter}
;; Hey there. This is what your text _could_ look like if you switched to **Computer Modern Typewriter**.
;; It really does have a nice feel to it for a sans font.
;; Very pleasing to look at for extended periods of time.
;; Knuth was a _real_ polymath.

;; ^{:class :bright}
;; ### Computer Modern Bright

;; `^{:class :bright}`

;; ^{:class :bright}
;; Hey there. This is what your text _could_ look like if you switched to **Computer Modern Bright**.
;; It really does have a nice feel to it for a sans-serif font.
;; Very pleasing to look at for extended periods of time.
;; Knuth was a _real_ polymath.

;; ^{:class :concrete}
;; ### Computer Modern Concrete

;; `^{:class :concrete}`

;; ^{:class :concrete}
;; Hey there. This is what your text _could_ look like if you switched to **Computer Modern Concrete** (the default).
;; It really does have a nice feel to it for a serif font.
;; Very pleasing to look at for extended periods of time.
;; Knuth was a _real_ polymath.
  
;; </br>



;; ## _Namespace as a notebook_ vs _scientific documentation_

;; Scientific document implies so much more:

;; * Code documentation
;; * Report generation
;; * Slideshows
;; * Technicals blogs or websites
;; * Knowledge management space?

;; However, conceptually it does not capture the  has resonated with people, and is certainly _part_ of the vision.


;; ## Related work

;; Kudos to Notespace and Clerk for innovation in this space, and exploring different ways of implementing these ideas.

;; * Clerk's paginated data inspection is very cool
;;   * This conflicts some with Oz's notion


;; ## Timeline

;; Oz `1.6.0` was technically in alpha (though relatively stable) for a couple years.
;; I left it this way to leave wiggle room for this phase (but intend to preserve as much of the API as possible).

;; * 2-3 months in `alpha`
;;   * currently broken functionality and incomplete implementations
;;   * work out bugs and let new design settle
;; * 1-2 months in `beta`
;; * Full release!


;; ## Roadmap

;; * Splitting apart components into separate subsystems
;;   * solve the "Swiss Army Knife" problem?
;;   * minimize dependencies for those who don't need everything
;; * Finish implementing smarter system for processing batches of edits together
;;   * some editors (vim) are weird, and git can cause weird things as well
;; * Implement paginated data structure navigators


;; ## Known issues:

;; * non-watched source files can't trigger updates
;; * not able to force reload of a file, compounding problem above
;; * live view can look subtly different from the static build



;; <br/>

;; # Thank you!

;; <br/>


