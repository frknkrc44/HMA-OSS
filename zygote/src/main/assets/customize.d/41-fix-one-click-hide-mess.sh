#!/system/bin/sh

# Try to fix all mess caused by one-click hide modules

if [ -d /data/misc/hide_my_applist ]
then
    ui_print "- Non-randomized config folder detected, fixing it"
    mv /data/misc/hide_my_applist /data/misc/hide_my_applist_$(tr -dc 'a-z0-9' < /dev/urandom | dd bs=1 count=16 2>/dev/null)
fi

if find /data/misc/ -maxdepth 1 -type d -name hide_my_applist\* >/dev/null 2>&1
then
    ui_print "- Applying filesystem permissions"
    chown system:system -R /data/misc/hide_my_applist*
    find /data/misc/hide_my_applist* -type d -exec chmod 700 {} \;
    find /data/misc/hide_my_applist* -type f -exec chmod 600 {} \;
fi
