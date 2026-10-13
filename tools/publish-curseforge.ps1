param(
    [string]$Config,
    [string]$WorkspaceRoot,
    [string[]]$Projects,
    [ValidateSet('both','neoforge','forge')][string]$Platform = 'both',
    [ValidateSet('alpha','beta','release')][string]$ReleaseType,
    [string]$NeoJavaHome,
    [string]$ForgeJavaHome,
    [string]$Plan,
    [switch]$Build,
    [switch]$Offline,
    [switch]$Upload,
    [switch]$ManualRelease,
    [switch]$Interactive
)
$ErrorActionPreference = 'Stop'
$env:PYTHONUTF8 = '1'
$arguments = @((Join-Path $PSScriptRoot 'curseforge_publish.py'), '--platform', $Platform)
foreach($pair in @(
    @('--config',$Config), @('--workspace-root',$WorkspaceRoot), @('--projects',($Projects -join ',')),
    @('--release-type',$ReleaseType), @('--java21-home',$NeoJavaHome),
    @('--java17-home',$ForgeJavaHome), @('--plan',$Plan)
)) { if($pair[1]) { $arguments += $pair } }
if($Build) { $arguments += '--build' }
if($Offline) { $arguments += '--offline' }
if($Upload) { $arguments += '--upload' }
if($ManualRelease) { $arguments += '--manual-release' }
if($Interactive) { $arguments += '--interactive' }
# Read the stored user-level token when this shell predates the environment setup.
if(-not $env:CURSEFORGE_API_TOKEN) {
    $env:CURSEFORGE_API_TOKEN = [Environment]::GetEnvironmentVariable('CURSEFORGE_API_TOKEN','User')
}
# Native stderr may contain ordinary build warnings in Windows PowerShell.
$previousErrorAction = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    & python @arguments
    $publishExit = $LASTEXITCODE
} finally { $ErrorActionPreference = $previousErrorAction }
exit $publishExit
