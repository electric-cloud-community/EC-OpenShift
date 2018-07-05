package com.electriccloud.plugin.spec

import spock.lang.*
import com.electriccloud.spec.*

class Deploy extends OpenShiftHelper {
    static def projectName = 'EC-OpenShift Specs Deploy'
    static def clusterName = 'OpenShift Spec Cluster'
    static def envName = 'OpenShift Spec Env'
    static def configName = 'OpenShift Spec Config'

    def doSetupSpec() {
        configName = 'OpenShift Spec Config'
        createCluster(projectName, envName, clusterName, configName)
    }

    @Unroll
    def "deploy service #imageName, #imageVersion, capacity #defaultCapacity, #containerPort:#listenerPort"() {
        given:
        def serviceName = 'OpenShift Deploy Spec'
        dslFile "dsl/Deploy.dsl", [
            serviceName    : serviceName,
            projectName    : projectName,
            clusterName    : clusterName,
            envName        : envName,
            imageName      : imageName,
            imageVersion   : imageVersion,
            defaultCapacity: defaultCapacity,
            containerPort  : containerPort,
            listenerPort   : listenerPort,
            maxCapacity    : maxCapacity,
            minCapacity    : minCapacity
        ]
        when:
        def result = deployService(projectName, serviceName)
        then:
        logger.debug(result.logs)
        assert result.outcome == 'success'
        def deployedServiceName = serviceName.replaceAll(/\s+/, '-').toLowerCase()
        def service = getService(deployedServiceName)
        logger.debug(objectToJson(service))
        def deployment = getDeployment(deployedServiceName)
        logger.debug(objectToJson(deployment))

        assert service.spec.ports.size() == 1
        assert service.spec.ports[0].port.toString() == listenerPort
        assert service.spec.selector
        assert service.spec.selector['ec-svc']
        assert service.spec.clusterIP
        assert service.spec.sessionAffinity == 'None'

        def container = deployment.spec.template.spec.containers[0]

        def expectedDefaultCapacity = defaultCapacity ? defaultCapacity.toInteger() : 1
        assert deployment.spec.replicas == expectedDefaultCapacity
        assert container.image == "${imageName}:${imageVersion ?: 'latest'}"
        assert container.ports[0].containerPort.toString() == containerPort

        /*
        def strategy = deployment.spec.strategy.rollingUpdate
        assert strategy
        def expectedMaxSurge = maxCapacity ? maxCapacity - expectedDefaultCapacity : 1
        def expectedMaxUnavailable = minCapacity ? expectedDefaultCapacity - minCapacity : 1
        assert strategy.maxSurge == expectedMaxSurge
        assert strategy.maxUnavailable == expectedMaxUnavailable
        */
        cleanup:
        undeployService(projectName, serviceName)
        dsl """
                deleteService(
                    projectName: '$projectName',
                    serviceName: '$serviceName'
                )
            """
        where:
        imageName                | imageVersion | defaultCapacity | containerPort | listenerPort | maxCapacity | minCapacity
        'imagostorm/hello-world' | null         | '1'             | '80'          | '8080'       | '2'         | '1'
        'imagostorm/hello-world' | '1.0'        | '1'             | '80'          | '8080'       | '2'         | '1'
        'imagostorm/hello-world' | '1.0'        | '2'             | '81'          | '8081'       | '3'         | null
        'imagostorm/hello-world' | '2.0'        | null            | '81'          | '8081'       | '3'         | null

    }

    def "deploy canary"() {
        given: "first-time deploy runs"
        def serviceName = 'OpenShift Deploy Spec Canary'
        dslFile "dsl/Deploy.dsl", [
            serviceName    : serviceName,
            projectName    : projectName,
            clusterName    : clusterName,
            envName        : envName,
            imageName      : 'imagostorm/hello-world',
            imageVersion   : '1.0',
            defaultCapacity: '1',
            containerPort  : '8080',
            listenerPort   : '80',
        ]
        def result = deployService(projectName, serviceName)
        assert result.outcome == 'success'
        when: "canary deploy runs"
        dslFile "dsl/Deploy.dsl", [
            serviceName: serviceName,
            projectName: projectName,
            clusterName: clusterName,
            envName: envName,
            serviceMappingParameters: [
                canaryDeployment: 'true',
                numberOfCanaryReplicas: '1'
            ]
        ]
        result = deployService(projectName, serviceName)
        then:
        logger.debug(result.logs)
        def deployedServiceName = serviceName.replaceAll(/\s+/, '-').toLowerCase() + '-canary'
        def canaryDeployment = getDeployment(deployedServiceName)
        logger.debug(objectToJson(canaryDeployment))
        assert canaryDeployment.status.replicas == 1
        assert result.outcome == 'success'
        cleanup:
        dslFile "dsl/Deploy.dsl", [
            serviceName: serviceName,
            projectName: projectName,
            clusterName: clusterName,
            envName: envName,
            serviceMappingParameters: [
                canaryDeployment: 'false'
            ]
        ]
        undeployService(projectName, serviceName)
        dsl """
            deleteService(projectName:'$projectName', serviceName: '$serviceName')
        """
    }

        def "deploy route"() {
        given: "first-time deploy runs"
        def serviceName = 'OpenShift Deploy Spec Route'
        def routeName = 'nginx-spec-route'
        def routePath = '/'
        def routeHostname = getEndpoint().replaceAll("https://", '').replaceAll(":8443", '')
        def routeTargetPort = 'servicehttpnginx-container01523616023078'

        dslFile "dsl/Deploy.dsl", [
                serviceName    : serviceName,
                projectName    : projectName,
                clusterName    : clusterName,
                envName        : envName,
                imageName      : 'nginx',
                imageVersion   : '1.10',
                defaultCapacity: '1',
                containerPort  : '8080',
                listenerPort   : '80',
                serviceMappingParameters: [
                        'routeHostname': routeHostname,
                        'routeName': routeName,
                        'routePath': routePath,
                        'routeTargetPort': routeTargetPort
                ]
        ]

        when: "route deploy runs"
        def result = deployService(projectName, serviceName)
        logger.debug(result.logs)

        then:
        def deployedRoute = getRoutes(routeName)
        logger.debug("serviceBody")
        logger.debug(objectToJson(deployedRoute))
        def deployedServiceName = serviceName.replaceAll(/\s+/, '-').toLowerCase()
        assert deployedRoute.spec.to.name == deployedServiceName
        assert deployedRoute.spec.port.targetPort == routeTargetPort
        assert deployedRoute.spec.host == routeHostname
        assert deployedRoute.spec.path == routePath

        cleanup:
        dslFile "dsl/Deploy.dsl", [
                serviceName    : serviceName,
                projectName    : projectName,
                clusterName    : clusterName,
                envName        : envName,
                imageName      : 'nginx',
                imageVersion   : '1.10',
                defaultCapacity: '1',
                containerPort  : '80',
                listenerPort   : '8080',
        ]
        undeployService(projectName, serviceName)
        cleanupRoute(routeName)
        dsl """
            deleteService(projectName:'$projectName', serviceName: '$serviceName')
        """
    }

    def "update previously deployed service"() {
        given: "first-time deploy runs"
        def serviceName = 'OpenShift Deploy previously deployed'
        dslFile "dsl/Deploy.dsl", [
                serviceName    : serviceName,
                projectName    : projectName,
                clusterName    : clusterName,
                envName        : envName,
                imageName      : 'nginx',
                imageVersion   : '1.10',
                defaultCapacity: '1',
                containerPort  : '8080',
                listenerPort   : '80'
        ]
        def result = deployService(projectName, serviceName)
        logger.debug(result.logs)
        assert result.outcome == 'success'
        def deployedServiceName = serviceName.replaceAll(/\s+/, '-').toLowerCase()
        def deployment = getDeployment(deployedServiceName)
        logger.debug("serviceBody")
        logger.debug(objectToJson(deployment))
        assert deployment.spec.template.spec.containers[0].image == "nginx:1.10"
        assert deployment.spec.template.spec.containers[0].ports[0].containerPort == 8080

        when: "update previously deployed service"
        dslFile "dsl/Deploy.dsl", [
                serviceName    : serviceName,
                projectName    : projectName,
                clusterName    : clusterName,
                envName        : envName,
                imageName      : 'nginx',
                imageVersion   : '1.11',
                defaultCapacity: '1',
                containerPort  : '80',
                listenerPort   : '80'
        ]
        result = deployService(projectName, serviceName)

        then:
        logger.debug(result.logs)
        def updatedDeployment = getDeployment(deployedServiceName)
        logger.debug(objectToJson(updatedDeployment))
        assert updatedDeployment.metadata.generation > deployment.metadata.generation
        assert updatedDeployment.spec.template.spec.containers[0].image == "nginx:1.11"
        assert updatedDeployment.spec.template.spec.containers[0].ports[0].containerPort == 80
        assert result.outcome == 'success'

        cleanup:
        dslFile "dsl/Deploy.dsl", [
                serviceName    : serviceName,
                projectName    : projectName,
                clusterName    : clusterName,
                envName        : envName,
                imageName      : 'nginx',
                imageVersion   : '1.11',
                defaultCapacity: '1',
                containerPort  : '80',
                listenerPort   : '8080',
        ]
        undeployService(projectName, serviceName)
        dsl """
            deleteService(projectName:'$projectName', serviceName: '$serviceName')
        """
    }


    def getServiceName(serviceName) {
        serviceName.replaceAll(/\s+/, '-').toLowerCase()
    }


    def doCleanupSpec() {
        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }


}
