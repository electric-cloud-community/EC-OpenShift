def configName = args.config_name
def pluginName = 'EC-OpenShift'
def logLevel = '2'
def desc = 'EC-OpenShift Config'
def userName = 'motorbike'
def password = args.service_token
def clusterEndpoint = args.ip

        // Create plugin configuration

        def pluginProjectName = getPlugin(pluginName: pluginName).projectName;
        def resp = runProcedure(
        projectName: pluginProjectName,
        procedureName: 'CreateConfiguration',
        actualParameter: [
                config: configName,
                credential: configName + '_config',
                desc: desc,
                logLevel: logLevel,
                clusterEndpoint: clusterEndpoint
        ],
        credential: [
                credentialName: configName + '_config',
                userName: userName,
                password: password
                ]
);