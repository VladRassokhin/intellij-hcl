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