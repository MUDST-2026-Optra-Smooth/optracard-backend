param(
    [Parameter(Mandatory=$false)]
    [string]$RunnerToken = ""
)

if (-not $RunnerToken) {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "GitHub Self-Hosted Runner Deployment to Kubernetes" -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "1. Open: https://github.com/MUDST-2026-Optra-Smooth/optracard-backend/settings/actions/runners/new"
    Write-Host "2. Copy the registration token shown under the runner configuration command."
    Write-Host ""
    $RunnerToken = Read-Host "Please enter your GitHub Runner registration token"
}

if (-not $RunnerToken -or $RunnerToken.Trim() -eq "") {
    Write-Error "Runner token cannot be empty."
    exit 1
}

Write-Host "Configuring github-runner namespace and secret..." -ForegroundColor Green
kubectl create namespace github-runner --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic github-runner-secret `
  --namespace=github-runner `
  --from-literal=REPO_URL="https://github.com/MUDST-2026-Optra-Smooth/optracard-backend" `
  --from-literal=RUNNER_TOKEN="$RunnerToken" `
  --from-literal=RUNNER_NAME="k8s-optracard-runner" `
  --from-literal=LABELS="self-hosted,linux,k8s" `
  --dry-run=client -o yaml | kubectl apply -f -

Write-Host "Deploying GitHub Actions Runner deployment and RBAC..." -ForegroundColor Green
kubectl apply -f github-runner.yaml

Write-Host "Checking runner pod status..." -ForegroundColor Green
kubectl get pods -n github-runner -w
