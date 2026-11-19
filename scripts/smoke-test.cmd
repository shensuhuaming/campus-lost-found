@echo off
rem 校园失物招领系统 · 冒烟测试包装脚本
rem 用途：绕过 Windows 默认的脚本执行策略限制，双击或命令行均可运行
rem 用法：scripts\smoke-test.cmd   或   scripts\smoke-test.cmd -BaseUrl http://192.168.1.10:8080
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0smoke-test.ps1" %*
exit /b %ERRORLEVEL%
