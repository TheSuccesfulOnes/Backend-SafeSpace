param([switch]$Refresh)
$ErrorActionPreference = 'Stop'
$taskRepoRoot = Split-Path -Parent $PSScriptRoot
$taskManifestPath = Join-Path $PSScriptRoot 'validation-sources.json'
$taskInventoryPath = Join-Path $taskRepoRoot 'src/test/resources/validation-inventory.json'
$taskManifest = Get-Content -LiteralPath $taskManifestPath -Raw | ConvertFrom-Json
$taskReports = @{}
$taskReportDirectory = Join-Path $taskRepoRoot 'target/surefire-reports'
foreach ($taskReportFile in Get-ChildItem -LiteralPath $taskReportDirectory -Filter 'TEST-*.xml') {
    [xml]$taskXml = Get-Content -LiteralPath $taskReportFile.FullName -Raw
    $taskSuite = $taskXml.testsuite
    $taskReports[$taskSuite.name] = $taskSuite
}
function Get-CaseType([string]$taskSuiteName, [string]$taskCaseName) {
    if ($taskSuiteName -eq 'com.experimentos.backend.shared.infrastructure.firebase.repositories.CompositeIdRepositoryTest') { return 'integration' }
    if ($taskCaseName -match '(?i)(^integration|integration:|bindsExternalConfiguration|bindsRecoverySettings|corsFilterEnforces)') { return 'integration' }
    return 'unit'
}
function Read-Suite([string]$taskSuiteName) {
    if (-not $taskReports.ContainsKey($taskSuiteName)) { throw "Missing executed suite: $taskSuiteName. Run the complete test command first." }
    $taskSuite = $taskReports[$taskSuiteName]
    $taskCases = @($taskSuite.testcase | ForEach-Object {
        $taskOutcome = 'passed'
        if ($null -ne $_.failure) { $taskOutcome = 'failed' }
        if ($null -ne $_.error) { $taskOutcome = 'error' }
        if ($null -ne $_.skipped) { $taskOutcome = 'skipped' }
        [pscustomobject][ordered]@{ name = [string]$_.name; type = Get-CaseType $taskSuiteName $_.name; result = $taskOutcome }
    })
    $taskExecuted = @($taskCases | Where-Object result -ne 'skipped')
    [pscustomobject][ordered]@{
        className = $taskSuiteName
        testSource = 'src/test/java/' + $taskSuiteName.Replace('.', '/') + '.java'
        caseCount = $taskExecuted.Count
        unitCases = @($taskExecuted | Where-Object type -eq 'unit').Count
        integrationCases = @($taskExecuted | Where-Object type -eq 'integration').Count
        failures = [int]$taskSuite.failures
        errors = [int]$taskSuite.errors
        skipped = [int]$taskSuite.skipped
        cases = $taskCases
    }
}
$taskOwners = @($taskManifest.owners | ForEach-Object {
    $taskOwner = $_
    if (-not (Test-Path -LiteralPath (Join-Path $taskRepoRoot $taskOwner.source))) { throw "Missing source: $($taskOwner.source)" }
    $taskSuites = @($taskOwner.suites | ForEach-Object { Read-Suite $_.className })
    $taskCount = [int](($taskSuites | Measure-Object caseCount -Sum).Sum)
    $taskUnits = [int](($taskSuites | Measure-Object unitCases -Sum).Sum)
    $taskIntegrations = [int](($taskSuites | Measure-Object integrationCases -Sum).Sum)
    if ($taskCount -lt $taskOwner.minimumCases) { throw "$($taskOwner.source) has $taskCount executed cases; needs $($taskOwner.minimumCases)." }
    [pscustomobject][ordered]@{ source = $taskOwner.source; caseCount = $taskCount; unitCases = $taskUnits; integrationCases = $taskIntegrations; suites = $taskSuites }
})
$taskSupplementary = @($taskManifest.supplementarySuites | ForEach-Object { Read-Suite $_.className })
$taskAllMapped = @($taskOwners | ForEach-Object { $_.suites } | ForEach-Object { $_.className }) + @($taskSupplementary | ForEach-Object { $_.className })
$taskUnmapped = @($taskReports.Keys | Where-Object { $_ -notin $taskAllMapped })
if ($taskUnmapped.Count -gt 0) { throw "Unmapped executed suites: $($taskUnmapped -join ', ')" }
$taskUniqueSuites = @($taskReports.Values)
$taskFailures = [int](($taskUniqueSuites | ForEach-Object { [int]$_.failures } | Measure-Object -Sum).Sum)
$taskErrors = [int](($taskUniqueSuites | ForEach-Object { [int]$_.errors } | Measure-Object -Sum).Sum)
$taskSkipped = [int](($taskUniqueSuites | ForEach-Object { [int]$_.skipped } | Measure-Object -Sum).Sum)
$taskTotal = [int](($taskUniqueSuites | ForEach-Object { [int]$_.tests } | Measure-Object -Sum).Sum)
if ($taskFailures -or $taskErrors -or $taskSkipped) { throw "Run is not green: $taskFailures failures, $taskErrors errors, $taskSkipped skipped." }
$taskInventory = [ordered]@{
    schemaVersion = 1
    command = 'pwsh -NoProfile -File tests/run-tests.ps1'
    countingRule = 'Each individual Surefire testcase is one case. Parameterized and dynamic invocations count individually. Assertion counts and code coverage percentages are not used.'
    ownerCount = $taskOwners.Count
    executedSuites = $taskUniqueSuites.Count
    executedCases = $taskTotal
    failures = $taskFailures
    errors = $taskErrors
    skipped = $taskSkipped
    owners = $taskOwners
    supplementarySuites = $taskSupplementary
    excludedCandidates = $taskManifest.excludedCandidates
}
$taskJson = ($taskInventory | ConvertTo-Json -Depth 30) + [Environment]::NewLine
if ($Refresh) {
    [System.IO.Directory]::CreateDirectory((Split-Path -Parent $taskInventoryPath)) | Out-Null
    [System.IO.File]::WriteAllText($taskInventoryPath, $taskJson, [System.Text.UTF8Encoding]::new($false))
} else {
    if (-not (Test-Path -LiteralPath $taskInventoryPath)) { throw 'Missing committed validation inventory; rerun with -RefreshInventory after reviewing all case changes.' }
    $taskSaved = Get-Content -LiteralPath $taskInventoryPath -Raw | ConvertFrom-Json
    $taskSavedJson = ($taskSaved | ConvertTo-Json -Depth 30) + [Environment]::NewLine
    if ($taskSavedJson -cne $taskJson) { throw 'Validation inventory differs from executed XML. Review the change and rerun with -RefreshInventory.' }
}
Write-Output "$($taskOwners.Count) validation owners; $taskTotal executed cases in $($taskUniqueSuites.Count) suites; zero failures/errors/skips. Inventory synchronized."
