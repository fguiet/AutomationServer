# Setting up Automation Server using Kubernetes

## Adding external disk drive to my k8s master node and share volume using NFS

First plug your external disk using one of the Raspberry 4 USB3 port (the blue one). Your hard disk should be USB3 comptabible of course.

Type :

```bash
sudo blkid
```

You shoud see that your device is connected 

```bash
/dev/sda1: UUID="78478eaa-5d12-49d1-aa28-2a7cc158ce34" TYPE="ext4" PARTUUID="788f9eee-01"
```

Create a folder so you can mount your hard drive

```bash
mkdir -p /mnt/touros-ext-drive
```

Automounting drive on boot

Edit `/etc/fstab` file

add the following line

```bash
UUID="78478eaa-5d12-49d1-aa28-2a7cc158ce34" /mnt/touros-ext-drive ext4 defaults    0 0
```

Now you can type to mount your external drive and see the content

```bash
sudo mount -a
cd /mnt/touros-ext-drive/
ls
```

## Sharing NFS folder accross Kubernetes cluster

First, let's install NFS Server one of your machine (I choose to use my k8s master node)

```bash
sudo apt-get install nfs-kernel-server -y
```

Once this is done, one need to enable the `nfs-server` service and start it

```bash
sudo systemctl enable nfs-server
sudo systemctl start nfs-server
sudo systemctl status nfs-server
```

Let's create a folder first and change rights (I do a 777 for testing purpose, this need to be sorted out)

```bash
mkdir -p /mnt/touros-ext-drive/k8s-data
#Don't do that in a production environment!
chmod 777 -R /mnt/touros-ext-drive/k8s-data
```

Export this folder to the world (don't do that either on production)

```bash
sudo vim /etc/exports
```

Copy the following line 

```bash
#no_root_squash : permet au client NFS de faire de modification
#sur le partage en tant que root (ex : changement de owership d'un répertoire...sans cette option l'action n'est pas autorisée)
/mnt/touros-ext-drive/k8s-data *(rw,sync,no_subtree_check,insecure,no_root_squash)
```

Exports this configuration so it became available, and now you're done !

```bash
sudo exportfs -ra
#Check if NFS has been done !
sudo exportfs -v
```

To check whether this NFS share is available on worker nodes
Connect to a worker node and type (you should see the NFS export)

```bash
fred@k8s-worker-01:~ $ showmount -e 192.168.1.3
Export list for 192.168.1.3:
/mnt/touros-ext-drive/k8s-data *
```

You can even try to mount it 

```bash
sudo mount -t nfs 192.168.1.3:/mnt/touros-ext-drive/k8s-data /mnt
```

## Creating local DNS server with PiHole

Get a spare raspberry with a static IP set and Docker installed

To prevent `/etc/resolv.conf` being overwritten, do as follow

Disable `systemd-resolved`

```bash
sudo systemctl disable systemd-resolved
sudo systemctl stop systemd-resolved
```

On every raspberry client, edit file `/etc/dhcpcd.conf` and check line `static domain_name_servers`, it should be 

```bash
static domain_name_servers=ip of your PiHole
```

Retstart all you raspberry and check that `/etc/resolv.conf` contains :

```bash
nameserver ip of your PiHole
```

Sometimes (dunno why?), `/etc/resolv.conf` keeps on being overwritten...in that cas edit the file manually and make the file read-only (if it is a symbolic link remove it and make a regular `/etc/resolv.conf` file)

```bash
sudo chattr +i resolv.conf
```

Launch [`PiHole`](../docker/pihole/docker-compose.yml) docker 

```bash
docker-compose up -d
```

__Note__
`etc/resolv.conf` on PiHole should be set to `nameserver 127.0.0.1` (otherwise DNS resolution should not work inside HAProxy container)

__Don't forget to turn off your Internet provider DHCP and turn on PiHole DHCP__

DHCP will be self dicovered by all your device (DHCP runs with broadcast, no need to specify the IP of the DHCP server on clients)

## Using HAProxy as LoadBalancer

Great series about HAProxy : <https://www.youtube.com/playlist?list=PLn6POgpklwWrY1WZqtwKog6pFuxcWDoNy>

Launch [`HAProxy`](../docker/haproxy/docker-compose.yml) docker 

Statistiques are available by browsing:

Statistiques : <http://192.168.1.27/stats>

## Ingress controller

Principles:

Ingress Controller = DaemonSet or Deployment. It is deployed on each worker node

Ingress Resources = the Ingress Controller will check Ingress Resources rules and route the http call to the matching service cluster IP

Service (ClusterIP) = balance traffic across pods (bind to Ingress Resources)

Typically, we have:

HAProxy (80/443) => WorkerNode IP loadbalancer => pod/daemonset nginx ingress controller => ingress resources => cluster ip service loadbalancer => pods 

Tutorial Ingress Controller Nginx : 

* xavki : <https://www.youtube.com/watch?v=0RTxzGCLpPE&list=PLn6POgpklwWqfzaosSgX2XEKpse5VY2v5&index=41>
* Just me and opensource : <https://www.youtube.com/watch?v=A_PjjCM1eLA>

* Creating Ingress Controller (Traefik)

Documentation : <https://docs.traefik.io/v1.7/user-guide/kubernetes/>

On Master node, do as follow:

```bash
kubectl apply -f https://raw.githubusercontent.com/containous/traefik/v1.7/examples/k8s/traefik-rbac.yaml

#Deploy DaemonSet
kubectl apply -f https://raw.githubusercontent.com/containous/traefik/v1.7/examples/k8s/traefik-ds.yaml
```

## Kubernetes Dashboard

Il suffit de suivre la documentation: <https://kubernetes.io/fr/docs/tasks/access-application-cluster/web-ui-dashboard/>

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/master/aio/deploy/recommended.yaml
```

Vidéo Youtube : <https://www.youtube.com/watch?v=6MnsSvChl1E&t=108s>

* Accéder au dashboard

Pour accéder au dashboard, il faut changer le type du service `kubernetes-dashboard` de `clusterIP` à `nodePort`

```bash
#Description du service kubernetes-dashboard
kubectl -n kubernetes-dashboard describe service kubernetes-dashboard
```

```bash
#Edition du service pour change son type
kubectl -n kubernetes-dashboard edit service kubernetes-dashboard
```

Ajout d'une entrée DNS dans `PiHole` : `kubernetes.guiet.lan` => `197.168.1.27` (`HAproxy`)

Ajout d'une entrée dans `HAProxy`:

Création d'un compte de service avec droits d'administration pour accéder au dashboard

```bash
#Récupération du fichier https://github.com/justmeandopensource/kubernetes/blob/master/dashboard/sa_cluster_admin.yaml
kubectl apply -f sa_cluster_admin.yaml
```

Récupération du Token

```bash
#Get all service account
kubectl -n kube-system get sa
```

```bash
#Get dashboard-admin service account token
kubectl -n kube-system describe sa dashboard-admin
```

```bash
#Get secret token
kubectl -n kube-system describe secret dashboard-admin-token-4bfj2
```

## Grafana deployment

Let's create a folder on our NFS sharing for the openhab PersistentVolume (on our master node)

```bash
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/grafana-pv
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/grafana-pv/logs
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/grafana-pv/data
#Grafana use user with uid 472 internally
sudo chown 472:472 grafana-pv/ -R
```

Creating the Grafana persistent volume

```bash
kubectl create -f grafana-nfs-pv.yaml
```

Check PV create PV on k8s cluster

```bash
kubectl get pv
```

Creating the Grafana persistent volume claim

```bash
kubectl create -f grafana-nfs-pvc.yaml
```

How to configure Grafana with Docker: <https://grafana.com/docs/grafana/latest/installation/configure-docker/>

## Node-RED deployment

Let's create a folder on our NFS sharing for the openhab PersistentVolume (on our master node)

```bash
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/node-red-pv
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/node-red-pv/data
#Node-RED use user with uid 1000 internally
sudo chown 1000:1000 /mnt/touros-ext-drive/k8s-data/node-red-pv/ -R
```

Creating the Node-RED persistent volume

```bash
kubectl create -f node-red-nfs-pv.yaml
```

Check PV create PV on k8s cluster

```bash
kubectl get pv
```

Creating the Node-RED persistent volume claim

```bash
kubectl create -f node-red-nfs-pvc.yaml
```

How to configure Node-RED with Docker: https://nodered.org/docs/getting-started/docker

To update node libraries used in flows (BigTimer for instance), go to NodeRed website and use `Manage palette` menu, select the node you want to update and click the update button it is that simple.

## Openhab deployment

### Creating PersistentVolume

Let's create a folder on our NFS sharing for the openhab PersistentVolume (on our master node)

```bash
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/openhab-pv
```

Creating the Openhab persistent volume

```bash
kubectl create -f openhab-nfs-pv.yaml
```

Check PV create PV on k8s cluster

```bash
kubectl get pv
```

```bash
NAME             CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS      CLAIM   STORAGECLASS     REASON   AGE
openhab-nfs-pv   500Mi      RWO            Retain           Available           openhab-nfs-pv            19s
```

Creating the Openhab persistent volume claim

```bash
kubectl create -f openhab-nfs-pvc.yaml
```

### Creating `openhab` user

```bash
#nfs rights
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/openhab-pv
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/openhab-pv/conf
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/openhab-pv/userdata
sudo mkdir -p /mnt/touros-ext-drive/k8s-data/openhab-pv/addons
#Openhab userid 9001
sudo chmod 9001:9001 /mnt/touros-ext-drive/k8s-data/openhab-pv
```

### Deploying openhab

Lancer la création d'un réplica du pod `openhab`

```bash
kubectl create -f openhab-deployment.yaml
```

Pour avoir une descript du pod en cours de déploiement

```bash
kubectl describe pods openhab-deployment-76d595fd8f-t7r6c
```





