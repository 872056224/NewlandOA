# ============================================================
#  OA 管理系统 一键启动 - 公共工具函数
# ============================================================

# ---------- 控制台编码 ----------
function Initialize-Console {
    try {
        [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
        $Global:OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    } catch { }
    # 老系统默认 TLS1.0 会导致 https 下载失败
    try {
        [Net.ServicePointManager]::SecurityProtocol = `
            [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls13
    } catch {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    }
}

# ---------- 输出 ----------
function Write-Step([string]$msg)  { Write-Host "`n==== $msg ====" -ForegroundColor Cyan }
function Write-Ok([string]$msg)    { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Info([string]$msg)  { Write-Host "  $msg" -ForegroundColor Gray }
function Write-Warn2([string]$msg) { Write-Host "  [警告] $msg" -ForegroundColor Yellow }
function Write-Fail([string]$msg)  { Write-Host "  [失败] $msg" -ForegroundColor Red }

# ---------- 端口 ----------
function Test-PortListening([int]$Port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Wait-PortListening([int]$Port, [string]$Name, [int]$TimeoutSec = 60) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        if (Test-PortListening $Port) {
            Write-Ok "$Name 已就绪 (端口 $Port, 耗时 $([int]$sw.Elapsed.TotalSeconds)s)"
            return $true
        }
        Start-Sleep -Milliseconds 1500
    }
    Write-Fail "$Name 在 ${TimeoutSec}s 内未监听端口 $Port"
    return $false
}

# ---------- 下载 / 解压 ----------
function Invoke-Download {
    param([string[]]$Urls, [string]$OutFile, [string]$Label)
    if (Test-Path $OutFile) {
        $sizeMb = [math]::Round((Get-Item $OutFile).Length / 1MB, 1)
        if ($sizeMb -gt 0.5) { Write-Info "$Label 安装包已存在 (${sizeMb}MB)，跳过下载"; return }
        Remove-Item $OutFile -Force
    }
    $dir = Split-Path $OutFile -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    foreach ($url in $Urls) {
        try {
            Write-Info "下载 $Label ：$url"
            $old = $ProgressPreference; $ProgressPreference = 'SilentlyContinue'
            try {
                Invoke-WebRequest -Uri $url -OutFile "$OutFile.part" -UseBasicParsing -TimeoutSec 1800
            } finally { $ProgressPreference = $old }
            Move-Item "$OutFile.part" $OutFile -Force
            $sizeMb = [math]::Round((Get-Item $OutFile).Length / 1MB, 1)
            Write-Ok "$Label 下载完成 (${sizeMb}MB)"
            return
        } catch {
            Write-Warn2 "该地址下载失败：$($_.Exception.Message)"
            Remove-Item "$OutFile.part" -Force -ErrorAction SilentlyContinue
        }
    }
    throw "[$Label] 所有下载地址均失败，请检查网络后重试"
}

function Expand-ZipTo([string]$Zip, [string]$DestDir) {
    if (-not (Test-Path $DestDir)) { New-Item -ItemType Directory -Path $DestDir -Force | Out-Null }
    $old = $ProgressPreference; $ProgressPreference = 'SilentlyContinue'
    try { Expand-Archive -LiteralPath $Zip -DestinationPath $DestDir -Force }
    finally { $ProgressPreference = $old }
}

# 取 GitHub 资源的全部候选地址（镜像加速 + 直连）
function Get-GithubUrls([string]$Path) {
    $urls = @()
    foreach ($m in $Global:GithubMirrors) {
        $urls += ('{0}https://github.com/{1}' -f $m, $Path)
    }
    return $urls
}

# ---------- 进程管理 ----------
function Get-PidFile { Join-Path $Global:RunDir 'pids.json' }

function Get-ManagedPids {
    $f = Get-PidFile
    if (Test-Path $f) {
        try { return (Get-Content $f -Raw -Encoding UTF8 | ConvertFrom-Json) } catch { }
    }
    return New-Object psobject
}

function Register-ManagedPid([string]$Name, [int]$ProcessId) {
    $obj = Get-ManagedPids
    if ($obj.PSObject.Properties[$Name]) { $obj.$Name = $ProcessId }
    else { $obj | Add-Member -NotePropertyName $Name -NotePropertyValue $ProcessId }
    $obj | ConvertTo-Json | Set-Content (Get-PidFile) -Encoding UTF8
}

# 生成一个 .cmd 启动器并以最小化窗口启动（进程独立于本控制台，关闭启动器窗口不影响服务）
# 注意：cmd 内容只允许 ASCII，路径一律使用相对路径或纯英文绝对路径
function Start-ServiceCmd {
    param(
        [string]$Name,          # 服务名（用于 PID 记录与日志名）
        [string]$CmdContent,    # cmd 脚本内容（不含日志重定向）
        [string]$WorkDir,       # 工作目录
        [string]$LogFile        # 日志文件（绝对路径，由调用方保证可写）
    )
    $cmdPath = Join-Path $Global:RunDir "run-$Name.cmd"
    $log = $LogFile.Replace('/', '\')
    # cmd 内容必须 100% ASCII（中文路径在批处理里会因代码页问题导致整行失败），
    # 日志统一写到纯英文的工具目录，工作目录里的中文路径由 .NET 层处理
    @(
        '@echo off',
        "$CmdContent >> `"$log`" 2>&1"
    ) | Set-Content -LiteralPath $cmdPath -Encoding ASCII
    # PS5.1 的 ArgumentList 不会自动加引号，必须手动引起来，否则路径含空格时整条命令被截断
    $p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/d', '/c', "`"$cmdPath`"" `
        -WorkingDirectory $WorkDir -WindowStyle Hidden -PassThru
    Register-ManagedPid $Name $p.Id
    Write-Info "$Name 已启动 (PID $($p.Id))，日志：$LogFile"
    return $p
}

function Stop-ProcessTree([int]$ProcessId) {
    & taskkill /PID $ProcessId /T /F 2>$null | Out-Null
}

# ---------- MySQL ----------
function Invoke-MySql {
    param([string]$Sql, [string]$Db = '')
    $argList = @("-u$($Global:MySqlUser)", '--default-character-set=utf8mb4', '-N', '-B')
    if ($Global:MySqlPwd -ne '') { $argList += "-p$($Global:MySqlPwd)" }
    if ($Db) { $argList += $Db }
    $argList += @('-e', $Sql)
    # mysql 的警告走 stderr，EAP=Stop 时 2>&1 会抛异常，局部放宽
    $eap = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    try { return (& $Global:MySqlExe @argList 2>&1 | ForEach-Object { "$_" }) }
    finally { $ErrorActionPreference = $eap }
}

function Invoke-MySqlScript {
    param([string]$ScriptPath, [string]$Db)
    $argList = @("-u$($Global:MySqlUser)", '--default-character-set=utf8mb4')
    if ($Global:MySqlPwd -ne '') { $argList += "-p$($Global:MySqlPwd)" }
    $argList += $Db
    Get-Content -LiteralPath $ScriptPath -Raw -Encoding UTF8 | & $Global:MySqlExe @argList
    if ($LASTEXITCODE -ne 0) { throw "导入 SQL 失败：$ScriptPath" }
}
