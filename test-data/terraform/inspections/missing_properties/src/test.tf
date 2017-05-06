provider "docker" {
}
provider "aws" {
}
terraform {
  backend "s3" {
  }
}