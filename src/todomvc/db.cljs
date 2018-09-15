(ns todomvc.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]
            [protocol55.step-cljs.alpha :refer [stepdef]]))


;; -- Spec --------------------------------------------------------------------
;;
;; This is a clojure.spec specification for the value in app-db. It is like a
;; Schema. See: http://clojure.org/guides/spec
;;
;; The value in app-db should always match this spec. Only event handlers
;; can change the value in app-db so, after each event handler
;; has run, we re-check app-db for correctness (compliance with the Schema).
;;
;; How is this done? Look in events.cljs and you'll notice that all handlers
;; have an "after" interceptor which does the spec re-check.
;;
;; None of this is strictly necessary. It could be omitted. But we find it
;; good practice.

(s/def ::id int?)
(s/def ::title string?)
(s/def ::done boolean?)
(s/def ::todo (s/keys :req-un [::id ::title ::done]))
(s/def ::todos (s/and                                       ;; should use the :kind kw to s/map-of (not supported yet)
                 (s/map-of ::id ::todo)                     ;; in this map, each todo is keyed by its :id
                 #(instance? PersistentTreeMap %)           ;; is a sorted-map (not just a map)
                 ))
(s/def ::showing                                            ;; what todos are shown to the user?
  #{:all                                                    ;; all todos are shown
    :active                                                 ;; only todos whose :done is false
    :done                                                   ;; only todos whose :done is true
    })
(s/def ::db (s/keys :req-un [::todos ::showing]))

;; events
(s/def ::initialise-db (s/tuple #{:initialise-db}))
(s/def ::set-showing (s/tuple #{:set-showing} ::showing))
(s/def ::add-todo (s/tuple #{:add-todo} ::title))
(s/def ::toggle-done (s/tuple #{:toggle-done} ::id))
(s/def ::save (s/tuple #{:save} ::id ::title))
(s/def ::delete-todo (s/tuple #{:delete-todo} ::id))
(s/def ::clear-completed (s/tuple #{:clear-completed}))
(s/def ::complete-all-toggle (s/tuple #{:complete-all-toggle}))

;; db states
(s/def :todomvc.db.uninitialised/db empty?)

(s/def :todomvc.db.empty/todos empty?)
(s/def :todomvc.db.empty/db
  (s/keys :req-un [:todomvc.db.empty/todos ::showing]))

(s/def :todomvc.db.incomplete/todos
  (s/and ::todos #(seq %)
         (fn [todos]
           (let [{done true active false} (group-by :done (vals todos))]
             (and (seq done) (seq active))))))
(s/def :todomvc.db.incomplete/db
  (s/keys :req-un [:todomvc.db.incomplete/todos ::showing]))

(s/def :todomvc.db.completed/todos
  (s/and ::todos #(seq %) #(every? :done (vals %))))
(s/def :todomvc.db.completed/db
  (s/keys :req-un [:todomvc.db.completed/todos ::showing]))

(s/def :todomvc.db.unbegan/todos
  (s/and ::todos #(seq %) #(every? (complement :done) (vals %))))
(s/def :todomvc.db.unbegan/db
  (s/keys :req-un [:todomvc.db.unbegan/todos ::showing]))

;; step spec
(stepdef ::step
  (:todomvc.db.empty/db
    ::add-todo (:todomvc.db.unbegan/db))

  (:todomvc.db.incomplete/db
    ::add-todo (:todomvc.db.incomplete/db)
    ::toggle-done (:todomvc.db.incomplete/db
                   :todomvc.db.completed/db
                   :todomvc.db.unbegan/db)
    ::complete-all-toggle (:todomvc.db.completed/db)
    ::clear-completed (:todomvc.db.unbegan/db))

  (:todomvc.db.unbegan/db
    ::add-todo (:todomvc.db.unbegan/db)
    ::toggle-done (:todomvc.db.incomplete/db :todomvc.db.completed/db)
    ::complete-all-toggle (:todomvc.db.completed/db))

  (:todomvc.db.completed/db
    ::add-todo (:todomvc.db.incomplete/db)
    ::toggle-done (:todomvc.db.incomplete/db)
    ::clear-completed (:todomvc.db.empty/db)
    ::complete-all-toggle (:todomvc.db.unbegan/db))

  (:todomvc.db.uninitialised/db
    ::initialise-db (:todomvc.db.empty/db
                     :todomvc.db.incomplete/db
                     :todomvc.db.completed/db
                     :todomvc.db.unbegan/db))

  ;; We can use a generic fall-back for events that should happen in every state
  (::db
    ::set-showing (::db)))


;; -- Default app-db Value  ---------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db
;; Unless, of course, there are todos in the LocalStore (see further below)
;; Look in:
;;   1.  `core.cljs` for  "(dispatch-sync [:initialise-db])"
;;   2.  `events.cljs` for the registration of :initialise-db handler
;;

(def default-db           ;; what gets put into app-db by default.
  {:todos   (sorted-map)  ;; an empty list of todos. Use the (int) :id as the key
   :showing :all})        ;; show all todos


;; -- Local Storage  ----------------------------------------------------------
;;
;; Part of the todomvc challenge is to store todos in LocalStorage, and
;; on app startup, reload the todos from when the program was last run.
;; But the challenge stipulates to NOT load the setting for the "showing"
;; filter. Just the todos.
;;

(def ls-key "todos-reframe")                         ;; localstore key

(defn todos->local-store
  "Puts todos into localStorage"
  [todos]
  (.setItem js/localStorage ls-key (str todos)))     ;; sorted-map written as an EDN map


;; -- cofx Registrations  -----------------------------------------------------

;; Use `reg-cofx` to register a "coeffect handler" which will inject the todos
;; stored in localstore.
;;
;; To see it used, look in `events.cljs` at the event handler for `:initialise-db`.
;; That event handler has the interceptor `(inject-cofx :local-store-todos)`
;; The function registered below will be used to fulfill that request.
;;
;; We must supply a `sorted-map` but in LocalStore it is stored as a `map`.
;;
(re-frame/reg-cofx
  :local-store-todos
  (fn [cofx _]
      ;; put the localstore todos into the coeffect under :local-store-todos
      (assoc cofx :local-store-todos
             ;; read in todos from localstore, and process into a sorted map
             (into (sorted-map)
                   (some->> (.getItem js/localStorage ls-key)
                            (cljs.reader/read-string)    ;; EDN map -> map
                            )))))
