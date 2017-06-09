def configName = '$[plugin_config_name]'
def pluginName = 'EC-OpenShift'
def logLevel = '2'
def desc = 'EC-OpenShift Config'
def userName = 'service_acc'
def password = '$[service_token]'
def clusterEndpoint

if ('$[topology]' == 'haproxy') {
    clusterEndpoint = 'https://' + '$[openshift_hostname_prefix]' + "lb." + '$[domain_name]' + ":8443"
} else {
    clusterEndpoint = 'https://' + '$[openshift_hostname_prefix]' + "master." + '$[domain_name]' + ":8443"
}

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