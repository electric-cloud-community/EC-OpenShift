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


    step 'Generate Certs', {
	    description = ''
	    alwaysRun = '0'
	    broadcast = '0'
	    command = "htpasswd -b -c passwordfile test test"
	    errorHandling = 'failProcedure'
	    exclusiveMode = 'none'
	    logFileName = ''
	    parallel = '0'
	    projectName = 'EC-OpenShift-1.2.0'
	    releaseMode = 'none'
	    subprocedure = null
	    subproject = null
	    timeLimitUnits = 'minutes'
	    
  	}

    

  	step 'Get Master IP', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/getIp.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

	step 'generateHostsFile', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

	step 'provisionCluster', 
	  command: "",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
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
	  
	def ip = '$[OpenShiftMasterIP]',
	    config_name = '$[plugin_config_name]',
	    service_token = '$[service_token]'

	step 'createPluginConfiguration', 
	  command: "ectool evalDsl --dslFile $pluginDir/dsl/createPluginConfig.dsl --parameters '{\"ip\":\"${ip}\",\"config_name\":\"${config_name}\",\"service_token\":\"${service_token}\"}'",
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: "postp",
	  releaseMode: 'none',
	  timeLimitUnits: 'minutes'
	
}
  
