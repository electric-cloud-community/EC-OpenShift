$[/myProject/scripts/preamble]

sleep(10000)
//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String clusterOrEnvProjectName = '$[clusterOrEnvProjectName]'
// default cluster project name if not explicitly set
if (!clusterOrEnvProjectName) {
    clusterOrEnvProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'

String resultsPropertySheet = '$[resultsPropertySheet]'
if (!resultsPropertySheet) {
    resultsPropertySheet = '/myParent/parent'
}

//// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()
OpenShiftClient client = new OpenShiftClient()

def pluginConfig = client.getPluginConfig(efClient, clusterName, clusterOrEnvProjectName, environmentName)
String accessToken = client.retrieveAccessToken (pluginConfig)

def clusterParameters = efClient.getProvisionClusterParameters(
                clusterName,
                clusterOrEnvProjectName,
                environmentName)

String clusterEndpoint = pluginConfig.clusterEndpoint
String namespace = clusterParameters.project

client.deployService(
        efClient,
        accessToken,
        clusterEndpoint,
        namespace,
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        clusterOrEnvProjectName,
        environmentName,
        resultsPropertySheet)