# Run once on your own machine. The token is never echoed or written to a repo file.
$ErrorActionPreference = 'Stop'
$secret = Read-Host 'CurseForge upload API token' -AsSecureString
$credential = [Net.NetworkCredential]::new('', $secret)
if([string]::IsNullOrWhiteSpace($credential.Password)) { throw 'Token is empty' }
[Environment]::SetEnvironmentVariable('CURSEFORGE_API_TOKEN', $credential.Password, 'User')
$env:CURSEFORGE_API_TOKEN = $credential.Password
Write-Output 'Saved CURSEFORGE_API_TOKEN in the current Windows user environment.'
