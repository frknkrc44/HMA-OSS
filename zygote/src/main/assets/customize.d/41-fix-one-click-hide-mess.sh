#!/system/bin/sh

# Try to fix all mess caused by one-click hide modules

if [ -d /data/misc/hide_my_applist ]
then
    ui_print "- Non-randomized config folder detected, fixing it"
    mv /data/misc/hide_my_applist /data/misc/hide_my_applist_$(tr -dc 'a-z0-9' < /dev/urandom | dd bs=1 count=16 2>/dev/null)
fi

for dir in /data/misc/hide_my_applist*
do
    [ -d "$dir" ] || continue
    ui_print "- Applying filesystem permissions for $dir"
    chown -R system:system "$dir"
    find "$dir" -type d -exec chmod 700 {} \;
    find "$dir" -type f -exec chmod 600 {} \;
done
