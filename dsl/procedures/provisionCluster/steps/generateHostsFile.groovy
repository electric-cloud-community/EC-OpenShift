$[/myProject/scripts/preamble]
import groovy.json.*

// Get Master VM IP from Properties set by EC-ESX plugin
def jobId = '$[/myJob]'
EFClient efClient = new EFClient()
def result
def uri = "/rest/v1.0/properties/ESX/master1/vms"
def queryArgs = [
                jobId: jobId
        ]
result = efClient.doHttpGet(uri,true, queryArgs)
def vms=result.data.property.value

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(vms)
def openShiftMasterIP = object.ip_address[0]
println openShiftMasterIP

// Input parameters
def pluginProjectName = '$[/myProject/projectName]'
def hostname_prefix = '$[openshift_hostname_prefix]'
def domain_name = '$[domain_name]'
def user_login = '$[user_login]'
def workspaceDir = System.getenv("COMMANDER_WORKSPACE")
def additional_children = ""
def additional_vars = ""

// Prepare default masterList
def master_hostname = "${hostname_prefix}master.${domain_name}"
def masterList = "$master_hostname openshift_ip=\"$openShiftMasterIP\" openshift_public_ip=\"$openShiftMasterIP\" openshift_node_labels=\"{'region':'infra','zone':'default'}\" openshift_hostname=\"${master_hostname}\" openshift_public_hostname=\"${master_hostname}\" containerized=\"true\" deployment_type=origin\n"

// Prepare default etcdList
def etcdList = "${hostname_prefix}master.${domain_name}\n"

// Prepare default nodeList
int no_openshift_nodes = '$[no_openshift_nodes]' as Integer
def nodeList = ""
for (int i = 1; i<=no_openshift_nodes; i++) {
    nodeList += "${hostname_prefix}node${i}.${domain_name} deployment_type=origin\n".toString()
}


// Prepare default LB (loadbanacer) list
def lbList = ""


// Define Additional ansible hosts file elements based on topology used

switch('$[topology]') {

	case 'haproxy':

        additional_children = "etcd\nlb"
	
	    master_hostname = '$[openshift_hostname_prefix]' + "lb." + '$[domain_name]'
	    lbList = "[lb]\n" + master_hostname + "\n"

	    additional_vars = """openshift_master_cluster_method=native
						|openshift_node_kubelet_args={'pods-per-core': ['10'], 'max-pods': ['250'], 'image-gc-high-threshold': ['90'], 'image-gc-low-threshold': ['80']}
						|openshift_clock_enabled=true""".stripMargin()

	    // construct etcd list
	    etcdList = ""
	    for(int i=1;i<=3;i++){
		    etcdList += "${hostname_prefix}etcd${i}.${domain_name} deployment_type=origin\n".toString()
	    }

		// Construct masterList and nodeList	
		def master = ""				
		masterList = ""

		for(int i=1;i<=3;i++){
			//Get the IP of master VMs from ESX/master
			uri = "/rest/v1.0/properties/ESX/master${i}/vms"
			queryArgs = [
                jobId: jobId
        	]
			result = efClient.doHttpGet(uri,true, queryArgs)
			vms=result.data.property.value	
			object = jsonSlurper.parseText(vms)
			openShiftMasterIP = object.ip_address[0]
			master= "${hostname_prefix}master${i}.${domain_name}"
			masterList += "$master openshift_ip=\"$openShiftMasterIP\" openshift_public_ip=\"$openShiftMasterIP\" openshift_node_labels=\"{'region':'infra','zone':'default'}\" openshift_hostname=\"${master_hostname}\" openshift_public_hostname=\"${master_hostname}\" containerized=\"true\" deployment_type=origin\n".toString()
			nodeList += "${master} deployment_type=origin\n".toString()
		}
			
		break

	case 'multipleEtcd':

        additional_children = "etcd"
        etcdList = ""
	    for(int i=1;i<=3;i++){
		    etcdList += "${hostname_prefix}etcd${i}.${domain_name} deployment_type=origin\n".toString()
	    }
	    nodeList += "${master_hostname} deployment_type=origin\n".toString()
	    break

	default:
	 	nodeList += "${master_hostname} deployment_type=origin\n".toString()
	 	break
}


// Read ansible hosts template
File hostTemplate = new File(System.getenv("COMMANDER_WORKSPACE") + "/ansible/ansible_hosts_template")
templateText = hostTemplate.text

def binding = ["additional_children":additional_children, 	   
			   "additional_vars":additional_vars,
			   "masterList":masterList,
			   "etcdList": etcdList,
			   "nodeList":nodeList,
			   "lbList":lbList,
			   "master_hostname":master_hostname,
			   "workspaceDir":workspaceDir,
			   "user_login":user_login]

def engine = new groovy.text.SimpleTemplateEngine() 
def template = engine.createTemplate(templateText).make(binding) 

println template

File hostsFile = new File(System.getenv("COMMANDER_WORKSPACE") + '/hosts')
hostsFile.text = template