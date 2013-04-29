(ns app.core
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json]))

(def pagenum 1)
(def max-songs-perpage 50)
(def attach-string (str "&page=" pagenum "&size=" max-songs-perpage "&app=ttpod&v=v4.1.0.2013042220&mid=iPhone4S&f=f570&s=s310&imsi=&hid=&splus=6.1.2&active=0&net=2"))

(defn mp3-search [name]
  (let [search-string (str "http://so.ard.iyyin.com/songs/search?q=" name attach-string)]
    (client/get search-string {:accept :json})))

(defn result-to-json [result-str]
  (json/read-str result-str))

(defn song-url-details [url-list]
  (for [url-detail url-list]
    (format "Duration: %s Bitrate: %s Format: %s Size: %s Url: %s" 
            (url-detail "duration")            
            (url-detail "bitrate") 
            (url-detail "format")
            (url-detail "size")
            (url-detail "url"))))

(defn last-mp3-link [url-list]
  (last url-list))

(defn get-flac-link [song-data]
  (if (nil? (song-data "ll_list"))
    nil
    ((first (song-data "ll_list")) "url")))

(defn get-lossless-type [song-data]
  (if (nil? (song-data "ll_list"))
    nil
    ((first (song-data "ll_list")) "format")))

(defn song-data-process [song-data]
   (hash-map
    :SongName (song-data "song_name")
    :SingerName (song-data "singer_name")
    :AlbumName (song-data "album_name")
    :SingerId (song-data "singer_id")
    :SongId (song-data "song_id")
    :Mp3Link ((last-mp3-link (song-data "url_list")) "url")
    :FlacLink (get-flac-link song-data)
    :LossLessType (get-lossless-type song-data)
    ))

(defn songs-data-process [song-list]
   (for [song-data song-list]
     (song-data-process song-data)))

(defn search-songs-by [name]
  (songs-data-process
    ((result-to-json
      ((mp3-search name) :body)) "data")))

;;(def results (search-songs-by "张国荣"))

;;(println results)
;;(def search-result (app.core/mp3-search "张国荣"))


;;(def json-result (app.core/result-to-json (search-result :body)))

;;(json-result "code")
;;(json-result "msg")
;;(json-result "count")
;;(json-result "allPage")
;;(def song-list (json-result "data"))
;;(class song-list)
;;(def song-data (first song-list))

;;(count (app.core/search-songs-by "王菲"))
;;(songs-data-process song-list)
;;(app.core/song-data-process song-data)

;;((first (song-data "url_list")) "duration")

;;(song-url-details (song-data "url_list"))

