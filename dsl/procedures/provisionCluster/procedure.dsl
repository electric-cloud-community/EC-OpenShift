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
   
   
	step "generateHostsFile", 
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
	
}