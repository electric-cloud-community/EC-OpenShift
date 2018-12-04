package dsl.openshift

def names = args.params,
        configName = names.configName,
        projectNamespace = names.projectNamespace

runProcedure(
        projectName: '/plugins/EC-OpenShift/project',
        procedureName: "Cleanup Cluster - Experimental",
        actualParameter: [
                namespace: projectNamespace,
                config: configName
        ]
)