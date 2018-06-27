package com.electriccloud.plugin.spec


import groovy.json.JsonSlurper
import spock.lang.*
import com.electriccloud.spec.*

class Discover extends OpenShiftHelper {
    static def projectName = 'EC-OpenShift Specs Discover'
    static def clusterName = 'OpenShift Spec Cluster'
    static def envName = 'OpenShift Spec Env'
    static def serviceName = 'openshift-spec-discovery-test'
    static def configName
    static def secretName
    static final def procedureName = 'Discover'

    @Shared
    def commonParams = [
        clusterName: clusterName,
        projName: projectName,
        envName: envName,
        envProjectName: projectName,
        namespace: 'flowqe-test-project'
    ]

    def doSetupSpec() {
        configName = 'OpenShift Spec Config'
        dsl """
            deleteProject(projectName: '$projectName')
        """
        cleanupCluster(configName)
        createCluster(projectName, envName, clusterName, configName)

        dslFile 'dsl/Discover.dsl', [
            projectName: projectName,
            params     : [
                envName                        : '',
                envProjectName                 : '',
                clusterName                    : '',
                namespace                      : '',
                projName                       : '',
                ecp_openshift_applicationScoped: '',
                ecp_openshift_applicationName  : '',
                ecp_openshift_apiEndpoint      : '',
                ecp_openshift_apiToken         : '',
            ]
        ]
    }

    def doCleanupSpec() {

    }

    def 'discover routes'() {
        given:
        def serviceName = 'sample-with-routes'
        cleanupService(serviceName)
        deployWithRoutes(serviceName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        logger.debug(result.logs)
        assert result.outcome != 'error'
        def service = getService(projectName, serviceName, clusterName, envName)
        assert service
        def routeName = getParameterDetail(service.service, 'routeName').parameterValue
        assert routeName == serviceName
        assert getParameterDetail(service.service, 'routeHostname').parameterValue
        assert getParameterDetail(service.service, 'routePath').parameterValue
        cleanup:
        cleanupService(serviceName)
        deleteService(projectName, serviceName)
    }

    def 'warning for two routes'() {
        given:
        def serviceName = 'sample-with-routes'
        def secondRouteName = 'second-route'
        cleanupService(serviceName)
        deployWithRoutes(serviceName)
        cleanupRoute(secondRouteName)
        deployRoute(serviceName, secondRouteName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        logger.debug(result.logs)
        def service = getService(projectName, serviceName, clusterName, envName)
        assert service
        def routeName = getParameterDetail(service.service, 'routeName').parameterValue
        assert routeName == serviceName
        assert getParameterDetail(service.service, 'routeHostname').parameterValue
        assert getParameterDetail(service.service, 'routePath').parameterValue

        assert result.outcome == 'warning'
        assert result.logs =~ /Only one route per service is allowed/
        cleanup:
        cleanupService(serviceName)
        deleteService(projectName, serviceName)
        cleanupRoute(secondRouteName)
    }

    @Ignore
    def "create application-scoped services"() {
        given:
        def sampleName = 'nginx-spec-application'
        cleanupService(sampleName)
        deploySample(sampleName)
        def applicationName = "Discovered Application Spec"
        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Discover',
                actualParameter: [
                    clusterName: '$clusterName',
                    namespace: 'default',
                    envProjectName: '$projectName',
                    envName: '$envName',
                    projName: '$projectName',
                    ecp_kubernetes_applicationScoped: 'true',
                    ecp_kubernetes_applicationName: '$applicationName'
                ]
            )
        """
        then:
        logger.debug(result.logs)
        def application = dsl "getApplication(projectName: '$projectName', applicationName: '$applicationName')"
        logger.debug(objectToJson(application))
        assert application.application?.applicationId
        def services = dsl "getServices(projectName: '$projectName', applicationName: '$applicationName')"
        logger.debug(objectToJson(services))
        assert services.service
        cleanup:
        cleanupService(sampleName)
        dsl "deleteApplication(projectName:'$projectName', applicationName: '$applicationName')"
    }

    @Ignore
    def "create environment from scratch"() {
        given:
        def sampleName = 'nginx-spec-scratch'
        cleanupService(sampleName)
        deploySample(sampleName)
        def token = System.getenv('KUBE_TOKEN')
        assert token
        def endpoint = System.getenv('KUBE_ENDPOINT')
        assert endpoint
        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Discover',
                actualParameter: [
                    clusterName: 'Created Cluster',
                    namespace: 'default',
                    envProjectName: '$projectName',
                    envName: 'Spec Kubernetes Created Env',
                    projName: '$projectName',
                    ecp_kubernetes_apiEndpoint: '$endpoint',
                    ecp_kubernetes_apiToken: '$token'
                ]
            )
        """
        then:
        logger.debug(result.logs)
        assert result.outcome != 'error'
        def environment = dsl "getEnvironment(projectName: '$projectName', environmentName: 'Spec Kubernetes Created Env')"
        assert environment.environment
        cleanup:
        cleanupService(serviceName)
        dsl "deleteEnvironment(projectName: '$projectName', environmentName: 'Spec Kubernetes Created Env')"
    }

    def "discover sample"() {
        given:
        def sampleName = 'nginx-spec'
        cleanupService(sampleName)
        deploySample(sampleName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            sampleName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))
        assert result.outcome != 'error'
        assert service.service
        def containers = service.service.container
        assert containers.size() == 1
        assert containers[0].containerName == 'nginx'
        assert containers[0].imageName == 'nginx'
        assert containers[0].imageVersion == '1.10'
        def port = containers[0].port[0]
        assert port
        assert service.service.defaultCapacity == '1'
        assert port.containerPort == '80'
        assert service.service.port[0].listenerPort == '80'
        def env = service.service.container[0].environmentVariable
        assert env.size() == 1
        assert env[0].value == 'TEST'
        assert env[0].environmentVariableName == 'TEST_ENV'

        def volumesJson = service.service.volumes
        def volumes = new JsonSlurper().parseText(volumesJson)
        assert volumes[0].name == 'my-volume'
        assert volumes[0].hostPath

        def volumeMountsJson = service.service.container[0].volumeMounts
        def volumeMounts = new JsonSlurper().parseText(volumeMountsJson)
        assert volumeMounts[0].name == 'my-volume'
        assert volumeMounts[0].mountPath
        cleanup:
        cleanupService(serviceName)
    }

    def "discover load balancer IP"() {
        given:
        def serviceName = 'openshift-spec-load-balancer-ip'
        cleanupService(serviceName)
        deployWithLoadBalancer(serviceName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))
        def kubeService = getService(serviceName)
        logger.debug(objectToJson(kubeService))
        assert getParameterDetail(service.service, 'loadBalancerIP').parameterValue == '35.224.8.81'
        assert getParameterDetail(service.service, 'serviceType').parameterValue == 'LoadBalancer'
        cleanup:
        cleanupService(serviceName)
    }

    @Ignore
    def "Liveness/readiness probe"() {
        given:
        def serviceName = 'openshift-spec-liveness'
        cleanupService(serviceName)
        deployLiveness(serviceName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))

        def container = service.service.container.first()
        assert getParameterDetail(container, 'livenessHttpProbeHttpHeaderName')
        assert getParameterDetail(container, 'livenessHttpProbeHttpHeaderValue')
        assert getParameterDetail(container, 'livenessHttpProbePath')
        assert getParameterDetail(container, 'livenessHttpProbePort')
        assert getParameterDetail(container, 'livenessInitialDelay')
        assert getParameterDetail(container, 'livenessPeriod')
        assert getParameterDetail(container, 'readinessInitialDelay')
        assert getParameterDetail(container, 'readinessPeriod')
        assert getParameterDetail(container, 'readinessCommand')
        assert container.command
        cleanup:
        cleanupService(serviceName)
    }

    @Ignore
    def "Discover secrets"() {
        given:
        cleanupService(serviceName)
        secretName = deployWithSecret(serviceName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName, envName
        )
        logger.debug(objectToJson(service))
        assert service.service.container.size() == 1
        assert service.service.container[0].imageName == 'imagostorm/hello-world'
        assert service.service.container[0].credentialName
        cleanup:
        cleanupService(serviceName)
        deleteSecret(secretName)
    }

    @Ignore("Until deploy strategies")
    def "Percentage in surge/maxUnavailable"() {
        given:
        def serviceName = 'openshift-spec-service-percentage'
        cleanupService(serviceName)
        deployWithPercentage(serviceName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:

        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))
        assert getParameterDetail(service.service, 'deploymentStrategy').parameterValue == 'rollingDeployment'
        assert getParameterDetail(service.service, 'maxRunningPercentage').parameterValue == '125'
        assert getParameterDetail(service.service, 'minAvailabilityPercentage').parameterValue == '75'
        cleanup:
        cleanupService(serviceName)
    }

    def "Two containers"() {
        given:
        def serviceName = 'two-containers-openshift-discover-spec'
        cleanupService(serviceName)
        deployTwoContainers(serviceName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        assert result.outcome != 'error'
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        logger.debug(objectToJson(service))

        def containers = service.service.container
        assert containers.size() == 2
        def first = containers.find {
            it.imageVersion == '1.0'
        }
        def second = containers.find {
            it.imageVersion == '2.0'
        }
        assert first
        assert second
        assert first.port[0].containerPort == '8080'
        assert second.port[0].containerPort == '8080'
        assert service.service.port.size() == 2
        def firstPort = service.service.port.find { it.subcontainer == 'hello' }
        def secondPort = service.service.port.find { it.subcontainer == 'hello-2' }
        assert firstPort.listenerPort == '80'
        assert secondPort.listenerPort == '81'
        cleanup:
        cleanupService(serviceName)
    }

    def 'two different services'() {
        given:
        def firstService = 'first-service'
        def secondService = 'second-service'
        cleanupService(firstService)
        cleanupService(secondService)
        deploySample(firstService)
        deploySample(secondService)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        logger.debug(result.logs);
        assert result.outcome != 'error'

        def first = getService(
            projectName,
            firstService,
            clusterName,
            envName
        )
        assert first

        def second = getService(
            projectName,
            secondService,
            clusterName,
            envName
        )

        assert second
        assert first.service?.container?.size() == 1
        assert second.service?.container?.size() == 1

        cleanup:
        cleanupService(firstService)
        cleanupService(secondService)
    }

    def 'discover deploymentConfig'() {
        given:
        def serviceName = 'test-deployment-config'
        cleanupDeploymentConfig(serviceName)
        deployConfig(serviceName)
        when:
        def result = runProcedure(projectName, procedureName, commonParams)
        then:
        assert result.outcome != 'error'
        logger.debug(result.logs)
        def service = getService(
            projectName,
            serviceName,
            clusterName,
            envName
        )
        assert service
        def containers = service.service.container
        assert containers.size() == 2
        cleanup:
        cleanupDeploymentConfig(serviceName)
    }



    def getParameterDetail(struct, name) {
        return struct.parameterDetail.find {
            it.parameterName == name
        }
    }


}
