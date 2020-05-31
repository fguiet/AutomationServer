# Kubernetes principles

## Volumes

hostPath : Volume du host monté dans le pod (dans le conteneur), attention quand un pod bouge d'un serveur à un autre
emptyDir : Partage entre containers du meme pod. emptyDir peut être de la RAM

## Deployment

Représentation logique de un plusieurs pods (configuration)

## Services

Moyen d'accéder au pods

* NodePort

Idéal pour ouvrir une app à l'extérieur du cluster

En gros, ca créé un service que l'on peut appeler depuis n'importe quel noeud k8s worker. Le service est mappé avec l'application d'un pod en utilisant des labels

Si on a qu'un noeud worker et plusieurs réplicas de l'application alors k8s utilisera un algo aléatoire pour choisir le pod qui répondra

Si on a plusieurs noeuds k8s avec plusieurs replica d'un pod (exemple nginx), on pourra alors appeller n'importe quelle IP d'un noeud k8s worker sur le port du service définit (entre 30000 et 32767). Par contre, il faut connaitre les ip des worker nodes (pas de notion de loadbalancing ici).
!!!Même si le worker node que l'on appelle ne fait pas tourner de pod avec l'app ca serait automatiquement rediriger vers un worker node qui fait tourner l'app!!!

L'inconvénient en prod est qu'on ne veut pas fournir l'ip des worker nodes aux end users!!!

* ClusterIP

Pour faire communiquer des services en interne (front-end / backend) = pas d'ouverture à l'extérieure

* LoadBalancer

Use a Kubernetes service of type LoadBalancer, which creates an external load balancer that points to a Kubernetes service in your cluster

* Ingress

Kubernetes ingress is a collection of routing rules that govern how external users access services running in a Kubernetes cluster

Voir article <https://blog.getambassador.io/kubernetes-ingress-nodeport-load-balancers-and-ingress-controllers-6e29f1c44f2d>

Ca peut via `Nginx`, `HAProxy`, `Traefik`

[Nginx Ingress](https://www.youtube.com/watch?v=chwofyGr80c)

Anyway an external load balancer is required like `HAProxy`

### References

<https://www.youtube.com/watch?v=5lzUpDtmWgM>

<https://www.youtube.com/watch?v=J30_ZdaEXbw>

Load Balancer service
https://www.youtube.com/watch?v=xCsz9IOt-fs&
list=PLMPZQTftRCS8Pp4wiiUruly5ODScvAwcQ&index=25&t=0s

[Traefik Ingress](https://www.youtube.com/watch?v=A_PjjCM1eLA)

[Nginx Ingress](https://www.youtube.com/watch?v=chwofyGr80c)


