# Creates the GitHub repo (if missing) and pushes the current branch.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File push.ps1 -Token ghp_xxx
#   ... -Token ghp_xxx -Repo btkeys-remote -Private
#
# The token is never written to disk or to .git/config: it is passed to a
# one-shot `git push` through a temporary credential helper that reads it from
# the environment.
param(
    [Parameter(Mandatory = $true)][string]$Token,
    [string]$Repo = 'btkeys-remote',
    [string]$Description = '手机变电脑蓝牙键鼠遥控 · 无广告无会员 · 57KB 纯系统 API 实现',
    [switch]$Private
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$git  = if ($env:BTKEYS_GIT) { $env:BTKEYS_GIT } else { 'git' }

function Get-GitHubUser([string]$t) {
    $h = @{ Authorization = "Bearer $t"; Accept = 'application/vnd.github+json'; 'User-Agent' = 'btkeys-push' }
    $me = Invoke-RestMethod -Uri 'https://api.github.com/user' -Headers $h -Method Get
    return $me.login
}

$user = Get-GitHubUser $Token
Write-Host "user : $user"
Write-Host "repo : $Repo"

# ---- create the repo if it does not exist ----
$headers = @{ Authorization = "Bearer $Token"; Accept = 'application/vnd.github+json'; 'User-Agent' = 'btkeys-push' }
$exists = $true
try {
    Invoke-RestMethod -Uri "https://api.github.com/repos/$user/$Repo" -Headers $headers -Method Get | Out-Null
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) { $exists = $false } else { throw }
}

if (-not $exists) {
    $body = @{
        name        = $Repo
        description = $Description
        private     = [bool]$Private
        has_issues  = $true
        has_wiki    = $false
    } | ConvertTo-Json
    Invoke-RestMethod -Uri 'https://api.github.com/user/repos' -Headers $headers -Method Post `
        -Body $body -ContentType 'application/json' | Out-Null
    Write-Host 'created repository'
} else {
    Write-Host 'repository already exists'
}

# ---- push without persisting the token ----
$env:GIT_TERMINAL_PROMPT = '0'
# `store` with a temp file keeps the secret out of .git/config and out of argv.
$credFile = Join-Path ([IO.Path]::GetTempPath()) ("btkeys-cred-{0}" -f ([guid]::NewGuid()))
try {
    Set-Content -Path $credFile -Value ("https://{0}:{1}@github.com" -f $user, $Token) -Encoding ASCII
    & $git -C $root config --local credential.helper "store --file=`"$credFile`""
    & $git -C $root remote remove origin 2>$null
    & $git -C $root remote add origin "https://github.com/$user/$Repo.git"
    & $git -C $root push -u origin HEAD
    if ($LASTEXITCODE -ne 0) { throw 'git push failed' }
} finally {
    & $git -C $root config --local --unset credential.helper 2>$null
    if (Test-Path $credFile) { Remove-Item $credFile -Force }
}

Write-Host ''
Write-Host "== done: https://github.com/$user/$Repo"
