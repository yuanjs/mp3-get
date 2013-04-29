;;This is GUI for mp3 downloader
(ns app.mp3gui
  (:require [clojure.set :as cljset])
  (:use seesaw.core)
  (:use seesaw.font)
  (:require [seesaw.bind :as b])
  (:require [seesaw.table :as table])
  (:require [app.core :refer [search-songs-by]])
  (:require [clj-http.client :as client]))

(import 'java.io.FileOutputStream)
(import 'java.io.BufferedOutputStream)

(def buffer-size 8192)

;;(def search-results (search-songs-by "王菲"))
;;(System/setProperty "file.encoding" "UTF-8")

(def f (frame :title "Mp3 downloader" :on-close :nothing))
(native!)

(defn display [content]
  (config! f :content content)
  content)

(def lb (label "Search: "))
(def search-text (text))

(def is-downloading (ref false))

(defn render-mp3-item
  [renderer {:keys [value]}]
  (config! renderer :text (format "%s" (value :SongName))))

;;(def mp3-list (listbox :renderer render-mp3-item))
(def mp3-table (table :model [
                              :columns [{:key :SongName, :text "歌名"} 
                                        {:key :SingerName, :text "演唱者"} 
                                        {:key :AlbumName, :text "专辑"}
                                        {:key :LossLessType, :text "无损格式"}]] 
                              ;;:rows search-results ]
                      :show-grid? true
                      :show-horizontal-lines? true
                      :show-vertical-lines? true))

;;(config! mp3-list :model search-results)
(def model-play-list (atom []))
(def play-list (listbox :renderer render-mp3-item))
(b/bind model-play-list (b/property play-list :model))


(def add-button (button :text "Add to Play"))
(def remove-button (button :text "Remove from Play"))
(def status-label (label :text "Ready"))

(def main-split (left-right-split (scrollable mp3-table) (scrollable play-list) :divider-location 2/3))

(display (border-panel
          :north (add! (horizontal-panel) lb search-text)
          :center main-split
          :south (add! (horizontal-panel) add-button remove-button status-label)
          :vgap 5 :hgap 5 :border 2))

(defn remove-from-list [items]
  "Remove selected items from play list"
  ;;(alert (class items))
  ;;(alert (class (first items)))
  ;;(alert (class (first @model-play-list)))
  ;;(alert (cljset/difference (set @model-play-list) items))
  (reset! model-play-list (cljset/difference (set @model-play-list) items)))

(defn add-to-list [items]
  "Add items to model-play-list
   Don't add duplicated item"
  ;;(alert (table/value-at mp3-table 0))
  (swap! model-play-list concat (table/value-at mp3-table items)))

(listen add-button :action
        (fn [e]
          (when-let [select-rows (selection mp3-table {:multi? true})]
            (add-to-list select-rows)
            ;;(alert is-downloading)
            (if (false? @is-downloading)
              (pcalls download-files-thread)))))

(listen remove-button :action
        (fn [e]
          (when-let [select-rows (selection play-list {:multi? true})]
            (remove-from-list select-rows))))

(listen search-text :action
        (fn [e]
          (let [search-str (text e)]
          (if (> (count search-str) 0) (config! mp3-table :model [
                              :columns [{:key :SongName, :text "歌名"} 
                                        {:key :SingerName, :text "演唱者"} 
                                        {:key :AlbumName, :text "专辑"}
                                        {:key :LossLessType, :text "无损格式"}] 
                              :rows (search-songs-by (text e))])))))
(listen f :window-closing
        (fn [e]
          (if (true? @is-downloading)
            (alert "Still downloading file")
            (dispose! f))))

(defn buf-download-bin-file [url to-file]
  (try
    (with-open [r (clojure.java.io/input-stream url)]
      (with-open [w (BufferedOutputStream. (FileOutputStream. to-file))]
        (let [buf (byte-array buffer-size)]
          (loop [bytes-read (.read r buf)]
            (if (>= bytes-read 0)
              (do
                (.write w buf 0 bytes-read)
                (recur (.read r buf))))))))
  (catch Exception e)))

(defn download-bin-file [url to-file]
  (try 
    (with-open [w (clojure.java.io/output-stream to-file)]
      (.write w (:body (client/get url {:as :byte-array}))))
  (catch Exception e)))

(defn download-song [item]
  (let [download-url (get-download-url item) 
        save-file-name (get-download-file-name item)]
    (config! status-label :text (str "正在下载 " (item :SongName)))
    (buf-download-bin-file download-url save-file-name)
    (config! status-label :text "Ready")))

(defn get-download-url [download-item]
  (if (nil? (download-item :FlacLink))
    (download-item :Mp3Link)
    (download-item :FlacLink)))

(defn get-download-file-name [download-item]
  (str "/Users/yuanjs/Music/ttpod/" 
       (download-item :SingerName) "-" 
       (download-item :SongName) "-"
       (download-item :SongId)
       (if (nil? (download-item :FlacLink))
         ".mp3"
         (str "." (download-item :LossLessType)))))

(defn download-files-thread []
  (loop [items @model-play-list]
    (if (= (count items) 0)
      (dosync (ref-set is-downloading false))
      (let [item (first items)]
        (dosync (ref-set is-downloading true))
        (swap! model-play-list rest)
        (download-song item)
        ;;(alert @model-play-list)
        (recur @model-play-list))
      )
    )
)

(defn -main[]
  ;;(alert (System/getProperty "file.encoding"))
  (-> f pack! show!))
;;(-main)
