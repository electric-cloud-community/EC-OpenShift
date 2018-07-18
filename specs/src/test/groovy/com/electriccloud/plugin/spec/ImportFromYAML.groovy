package com.electriccloud.plugin.spec

import spock.lang.*
import com.electriccloud.spec.*

class ImportFromYAML extends OpenShiftHelper {
    static def kubeYAMLFile
    static def projectName = 'EC-OpenShift Specs Import'
    static def applicationScoped = true
    static def applicationName = 'OpenShift Spec App'
    static def envName = 'OpenShift Spec Env'
    static def serviceName = 'openshift-spec-import-test'
    static def clusterName = 'OpenShift Spec Cluster'
    static def configName

    def doSetupSpec() {
        configName = 'OpenShift Spec Config'

        dsl """
            deleteProject(projectName: '$projectName')
        """
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/ImportFromYAML.dsl', [
                projectName: projectName,
                params     : [
                        osTemplateYaml     : '',
                        templateParamValues: '',
                        projName           : '',
                        application_scoped : '',
                        application_name   : '',
                        envProjectName     : '',
                        envName            : '',
                        clusterName        : '',
                ]
        ]

    }

    def doCleanupSpec() {

    }


    def getTemplate(name, variables) {
        File resource = new File(this.getClass().getResource("/templates/$name").toURI())
        def text = resource.text

        def engine = new groovy.text.SimpleTemplateEngine()
        def template = engine.createTemplate(text).make(variables)
        return template.toString()
    }

    def "top level service"() {
        given:
        def sampleName = 'my-service-nginx-deployment'
        def fileName = 'simpleService.yaml'
        kubeYAMLFile = getTemplate(fileName, [serviceName: sampleName])
        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Microservices',
                actualParameter: [
                    osTemplateYaml: '''$kubeYAMLFile''',
                    projName: '$projectName',
                    envProjectName: '$projectName',
                    envName: '$envName',
                    clusterName: '$clusterName'
                ]
            )
        """
        then:
        logger.debug(result.logs)
        def service = getService(
                projectName,
                sampleName,
                clusterName,
                envName
        )
        assert result.outcome != 'error'
        assert service.service
        assert service.service.defaultCapacity == '3'
        def containers = service.service.container
        assert containers.size() == 1
        assert containers[0].containerName == 'nginx'
        assert containers[0].imageName == 'nginx'
        assert containers[0].imageVersion == '1.7.9'
        def port = containers[0].port[0]
        assert port
        assert port.containerPort == '80'
        cleanup:
        deleteService(projectName, serviceName)
    }

    @Issue("ECPOPSHIFT-127")
    def "top level service without env mapping"() {
        given:
        def serviceName = 'test-service-without-env-mapping'
        def fileName = 'simpleService.yaml'
        kubeYAMLFile = getTemplate(fileName, [serviceName: serviceName])

        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Microservices',
                actualParameter: [
                    osTemplateYaml: '''$kubeYAMLFile''',
                    projName: '$projectName',
                    envProjectName: '',
                    envName: '',
                    clusterName: ''
                ]
            )
        """

        then:
        logger.debug(result.logs)
        assert result.outcome != 'error'

        def service = getServiceDsl(
                projectName,
                serviceName
        )
        assert service.service
        assert service.service.defaultCapacity == '3'
        assert service.service.containerCount == '1'
        assert service.service.environmentMapCount == '0'

        def container = getContainerDsl(
                projectName,
                serviceName,
                'nginx',
                true
        )
        assert container.container
        assert container.container.containerName == 'nginx'
        assert container.container.imageName == 'nginx'
        assert container.container.imageVersion == '1.7.9'
        assert container.container.cpuCount == '0.25'
        assert container.container.cpuLimit == '0.5'
        assert container.container.memorySize == '128'
        assert container.container.memoryLimit == '256'

        assert container.container.port.size() == 1

        def port = container.container.port[0]
        assert port
        assert port.containerPort == '80'

        cleanup:
        deleteService(projectName, serviceName)
    }

    def "negative, wrong env mapping params provided"(envProjectNameSample, envNameSample, clusterNameSample) {
        given:
        def serviceName = 'negative-test-wrong-env-mapping-params'
        def fileName = 'simpleService.yaml'
        kubeYAMLFile = getTemplate(fileName, [serviceName: serviceName])

        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Microservices',
                actualParameter: [
                    osTemplateYaml: '''$kubeYAMLFile''',
                    projName: '$projectName',
                    envProjectName: '$envProjectNameSample',
                    envName: '$envNameSample',
                    clusterName: '$clusterNameSample'
                ]
            )
        """

        then:
        logger.debug(result.logs)
        assert result.outcome == 'error'

        def services = getServicesDsl(projectName)
        assert !(services.service.any { it.serviceName == serviceName })

        where:
        envProjectNameSample   | envNameSample      | clusterNameSample
        projectName            | envName            | ""
        projectName            | ""                 | clusterName
        ""                     | envName            | clusterName
        projectName            | envName            | "non-existing-cluster"
        projectName            | "non-existing-env" | clusterName
        "non-existing-project" | envName            | clusterName

    }


    @Issue("ECPOPSHIFT-132")
    // Also covers ECPOPSHIFT-133, ECPOPSHIFT-134, ECPOPSHIFT-149
    def 'Some template to support. Objects: deployment config, service, route, hpa (not supported); non standard registry and namespace; parameters with default values)'(paramValues) {
        given:
        def serviceName = 'my-service'
        def autoscalerName = 'my-autoscaler'
        kubeYAMLFile = getTemplate('withHPA.yaml', [serviceName: serviceName, autoscalerName: autoscalerName])

        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Microservices',
                actualParameter: [
                    osTemplateYaml: '''$kubeYAMLFile''',
                    projName: '$projectName',
                    envProjectName: '$projectName',
                    envName: '$envName',
                    clusterName: '$clusterName',
                    templateParamValues: '''$paramValues'''
                ]
            )
        """

        then:
        logger.debug(result.logs)
        assert result.outcome != 'error'
        def service = getService(
                projectName,
                serviceName,
                clusterName,
                envName).service

        assert service
        assert service.defaultCapacity == '1'

        def containers = service.container
        assert containers.size() == 1

        def container = containers[0]
        assert container
        assert container.containerName == serviceName
        // ECPOPSHIFT-134 non standard registry and repository namespace
        assert container.registryUri == 'some.registry.with.dots'
        assert container.imageName == "some.repository.namespace.with.dots/$serviceName"
        // ECPOPSHIFT-133 provided and default values (2 test cases)
        assert container.imageVersion == paramValues ? '1.0' : 'local'

        def containerVolumeMounts = container.volumeMounts
        assert containerVolumeMounts.size() == 1

        def containerVolumeMount = containerVolumeMounts[0]
        // for container volumeMounts 'readonly' is currently ignored due to ECPOPSHIFT-135 so let's check only 'name' and 'mountPath' properties
        assert containerVolumeMount.name == 'myVolume'
        assert containerVolumeMount.mountPath == '/mount/path'

        assert container.cpuCount == '2.0'
        assert container.cpuLimit == '4.0'
        assert container.memorySize == '3500'
        assert container.memoryLimit == '5000'

        def containerMappingParams = container.parameterDetail
        def containerMappingParamsMap = containerMappingParams.collectEntries {
            [(it.parameterName): it.parameterValue]
        }

        // for now we support only first liveness probes http header: ECPOPSHIFT-143
        assert containerMappingParamsMap.livenessHttpProbeHttpHeaderName == 'myHeader'
        assert containerMappingParamsMap.livenessHttpProbeHttpHeaderValue == 'value'
        assert containerMappingParamsMap.livenessHttpProbePath == '/some/path/probes/ping'
        assert containerMappingParamsMap.livenessHttpProbePort == '8080'
        assert containerMappingParamsMap.livenessInitialDelay == '180'
        assert containerMappingParamsMap.livenessPeriod == '10'
        // we have very limited support of readyness probes: ECPOPSHIFT-144, actually there are limitation for liveness probes as well
        assert containerMappingParamsMap.readinessInitialDelay == '180'
        assert containerMappingParamsMap.readinessPeriod == '10'

        def containerPorts = container.port
        assert containerPorts.size() == 1

        def containerPort = containerPorts[0]
        assert containerPort
        // name generated by pattern: protocol.toLowerCase() + containerPort
        assert containerPort.portName == 'tcp8080'
        assert containerPort.containerPort == '8080'

        def serviceMappingParams = service.parameterDetail
        def serviceMappingParamsMap = serviceMappingParams.collectEntries {
            [(it.parameterName): it.parameterValue]
        }

        assert serviceMappingParamsMap.routeName == 'my-service'
        assert serviceMappingParamsMap.routeTargetPort == '8080-tcp'

        assert serviceMappingParamsMap.serviceType == 'ClusterIP'
        assert serviceMappingParamsMap.sessionAffinity == 'None'

        def servicePorts = service.port
        assert servicePorts.size() == 1

        def servicePort = servicePorts[0]
        assert servicePort
        assert servicePort.listenerPort == '8080'
        // portName can be service port name (if provided, current example) or generated by pattern - ECPOPSHIFT-149
        // FYI.. it can be used for mapping route to a service
        assert servicePort.portName == '8080-tcp'
        assert servicePort.subcontainer == 'my-service'
        assert servicePort.subport == 'tcp8080' // see containerPort.portName

        def serviceVolumes = service.volumes
        assert serviceVolumes.size() == 1

        def serviceVolume = serviceVolumes[0]
        // for services volumes 'configMap' is currently ignored due to ECPOPSHIFT-142 so let's check only 'name' property
        assert serviceVolume.name == 'myVolume'

        cleanup:
        deleteService(projectName, serviceName)

        where:
        paramValues << ['DOCKERIMAGE_VERSION=1.0, REPLICAS=3', '']
    }

    @Ignore
    def 'with several params'() {
        given:
        def serviceName = 'myservice'
        def yaml = getTemplate('severalParams.yaml', [serviceName: serviceName])
        def paramValues = 'PARAM=1'
        when:
        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Microservices',
                actualParameter: [
                    osTemplateYaml: '''$yaml''',
                    projName: '$projectName',
                    envProjectName: '$projectName',
                    envName: '$envName',
                    clusterName: '$clusterName',
                    templateParamValues: '''$paramValues'''
                ]
            )
        """
        then:
        logger.debug(result.logs)
        assert result.outcome != 'error'
        cleanup:
        deleteService(projectName, serviceName)
    }

    def "application-scoped-service"() {
        given:
        def sampleName = 'my-service-nginx-deployment'
        kubeYAMLFile = getTemplate('simpleService.yaml', [serviceName: sampleName])
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        osTemplateYaml: '''$kubeYAMLFile''',
                        projName: '$projectName',
                        application_scoped: '$applicationScoped',
                        application_name:   '$applicationName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getAppScopedService(
                projectName,
                sampleName,
                applicationName,
                clusterName,
                envName
        )
        assert result.outcome != 'error'
        assert service.service
        assert service.service.defaultCapacity == '3'
        def containers = service.service.container
        assert containers.size() == 1
        assert containers[0].containerName == 'nginx'
        assert containers[0].imageName == 'nginx'
        assert containers[0].imageVersion == '1.7.9'
        def port = containers[0].port[0]
        assert port
        assert port.containerPort == '80'
        cleanup:
        deleteService(projectName, serviceName)
    }

    def 'import routes'() {
        given:
        def sampleOneName = 'my-service1-nginx-deployment1'
        def sampleTwoName = 'my-service2-nginx-deployment2'
        def serviceName = 'my-service-with-routes'
        def routeHostname = 'routeHostname'

        def yamlContent = getTemplate("routes.yaml", [routeHostname: routeHostname, serviceName: serviceName])
        when:

        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        osTemplateYaml: '''$yamlContent''',
                        projName: '$projectName',
                        application_scoped: '0',
                        application_name:   '',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        assert result.outcome != 'error'
        def service = getService(projectName, serviceName, clusterName, envName)
        logger.debug(objectToJson(service))
        assert getMappingDetail(service, 'routeHostname') == routeHostname
        assert getMappingDetail(service, 'routeName') == 'nginx-route'
        assert getMappingDetail(service, 'routePath') == '/'
        assert getMappingDetail(service, 'routeTargetPort')
        cleanup:
        deleteService(projectName, serviceName)
    }

    def 'import routes with warning (two routes per service)'() {
        given:
        def serviceName = 'my-service-with-routes'
        def routeHostname = 'routeHostname'
        def yamlContent = getTemplate("twoRoutes.yaml", [serviceName: serviceName])
        when:

        def result = runProcedureDsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Import Microservices',
                actualParameter: [
                    osTemplateYaml: '''$yamlContent''',
                    projName: '$projectName',
                    application_scoped: '0',
                    application_name:   '',
                    envProjectName: '$projectName',
                    envName: '$envName',
                    clusterName: '$clusterName'
                ]
            )
        """
        then:
        logger.debug(result.logs)
        assert result.outcome == 'warning'
        def service = getService(projectName, serviceName, clusterName, envName)
        logger.debug(objectToJson(service))
        assert result.logs =~ /Only one route per service is allowed in ElectricFlow. The route/
        cleanup:
        deleteService(projectName, serviceName)
    }

    def "many top-level services"() {
        given:
        def sampleOneName = 'my-service1-nginx-deployment1'
        def sampleTwoName = 'my-service2-nginx-deployment2'
        def template = getTemplate('twoTopLevel.yaml', [:])
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        osTemplateYaml: '''$template''',
                        projName: '$projectName',
                        envProjectName: '$projectName',
                        envName: '$envName',
                        clusterName: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def serviceOne = getService(
                projectName,
                sampleOneName,
                clusterName,
                envName
        )
        def serviceTwo = getService(
                projectName,
                sampleTwoName,
                clusterName,
                envName
        )


        assert result.outcome != 'error'
        assert serviceOne.service
        assert serviceOne.service.defaultCapacity == '3'
        assert serviceTwo.service
        assert serviceTwo.service.defaultCapacity == '3'
        def containersOne = serviceOne.service.container
        assert containersOne.size() == 2
        assert containersOne[0].containerName == 'nginx'
        assert containersOne[0].imageName == 'nginx'
        assert containersOne[0].imageVersion == '1.7.9'
        assert containersOne[1].containerName == 'nginx2'
        assert containersOne[1].imageName == 'nginx'
        assert containersOne[1].imageVersion == '1.7.10'
        def portOne1 = containersOne[0].port[0]
        assert portOne1
        assert portOne1.containerPort == '80'
        def portOne2 = containersOne[1].port[0]
        assert portOne2
        assert portOne2.containerPort == '90'
        def containersTwo = serviceTwo.service.container
        assert containersTwo.size() == 1
        assert containersTwo[0].containerName == 'nginx'
        assert containersTwo[0].imageName == 'nginx'
        assert containersTwo[0].imageVersion == '1.7.9'
        def portTwo = containersTwo[0].port[0]
        assert portTwo
        assert portTwo.containerPort == '80'
        cleanup:
        deleteService(projectName, sampleOneName)
        deleteService(projectName, sampleTwoName)
    }


    def getMappingDetail(service, name) {
        def parameterDetail = service.service?.parameterDetail.find {
            it.parameterName == name
        }.parameterValue
        return parameterDetail
    }


}
