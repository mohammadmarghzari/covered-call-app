# ==========================================================================
#  VPN Sales Bot - Windows one-shot setup (PowerShell)
#  Builds and runs the whole stack with Docker Desktop.
#  Usage (from the project root, after cloning):
#     powershell -ExecutionPolicy Bypass -File .\vpn-sales-bot\scripts\setup_windows.ps1
# ==========================================================================
$ErrorActionPreference = "Stop"

function Info($m) { Write-Host "[*] $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "[OK] $m" -ForegroundColor Green }
function Warn($m) { Write-Host "[!] $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "[x] $m" -ForegroundColor Red; exit 1 }

Write-Host "==== VPN Sales Bot - Windows setup ====" -ForegroundColor Magenta

# --- 1) prerequisites -----------------------------------------------------
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Fail "Git نصب نیست. از https://git-scm.com نصب کن و دوباره اجرا کن."
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Fail "Docker Desktop نصب نیست. از https://www.docker.com/products/docker-desktop نصب کن."
}
try { docker info *> $null } catch {
    Fail "Docker Desktop در حال اجرا نیست. بازش کن تا کامل بالا بیاید (آیکن نهنگ سبز شود) و دوباره اجرا کن."
}
Ok "Git و Docker آماده‌اند."

# --- 2) move to the vpn-sales-bot directory -------------------------------
# This script lives in vpn-sales-bot/scripts, so go one level up.
$projectDir = Split-Path -Parent $PSScriptRoot
Set-Location $projectDir
Info "پوشهٔ پروژه: $projectDir"

# --- 3) build .env --------------------------------------------------------
if (Test-Path ".env") {
    Warn "فایل .env از قبل وجود دارد؛ از همان استفاده می‌شود (اگر می‌خواهی از نو بسازی، حذفش کن)."
} else {
    Write-Host "`n--- تنظیم اطلاعات ربات ---" -ForegroundColor Yellow
    $token = Read-Host "توکن ربات (BOT_TOKEN)"
    if ([string]::IsNullOrWhiteSpace($token)) { Fail "توکن نمی‌تواند خالی باشد." }
    $admin = Read-Host "آیدی عددی مدیر (ADMIN_IDS) - چند مدیر با کاما"
    if ([string]::IsNullOrWhiteSpace($admin)) { Fail "آیدی مدیر نمی‌تواند خالی باشد." }
    $channel = Read-Host "آدرس کانال (Enter = https://t.me/unnamed_vpn_7)"
    if ([string]::IsNullOrWhiteSpace($channel)) { $channel = "https://t.me/unnamed_vpn_7" }

    # random DB password (letters+digits only)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $buf = New-Object 'System.Byte[]' 32
    $rng.GetBytes($buf)
    $pass = ([Convert]::ToBase64String($buf) -replace '[^A-Za-z0-9]', '').Substring(0, 24)

    $envContent = @"
BOT_TOKEN=$token
ADMIN_IDS=$admin
ADMIN_CHANNEL_ID=$channel

POSTGRES_USER=vpnbot
POSTGRES_PASSWORD=$pass
POSTGRES_DB=vpnbot
POSTGRES_HOST=db
POSTGRES_PORT=5432
DATABASE_URL=postgresql+asyncpg://vpnbot:$pass@db:5432/vpnbot

REDIS_URL=redis://redis:6379/0

RATE_LIMIT_MESSAGES=5
RATE_LIMIT_WINDOW=3
LOG_LEVEL=INFO
CURRENCY=تومان
TIMEZONE=Asia/Tehran
"@
    # Write UTF-8 WITHOUT BOM so Docker/pydantic read Persian values correctly.
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText((Join-Path $projectDir ".env"), $envContent, $utf8NoBom)
    Ok "فایل .env ساخته شد."
}

# --- 4) build & start -----------------------------------------------------
Info "در حال ساخت و اجرای ربات (بار اول چند دقیقه طول می‌کشد)..."
docker compose up -d --build
if ($LASTEXITCODE -ne 0) { Fail "اجرای docker compose ناموفق بود. متن خطا را برای پشتیبانی بفرست." }

# --- 5) seed initial plans -----------------------------------------------
Info "وارد کردن پلن‌های اولیه..."
docker compose run --rm bot python -m app.seed

Write-Host ""
Ok "همه‌چیز آماده شد! ربات روشن است."
Write-Host "----------------------------------------------------" -ForegroundColor Magenta
Write-Host " • دیدن لاگ:     docker compose logs -f bot"
Write-Host " • توقف ربات:    docker compose down"
Write-Host " • اجرای دوباره:  docker compose up -d"
Write-Host "----------------------------------------------------" -ForegroundColor Magenta
Write-Host "حالا در تلگرام به @HighQualityVPN_bot پیام /start بده و بعد /admin را امتحان کن."
