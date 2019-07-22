@echo off
cls


set CLASSES=classes
set JAVA=C:\Jdk\Jdk_x64


set RESULT=dpq-01
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac -Xlint:unchecked Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-02
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac -Xlint:unchecked Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-03
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac -Xlint:unchecked Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-04
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-05
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-06
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-07
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-08
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%


set RESULT=dpq-09
rd /s/q %CLASSES%
md %CLASSES%
%JAVA%\bin\javac Main.java -sourcepath prev;. -d %CLASSES%
%JAVA%\bin\java -Xbatch -classpath %CLASSES% Main > %RESULT%
