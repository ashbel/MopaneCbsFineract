#Requires -Version 3.0
<#
  Populates fineract-provider/pentaho-m2-local for Gradle Pentaho dependencies.
  Downloads from SourceForge + (optional) PRD CE zip for wizard/scripting JARs.

  Usage (from repo root):
    powershell -ExecutionPolicy Bypass -File fineract-provider/scripts/bootstrap-pentaho-m2.ps1

  Env: MOPANE_PRD_ZIP = full path to prd-ce-3.9.1-GA.zip (skips PRD download)
  Switch: -SkipPrdDownload — do not download PRD; error if wizard JARs still needed
#>
param(
    [switch]$SkipPrdDownload
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$M2Root = (New-Item -ItemType Directory -Force -Path (Join-Path $ScriptDir '..\pentaho-m2-local')).FullName
$TempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("mopane-pentaho-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $TempRoot | Out-Null

function Write-Pom($GroupId, $ArtifactId, $Version) {
    @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>$GroupId</groupId>
  <artifactId>$ArtifactId</artifactId>
  <version>$Version</version>
  <packaging>jar</packaging>
</project>
"@
}

function Publish-Jar($GroupId, $ArtifactId, $Version, $JarPath) {
    $seg = $GroupId -replace '\.', '/'
    $dir = Join-Path $M2Root "$seg/$ArtifactId/$Version"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $base = "$ArtifactId-$Version"
    Copy-Item -Force $JarPath (Join-Path $dir "$base.jar")
    Set-Content -Encoding UTF8 (Join-Path $dir "$base.pom") (Write-Pom $GroupId $ArtifactId $Version)
    Write-Host "  OK $GroupId`:$ArtifactId`:$Version"
}

function Download-File($Url, $Dest) {
    Write-Host "  GET $Url"
    curl.exe -sL --connect-timeout 120 --max-time 7200 -o $Dest $Url
    if (-not (Test-Path $Dest) -or (Get-Item $Dest).Length -lt 1000) {
        throw "Download failed or too small: $Url"
    }
}

function Expand-Zip($ZipPath, $DestDir) {
    [System.IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, $DestDir)
}

function Find-JarInTree($Root, [string]$NamePattern) {
    Get-ChildItem -Path $Root -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like $NamePattern } |
        Select-Object -First 1 -ExpandProperty FullName
}

function Extract-Entry($ZipPath, $Entry, $OutFile) {
    $z = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
    try {
        $es = $z.GetEntry($Entry.FullName)
        if (-not $es) { throw "Missing entry $($Entry.FullName)" }
        $dir = Split-Path -Parent $OutFile
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        $s = $es.Open()
        try {
            $fs = [System.IO.File]::Create($OutFile)
            try { $s.CopyTo($fs) } finally { $fs.Dispose() }
        } finally { $s.Dispose() }
    } finally { $z.Dispose() }
}

try {
    $sf = 'https://downloads.sourceforge.net/project/jfreereport'
    $libUrl = "$sf/02.%20Libraries/1.2.8-stable"

    Write-Host "`n== Pentaho classic core (3.9.1.1 JAR from GA bundle) =="
    $coreZip = Join-Path $TempRoot 'classic-core.zip'
    Download-File "$sf/01.%20Classic%20Engine/3.9.1-stable/pentaho-reporting-engine-classic-core-3.9.1-GA.zip" $coreZip
    $coreOut = Join-Path $TempRoot 'core'
    Expand-Zip $coreZip $coreOut
    $coreJar = Find-JarInTree $coreOut 'pentaho-reporting-engine-classic-core-3.9.1-GA.jar'
    if (-not $coreJar) { throw 'classic-core jar not found in zip' }
    Publish-Jar 'pentaho-reporting-engine' 'pentaho-reporting-engine-classic-core' '3.9.1.1' $coreJar

    Write-Host "`n== Pentaho classic extensions =="
    $extZip = Join-Path $TempRoot 'classic-ext.zip'
    Download-File "$sf/01.%20Classic%20Engine/3.9.1-stable/pentaho-reporting-engine-classic-extensions-3.9.1-GA.zip" $extZip
    $extOut = Join-Path $TempRoot 'ext'
    Expand-Zip $extZip $extOut
    $extJar = Find-JarInTree $extOut 'pentaho-reporting-engine-classic-extensions-3.9.1-GA.jar'
    if (-not $extJar) { throw 'classic-extensions jar not found' }
    Publish-Jar 'pentaho-reporting-engine' 'pentaho-reporting-engine-classic-extensions' '3.9.1-GA' $extJar

    Write-Host "`n== Pentaho libraries (1.2.8) =="
    $libs = @('libbase','libdocbundle','libfonts','libformat','libformula','libloader','librepository','libserializer','libsparkline','libxml')
    foreach ($lib in $libs) {
        $j = Join-Path $TempRoot "$lib.jar"
        Download-File "$libUrl/${lib}-1.2.8.jar" $j
        Publish-Jar 'pentaho-library' $lib '1.2.8' $j
    }

    $prdZip = $env:MOPANE_PRD_ZIP
    if (-not $prdZip -or -not (Test-Path $prdZip)) {
        $cacheDir = Join-Path $env:USERPROFILE '.cache\mopane-pentaho'
        $prdZip = Join-Path $cacheDir 'prd-ce-3.9.1-GA.zip'
    }

    if (-not (Test-Path $prdZip)) {
        if ($SkipPrdDownload) {
            throw @"
Missing wizard/scripting JARs. Either:
  Remove -SkipPrdDownload to download PRD CE (~178 MB), or
  Set MOPANE_PRD_ZIP to the full path of prd-ce-3.9.1-GA.zip
"@
        }
        Write-Host "`n== Downloading Report Designer CE (large, ~178 MB) for scripting + wizard JARs =="
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $prdZip) | Out-Null
        Download-File "$sf/04.%20Report%20Designer/3.9.1-stable/prd-ce-3.9.1-GA.zip" $prdZip
    } else {
        Write-Host "`n== Using PRD zip: $prdZip =="
    }

    $scriptNames = @(
        @{ n = 'pentaho-reporting-engine-classic-extensions-scripting-3.9.1-GA.jar'; g = 'pentaho-reporting-engine'; a = 'pentaho-reporting-engine-classic-extensions-scripting'; v = '3.9.1-GA' },
        @{ n = 'pentaho-reporting-engine-wizard-core-3.9.1-GA.jar'; g = 'pentaho-reporting-engine'; a = 'pentaho-reporting-engine-wizard-core'; v = '3.9.1-GA' },
        @{ n = 'pentaho-reporting-engine-wizard-xul-3.9.1-GA.jar'; g = 'pentaho-report-designer'; a = 'pentaho-reporting-engine-wizard-xul'; v = '3.9.1-GA' }
    )

    function Get-PrdJarEntry([string]$ArtifactHint) {
        $z = [System.IO.Compression.ZipFile]::OpenRead($prdZip)
        try {
            $rx = switch -Regex ($ArtifactHint) {
                'extensions-scripting' { '^pentaho-reporting-engine-classic-extensions-scripting.*\.jar$' }
                'wizard-core' { '^pentaho-reporting-engine-wizard-core.*\.jar$' }
                'wizard-xul' { '^pentaho-reporting-engine-wizard-xul.*\.jar$' }
                default { throw "Unknown artifact $ArtifactHint" }
            }
            return $z.Entries | Where-Object {
                $_.Name -match $rx -and $_.Name -notmatch 'sources|javadoc'
            } | Select-Object -First 1
        } finally { $z.Dispose() }
    }

    foreach ($item in $scriptNames) {
        $hint = if ($item.a -match 'scripting') { 'extensions-scripting' }
            elseif ($item.a -match 'wizard-core') { 'wizard-core' }
            else { 'wizard-xul' }
        $ent = Get-PrdJarEntry $hint
        if (-not $ent) {
            throw "Could not find $($item.n) inside PRD zip. Open prd-ce-3.9.1-GA.zip and locate the jar manually."
        }
        $out = Join-Path $TempRoot $item.n
        Extract-Entry $prdZip $ent $out
        Publish-Jar $item.g $item.a $item.v $out
    }

    Write-Host "`nDone. Local repo: $M2Root"
    Write-Host "Run: cd fineract-provider && ..\gradlew compileJava   (or your usual build)`n"
}
finally {
    Remove-Item -Recurse -Force $TempRoot -ErrorAction SilentlyContinue
}
