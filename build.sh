#/bin/bash

rm -rf out/
mkdir -p out

javac -cp /home/rsalesc/robocode/libs/robocode.jar:./src -d out src/rsalesc/roborio/Roborio.java \
    && ruby build.rb rsalesc.roborio.Roborio $* && jar cmf MANIFEST.MF rsalesc.roborio.Roborio_$*.jar -C out . \
    && cp rsalesc.roborio.Roborio_$*.jar /home/rsalesc/robocode/robots && true && mkdir -p obfuscated \
    && proguard -injars rsalesc.roborio.Roborio_$*.jar -outjars obfuscated/rsalesc.roborio.Roborio_$*.jar \
        -libraryjars /home/rsalesc/robocode/libs/robocode.jar -libraryjars $JAVA_HOME/jre/lib/rt.jar \
        -keep "public class rsalesc.roborio.Roborio" && rm -rf obfuscated/*.*tc.jar
