; Inno Setup script for the RIMVISUALS launcher (built by CI: iscc packaging\installer.iss /DAppVersion=...)
#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif
[Setup]
AppId={{6E0F6A8C-52D4-4B3B-9B65-5A2F0C7D4E11}
AppName=RIMVISUALS
AppVersion={#AppVersion}
AppPublisher=RIMVISUALS
DefaultDirName={autopf}\RIMVISUALS
DefaultGroupName=RIMVISUALS
OutputDir=..\dist-installer
OutputBaseFilename=RIMVISUALS-Setup
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern
UninstallDisplayIcon={app}\RIMVISUALS.exe
[Files]
Source: "..\dist\RIMVISUALS.exe"; DestDir: "{app}"; Flags: ignoreversion
[Icons]
Name: "{group}\RIMVISUALS"; Filename: "{app}\RIMVISUALS.exe"
Name: "{autodesktop}\RIMVISUALS"; Filename: "{app}\RIMVISUALS.exe"; Tasks: desktopicon
[Tasks]
Name: "desktopicon"; Description: "Ярлык на рабочем столе / Desktop shortcut"
[Run]
Filename: "{app}\RIMVISUALS.exe"; Description: "Запустить RIMVISUALS"; Flags: nowait postinstall skipifsilent
; The uninstaller removes only the program. Your game data (%APPDATA%\RIMVISUALS: profiles, worlds, configs) is never deleted.
