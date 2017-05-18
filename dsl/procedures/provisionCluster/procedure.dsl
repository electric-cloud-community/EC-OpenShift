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
   
    step 'Import Master Node',
      command: "",
      releaseMode: 'none',
      projectName: 'EC-OpenShift-1.2.0',
      subprocedure: 'Import',
      subproject: '/plugins/EC-ESX/project',
      errorHandling: 'failProcedure',
      timeLimitUnits: 'minutes', {

	      actualParameter 'connection_config', '$[esx_config]'
		  actualParameter 'esx_datastore', '$[esx_datastore]'
		  actualParameter 'esx_host', '$[esx_host]'
		  actualParameter 'esx_number_of_vms', '1'
		  actualParameter 'esx_source_directory', '/home/vagrant/CentOS7_v4/CentOS7_v4.ovf'
		  actualParameter 'esx_vmname', 'OpenShift-Master'
		  actualParameter 'ovftool_path', '$[ovftool_path]'
		  actualParameter 'esx_vm_poweron', '1'
    
    }

  	step 'Get Master IP', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/getIp.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'


	step 'Import worker Node1',
      command: "",
      releaseMode: 'none',
      projectName: 'EC-OpenShift-1.2.0',
      subprocedure: 'Import',
      subproject: '/plugins/EC-ESX/project',
      errorHandling: 'failProcedure',
      timeLimitUnits: 'minutes', {

	      actualParameter 'connection_config', '$[esx_config]'
		  actualParameter 'esx_datastore', '$[esx_datastore]'
		  actualParameter 'esx_host', '$[esx_host]'
		  actualParameter 'esx_number_of_vms', '1'
		  actualParameter 'esx_source_directory', '/home/vagrant/OpenShift-Node1/OpenShift-Node1.ovf'
		  actualParameter 'esx_vmname', 'OpenShift-Node1'
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
      timeLimitUnits: 'minutes', {

	      actualParameter 'connection_config', '$[esx_config]'
		  actualParameter 'esx_datastore', '$[esx_datastore]'
		  actualParameter 'esx_host', '$[esx_host]'
		  actualParameter 'esx_number_of_vms', '1'
		  actualParameter 'esx_source_directory', '/home/vagrant/OpenShift-Node2/OpenShift-Node2.ovf'
		  actualParameter 'esx_vmname', 'OpenShift-Node2'
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
	  timeLimitUnits: 'minutes'

	step 'provisionCluster', 
	  command: "cd $pluginDir/openshift-ansible; export ANSIBLE_ROLES_PATH=$pluginDir/openshift-ansible/roles;export ANSIBLE_CONFIG=$pluginDir/openshift-ansible/ansible.cfg;ansible-playbook -vvvv $pluginDir/openshift-ansible/playbooks/byo/config.yml -i /tmp/hosts -M $pluginDir/openshift-ansible/library",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  timeLimitUnits: 'minutes'
	  
}
  
