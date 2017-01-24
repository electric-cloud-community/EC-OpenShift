import java.io.File

procedure 'CreateConfiguration',
        description: 'Creates a configuration for OpenShift cluster', {

	step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

    	  actualParameter 'additionalArtifactVersion', 'com.electriccloud:EC-OpenShift-Grapes:1.0.1'
    }

    step 'testConnection',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/testConnection.groovy').text,
            errorHandling: 'abortProcedure',
            exclusiveMode: 'none',
            postProcessor: 'postp',
            releaseMode: 'none',
            shell: 'ec-groovy',
            timeLimitUnits: 'minutes'

    step 'createConfiguration',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createConfiguration.pl').text,
            errorHandling: 'abortProcedure',
            exclusiveMode: 'none',
            postProcessor: 'postp',
            releaseMode: 'none',
            shell: 'ec-perl',
            timeLimitUnits: 'minutes'

    step 'createAndAttachCredential',
        command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createAndAttachCredential.pl').text,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        releaseMode: 'none',
        shell: 'ec-perl',
        timeLimitUnits: 'minutes'

}
