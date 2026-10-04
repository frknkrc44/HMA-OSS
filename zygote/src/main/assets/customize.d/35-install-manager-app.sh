#!/system/bin/sh

APK_FILE=$MODPATH/manager.apk

install_pkg() {
  pm install --user $1 $APK_FILE 2>&1

  [ $? -ne 0 ] && ui_print "! Cannot install the manager app for user "$user
}

# we need to determine the manager app was installed or not at first
if test -n "$(pm list packages --user -1 org.frknkrc44.hma_oss)"
then
  # determine the installed user
  for user in $(pm list users | cut -f1 -d: | cut -f2 -d{ | tail -n +2)
  do
    # if path detected in user then install the manager app for it (except Xiaomi's dual app space)
    if [ "$user" != "999" ] && pm path --user $user org.frknkrc44.hma_oss &> /dev/null
    then
      ui_print "- Installing manager app update for user "$user

      install_pkg $user
    fi
  done
else
  ui_print "- Installing manager app for user 0"

  # install it into user 0
  install_pkg 0
fi
