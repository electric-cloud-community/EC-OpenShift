package com.electriccloud.procedures

import com.electriccloud.client.api.OpenshiftApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.OpenshiftClient


class NamingTestBase {

    public static String configName
    public static String projectName
    public static String environmentProjectName
    public static String environmentName
    public static String clusterName
    public static String serviceName
    public static String applicationName
    public static String containerName
    public static String pluginName
    public static String pluginVersion
    public static String clusterEndpoint
    public static String nodeEndpoint
    public static String clusterToken
    public static String clusterVersion
    public static String pluginProjectName
    public static String adminAccount
    public static String namespace
    public static String username
    public static String password
    public static String serviceaccount
    public static String osProject
    public static String osRouteHost

    public OpenshiftClient osClient
    public OpenshiftApi osApi
    public EctoolApi ectoolApi
    // Topology
    public static String ecpNamespaceId
    public static String ecpClusterId
    public static String ecpClusterName
    public static String ecpServiceId
    public static String ecpServiceName
    public static String ecpContainerId
    public static String ecpContainerName
    public static String ecpNamespaceName = "default"
    public static String ecpPodName = ""
    public static String ecpPodId = ""
    public static String environmentId
    public static String applicationId
    public static String serviceId
    public static String appServiceId
    public static String clusterId
    public static String topologyOutcome
    public static String description
    public static String endpoint


}

