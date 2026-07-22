@echo off
title OA AI Service

set JAVA_HOME=C:\Program Files\Java\jdk-21
set JAR_PATH=D:\my_project\newland\backend\oa-ai-service\target\oa-ai-service-1.0.0.jar

if not exist "%JAR_PATH%" (
    echo Error: JAR not found at %JAR_PATH%
    pause
    exit /b 1
)

echo Starting oa-ai-service...
start "oa-ai-service" "%JAVA_HOME%\bin\java.exe" -jar "%JAR_PATH%" --spring.ai.ollama.chat.options.model=qwen2.5:7b --spring.ai.ollama.embedding.options.model=nomic-embed-text --ai.assistant.vector-mode=memory --spring.ai.vectorstore.redis.initialize-schema=false

echo Waiting 8 seconds...
timeout /t 8 /nobreak >nul

echo Verifying...
powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:8083/api/v1/ai/chat/health' -TimeoutSec 3 -UseBasicParsing; if($r.StatusCode -eq 200){Write-Output 'OK'}else{Write-Output 'FAIL'}}catch{Write-Output 'Not ready yet'}"
echo.
echo Service started. Check http://localhost:8083/api/v1/ai/chat/health
pause
