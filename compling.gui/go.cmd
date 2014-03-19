@echo off

set LIB=c:\users\lucag\documents\school\CogSci 199\lib
set CLASSPATH=%LIB%\java-cup-v11a.jar;%LIB%\org.eclipse.swt.win32.win32.x86_3.3.2.v3347a.jar;.\jface.jar
rem for %f in ("%LIB%"\org.eclipse*jar) do set CLASSPATH=%CLASSPATH%;%f
rem echo %CLASSPATH%

rem java -version:1.5 -cp "%CLASSPATH%;c:\users\lucag\documents\school\cogsci 199\bin" compling.gui.grammargui.GrammarBrowser %1 %2 %3 %4
rem java -version:1.5 -jar .\gui.jar compling.gui.grammargui.GrammarBrowser %1 %2 %3 %4

java -version:1.5 -cp "%CLASSPATH%";bin %1 %2 %3 %4
