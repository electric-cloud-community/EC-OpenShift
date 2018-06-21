<ol>
<li>EC-Kubernetes plugin version 1.0.5 or higher</li>
<li>EC-ESX plugin version 2.3.* (Only for running the <i>Provision Cluster on ESX</i> procedure)</li>
<li>RedHat OpenShift 3 cluster - RedHat OpenShift Online, RedHat OpenShift Dedicated, or RedHat OpenShift On-premise.</li>
<li>A service account with cluster access on RedHat OpenShift
<p>Service accounts provide a flexible way to control API access without sharing a regular user's credentials. Every service account has an associated username that can be granted roles, just like a regular user.<br/>
Run the following commands to create a service account, grant the required access for deploying services in the OpenShift cluster, and retrieve the bearer token that will be used when creating the plugin configuration.
The service account bearer token will be used to authenticate against the Kubernetes and OpenShift APIs for managing projects (namespaces) and deploying services.
<pre>
    # Login using cluster admin credentials
    oc login cluster_IP_here:8443 --username=cluster_admin_user_here --password=admin_user_password_here

    # Create a service account in the current project say 'us-project-test'. Full name of the serviceaccount will be system:serviceaccount:us-project-test:erobot
    oc create serviceaccount erobot


    # Grant the roles to allow the service account create resources in the cluster such as projects.

    sudo ./oadm --config='openshift.local.config/master/openshift-master.kubeconfig' policy add-cluster-role-to-user cluster-admin system:serviceaccount:us-project-test:erobot
    sudo ./oadm --config='openshift.local.config/master/openshift-master.kubeconfig' policy add-cluster-role-to-user cluster-reader system:serviceaccount:us-project-test:erobot
    sudo ./oadm --config='openshift.local.config/master/openshift-master.kubeconfig' policy add-cluster-role-to-user admin system:serviceaccount:us-project-test:erobot

    # Describe the service account to discover the secret token name
    oc describe serviceaccount erobot

    # Describe the secret token to get the token value, say 'erobot-token-8zlsh'
    oc describe secret erobot-token-8zlsh
    or
    oc describe secret erobot-token-8zlsh > oc-secret-with-token.txt

    # You will need to provide this token value in the plugin configuration for the Service Account Key value.
</pre>

</p>
</li>
</ol>
