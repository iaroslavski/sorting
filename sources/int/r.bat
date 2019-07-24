@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > dpq-09
