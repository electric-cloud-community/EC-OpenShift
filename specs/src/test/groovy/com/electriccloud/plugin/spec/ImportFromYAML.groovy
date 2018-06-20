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
            params: [
                osTemplateYaml:       '',
                templateParamValues:  '',
                projName:             '',
                application_scoped:   '',
                application_name:   '',
                envProjectName:     '',
                envName:            '',
                clusterName:       '',
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

    def 'with hpa'() {
        given:
        def serviceName = 'my-service'
        def autoscalerName = 'my-autoscaler'
        kubeYAMLFile = getTemplate('withHPA.yaml', [serviceName: serviceName, autoscalerName: autoscalerName])
        def paramValues = 'DOCKERIMAGE_VERSION=1.0, REPLICAS=3'
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
        cleanup:
        deleteService(projectName, serviceName)
    }

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

    def getMappingDetail(service, name) {
      def parameterDetail = service.service?.parameterDetail.find {
        it.parameterName == name
      }.parameterValue
      return parameterDetail
    }

}
