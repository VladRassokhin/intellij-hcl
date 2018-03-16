provider "docker" {
}
provider "aws" {
}
//noinspection MissingProperty
provider "atlas" {
}
terraform {
  backend "s3" {
  }
}