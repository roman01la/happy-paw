(ns hp.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :filter/Type
  #{2})

(s/def :filter/City
  #{36, 55, 58, 59, 74, 83, 85, 90, 106, 131, 146, 154, 168, 178, 181, 183, 184, 185, 186, 187, 188, 191, 192, 196, 209, 265, 286, 303, 312, 317, 319, 331, 343, 359, 368, 378, 379, 384, 394, 400, 415, 432, 444, 452})

(s/def :filter/Name string?)

(s/def :filter/Age
  #{1 2 3 4 5 6 7 8 9 10 11})

(s/def :filter/Size
  #{5 6 7 8})

(s/def :filter/Gender
  #{:male :female})

(s/def :filter/Sterile
  #{:sterile :nosterile})

(s/def :filter/Uniq string?)

(s/def :filter/AnimalId string?)

(s/def :filter/Host
  #{-1, 45, 48, 55, 57, 58, 59, 60, 61, 63, 64, 65, 66, 67, 69, 70, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116})

(s/def :filter/Labels
  #{:not-sheltered :sheltered :ill})
