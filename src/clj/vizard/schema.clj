(ns vizard.schema
  "Schema for Vega-Lite specs. Translated from the JSON schema
  https://vega.github.io/vega-lite/vega-lite-schema.json"
  (:use [schema.core :exclude [atom fn defn defmethod letfn defrecord]]))

(defn ranged [& {:keys [min max]}]
 (cond
   (and min max) (constrained Num (fn [x] (<= min x max)))
   min (constrained Num (fn [x] (<= min x)))
   max (constrained Num (fn [x] (<= x max)))
   :else Num))

(defn process-schema [schema]
  (if (map? schema)
    (into {} (for [[k v] schema] [(if (keyword? k) (optional-key k) k) v]))
    schema))

(defmacro defschema*
  "Like schema.core/defschema, but keyword map keys are optional by default."
  [name body]
  (list 'defschema name (process-schema body)))

(defschema*
 Formula
 {(required-key :field) Str, (required-key :expr) Str})

(defschema* Orient (enum "horizontal" "vertical"))

(defschema*
 NiceTime
 (enum "second" "minute" "hour" "day" "week" "month" "year"))

(defschema*
 Interpolate
 (enum
  "linear"
  "linear-closed"
  "step"
  "step-before"
  "step-after"
  "basis"
  "basis-open"
  "basis-closed"
  "cardinal"
  "cardinal-open"
  "cardinal-closed"
  "bundle"
  "monotone"))

(defschema* BandSize (enum "fit"))

(defschema*
 Mark
 (enum
  "area"
  "bar"
  "line"
  "point"
  "text"
  "tick"
  "rule"
  "circle"
  "square"
  "errorBar"))

(defschema*
 AxisConfig
 {:gridDash [Num],
  :properties Any,
  :labels Bool,
  :titleFontSize Num,
  :labelMaxLength (ranged :min 1),
  :tickPadding Num,
  :labelBaseline Str,
  :gridOpacity Num,
  :titleOffset Num,
  :offset Num,
  :tickWidth Num,
  :tickLabelColor Str,
  :grid Bool,
  :tickSizeEnd (ranged :min 0),
  :shortTimeLabels Bool,
  :titleMaxLength (ranged :min 0),
  :labelAngle Num,
  :titleFont Str,
  :tickSizeMinor (ranged :min 0),
  :titleColor Str,
  :tickLabelFontSize Num,
  :tickSize (ranged :min 0),
  :layer Str,
  :gridWidth Num,
  :subdivide Num,
  :axisWidth Num,
  :ticks (ranged :min 0),
  :tickSizeMajor (ranged :min 0),
  :characterWidth Num,
  :labelAlign Str,
  :gridColor Str,
  :tickLabelFont Str,
  :axisColor Str,
  :titleFontWeight Str,
  :tickColor Str})

(defschema* AxisOrient (enum "top" "right" "left" "bottom"))

(defschema* VerticalAlign (enum "top" "middle" "bottom"))

(defschema* FontWeight (enum "normal" "bold"))

(defschema* HorizontalAlign (enum "left" "right" "center"))

(defschema*
 ScaleType
 (enum
  "linear"
  "log"
  "pow"
  "sqrt"
  "quantile"
  "quantize"
  "ordinal"
  "time"
  "utc"))

(defschema* SortOrder (enum "ascending" "descending" "none"))

(defschema*
 TimeUnit
 (enum
  "year"
  "month"
  "day"
  "date"
  "hours"
  "minutes"
  "seconds"
  "milliseconds"
  "yearmonth"
  "yearmonthdate"
  "yearmonthdatehours"
  "yearmonthdatehoursminutes"
  "yearmonthdatehoursminutesseconds"
  "monthdate"
  "hoursminutes"
  "hoursminutesseconds"
  "minutesseconds"
  "secondsmilliseconds"
  "quarter"
  "yearquarter"
  "quartermonth"
  "yearquartermonth"))

(defschema*
 Bin
 {:min Num,
  :max Num,
  :base Num,
  :step Num,
  :steps [Num],
  :minstep Num,
  :div [Num],
  :maxbins (ranged :min 2)})

(defschema*
 Shape
 (enum
  "circle"
  "square"
  "cross"
  "diamond"
  "triangle-up"
  "triangle-down"))

(defschema*
 ScaleConfig
 {:useRawDomain Bool,
  :fontSizeRange [Num],
  :shapeRange (cond-pre [Str] Str),
  :bandSize (cond-pre BandSize Num),
  :tickSizeRange [Num],
  :barSizeRange [Num],
  :textBandWidth (ranged :min 0),
  :nominalColorRange (cond-pre [Str] Str),
  :round Bool,
  :opacity [Num],
  :padding Num,
  :ruleSizeRange [Num],
  :sequentialColorRange (cond-pre [Str] Str),
  :pointSizeRange [Num]})

(defschema* FacetScaleConfig {:round Bool, :padding Num})

(defschema*
 AggregateOp
 (enum
  "values"
  "count"
  "valid"
  "missing"
  "distinct"
  "sum"
  "mean"
  "average"
  "variance"
  "variancep"
  "stdev"
  "stdevp"
  "median"
  "q1"
  "q3"
  "modeskew"
  "min"
  "max"
  "argmin"
  "argmax"))

(defschema*
 SortField
 {(required-key :field) Str,
  (required-key :op) AggregateOp,
  :order SortOrder})

(defschema* Type (enum "quantitative" "ordinal" "temporal" "nominal"))

(defschema*
 FieldDef
 {:field Str,
  :type Type,
  :value (cond-pre Str Num Bool),
  :timeUnit TimeUnit,
  :bin (cond-pre Bin Bool),
  :aggregate AggregateOp,
  :title Str})

(defschema* StackOffset (enum "zero" "center" "normalize" "none"))

(defschema* FontStyle (enum "normal" "italic"))

(defschema*
 MarkConfig
 {:baseline VerticalAlign,
  :strokeOpacity (ranged :min 0 :max 1),
  :format Str,
  :align HorizontalAlign,
  :stroke Str,
  :applyColorToBackground Bool,
  :dx Num,
  :color Str,
  :tension Num,
  :barSize Num,
  :fill Str,
  :strokeDash [Num],
  :dy Num,
  :shortTimeLabels Bool,
  :fillOpacity (ranged :min 0 :max 1),
  :orient Orient,
  :interpolate Interpolate,
  :angle Num,
  :strokeDashOffset Num,
  :theta Num,
  :radius Num,
  :font Str,
  :size Num,
  :tickThickness Num,
  :stacked StackOffset,
  :tickSize Num,
  :strokeWidth (ranged :min 0),
  :opacity (ranged :min 0 :max 1),
  :fontStyle FontStyle,
  :fontWeight FontWeight,
  :shape (cond-pre Shape Str),
  :barThinSize Num,
  :fontSize Num,
  :ruleSize Num,
  :filled Bool,
  :lineSize Num,
  :text Str})

(defschema*
 CellConfig
 {:strokeOpacity Num,
  :stroke Str,
  :clip Bool,
  :fill Str,
  :strokeDash [Num],
  :width Num,
  :fillOpacity Num,
  :strokeDashOffset Num,
  :strokeWidth Num,
  :height Num})

(defschema*
 LegendConfig
 {:properties Any,
  :symbolSize Num,
  :titleFontSize Num,
  :labelBaseline Str,
  :gradientStrokeWidth Num,
  :offset Num,
  :shortTimeLabels Bool,
  :labelOffset Num,
  :gradientHeight Num,
  :orient Str,
  :gradientStrokeColor Str,
  :labelColor Str,
  :titleFont Str,
  :symbolShape Str,
  :titleColor Str,
  :gradientWidth Num,
  :padding Num,
  :labelFont Str,
  :labelAlign Str,
  :labelFontSize Num,
  :symbolColor Str,
  :symbolStrokeWidth (ranged :min 0),
  :titleFontWeight Str,
  :margin Num})

(defschema*
 DateTime
 {:quarter (ranged :min 1 :max 4),
  :day (cond-pre Str Num),
  :date (ranged :min 1 :max 31),
  :month (cond-pre Str Num),
  :seconds (ranged :min 0 :max 59),
  :year Num,
  :hours (ranged :min 0 :max 23),
  :milliseconds (ranged :min 0 :max 999),
  :minutes (ranged :min 0 :max 59)})

(defschema*
 EqualFilter
 {:timeUnit TimeUnit,
  (required-key :field) Str,
  :equal (cond-pre DateTime (cond-pre Str Num Bool))})

(defschema*
 OneOfFilter
 {:timeUnit TimeUnit,
  (required-key :field) Str,
  :oneOf [(cond-pre DateTime (cond-pre Str Num Bool))]})

(defschema*
 Axis
  {:gridDash [Num],
   :properties Any,
   :labels Bool,
   :titleFontSize Num,
   :format Str,
   :labelMaxLength (ranged :min 1),
   :tickPadding Num,
   :labelBaseline Str,
   :gridOpacity Num,
   :titleOffset Num,
   :offset Num,
   :tickWidth Num,
   :tickLabelColor Str,
   :grid Bool,
   :tickSizeEnd (ranged :min 0),
   :shortTimeLabels Bool,
   :orient AxisOrient,
   :titleMaxLength (ranged :min 0),
   :title Str,
   :labelAngle (ranged :min 0 :max 360),
   :titleFont Str,
   :tickSizeMinor (ranged :min 0),
   :titleColor Str,
   :tickLabelFontSize Num,
   :tickSize (ranged :min 0),
   :layer Str,
   :gridWidth Num,
   :values (cond-pre [Num] [DateTime]),
   :subdivide Num,
   :axisWidth Num,
   :ticks (ranged :min 0),
   :tickSizeMajor (ranged :min 0),
   :characterWidth Num,
   :labelAlign Str,
   :gridColor Str,
   :tickLabelFont Str,
   :axisColor Str,
   :titleFontWeight Str,
   :tickColor Str})

(defschema*
 Scale
 {:zero Bool,
  :useRawDomain Bool,
  :exponent Num,
  :bandSize (cond-pre BandSize Num),
  :type ScaleType,
  :round Bool,
  :padding Num,
  :nice (cond-pre NiceTime Bool),
  :domain (cond-pre [Str] [Num] [DateTime]),
  :clamp Bool,
  :range (cond-pre [Str] [Num] Str)})

(defschema*
 PositionChannelDef
 {:scale Scale,
  :value (cond-pre Str Num Bool),
  :field Str,
  :type Type,
  :title Str,
  :bin (cond-pre Bin Bool),
  :timeUnit TimeUnit,
  :aggregate AggregateOp,
  :axis Axis,
  :sort (cond-pre SortOrder SortField)})

(defschema* Facet {:row PositionChannelDef, :column PositionChannelDef})

(defschema*
 RangeFilter
 {:timeUnit TimeUnit,
  (required-key :field) Str,
  :range [(cond-pre DateTime Num)]})

(defschema*
 Transform
 {:filter
  (cond-pre
   EqualFilter
   RangeFilter
   OneOfFilter
   [(cond-pre EqualFilter RangeFilter OneOfFilter Str)]
   Str),
  :filterInvalid Bool,
  :calculate [Formula]})

(defschema*
 Legend
  {:properties Any,
   :symbolSize Num,
   :titleFontSize Num,
   :format Str,
   :labelBaseline Str,
   :gradientStrokeWidth Num,
   :offset Num,
   :shortTimeLabels Bool,
   :labelOffset Num,
   :gradientHeight Num,
   :orient Str,
   :gradientStrokeColor Str,
   :labelColor Str,
   :title Str,
   :titleFont Str,
   :symbolShape Str,
   :titleColor Str,
   :gradientWidth Num,
   :values (cond-pre [Str] [Num] [DateTime]),
   :padding Num,
   :labelFont Str,
   :labelAlign Str,
   :labelFontSize Num,
   :symbolColor Str,
   :symbolStrokeWidth (ranged :min 0),
   :titleFontWeight Str,
   :margin Num})

(defschema*
 ChannelDefWithLegend
 {:scale Scale,
  :value (cond-pre Str Num Bool),
  :legend Legend,
  :field Str,
  :type Type,
  :title Str,
  :bin (cond-pre Bin Bool),
  :timeUnit TimeUnit,
  :aggregate AggregateOp,
  :sort (cond-pre SortOrder SortField)})

(defschema* AreaOverlay (enum "line" "linepoint" "none"))

(defschema*
 OverlayConfig
 {:line Bool,
  :area AreaOverlay,
  :pointStyle MarkConfig,
  :lineStyle MarkConfig})

(defschema*
 OrderChannelDef
 {:sort SortOrder,
  :field Str,
  :type Type,
  :value (cond-pre Str Num Bool),
  :timeUnit TimeUnit,
  :bin (cond-pre Bin Bool),
  :aggregate AggregateOp,
  :title Str})

(defschema*
 UnitEncoding
 {:y PositionChannelDef,
  :path (cond-pre OrderChannelDef [OrderChannelDef]),
  :color ChannelDefWithLegend,
  :size ChannelDefWithLegend,
  :opacity ChannelDefWithLegend,
  :label FieldDef,
  :shape ChannelDefWithLegend,
  :order (cond-pre OrderChannelDef [OrderChannelDef]),
  :x PositionChannelDef,
  :y2 FieldDef,
  :x2 FieldDef,
  :text FieldDef,
  :detail (cond-pre FieldDef [FieldDef])})

(defschema*
 Encoding
 {:y PositionChannelDef,
  :path (cond-pre OrderChannelDef [OrderChannelDef]),
  :color ChannelDefWithLegend,
  :size ChannelDefWithLegend,
  :column PositionChannelDef,
  :opacity ChannelDefWithLegend,
  :label FieldDef,
  :shape ChannelDefWithLegend,
  :order (cond-pre OrderChannelDef [OrderChannelDef]),
  :x PositionChannelDef,
  :y2 FieldDef,
  :x2 FieldDef,
  :row PositionChannelDef,
  :text FieldDef,
  :detail (cond-pre FieldDef [FieldDef])})

(defschema* FacetGridConfig {:color Str, :opacity Num, :offset Num})

(defschema*
 FacetConfig
 {:scale FacetScaleConfig,
  :axis AxisConfig,
  :grid FacetGridConfig,
  :cell CellConfig})

(defschema*
 Config
 {:scale ScaleConfig,
  :mark MarkConfig,
  :countTitle Str,
  :facet FacetConfig,
  :background Str,
  :legend LegendConfig,
  :numberFormat Str,
  :overlay OverlayConfig,
  :axis AxisConfig,
  :viewport Num,
  :cell CellConfig,
  :timeFormat Str})

(defschema* DataFormatType (enum "json" "csv" "tsv" "topojson"))

(defschema*
 DataFormat
 {:type DataFormatType,
  :parse Any,
  :property Str,
  :feature Str,
  :mesh Str})

(defschema* Data {:format DataFormat, :url Str, :values [Any]})

(defschema*
 ExtendedUnitSpec
 {:description Str,
  :encoding Encoding,
  :transform Transform,
  :config Config,
  :name Str,
  :width Num,
  (required-key :mark) Mark,
  :height Num,
  :data Data})

(defschema*
 UnitSpec
 {:description Str,
  :encoding UnitEncoding,
  :transform Transform,
  :config Config,
  :name Str,
  :width Num,
  (required-key :mark) Mark,
  :height Num,
  :data Data})

(defschema*
 LayerSpec
 {:width Num,
  :height Num,
  :layers [UnitSpec],
  :name Str,
  :description Str,
  :data Data,
  :transform Transform,
  :config Config})

(defschema*
 FacetSpec
 {(required-key :facet) Facet,
  :spec (cond-pre UnitSpec LayerSpec),
  :name Str,
  :description Str,
  :data Data,
  :transform Transform,
  :config Config})

(defschema* Vega (cond-pre ExtendedUnitSpec FacetSpec LayerSpec))
