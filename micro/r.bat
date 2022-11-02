@echo off
cls

rem Script run_jmh_sort.sh from Laurent
rem
rem #!/bin/bash
rem export JAVA_OPTIONS="-Xms1g -Xmx1g"
rem make test TEST="micro:java.util.ArraysSort" MICRO="FORK=1;RESULTS_FORMAT=json"

call mvn clean package

echo.
echo Benchmarking...

C:\Jdk\Jdk\bin\java -jar target/benchmarks.jar org.openjdk.bench.java.util.Main > 1.txt
