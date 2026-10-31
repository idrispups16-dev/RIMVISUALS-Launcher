param([Parameter(Mandatory=$true)][string]$Target,[Parameter(Mandatory=$true)][string]$Report)
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force -Path (Split-Path $Report) | Out-Null
$result=[ordered]@{schemaVersion=1;startedAt=(Get-Date).ToUniversalTime().ToString('o');status='unavailable';target=$Target;signatures=@();detections=@()}
$scanRoot=Join-Path $env:ProgramData ('RIMVISUALS-security-scan-'+[guid]::NewGuid())
try {
  $files=@(Get-ChildItem -LiteralPath $Target -File -Recurse)
  if(!$files.Count){throw 'No files available for scanning'}
  foreach($file in @($files | Where-Object Extension -in '.exe','.dll','.node')){
    $sig=Get-AuthenticodeSignature -LiteralPath $file.FullName
    $result.signatures+=@{file=$file.Name;sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant();status=[string]$sig.Status;subject=if($sig.SignerCertificate){$sig.SignerCertificate.Subject}else{$null};thumbprint=if($sig.SignerCertificate){$sig.SignerCertificate.Thumbprint}else{$null}}
  }
  Start-Service WinDefend -ErrorAction Stop
  try{Update-MpSignature -ErrorAction Stop}catch{$result.updateWarning=$_.Exception.Message}
  $status=Get-MpComputerStatus
  $result.engine=@{version=$status.AMEngineVersion;product=$status.AMProductVersion;signatureVersion=$status.AntivirusSignatureVersion;signatureUpdated=[string]$status.AntivirusSignatureLastUpdated;antivirusEnabled=$status.AntivirusEnabled}
  if(!$status.AMServiceEnabled -or !$status.AntivirusEnabled){throw 'Defender antivirus is not enabled'}
  if(((Get-Date)-$status.AntivirusSignatureLastUpdated).TotalDays -gt 3){throw 'Defender signatures are more than three days old'}
  New-Item -ItemType Directory -Path $scanRoot | Out-Null
  Copy-Item -LiteralPath $Target -Destination (Join-Path $scanRoot 'payload') -Recurse
  $preferences=Get-MpPreference
  foreach($excluded in @($preferences.ExclusionPath)){if($excluded -and ($scanRoot -like $excluded -or $scanRoot.StartsWith($excluded.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase))){throw 'Scan directory is excluded by runner policy'}}
  if(@($preferences.ExclusionExtension | Where-Object {$_ -in 'exe','.exe','*'}).Count){throw 'Executable extension excluded by runner policy'}
  $before=@(Get-MpThreatDetection | ForEach-Object DetectionID)
  $mp=Get-ChildItem "$env:ProgramData\Microsoft\Windows Defender\Platform\*\MpCmdRun.exe" | Sort-Object FullName -Descending | Select-Object -First 1
  if(!$mp){$mp=Get-Item "$env:ProgramFiles\Windows Defender\MpCmdRun.exe"}
  & $mp.FullName -Scan -ScanType 3 -File $scanRoot 2>&1 | Out-File ($Report+'.scan.log') -Encoding utf8
  $code=$LASTEXITCODE;$result.exitCode=$code
  $result.detections=@(Get-MpThreatDetection | Where-Object {$_.DetectionID -notin $before} | Select-Object ThreatID,DetectionID,InitialDetectionTime,ActionSuccess,Resources)
  if($result.detections.Count){$result.status='detected';throw 'Defender reported a detection; artifact publication blocked'}
  if($code -ne 0){throw "Defender scan did not complete cleanly ($code)"}
  # A successful scan cannot silently quarantine the expected files.
  foreach($file in $files){$relative=[IO.Path]::GetRelativePath((Resolve-Path $Target).Path,$file.FullName);$copy=Join-Path (Join-Path $scanRoot 'payload') $relative;if((Get-FileHash -LiteralPath $copy).Hash -ne (Get-FileHash -LiteralPath $file.FullName).Hash){throw 'Payload changed or disappeared during scan'}}
  $result.status='no-detection';$result.completedAt=(Get-Date).ToUniversalTime().ToString('o')
} catch {$result.error=$_.Exception.Message;throw} finally {
  $result | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $Report -Encoding utf8
  if(Test-Path -LiteralPath $scanRoot){Remove-Item -LiteralPath $scanRoot -Recurse -Force}
}
