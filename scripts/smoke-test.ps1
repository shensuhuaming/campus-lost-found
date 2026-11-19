<#
.SYNOPSIS
    校园失物招领系统 · 接口冒烟测试（端到端主流程）

.DESCRIPTION
    面向集成测试同学：在已启动的全栈环境上跑通
    「注册 → 登录 → 发布 → 审核 → 列表/详情 → 认领 → 核验 → 留言 → 消息 → 统计」主流程，
    并顺带验证几个关键负向用例（错误口令、重复认领、越权访问后台、已完成不可关闭）。

    纯 HTTP 调用，不依赖数据库客户端；每次运行用随机学号注册两个新用户，
    不修改已有数据，可反复执行。兼容 Windows PowerShell 5.1 / PowerShell 7+。

.PARAMETER BaseUrl
    后端地址，默认 http://localhost:8080

.PARAMETER AdminNo / AdminPwd
    管理员账号（默认取本地环境 admin / Admin@123）

.PARAMETER Password
    脚本创建的测试用户密码（6~20 位且含字母和数字）

.EXAMPLE
    powershell -NoProfile -File scripts/smoke-test.ps1
    powershell -NoProfile -File scripts/smoke-test.ps1 -BaseUrl http://192.168.1.10:8080

.NOTES
    退出码：0 全部通过；1 有失败步骤（可接入 CI）。
#>
#requires -Version 5.1
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$AdminNo = 'admin',
    [string]$AdminPwd = 'Admin@123',
    [string]$Password = 'Test1234'
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$script:passed = 0
$script:failed = 0
$script:base = $BaseUrl.TrimEnd('/')

function Invoke-Api {
    param(
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$Path,
        [string]$Token,
        [object]$Body
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    $params = @{
        Uri                = "$($script:base)$Path"
        Method             = $Method
        Headers            = $headers
        UseBasicParsing    = $true
        TimeoutSec         = 20
    }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 6 -Compress
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes($json)
        $params['ContentType'] = 'application/json; charset=utf-8'
    }

    $status = 0
    $text = ''
    try {
        $resp = Invoke-WebRequest @params
        $status = [int]$resp.StatusCode
        $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    } catch {
        $webResp = $null
        if ($_.Exception.PSObject.Properties['Response']) { $webResp = $_.Exception.Response }
        if ($null -eq $webResp) { throw }
        $status = [int]$webResp.StatusCode
        $stream = $webResp.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
        $text = $reader.ReadToEnd()
        $reader.Dispose()
    }

    $parsed = $null
    if ($text) {
        try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null }
    }
    [pscustomobject]@{ Status = $status; Body = $parsed; Raw = $text }
}

function Assert-Equal {
    param($Actual, $Expected, [string]$Message)
    if ($Actual -ne $Expected) {
        throw "断言失败：$Message（期望 [$Expected]，实际 [$Actual]）"
    }
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "断言失败：$Message" }
}

function Test-Step {
    param([string]$Name, [scriptblock]$Body)
    try {
        & $Body
        $script:passed++
        Write-Host ("  [PASS] {0}" -f $Name) -ForegroundColor Green
    } catch {
        $script:failed++
        Write-Host ("  [FAIL] {0}" -f $Name) -ForegroundColor Red
        Write-Host ("         {0}" -f $_.Exception.Message) -ForegroundColor DarkYellow
    }
}

# 每次运行使用唯一学号，避免“学号已注册”导致重跑失败（6~12 位数字）
$suffix = (Get-Date -Format 'HHmmss') + (Get-Random -Minimum 10 -Maximum 99)
$studentA = '9' + $suffix
$studentB = '8' + $suffix
$stamp = Get-Date -Format 'MMdd-HHmmss'
$itemTitle = "冒烟测试-蓝色水杯-$stamp"

Write-Host ""
Write-Host "=== 校园失物招领系统 · 冒烟测试 ===" -ForegroundColor Cyan
Write-Host "目标：$($script:base)"
Write-Host "测试用户：A=$studentA  B=$studentB  口令=$Password"
Write-Host ""

Test-Step '1. 健康检查：服务与数据库可用' {
    $r = Invoke-Api GET '/api/system/health'
    Assert-Equal $r.Status 200 'HTTP 状态'
    Assert-Equal $r.Body.code 200 '业务码'
    Assert-True ($null -ne $r.Body.data) '应返回健康数据'
}

Test-Step '2. 管理员登录并获取 token' {
    $r = Invoke-Api POST '/api/auth/login' $null @{ studentNo = $AdminNo; password = $AdminPwd }
    Assert-Equal $r.Body.code 200 '管理员登录应成功（检查 AdminNo/AdminPwd）'
    $script:adminToken = $r.Body.data.token
    Assert-True ($script:adminToken.Length -gt 20) 'token 长度异常'
}

Test-Step '3. 注册两个学生账号（A 发布者 / B 认领人）' {
    $a = Invoke-Api POST '/api/auth/register' $null @{
        studentNo = $studentA; nickname = '冒烟A'; phone = '13800000001'
        password = $Password; confirmPassword = $Password
    }
    Assert-Equal $a.Body.code 200 "注册 A 失败：$($a.Body.message)"
    $b = Invoke-Api POST '/api/auth/register' $null @{
        studentNo = $studentB; nickname = '冒烟B'; phone = '13800000002'
        password = $Password; confirmPassword = $Password
    }
    Assert-Equal $b.Body.code 200 "注册 B 失败：$($b.Body.message)"
}

Test-Step '4. 两个学生分别登录' {
    $a = Invoke-Api POST '/api/auth/login' $null @{ studentNo = $studentA; password = $Password }
    Assert-Equal $a.Body.code 200 'A 登录失败'
    $script:tokenA = $a.Body.data.token
    $script:userA = $a.Body.data.user
    $b = Invoke-Api POST '/api/auth/login' $null @{ studentNo = $studentB; password = $Password }
    Assert-Equal $b.Body.code 200 'B 登录失败'
    $script:tokenB = $b.Body.data.token
    $script:userB = $b.Body.data.user
    Assert-Equal $script:userA.studentNo $studentA 'A 学号不一致'
}

Test-Step '5. 错误口令登录应返回 1003' {
    $r = Invoke-Api POST '/api/auth/login' $null @{ studentNo = $studentA; password = 'wrong123' }
    Assert-Equal $r.Body.code 1003 '错误口令应返回 1003'
}

Test-Step '6. A 发布拾物信息（待审核，不应出现在前台列表）' {
    $r = Invoke-Api POST '/api/items' $script:tokenA @{
        type = 1; title = $itemTitle
        description = '冒烟测试用：图书馆三楼捡到蓝色水杯，杯身有白色贴纸'
        categoryId = 1; happenTime = (Get-Date).AddHours(-2).ToString('yyyy-MM-dd HH:mm:ss')
        happenPlace = '图书馆三楼'; contact = '13800000001'; imageUrls = @()
    }
    Assert-Equal $r.Body.code 200 "发布失败：$($r.Body.message)"
    $script:itemId = $r.Body.data.id
    $list = Invoke-Api GET '/api/items?page=1&size=50'
    $visible = @($list.Body.data.records | Where-Object { $_.id -eq $script:itemId })
    Assert-Equal $visible.Count 0 '待审核信息不应对公众可见'
}

Test-Step '7. 管理员审核通过后前台可见' {
    $r = Invoke-Api PUT "/api/admin/items/$($script:itemId)/audit" $script:adminToken @{ auditStatus = 1 }
    Assert-Equal $r.Body.code 200 "审核失败：$($r.Body.message)"
    $list = Invoke-Api GET '/api/items?page=1&size=50'
    $visible = @($list.Body.data.records | Where-Object { $_.id -eq $script:itemId })
    Assert-Equal $visible.Count 1 '审核通过后应出现在列表'
    Assert-Equal $visible[0].itemStatusText '进行中' '物品状态应为进行中'
}

Test-Step '8. 游客查看详情：联系方式脱敏' {
    $r = Invoke-Api GET "/api/items/$($script:itemId)"
    Assert-Equal $r.Body.code 200 '详情接口失败'
    Assert-Equal $r.Body.data.contact '138****0001' '游客应看到脱敏手机号'
    Assert-Equal $r.Body.data.myClaimStatus $null '游客 myClaimStatus 应为 null'
}

Test-Step '9. B 发起认领：物品进入处理中' {
    $r = Invoke-Api POST '/api/claims' $script:tokenB @{
        itemId = $script:itemId; detailDesc = '杯子是我的，杯底有名字缩写，可提供学号核验'
    }
    Assert-Equal $r.Body.code 200 "认领失败：$($r.Body.message)"
    Assert-Equal $r.Body.data.itemStatus 1 '认领后物品应为处理中(1)'
    $script:claimId = $r.Body.data.claimId
}

Test-Step '10. 重复认领应返回 3003' {
    $r = Invoke-Api POST '/api/claims' $script:tokenB @{ itemId = $script:itemId; detailDesc = '再来一次' }
    Assert-Equal $r.Body.code 3003 '重复申请应返回 3003'
}

Test-Step '11. 发布者不能认领自己的物品（A 认领应 3001）' {
    $r = Invoke-Api POST '/api/claims' $script:tokenA @{ itemId = $script:itemId; detailDesc = '自己认领自己' }
    Assert-Equal $r.Body.code 3001 '认领自己的物品应返回 3001'
}

Test-Step '12. 管理员核验列表可见该申请' {
    $r = Invoke-Api GET '/api/claims?status=0&page=1&size=50' $script:adminToken
    Assert-Equal $r.Body.code 200 '核验列表失败'
    $hit = @($r.Body.data.records | Where-Object { $_.id -eq $script:claimId })
    Assert-Equal $hit.Count 1 '待核验列表中应有该申请'
    Assert-Equal $hit[0].applicant.studentNo $studentB '申请人学号不一致'
}

Test-Step '13. 核验通过：物品已完成' {
    $r = Invoke-Api PUT "/api/claims/$($script:claimId)/approve" $script:adminToken @{ reply = '请到图书馆服务台领取' }
    Assert-Equal $r.Body.code 200 "核验失败：$($r.Body.message)"
    Assert-Equal $r.Body.data.itemStatus 2 '核验通过后物品应为已完成(2)'
    $detail = Invoke-Api GET "/api/items/$($script:itemId)"
    Assert-Equal $detail.Body.data.itemStatus 2 '详情页状态应与列表一致'
}

Test-Step '14. 已完成的物品不能再关闭（2003）' {
    $r = Invoke-Api PUT "/api/items/$($script:itemId)/close" $script:tokenA @{}
    Assert-Equal $r.Body.code 2003 '已完成物品关闭应返回 2003'
}

Test-Step '15. B 发表留言成功，A 收到未读消息' {
    $c = Invoke-Api POST '/api/comments' $script:tokenB @{
        itemId = $script:itemId; content = '冒烟测试留言：恭喜找回，感谢管理员'
    }
    Assert-Equal $c.Body.code 200 "留言失败：$($c.Body.message)"
    $m = Invoke-Api GET '/api/messages/unread-count' $script:tokenA
    Assert-Equal $m.Body.code 200 '未读数接口失败'
    Assert-True ($m.Body.data.count -gt 0) 'A 应有未读消息'
    $list = Invoke-Api GET '/api/messages?isRead=0&page=1&size=10' $script:tokenA
    $hit = @($list.Body.data.records | Where-Object { $_.type -eq 2 })
    Assert-True ($hit.Count -gt 0) '应存在留言提醒(type=2)消息'
    $script:msgId = $hit[0].id
}

Test-Step '16. 标记消息已读后未读数减少' {
    $before = (Invoke-Api GET '/api/messages/unread-count' $script:tokenA).Body.data.count
    $r = Invoke-Api PUT "/api/messages/$($script:msgId)/read" $script:tokenA
    Assert-Equal $r.Body.code 200 '标记已读失败'
    Assert-Equal $r.Body.data.isRead 1 '返回应为已读'
    $after = (Invoke-Api GET '/api/messages/unread-count' $script:tokenA).Body.data.count
    Assert-Equal $after ($before - 1) '未读数应减 1'
}

Test-Step '17. 学生访问管理接口应 2002' {
    $r = Invoke-Api GET '/api/admin/stats' $script:tokenB
    Assert-Equal $r.Body.code 2002 '学生访问后台应返回 2002'
    Assert-Equal $r.Status 403 'HTTP 状态应为 403'
}

Test-Step '18. 统计接口返回四项指标与 30 天趋势' {
    $r = Invoke-Api GET '/api/admin/stats' $script:adminToken
    Assert-Equal $r.Body.code 200 '统计接口失败'
    Assert-True ($r.Body.data.totalItems -ge 1) 'totalItems 应 ≥1'
    Assert-Equal @($r.Body.data.trend).Count 30 '默认趋势应为 30 个点'
    Assert-True ($r.Body.data.returnRate -ge 0 -and $r.Body.data.returnRate -le 100) '归还率应在 0~100'
}

Test-Step '19. 个人中心统计与我的认领可见' {
    $p = Invoke-Api GET '/api/user/profile' $script:tokenB
    Assert-Equal $p.Body.code 200 '个人资料接口失败'
    Assert-True ($p.Body.data.stat.claiming -ge 1) 'B 的认领数应 ≥1'
    $c = Invoke-Api GET '/api/user/claims?page=1&size=10' $script:tokenB
    $hit = @($c.Body.data.records | Where-Object { $_.itemId -eq $script:itemId })
    Assert-Equal $hit.Count 1 '我的认领中应有该记录'
    Assert-Equal $hit[0].status 1 '认领结果应为已通过'
}

Write-Host ""
Write-Host "=== 结果：通过 $($script:passed) / 失败 $($script:failed) ===" -ForegroundColor Cyan
if ($script:failed -gt 0) {
    Write-Host '冒烟测试未通过，请查看上方 [FAIL] 项（服务日志：docker compose logs -f backend）' -ForegroundColor Red
    exit 1
}
Write-Host '冒烟测试全部通过。' -ForegroundColor Green
exit 0
