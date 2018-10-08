provider "aws" {
  access_key = "" // default empty string
  insecure = false  // default bool
  max_retries = 25  // default int
  region = "us-east-1"

  endpoints {
    acm = ""  // default in nested block
  }
}

resource "aws_autoscaling_group" "foo" {
  max_size = 0
  min_size = 0
  metrics_granularity = "1Minute" // default non-empty string
}

resource "google_compute_backend_service" "foo" {
  health_checks = []
  name = ""

  backend {
    max_utilization = "0.8" // default float
  }
}
