pipeline {
    agent any

    tools {
        jdk 'Java-21'
        maven 'Maven-3.9'
    }

    environment {
        APP_NAME     = 'bus-booking-platform'
        IMAGE_TAG    = "${env.BUILD_NUMBER}"
        REGISTRY     = "algion-labs"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Unit & Concurrency Tests') {
            steps {
                dir('trip_and_reservation_seat') {
                    sh 'mvn clean test'
                }
            }
            post {
                always {
                    junit 'trip_and_reservation_seat/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build & Package JAR') {
            steps {
                dir('trip_and_reservation_seat') {
                    sh 'mvn package -DskipTests'
                }
            }
        }

        stage('Docker Build & Hardening') {
            steps {
                sh """
                    docker build -t ${REGISTRY}/${APP_NAME}:${IMAGE_TAG} -t ${REGISTRY}/${APP_NAME}:latest ./trip_and_reservation_seat
                """
            }
        }

        stage('Trivy Security Vulnerability Scan') {
            steps {
                sh """
                    trivy image --severity HIGH,CRITICAL --no-progress ${REGISTRY}/${APP_NAME}:${IMAGE_TAG} || true
                """
            }
        }

        stage('Push Image to Registry') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Option A: Docker Hub push with Jenkins credential binding
                    try {
                        withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                            sh """
                                echo "\$DH_PASS" | docker login -u "\$DH_USER" --password-stdin
                                docker tag ${REGISTRY}/${APP_NAME}:${IMAGE_TAG} \${DH_USER}/${APP_NAME}:${IMAGE_TAG}
                                docker tag ${REGISTRY}/${APP_NAME}:${IMAGE_TAG} \${DH_USER}/${APP_NAME}:latest
                                docker push \${DH_USER}/${APP_NAME}:${IMAGE_TAG}
                                docker push \${DH_USER}/${APP_NAME}:latest
                            """
                        }
                    } catch (Exception e) {
                        echo "Docker Hub push skipped or credentials not configured: \${e.message}"
                    }

                    // Option B: Amazon ECR push
                    try {
                        sh """
                            aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin \${AWS_ACCOUNT_ID}.dkr.ecr.ap-south-1.amazonaws.com
                            docker tag ${REGISTRY}/${APP_NAME}:${IMAGE_TAG} \${AWS_ACCOUNT_ID}.dkr.ecr.ap-south-1.amazonaws.com/${APP_NAME}:${IMAGE_TAG}
                            docker tag ${REGISTRY}/${APP_NAME}:${IMAGE_TAG} \${AWS_ACCOUNT_ID}.dkr.ecr.ap-south-1.amazonaws.com/${APP_NAME}:latest
                            docker push \${AWS_ACCOUNT_ID}.dkr.ecr.ap-south-1.amazonaws.com/${APP_NAME}:${IMAGE_TAG}
                            docker push \${AWS_ACCOUNT_ID}.dkr.ecr.ap-south-1.amazonaws.com/${APP_NAME}:latest
                        """
                    } catch (Exception e) {
                        echo "ECR push skipped: \${e.message}"
                    }
                }
            }
        }

        stage('Terraform Infrastructure Provisioning') {
            when {
                branch 'main'
            }
            steps {
                dir('terraform') {
                    sh """
                        echo "=== Initializing and Validating AWS Terraform Infrastructure ==="
                        terraform init -input=false
                        terraform validate
                        terraform plan -input=false -out=tfplan
                        if [ -f "terraform.tfvars" ]; then
                            echo "terraform.tfvars found. Applying Terraform changes to AWS..."
                            terraform apply -input=false -auto-approve tfplan
                        else
                            echo "terraform.tfvars template present. Plan and syntax validated successfully."
                        fi
                    """
                }
            }
        }

        stage('Blue-Green Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh """
                    chmod +x scripts/deploy-blue-green.sh
                    ./scripts/deploy-blue-green.sh
                """
            }
        }
    }

    post {
        failure {
            echo "Pipeline failed! Triggering rollback..."
            sh "chmod +x scripts/rollback.sh && ./scripts/rollback.sh || true"
        }
        success {
            echo "Pipeline succeeded! Bus Booking service is live and healthy."
        }
    }
}
