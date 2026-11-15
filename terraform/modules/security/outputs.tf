output "alb_sg_id" {
  value       = aws_security_group.alb.id
  description = "Security Group ID for ALB / Nginx"
}

output "app_sg_id" {
  value       = aws_security_group.app.id
  description = "Security Group ID for Application Server"
}

output "rds_sg_id" {
  value       = aws_security_group.rds.id
  description = "Security Group ID for PostgreSQL RDS"
}
