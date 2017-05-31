$[/myProject/scripts/preamble]


import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.HTML

EFClient efClient = new EFClient()
def openshiftNotPresent;
def request = 'https://' + '$[openshift_public_hostname]' + '.'+'$[domain_name]' + ':8443/console'
def http = new HTTPBuilder(request)
http.ignoreSSLIssues()
try{
	http.request(GET,HTML) { req ->
		response.success = { resp, html ->	
			println "Server Response: ${resp.status}"
	        openshiftNotPresent = false
		}
		response.failure = { resp ->
			println "Server Response: ${resp.statusLine}"
	        openshiftNotPresent = true
		}
	}
} catch(Exception e) {

	println "Cought Exception: ${e}"
	openshiftNotPresent = true
}

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

payload = [:]
payload << [
        propertyName: "OpenshiftNotPresent",
        value: openshiftNotPresent,
        jobId: jobId
	]
efClient.doHttpPost("/rest/v1.0/properties", /* request body */ payload)









