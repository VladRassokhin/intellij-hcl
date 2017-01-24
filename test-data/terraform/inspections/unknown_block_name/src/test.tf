resource "google_compute_instance" "mesos-slave" {
  network_interface {
    network = "default"
    access_config {
        // Ephemeral IP
    }
    abracadabra {
    }
  }
}

terraform {
  required_version = "> 0.8.0"
}
