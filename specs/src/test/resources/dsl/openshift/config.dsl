package dsl.openshift

def names = args.params,
    configName = names.configName,
    pluginName = 'EC-OpenShift',
    endpoint = names.endpoint,
    logLevel = '1',
    desc = 'EC-OpenShift Config',
    userName = names.userName,
    token = names.token,
    version = names.version,
    testConnection = names.testConnection.toString()

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'CreateConfiguration',
        actualParameter: [
                config: configName,
                credential: 'credential',
                desc: desc,
                logLevel: logLevel,
                clusterEndpoint: endpoint,
                kubernetesVersion: version,
                testConnection: testConnection
        ],
        credential: [
                credentialName: 'credential',
                userName: userName,
                password: token
        ]
)

