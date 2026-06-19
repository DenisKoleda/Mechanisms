$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path "$PSScriptRoot\.."
$serverPlugins = "D:\Documents\Minecraft\server-dev\plugins"
$jar = Join-Path $projectRoot "build\libs\Mechanisms-0.1.0.jar"

if (!(Test-Path $jar)) {
    throw "Jar not found: $jar. Run scripts\gradle-local.ps1 build first."
}

New-Item -ItemType Directory -Force -Path $serverPlugins | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $serverPlugins "Mechanisms.jar") -Force
Write-Host "Installed Mechanisms.jar to $serverPlugins"
