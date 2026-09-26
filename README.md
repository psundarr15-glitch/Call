# VaultCall — Premium Call Backup Vault

## Included functional screens
- PIN Create / PIN Unlock
- Premium Dashboard
- Call Records with number search
- Contacts directory
- Backup Schedule
- Backup History from actual successful backup runs
- Settings / Automation status
- Security Info
- Bottom navigation across the main sections

## Backup automation
- Full backup every 4 hours backup at 00/04/08/12/16/20
- Daily full backup at 7:00 PM
- Weekly full backup Tuesday and Friday at 5:00 PM
- Deleted-call detection is persisted locally and shown as DELETED

## Dashboard metrics
- Next data send
- Saved calls
- Deleted calls
- Last sent count
- Successful backup count

## Notes
Telegram bot token/chat ID are still supplied through Gradle properties:
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
