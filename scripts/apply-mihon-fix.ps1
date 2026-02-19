param (
    [Parameter(Mandatory=$true)]
    [string]$CommitHash
)

Write-Host "--- ShinKu: Applying Mihon Fix ---" -ForegroundColor Cyan
Write-Host "Target Hash: $CommitHash" -ForegroundColor Yellow

# Attempt to cherry-pick
Write-Host "Attempting to cherry-pick $CommitHash..."
git cherry-pick $CommitHash

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[!] Conflict detected during cherry-pick." -ForegroundColor Red
    Write-Host "Please resolve conflicts manually, then run 'git cherry-pick --continue'."
} else {
    Write-Host "`n[+] Fix applied successfully!" -ForegroundColor Green
    Write-Host "Run build verification: ./gradlew assembleDevDebug"
}
