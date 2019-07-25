@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainDouble.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainDouble > dpq-09
