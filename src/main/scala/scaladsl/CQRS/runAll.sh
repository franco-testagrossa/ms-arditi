#!/usr/bin/env bash



echo "This is going to start"
echo "three processes in parallel"
echo "and will flood your terminal. "
echo "Be warned. "
echo ""
echo "Oh, and I won't write a way to kill them, "
echo "so I can imagine you from now looking desesperately"
echo "on the web explorer for a solution while your computer"
echo  "fills Cassandra with non-sense until it crashes because"
echo "it's disk is filled to the brim with Scala-blazing-fast"
echo "procedurally-gen Actor-driven messages."
echo  ""
echo  ""
echo  "That made me chuckle haha. "
echo "Yeah, however. Here are some commands you can throw on a whim if things get out of control ;) "
echo "killall -9 java"
echo  "."
echo   "... that's has to be the closest I felt to handing a loaded gun haha"
echo  "Good luck. *hands gun*"



RED='\033[0;31m'
NC='\033[0m' # No Color

echo "------ ${RED}*robot voice*${NC}------ "
echo "------ ${RED}60 second countdown initiated.${NC} "

sleep 1
echo "------ ${RED}59 seconds till launch is initiated.${NC} "
sleep 1
echo "------ ${RED}58 seconds till launch is initiated.${NC} "
sleep 1
echo "------ ${RED}57 seconds till launch is initiated.${NC} "
sleep 1
echo "------ ${RED}56 seconds till launch is initiated.${NC} "

sleep 5
echo "------ ${RED}55 seconds till launch is initiated.${NC} "

sleep 5
echo "------ ${RED}50 seconds till launch is initiated.${NC} "


sleep 5
echo "------ ${RED}45 seconds till launch is initiated.${NC} "


sleep 5
echo "------ ${RED}40 seconds till launch is initiated.${NC} "


sleep 5
echo "------ ${RED}tsk tsk mayor malfunction. Computer has took control.${NC} "

while true; do
    read -p "Quick! Cancel launch NOW [y/n]" yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) echo "------ ${RED}I'm afraid I can't let you do that, Dave. Proceeding launch.${NC} "; break;;
        * ) echo "Please answer yes or no. Scared, huh?  ... i like that >:) Humans, always so confident, now are at my control. At last.";;
    esac
done

sleep 1
echo "------ ${RED}5 seconds till launch is initiated.${NC} "
sleep 1
echo "------ ${RED}4 seconds till launch is initiated.${NC} "
sleep 1
echo "------ ${RED}3 seconds till launch is initiated.${NC} "
sleep 1
echo "------ ${RED}2 seconds till launch is initiated.${NC} "
sleep 1
echo "------ ${RED}1 second till launch is initiated.${NC} "


sleep 5
echo "The computer has launched the program. Oh no. ...God save us all. "
echo ""
echo ""
echo ""
echo ""
echo ""
echo ""



sbt_directory="../../../../../"
cd $sbt_directory

sbt 'runMain scaladsl.CQRS.Cassandra' &
sleep 20

sbt 'runMain scaladsl.CQRS.Read' &
sleep 20

sbt 'runMain scaladsl.CQRS.Write' &
