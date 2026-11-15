output "vpc_id" {
  value       = module.vpc.vpc_id
  description = "ID of the provisioned VPC"
}

output "rds_endpoint" {
  value       = module.rds.endpoint
  description = "Connection endpoint for PostgreSQL RDS"
}

output "ecr_repository_url" {
  value       = module.ecr.repository_url
  description = "Push URL for Amazon ECR repository"
}

output "app_server_public_ip" {
  value       = module.compute.public_ip
  description = "Public IP of the application server"
}
