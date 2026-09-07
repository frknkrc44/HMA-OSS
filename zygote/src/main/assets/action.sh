#!/system/bin/sh

MODDIR="${0%/*}"
PKG=org.frknkrc44.hma_oss
ACTIVITY=org.frknkrc44.hma_oss.ui.activity.MainActivity
APK_FILE="$MODDIR/manager.apk"

# reload module status first (regardless of launch outcome)
sh "$MODDIR/update_desc.sh"
echo "- Updated module status"

install_pkg() {
  pm install --user $1 $APK_FILE 2>&1

  [ $? -ne 0 ] && echo "! Cannot install the manager app for user "$1
}

launch_pkg() {
  echo "- Launching HMA-OSS manager on user "$1
  am start -n $PKG/$ACTIVITY --user $1
}

for user in $(pm list users | cut -f1 -d: | cut -f2 -d{ | tail -n +2)
  do
    # if path detected in user then install the manager app for it (except Xiaomi's dual app space)
    if [ "$user" != "999" ] && pm path --user $user $PKG &> /dev/null
    then
      launch_pkg $user
      exit 0
    fi
done

echo "- Manager app not found, installing for user 0"
install_pkg 0 && launch_pkg 0
