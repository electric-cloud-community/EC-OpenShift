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
        projectName: 'EC-OpenShift-1.2.0',
        errorHandling: 'failProcedure',
        condition: '$[openshiftNotPresent]',
        timeLimitUnits: 'minutes'


    step 'Import Master Node',
      command: "",
      releaseMode: 'none',
      projectName: 'EC-OpenShift-1.2.0',
      subprocedure: 'Import',
      subproject: '/plugins/EC-ESX/project',
      errorHandling: 'failProcedure',
      condition: '$[openshiftNotPresent]',
      timeLimitUnits: 'minutes', {

	      actualParameter 'connection_config', '$[esx_config]'
		  actualParameter 'esx_datastore', '$[esx_datastore]'
		  actualParameter 'esx_host', '$[esx_host]'
		  actualParameter 'esx_number_of_vms', '1'
		  actualParameter 'esx_properties', 'hostname=$[openshift_public_hostname]'
		  actualParameter 'esx_vm_memory', '$[master_memory]'
		  actualParameter 'esx_vm_num_cpus', '$[master_cpu]'
		  actualParameter 'esx_source_directory', '/home/vagrant/CentOS7/CentOS7.ovf'
		  actualParameter 'esx_vmname', 'OpenShift-$[openshift_public_hostname]'
		  actualParameter 'ovftool_path', '$[ovftool_path]'
		  actualParameter 'esx_vm_poweron', '1'
		  actualParameter 'esx_properties_location', '/myJob/ESX'

  	step 'Get Master IP', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/getIp.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'


	step 'Import worker Node1',
      command: "",
      releaseMode: 'none',
      projectName: 'EC-OpenShift-1.2.0',
      subprocedure: 'Import',
      subproject: '/plugins/EC-ESX/project',
      errorHandling: 'failProcedure',
      condition: '$[openshiftNotPresent]',
      timeLimitUnits: 'minutes', {

	      actualParameter 'connection_config', '$[esx_config]'
		  actualParameter 'esx_datastore', '$[esx_datastore]'
		  actualParameter 'esx_host', '$[esx_host]'
		  actualParameter 'esx_number_of_vms', '1'
		  actualParameter 'esx_properties', 'hostname=$[node-1]'
		  actualParameter 'esx_vm_memory', '$[node_memory]'
		  actualParameter 'esx_vm_num_cpus', '$[node_cpu]'
		  actualParameter 'esx_source_directory', '/home/vagrant/CentOS7/CentOS7.ovf'
		  actualParameter 'esx_vmname', 'OpenShift-$[node-1]'
		  actualParameter 'ovftool_path', '$[ovftool_path]'
		  actualParameter 'esx_vm_poweron', '1'
    
    }

    step 'Import worker Node2',
      command: "",
      releaseMode: 'none',
      projectName: 'EC-OpenShift-1.2.0',
      subprocedure: 'Import',
      subproject: '/plugins/EC-ESX/project',
      errorHandling: 'failProcedure',
      condition: '$[openshiftNotPresent]',
      timeLimitUnits: 'minutes', {

	      actualParameter 'connection_config', '$[esx_config]'
		  actualParameter 'esx_datastore', '$[esx_datastore]'
		  actualParameter 'esx_host', '$[esx_host]'
		  actualParameter 'esx_number_of_vms', '1'
		  actualParameter 'esx_properties', 'hostname=$[node-2]'
		  actualParameter 'esx_vm_memory', '$[node_memory]'
		  actualParameter 'esx_vm_num_cpus', '$[node_cpu]'
		  actualParameter 'esx_source_directory', '/home/vagrant/CentOS7/CentOS7.ovf'
		  actualParameter 'esx_vmname', 'OpenShift-$[node-2]'
		  actualParameter 'ovftool_path', '$[ovftool_path]'
		  actualParameter 'esx_vm_poweron', '1'
    
    }

	step 'generateHostsFile', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'

	step 'provisionCluster', 
	  command: "",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  condition: '$[openshiftNotPresent]',
	  timeLimitUnits: 'minutes'
	
    def project_name = '$[project]'
	step 'configureCluster', 
	  command: "ansible-playbook $pluginDir/ansible-scripts/get_service_token.yml -i /tmp/hosts --extra-vars \"project_name=$project_name\"",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: "postp --load $pluginDir/dsl/postp_matchers.pl",
	  releaseMode: 'none',
	  timeLimitUnits: 'minutes'
	
    def project_name = '$[project]'
	step 'configureCluster', 
	  command: "ansible-playbook $pluginDir/ansible-scripts/get_service_token.yml -i /tmp/hosts --extra-vars \"project_name=$project_name\"",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: "postp --load $pluginDir/dsl/postp_matchers.pl",
	  releaseMode: 'none',
	  timeLimitUnits: 'minutes'

	step 'createPluginConfiguration', 
	  command: new File(pluginDir, 'dsl/createPluginConfig.dsl').text,	 
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  shell: 'ectool evalDsl --dslFile {0}',
	  postProcessor: "postp",
	  releaseMode: 'none',
	  timeLimitUnits: 'minutes'
	
}
  
