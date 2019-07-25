@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainChar.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainChar > dpq-09
