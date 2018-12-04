package com.electriccloud.procedures

import com.electriccloud.client.api.OpenshiftApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.OpenshiftClient
import com.electriccloud.listeners.TestListener
import io.qameta.allure.Epic
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import org.testng.annotations.BeforeClass
import org.testng.annotations.Listeners

import java.util.concurrent.TimeUnit

import static io.restassured.RestAssured.*
import static com.electriccloud.models.enums.ServiceTypes.ServiceType.*
import static org.awaitility.Awaitility.*

@Epic('EC-Openshift')
@Listeners(TestListener.class)
class OpenshiftTestBase extends TopologyMatcher {


    def getHost = { hostValue -> new URL(hostValue).host }

    def req = given().relaxedHTTPSValidation()
            .filters(new RequestLoggingFilter(), new ResponseLoggingFilter())
            .when()

    def volumes = [ source: '[{"name": "html-content","hostPath": "/var/html"}]',
                    target: '[{"name": "html-content","mountPath": "/usr/share/nginx/html"}]' ]

    @BeforeClass(alwaysRun = true)
    void setUpData(){
        setDefaultTimeout(200, TimeUnit.SECONDS)

        configName             = 'openshiftConfig'
        projectName            = 'openshiftProj'
        environmentProjectName = 'openshiftProj'
        environmentName        = "openshift-environment"
        clusterName            = "kube-cluster"
        serviceName            = 'nginx-service'
        applicationName        = 'nginx-application'
        containerName          = "nginx-container"

        pluginName             = System.getenv("PLUGIN_NAME")
        pluginVersion          = System.getenv("PLUGIN_BUILD_VERSION")
        clusterEndpoint        = System.getenv("CLUSTER_ENDPOINT")
        clusterToken           = System.getenv("CLUSTER_TOKEN")
        clusterVersion         = System.getenv("CLUSTER_VERSION")
        serviceaccount         = System.getenv("OS_SERVICEACCOUNT")
        username               = System.getenv("OS_USERNAME")
        password               = System.getenv("OS_PASSWORD")
        osProject              = System.getenv("OS_PROJECT")

        osRouteHost           = getHost(clusterEndpoint)
        ecpNamespaceName      = osProject
        pluginProjectName     = "${pluginName}-${pluginVersion}"

        ectoolApi = new EctoolApi(true)
        osApi = new OpenshiftApi(username, password, clusterToken, serviceaccount, clusterEndpoint)
        osClient = new OpenshiftClient()

        ectoolApi.ectoolLogin()

    }





    /**
     * Topology electricflow methods
     */

    void createAndDeployService(appLevel = false){
        pluginProjectName = "${pluginName}-${pluginVersion}"
        osClient.deleteConfiguration(configName)
        osClient.createConfiguration(configName, clusterEndpoint, 'qe', clusterToken, clusterVersion)
        osClient.createEnvironment(configName, osProject)
        if (appLevel){
            osClient.createApplication(2, [:], false, getHost(clusterEndpoint), LOAD_BALANCER)
            osClient.deployApplication(projectName, applicationName)
        } else {
            osClient.createService(2, [:], false, getHost(clusterEndpoint), LOAD_BALANCER)
            osClient.deployService(projectName, serviceName)
        }
        await().until { osApi.client.pods().list().getItems().last().status.phase == 'Running' }
    }

    def setTopology(appLevel = false) {
        ecpPodName = osApi.client.pods().list().getItems().last().metadata.name
        environmentId = osClient.client.getEnvironment(projectName, environmentName).json.environment.environmentId
        clusterId = osClient.client.getEnvCluster(projectName, environmentName, clusterName).json.cluster.clusterId

        ecpNamespaceId   = "$clusterEndpoint::$ecpNamespaceName"
        ecpClusterId     = clusterEndpoint
        ecpClusterName   = clusterEndpoint
        ecpServiceId     = "$ecpNamespaceId::$serviceName"
        ecpServiceName   = "$ecpNamespaceName::$serviceName"
        ecpPodId         = "$clusterEndpoint::$ecpNamespaceName::$ecpPodName"
        ecpContainerId   = "$ecpPodId::$containerName"
        ecpContainerName = "$ecpNamespaceName::$ecpPodName::$containerName"

        if(appLevel) {
            applicationId = osClient.client.getApplication(projectName, applicationName).json.application.applicationId
            serviceId = osClient.client.getApplicationService(projectName, applicationName, serviceName).json.service.serviceId
            appServiceId = serviceId
        } else {
            serviceId = osClient.client.getService(projectName, serviceName).json.service.serviceId
        }
    }


}