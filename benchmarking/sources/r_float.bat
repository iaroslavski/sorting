@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainFloat.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainFloat > dpq-09
