# ============================================================
#  OA 管理系统 一键启动 - 集中配置
#  按需修改后保存即可，所有脚本共用本文件
# ============================================================

# ---------- 路径 ----------
# 工具安装目录（JDK/Maven/Nacos/Redis8/ES 自动下载到这里）
# 注意：必须是不含中文、不含空格的纯英文路径，避免各类中间件脚本踩坑
$Global:ToolsDir = Join-Path (Split-Path $Global:ProjectRoot -Parent) '.oa-tools'

# ---------- MySQL ----------
# root 密码；留空字符串表示无密码。脚本会先用这里的值，失败后再尝试常见候选
$Global:MySqlPassword = 'ljn050825'
$Global:MySqlPasswordCandidates = @('', 'root123', '123456', 'root', '123')
$Global:MySqlDb = 'day'
$Global:MySqlPort = 3306

# ---------- 中间件端口 ----------
$Global:NacosPort = 8848
$Global:RedisPort = 6379          # OA-7 用的普通 Redis
$Global:RedisVectorPort = 6380    # AI 服务用的向量 Redis（Redis 8 自带查询引擎）
$Global:EsPort = 9200             # Elasticsearch（签到数据检索，OA-2/OA-7 启动必需）
$Global:OllamaPort = 11434

# ---------- 服务端口 ----------
$Global:GatewayPort = 8888
$Global:EmpPort = 8081            # OA-2 员工服务
$Global:AdminPort = 8082          # OA-7 管理员服务
$Global:AiPort = 8083             # AI 客服服务
$Global:FrontendPort = 5173

# ---------- AI 模型（Ollama）----------
$Global:EnableAiService = $true               # 设为 $false 可整体跳过 AI 服务与模型下载
$Global:OllamaChatModel = 'qwen2.5:1.5b'      # 对话模型，约 1.0 GB
$Global:OllamaEmbedModel = 'nomic-embed-text' # 向量化模型，约 0.3 GB

# ---------- 组件版本 ----------
$Global:JdkMajor = 21
$Global:MavenVersion = '3.9.9'
$Global:NacosVersion = '2.3.2'
$Global:RedisWinVersion = '8.0.0'
$Global:EsVersion = '7.17.18'

# ---------- 下载源 ----------
# GitHub 加速前缀，按顺序尝试；最后会回退 GitHub 直连
$Global:GithubMirrors = @(
    'https://ghfast.top/',
    'https://gh-proxy.com/',
    ''
)

# ---------- npm ----------
$Global:NpmRegistry = 'https://registry.npmmirror.com'
