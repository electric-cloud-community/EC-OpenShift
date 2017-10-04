$[/myProject/scripts/preamble]

//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String envProjectName = '$[envProjectName]'
// default env project name if not explicitly set
if (!envProjectName) {
    envProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'

//// -- Driver script logic to undeploy service -- //
EFClient efClient = new EFClient()
// if cluster is not specified, find the cluster based on the environment that the application is mapped to.
if (!clusterName) {
    clusterName = efClient.getServiceCluster(serviceName,
            serviceProjectName,
            applicationName,
            applicationRevisionId,
            environmentName,
            envProjectName)
}

OpenShiftClient client = new OpenShiftClient()
def pluginConfig = client.getPluginConfig(efClient, clusterName, envProjectName, environmentName)
String accessToken = client.retrieveAccessToken (pluginConfig)
def clusterEndpoint = pluginConfig.clusterEndpoint

def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        envProjectName,
        environmentName)

String namespace = clusterParameters.project

client.undeployService(
        efClient,
        accessToken,
        clusterEndpoint,
        namespace,
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        envProjectName,
        environmentName)
