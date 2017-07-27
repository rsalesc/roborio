#/bin/bash

rm -rf out/
mkdir -p out

javac -cp /home/rsalesc/robocode/libs/robocode.jar:./src -d out src/roborio/Roborio.java \
    && ruby build.rb $* && cp -R src/roborio/ out/ && jar cmf MANIFEST.MF roborio.Roborio_$*.jar -C out . \
    && cp roborio.Roborio_$*.jar /home/rsalesc/robocode/robots
