# ONLY strengthens protection in a disposable GitHub-hosted Windows CI machine.
# Never run or apply these environment changes on the user's computer.
$ErrorActionPreference='Stop'
if($env:GITHUB_ACTIONS -ne 'true' -or $env:RUNNER_ENVIRONMENT -ne 'github-hosted'){throw 'Refusing to change Defender outside a disposable GitHub-hosted runner'}
New-Item -ItemType Directory -Force security-output | Out-Null
$before=Get-MpPreference
$report=[ordered]@{before=@{paths=@($before.ExclusionPath);extensions=@($before.ExclusionExtension);disableArchiveScanning=$before.DisableArchiveScanning;disableRealtimeMonitoring=$before.DisableRealtimeMonitoring;disableScriptScanning=$before.DisableScriptScanning};purpose='Enable protection for an actual scan; no exclusions are added'}
try {
 Start-Service WinDefend
 if(@($before.ExclusionPath).Count){Remove-MpPreference -ExclusionPath $before.ExclusionPath}
 $extensions=@($before.ExclusionExtension | Where-Object {$_})
 if($extensions.Count){Remove-MpPreference -ExclusionExtension $extensions}
 Set-MpPreference -DisableArchiveScanning $false -DisableScriptScanning $false -DisableBehaviorMonitoring $false -DisableIOAVProtection $false -DisableRealtimeMonitoring $false -PUAProtection Enabled -ScanAvgCPULoadFactor 50
 $after=Get-MpPreference
 $report.after=@{paths=@($after.ExclusionPath);extensions=@($after.ExclusionExtension);disableArchiveScanning=$after.DisableArchiveScanning;disableRealtimeMonitoring=$after.DisableRealtimeMonitoring;disableScriptScanning=$after.DisableScriptScanning}
 if(@($after.ExclusionPath | Where-Object {$_}).Count -or @($after.ExclusionExtension | Where-Object {$_}).Count -or $after.DisableArchiveScanning -or $after.DisableScriptScanning -or $after.DisableRealtimeMonitoring){throw 'Runner protection could not be enabled; no clean verdict can be claimed'}
 $report.status='protection-enabled'
} catch {$report.status='unavailable';$report.error=$_.Exception.Message;throw} finally {$report | ConvertTo-Json -Depth 10 | Set-Content security-output/runner-protection.json -Encoding utf8}
