package com.electriccloud.procedures.topology

import com.electriccloud.procedures.OpenshiftTestBase
import io.qameta.allure.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import static com.electriccloud.models.enums.LogLevels.*

@Feature("Topology")
class GetRealtimeClusterTopology extends OpenshiftTestBase {


    @BeforeClass(alwaysRun = true)
    void createAndDeployProjectLevelMicroservice() {
        createAndDeployService(false)
        setTopology()
    }

    @BeforeMethod(alwaysRun = true)
    void backendAuthorization(){
        ectoolApi.ectoolLogin()
    }

    @AfterClass(alwaysRun = true)
    void tearDown() {
        osClient.cleanUpCluster(configName)
        osClient.deleteConfiguration(configName)
        osClient.client.deleteProject(projectName)
    }


    @Test(groups = "Positive")
    @TmsLink("")
    @Story("Get Realtime Cluster Topology positive")
    @Description("Get a Response with correct fields for all Node Types in Topology")
    void getAResponseWithCorrectFieldsForAllNodeTypesInTopology() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, clusterName,
                "--environmentName", environmentName

        [
                [ id: clusterId,       name: clusterName,      type: "cluster"        ],
                [ id: clusterEndpoint, name: clusterEndpoint,  type: "ecp-cluster",    efRef: true ],
                [ id: ecpNamespaceId,  name: ecpNamespaceName, type: "ecp-namespace",  efRef: true ],
                [ id: ecpServiceId,    name: serviceName,      type: "ecp-service",   status: "Running", efId: serviceId, efRef: true ],
                [ id: ecpPodId,        name: ecpPodName,       type: "ecp-pod",       status: "Running", efRef: true ],
                [ id: ecpContainerId,  name: containerName,    type: "ecp-container", status: "running", efRef: true ],
        ].each { item ->
            assert !_node(item).toString().empty
        }

        [
                [ source: clusterId,        target: clusterEndpoint ],
                [ source: clusterEndpoint,  target: ecpNamespaceId ],
                [ source: ecpNamespaceId,   target: ecpServiceId ],
                [ source: ecpServiceId,     target: ecpPodId ],
                [ source: ecpPodId,         target: ecpContainerId ],
        ].each { item ->
            assert !_link(item).toString().empty
        }
    }



    @Test(groups = "Positive", enabled = true)
    @TmsLink("")
    @Story("Get Realtime Cluster Topology positive")
    @Description("Get a Response with correct fields for all Node Types in Topology after Deploy Imported Microservice")
    void getAResponseWithCorrectFieldsForAllNodeTypesInTopologyAfterDeployImportedMicroservice() {

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, clusterName,
                "--environmentName", environmentName

        [
                [ id: clusterId,       name: clusterName,      type: "cluster"        ],
                [ id: clusterEndpoint, name: clusterEndpoint,  type: "ecp-cluster",    efRef: true ],
                [ id: ecpNamespaceId,  name: ecpNamespaceName, type: "ecp-namespace",  efRef: true ],
                [ id: ecpServiceId,    name: serviceName,      type: "ecp-service",   status: "Running", efId: serviceId, efRef: true ],
                [ id: ecpPodId,        name: ecpPodName,       type: "ecp-pod",       status: "Running", efRef: true ],
                [ id: ecpContainerId,  name: containerName,    type: "ecp-container", status: "running", efRef: true ],
        ].each { item ->
            assert !_node(item).toString().empty
        }

        [
                [ source: clusterId,        target: clusterEndpoint ],
                [ source: clusterEndpoint,  target: ecpNamespaceId ],
                [ source: ecpNamespaceId,   target: ecpServiceId ],
                [ source: ecpServiceId,     target: ecpPodId ],
                [ source: ecpPodId,         target: ecpContainerId ],
        ].each { item ->
            assert !_link(item).toString().empty
        }
        // checkResponseForGetRealtimeClusterTopology(topologyOutcome)
    }



    @Test(groups = "Negative", enabled = true)
    @TmsLink("")
    @Story("Get Realtime Cluster Topology negative")
    @Description("Unable to Get Realtime Cluster Topology for non-existing Configuration")
    void getTopologyWithoutPluginConfiguration() {

        osClient.deleteConfiguration(configName)

        topologyOutcome = ectoolApi.run "ectool", "getRealtimeClusterTopology", projectName, clusterName,
                "--environmentName", environmentName

        assert topologyOutcome == "ectool error [NoSuchConfiguration]: No plugin configuration '$configName' " +
                "found at 'ec_plugin_cfgs' for '$pluginName-$pluginVersion'"

        osClient.createConfiguration(configName, clusterEndpoint, 'qe', clusterToken, clusterVersion)
    }


    @Test(groups = "Postive")
    @TmsLink("")
    @Story("Get Realtime Cluster Topology positive")
    @Description("Get Realtime Cluster Topology using DSL")
    void getRealtimeClusterTopologyUsingDSL() {

        topologyOutcome = ectoolApi.dsl "getRealtimeClusterTopology(projectName:'$projectName',clusterName:'$clusterName',environmentName:'$environmentName')"

        [
                [ id: clusterId,       name: clusterName,      type: "cluster"        ],
                [ id: clusterEndpoint, name: clusterEndpoint,  type: "ecp-cluster",    efRef: true ],
                [ id: ecpNamespaceId,  name: ecpNamespaceName, type: "ecp-namespace",  efRef: true ],
                [ id: ecpServiceId,    name: serviceName,      type: "ecp-service",   status: "Running", efId: serviceId, efRef: true ],
                [ id: ecpPodId,        name: ecpPodName,       type: "ecp-pod",       status: "Running", efRef: true ],
                [ id: ecpContainerId,  name: containerName,    type: "ecp-container", status: "running", efRef: true ],
        ].each { item ->
            assert !_node(item).toString().empty
        }

        [
                [ source: clusterId,        target: clusterEndpoint ],
                [ source: clusterEndpoint,  target: ecpNamespaceId ],
                [ source: ecpNamespaceId,   target: ecpServiceId ],
                [ source: ecpServiceId,     target: ecpPodId ],
                [ source: ecpPodId,         target: ecpContainerId ],
        ].each { item ->
            assert !_link(item).toString().empty
        }
    }


    



}
