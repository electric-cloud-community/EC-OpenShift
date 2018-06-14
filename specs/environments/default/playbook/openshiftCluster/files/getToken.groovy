import groovy.json.JsonSlurper

def shellPath = args[0]
println shellPath
if (!shellPath) {
    println "No shell path was provided"
    System.exit(1)
}
def clusterIpPort = '10.200.1.106:8443'
def username = 'admin'
def password = 'admin'
def projectName = 'flowqe-test-project'
def serviceAccountName = 'flowqe'
def configPath = '/var/lib/origin/openshift.local.config/master/admin.kubeconfig'

// Login using cluster admin credentials
"${shellPath} login $clusterIpPort --username=$username --password=$password".execute().text

// Describe the service account to discover the secret token name
def _token
"${shellPath} describe secret $serviceAccountName".execute().text.eachLine {
    if (it =~ /token/) {
        _token = it.tokenize().last().trim()
    }
}

println "ENVIRONMENT VARIABLE TOKEN: {{ $_token }}"

