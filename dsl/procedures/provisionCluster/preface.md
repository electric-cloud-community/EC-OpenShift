<p>The procedure checks if the OpenShift cluster exists and is reachable with provided details. If not, then provisions a new one.<br/>

The procedure uses ESX plugin's import procedure to setup underlying VMs.For system requirements, see <a href="https://docs.openshift.com/enterprise/3.0/install_config/install/prerequisites.html#install-config-install-prerequisites">here</a>.For networking requirements, see <a href="https://docs.openshift.org/latest/install_config/install/prerequisites.html#prereq-network-access">here</a>.
<br/>

Once VMs are available, the procedure uses <a href="https://github.com/openshift/openshift-ansible">ansible scripts</a> provided by OpenShift to setup an OpenShift cluster. <br/>Below are the prerequisites to use this procedure:
    <ol>
        <li> Ansible (v2.2.0.0) must be installed on CloudBees Flow agent machines and <b>'ansible-playbook'</b> command should be included in PATH. </li>
        <li> <b>'htpasswd'</b> must be installed and included in PATH on CloudBees Flow agent machine.</li>
        <li> CloudBees Flow agent machine should have passwordless SSH access to imported VMs.<br/></li>
        <li>The plugin passes the hostname to OVF template using OVF property "--prop:hostname=some_hostname" while importing using OVF tool.</li>
       <li>The OVF template must have a mechanism (<a href="https://github.com/vmware/open-vm-tools">'open-vm-tools'</a> is one such example) to read this OVF property and set its hostname accordingly. </li>
        <li> It should also add "127.0.0.1 some_hostname some_hostname.domain_name" entry to /etc/hosts file.</li>
        <li> <a href="https://buildlogs.centos.org/centos/7/paas/x86_64/openshift-origin/origin-docker-excluder-1.4.0-2.el7.noarch.rpm">'origin-docker-excluder'</a> and <a href="https://buildlogs.centos.org/centos/7/paas/x86_64/openshift-origin/origin-excluder-1.4.0-2.el7.noarch.rpm">'origin-excluder'</a> RPMs must be already installed on OVF template. </li>
    </ol>
</p>
