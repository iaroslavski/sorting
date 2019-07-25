@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainByte.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainByte > dpq-09
