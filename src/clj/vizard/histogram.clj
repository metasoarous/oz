(ns vizard.histogram
  "Functions for computing a histogram from a collection of numbers.")

(defn linspace
  "Generates n evenly-spaced points, with distance (x2-x1)/(n-1) between points."
  [x1 x2 n]
  (let [spacing (/ (- x2 x1) (dec n))]
    (range x1 (+ x1 (* spacing n)) spacing)))

(defmulti bin
  "Find histogram bin corresponding to x.
  Result is i when left-edge(i) >= value and right-edge(i) < value. note that
  the first and last bins correspond to half-open intervals."
  (fn [hist x] (:type hist)))

(defmulti edges :type)

(defn uniform
  "Get parameters for simple 1D histogram with uniform bin widths.
  Specify the domain of the histogram using xmin/xmax. Otherwise, the sample
  minimum and maximum will be used."
  ([xs bins]
   (let [xmin (apply min xs)
         xmax (apply max xs)]
     {:type :uniform
      ;; minimum value in data
      :xmin xmin
      ;; maximum value in data
      :xmax xmax
      ;; number of bins in histogram
      :bins bins
      ;; width of each histogram bin
      :width (/ (- xmax xmin) bins)})))

(defn custom
  "Create histogram with custom bin edges."
  [edges]
  {:type :custom
   :edges edges})

(defmethod bin :uniform [hist x]
  (let [{:keys [xmin width bins]} hist]
    (-> x
        (- xmin)
        (quot width)
        int
        (max 0)
        (min (dec bins)))))

(defmethod edges :uniform [hist]
  (let [{:keys [xmin xmax bins]} hist]
    (linspace xmin xmax (inc bins))))

(defmethod bin :default [hist x]
  (let [edges (:edges hist)
        idx (java.util.Collections/binarySearch edges x)]
    (if (>= idx 0)
      idx
      ;; index is negative when index = (-(insertion point) - 1)
      ;; insertion point is the index of the first element greater than
      ;; the key, or list.size() if all elements in the list are less
      ;; than the specified key. thus, insertion point gives right edge.
      (-> idx (+ 2) (* -1) (max 0) (min (- (count edges) 2))))))

(defmethod edges :default [hist]
  (:edges hist))

(defn histogram
  "Compute simple 1-D histogram with fixed number of equal-width bins.
  Specify the domain of the histogram using xmin/xmax. Otherwise, the sample
  minimum and maximum will be used."
  ([xs bins]
   (let [hist (uniform xs bins)
         counts (->> xs (map #(bin hist %)) frequencies)
         counts (map #(get counts % 0) (range bins))]
     {:edges (edges hist) :counts counts})))
