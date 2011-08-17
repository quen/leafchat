;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"

;--------------------------------
;General

  ;Name and file
  Name "leafChat @FULLVERSION@"
  OutFile "leafchat@BRIEFVERSION@.exe"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\leafChat 2"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\leafdigital\leafChat2" ""

  ;Request application privileges for Windows Vista+
  RequestExecutionLevel admin

  SetCompressor lzma

;--------------------------------
;Java check

!define MUI_CUSTOMFUNCTION_GUIINIT myGuiInit

Function myGUIInit
  ; Find java
  SearchPath $0 javaw.exe
  IfErrors 0 JavaExists
    MessageBox MB_OK "Java is not installed on this system. Please install Java from www.java.com, then run this installer again."
    Quit

JavaExists:
	SetOutPath "$TEMP"
	File Version.class
 	ExecWait '"$0" -classpath $TEMP Version $TEMP\java.version'
 	FileOpen $1 "$TEMP\java.version" r
 	FileRead $1 $2
 	FileClose $1
 	Delete "$TEMP\Version.class"
 	Delete "$TEMP\java.version"
 	
 	IntCmp $2 1004 AtLeast14 0 AtLeast14
    MessageBox MB_OK "Java 1.4 or above is not installed on this system. Please install the latest Java version from www.java.com, then run this installer again."
    Quit
 	
AtLeast14: 

FunctionEnd

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING

; Run after
;--------------------------------
  Function RunProgram
    SearchPath $0 javaw.exe
    Exec '"$0" -jar leafChat.jar'
  FunctionEnd

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  !define MUI_FINISHPAGE_RUN
  !define MUI_FINISHPAGE_RUN_FUNCTION RunProgram
  !insertmacro MUI_PAGE_FINISH
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "leafChat program files" SecFiles

  SetOutPath "$INSTDIR"
  SetShellVarContext all

  File /r /x leafchat.nsi /x Version.class *.*

  ;Store installation folder
  WriteRegStr HKCU "Software\leafdigital\leafChat2" "" $INSTDIR

  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  
  ;Create shortcuts
  CreateDirectory "$SMPROGRAMS\leafChat 2"
  CreateShortCut "$SMPROGRAMS\leafChat 2\leafChat 2.lnk" "$0" \
    "-jar leafChat.jar" "$INSTDIR\leafChat.ico"
  CreateShortCut "$SMPROGRAMS\leafChat 2\Uninstall.lnk" \
    "$INSTDIR\Uninstall.exe"

SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_SecFiles ${LANG_ENGLISH} "leafChat program files"

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecFiles} $(DESC_SecFiles)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END
 
;--------------------------------
;Uninstaller Section

Section "Uninstall"

  SetShellVarContext all

  Delete "$INSTDIR\leafChat.jar"
  Delete "$INSTDIR\leafChat.ico"
  RMDir /r "$INSTDIR\core"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\help"
  RMDir /r "$INSTDIR\plugins"
  RMDir /r "$INSTDIR\sounds"
  RMDir /r "$INSTDIR\themes"

  Delete "$INSTDIR\Uninstall.exe"

  RMDir "$INSTDIR"
  
  Delete "$SMPROGRAMS\leafChat 2\leafChat 2.lnk"
  Delete "$SMPROGRAMS\leafChat 2\Uninstall.lnk"
  RMDir "$SMPROGRAMS\leafChat 2"
  
  DeleteRegKey /ifempty HKCU "Software\leafdigital\leafChat2"

SectionEnd