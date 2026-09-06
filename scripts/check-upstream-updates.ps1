<#
.SYNOPSIS
    ShinKu Upstream Update Checker
    Checks for the latest releases, commits, and merged pull requests from
    upstream repositories (Mihon and TachiyomiSY) to identify features and fixes
    that should be ported to ShinKu.

.DESCRIPTION
    This script queries GitHub REST APIs for:
    - mihonapp/mihon (Mihon upstream)
    - jobobby04/TachiyomiSY (TachiyomiSY upstream)

    It categorizes updates into high-value areas (Anti-Bot/WebView, Downloader,
    Backup, Trackers, Reader/UI, and Dependencies) and displays actionable links.

.PARAMETER Target
    Repository to inspect: "All" (default), "Mihon", or "TachiyomiSY".

.PARAMETER Limit
    Number of recent releases and PRs/commits to fetch (default: 10).

.PARAMETER GitHubToken
    Optional GitHub Personal Access Token to avoid API rate limits (60 req/hr unauth vs 5,000 auth).
    Can also be provided via the GITHUB_TOKEN environment variable.

.PARAMETER SetupGitRemotes
    If specified, adds/configures local git remotes 'mihon' and 'sy' for local diff inspection.

.EXAMPLE
    pwsh scripts/check-upstream-updates.ps1
    pwsh scripts/check-upstream-updates.ps1 -Target Mihon -Limit 15
    pwsh scripts/check-upstream-updates.ps1 -SetupGitRemotes
#>

[CmdletBinding()]
param (
    [ValidateSet("All", "Mihon", "TachiyomiSY")]
    [string]$Target = "All",

    [int]$Limit = 10,

    [string]$GitHubToken = $env:GITHUB_TOKEN,

    [switch]$SetupGitRemotes
)

$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         ShinKu - Upstream Update Checker                 " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Optionally setup Git remotes
if ($SetupGitRemotes) {
    Write-Host "[*] Configuring Git Remotes..." -ForegroundColor Yellow
    $remotes = git remote
    if ($remotes -notcontains "mihon") {
        Write-Host "  -> Adding 'mihon' remote (https://github.com/mihonapp/mihon.git)" -ForegroundColor Gray
        git remote add mihon https://github.com/mihonapp/mihon.git
    } else {
        Write-Host "  -> 'mihon' remote already present" -ForegroundColor Gray
    }

    if ($remotes -notcontains "sy") {
        Write-Host "  -> Adding 'sy' remote (https://github.com/jobobby04/TachiyomiSY.git)" -ForegroundColor Gray
        git remote add sy https://github.com/jobobby04/TachiyomiSY.git
    } else {
        Write-Host "  -> 'sy' remote already present" -ForegroundColor Gray
    }

    Write-Host "  -> Fetching remote branches..." -ForegroundColor Gray
    git fetch mihon main --tags --quiet
    git fetch sy master --tags --quiet
    Write-Host "  [+] Remotes configured and fetched.`n" -ForegroundColor Green
}

# 2. HTTP Helper
function Invoke-GhApi {
    param ([string]$Path)

    $headers = @{
        "User-Agent" = "ShinKu-Upstream-Checker/1.0"
        "Accept"     = "application/vnd.github.v3+json"
    }

    if (![string]::IsNullOrWhiteSpace($GitHubToken)) {
        $headers["Authorization"] = "Bearer $GitHubToken"
    }

    $uri = "https://api.github.com/$Path"
    try {
        $response = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get
        return $response
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 403) {
            Write-Warning "GitHub API rate limit exceeded. Set GITHUB_TOKEN environment variable or pass -GitHubToken."
        } else {
            Write-Warning "API request failed for ${uri}: $($_.Exception.Message)"
        }
        return $null
    }
}

# 3. Categorization logic
function Get-ChangeCategory {
    param ([string]$Text)

    $t = $Text.ToLower()
    if ($t -match "webview|user-agent|sec-ch-ua|turnstile|cloudflare|anti-bot|browser") {
        return @{ Tag = "[Anti-Bot/WebView]"; Color = "Red" }
    }
    if ($t -match "download|chunk|resume|cbz|storage|diskcache") {
        return @{ Tag = "[Downloader]"; Color = "Magenta" }
    }
    if ($t -match "backup|restore|sync|category") {
        return @{ Tag = "[Backup/Sync]"; Color = "Yellow" }
    }
    if ($t -match "tracker|anilist|myanimelist|mal|bangumi|shikimori") {
        return @{ Tag = "[Tracker]"; Color = "DarkCyan" }
    }
    if ($t -match "reader|slider|pager|viewer|fullscreen|appbar|toolbar|navigator") {
        return @{ Tag = "[Reader/UI]"; Color = "Cyan" }
    }
    if ($t -match "source|extension|repo|protobuf|filter|nsfw") {
        return @{ Tag = "[Source API]"; Color = "Blue" }
    }
    if ($t -match "bump|gradle|kotlin|compose|libs\.versions|dependency|r8|compiler") {
        return @{ Tag = "[Dependencies]"; Color = "DarkGray" }
    }
    return @{ Tag = "[General]"; Color = "White" }
}

function Format-Date {
    param ([string]$DateStr)
    if ([string]::IsNullOrWhiteSpace($DateStr)) { return "" }
    try {
        return [DateTimeOffset]::Parse($DateStr).LocalDateTime.ToString("yyyy-MM-dd")
    } catch {
        try {
            return ([DateTime]$DateStr).ToString("yyyy-MM-dd")
        } catch {
            return $DateStr
        }
    }
}

# 4. Check Single Repository
function Check-Repo {
    param (
        [string]$Name,
        [string]$Owner,
        [string]$Repo,
        [string]$Branch
    )

    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " [$Name] https://github.com/$Owner/$Repo" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green

    # Fetch Releases
    Write-Host "`n>> Recent Releases:" -ForegroundColor Yellow
    $releases = Invoke-GhApi -Path "repos/$Owner/$Repo/releases?per_page=5"
    if ($releases) {
        foreach ($rel in $releases) {
            $pubDate = Format-Date $rel.published_at
            Write-Host "  * Tag: $($rel.tag_name) ($pubDate)" -ForegroundColor White -NoNewline
            if ($rel.prerelease) {
                Write-Host " [Pre-release]" -ForegroundColor DarkYellow -NoNewline
            }
            Write-Host " - $($rel.name)" -ForegroundColor Gray
            Write-Host "    Link: $($rel.html_url)" -ForegroundColor DarkGray
        }
    } else {
        Write-Host "  (No release data available)" -ForegroundColor DarkGray
    }

    # Fetch Merged PRs
    Write-Host "`n>> Recent Merged Pull Requests (Up to $Limit):" -ForegroundColor Yellow
    $prs = Invoke-GhApi -Path "repos/$Owner/$Repo/pulls?state=closed&sort=updated&direction=desc&per_page=$Limit"
    if ($prs) {
        $mergedCount = 0
        foreach ($pr in $prs) {
            if (-not $pr.merged_at) { continue }
            $mergedCount++
            $mergedDate = Format-Date $pr.merged_at
            $cat = Get-ChangeCategory -Text "$($pr.title) $($pr.body)"

            Write-Host "  #$($pr.number) " -ForegroundColor Cyan -NoNewline
            Write-Host "$($cat.Tag) " -ForegroundColor $cat.Color -NoNewline
            Write-Host "$($pr.title) " -ForegroundColor White -NoNewline
            Write-Host "($mergedDate)" -ForegroundColor DarkGray
            Write-Host "    URL: $($pr.html_url)" -ForegroundColor DarkGray
        }

        if ($mergedCount -eq 0) {
            Write-Host "  (No recently merged PRs found)" -ForegroundColor DarkGray
        }
    }

    # Fetch Recent Commits on Default Branch
    Write-Host "`n>> Recent Commits on $Branch (Up to 5):" -ForegroundColor Yellow
    $commits = Invoke-GhApi -Path "repos/$Owner/$Repo/commits?sha=$Branch&per_page=5"
    if ($commits) {
        foreach ($c in $commits) {
            $sha = $c.sha.Substring(0, 7)
            $date = Format-Date $c.commit.committer.date
            $firstLine = ($c.commit.message -split "`n")[0]
            $cat = Get-ChangeCategory -Text $firstLine

            Write-Host "  $sha " -ForegroundColor DarkYellow -NoNewline
            Write-Host "$($cat.Tag) " -ForegroundColor $cat.Color -NoNewline
            Write-Host "$firstLine " -ForegroundColor White -NoNewline
            Write-Host "($date)" -ForegroundColor DarkGray
        }
    }
    Write-Host ""
}

# Run inspections
if ($Target -eq "All" -or $Target -eq "Mihon") {
    Check-Repo -Name "Mihon" -Owner "mihonapp" -Repo "mihon" -Branch "main"
}

if ($Target -eq "All" -or $Target -eq "TachiyomiSY") {
    Check-Repo -Name "TachiyomiSY" -Owner "jobobby04" -Repo "TachiyomiSY" -Branch "master"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Tip: To inspect local diffs with git, run with -SetupGitRemotes:" -ForegroundColor Gray
Write-Host "     pwsh scripts/check-upstream-updates.ps1 -SetupGitRemotes" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan
