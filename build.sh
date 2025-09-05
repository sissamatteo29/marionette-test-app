#!/bin/bash
# deploy.sh

eval $(minikube docker-env)

echo "Building common code"
cd common && mvn clean install && cd ..

echo "Building Docker images..."
cd services

# Build image-processor-service
echo "Building image-processor-service..."
cd image-processor-service-marionette
mvn clean package
docker build -t image-processor-service:latest .
cd ..

# Build imagestore-service  
echo "Building imagestore-service..."
cd imagestore-service-marionette
mvn clean package
docker build -t imagestore-service:latest .
cd ..

# Build ui-service
echo "Building ui-service..."
cd ui-service-marionette
mvn clean package  
docker build -t ui-service:latest .
cd ..

echo "== Docker images built =="
docker images | grep -E "(image-processor-service|imagestore-service|ui-service)"

# Deploy to minikube
cd k8s
kubectl apply -f namespace.yaml

kubectl apply -f imagestore-service.yaml
kubectl apply -f image-processor-service-marionette.yaml  
kubectl apply -f ui-service.yaml
kubectl rollout restart deployment/image-processor-service -n outfit-app
kubectl rollout restart deployment/imagestore-service -n outfit-app  
kubectl rollout restart deployment/ui-service -n outfit-app

echo "Deploying service monitor"
kubectl apply -f outfit-app-servicemonitor.yaml

echo "Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/image-processor-service -n outfit-app
kubectl wait --for=condition=available --timeout=300s deployment/imagestore-service -n outfit-app
kubectl wait --for=condition=available --timeout=300s deployment/ui-service -n outfit-app

echo "Deployment status:"
kubectl get pods -n outfit-app