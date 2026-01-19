# Wix toolset downloaded from https://sourceforge.net/projects/npackd/files/org.wixtoolset.WiX-3.8.exe/download
# (C) MA Wheadon 2026

jpackage --type msi --name ArmyListDesigner --input . --main-jar ald-2026.01.19.jar --dest . --app-version 1.0.0 --description "Army List Designer for DBM armies" --win-shortcut --win-shortcut-prompt --win-menu --vendor peltast.org.uk --icon heater_shield_cross.ico --copyright "MA Wheadon 2026" --license-file LICENSE