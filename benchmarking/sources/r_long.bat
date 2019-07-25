@echo off
cls

set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-01

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-02

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-03

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-04

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-05

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-06

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-07

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-08

rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac MainLong.java -sourcepath . -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% MainLong > dpq-09
