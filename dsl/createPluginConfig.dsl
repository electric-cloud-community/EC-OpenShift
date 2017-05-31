def configName = '$[plugin_config_name]'
def pluginName = 'EC-OpenShift'
def logLevel = '2'
def desc = 'EC-OpenShift Config'
def userName = 'service-acct'
def password = '$[service_token]'
def clusterEndpoint = "https://" + '$[openshift_public_hostname]' + "." + '$[domain_name]' + ":8443"

// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: pluginName).projectName;
def resp = runProcedure(
  projectName: pluginProjectName,
  procedureName: 'CreateConfiguration',
    actualParameter: [
    config: configName,
    credential: configName,
    desc: desc,
    logLevel: logLevel,
    clusterEndpoint: clusterEndpoint
    ],
    credential: [
    credentialName: configName,
    userName: userName,
    password: password
    ]
);