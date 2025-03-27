@echo off

rem clean up any temp files, and create the tmp folder again
rd /s/q tmp
mkdir tmp
cd tmp

rem Combine all the jars together into one jar
"C:\Program Files\7-Zip\7z" x -aoa ..\CR-v1_7_R4.jar
"C:\Program Files\7-Zip\7z" x -aoa ..\CR-v1_8_R3.jar
"C:\Program Files\7-Zip\7z" x -aoa ..\CR-v1_12_R1.jar
"C:\Program Files\7-Zip\7z" x -aoa ..\CR-v1_16_R3.jar
"C:\Program Files\7-Zip\7z" x -aoa ..\CR-Base.jar

cd ..

"jar" -cvf CorpseReborn.jar -C tmp .

rem copy the built jar to my test server
copy CorpseReborn.jar ./CorpseReborn.jar

rem Delete the temp files
del CorpseReborn.jar
rd /s/q tmp