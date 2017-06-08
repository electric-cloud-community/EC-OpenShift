import java.io.File

procedure 'Provision Cluster', 
	description: 'Provisions a OpenShift cluster. Pods, services, and replication controllers all run on top of a cluster.', {

	step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

    	  actualParameter 'additionalArtifactVersion', 'com.electriccloud:EC-OpenShift-Grapes:1.0.0'
    }

   step 'Prepare Setup', 
      command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/prepare.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

	step 'Install Ansible Playbooks', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/installPlaybooks.pl').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-perl',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'
   
   step 'Generate Certs',
	  command: "htpasswd -b -c passwordfile test test",
	  releaseMode: 'none',
	  errorHandling: 'failProcedure',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'
	    
   step 'Import VMs', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/generateSteps.pl').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-perl',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	step "generateHostsFile", 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	step 'provisionCluster', 
	  command: "cd \$COMMANDER_DATA/ansible/openshift-ansible; export ANSIBLE_ROLES_PATH=\$COMMANDER_DATA/ansible/openshift-ansible/roles;export ANSIBLE_CONFIG=\$COMMANDER_DATA/ansible/openshift-ansible/ansible.cfg;export ANSIBLE_FILTER_PLUGINS=\$COMMANDER_DATA/ansible/openshift-ansible/filter_plugins;ansible-playbook -vvvv \$COMMANDER_DATA/ansible/openshift-ansible/playbooks/byo/config.yml -i /tmp/hosts -M \$COMMANDER_DATA/ansible/openshift-ansible/library",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	def project_name = '$[project]'
	def service_account = '$[service_account]'
	step 'configureCluster', 
	  command: "ansible-playbook \$COMMANDER_DATA/ansible/ansible-scripts/get_service_token.yml -i /tmp/hosts --extra-vars \"project_name=$project_name service_account_name=$service_account\"",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: "postp --load \$COMMANDER_DATA/ansible/postp_matchers.pl",
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'
	
	step 'createPluginConfiguration', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/createPluginConfig.dsl').text,	 
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  shell: 'ectool evalDsl --dslFile {0}',
	  postProcessor: "postp",
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	step 'Wait for PluginConfig to populate',
        command: "sleep 30s",
        releaseMode: 'none',
        errorHandling: 'failProcedure',
        condition: '$[openshiftNotPresent]',
        timeLimitUnits: 'minutes'

}