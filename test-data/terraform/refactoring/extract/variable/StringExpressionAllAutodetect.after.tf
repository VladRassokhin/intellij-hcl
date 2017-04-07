variable "value" {
  type    = "string"
  default = "str"
}
output "test" {
  value = "${var.value}"
}
output "second" {
  value = "${var.value}"
}