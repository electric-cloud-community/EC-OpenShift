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
client.setVersion(pluginConfig)
def resp = client.checkClusterHealth(openshiftHealthUrl, accessToken)
if (resp.status == 200){ 
	efClient.logger INFO, "OpenShift cluster is reachable at ${clusterEndpoint}"
}
if (resp.status >= 400){
	efClient.handleProcedureError("OpenShift cluster at ${clusterEndpoint} was not reachable. Health check at $openshiftHealthUrl failed with $resp.statusLine")
}
