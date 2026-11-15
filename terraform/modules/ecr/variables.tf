variable "repository_name" {
  type        = string
  default     = "bus-booking-platform"
  description = "Name of the ECR repository"
}

variable "environment" {
  type        = string
  default     = "production"
  description = "Deployment environment"
}
