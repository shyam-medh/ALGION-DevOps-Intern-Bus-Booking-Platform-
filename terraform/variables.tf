variable "aws_region" {
  type        = string
  default     = "ap-south-1" # Mumbai region for Indian transport network
  description = "AWS deployment region"
}

variable "environment" {
  type        = string
  default     = "production"
  description = "Deployment environment"
}

variable "vpc_cidr" {
  type        = string
  default     = "10.0.0.0/16"
  description = "CIDR block for the VPC"
}

variable "db_password" {
  type        = string
  sensitive   = true
  default     = "SecurePostgres2026!ChangeMe"
  description = "PostgreSQL Administrator Password"
}

variable "ec2_instance_type" {
  type        = string
  default     = "t3.small"
  description = "EC2 instance size for Docker host"
}
