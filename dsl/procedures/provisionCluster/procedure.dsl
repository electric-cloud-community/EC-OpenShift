import java.io.File

procedure 'Provision Cluster on ESX',
	description: 'Provisions a OpenShift cluster. Pods, services, and replication controllers all run on top of a cluster.', {

	step 'setup',
      subprocedure: 'flowpdk-setup',
      command: null,
      subproject: '/plugins/EC-Kubernetes/project',
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes'

   step 'Prepare Setup',
      command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/prepare.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

	step 'Install Ansible Playbooks',
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/installPlaybooks.pl').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-perl',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

   step 'Generate Certs',
	  command: "htpasswd -b -c passwordfile test test",
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  errorHandling: 'failProcedure',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

   step 'Import VMs',
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/generateSteps.pl').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-perl',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	step "generateHostsFile",
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/generateHostsFile.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-groovy',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	step 'provisionCluster',
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.sh').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
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
	  resourceName: '$[grabbedResource]',
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
