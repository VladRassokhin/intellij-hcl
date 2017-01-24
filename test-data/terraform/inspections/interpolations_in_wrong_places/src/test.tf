variable "x" {
  type = "${var.x}"
  default = "${var.y.y}"
}
variable "y" {
  type = "map"
  default = {
    x = "${var.x}"
    y = <<EOL
Two interpolation in one line ${var.x} should be reported separately ${var.y}
EOL
  }
}
module "a" {
  source = "${var.x}"
}
resource "a" "b" {
  depends_on = ["${var.y}"]
}
terraform {
  required_version = "> ${version}"
}