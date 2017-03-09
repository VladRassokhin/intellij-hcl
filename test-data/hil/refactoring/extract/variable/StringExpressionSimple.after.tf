variable "str" {
  type = "string"
  default = "str"
}
output "test" {
  value = "${var.str}"
}
