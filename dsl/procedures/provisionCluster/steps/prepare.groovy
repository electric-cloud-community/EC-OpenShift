$[/myProject/scripts/preamble]

// This script checks if Openshift is already deployed
// by hitting Console https://<openshift_master>:8443/console
// and sets "/myJob/openshiftNotPresent" property accordingly
// This property is then used by further steps to skip actions if
// openshift is already deployed.

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.HTML

EFClient efClient = new EFClient()
def openshiftNotPresent;
def request

if ('$[topology]' == 'haproxy') {
	request = 'https://' + '$[openshift_hostname_prefix]' + "lb." + '$[domain_name]' + ':8443/console'
} else {
	request = 'https://' + '$[openshift_hostname_prefix]' + "master." + '$[domain_name]' + ':8443/console'
}

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
def payload = [:]
payload << [
        propertyName: "OpenshiftNotPresent",
        value: openshiftNotPresent,
        jobId: jobId
	]
efClient.doHttpPost("/rest/v1.0/properties", /* request body */ payload)

payload << [
        propertyName: "/myJob/config",
        value: "$[plugin_config_name]",
        jobId: jobId
	]

efClient.doHttpPost("/rest/v1.0/properties", /* request body */ payload)







