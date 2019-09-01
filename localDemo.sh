docker-compose down
docker-compose up -d

sleep 10

sbt produce 2>/dev/null

sleep 10

sbt main
echo # Run this command! -- may take a while to see results!
echo curl $API/state/Person/2