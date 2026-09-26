# Rolling 4-Hour Full Backup

The schedule is NOT fixed to clock windows.

Example:
- Backup succeeds at 6:00 PM.
- Next backup is due at 10:00 PM.
- Next is due at 2:00 AM.
- Next is due at 6:00 AM.

Each successful backup sends all calls recorded since the previous successful backup.
If a backup fails, the checkpoint is not advanced, so the next successful run includes
the unsent calls instead of losing them.

No 7 PM special backup, no Tuesday/Friday special backup, and no fixed 12–4/4–8 slots.
