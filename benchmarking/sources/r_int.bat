@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainInt.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainInt > dpq-09
