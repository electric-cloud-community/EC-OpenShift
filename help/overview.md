EC-OpenShift plugin integrates with the <a href="https://www.openshift.com/">OpenShift container platform</a> allowing you to coordinate and manage microservices-based applications deployments and releases on both on-premise and hosted OpenShift offerings.
<ul>
    <li>
        In case of on-premise setup, plugin helps you install <a href="https://www.openshift.org/">OpenShift Origin</a> on VMWare setup right from importing underlying hosts (VMs) from OVF template, installing OpenShift Origin using <a href="https://github.com/openshift/openshift-ansible">Ansible scripts</a> and performs post installation steps like creation of project, service accout and router (with same name as project name). This makes deployment of any micro-service simple and straight forward. For the detailed requirements about OVF template, see <a href="#ProvisionClusterOnESX">Provision Cluster on ESX procedure details.</a>
    <br/> Plugin also supports various installation topologies based on requirements. Supported topologies are:
    <br/>
        <ul>
            <li><b>Minimal</b> : Sets up one OpenShift master, embedded etcd and user supplied number of nodes. Suitable for setting up OpenShift cluster for evaluation purpose or for development setup.  </li>
            <li><b>Multiple Etcd</b> : Sets up one OpenShift master, separate cluster of three etcd instances and user supplied number of nodes.</li>
            <li><b>Multiple Etcd + HAProxy</b> : Highly available setup with three OpenShift masters, cluster of three etcd instances and user supplied number of nodes. Suitable for production environment.</li>
        </ul>
    </li>
    <li>
        For hosted OpenShift offerings, plugin assumes that you have a service account which has full access on projects that you intend to use through this plugin.
    </li>
</ul>
