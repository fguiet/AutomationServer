# Creating Kubernetes cluster from Raspberry 4

## Hardware requirement

* x3 - Raspberry 4 (4giga RAM)
* x3 - USB to 
* x3 - 32 giga SD cards
* x1 - Gigabit Ethernet switch
* x? - RJ45 Ethernet cables
* Cluster Case (to keep everything nice and clean)
* USB Charger

## Sofware requirement

* [Etcher](https://www.balena.io/etcher/)
* [Raspbian Buster lite](https://www.raspberrypi.org/downloads/raspbian/)
* [MobaXTerm](https://mobaxterm.mobatek.net/download-home-edition.html)
* [Advanced IP Scanner](https://www.advanced-ip-scanner.com/fr/)

## Setting up Raspbian

1. Install `Etcher`

2. Install `MobaXTerm`

3. Install `Advanced IP Scanner`

4. Preparing `Raspbian`

    * Flashing `Raspbian`

        Follow Step 1,2,3 of this tutorial : <https://desertbot.io/blog/headless-raspberry-pi-4-ssh-wifi-setup>

    * Insert SD Cards into your Raspberry cluster

    * Prepare your cluster case (connect your Raspberry to your switch and to the USB Charger)

5. Use `Advanced IP Scanner` to look for new Raspberry IPs

6. Use `MobaXTerm` to connect to each Raspberry, then enter Multi-execution mode

7. Configure static IP/hostname

On each file, raspberry

* Edit file `/etc/hostname`

```bash
sudo nano /etc/hostname
```

Change hostname replace `raspberrypi` by

`k8s-master-01`
`k8s-worker-02`
`k8s-worker-02`

* Edit file `/etc/hosts`

```bash
sudo nano /etc/hosts
```

Change hostname replace `raspberrypi` by 

`k8s-master-01`
`k8s-worker-02`
`k8s-worker-02`

* Edit file `/etc/dhcpcd.conf`

```bash
sudo nano /etc/dhcpcd.conf
```

At the end of the file add

```bash
# static IP configuration:
interface eth0
static ip_address=192.168.1.3/24
# static ip6_address=fd51:42f8:caae:d92e::ff/64
static routers=192.168.1.1
static domain_name_servers=1.1.1.1 192.168.1.1
```

8. Update, upgrade raspbian distribution

```bash
sudo apt update && sudo apt dist-upgrade
```

9. Ajust timezone with `raspi-config`

```bash
sudo raspi-config
```

10. Edit file /boot/cmdline.txt

Add this to the end of the file

```bash
cgroup_enable=cpuset cgroup_memory=1 cgroup_enable=memory
```

11. Disable swap

Swap is enable...

```bash
free -m
```

Disable swap

```bash
sudo dphys-swapfile swapoff 
sudo dphys-swapfile uninstall 
sudo apt purge dphys-swapfile
```

12. Reboot all raspberry

```bash
sudo reboot
```

13. Install `Docker`

```bash
curl -sSL get.docker.com | sh
```

14. Modification de la configuration `Docker`

```bash
sudo vim /etc/docker/daemon.json
```

Paste

```bash
{
   "exec-opts": ["native.cgroupdriver=systemd"],
   "log-driver": "json-file",
   "log-opts": {
     "max-size": "100m"
   },
   "storage-driver": "overlay2"
 }
```

15. Add a new user

```bash
sudo adduser fred
```

Add all privilege for this user

```bash
sudo visudo
```

Enter : `fred    ALL=(ALL:ALL) ALL` after `root` user

Add user to docker group

```bash
sudo usermod -aG docker fred
```

Logout and login again so above commands to take effect
```bash
logout
```

16. Activating IP forwarding (your raspberry acts as a router)

```bash
sudo nano /etc/sysctl.conf
sudo sysctl -p /etc/sysctl.conf
```

## Installing Kubernetes

1. Add `Kubernetes repository`

```bash
sudo apt-get install vim
sudo nano /etc/apt/sources.list.d/kubernetes.list
```

Add

```bash
deb http://apt.kubernetes.io/ kubernetes-xenial main
```

Add the GPG key to the Pi

```bash
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
```

2. Install needed `Kubernetes` package

```bash
#This command may result with an error, just start it again...
sudo apt update
sudo apt install kubeadm kubectl kubelet
```

3. Only on the master node

Initialize Kubernetes cluster (this may require a long time, please be patient...)

```bash
#This may take a while !
sudo kubeadm init --pod-network-cidr=10.244.0.0/16

mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

4. Install a network driver (here Flannel, but you can use Calico, Weave, etc)

Flannel seemes to work well on Raspberry

```bash
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

5. Check all pods come up

```bash
kubectl get pods --all-namespaces
```

If everything's ok, every pods should be in running states ! 
Flannel may take a while to init...

```bash
NAMESPACE     NAME                                    READY   STATUS    RESTARTS   AGE
kube-system   coredns-66bff467f8-8ch7f                1/1     Running   0          7m33s
kube-system   coredns-66bff467f8-f8kgh                1/1     Running   0          7m33s
kube-system   etcd-k8s-master-01                      1/1     Running   0          7m40s
kube-system   kube-apiserver-k8s-master-01            1/1     Running   0          7m40s
kube-system   kube-controller-manager-k8s-master-01   1/1     Running   0          7m40s
kube-system   kube-flannel-ds-arm-wm7q4               1/1     Running   0          2m51s
kube-system   kube-proxy-r28lf                        1/1     Running   0          7m32s
kube-system   kube-scheduler-k8s-master-01            1/1     Running   0          7m40s
```

6. Joining wokers node to the cluster 

```bash
sudo kubeadm join 192.168.1.3:6443 --token xxxx     --discovery-token-ca-cert-hash xxxx
```

7. Check if nodes joined and are ready

On Master node

```bash
kubectl get nodes
```

```bash
NAME            STATUS   ROLES    AGE   VERSION
k8s-master-01   Ready    master   23m   v1.18.2
k8s-worker-01   Ready    <none>   11m   v1.18.2
k8s-worker-02   Ready    <none>   11m   v1.18.2
```

## Glossary

`master node` :

* Manage all the Kubernetes cluster. 
* Monitor the nodes and containers. Plan where each containers goes where
* Store information regarding the different nodes
* Control plain components are used to carry out those tasks

`etcd` : 

* key/pair databse holding information about the cluster

`kube-scheduler` : 

* responsible of placing container on the right node (according to capacity, etc...)

`node-controller` : 

* take care of nodes (handle situation when node become non available, etc)

`replication-controller` : 

* responsible of motoring how many containers are running

`kube-apiserver` : 

* orchestre all operation across the cluster, management

`kubelet` : 

* Agent running on each node of a cluster (communicate important information with master node, status of the containers, listen to the `kube-apiserver`, etc)

`kube-proxy` : 

* service that allows containers to communicate with each others across the cluster

`pod` :

* A Pod (as in a pod of whales or pea pod) is a group of one or more containers (such as Docker containers), with shared storage/network, and a specification for how to run the containers


