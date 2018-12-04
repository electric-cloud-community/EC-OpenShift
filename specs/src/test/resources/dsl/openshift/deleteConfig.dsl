package dsl.openshift

def names = args.params,
        pluginName = 'EC-OpenShift',
        configName = names.configName

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: "/plugins/${pluginProjectName}/project",
        procedureName: "DeleteConfiguration",
        actualParameter: [
                config: configName
        ]
)