docker-compose down
docker-compose up -d

minikube delete
minikube start

eval $(minikube docker-env)
sbt docker:publishLocal

kubectl apply -f k8s/ms_arditi-rbac.yml --validate=false
kubectl apply -f k8s/ms_arditi-deployment.yml --validate=false
kubectl apply -f k8s/ms_arditi-service.yml --validate=false

sleep 30


KUBE_IP=$(minikube ip)
API_PORT=$(kubectl get svc ms_arditi-cluster -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
API=http://$KUBE_IP:$API_PORT

echo "Sleeping 30 seconds for good measure..."
sleep 60

sbt 'runMain transaction.ProducerApp' 2>/dev/null

echo "Sleeping 30 seconds for good measure..."
sleep 60

echo # Run this command! -- may take a while to see results!
echo curl $API/state/Person/2