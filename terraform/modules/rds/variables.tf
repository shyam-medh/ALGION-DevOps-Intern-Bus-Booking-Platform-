variable "environment" {
  type        = string
  default     = "production"
  description = "Deployment environment"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "List of private subnet IDs for DB subnet group"
}

variable "rds_sg_id" {
  type        = string
  description = "Security Group ID for RDS"
}

variable "db_name" {
  type        = string
  default     = "bus_booking_db"
  description = "Name of the initial database"
}

variable "db_username" {
  type        = string
  default     = "algion_admin"
  description = "Database master username"
}

variable "db_password" {
  type        = string
  sensitive   = true
  description = "Database master password"
}

variable "instance_class" {
  type        = string
  default     = "db.t4g.micro"
  description = "RDS instance class"
}
