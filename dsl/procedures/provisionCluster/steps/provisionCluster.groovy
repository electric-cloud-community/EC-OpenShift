$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

def openshift_hostname = '$[openshift_hostname]'
def openshift_public_hostname = '$[openshift_public_hostname]'
def openshift_ip = '$[openshift_ip]'
def openshift_public_ip = '$[openshift_public_ip]'
def openshift_nodes = '$[openshift_nodes]'.replaceAll(",","\n")

def text ="""[OSEv3:children]
masters
nodes
 
 
[OSEv3:vars]
ansible_ssh_user=cc_chanat
ansible_become=true
openshift_master_htpasswd_file=/home/vagrant/passwordfile
deployment_type=origin
containerized=true
openshift_release=\"1.4.0\"
openshift_master_overwrite_named_certificates=true
openshift_master_named_certificates=[{\"certfile\": \"/home/vagrant/ecloud.infracloud.website.cert\", \"keyfile\": \"/home/vagrant/ecloud.infracloud.website.key\"}]
openshift_master_cluster_hostname=$openshift_hostname
openshift_master_cluster_public_hostname=$openshift_public_hostname
 
 
[masters]
$openshift_hostname openshift_ip=\"$openshift_ip\" openshift_public_ip=\"$openshift_public_ip\" openshift_node_labels=\"{'region':'infra','zone':'default'}\" openshift_hostname=\"$openshift_hostname\" openshift_public_hostname=\"$openshift_public_hostname\" containerized=\"true\"
 
 
[etcd]
$openshift_hostname
 
 
[nodes]
$openshift_nodes"""

def binding = ["openshift_hostname":openshift_hostname, 
			   "openshift_public_hostname":openshift_public_hostname,
			   "openshift_ip":openshift_ip,
			   "openshift_public_ip":openshift_public_ip,
			   "openshift_nodes":openshift_nodes]

def engine = new groovy.text.SimpleTemplateEngine() 
def template = engine.createTemplate(text).make(binding) 

println template

File hostsFile = new File('hosts')
hostsFile.text = template


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


