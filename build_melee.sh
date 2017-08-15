#/bin/bash

rm -rf out/
mkdir -p out

javac -cp /home/rsalesc/robocode/libs/robocode.jar:./src -d out src/roborio/RoborioPorradeiro.java \
    && ruby build.rb RoborioPorradeiro $* && cp -R src/roborio/ out/ && jar cmf MANIFEST.MF roborio.RoborioPorradeiro_$*.jar -C out . \
    && cp roborio.RoborioPorradeiro_$*.jar /home/rsalesc/robocode/robots
