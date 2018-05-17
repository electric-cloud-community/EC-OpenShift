$[/myProject/scripts/preamble]
$[/myProject/scripts/ImportFromTemplate]

// Input parameters
def osTemplateYaml = '''$[osTemplateYaml]'''.trim()
def osTemplateValues = '''$[templateParamValues]'''.trim()
def projectName = '$[projName]'
def envProjectName = '$[envProjectName]'
def environmentName = '$[envName]'
def clusterName = '$[clusterName]'
def applicationScoped = '$[application_scoped]'
def applicationName = '$[application_name]'
def NAMESPACE = "default"


EFClient efClient = new EFClient()

if(efClient.toBoolean(applicationScoped)) {
    if (!applicationName) {
        println "Application name is required for creating application-scoped microservices"
        System.exit(-1)
    }
} else {
    //reset application name since its not relevant if application_scoped is not set
    applicationName = null
}

def param2value = [:]
if (osTemplateValues != null && !osTemplateValues.equals("")) {
    def values = osTemplateValues.split(',')
    values.each { parameterAndValue ->
        if (parameterAndValue.contains('=')) {
            String [] parameterAndValueSplitted = parameterAndValue.split('=')
            param2value.put(parameterAndValueSplitted[0].trim(), parameterAndValueSplitted[1].trim())
        }
    }
}

param2value.each { p,v ->
    osTemplateYaml = osTemplateYaml.replace('{{' + p + '}}', v)
    osTemplateYaml = osTemplateYaml.replace('{' + p + '}', v)
}

if (envProjectName && environmentName && clusterName) {
    def clusters = efClient.getClusters(envProjectName, environmentName)
    def cluster = clusters.find {
        it.clusterName == clusterName
    }
    if (!cluster) {
        println "Cluster '${clusterName}' does not exist in '${environmentName}' environment!"
        System.exit(-1)
    }
    if (cluster.pluginKey != 'EC-OpenShift') {
        println "Wrong cluster type: ${cluster.pluginKey}"
        println "ElectricFlow cluster '${clusterName}' in '${environmentName}' environment is not backed by a OpenShift-based cluster."
        System.exit(-1)
    }
} else if (envProjectName || environmentName || clusterName) {
    // If any of the environment parameters are specified then *all* of them must be specified.
    println "Either specify all the parameters required to identify the OpenShift-backed ElectricFlow cluster (environment project name, environment name, and cluster name) where the newly created microservice(s) will be deployed. Or do not specify any of the cluster related parameters in which case the service mapping to a cluster will not be created for the microservice(s)."
    System.exit(-1)
}

def importFromTemplate = new ImportFromTemplate()

def services = importFromTemplate.importFromTemplate(NAMESPACE, osTemplateYaml)
importFromTemplate.saveToEF(services, projectName, envProjectName, environmentName, clusterName, applicationName)