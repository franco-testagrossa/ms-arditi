# kubectl delete pods,deployments,services,daemonsets,replicasets --all
# minikube stop
# minikube delete

minikube start
eval $(minikube docker-env)
sbt docker:publishLocal

echo # create serviceAccount and role
kubectl apply -f k8s/ms-arditi-rbac.yml --validate=false
echo # create deployment
kubectl apply -f k8s/ms-arditi-deployment.yml --validate=false
echo # create service
kubectl apply -f k8s/ms-arditi-service.yml --validate=false

echo "Sleeping 30 seconds for good measure..."
sleep 30
KUBE_IP=$(minikube ip)
MANAGEMENT_PORT=$(kubectl get svc ms-arditi-cluster -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")
curl http://$KUBE_IP:$MANAGEMENT_PORT/cluster/members | jq
API_PORT=$(kubectl get svc ms-arditi-cluster -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
API=http://$KUBE_IP:$API_PORT/

echo "Sleeping 30 seconds for good measure..."
sleep 30


curl $API

echo ""
echo ""
echo $API