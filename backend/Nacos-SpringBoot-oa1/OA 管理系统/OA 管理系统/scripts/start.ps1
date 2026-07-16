# ============================================================
#  OA 管理系统 一键启动
#  环境配置(JDK/Maven/Nacos/Redis/ES/Ollama) + 模型下载 + 建库 + 构建 + 启动
#
#  用法：双击 start.bat，或：
#    powershell -File scripts\start.ps1 [-Rebuild] [-ReinitDb] [-SkipModels] [-SkipFrontend] [-CheckOnly]
# ============================================================
[CmdletBinding()]
param(
    [switch]$Rebuild,       # 强制重新打包后端
    [switch]$ReinitDb,      # 强制重新导入 day.sql / ai_kb.sql（会清空现有数据！）
    [switch]$SkipModels,    # 跳过 Ollama 模型下载
    [switch]$SkipFrontend,  # 不启动前端
    [switch]$CheckOnly      # 只检测环境，不下载、不启动
)

$ErrorActionPreference = 'Stop'
$Global:ProjectRoot = Split-Path $PSScriptRoot -Parent

. (Join-Path $PSScriptRoot 'common.ps1')
. (Join-Path $PSScriptRoot 'config.ps1')
Initialize-Console

# 生成的 cmd 启动器与日志必须放在纯英文路径：
# 含中文/空格的路径会让 cmd /c 解析与批处理重定向双双踩坑
$Global:RunDir = Join-Path $Global:ToolsDir 'run'
$LogDir = Join-Path $Global:ToolsDir 'logs'
$DownloadDir = Join-Path $Global:ToolsDir 'downloads'
foreach ($d in @($Global:RunDir, $Global:ToolsDir, $LogDir, $DownloadDir)) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
}
$projLogs = Join-Path $Global:ProjectRoot 'logs'
if (Test-Path $projLogs) {
    $item = Get-Item $projLogs -Force
    # 旧版真实目录且为空 -> 替换成联接
    if (-not ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        if (-not (Get-ChildItem $projLogs -Force -ErrorAction SilentlyContinue)) {
            Remove-Item $projLogs -Force
        }
    }
}
if (-not (Test-Path $projLogs)) {
    try { New-Item -ItemType Junction -Path $projLogs -Target $LogDir | Out-Null } catch { }
}

Write-Host ''
Write-Host '  ============================================' -ForegroundColor Magenta
Write-Host '       OA 管理系统 一键启动' -ForegroundColor Magenta
Write-Host '       前端 + 网关 + 微服务 + AI(本地大模型)' -ForegroundColor Magenta
Write-Host '  ============================================' -ForegroundColor Magenta
Write-Info "项目目录：$Global:ProjectRoot"
Write-Info "工具目录：$Global:ToolsDir"

# ============================================================
# 1. JDK 21
# ============================================================
function Get-JavaMajor([string]$JavaExe) {
    # java -version 输出在 stderr，EAP=Stop 时 2>&1 会把 stderr 变成异常，这里局部放宽
    $eap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = (& $JavaExe -version 2>&1 | ForEach-Object { "$_" }) -join ' '
        if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    } catch { } finally { $ErrorActionPreference = $eap }
    return 0
}

function Find-Jdk {
    $candidates = @()
    $candidates += Get-ChildItem -Path $Global:ToolsDir -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName }
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu")) {
        $candidates += Get-ChildItem -Path $base -Directory -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
    }
    foreach ($c in $candidates) {
        $java = Join-Path $c 'bin\java.exe'
        $javac = Join-Path $c 'bin\javac.exe'
        # 必须是完整 JDK（含 javac），编译 liuvei-common 兼容包要用
        if ((Test-Path $java) -and (Test-Path $javac) -and ((Get-JavaMajor $java) -ge $Global:JdkMajor)) { return $c }
    }
    return $null
}

function Install-Jdk {
    $zip = Join-Path $DownloadDir "jdk$($Global:JdkMajor).zip"
    $urls = @()
    # 清华 TUNA 镜像：解析目录列表拿到最新 zip
    try {
        $listing = (Invoke-WebRequest -Uri "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/$($Global:JdkMajor)/jdk/x64/windows/" -UseBasicParsing -TimeoutSec 30).Content
        $names = @([regex]::Matches($listing, "OpenJDK$($Global:JdkMajor)U-jdk_x64_windows_hotspot_[0-9][\w\.\-]*\.zip") |
            ForEach-Object { $_.Value } | Sort-Object -Unique)
        if ($names.Count -gt 0 -and $names[-1] -like 'OpenJDK*.zip') {
            $urls += "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/$($Global:JdkMajor)/jdk/x64/windows/$($names[-1])"
        }
    } catch { Write-Warn2 "TUNA 镜像列表获取失败，将使用官方源" }
    $urls += "https://api.adoptium.net/v3/binary/latest/$($Global:JdkMajor)/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
    Invoke-Download -Urls $urls -OutFile $zip -Label "JDK $($Global:JdkMajor) (约190MB)"
    $tmp = Join-Path $Global:ToolsDir '_jdk_tmp'
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-ZipTo $zip $tmp
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    $dest = Join-Path $Global:ToolsDir $inner.Name
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    Move-Item $inner.FullName $dest
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    return $dest
}

Write-Step "1/9 检查 JDK $($Global:JdkMajor)"
$JdkHome = Find-Jdk
if ($JdkHome) {
    Write-Ok "JDK $((Get-JavaMajor (Join-Path $JdkHome 'bin\java.exe'))) ：$JdkHome"
} elseif ($CheckOnly) {
    Write-Warn2 "未找到 JDK $($Global:JdkMajor)，正式启动时将自动下载便携版"
} else {
    Write-Info "未找到 JDK $($Global:JdkMajor)（项目编译目标为 21），开始下载便携版..."
    $JdkHome = Install-Jdk
    Write-Ok "JDK 安装完成：$JdkHome"
}
if ($JdkHome) {
    $env:JAVA_HOME = $JdkHome
    $env:Path = "$JdkHome\bin;$env:Path"
    $JavaExe = Join-Path $JdkHome 'bin\java.exe'
}

# ============================================================
# 2. Maven
# ============================================================
Write-Step '2/9 检查 Maven'
function Find-Maven {
    $inTools = Get-ChildItem -Path $Global:ToolsDir -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($inTools) { return (Join-Path $inTools.FullName 'bin\mvn.cmd') }
    $cmd = Get-Command mvn.cmd, mvn -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cmd) { return $cmd.Source }
    return $null
}

$MvnCmd = Find-Maven
if ($MvnCmd) {
    Write-Ok "Maven：$MvnCmd"
} elseif ($CheckOnly) {
    Write-Warn2 '未找到 Maven，正式启动时将自动下载便携版'
} else {
    Write-Info '未找到 Maven，开始下载便携版...'
    $v = $Global:MavenVersion
    $zip = Join-Path $DownloadDir "maven-$v.zip"
    Invoke-Download -OutFile $zip -Label "Maven $v (约9MB)" -Urls @(
        "https://repo.huaweicloud.com/apache/maven/maven-3/$v/binaries/apache-maven-$v-bin.zip",
        "https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/$v/binaries/apache-maven-$v-bin.zip",
        "https://archive.apache.org/dist/maven/maven-3/$v/binaries/apache-maven-$v-bin.zip"
    )
    Expand-ZipTo $zip $Global:ToolsDir
    $MvnCmd = Find-Maven
    if (-not $MvnCmd) { throw 'Maven 解压后未找到 mvn.cmd' }
    Write-Ok "Maven 安装完成：$MvnCmd"
}
$MavenSettings = Join-Path $PSScriptRoot 'maven-settings.xml'

# ============================================================
# 3. 私有依赖 liuvei-common（仓库中不存在，按源码用法自动补齐兼容实现）
# ============================================================
Write-Step '3/9 检查私有依赖 liuvei-common'
$LiuveiJar = Join-Path $env:USERPROFILE '.m2\repository\com\liuvei\common\liuvei-common\1.2.0\liuvei-common-1.2.0.jar'
if (Test-Path $LiuveiJar) {
    Write-Ok 'liuvei-common 1.2.0 已在本地仓库'
} elseif ($CheckOnly) {
    Write-Warn2 'liuvei-common 1.2.0 缺失，正式启动时将自动生成兼容包（项目仅使用 SysFun.md5）'
} else {
    Write-Info '本地仓库缺少 liuvei-common 1.2.0（公共仓库不存在该包），自动生成兼容实现...'
    $stubDir = Join-Path $Global:ToolsDir 'liuvei-stub'
    $srcDir = Join-Path $stubDir 'src\com\liuvei\common'
    $clsDir = Join-Path $stubDir 'classes'
    New-Item -ItemType Directory -Path $srcDir, $clsDir -Force | Out-Null
    # 注意：必须无 BOM，javac 会把 BOM 报为非法字符；源码保持纯 ASCII
    $sysFunSrc = @'
package com.liuvei.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Compatibility implementation of liuvei-common (not available in any public repo).
 * The project only uses SysFun.md5(String): 32-char lowercase hex MD5,
 * matching existing password hashes in DB (e.g. 202cb962ac59075b964b07152d234b70 = md5("123")).
 */
public final class SysFun {

    private SysFun() {
    }

    public static String md5(String input) {
        if (input == null) {
            input = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }
}
'@
    [IO.File]::WriteAllText((Join-Path $srcDir 'SysFun.java'), $sysFunSrc, (New-Object System.Text.UTF8Encoding($false)))
    $javac = Join-Path $JdkHome 'bin\javac.exe'
    $jarTool = Join-Path $JdkHome 'bin\jar.exe'
    & $javac --release 8 -encoding UTF-8 -d $clsDir (Join-Path $srcDir 'SysFun.java')
    if ($LASTEXITCODE -ne 0) { throw 'liuvei-common 兼容包编译失败' }
    $stubJar = Join-Path $stubDir 'liuvei-common-1.2.0.jar'
    & $jarTool cf $stubJar -C $clsDir .
    if ($LASTEXITCODE -ne 0) { throw 'liuvei-common 兼容包打包失败' }
    & $MvnCmd -q install:install-file "-Dfile=$stubJar" '-DgroupId=com.liuvei.common' `
        '-DartifactId=liuvei-common' '-Dversion=1.2.0' '-Dpackaging=jar' "-s" $MavenSettings
    if ($LASTEXITCODE -ne 0) { throw 'liuvei-common 安装到本地仓库失败' }
    Write-Ok 'liuvei-common 1.2.0 兼容包已安装到本地仓库'
}

# ============================================================
# 4. MySQL + 数据库初始化
# ============================================================
Write-Step '4/9 检查 MySQL 与数据库'
$Global:MySqlUser = 'root'

# 解析 MySQL 安装的 bin 目录（同时含客户端 mysql.exe 与服务端 mysqld.exe）
function Resolve-MySqlBin {
    # 1) 正在监听 3306 的进程所在目录（用户已自行启动的实例优先）
    $proc = Get-NetTCPConnection -State Listen -LocalPort $Global:MySqlPort -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($proc) {
        $p = Get-Process -Id $proc.OwningProcess -ErrorAction SilentlyContinue
        if ($p -and $p.Path) {
            $dir = Split-Path $p.Path -Parent
            if (Test-Path (Join-Path $dir 'mysql.exe')) { return $dir }
        }
    }
    # 2) XAMPP（自带数据目录，优先于独立 MySQL，避免脚本另起空库导致数据分裂）
    if (Test-Path 'C:\xampp\mysql\bin\mysql.exe') { return 'C:\xampp\mysql\bin' }
    # 3) 标准安装目录（多版本时取版本号最大的）
    $glob = Get-ChildItem "$env:ProgramFiles\MySQL\MySQL Server *\bin\mysql.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if ($glob) { return (Split-Path $glob.FullName -Parent) }
    $glob = Get-ChildItem "${env:ProgramFiles(x86)}\MySQL\MySQL Server *\bin\mysql.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($glob) { return (Split-Path $glob.FullName -Parent) }
    # 3) PATH
    $cmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($cmd) { return (Split-Path $cmd.Source -Parent) }
    return $null
}

$mysqlBin = Resolve-MySqlBin
if (-not $mysqlBin) {
    throw "未找到 MySQL（mysql.exe）。请先安装 MySQL 8.x（默认目录 $env:ProgramFiles\MySQL）后重试。"
}
$Global:MySqlExe = Join-Path $mysqlBin 'mysql.exe'
$mysqldExe = Join-Path $mysqlBin 'mysqld.exe'

if (-not (Test-PortListening $Global:MySqlPort)) {
    $svc = Get-Service -Name 'MySQL*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($svc) {
        Write-Info "MySQL 未运行，启动 Windows 服务 $($svc.Name) ..."
        if (-not $CheckOnly) {
            Start-Service $svc.Name
            if (-not (Wait-PortListening $Global:MySqlPort 'MySQL' 30)) { throw 'MySQL 服务启动超时' }
        }
    } elseif ($Global:MySqlAutoStart -and (Test-Path $mysqldExe)) {
        # 本机无 MySQL 服务：用 mysqld 在工具目录自带 data 上启动（首次自动初始化为 root 空密码）
        if (-not $CheckOnly) {
            if (-not (Test-Path (Join-Path $Global:MySqlDataDir 'ibdata1'))) {
                Write-Info "首次使用，初始化 MySQL 数据目录（root 空密码）：$($Global:MySqlDataDir)"
                if (Test-Path $Global:MySqlDataDir) { Remove-Item $Global:MySqlDataDir -Recurse -Force }
                New-Item -ItemType Directory -Path $Global:MySqlDataDir -Force | Out-Null
                & $mysqldExe --initialize-insecure "--datadir=$($Global:MySqlDataDir)" --character-set-server=utf8mb4
                if ($LASTEXITCODE -ne 0) { throw 'MySQL 数据目录初始化失败，详见 data 目录下 *.err 日志' }
                Write-Ok 'MySQL 数据目录初始化完成'
            }
            Write-Info '本机未注册 MySQL 服务，使用 mysqld 自动启动内置实例 ...'
            Start-ServiceCmd -Name 'mysql' `
                -CmdContent "`"$mysqldExe`" --datadir=`"$($Global:MySqlDataDir)`" --port=$($Global:MySqlPort) --character-set-server=utf8mb4" `
                -WorkDir $mysqlBin -LogFile (Join-Path $LogDir 'mysql.log') | Out-Null
            if (-not (Wait-PortListening $Global:MySqlPort 'MySQL' 40)) { throw 'MySQL 启动超时，详见 logs\mysql.log' }
        }
    } else {
        throw "未检测到 MySQL（端口 $($Global:MySqlPort) 无监听，且本机无 MySQL 服务）。请先安装 MySQL 8.x 后重试。"
    }
} else {
    Write-Info "MySQL 已在运行 (端口 $($Global:MySqlPort))"
}
Write-Ok "MySQL 客户端：$Global:MySqlExe"

# 探测可用密码（配置值优先）
$Global:MySqlPwd = $null
$pwCandidates = @($Global:MySqlPassword) + $Global:MySqlPasswordCandidates | Select-Object -Unique
foreach ($pw in $pwCandidates) {
    $Global:MySqlPwd = $pw
    $r = Invoke-MySql 'SELECT 1;'
    if ($LASTEXITCODE -eq 0) { break }
    $Global:MySqlPwd = $null
}
if ($null -eq $Global:MySqlPwd) {
    throw "MySQL root 密码探测失败，请编辑 scripts\config.ps1 中的 `$Global:MySqlPassword 后重试"
}
$pwShown = if ($Global:MySqlPwd -eq '') { '(空密码)' } else { '******' }
Write-Ok "MySQL 连接成功，root 密码：$pwShown"

if (-not $CheckOnly) {
    Invoke-MySql "CREATE DATABASE IF NOT EXISTS $($Global:MySqlDb) DEFAULT CHARACTER SET utf8mb4;" | Out-Null
    $hasAdmin = @(Invoke-MySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$($Global:MySqlDb)' AND table_name='admin';") | Select-Object -Last 1
    if ($ReinitDb -or ("$hasAdmin".Trim() -eq '0')) {
        Write-Info '导入主数据库 day.sql ...'
        Invoke-MySqlScript (Join-Path $Global:ProjectRoot 'day.sql') $Global:MySqlDb
        Write-Ok 'day.sql 导入完成'
    } else {
        Write-Ok "数据库 $($Global:MySqlDb) 已就绪（如需重置请用 -ReinitDb）"
    }
    $hasKb = @(Invoke-MySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$($Global:MySqlDb)' AND table_name='kb_doc';") | Select-Object -Last 1
    if ($ReinitDb -or ("$hasKb".Trim() -eq '0')) {
        Write-Info '导入 AI 知识库 ai_kb.sql ...'
        Invoke-MySqlScript (Join-Path $Global:ProjectRoot 'ai_kb.sql') $Global:MySqlDb
        Write-Ok 'ai_kb.sql 导入完成'
    } else {
        Write-Ok 'AI 知识库表 kb_doc 已存在'
    }
}

# ============================================================
# 5. Nacos 注册中心
# ============================================================
Write-Step '5/9 检查 Nacos'
if (Test-PortListening $Global:NacosPort) {
    Write-Ok "Nacos 已在运行 (端口 $($Global:NacosPort))"
} elseif ($CheckOnly) {
    Write-Warn2 'Nacos 未运行，正式启动时将自动下载并以单机模式启动'
} else {
    $nacosHome = Join-Path $Global:ToolsDir 'nacos'
    if (-not (Test-Path (Join-Path $nacosHome 'bin\startup.cmd'))) {
        $v = $Global:NacosVersion
        $zip = Join-Path $DownloadDir "nacos-$v.zip"
        Invoke-Download -OutFile $zip -Label "Nacos $v (约150MB)" `
            -Urls (Get-GithubUrls "alibaba/nacos/releases/download/$v/nacos-server-$v.zip")
        Expand-ZipTo $zip $Global:ToolsDir   # 包内自带顶层 nacos 目录
    }
    $cmdContent = "set `"JAVA_HOME=$JdkHome`"`r`ncall `"$nacosHome\bin\startup.cmd`" -m standalone"
    Start-ServiceCmd -Name 'nacos' -CmdContent $cmdContent `
        -WorkDir (Join-Path $nacosHome 'bin') -LogFile (Join-Path $LogDir 'nacos.log') | Out-Null
    if (-not (Wait-PortListening $Global:NacosPort 'Nacos' 180)) {
        throw "Nacos 启动失败，请查看 logs\nacos.log 与 $nacosHome\logs\start.out"
    }
}

# ============================================================
# 6. Redis（6379 普通 + 6380 向量库）
# ============================================================
Write-Step '6/9 检查 Redis'
$redis8Dir = Join-Path $Global:ToolsDir 'redis8'

function Install-Redis8 {
    if (Test-Path (Join-Path $redis8Dir 'redis-server.exe')) { return }
    $v = $Global:RedisWinVersion
    $zip = Join-Path $DownloadDir "redis8-$v.zip"
    Invoke-Download -OutFile $zip -Label "Redis $v for Windows (约12MB, 内置向量检索引擎)" `
        -Urls (Get-GithubUrls "redis-windows/redis-windows/releases/download/$v/Redis-$v-Windows-x64-msys2.zip")
    Expand-ZipTo $zip $redis8Dir
    # 包内可能有一层子目录，把文件提到顶层
    if (-not (Test-Path (Join-Path $redis8Dir 'redis-server.exe'))) {
        $inner = Get-ChildItem $redis8Dir -Directory | Where-Object {
            Test-Path (Join-Path $_.FullName 'redis-server.exe')
        } | Select-Object -First 1
        if ($inner) {
            Get-ChildItem $inner.FullName | Move-Item -Destination $redis8Dir -Force
            Remove-Item $inner.FullName -Recurse -Force
        }
    }
    if (-not (Test-Path (Join-Path $redis8Dir 'redis-server.exe'))) {
        throw 'Redis 8 解压后未找到 redis-server.exe'
    }
}

# 6379：OA-7 用
if (Test-PortListening $Global:RedisPort) {
    Write-Ok "Redis 已在运行 (端口 $($Global:RedisPort))"
} elseif ($CheckOnly) {
    Write-Warn2 'Redis(6379) 未运行，正式启动时将自动启动'
} else {
    $svc = Get-Service -Name 'Redis*', 'Memurai*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($svc) {
        Start-Service $svc.Name
    } else {
        Install-Redis8
        $dataDir = Join-Path $Global:ToolsDir 'redis-data-6379'
        New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
        Start-ServiceCmd -Name 'redis-6379' `
            -CmdContent "`"$redis8Dir\redis-server.exe`" --port $($Global:RedisPort) --dir `"$dataDir`"" `
            -WorkDir $redis8Dir -LogFile (Join-Path $LogDir 'redis-6379.log') | Out-Null
    }
    Wait-PortListening $Global:RedisPort 'Redis(6379)' 30 | Out-Null
}

# 6380：AI 向量库。Windows 原生 Redis 不含查询引擎(RediSearch)，
# 仅当已有 Redis Stack（如 Docker）监听 6380 且 FT 命令可用时走 redis 模式，
# 否则 AI 服务自动降级为内存向量库模式（RAG 功能完整，重启后运行期新增知识不保留）
$Global:AiMode = 'none'
if ($Global:EnableAiService) {
    if (Test-PortListening $Global:RedisVectorPort) {
        $cli = Join-Path $redis8Dir 'redis-cli.exe'
        if (-not (Test-Path $cli)) { $cli = (Get-Command redis-cli -ErrorAction SilentlyContinue).Source }
        if ($cli) {
            $eap = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
            $ft = (& $cli -h 127.0.0.1 -p $Global:RedisVectorPort FT._LIST 2>&1 | ForEach-Object { "$_" }) -join ' '
            $ErrorActionPreference = $eap
            if ($ft -notmatch 'ERR|unknown') {
                $Global:AiMode = 'redis'
                Write-Ok "向量 Redis 可用 (端口 $($Global:RedisVectorPort)，含查询引擎)"
            } else {
                $Global:AiMode = 'memory'
                Write-Warn2 "端口 $($Global:RedisVectorPort) 的 Redis 不含查询引擎(FT)，AI 服务将使用内存向量库模式"
            }
        } else {
            # 没有 redis-cli 可探测时乐观按 redis 模式处理
            $Global:AiMode = 'redis'
            Write-Ok "向量 Redis 已在运行 (端口 $($Global:RedisVectorPort))"
        }
    } else {
        $Global:AiMode = 'memory'
        Write-Info "未检测到 Redis Stack(6380)，AI 服务将使用内存向量库模式（RAG 功能完整）"
        Write-Info "如需持久化向量库：安装 Docker 后执行 docker run -d --name redis-stack -p 6380:6379 redis/redis-stack-server"
    }
}

# ============================================================
# 7. Elasticsearch（签到检索，OA-2/OA-7 启动必需）
# ============================================================
Write-Step '7/9 检查 Elasticsearch'
if (Test-PortListening $Global:EsPort) {
    Write-Ok "Elasticsearch 已在运行 (端口 $($Global:EsPort))"
} elseif ($CheckOnly) {
    Write-Warn2 'Elasticsearch 未运行，正式启动时将自动下载 7.17 并启动（约300MB）'
} else {
    $esHome = Join-Path $Global:ToolsDir "elasticsearch-$($Global:EsVersion)"
    if (-not (Test-Path (Join-Path $esHome 'bin\elasticsearch.bat'))) {
        $v = $Global:EsVersion
        $zip = Join-Path $DownloadDir "elasticsearch-$v.zip"
        Invoke-Download -OutFile $zip -Label "Elasticsearch $v (约310MB)" -Urls @(
            "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-$v-windows-x86_64.zip"
        )
        Expand-ZipTo $zip $Global:ToolsDir   # 包内自带 elasticsearch-<v> 目录
    }
    # 单机、关安全、限内存（只追加一次）
    $esCfg = Join-Path $esHome 'config\elasticsearch.yml'
    if ((Get-Content $esCfg -Raw -ErrorAction SilentlyContinue) -notmatch 'OA-ONECLICK') {
        @"

# ---- OA-ONECLICK 自动追加 ----
discovery.type: single-node
network.host: 127.0.0.1
http.port: $($Global:EsPort)
xpack.security.enabled: false
ingest.geoip.downloader.enabled: false
"@ | Add-Content $esCfg -Encoding UTF8
    }
    $jvmDir = Join-Path $esHome 'config\jvm.options.d'
    New-Item -ItemType Directory -Path $jvmDir -Force | Out-Null
    "-Xms512m`r`n-Xmx512m" | Set-Content (Join-Path $jvmDir 'oa.options') -Encoding ASCII
    Start-ServiceCmd -Name 'elasticsearch' `
        -CmdContent "call `"$esHome\bin\elasticsearch.bat`"" `
        -WorkDir (Join-Path $esHome 'bin') -LogFile (Join-Path $LogDir 'elasticsearch.log') | Out-Null
    if (-not (Wait-PortListening $Global:EsPort 'Elasticsearch' 180)) {
        throw 'Elasticsearch 启动失败，请查看 logs\elasticsearch.log'
    }
}

# ============================================================
# 8. Ollama + 本地大模型下载
# ============================================================
Write-Step '8/9 检查 Ollama 与模型'
$OllamaExe = $null
if ($Global:EnableAiService) {
    function Find-Ollama {
        $cmd = Get-Command ollama -ErrorAction SilentlyContinue
        if ($cmd) { return $cmd.Source }
        $local = Join-Path $env:LOCALAPPDATA 'Programs\Ollama\ollama.exe'
        if (Test-Path $local) { return $local }
        return $null
    }
    $OllamaExe = Find-Ollama
    if (-not $OllamaExe -and -not $CheckOnly) {
        Write-Info '未找到 Ollama，开始安装（首选 winget）...'
        $winget = Get-Command winget -ErrorAction SilentlyContinue
        if ($winget) {
            & winget install -e --id Ollama.Ollama --silent --accept-package-agreements --accept-source-agreements
        }
        $OllamaExe = Find-Ollama
        if (-not $OllamaExe) {
            Write-Info 'winget 安装未成功，改为直接下载安装包（约700MB）...'
            $setup = Join-Path $DownloadDir 'OllamaSetup.exe'
            Invoke-Download -OutFile $setup -Label 'Ollama 安装包' -Urls @('https://ollama.com/download/OllamaSetup.exe')
            Start-Process $setup -ArgumentList '/VERYSILENT', '/NORESTART', '/SP-' -Wait
            $OllamaExe = Find-Ollama
        }
        if (-not $OllamaExe) { throw 'Ollama 安装失败，请手动安装 https://ollama.com 后重试' }
        Write-Ok "Ollama 安装完成：$OllamaExe"
    } elseif ($OllamaExe) {
        Write-Ok "Ollama：$OllamaExe"
    } else {
        Write-Warn2 '未找到 Ollama，正式启动时将自动安装'
    }

    if ($OllamaExe -and -not $CheckOnly) {
        if (-not (Test-PortListening $Global:OllamaPort)) {
            Start-ServiceCmd -Name 'ollama' -CmdContent "`"$OllamaExe`" serve" `
                -WorkDir $Global:ToolsDir -LogFile (Join-Path $LogDir 'ollama.log') | Out-Null
            Wait-PortListening $Global:OllamaPort 'Ollama' 30 | Out-Null
        } else {
            Write-Ok "Ollama 服务已在运行 (端口 $($Global:OllamaPort))"
        }
        if (-not $SkipModels) {
            $eap = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
            $installed = (& $OllamaExe list 2>&1 | ForEach-Object { "$_" }) -join "`n"
            $ErrorActionPreference = $eap
            foreach ($model in @($Global:OllamaChatModel, $Global:OllamaEmbedModel)) {
                $bare = ($model -split ':')[0]
                if ($installed -match [regex]::Escape($bare)) {
                    Write-Ok "模型 $model 已存在"
                } else {
                    Write-Info "拉取模型 $model（首次下载需要几分钟，请耐心等待）..."
                    & $OllamaExe pull $model
                    if ($LASTEXITCODE -ne 0) { throw "模型 $model 下载失败，可稍后手动执行: ollama pull $model" }
                    Write-Ok "模型 $model 下载完成"
                }
            }
        } else {
            Write-Warn2 '已按 -SkipModels 跳过模型检查'
        }
    }
} else {
    Write-Warn2 '配置已关闭 AI 服务（EnableAiService=false），跳过 Ollama 与模型'
}

if ($CheckOnly) {
    Write-Step '环境检测完成（CheckOnly 模式，未做任何变更）'
    exit 0
}

# ============================================================
# 9. 构建 + 启动全部服务
# ============================================================
Write-Step '9/9 构建并启动服务'
$GatewayJar = 'backend\gateway\target\gateway-1.0.0.jar'
$EmpJar = 'backend\OA-2\target\oa-emp-service-1.0.0.jar'
$AdminJar = 'backend\OA-7\target\oa-admin-service-1.0.0.jar'
$AiJar = 'backend\oa-ai-service\target\oa-ai-service-1.0.0.jar'

function Test-Jar([string]$Rel) { Test-Path (Join-Path $Global:ProjectRoot $Rel) }

$needBuildMain = $Rebuild -or -not ((Test-Jar $GatewayJar) -and (Test-Jar $EmpJar) -and (Test-Jar $AdminJar))
if ($needBuildMain) {
    Write-Info '打包 gateway / OA-2 / OA-7（首次构建需下载依赖，可能较久）...'
    & $MvnCmd -ntp -DskipTests -s $MavenSettings -f (Join-Path $Global:ProjectRoot 'backend\pom.xml') clean package
    if ($LASTEXITCODE -ne 0) { throw '后端主工程构建失败，请检查上方 Maven 输出' }
    Write-Ok '主工程打包完成'
} else {
    Write-Ok '主工程 jar 已存在，跳过构建（如需重新打包请用 -Rebuild）'
}

$startAi = $Global:EnableAiService -and ($Global:AiMode -ne 'none')
if ($startAi) {
    if ($Rebuild -or -not (Test-Jar $AiJar)) {
        Write-Info '打包 oa-ai-service（Spring Boot 3 + Spring AI）...'
        & $MvnCmd -ntp -DskipTests -s $MavenSettings -f (Join-Path $Global:ProjectRoot 'backend\oa-ai-service\pom.xml') clean package
        if ($LASTEXITCODE -ne 0) { throw 'oa-ai-service 构建失败，请检查上方 Maven 输出' }
        Write-Ok 'oa-ai-service 打包完成'
    } else {
        Write-Ok 'oa-ai-service jar 已存在，跳过构建'
    }
}

# OA-2 自建向量库的持久化目录
New-Item -ItemType Directory -Path (Join-Path $Global:ProjectRoot 'backend\OA-2\data') -Force | Out-Null

$javaQ = "`"$JavaExe`""
$dbArgs = "--spring.datasource.username=root --spring.datasource.password=$($Global:MySqlPwd) --spring.cloud.nacos.config.import-check.enabled=false"
$dbUrl = "`"--spring.datasource.url=jdbc:mysql://localhost:$($Global:MySqlPort)/$($Global:MySqlDb)?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true`""

# 幂等：端口已被占用视为服务已在运行，跳过启动
if (Test-PortListening $Global:EmpPort) {
    Write-Ok "员工服务(OA-2) 已在运行 (端口 $($Global:EmpPort))，跳过启动"
} else {
    Start-ServiceCmd -Name 'oa-emp-service' -WorkDir $Global:ProjectRoot -LogFile (Join-Path $LogDir 'oa-emp-service.log') `
        -CmdContent "$javaQ -jar `"$EmpJar`" $dbArgs $dbUrl --ai.vector-store.persist-path=./backend/OA-2/data/vector-store.json" | Out-Null
}
if (Test-PortListening $Global:AdminPort) {
    Write-Ok "管理员服务(OA-7) 已在运行 (端口 $($Global:AdminPort))，跳过启动"
} else {
    Start-ServiceCmd -Name 'oa-admin-service' -WorkDir $Global:ProjectRoot -LogFile (Join-Path $LogDir 'oa-admin-service.log') `
        -CmdContent "$javaQ -jar `"$AdminJar`" $dbArgs $dbUrl" | Out-Null
}
if (Test-PortListening $Global:GatewayPort) {
    Write-Ok "网关已在运行 (端口 $($Global:GatewayPort))，跳过启动"
} else {
    Start-ServiceCmd -Name 'gateway' -WorkDir $Global:ProjectRoot -LogFile (Join-Path $LogDir 'gateway.log') `
        -CmdContent "$javaQ -jar `"$GatewayJar`" --spring.cloud.nacos.config.import-check.enabled=false" | Out-Null
}
if ($startAi) {
    if (Test-PortListening $Global:AiPort) {
        Write-Ok "AI 客服服务已在运行 (端口 $($Global:AiPort))，跳过启动"
    } else {
        # 模型名以 config.ps1 为准（改配置即可换模型，无需动 application.yml）
        $aiArgs = " --spring.ai.ollama.chat.options.model=$($Global:OllamaChatModel)" +
                  " --spring.ai.ollama.embedding.options.model=$($Global:OllamaEmbedModel)"
        if ($Global:AiMode -eq 'memory') {
            # 内存向量库降级：跳过 Redis 索引初始化，启用 SimpleVectorStore
            $aiArgs += ' --ai.assistant.vector-mode=memory --spring.ai.vectorstore.redis.initialize-schema=false'
        }
        Start-ServiceCmd -Name 'oa-ai-service' -WorkDir $Global:ProjectRoot -LogFile (Join-Path $LogDir 'oa-ai-service.log') `
            -CmdContent "$javaQ -jar `"$AiJar`"$aiArgs" | Out-Null
    }
}

$okEmp = Wait-PortListening $Global:EmpPort '员工服务(OA-2)' 150
$okAdmin = Wait-PortListening $Global:AdminPort '管理员服务(OA-7)' 150
$okGw = Wait-PortListening $Global:GatewayPort '网关' 120
$okAi = $true
if ($startAi) { $okAi = Wait-PortListening $Global:AiPort 'AI 客服服务' 150 }

# ---------- 前端 ----------
$okFe = $true
if (-not $SkipFrontend) {
    $feDir = Join-Path $Global:ProjectRoot 'frontend'
    if (Test-PortListening $Global:FrontendPort) {
        Write-Ok "前端已在运行 (端口 $($Global:FrontendPort))，跳过启动"
    } else {
        if (-not (Test-Path (Join-Path $feDir 'node_modules'))) {
            Write-Info '安装前端依赖 (npm install)...'
            Push-Location $feDir
            try {
                & cmd /c "npm install --registry=$($Global:NpmRegistry)"
                if ($LASTEXITCODE -ne 0) { throw 'npm install 失败' }
            } finally { Pop-Location }
            Write-Ok '前端依赖安装完成'
        }
        Start-ServiceCmd -Name 'frontend' -WorkDir $feDir -LogFile (Join-Path $LogDir 'frontend.log') `
            -CmdContent 'npm run dev' | Out-Null
        $okFe = Wait-PortListening $Global:FrontendPort '前端(Vite)' 90
    }
}

# ---------- 汇总 ----------
Write-Host ''
Write-Host '  ============================================' -ForegroundColor Magenta
Write-Host '       启动完成' -ForegroundColor Magenta
Write-Host '  ============================================' -ForegroundColor Magenta
$rows = @(
    @{ N = '前端入口';        U = "http://localhost:$($Global:FrontendPort)";            S = $okFe },
    @{ N = '网关';            U = "http://localhost:$($Global:GatewayPort)";             S = $okGw },
    @{ N = '员工服务(OA-2)';  U = "http://localhost:$($Global:EmpPort)/api/v1/employee"; S = $okEmp },
    @{ N = '管理服务(OA-7)';  U = "http://localhost:$($Global:AdminPort)/api/v1/admin";  S = $okAdmin },
    @{ N = 'AI 客服(8083)';   U = "http://localhost:$($Global:AiPort)/api/v1/ai";        S = ($startAi -and $okAi) },
    @{ N = 'Nacos 控制台';    U = "http://localhost:$($Global:NacosPort)/nacos";         S = $true }
)
foreach ($r in $rows) {
    $mark = if ($r.S) { '[OK]  ' } else { '[失败]' }
    $color = if ($r.S) { 'Green' } else { 'Red' }
    Write-Host ("  {0} {1,-14} {2}" -f $mark, $r.N, $r.U) -ForegroundColor $color
}
if ($startAi -and -not $okAi) { Write-Warn2 'AI 服务未就绪，详见 logs\oa-ai-service.log' }
if ($Global:AiMode -eq 'memory') { Write-Info 'AI 客服运行于内存向量库模式（RAG 功能完整；重启后运行期新增知识不保留）' }
Write-Info "服务日志目录：$LogDir"
Write-Info '停止全部服务：双击 stop.bat'

if ($okFe) { Start-Process "http://localhost:$($Global:FrontendPort)" }
