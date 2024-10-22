(ns data-windows-demo
  (:require [flow-storm.api :as fsa]
            [flow-storm.debugger.ui.data-windows.visualizers :as viz]
            [flow-storm.runtime.values :as fs-values]))

;; Let's start the FlowStorm UI first. Since we are using FlowStorm as a library, (without ClojureStorm),
;; we have to call local-connect.

(comment

  (fsa/local-connect {:theme :dark})

  )

;; Once the FlowStorm UI shows up, let's switch to the `Taps` tab on the left.

;;;;;;;;;;;;;;;;;;;;;;;
;; Basic visualizers ;;
;;;;;;;;;;;;;;;;;;;;;;;

(comment

  ;; Let's tap something simple like a string
  (tap> "Clojure rocks!")

  ;; You should be seeing the tapped value on the taps list, double clicking on it
  ;; should open a DataWindow for it.

  ;; There are a bunch of things here.

  ;; At the top, we have the `data-window-id`, should be something like `:value-1`. For
  ;; taps this id will be assigned automatically, and as you will see soon it can be
  ;; used to refer to the DataWindow programatically.

  ;; Right after in magenta are the DataWindow breadcrums, you can use them for navigating back when drilling
  ;; down on nested data.

  ;; On the next row we have a dropdown that let us choose between different visualizers for the current value,
  ;; then the type of the value, and last a button that allows us to define the current value with a name, so we can
  ;; use it at the repl.

  ;; And at the bottom we have the rendering of the selected visualization, which by default for strings is just a print.

  ;; Change it to :seqable and you should see a list of chars. You can click on any to navigate to them.

  ;; Use the breadcrums at the top to navigate back.

  ;; Go ahead and discard the DataWindow if you want, but you can have multiple DataWindows at the same time.

  ;; Let's now tap some nested data with infinite sequences
  (tap> {:a (filter odd? (range))})

  ;; By default map will be visualized  with the :map visualizer, which allows you to preview key and values
  ;; and also dig into them.

  ;; Let's dig into the lazy sequence by clicking on the val.

  ;; Lazy sequences will show by default with the :seqable viewer, which allows you to retrieve pages
  ;; lazily (by clicking more), and also to navigate further into any of those values.

  ;; Let's now tap a nested structure containing some meta.
  (tap> {:a ^:great {:name ^{:some-meta [1 2]}
                     {:other :hello
                      :bla "world"}}
         :b {:age 10}})

  ;; As soon as you dig into a value that contains meta, it will show it at the top.
  ;; Cliking on that meta value will navigate into it.

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Realtime visualizers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Now let's explore another feature of DataWindows, which allows us to see
;; values while they are changing.

(comment

  ;; First, we can push a value directly to a DataWindow (even when it doesn't exist)
  ;; by callling `fsa/data-window-push-val`, providing a DataWindow id, the value itself
  ;; and optionally a tag for the breadcrum.
  (fsa/data-window-push-val :changing-long 0 "a-long")

  ;; We can also send updates to the current value showing on a DataWindow like this :
  (fsa/data-window-val-update :changing-long 0.5)

  ;; But let's try something a little bit more fun, by creating a thread that sends updates
  ;; to our :changing-long DataWindow following a sine wave.

  (def scale 0.2) ;; define a scale we will soon redefine while the loop is running

  ;; create a thread that loops and sends the update
  (def th
    (Thread.
     (fn []
       (loop [x 0]
         (when-not (Thread/interrupted)
           (Thread/sleep 10)
           (fsa/data-window-val-update :changing-long (* scale (Math/sin x)))
           (recur (+ x 0.1)))))))

  ;; start the thread
  (.start th)

  ;; After you start the thread you will see the number on your :preview visualizer changing like
  ;; crazy. This isn't very useful. Try to select the :scope visualizer to see a plot of those values
  ;; in real time.
  ;; While everything is running try to redef the `scale` var with different values.

  ;; you can now interrupt that thread
  (.interrupt th)

  )

;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom visualizers ;;
;;;;;;;;;;;;;;;;;;;;;;;;

;; One important feature of DataWindows is they allow users to extend visualizers.

;; Inspired on this great article https://neuroning.com/boardgames-exercise/ let's say we are working on a
;; chess game and have model our chess board like this :

(def chess-board
  ^{:type 'chess-board} ;; let's give it a type, so (type chess-board) returns chess-board instead of a set
  #{{:kind :king  :player :white :pos [0 5]}
    {:kind :rook  :player :white :pos [5 1]}
    {:kind :pawn  :player :white :pos [5 3]}
    {:kind :king  :player :black :pos [7 2]}
    {:kind :pawn  :player :black :pos [6 6]}
    {:kind :queen :player :black :pos [3 1]}})

(comment

  ;; Let's tap it and open a DataWindow for it
  (tap> chess-board)

  ;; We have already some visualizers to explore this data, but it would be great
  ;; for debugging if we could have a visualizer that shows a proper chess board.
  ;; Let's try to define one.

  ;; Data visualization in FlowStorm is composed of two things:
  ;; - a data aspect extractor, which runs on the runtime process, and will build data for the visualization part
  ;; - a visualizer, which runs on the debugger process, and will render extracted data for a value

  ;; For this example everything is running under the same process, but this is not the case for ClojureScript
  ;; or remote Clojure.

  ;; We can register a new visualizer (the UI part) like this :

  #_(viz/register-visualizer
     {:id :my-viz
      :pred (fn [val] )
      :on-create (fn [val] {:fx/node :any-java-fx-node-that-renders-the-value
                            :more-ctx-data :anything})
      ;; OPTIONALLY
      :on-update (fn [val created-ctx-map {:keys [new-val]}] )
      :on-destroy (fn [created-ctx-map] )
      })

  ;; The important part there are :id, :pred, and :on-create.
  ;; The :id will be the one displayed on the visualizers dropdown, and re-registering a visualizer
  ;; with the same id will replace the previous one.
  ;; :pred is a predicate on the data extracted from values, it should return true if this visualizer
  ;; can handle the value.
  ;; And :on-create will be a function that receives this value and renders a java fx node.

  ;; Optionally you can provide :on-update and :on-destroy.
  ;; :on-update will receive values via `fsa/data-window-val-update`. It will also get a handle to
  ;; the original value (the one that created the DataWindow) and whatever map was returned by :on-create.
  ;; :on-destroy will be called everytime a visualizer gets removed, because you swapped your current visualizer
  ;; or because you went back with breadcrums. It can be useful in case you need to clear resources created by
  ;; :on-create.

  ;; The three of them should allow to create some pretty fancy stateful visualizers like the :scope one.

  ;; You can check what data you have available at the current value on a DataWindow with :
  (viz/data-window-current-val :YOUR-CHESS-BOARD-DW-ID)

  ;; As you can see this data isn't suitable for rendering a proper board so let's change that.
  )


(comment

  ;; For each value you want to visualize, all registered data aspect extractors will run,
  ;; and all extracted aspects will be merged.
  ;; So first let's register a new data-aspect-extractor, that will run for values of type chess-board
  ;; and just merges the board under :chess/board.

  (fs-values/register-data-aspect-extractor
   {:id :chess-board
    :pred (fn [val] (= 'chess-board (type val)))
    :extractor (fn [board] {:chess/board board})})

  ;; Now if we discard and re-open our board DataWindow and re-check it :
  (viz/data-window-current-val :YOUR-CHESS-BOARD-DW-ID)
  (viz/data-window-current-val :value-229)

  ;; we should see that we have this info available for the visualizers.
  ;; You may also notice that there is a :flow-storm.runtime.values/kinds key
  ;; which contains #{:chess-board :previewable :paged-shallow-seqable}.
  ;; This tells us all the data extractors that run over our values, which we
  ;; can use in a visualizer to know if we have interesting data to visualize.
  )


;; First we import a grid and a label component, which should be enough
(import '[javafx.scene.layout GridPane])
(import '[javafx.scene.control Label])

(comment

  ;; Now we can create our chess board visualizer.
  (viz/register-visualizer
   {:id :chess-board
    ;; only be available if the chess-board data extractor run on this value
    :pred (fn [val] (contains? (::fs-values/kinds val) :chess-board))

    ;; use the chess/board info to render a board with java fx
    :on-create (fn [{:keys [chess/board]}]
                 (let [kind->sprite {:king "♚" :queen "♛" :rook "♜" :bishop "♝" :knight "♞" :pawn "♟"}
                       pos->piece (->> board
                                       (mapv #(vector (:pos %) %))
                                       (into {}))]
                   {:fx/node (let [gp (GridPane.)]
                               (doall
                                (for [row (range 8) col (range 8)]
                                  (let [cell-color (if (zero? (mod (+ col (mod row 2)) 2)) "#f0d9b5" "#b58863")
                                        {:keys [kind player]} (pos->piece [row col])
                                        cell-str (kind->sprite kind "")
                                        player-color (when player (name player))]
                                    (.add gp (doto (Label. cell-str)
                                               (.setStyle (format "-fx-background-color:%s; -fx-font-size:40; -fx-text-fill:%s; -fx-alignment: center;"
                                                                  cell-color player-color))
                                               (.setPrefWidth 50))
                                          (int col)
                                          (int row)))))
                               gp)}))})


  ;; After registering it, if you re-open the DataWindow for the board you should see a new option on the
  ;; visualizers dropdown called :chess-board. Clicking it should show a proper board.

  ;; You can make it the default by calling `set-default-visualizer` which takes a type name and
  ;; a visualizer key.
  (viz/set-default-visualizer "chess-board" :chess-board)

  ;; For all FlowStorm provided visualizers take a look at `flow-storm.debugger.ui.data-windows.visualizers` namespace.
  )

;;;;;;;;;;;;;;;;;;;;
;; Datafy and nav ;;
;;;;;;;;;;;;;;;;;;;;

;; Now let's see DataWindows datafy navigation capabilities.
;; For this we will create a small datascript db, since it's entities
;; alredy implement datafy protocols.

;; Bring the dependency in
(clojure.repl.deps/add-lib 'datascript/datascript {:mvn/version "1.7.3"})

;; Require the basic namespaces
(require '[datascript.core :as d])
(require 'datascript.datafy)


(comment

  ;; Define a very simple schema
  (def schema {:person/likes {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}})

  ;; Create a db with some data
  (def db (-> (d/empty-db schema)
              (d/db-with [{:db/id 1 :person/name "John" :person/likes [2 3]}
                          {:db/id 2 :person/name "Peter" :person/likes [3]}
                          {:db/id 3 :person/name "Alicia" :person/likes [1]}])))

  ;; Tap entity with id 1
  (tap> (d/entity db 1))

  ;; On the map or indexed views you should see arrows when there is navigation available under those
  ;; keys. Clicking on the will navigate on the entities objects calling nav.
  ;; You can use them to navigate the graph.

  ;; Set the default visualizer for entities
  (viz/set-default-visualizer "datascript.impl.entity.Entity" :map)

  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Numbers and bytes arrays visualizations ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment

  ;; All int? numbers can be visualized in deciml, hex, and binary.
  (tap> 42)

  (import '[java.io FileInputStream])

  ;; Let's tap any binary file byte array
  (tap> (.readAllBytes (FileInputStream. "./resources/ant.png")))

  ;; Now open its data window and try the :bin-byte-array and :hex-byte-array visualizations

  )
