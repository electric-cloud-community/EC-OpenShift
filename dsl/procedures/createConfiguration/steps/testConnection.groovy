$[/myProject/scripts/preamble]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (efClient.toBoolean(actualParams.get('testConnection'))) {

	def cred = efClient.getCredentials('credential')

	String accessToken = 'Bearer ' + cred.password

	String clusterEndpoint = actualParams.get('clusterEndpoint')
	String openshiftHealthUrl = "$clusterEndpoint"

	OpenShiftClient client = new OpenShiftClient()
	client.setVersion(actualParams)

	def resp = client.checkClusterHealth(openshiftHealthUrl, accessToken)
	if (resp.status == 200){ 
		efClient.logger INFO, "OpenShift cluster is reachable at ${clusterEndpoint}"
	}
	if (resp.status >= 400){
		efClient.handleProcedureError("OpenShift cluster at ${clusterEndpoint} was not reachable. Health check at $openshiftHealthUrl failed with $resp.statusLine")
	}
}