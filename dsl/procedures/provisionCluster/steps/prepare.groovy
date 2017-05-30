$[/myProject/scripts/preamble]

EFClient efClient = new EFClient()
def jobId = "$[/myJob]"
def nodeList = '$[openshift_nodes]'
String[] nodes = nodeList.split(',')
def payload = [:]
def i = 1

for (String node: nodes) {
    println "node-${i}=${node}";
    payload << [
        propertyName: "node-${i}".toString(),
        value: node,
        jobId: jobId
	]
	efClient.doHttpPost("/rest/v1.0/properties", /* request body */ payload)
	i++
}




