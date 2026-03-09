#!/bin/bash
# Run this script ONCE on a fresh Hetzner CX31 (Ubuntu 24.04)
# as root or with sudo.
set -e

# 1. Install k3s (includes Traefik ingress)
curl -sfL https://get.k3s.io | sh -

# 2. Wait for k3s to be ready
sleep 15
kubectl get nodes

# 3. Install cert-manager (for Let's Encrypt TLS)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml
echo "Waiting for cert-manager..."
kubectl wait --for=condition=Available deployment/cert-manager -n cert-manager --timeout=120s
kubectl wait --for=condition=Available deployment/cert-manager-webhook -n cert-manager --timeout=120s

# 4. Create app directory
mkdir -p /opt/insurance/k8s

# 5. Create namespace and secret (edit secret.yaml first!)
echo "---"
echo "Next steps:"
echo "  1. Copy k8s/ manifests to /opt/insurance/k8s/ on this server"
echo "  2. Edit /opt/insurance/k8s/cert-issuer.yaml — set your email"
echo "  3. Edit /opt/insurance/k8s/secret.yaml   — set real passwords"
echo "  4. kubectl apply -f /opt/insurance/k8s/namespace.yaml"
echo "  5. kubectl apply -f /opt/insurance/k8s/secret.yaml"
echo "  6. kubectl apply -f /opt/insurance/k8s/cert-issuer.yaml"
echo "  7. kubectl apply -f /opt/insurance/k8s/postgres.yaml"
echo "  8. kubectl apply -f /opt/insurance/k8s/engine.yaml"
echo "  9. kubectl apply -f /opt/insurance/k8s/backend.yaml"
echo " 10. kubectl apply -f /opt/insurance/k8s/frontend.yaml"
echo " 11. kubectl apply -f /opt/insurance/k8s/ingress.yaml"
echo "---"
echo "For GitHub Actions add these secrets to your repo:"
echo "  HETZNER_HOST  — server IP"
echo "  HETZNER_USER  — root (or your sudo user)"
echo "  HETZNER_SSH_KEY — private SSH key (GitHub Actions runner key)"
echo "  Also replace GITHUB_USER in k8s/*.yaml with your GitHub username"
