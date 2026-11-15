# Security Group: Public Ingress (ALB / Nginx Reverse Proxy)
resource "aws_security_group" "alb" {
  name        = "algion-alb-sg-${var.environment}"
  description = "Allow inbound HTTP (80) and HTTPS (443) traffic"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTP Public"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS Public"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "algion-alb-sg-${var.environment}"
    Environment = var.environment
  }
}

# Security Group: Application Tier (EC2 / Container)
resource "aws_security_group" "app" {
  name        = "algion-app-sg-${var.environment}"
  description = "Allow inbound traffic from ALB only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "App Port from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description = "SSH for Administration (Optional)"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "algion-app-sg-${var.environment}"
    Environment = var.environment
  }
}

# Security Group: Isolated RDS PostgreSQL Tier
resource "aws_security_group" "rds" {
  name        = "algion-rds-sg-${var.environment}"
  description = "Allow inbound PostgreSQL from App tier only"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from App Tier"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "algion-rds-sg-${var.environment}"
    Environment = var.environment
  }
}
