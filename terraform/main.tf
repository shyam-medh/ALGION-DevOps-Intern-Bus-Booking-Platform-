terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# 1. Modular VPC & Networking
module "vpc" {
  source      = "./modules/vpc"
  vpc_cidr    = var.vpc_cidr
  environment = var.environment
  aws_region  = var.aws_region
}

# 2. Modular Least-Privilege Security Groups
module "security" {
  source      = "./modules/security"
  vpc_id      = module.vpc.vpc_id
  environment = var.environment
}

# 3. Modular Multi-AZ RDS PostgreSQL
module "rds" {
  source             = "./modules/rds"
  environment        = var.environment
  private_subnet_ids = module.vpc.private_subnet_ids
  rds_sg_id          = module.security.rds_sg_id
  db_password        = var.db_password
}

# 4. Modular Amazon ECR Container Registry
module "ecr" {
  source          = "./modules/ecr"
  repository_name = "bus-booking-platform"
  environment     = var.environment
}

# 5. Modular Compute (EC2 Docker Host)
module "compute" {
  source            = "./modules/compute"
  environment       = var.environment
  subnet_id         = module.vpc.public_subnet_ids[0]
  security_group_id = module.security.app_sg_id
  instance_type     = var.ec2_instance_type
}
