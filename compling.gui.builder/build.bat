@echo off
rem set ECLIPSE_HOME=e:\usr\eclipse
rem set ECLIPSE_HOME=e:\usr\eclipse-rcp-3.7-test
set ECLIPSE_HOME=e:\usr\eclipse\3.7-rcp-base

%ECLIPSE_HOME%\eclipsec -nosplash -application org.eclipse.ant.core.antRunner -buildfile productBuild.xml -consoleLog %*