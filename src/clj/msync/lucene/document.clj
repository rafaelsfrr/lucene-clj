(ns msync.lucene.document
  (:import [org.apache.lucene.index IndexOptions IndexableFieldType]
           [org.apache.lucene.search.suggest.document SuggestField ContextSuggestField]
           [org.apache.lucene.document FieldType Field Document]))

(def ^:private index-options
  {:full           IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
   true            IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS

   :none           IndexOptions/NONE
   :nil            IndexOptions/NONE
   false           IndexOptions/NONE

   :docs-freqs     IndexOptions/DOCS_AND_FREQS
   :docs-freqs-pos IndexOptions/DOCS_AND_FREQS_AND_POSITIONS})

(def suggest-field-prefix "$suggest-")

(defn- ^IndexableFieldType >field-type
  "FieldType information for the given field."
  [{:keys [index-type store? tokenize?]
    :or   {tokenize? true}}]
  (let [index-option (index-options index-type IndexOptions/NONE)]
    (doto (FieldType.)
      (.setIndexOptions index-option)
      (.setStored store?)
      (.setTokenized tokenize?))))

(defn- ^Field >field
  "Document Field"
  [key value opts]
  {:pre [(not (.startsWith (name key) suggest-field-prefix))]}
  (let [field-type (>field-type opts)
        value      (if (keyword? value) (name value) (str value))]
    (Field. ^String (name key) ^String value field-type)))

(defn- ^SuggestField >suggest-field
  "Document SuggestField"
  [key contexts value weight]
  (let [key                        (str suggest-field-prefix (name key))
        contexts                   (if (empty? contexts) nil contexts)
        ^ContextSuggestField field (ContextSuggestField. key value weight contexts)]
    field))

(defmulti ^:private add-fields! (fn [document field-meta field-value field-creator] (sequential? field-value)))
(defmethod add-fields! false
  [document field-meta field-value field-creator]
  (.add document (field-creator field-meta field-value)))
(defmethod add-fields! true
  [document field-meta field-values field-creator]
  (doseq [field-value field-values]
    (.add document (field-creator field-meta field-value))))

(defn map->document [m {:keys [string-fields stored-fields indexed-fields suggest-fields context-fn]}]
  "Convert a map to a Lucene document.
  Lossy on the way back. Also, string field names come back as keywords."
  (let [field-keys            (keys m)
        string-fields         (or string-fields #{})
        stored-fields         (or stored-fields (into #{} field-keys))
        indexed-fields        (or indexed-fields (zipmap (keys m) (repeat :full)))
        suggest-fields        (or suggest-fields {})
        field-creator         (fn [k v]
                                (>field k v
                                        {:index-type (get indexed-fields k false)
                                         :store?     (contains? stored-fields k)
                                         :tokenize?  (not (contains? string-fields k))}))
        context-fn            (or context-fn (constantly nil))
        contexts              (context-fn m)
        suggest-field-creator (fn [[field-name weight] v]
                                (let [value v]
                                  (>suggest-field field-name contexts value weight)))
        doc                   (Document.)]
    (doseq [field-key field-keys]
      (add-fields! doc field-key (get m field-key) field-creator))
    (doseq [[field-key weight] suggest-fields]
      (add-fields! doc [field-key weight] (get m field-key) suggest-field-creator))
    doc))

(defn document->map [^Document document]
  "Convenience function.
  Lucene document to map. Keys are always keywords. Values come back as string.
  Only stored fields come back."
  (reduce
    (fn [m field]
      (let [k          (-> field .name keyword)
            v          (-> field .stringValue)
            existing-v (get m k)]
        (if existing-v
          (assoc m k (if (string? existing-v) [existing-v v] (conj existing-v v)))
          (assoc m k v))))
    {}
    document))
