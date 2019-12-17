import java.io.File

procedure 'Check Cluster',
	description: 'Checks that the configured OpenShift cluster is accessible using the given service account bearer token.', {

	step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'flowpdk-setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes'

   step 'checkCluster',
      command: new File(pluginDir, 'dsl/procedures/checkCluster/steps/checkCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
}