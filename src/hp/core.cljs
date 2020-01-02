(ns ^:figwheel-hooks hp.core
  (:require [uix.core.alpha :as uix.core]
            [uix.dom.alpha :as uix.dom]
            [cljs.tools.reader :as reader]
            [hp.specs :as hp.specs]))

(set! *warn-on-infer* true)

(def cities
  (->> [[36,"Киев"],[55,"Винница"],[58,"Казатин"],[59,"Калиновка"],[74,"Луцк"],[83,"Днепропетровск"],[85,"Кривой Рог"],[90,"Павлоград"],[106,"Донецк"],[131,"Житомир"],[146,"Ужгород"],[154,"Запорожье"],[168,"Ивано-Франковск"],[178,"Белая Церковь"],[181,"Борисполь"],[183,"Бровары"],[184,"Буча"],[185,"Васильков"],[186,"Вишнёвое"],[187,"Вышгород"],[188,"Ирпень"],[191,"Обухов"],[192,"Переяслав-Хмельницкий"],[196,"Славутич"],[209,"Кропивницкий"],[265,"Львов"],[286,"Николаев"],[303,"Одесса"],[312,"Кременчуг"],[317,"Миргород"],[319,"Полтава"],[331,"Ровно"],[343,"Сумы"],[359,"Тернополь"],[368,"Волчанск"],[378,"Харьков"],[379,"Чугуев"],[384,"Новая Каховка"],[394,"Каменец-Подольский"],[400,"Хмельницкий"],[415,"Черкассы"],[432,"Чернигов"],[444,"Черновцы"],[452,"Макаров"]]
       (into (sorted-map))))


(defonce db (atom {:dogs []}))

(defn fetch-cache []
  (-> (js/fetch "/cache.edn")
      (.then #(.text ^js %))
      (.then #(reader/read-string %))))

(defn app-top-bar [{:keys [city on-change-city]}]
  [:div {:style {:height 64
                 :max-height 64
                 :min-height 64
                 :width "100%"
                 :background "#fff"
                 :box-shadow "0 2px 8px rgba(0, 0, 0, 0.07)"
                 :display :flex
                 :align-items :center
                 :justify-content :center}}
   [:a {:href "https://happypaw.ua/ua"}
    [:img {:src "logo.svg"
           :style {:height 40}}]]
   #_
   [:select {:value city
             :on-change #(on-change-city (.. % -target -value))
             :style {:font-size 16}}
    (for [[code name] cities]
      ^{:key code}
      [:option {:value code} name])]])

(defn app-bottom-bar []
      [:div {:style {:height 64
                     :max-height 64
                     :min-height 64
                     :width "100%"
                     :display :flex
                     :justify-content :center}}
       [:button {:style {:border :none
                         :padding 0
                         :display :block
                         :box-shadow "0 1px 4px rgba(0,0,0,0.2)"
                         :border-radius "50%"
                         :width 44
                         :height 44
                         :background-color "#f93824"
                         :background-position "10px 11px"
                         :background-repeat :no-repeat
                         :background-image "url(like.svg)"}}]])

(defn pet-view [{:keys [pet on-prev on-next iidx change-iidx]}]
  (let [{:pet/keys [name gender age image images]} pet
        imgs (uix.core/state {})
        ref (uix.core/ref)
        img-loaded? (contains? @imgs (nth images iidx))]
    (uix.core/effect!
      (fn []
        (-> (js/Hammer. @ref)
            (.on "swipe" (fn [^js e]
                           (cond
                             (and (< (.-deltaX e) -100) (< (.-deltaY e) 50))
                             (on-next)

                             (and (> (.-deltaX e) 100) (< (.-deltaY e) 50))
                             (on-prev))))))
      [])
    (uix.core/effect!
      (fn []
          (let [rw (.-clientWidth ^js @ref)
                rh (.-clientHeight ^js @ref)]
            (doseq [url images]
              (let [img (js/Image.)]
                (set! (.-onload img)
                      #(let [w (.-naturalWidth img)
                             h (.-naturalHeight img)
                             ratio (Math/floor (/ w rw))
                             sw (/ w ratio)
                             sh (/ h ratio)]
                         (swap! imgs assoc url [sw sh])))
                (set! (.-src img) url)))))
      [images])
    [:div {:ref ref
           :style {:flex 1
                   :display :flex
                   :flex-direction :column
                   :border-radius 7
                   :overflow :hidden
                   :box-shadow "0 0 8px rgba(0, 0, 0, 0.07)"
                   :background-image (when img-loaded?
                                       (str "url(" (nth images iidx) ")"))
                   :background-size :cover
                   :color "#fff"
                   :text-shadow "0 2px 4px rgba(0,0,0,0.3)"}}
     [:div {:style {:padding "4px 4px 0 4px"
                    :display :flex}}
      (when (> (count images) 1)
        (for [idx (range (count images))]
          ^{:key (nth images idx)}
          [:div {:style {:margin "0 1px"
                         :flex 1
                         :background (if (== iidx idx)
                                       "#fff"
                                       "rgba(255,255,255,0.6)")
                         :height 4
                         :border-radius 2}}]))]
     [:div {:style {:flex 1
                    :display :flex}}
      [:div {:style {:flex 1}
             :on-click (when (pos? iidx)
                         #(change-iidx dec))}]
      [:div {:style {:flex 1}
             :on-click (when (< iidx (dec (count images)))
                         #(change-iidx inc))}]]
     [:div {:style {:display :flex
                    :align-items :center
                    :justify-self :flex-end
                    :padding 8
                    :background "rgba(0,0,0,0.3)"}}
      [:div {:style {:font-size "24px"
                     :font-weight 700}}
       name]
      [:div {:style {:font-size "16px"
                     :margin-left 24
                     :font-weight 600}}
       (str gender ", " age)]]]))

(defn app-main [{:keys [pets]}]
  (let [idx (uix.core/state 0)
        iidx (uix.core/state 0)
        change-idx #(do (reset! iidx 0)
                        (reset! idx %))]
    [:div {:style {:flex 1
                   :display :flex
                   :flex-direction :column
                   :padding 16}}
     [pet-view {:pet (nth pets @idx)
                :iidx @iidx
                :change-iidx #(swap! iidx %)
                :on-prev #(when (pos? @idx)
                                (change-idx (dec @idx)))
                :on-next #(when (< @idx (count pets))
                                (change-idx (inc @idx)))}]]))


(defn app [st]
  (let [city (uix.core/state 36)]
    [:div {:style {:display :flex
                   :flex-direction :column
                   :background "#fff"
                   :height "100%"}}
     [app-top-bar {:city @city
                   :on-change-city #(reset! city %)}]
     [app-main {:pets (:dogs st)}]
     [app-bottom-bar]]))

(defn ^:after-load render []
  (uix.dom/render [app @db] js/root))

(defn ^:export init []
      (-> (fetch-cache)
          (.then #(swap! db assoc :dogs (vec (vals %)))))
      (add-watch db ::watch #(render)))
