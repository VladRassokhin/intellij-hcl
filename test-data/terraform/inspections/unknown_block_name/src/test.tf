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
