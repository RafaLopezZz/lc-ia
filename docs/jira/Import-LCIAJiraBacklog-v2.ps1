param(
    [Parameter(Mandatory = $true)]
    [string]$CsvPath,

    [string]$ProjectKey = "LCIA",

    [switch]$Execute
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
    throw "[LC-IA Jira Import] $Message"
}

function Get-RequiredEnv([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        Fail "Falta la variable de entorno $Name."
    }
    return $value
}

$BaseUrl = (Get-RequiredEnv "JIRA_BASE_URL").TrimEnd("/")
$Email = Get-RequiredEnv "JIRA_EMAIL"
$ApiToken = Get-RequiredEnv "JIRA_API_TOKEN"

$authText = "$Email`:$ApiToken"
$authBytes = [System.Text.Encoding]::UTF8.GetBytes($authText)
$authBase64 = [Convert]::ToBase64String($authBytes)

$JiraHeaders = @{
    Authorization = "Basic $authBase64"
    Accept        = "application/json"
}

function Invoke-Jira {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        $Body = $null
    )

    $uri = "$BaseUrl$Path"
    try {
        if ($null -eq $Body) {
            return Invoke-RestMethod -Method $Method -Uri $uri -Headers $JiraHeaders
        }

        $json = $Body | ConvertTo-Json -Depth 30
        return Invoke-RestMethod `
            -Method $Method `
            -Uri $uri `
            -Headers $JiraHeaders `
            -ContentType "application/json" `
            -Body ([System.Text.Encoding]::UTF8.GetBytes($json))
    }
    catch {
        $details = $_.Exception.Message
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $details += "`n" + $_.ErrorDetails.Message
        }
        Fail "$Method $Path falló.`n$details"
    }
}

function ConvertTo-Adf([string]$Text) {
    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @{
            type = "doc"
            version = 1
            content = @(
                @{
                    type = "paragraph"
                    content = @()
                }
            )
        }
    }

    $paragraphs = @()
    $normalized = $Text -replace "`r`n", "`n" -replace "`r", "`n"

    foreach ($block in ($normalized -split "`n`n+")) {
        $trimmed = $block.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) { continue }

        $content = @()
        $lines = $trimmed -split "`n"
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($i -gt 0) {
                $content += @{ type = "hardBreak" }
            }
            if (-not [string]::IsNullOrEmpty($lines[$i])) {
                $content += @{
                    type = "text"
                    text = $lines[$i]
                }
            }
        }

        $paragraphs += @{
            type = "paragraph"
            content = $content
        }
    }

    return @{
        type = "doc"
        version = 1
        content = $paragraphs
    }
}

function Read-Utf8Csv([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        Fail "No existe el CSV: $Path"
    }

    $text = [System.IO.File]::ReadAllText(
        (Resolve-Path -LiteralPath $Path),
        [System.Text.UTF8Encoding]::new($false)
    )

    return @($text | ConvertFrom-Csv)
}

function Get-ImportLabel([string]$SourceId) {
    return "lcia-import-id-$SourceId"
}

function Find-ExistingImportedIssue([string]$SourceId) {
    $label = Get-ImportLabel $SourceId
    $jql = "project = $ProjectKey AND labels = `"$label`""

    $body = @{
        jql = $jql
        maxResults = 2
        fields = @("summary", "status", "issuetype", "parent")
    }

    $result = Invoke-Jira -Method "POST" -Path "/rest/api/3/search/jql" -Body $body
    $issues = @($result.issues)

    if ($issues.Count -gt 1) {
        Fail "Hay más de un ticket con la etiqueta única $label. Revisión manual obligatoria."
    }

    if ($issues.Count -eq 1) {
        return $issues[0]
    }

    return $null
}

# ---------- PRE-FLIGHT DEL CSV ----------

$Rows = Read-Utf8Csv $CsvPath
if ($Rows.Count -eq 0) {
    Fail "El CSV está vacío."
}

$requiredColumns = @(
    "Summary",
    "Work Type",
    "Work item ID",
    "Parent",
    "Description",
    "Priority",
    "Labels"
)

$CsvColumnNames = @($Rows[0].PSObject.Properties.Name)
foreach ($column in $requiredColumns) {
    if ($column -notin $CsvColumnNames) {
        Fail "Falta la columna requerida '$column'."
    }
}

$duplicateIds = $Rows |
    Group-Object "Work item ID" |
    Where-Object Count -gt 1

if ($duplicateIds) {
    Fail "Hay Work item ID duplicados: $($duplicateIds.Name -join ', ')"
}

$Epics = @($Rows | Where-Object { $_."Work Type" -eq "Epic" })
$Children = @($Rows | Where-Object { $_."Work Type" -in @("Story", "Task") })

if ($Epics.Count -eq 0) {
    Fail "El CSV no contiene ningún Epic."
}

$epicSourceIds = @{}
foreach ($epic in $Epics) {
    if (-not [string]::IsNullOrWhiteSpace($epic.Parent)) {
        Fail "El Epic '$($epic.Summary)' no debe tener Parent."
    }
    $epicSourceIds[$epic."Work item ID"] = $true
}

foreach ($child in $Children) {
    if ([string]::IsNullOrWhiteSpace($child.Parent)) {
        Fail "'$($child.Summary)' necesita Parent."
    }
    if (-not $epicSourceIds.ContainsKey($child.Parent)) {
        Fail "'$($child.Summary)' apunta a Parent '$($child.Parent)', que no es un Epic del CSV."
    }
}

$unsupported = @($Rows | Where-Object { $_."Work Type" -notin @("Epic", "Story", "Task") })
if ($unsupported.Count -gt 0) {
    Fail "Tipos no soportados: $((@($unsupported | ForEach-Object { $_.'Work Type' } | Select-Object -Unique)) -join ', ')"
}

# ---------- PRE-FLIGHT CONTRA JIRA ----------

Write-Host "`n=== PRE-FLIGHT JIRA ==="

$project = Invoke-Jira -Method "GET" -Path "/rest/api/3/project/$ProjectKey"
Write-Host "Proyecto: $($project.key) — $($project.name) (id $($project.id))"

$issueTypesRaw = Invoke-Jira -Method "GET" -Path "/rest/api/3/issuetype/project?projectId=$($project.id)"
$issueTypes = @($issueTypesRaw)

$typeByName = @{}
foreach ($type in $issueTypes) {
    $typeByName[$type.name.ToLowerInvariant()] = $type
}

foreach ($requiredType in @("Epic", "Story", "Task")) {
    if (-not $typeByName.ContainsKey($requiredType.ToLowerInvariant())) {
        Fail "El proyecto no tiene disponible el tipo '$requiredType'."
    }

    $t = $typeByName[$requiredType.ToLowerInvariant()]
    Write-Host ("Tipo {0}: id={1}, hierarchyLevel={2}" -f $t.name, $t.id, $t.hierarchyLevel)
}

$projectStatuses = @(Invoke-Jira -Method "GET" -Path "/rest/api/3/project/$ProjectKey/statuses")
foreach ($requiredType in @("Epic", "Story", "Task")) {
    $statusGroup = $projectStatuses | Where-Object { $_.name -eq $requiredType } | Select-Object -First 1
    if ($null -eq $statusGroup) {
        Fail "No se pudo leer el workflow/statuses para '$requiredType'."
    }

    $backlog = @($statusGroup.statuses | Where-Object { $_.name -eq "Backlog" })
    if ($backlog.Count -eq 0) {
        Fail "'$requiredType' no tiene el estado Backlog en su workflow."
    }
}
Write-Host "Workflow: Epic/Story/Task contienen Backlog."

# Qué campos admite el endpoint de creación para cada tipo
$createFieldsByType = @{}
foreach ($requiredType in @("Epic", "Story", "Task")) {
    $type = $typeByName[$requiredType.ToLowerInvariant()]
    $meta = Invoke-Jira -Method "GET" -Path "/rest/api/3/issue/createmeta/$ProjectKey/issuetypes/$($type.id)"
    $fieldIds = @($meta.fields | ForEach-Object { $_.fieldId })
    $createFieldsByType[$requiredType] = $fieldIds

    foreach ($mandatory in @("summary", "labels")) {
        if ($mandatory -notin $fieldIds) {
            Fail "El campo '$mandatory' no está disponible al crear '$requiredType'. Es obligatorio para una importación segura e idempotente."
        }
    }
}

Write-Host ("CSV: {0} elementos = {1} Epic + {2} hijos." -f $Rows.Count, $Epics.Count, $Children.Count)

if (-not $Execute) {
    Write-Host "`nDRY RUN correcto. No se ha creado ningún ticket." -ForegroundColor Yellow
    Write-Host "Para ejecutar de verdad, repite el comando añadiendo -Execute."
    exit 0
}

# ---------- EJECUCIÓN ----------

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ledgerPath = Join-Path (Split-Path -Parent (Resolve-Path $CsvPath)) "jira-import-ledger-$timestamp.csv"
$ledger = @()
$sourceToJira = @{}

function Save-Ledger {
    $ledger |
        Export-Csv -LiteralPath $ledgerPath -NoTypeInformation -Encoding UTF8
}

function Add-LedgerEntry(
    [string]$SourceId,
    [string]$Type,
    [string]$Summary,
    [string]$Action,
    [string]$JiraId,
    [string]$JiraKey,
    [string]$Status,
    [string]$ErrorMessage
) {
    $script:ledger += [pscustomobject]@{
        SourceId = $SourceId
        Type = $Type
        Summary = $Summary
        Action = $Action
        JiraId = $JiraId
        JiraKey = $JiraKey
        Status = $Status
        Error = $ErrorMessage
    }
    Save-Ledger
}

function Create-One($Row) {
    $sourceId = $Row."Work item ID"
    $typeName = $Row."Work Type"
    $uniqueLabel = Get-ImportLabel $sourceId

    Write-Host "`n[$typeName/$sourceId] $($Row.Summary)"

    $existing = Find-ExistingImportedIssue $sourceId
    if ($null -ne $existing) {
        Write-Host "  SKIP: ya existe $($existing.key)" -ForegroundColor DarkYellow
        $script:sourceToJira[$sourceId] = @{
            id = $existing.id
            key = $existing.key
        }
        Add-LedgerEntry $sourceId $typeName $Row.Summary "SKIPPED_EXISTING" $existing.id $existing.key $existing.fields.status.name ""
        return
    }

    $type = $typeByName[$typeName.ToLowerInvariant()]
    $supportedFields = @($createFieldsByType[$typeName])

    $labels = @()
    if (-not [string]::IsNullOrWhiteSpace($Row.Labels)) {
        $labels += @($Row.Labels -split "[,;]" | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    }
    $labels += $uniqueLabel
    $labels += "lcia-backlog-import-v1"
    $labels = @($labels | Select-Object -Unique)

    $fields = @{
        project = @{ key = $ProjectKey }
        issuetype = @{ id = $type.id }
        summary = $Row.Summary
    }

    if ("description" -in $supportedFields) {
        $fields.description = ConvertTo-Adf $Row.Description
    }

    if ("labels" -in $supportedFields) {
        $fields.labels = $labels
    }

    if (
        "priority" -in $supportedFields -and
        -not [string]::IsNullOrWhiteSpace($Row.Priority)
    ) {
        $fields.priority = @{ name = $Row.Priority }
    }

    $parentData = $null
    if ($typeName -ne "Epic") {
        if (-not $script:sourceToJira.ContainsKey($Row.Parent)) {
            $parentExisting = Find-ExistingImportedIssue $Row.Parent
            if ($null -eq $parentExisting) {
                Fail "No existe el Epic padre de sourceId=$sourceId (Parent=$($Row.Parent))."
            }
            $script:sourceToJira[$Row.Parent] = @{
                id = $parentExisting.id
                key = $parentExisting.key
            }
        }

        $parentData = $script:sourceToJira[$Row.Parent]

        if ("parent" -in $supportedFields) {
            $fields.parent = @{ id = $parentData.id }
        }
    }

    $created = Invoke-Jira -Method "POST" -Path "/rest/api/3/issue" -Body @{ fields = $fields }

    # Si Parent no estaba disponible en create metadata, asociarlo después.
    if ($typeName -ne "Epic" -and "parent" -notin $supportedFields) {
        Invoke-Jira -Method "PUT" -Path "/rest/api/3/issue/$($created.key)" -Body @{
            fields = @{
                parent = @{ id = $parentData.id }
            }
        } | Out-Null
    }

    $verify = Invoke-Jira -Method "GET" -Path "/rest/api/3/issue/$($created.key)?fields=summary,status,issuetype,parent,labels"

    if ($verify.fields.status.name -ne "Backlog") {
        Add-LedgerEntry $sourceId $typeName $Row.Summary "CREATED_WRONG_STATUS" $created.id $created.key $verify.fields.status.name "Esperado Backlog"
        Fail "$($created.key) se creó en '$($verify.fields.status.name)' y no en Backlog. Importación detenida inmediatamente."
    }

    if ($typeName -ne "Epic") {
        if ($null -eq $verify.fields.parent) {
            Add-LedgerEntry $sourceId $typeName $Row.Summary "CREATED_MISSING_PARENT" $created.id $created.key $verify.fields.status.name "Parent ausente"
            Fail "$($created.key) se creó sin Parent. Importación detenida inmediatamente."
        }

        if ($verify.fields.parent.id -ne $parentData.id) {
            Add-LedgerEntry $sourceId $typeName $Row.Summary "CREATED_WRONG_PARENT" $created.id $created.key $verify.fields.status.name "Parent incorrecto"
            Fail "$($created.key) tiene Parent '$($verify.fields.parent.id)' pero se esperaba '$($parentData.id)'."
        }
    }

    $script:sourceToJira[$sourceId] = @{
        id = $created.id
        key = $created.key
    }

    Add-LedgerEntry $sourceId $typeName $Row.Summary "CREATED" $created.id $created.key $verify.fields.status.name ""
    Write-Host "  OK: $($created.key) → Backlog" -ForegroundColor Green
}

try {
    # Padres primero.
    foreach ($row in $Epics) {
        Create-One $row
    }

    # Después los hijos.
    foreach ($row in $Children) {
        Create-One $row
    }
}
catch {
    Write-Host "`nIMPORTACIÓN DETENIDA." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Ledger parcial: $ledgerPath"
    throw
}

Write-Host "`nIMPORTACIÓN COMPLETADA." -ForegroundColor Green
Write-Host "Creados/recuperados: $($sourceToJira.Count)"
Write-Host "Ledger: $ledgerPath"
