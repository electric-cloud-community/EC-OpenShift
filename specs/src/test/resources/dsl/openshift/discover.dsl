package dsl.openshift

def names = args.params,
        projectName = names.projectName,
        envProjectName = names.envProjectName,
        environmentName = names.environmentName,
        openshiftProject = names.openshiftProject,
        clusterName = names.clusterName,
        clusterEndpoint = names.clusterEndpoint,
        clusterApiToken = names.clusterApiToken,
        applicationScoped = names.applicationScoped
        applicationName = names.applicationName

// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: 'EC-OpenShift').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Discover',
        actualParameter: [
                envProjectName: envProjectName,
                envName: environmentName,
                clusterName: clusterName,
                ecp_openshift_apiEndpoint: clusterEndpoint,
                ecp_openshift_apiToken: clusterApiToken,
                namespace: openshiftProject,
                projName: projectName,
                ecp_openshift_applicationScoped: applicationScoped,
                ecp_openshift_applicationName: applicationName
        ]
)
