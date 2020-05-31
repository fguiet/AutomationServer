## Kubernetes commands reminder

Reference : <https://kubernetes.io/fr/docs/reference/kubectl/cheatsheet/>

* Exposer un deployment 

Cette commande créée un service de type cluster ip 

``bash
kubectl expose deploy openhab-deployment --port=80
kubectl get services
```

Output sample

```bash
NAME                 TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
kubernetes           ClusterIP   10.96.0.1       <none>        443/TCP        8d
openhab-deployment   ClusterIP   10.106.225.37   <none>        80/TCP         2m38s
```

* Se connecter à un pod

```bash
kubectl exec -it openhab-deployment-85456d6fbf-v7p2w -- bash
```

* Accès aux logs d'un pod

```bash
kubectl logs [podname] -p
```

* Evénements dans le cluster

```bash
kubectl get events
```

Output sample

```bash
LAST SEEN   TYPE     REASON              OBJECT                                     MESSAGE
<unknown>   Normal   Scheduled           pod/openhab-deployment-76d595fd8f-t7r6c    Successfully assigned default/openhab-deployment-76d595fd8f-t7r6c to k8s-worker-01
16m         Normal   Pulling             pod/openhab-deployment-76d595fd8f-t7r6c    Pulling image "openhab/openhab:2.5.4-armhf-debian"
16m         Normal   SuccessfulCreate    replicaset/openhab-deployment-76d595fd8f   Created pod: openhab-deployment-76d595fd8f-t7r6c
16m         Normal   ScalingReplicaSet   deployment/openhab-deployment              Scaled up replica set openhab-deployment-76d595fd8f to 1
```

* Cluster info

```bash
kubectl cluster-info
```

Output sample

```bash
Kubernetes master is running at https://192.168.1.3:6443
KubeDNS is running at https://192.168.1.3:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
```

* Cluster version

```bash
kubectl version --short
```

Output sample

```bash
Client Version: v1.18.2
Server Version: v1.18.2
```

* Showing nodes of the cluster

```bash
kubectl get nodes
```

```bash
kubectl get nodes -o wide
```

Output sample

```bash
NAME            STATUS   ROLES    AGE   VERSION
k8s-master-01   Ready    master   36h   v1.18.2
k8s-worker-01   Ready    <none>   36h   v1.18.2
k8s-worker-02   Ready    <none>   36h   v1.18.2
```

* Getting services running on the cluster

```bash
kubectl get service -l app=nginx-app
```

* Describing a service

```bash
kubectl describe service my-service
```

* Get k8s namespaces

```bash
kubectl get ns
```

* Get all ressources in a namespace

```bash
kubectl get all -n <namespace>
```