(ns hp.core
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [hp.specs :as hp.specs]))

(s/check-asserts true)

(s/def :req/filter
  (s/keys :req-un [:filter/Type :filter/City]
          :opt-un [:filter/Name
                   :filter/Age :filter/Size :filter/Gender
                   :filter/Sterile :filter/Uniq :filter/AnimalId
                   :filter/Host :filter/Labels]))

(s/def :req/per-page pos-int?)
(s/def :req/page pos-int?)

(s/def :req-url/opts
 (s/keys :req-un [:req/filter :req/per-page :req/page]))

(def BASE_URL "https://happypaw.ua")
(def USER_AGENT "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36")

(defn make-dog-url [{:keys [page per-page] :as opts}]
  {:pre [(s/assert :req-url/opts opts)]}
  (let [filters (->> (:filter opts)
                     (filter (comp some? val))
                     (map #(str "filter[" (name (key %)) "]=" (val %)))
                     (str/join "&")
                     (str "?"))]
    (str BASE_URL "/ru/shelter/dog"
         filters
         "&per_page=" per-page
         "&page=" page)))

(defn find-all-dogs []
  (-> (make-dog-url {:page 1
                     :per-page 500
                     :filter {:Type 2
                              :City 36}})
      (http/get {:headers {:User-Agent USER_AGENT} :as :stream})
      :body))

(defn fetch-details [id]
  (->> (http/get (str BASE_URL "/ru/shelter/card/" id) {:headers {:User-Agent USER_AGENT} :as :stream})
       :body))

(defn string->number [s]
  (->> s
       str/trim
       (re-find #"[0-9]+")
       read-string))

(defn parse-results [h]
  (let [names (->> (html/select h [:.list-results :.list-item-title])
                   (map (comp str/trim first :content)))
        images (->> (html/select h [:.list-results :.list-item-image-src])
                    (map (comp #(str BASE_URL %) :src :attrs)))
        descriptions (->> (html/select h [:.list-results :.list-item-text])
                          (map (comp str/trim first :content)))
        ids (->> (html/select h [:.list-results :.list-item-id])
                 (map (comp string->number first :content)))
        fields [[:pet/name names]
                [:pet/image images]
                [:pet/description descriptions]
                [:pet/id ids]]]
    (->> (apply interleave (map second fields))
         (partition (count fields))
         (map #(zipmap (map first fields) %)))))

(defn parse-details [h]
  (let [images (->> (html/select h [:.animal :.list-item-image-src])
                    (map (comp #(str BASE_URL %) :data-full :attrs)))
        details (->> (html/select h [:.animal-traits :.animal-trait])
                     (map (comp str/trim last :content))
                     (filter (comp not str/blank?))
                     (zipmap [:pet/age :pet/gender :pet/height :pet/steril
                              :owner/name :owner/city :owner/phone-number :owner/email]))]
    (assoc details :pet/images images)))

(def cache (atom {:dogs/index nil}))

(defn find-all-dogs-with-cache []
  (if-some [dogs (:dogs/index @cache)]
    dogs
    (->> (find-all-dogs)
         html/html-resource
         parse-results
         (reduce #(assoc! %1 (:pet/id %2) %2) (transient {}))
         persistent!
         (swap! cache assoc :dogs/index)
         :dogs/index)))

(defn fetch-parsed-details [id]
  (->> (fetch-details id)
       html/html-resource
       parse-details))

(defn update-db! []
  (let [dogs (find-all-dogs-with-cache)]
    (->> (keys dogs)
         (reduce #(update %1 %2 merge (fetch-parsed-details %2)) dogs)
         (swap! cache assoc :dogs/index))))

(defn persist-cache []
  (when-some [dogs (:dogs/index @cache)]
    (spit "cache.edn" (str dogs))))

(defn hydrate-cache []
  (->> (slurp "cache.edn")
       read-string
       (swap! cache assoc :dogs/index)))

(comment
  (hydrate-cache)
  (update-db!)
  (persist-cache))

