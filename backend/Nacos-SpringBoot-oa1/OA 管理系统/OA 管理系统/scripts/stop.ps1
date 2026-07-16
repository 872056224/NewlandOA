# ============================================================
#  OA 管理系统 一键停止
#  只停止由 start.ps1 启动并登记的进程（不会动系统级 MySQL/Redis 服务）
# ============================================================
$ErrorActionPreference = 'SilentlyContinue'
$Global:ProjectRoot = Split-Path $PSScriptRoot -Parent

. (Join-Path $PSScriptRoot 'common.ps1')
. (Join-Path $PSScriptRoot 'config.ps1')
Initialize-Console

$Global:RunDir = Join-Path $Global:ToolsDir 'run'

Write-Step '停止 OA 管理系统'

$pids = Get-ManagedPids
$names = @($pids.PSObject.Properties | ForEach-Object { $_.Name })
if ($names.Count -eq 0) {
    Write-Info '没有登记在册的进程（可能从未启动或已停止）'
} else {
    foreach ($n in $names) {
        $procId = [int]$pids.$n
        $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($p) {
            Stop-ProcessTree $procId
            Write-Ok "已停止 $n (PID $procId)"
        } else {
            Write-Info "$n (PID $procId) 已不在运行"
        }
    }
    Remove-Item (Get-PidFile) -Force -ErrorAction SilentlyContinue
}

# 兜底：按命令行特征清理可能漏网的本项目 java 服务
$jarPattern = 'gateway-1\.0\.0\.jar|oa-emp-service-1\.0\.0\.jar|oa-admin-service-1\.0\.0\.jar|oa-ai-service-1\.0\.0\.jar'
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
    $_.CommandLine -match $jarPattern
} | ForEach-Object {
    Stop-ProcessTree $_.ProcessId
    Write-Ok "已清理残留服务进程 (PID $($_.ProcessId))"
}

Write-Host ''
Write-Ok '停止完成。MySQL / 系统 Redis 服务保持运行；Nacos、ES、向量Redis、Ollama 若由本工具启动也已一并停止。'
