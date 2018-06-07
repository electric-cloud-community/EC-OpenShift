import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.util.BasePlugin

//noinspection GroovyUnusedAssignment
@BaseScript BasePlugin baseScript

// Variables available for use in DSL code
def pluginName = args.pluginName
def upgradeAction = args.upgradeAction
def otherPluginName = args.otherPluginName

def pluginKey = getProject("/plugins/$pluginName/project").pluginKey
def pluginDir = getProperty("/projects/$pluginName/pluginDir").value

def pluginCategory = 'Container Management'
project pluginName, {
    description = 'Integrates with the OpenShift to run Docker containers on a hosted or on premise OpenShift cluster.'
    ec_visibility = 'pickListOnly'

    loadPluginProperties(pluginDir, pluginName)
    loadProcedures(pluginDir, pluginKey, pluginName, pluginCategory)

    //register container service plugin metadata
    property 'ec_container_service_plugin', {
        displayName = 'OpenShift'
        hasConfiguration = 1
        configurationLocation = 'ec_plugin_cfgs'
        property 'operations', {
            property 'provisionCluster', {
                property 'procedureName', value: 'Check Cluster'
                property 'ui_formRefs', {
                    parameterForm = 'ec_parameterForm'
                }
                property 'parameterRefs', {
                    configuration = 'config'
                    platformClusterName = 'clusterName'
                    platformProjectReference = 'clusterProjectID'
                }
            }
            property 'defineContainerMappings', {
                property 'procedureName', value: 'Define Container'
                property 'ui_formRefs', {
                    parameterForm = 'containerMappingsForm'
                }
            }
            property 'defineServiceMappings', {
                property 'procedureName', value: 'Define Service'
                property 'ui_formRefs', {
                    parameterForm = 'serviceMappingsForm'
                }
            }
            property 'deployService', {
                property 'procedureName', value: 'Deploy Service'
                property 'ui_formRefs', {
                    parameterForm = 'ec_parameterForm'
                }
                property 'parameterRefs', {
                    property 'serviceName', value: 'serviceName'
                    property 'projectName', value: 'serviceProjectName'
                    property 'applicationName', value: 'applicationName'
                    property 'applicationRevisionId', value: 'applicationRevisionId'
                    property 'clusterName', value: 'clusterName'
                    property 'clusterOrEnvironmentProjectName', value: 'clusterOrEnvProjectName'
                    property 'environmentName', value: 'environmentName'
                    property 'serviceEntityRevisionId', value: 'serviceEntityRevisionId'
                }
            }
            property 'undeployService', {
                property 'procedureName', value: 'Undeploy Service'
                property 'ui_formRefs', {
                    parameterForm = 'ec_parameterForm'
                }
                property 'parameterRefs', {
                    property 'serviceName', value: 'serviceName'
                    property 'projectName', value: 'serviceProjectName'
                    property 'applicationName', value: 'applicationName'
                    property 'applicationRevisionId', value: 'applicationRevisionId'
                    property 'clusterName', value: 'clusterName'
                    property 'clusterOrEnvironmentProjectName', value: 'envProjectName'
                    property 'environmentName', value: 'environmentName'
                    property 'serviceEntityRevisionId', value: 'serviceEntityRevisionId'
                }
            }
            property 'createConfiguration', {
                property 'procedureName', value: 'CreateConfiguration'
                property 'ui_formRefs', {
                    parameterForm = 'ec_parameterForm'
                }
                property 'parameterRefs', {
                    configuration = 'config'
                }
            }
            property 'deleteConfiguration', {
                property 'procedureName', value: 'DeleteConfiguration'
                property 'ui_formRefs', propertyType: 'sheet'
            }
        }

        property 'clusterTopology', credentialProtected: true, {
            property 'cluster', credentialProtected: true, {

            }
            property 'container', credentialProtected: true, {
                property 'actions', {
                    property 'viewLogs', credentialProtected: true, {

                    }
                }
            }
            property 'namespace', credentialProtected: true, {

            }
            property 'pod', credentialProtected: true, {
                property 'actions', {
                    property 'viewLogs', credentialProtected: true, {

                    }
                }
            }
            property 'service', credentialProtected: true, {

            }
        }
    }
    property 'ec_dsl_libraries_path', value: 'libs'

    //plugin configuration metadata
    property 'ec_formXmlCompliant', value: 'true'
    property 'ec_config', {
        configLocation = 'ec_plugin_cfgs'
        form = '$[' + "/projects/${pluginName}/procedures/CreateConfiguration/ec_parameterForm]"
        property 'fields', {
            property 'desc', {
                property 'label', value: 'Description'
                property 'order', value: '1'
            }
        }
    }

    procedure 'Define Container', {
        containerMappingsForm = new File(pluginDir, 'dsl/procedures/defineContainer/containerMappingsForm.xml').text
    }
    procedure 'Define Service', {
        serviceMappingsForm = new File(pluginDir, 'dsl/procedures/defineService/serviceMappingsForm.xml').text
    }
    // End-of container service plugin metadata

}

//Grant permissions to the plugin project
def objTypes = ['resources', 'workspaces', 'projects'];

objTypes.each { type ->
        aclEntry principalType: 'user',
             principalName: "project: $pluginName",
             systemObjectName: type,
             objectType: 'systemObject',
             readPrivilege: 'allow',
             modifyPrivilege: 'allow',
             executePrivilege: 'allow',
             changePermissionsPrivilege: 'allow'
}

// Copy existing plugin configurations from the previous
// version to this version. At the same time, also attach
// the credentials to the required plugin procedure steps.
upgrade(upgradeAction, pluginName, otherPluginName,
        [[
            procedureName: 'Check Cluster',
            stepName: 'checkCluster'
         ],
         [
            procedureName: 'Provision Cluster on ESX',
            stepName: 'provisionCluster'
        ], [
            procedureName: 'Deploy Service',
            stepName: 'createOrUpdateDeployment'
        ], [
            procedureName: 'Cleanup Cluster - Experimental',
            stepName: 'cleanup'
        ], [
            procedureName: 'Undeploy Service',
            stepName: 'undeployService'
         ],[
                procedureName: 'Discover',
                stepName: 'discover'
        ]])
