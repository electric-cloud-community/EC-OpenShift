import java.io.File

procedure 'EditConfiguration',
        description: 'Edit a configuration for OpenShift cluster', {

    
    step 'setup',
          subproject: '/plugins/EC-Kubernetes/project',
          subprocedure: 'Setup',
          command: null,
          errorHandling: 'failProcedure',
          condition: '$[testConnection]',
          exclusiveMode: 'none',
          postProcessor: 'postp',
          releaseMode: 'none',
          timeLimitUnits: 'minutes'

    step 'testConnection',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/testConnection.groovy').text,
            errorHandling: 'abortProcedure',
            condition: '$[testConnection]',
            exclusiveMode: 'none',
            releaseMode: 'none',
            resourceName: '$[grabbedResource]',
            shell: 'ec-groovy',
            timeLimitUnits: 'minutes'

}
