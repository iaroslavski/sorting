@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainShort.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainShort > dpq-09
