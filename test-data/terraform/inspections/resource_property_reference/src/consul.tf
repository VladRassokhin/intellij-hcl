data "consul_keys" "demo" {
}
data "consul_keys" "demo2" {
  var {}
}
resource "archive_file" "init" {
  source_content = "${data.consul_keys.demo.var.example}"
}
resource "archive_file" "init" {
  source_content = "${data.consul_keys.demo2.var.example}"
}