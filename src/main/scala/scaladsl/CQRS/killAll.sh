#!/usr/bin/env bash

sbt_directory="../../../../../"
cd $sbt_directory



cd -

while read -r PID; do 
echo "kill -9 $PID";
echo $PID | xargs kill -9
done < KILL_SWITCH.txt
// rm KILL_SWITCH.txt

kill -9 $(cat PID)
