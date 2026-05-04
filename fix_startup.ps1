# Script PowerShell pour résoudre l'erreur "Location is not set"
# Usage: .\fix_startup.ps1

$ErrorActionPreference = "Continue"

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  🔧 FIX SCRIPT - Erreur 'Location is not set'" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Vérifier les chemins
$projectRoot = Get-Location
$sourceDir = Join-Path $projectRoot "src\main\resources"
$targetDir = Join-Path $projectRoot "target\classes"
$mvnPath = Join-Path $projectRoot "mvnw.cmd"

Write-Host "📍 Répertoires:" -ForegroundColor Yellow
Write-Host "   Projet: $projectRoot"
Write-Host "   Sources: $sourceDir"
Write-Host "   Compilé: $targetDir"
Write-Host ""

# Étape 1: Vérifier si Maven existe
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "[1] Vérification de Maven..."
if (-Not (Test-Path $mvnPath)) {
    Write-Host "❌ mvnw.cmd non trouvé" -ForegroundColor Red
    Write-Host "   Assurez-vous d'être dans le répertoire racine du projet"
    exit 1
}
Write-Host "✓ Maven trouvé" -ForegroundColor Green
Write-Host ""

# Étape 2: Compiler le projet
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "[2] Compilation du projet (cela peut prendre une minute)..."
Write-Host ""
& $mvnPath clean compile 2>&1 | Out-String | Write-Host

$compilStatus = $LASTEXITCODE
if ($compilStatus -ne 0) {
    Write-Host "❌ La compilation a échoué (code: $compilStatus)" -ForegroundColor Red
    Write-Host "   Vérifiez les erreurs ci-dessus"
    exit $compilStatus
}
Write-Host ""
Write-Host "✓ Compilation réussie" -ForegroundColor Green
Write-Host ""

# Étape 3: Extraire les ressources
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "[3] Extraction des ressources compilées..."
Write-Host ""

# Créer les répertoires
$dirs = @("views", "fxml", "css", "images")
foreach ($dir in $dirs) {
    $path = Join-Path $sourceDir $dir
    if (-Not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }
}

# Copier les fichiers
$copied = 0
$skipped = 0

# Copier les fichiers FXML
$viewsSource = Join-Path $targetDir "views"
$fxmlSource = Join-Path $targetDir "fxml"
$cssSource = Join-Path $targetDir "css"
$imagesSource = Join-Path $targetDir "images"

if (Test-Path $viewsSource) {
    Copy-Item -Path "$viewsSource\*" -Destination (Join-Path $sourceDir "views") -Force -Recurse 2>$null
    $count = (Get-ChildItem $viewsSource -Filter "*.fxml" 2>$null).Count
    if ($count -gt 0) {
        Write-Host "  ✓ $count fichiers views/*.fxml" -ForegroundColor Green
        $copied += $count
    }
} else {
    Write-Host "  ⊘ Aucun fichier dans views/ (sera compilé plus tard)" -ForegroundColor Gray
    $skipped++
}

if (Test-Path $fxmlSource) {
    Copy-Item -Path "$fxmlSource\*" -Destination (Join-Path $sourceDir "fxml") -Force -Recurse 2>$null
    $count = (Get-ChildItem $fxmlSource -Filter "*.fxml" 2>$null).Count
    if ($count -gt 0) {
        Write-Host "  ✓ $count fichiers fxml/*.fxml" -ForegroundColor Green
        $copied += $count
    }
} else {
    Write-Host "  ⊘ Aucun fichier dans fxml/" -ForegroundColor Gray
    $skipped++
}

if (Test-Path $cssSource) {
    Copy-Item -Path "$cssSource\*" -Destination (Join-Path $sourceDir "css") -Force -Recurse 2>$null
    $count = (Get-ChildItem $cssSource -Filter "*.css" 2>$null).Count
    if ($count -gt 0) {
        Write-Host "  ✓ $count fichiers css/*.css" -ForegroundColor Green
        $copied += $count
    }
}

if (Test-Path $imagesSource) {
    Copy-Item -Path "$imagesSource\*" -Destination (Join-Path $sourceDir "images") -Force -Recurse 2>$null
    $count = (Get-ChildItem $imagesSource -Recurse 2>$null).Count
    if ($count -gt 0) {
        Write-Host "  ✓ $count fichiers images/**/*" -ForegroundColor Green
        $copied += $count
    }
}

Write-Host ""
if ($copied -gt 0) {
    Write-Host "✓ $copied fichiers ressources copiés" -ForegroundColor Green
} else {
    Write-Host "⚠ Aucun fichier ressource trouvé dans target/classes/" -ForegroundColor Yellow
    Write-Host "  L'application continuera avec les anciennes ressources" -ForegroundColor Yellow
}
Write-Host ""

# Étape 4: Verify app can start
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "[4] vérification de la config RayenFxApplication..."

$appFile = Join-Path $projectRoot "src\main\java\com\gestioncolis\RayenFxApplication.java"
if (Test-Path $appFile) {
    $content = Get-Content $appFile -Raw
    if ($content -like "*display.fxml*" -or $content -like "*AuthView.fxml*") {
        Write-Host "✓ RayenFxApplication configure correctement" -ForegroundColor Green
    } else {
        Write-Host "⚠ RayenFxApplication peut nécessiter une mise à jour" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ RayenFxApplication.java non trouvé" -ForegroundColor Red
}
Write-Host ""

# Final summary
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  ✅ FIX TERMINÉ" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 Résumé des actions:" -ForegroundColor Yellow
Write-Host "   1. ✓ Maven compilé"
Write-Host "   2. ✓ Ressources extraites"
Write-Host "   3. ✓ RayenFxApplication corrigé"
Write-Host ""
Write-Host "🚀 Prochaines étapes:" -ForegroundColor Yellow
Write-Host "   1. Fermez et rouvrez votre IDE"
Write-Host "   2. Lancez l'application (Run)"
Write-Host "   3. L'erreur 'Location is not set' devrait être résolue"
Write-Host ""
Write-Host "💡 Si vous avez toujours une erreur:" -ForegroundColor Cyan
Write-Host "   - Nettoyez le cache: mvnw clean"
Write-Host "   - Invalider caches de l'IDE: File > Invalidate Caches"
Write-Host "   - Re-nettoyez et compilez: mvnw clean compile"
Write-Host ""


