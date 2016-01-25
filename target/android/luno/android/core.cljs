(ns ^:figwheel-load luno.android.core
  (:require-macros [env.require-img :refer [require-img]])
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [luno.handlers]
            [luno.subs]
            [luno.ui :as ui]
            [luno.android.styles :as s]
            [luno.android.routes :refer [routes]]
            [luno.android.components.drawer :refer [drawer-component]]
            [luno.shared.scenes.main :refer [main-scene]]
            [luno.shared.scenes.about :refer [about-scene]]))

(defn show-add-dialog []
  (ui/show-dialog-android {:title        "Add city"
                           :input        {:hint     "Please, input city's name"
                                          :callback (fn [text]
                                                      (dispatch [:load-weather text]))}
                           :positiveText "Add"}))

(defn wrapped-main-scene [{navigator :navigator}]
  (let [drawer (subscribe [:get-android-drawer])]
    [ui/drawer-layout {:drawer-width           300
                       :drawer-position        js/React.DrawerLayoutAndroid.positions.Left
                       :render-navigation-view #(r/as-element [drawer-component navigator])
                       :ref                    (fn [drawer]
                                                 (dispatch [:set-android-drawer drawer]))}
     [ui/view {:flex 1}
      [ui/view {:style (get-in s/styles [:statusbar])}]
      [ui/toolbar {:title         "Luno"
                   :icon          "menu"
                   :actions       [{:icon    "add-circle"
                                    :onPress (fn [_]
                                               (show-add-dialog))}]
                   :style         (get-in s/styles [:toolbar])
                   :on-icon-press (fn [_]
                                    (.openDrawer @drawer))}]
      [main-scene {:platform        :android
                   :navigator       navigator
                   :style           (get-in s/styles [:scenes :main])
                   :city-wrapper-fn (fn [city component]
                                      (let [media-url (-> (get-in city [:bing-image :MediaUrl]) (str))]
                                        [ui/card {:style (get-in s/styles [:scenes :main :city-card :card])}
                                         [ui/card-media {:image (r/as-element [ui/image {:source {:uri media-url}}])}
                                          component]]))}]]]))

(defn wrapped-about-scene [{navigator :navigator}]
  [ui/view
   [ui/view {:style (get-in s/styles [:statusbar])}]
   [ui/toolbar {:title         "About Luno"
                :icon          "arrow-back"
                :style         (get-in s/styles [:toolbar])
                :on-icon-press (fn [_]
                                 (.pop navigator))}]
   [about-scene {:platform  :android
                 :navigator navigator
                 :style     (get-in s/styles [:scenes :about])}]])

(defn root []
  [ui/navigator {:initial-route   (routes :main)
                 :style           (get-in s/styles [:app])
                 :configure-scene (fn [_ _]
                                    js/React.Navigator.SceneConfigs.FloatFromBottomAndroid)
                 :render-scene    (fn [route navigator]
                                    (let [route  (js->clj route :keywordize-keys true)]
                                      (r/as-element
                                        (condp = (:name route)
                                          "main" [wrapped-main-scene {:navigator navigator}]
                                          "about" [wrapped-about-scene {:navigator navigator}]))))}])

(defn mount-root []
  (r/render [root] 1))

(defn ^:export init []
  (dispatch-sync [:initialize-db])
  (fn []
    (.registerRunnable ui/app-registry "Luno" #(mount-root))))
