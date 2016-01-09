(ns metabase.db.metadata-queries
  "Predefined QP queries for getting metadata about an external database."
  (:require [metabase.driver :as driver]
            [metabase.driver.query-processor.expand :as ql]
            [metabase.util :as u]))

(defn- qp-query [db-id query]
  (-> (driver/process-query
       {:type     :query
        :database db-id
        :query    query})
      :data
      :rows))

(defn- field-query [field query]
  (qp-query ((u/deref-> field :table :db) :id)
            (ql/query (merge query)
                      (ql/source-table ((u/deref-> field :table) :id)))))

(defn table-row-count
  "Fetch the row count of TABLE via the query processor."
  [table]
  {:pre  [(map? table)]
   :post [(integer? %)]}
  (-> (qp-query (:db_id table) (ql/query (ql/source-table (:id table))
                                         (ql/aggregation (ql/count))))
      first first int))

(defn field-distinct-values
  "Return the distinct values of FIELD.
   This is used to create a `FieldValues` object for `:category` Fields."
  [{field-id :id :as field}]
  (mapv first (field-query field (-> {}
                                     (ql/breakout (ql/field-id field-id))
                                     (ql/limit @(resolve 'metabase.driver.sync/low-cardinality-threshold))))))

(defn field-distinct-count
  "Return the distinct count of FIELD."
  [{field-id :id :as field} & [limit]]
  (-> (field-query field (-> {}
                             (ql/aggregation (ql/distinct (ql/field-id field-id)))
                             (ql/limit limit)))
      first first int))

(defn field-count
  "Return the count of FIELD."
  [{field-id :id :as field}]
  (-> (field-query field (ql/aggregation {} (ql/count (ql/field-id field-id))))
      first first int))
