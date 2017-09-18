data "terraform_remote_state" "remote" {
  backend = ""
}
resource "terraform_remote_state" "r2" {
  backend = ""
}
module "staging" {
  source = ""
  docker_repo_url="${data.terraform_remote_state.remote.docker_repo_url}"
  docker_repo_url2="${terraform_remote_state.r2.docker_repo_url}"
}

resource "aws_launch_configuration" "launch_config" {
  key_name = "${data.terraform_remote_state.remote.ssh_key_name.a.b.c}"
}