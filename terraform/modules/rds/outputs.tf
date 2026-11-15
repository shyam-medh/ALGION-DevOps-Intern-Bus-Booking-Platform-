output "endpoint" {
  value       = aws_db_instance.primary.endpoint
  description = "Connection endpoint for PostgreSQL RDS"
}

output "db_instance_id" {
  value       = aws_db_instance.primary.id
  description = "ID of the RDS instance"
}
