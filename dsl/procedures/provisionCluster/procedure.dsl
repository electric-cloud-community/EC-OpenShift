import java.io.File

procedure 'Provision Cluster on ESX',
	description: 'Provisions a OpenShift cluster. Pods, services, and replication controllers all run on top of a cluster.', {

	step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'call',
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
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/generateHostsFile.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	step 'provisionCluster', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.sh').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	def project_name = '$[project]'
	def service_account = '$[service_account]'
	step 'configureCluster', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/configureCluster.sh').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: "postp --load \$COMMANDER_WORKSPACE/ansible/postp_matchers.pl",
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