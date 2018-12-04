package dsl.openshift

def names = args.params,
        templateYaml = names.templateYaml,
        yamlPrameters = names.yamlPrameters,
        projectName = names.projectName,
        applicationScoped = names.applicationScoped,
        applicationName = names.applicationName,
        envProjectName = names.envProjectName,
        environmentName = names.environmentName,
        clusterName = names.clusterName

// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: 'EC-OpenShift').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Import Microservices',
        actualParameter: [
                osTemplateYaml: templateYaml,
                templateParamValues: yamlPrameters,
                projName: projectName,
                application_scoped: applicationScoped,
                application_name: applicationName,
                envProjectName: envProjectName,
                envName: environmentName,
                clusterName: clusterName
        ]
)