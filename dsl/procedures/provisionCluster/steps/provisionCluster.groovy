$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

String accessToken = 'Bearer ' + pluginConfig.credential.password
String clusterEndpoint = pluginConfig.clusterEndpoint

String openshiftHealthUrl = "$clusterEndpoint"

OpenShiftClient client = new OpenShiftClient()
def resp = client.checkClusterHealth(openshiftHealthUrl, accessToken)
if (resp.status == 200){ 
	efClient.logger INFO, "The service is reachable at ${clusterEndpoint}"
}
if (resp.status >= 400){
	efClient.handleProcedureError("The Kubernetes cluster at ${clusterEndpoint} was not reachable. Health check at $openshiftHealthUrl failed with $resp.statusLine")
}


