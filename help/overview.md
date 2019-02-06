EC-OpenShift plugin integrates with the [OpenShift container platform](https://www.openshift.com/) and helps you manage microservices based applications deployments and releases on both on-premise as well as hosted OpenShift offerings.

In case of on-premise setup, plugin helps you in addition to install [OpenShift Origin on VMWare](https://www.openshift.org/). Plugin supports the following topologies:

* __Minimal__: Sets up one OpenShift master, embedded etcd and user supplied number of nodes. This is typically used for creating an OpenShift cluster for a Development environment .
* __Multiple Etcd__: Sets up one OpenShift master, separate cluster of three etcd instances and user supplied number of nodes.
* __Multiple Etcd + HAProxy__: Highly available setup with three OpenShift masters, cluster of three etcd instances and user supplied number of nodes. This is meant for a production
environment.

