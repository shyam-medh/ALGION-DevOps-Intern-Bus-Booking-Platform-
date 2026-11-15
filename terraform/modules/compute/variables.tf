variable "environment" {
  type        = string
  default     = "production"
  description = "Deployment environment"
}

variable "subnet_id" {
  type        = string
  description = "Public subnet ID for compute instance"
}

variable "security_group_id" {
  type        = string
  description = "Security Group ID for application compute"
}

variable "instance_type" {
  type        = string
  default     = "t3.small"
  description = "EC2 instance type"
}
