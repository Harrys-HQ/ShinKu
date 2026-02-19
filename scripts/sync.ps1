# ShinKu Sync Tool
# This script helps synchronize the local codebase with TachiyomiSY and Mihon upstreams.

$SY_REMOTE = "sy"
$MIHON_REMOTE = "mihon"
$MAIN_BRANCH = "master"

Write-Host "--- ShinKu Sync Tool ---" -ForegroundColor Cyan

# 1. Fetch updates
Write-Host "[1/3] Fetching updates from remotes..." -ForegroundColor Yellow
git fetch --all

# 2. Check Status against TachiyomiSY
$sy_diff = git rev-list --count HEAD..$($SY_REMOTE + "/master")
if ($sy_diff -gt 0) {
    Write-Host "(!) Your branch is $sy_diff commits behind $SY_REMOTE/master." -ForegroundColor Red
} else {
    Write-Host "(+) Your branch is up-to-date with $SY_REMOTE/master." -ForegroundColor Green
}

# 3. Check Status against Mihon
$mihon_diff = git rev-list --count HEAD..$($MIHON_REMOTE + "/main")
Write-Host "(i) There are $mihon_diff new commits in $MIHON_REMOTE/main since the last common ancestor." -ForegroundColor Blue

Write-Host "`n--- Recommendations ---" -ForegroundColor Cyan
if ($sy_diff -gt 0) {
    Write-Host "To update from TachiyomiSY, run:"
    Write-Host "  git merge $SY_REMOTE/master" -ForegroundColor Gray
}

Write-Host "`nTo check for specific Mihon fixes, use:"
Write-Host "  git log HEAD..$MIHON_REMOTE/main --oneline" -ForegroundColor Gray

Write-Host "`nTo verify build after any merge:"
Write-Host "  ./gradlew assembleDevDebug" -ForegroundColor Gray

Write-Host "`n--- Safety Reminders ---" -ForegroundColor Cyan
Write-Host "1. Google Drive sync filename must remain 'TachiyomiSY_sync.proto.gz'." -ForegroundColor Magenta
Write-Host "2. App name in strings.xml should remain 'ShinKu'." -ForegroundColor Magenta
