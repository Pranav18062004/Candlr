param(
    [Parameter(Mandatory = $true)][string]$AndroidSdk,
    [Parameter(Mandatory = $true)][string]$CertificateSha256
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
foreach ($name in @('CANDLR_KEYSTORE', 'CANDLR_KEYSTORE_PASSWORD', 'CANDLR_KEY_ALIAS', 'CANDLR_KEY_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($name)) { throw "Configure $name locally before preparing a production release." }
}
& .\gradlew.bat :app:verifyUploadSigning :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:bundleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Release build or validation failed.' }
& python scripts/audit_package.py --sdk $AndroidSdk --variant release --apk app/build/outputs/apk/release/app-release.apk --certificate-sha256 $CertificateSha256
if ($LASTEXITCODE -ne 0) { throw 'Production package/certificate audit failed.' }
& (Join-Path $AndroidSdk 'build-tools/35.0.0/zipalign.exe') -c -P 16 4 app/build/outputs/apk/release/app-release.apk
if ($LASTEXITCODE -ne 0) { throw 'APK alignment check failed.' }
$releaseFolder = Join-Path (Get-Location) 'artifacts/release'
New-Item -ItemType Directory -Force -Path $releaseFolder | Out-Null
Copy-Item app/build/outputs/apk/release/app-release.apk (Join-Path $releaseFolder 'Candlr-1.0.0.apk')
Copy-Item app/build/outputs/bundle/release/app-release.aab (Join-Path $releaseFolder 'Candlr-1.0.0.aab')
Copy-Item app/build/outputs/mapping/release/mapping.txt (Join-Path $releaseFolder 'mapping.txt')
Copy-Item artifacts/package-audit.json (Join-Path $releaseFolder 'package-audit.json')
$hashes = Get-ChildItem -LiteralPath $releaseFolder -File | Where-Object Name -ne 'SHA256SUMS.txt' | ForEach-Object {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(), $_.Name
}
$hashes | Set-Content -Encoding ascii (Join-Path $releaseFolder 'SHA256SUMS.txt')
Write-Output "Signed artifacts prepared in $releaseFolder. Complete device checks before publishing the GitHub draft."
