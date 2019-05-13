variable "stringButMap" {
  type = "string"
  default = {
    x = 42
  }
}
variable "stringButList" {
  type = "string"
  default = [
    42
  ]
}
variable "mapButString" {
  type = "map"
  default = "42"
}
variable "mapButList" {
  type = "map"
  default = [
    42
  ]
}
variable "listButString" {
  type = "list"
  default = "42"
}
variable "listButMap" {
  type = "list"
  default = {
    x = 42
  }
}
variable "weird" {
  type = "weird"
}
variable "infer-map" {
  default = {
    a = "value-a"
  }
}
variable "infer-list" {
  default = [
    "list1",
    "list2",
  ]
}