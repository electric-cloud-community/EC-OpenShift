package com.electriccloud.plugin.spec

import spock.lang.IgnoreRest

class SmartMap extends OpenShiftHelper {
    static final def projectName = 'EC-OpenShift Specs SmartMap'
    static final def clusterName = 'OpenShift Spec Cluster'
    static final def envName = 'OpenShift Spec Env'
    static final def configName = 'OpenShift Spec Config'

    def doSetupSpec() {
        createCluster(projectName, envName, clusterName, configName)
    }

    def 'get cluster topology'() {
        given:
        def serviceName = 'test-topology'
        cleanupService(serviceName)
        deploySample(serviceName)
        when:
        def result = getRealtimeClusterTopology()
        then:
        assert result
        def service = getService(result, serviceName)
        assert service
        assert service.id =~ /$serviceName/
        assert service.status in ['Running', 'Pending']

        def container = getContainer(result, 'nginx')
        assert container

        def pod = getPod(result, serviceName)
        assert pod

        def nodes = result?.nodes?.node
        assert ! nodes.find { it.name =~ /openshift/ }
        cleanup:
        cleanupService(serviceName)
    }

    def 'get service details'() {
        given:
        def serviceName = 'test-topology'
        cleanupService(serviceName)
        deploySample(serviceName)
        when:
        def result = getRealtimeClusterTopology()
        def service = getService(result, serviceName)
        assert service
        def details = getRealtimeClusterDetails(service.id, service.type)
        assert details
        assert getNodeAttributeValue(details, 'Status') in ['Pending', 'Running']
        assert getNodeAttributeValue(details, 'Running Pods') =~ /of 1/
        assert getNodeAttributeValue(details, 'Volumes')
        then:
        assert result
        cleanup:
        cleanupService(serviceName)
    }

    def 'get pod details'() {
        given:
        def serviceName = 'test-topology'
        cleanupService(serviceName)
        deploySample(serviceName)
        when:
        def topology = getRealtimeClusterTopology()
        then:
        def pod = getPod(topology, serviceName)
        assert pod
        def details  = getRealtimeClusterDetails(pod.id, pod.type)
        println details
        assert getNodeAttributeValue(details, "Status") in ['Pending', 'Running']
        assert getNodeAttributeValue(details, "Start time")
        assert getNodeAttributeValue(details, "Node")
        assert getNodeAttributeValue(details, "Labels")?.items
        cleanup:
        cleanupService(serviceName)
    }

    def 'get container details'() {
        given:
        def serviceName = 'test-topology'
        cleanupService(serviceName)
        deploySample(serviceName)
        when:
        def topology = getRealtimeClusterTopology()
        then:
        def container = getContainer(topology, 'nginx')
        def details  = getRealtimeClusterDetails(container.id, container.type)
        logger.debug(objectToJson(details))
        assert getNodeAttributeValue(details, "Status")
        assert details.actions?.action
        assert getNodeAttributeValue(details, "Image") =~ /nginx/
        assert getNodeAttributeValue(details, "Ports")?.items
        assert getNodeAttributeValue(details, "Volume Mounts")?.items

        cleanup:
        cleanupService(serviceName)
    }

    def getRealtimeClusterTopology() {
        def result = dsl """
        getRealtimeClusterTopology projectName: '$projectName', clusterName: '$clusterName', environmentName: '$envName'
    """
        return result?.clusterTopology
    }

    def getRealtimeClusterDetails(objectId, objectType) {
        assert objectId
        assert objectType
        def result = dsl """
            getRealtimeClusterDetails(projectName: '$projectName', clusterName: '$clusterName',
                environmentName: '$envName', objectId: '$objectId', objectType: '$objectType')
        """
        return result?.clusterNode
    }

    def getService(result, serviceName) {
        def service = result?.nodes?.node?.find { it.type == 'ecp-service' && it.name == serviceName }
        return service
    }

    def getContainer(result, containerName) {
        def service = result?.nodes?.node?.find { it.type == 'ecp-container' && it.name == containerName }
        return service
    }

    def getPod(result, serviceName) {
        def pod = result?.nodes?.node?.find { it.type == 'ecp-pod' && it.name =~ /$serviceName/ }
        return pod
    }

    def getNodeAttributeValue(node, name) {
        return node?.attributes?.attribute.find { it.name == name }?.value
    }

}
