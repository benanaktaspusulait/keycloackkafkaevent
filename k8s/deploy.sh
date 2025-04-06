#!/bin/bash

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "kubectl is not installed. Please install it first."
    exit 1
fi

# Check if kustomize is installed
if ! command -v kustomize &> /dev/null; then
    echo "kustomize is not installed. Please install it first."
    exit 1
fi

# Function to deploy to a specific environment
deploy_environment() {
    local env=$1
    echo "Deploying to $env environment..."
    
    # Apply the kustomization
    kustomize build k8s/overlays/$env | kubectl apply -f -
    
    # Wait for deployments to be ready
    echo "Waiting for deployments to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/keycloak -n smartface
    kubectl wait --for=condition=available --timeout=300s deployment/event-service -n smartface
    
    # Wait for statefulsets to be ready
    echo "Waiting for statefulsets to be ready..."
    kubectl wait --for=condition=ready --timeout=300s statefulset/postgres -n smartface
    kubectl wait --for=condition=ready --timeout=300s statefulset/redpanda -n smartface
    
    echo "Deployment to $env environment completed!"
}

# Main script
case "$1" in
    "dev")
        deploy_environment "dev"
        ;;
    "prod")
        deploy_environment "prod"
        ;;
    *)
        echo "Usage: $0 {dev|prod}"
        exit 1
        ;;
esac 