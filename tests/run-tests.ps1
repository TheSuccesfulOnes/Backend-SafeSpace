param(
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr',
    [switch]$Format,
    [switch]$RefreshInventory
)
$ErrorActionPreference = 'Stop'
$taskRepoRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Set -JavaHome to a Java 21+ JDK.' }
$taskPreviousJavaHome = $env:JAVA_HOME
$taskPreviousLocation = Get-Location
try {
    $env:JAVA_HOME = $JavaHome
    Set-Location -LiteralPath $taskRepoRoot
    if ($Format) {
        $taskJavaFiles = @(@(git diff --name-only --diff-filter=ACMR) + @(git ls-files --others --exclude-standard) |
            Where-Object { $_ -like '*.java' } | Sort-Object -Unique)
        if ($taskJavaFiles.Count -eq 0) { throw 'No changed Java files to format; run without -Format.' }
        # Spotless 2.44 matches regular expressions against absolute file paths.
        $taskAbsolutePatterns = @($taskJavaFiles | ForEach-Object {
            [regex]::Escape([System.IO.Path]::GetFullPath((Join-Path $taskRepoRoot $_)))
        })
        $taskSpotlessSelection = '-DspotlessFiles=' + ($taskAbsolutePatterns -join ',')
        & mvn $taskSpotlessSelection spotless:apply test
    } else {
        & mvn test
    }
    if ($LASTEXITCODE -ne 0) { throw "Tests failed (exit $LASTEXITCODE)." }
    & mvn spotless:check
    if ($LASTEXITCODE -ne 0) { throw "Formatting check failed (exit $LASTEXITCODE)." }
    & mvn '-DskipTests' package
    if ($LASTEXITCODE -ne 0) { throw "Package build failed (exit $LASTEXITCODE)." }
    & (Join-Path $PSScriptRoot 'verify-inventory.ps1') -Refresh:$RefreshInventory
} finally {
    $env:JAVA_HOME = $taskPreviousJavaHome
    Set-Location -LiteralPath $taskPreviousLocation.Path
}
