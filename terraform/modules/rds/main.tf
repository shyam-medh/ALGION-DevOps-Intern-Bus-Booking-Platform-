resource "aws_db_subnet_group" "main" {
  name       = "algion-db-subnet-group-${var.environment}"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name        = "algion-db-subnet-group-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_db_instance" "primary" {
  identifier             = "algion-postgres-${var.environment}"
  engine                 = "postgres"
  engine_version         = "16.3"
  instance_class         = var.instance_class
  allocated_storage      = 20
  max_allocated_storage  = 100
  storage_type           = "gp3"
  storage_encrypted      = true
  multi_az               = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.rds_sg_id]
  publicly_accessible    = false
  skip_final_snapshot    = true

  backup_retention_period = 7
  backup_window           = "02:00-03:00"
  maintenance_window      = "Sun:04:00-Sun:05:00"

  tags = {
    Name        = "algion-postgres-${var.environment}"
    Environment = var.environment
  }
}
