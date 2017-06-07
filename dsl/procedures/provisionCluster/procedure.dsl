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
	  command: "cd $pluginDir/openshift-ansible; export ANSIBLE_ROLES_PATH=$pluginDir/openshift-ansible/roles;export ANSIBLE_CONFIG=$pluginDir/openshift-ansible/ansible.cfg;ansible-playbook -vvvv $pluginDir/openshift-ansible/playbooks/byo/config.yml -i /tmp/hosts -M $pluginDir/openshift-ansible/library",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	def project_name = '$[project]'
	def service_account = '$[service_account]'
	step 'configureCluster', 
	  command: "ansible-playbook $pluginDir/ansible-scripts/get_service_token.yml -i /tmp/hosts --extra-vars \"project_name=$project_name service_account_name=$service_account\"",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: "postp --load $pluginDir/dsl/procedures/provisionCluster/steps/postp_matchers.pl",
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'
	
	step 'createPluginConfiguration', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/createPluginConfig.pl').text,	 
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  shell: 'ec-perl',
	  postProcessor: "postp",
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

}