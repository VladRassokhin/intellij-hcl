variable "foo" {
  type    = "string"
  default = "str"
}
output "test" {
  value = "${var.foo}"
}
output "second" {
  value = "${var.foo}"
}