#!/usr/bin/env pwsh

Write-Host "Building Flipping Optimizer plugin with Gradle..." -ForegroundColor Cyan

# Use installed Gradle instead of wrapper
gradle shadowJar

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Build successful! Starting RuneLite..." -ForegroundColor Green
    Write-Host ""
    java -ea -jar build\libs\Flip_off-1.0-SNAPSHOT-all.jar
} else {
    Write-Host ""
    Write-Host "Build failed. Please check the errors above." -ForegroundColor Red
    exit 1
}
