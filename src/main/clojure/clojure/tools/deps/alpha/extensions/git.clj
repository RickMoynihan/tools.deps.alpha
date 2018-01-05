;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.deps.alpha.extensions.git
  (:require
    [clojure.tools.deps.alpha.extensions :as ext]
    [clojure.tools.gitlibs :as gitlibs]))

(defmethod ext/canonicalize :git
  [lib {:keys [git/url rev] :as coord} config]
  (let [lib (if (nil? (namespace lib))
              (symbol (name lib) (name lib))
              lib)]
    [lib (assoc coord :rev (gitlibs/resolve url rev))]))

(defmethod ext/dep-id :git
  [lib coord config]
  (select-keys coord [:git/url :rev]))

(defmethod ext/manifest-type :git
  [lib {:keys [git/url rev deps/manifest] :as coord} config]
  (let [rev-dir (gitlibs/procure url lib rev)]
    (if manifest
      {:deps/manifest manifest, :deps/root rev-dir}
      (ext/detect-manifest rev-dir))))

;; 0 if x and y are the same commit
;; negative if x is parent of y (y derives from x)
;; positive if y is parent of x (x derives from y)
(defmethod ext/compare-versions [:git :git]
  [lib {x-url :git/url, x-rev :rev :as x} {y-url :git/url, y-rev :rev :as y} config]
  (if (= x-rev y-rev)
    0
    (let [desc (or
                 (gitlibs/descendant x-url [x-rev y-rev])
                 (gitlibs/descendant y-url [x-rev y-rev]))]
      (cond
        (nil? desc) (throw (ex-info "No known relationship between git versions" {:x x :y y}))
        (= desc x-rev) 1
        (= desc y-rev) -1))))

(comment
  (ext/canonicalize 'org.clojure/spec.alpha
    {:git/url "https://github.com/clojure/spec.alpha.git" :rev "739c1af5"}
    nil)

  (ext/dep-id 'org.clojure/spec.alpha
    {:git/url "https://github.com/clojure/spec.alpha.git" :rev "739c1af56dae621aedf1bb282025a0d676eff713"}
    nil)

  (ext/manifest-type 'org.clojure/spec.alpha
    {:git/url "https://github.com/clojure/spec.alpha.git" :rev "739c1af56dae621aedf1bb282025a0d676eff713"}
    nil)

  (ext/compare-versions
    'org.clojure/spec.alpha
    {:git/url "https://github.com/clojure/spec.alpha.git" :rev "739c1af56dae621aedf1bb282025a0d676eff713"}
    {:git/url "git@github.com:clojure/spec.alpha.git" :rev "a65fb3aceec67d1096105cab707e6ad7e5f063af"}
    nil)
  )