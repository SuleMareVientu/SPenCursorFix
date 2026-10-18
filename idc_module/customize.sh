#!/system/bin/sh
SKIPMOUNT=false
PROPFILE=false
POSTFSDATA=false
LATESTARTSERVICE=false

ui_print "****************************************"
ui_print "*  S Pen IDC Overlay for LineageOS Tablets  *"
ui_print "****************************************"
ui_print "- Installing sec_e-pen.idc..."

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/system/usr/idc/sec_e-pen.idc" 0 0 0644
set_perm "$MODPATH/system/vendor/usr/idc/sec_e-pen.idc" 0 0 0644

ui_print "- Done! Reboot to apply native stylus routing."
