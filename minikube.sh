minikube start --vm-driver=virtualbox
eval $(minikube docker-env)
sbt docker:publishLocal

echo # create serviceAccount and role
kubectl apply -f k8s/ms-arditi-rbac.yml --validate=false
echo # create deployment
kubectl apply -f k8s/ms-arditi-deployment.yml --validate=false
echo # create service
kubectl apply -f k8s/ms-arditi-service.yml --validate=false

KUBE_IP=$(minikube ip)
MANAGEMENT_PORT=$(kubectl get svc ms-arditi-cluster -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")
curl http://$KUBE_IP:$MANAGEMENT_PORT/cluster/members | jq
API_PORT=$(kubectl get svc ms-arditi-cluster -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
API=http://$KUBE_IP:$API_PORT/
curl $API
echo $API >> API