import spock.lang.*
import com.electriccloud.spec.*

class ImportFromTemplate extends OpenShiftHelper {
    static def openshiftTemplate
    static def projectName = 'Test'
    static def applicationScoped = true
    static def applicationName = 'OpenShift Spec App'
    static def envName = 'Test'
    static def serviceName = 'openShift-spec-import-test'
    static def clusterName = 'Cluster 3'
    static def configName

    def doSetupSpec() {
        configName = 'OpenShift Spec Config'
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/ImportFromTemplate.dsl', [
            projectName: projectName,
            params: [
                osTemplateYaml:       '',
                templateParamValues: '',
                projName:           '',
                application_scoped: '',
                application_name:   '',
                envProjectName:     '',
                envName:            '',
                clusterName:        '',
            ]
        ]

    }

    def doCleanupSpec() {
        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    def "top level service"() {
        given:
            def sampleName = 'my-service-nginx-deployment-withTempParams'
            cleanupService(sampleName)
            def tempParamValues = 'protocol = TCP, port = 1234'
            openshiftTemplate =
'''
kind: "Template"
apiVersion: "v1"
metadata:
  name: my-template
objects:
  - kind: Service
    apiVersion: v1
    metadata:
      name: another-nginx-service
      namespace: default
      selfLink: /api/v1/namespaces/default/services/nginx-service
      labels:
        ec-svc: nginx-service
    spec:
      ports:
      - name: nginx-container-port-1
        protocol: {protocol}
        port: {port}
        targetPort: http
        nodePort: 32375
      selector:
        ec-svc: nginx-service
      loadBalancerIP: 35.192.4.49
      type: LoadBalancer
  - kind: Deployment
    apiVersion: v1beta2
    metadata:
      name: nginx-service
      namespace: default
      selfLink: /api/v1/namespaces/default/services/nginx-service
      generation: 1
      labels:
        ec-svc: nginx-service
        ec-track: stable
    spec:
      replicas: 2
      selector:
        matchLabels:
          ec-svc: nginx-service
      template:
        metadata:
          name: nginx-service
          labels:
            ec-svc: nginx-service
            ec-track: stable
        spec:
          containers:
          - name: nginx-container
            image: nginx:stable
            ports:
            - protocol: TCP
              containerPort: 80
              name: http
            resources:
              limits:
                cpu: 2
                memory: 255M
              requests:
                cpu: 0.1
                memory: 128M
            volumeMounts:
            - mountPath: "/usr/share/nginx/html"
              name: nginx-html
          volumes:
          - name: nginx-html
            hostPath:
              path: /var/lib/docker
              type: Directory
'''.trim()
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        osTemplateYaml: '''$openshiftTemplate''',
                        templateParamValues: '$tempParamValues',
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
            assert service.service.defaultCapacity == '2'
            def containers = service.service.container
            assert containers.size() == 1
            assert containers[0].containerName == 'nginx-container'
            assert containers[0].imageName == 'nginx'
            assert containers[0].imageVersion == 'stable'
            def port = containers[0].port[0]
            assert port
            assert port.containerPort == '80'
    }

    def "application-scoped-service"() {
        given:
        def sampleName = 'my-service-nginx-deployment-withOutTempParams'
        cleanupService(sampleName)
        openshiftTemplate =
                '''
kind: "Template"
apiVersion: "v1"
metadata:
  name: my-template
objects:
  - kind: Service
    apiVersion: v1
    metadata:
      name: another-nginx-service
      namespace: default
      selfLink: /api/v1/namespaces/default/services/nginx-service
      labels:
        ec-svc: nginx-service
    spec:
      ports:
      - name: nginx-container-port-1
        protocol: TCP
        port: 80
        targetPort: http
        nodePort: 32375
      selector:
        ec-svc: nginx-service
      loadBalancerIP: 35.192.4.49
      type: LoadBalancer
  - kind: Deployment
    apiVersion: v1beta2
    metadata:
      name: nginx-service
      namespace: default
      selfLink: /api/v1/namespaces/default/services/nginx-service
      generation: 1
      labels:
        ec-svc: nginx-service
        ec-track: stable
    spec:
      replicas: 2
      selector:
        matchLabels:
          ec-svc: nginx-service
      template:
        metadata:
          name: nginx-service
          labels:
            ec-svc: nginx-service
            ec-track: stable
        spec:
          containers:
          - name: nginx-container
            image: nginx:stable
            ports:
            - protocol: TCP
              containerPort: 80
              name: http
            resources:
              limits:
                cpu: 2
                memory: 255M
              requests:
                cpu: 0.1
                memory: 128M
            volumeMounts:
            - mountPath: "/usr/share/nginx/html"
              name: nginx-html
          volumes:
          - name: nginx-html
            hostPath:
              path: /var/lib/docker
              type: Directory
'''.trim()
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        osTemplateYaml: '''$openshiftTemplate''',
                        projName: '$projectName',
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
        assert service.service.defaultCapacity == '2'
        def containers = service.service.container
        assert containers.size() == 1
        assert containers[0].containerName == 'nginx-container'
        assert containers[0].imageName == 'nginx'
        assert containers[0].imageVersion == 'stable'
        def port = containers[0].port[0]
        assert port
        assert port.containerPort == '80'
    }

    def "many top-level services"() {
        given:
        def sampleOneName = 'my-service1-nginx-deployment1'
        def sampleTwoName = 'my-service2-nginx-deployment2'
        cleanupService(sampleOneName)
        cleanupService(sampleTwoName)
        openshiftTemplate =
                '''
kind: "Template"
apiVersion: "v1"
metadata:
  name: my-template
objects:
  - kind: Service
    apiVersion: v1
    metadata:
      name: my-service1
    spec:
      selector:
        app: nginx1
      ports:
      - protocol: TCP
        port: 80
        targetPort: 9376
    apiVersion: apps/v1
  - kind: Deployment
    metadata:
      name: nginx-deployment1
      labels:
        app: nginx1
    spec:         
      replicas: 3
      selector:
        matchLabels:
          app: nginx
      template:
        metadata:
          labels:
            app: nginx1
        spec:
          containers:
          - name: nginx
            image: nginx:1.7.9
            resources:
              limits:
                memory: 20480k
                cpu: 0.7
              requests:
                memory: 0.5g
                cpu: 0.5
            ports:
            - containerPort: 80
          - name: nginx2
            image: nginx:1.7.10
            resources:
              limits:
                memory: 0.01t
                cpu: 0.7
              requests:
                memory: 0.001p
                cpu: 0.5
            ports:
            - containerPort: 90
  - kind: Service
    apiVersion: v1
    metadata:
      name: my-service2
    spec:
      selector:
        app: nginx2
      ports:
      - protocol: TCP
        port: 80
        targetPort: 9376
    apiVersion: apps/v1
  - kind: Deployment
    metadata:
      name: nginx-deployment2
      labels:
        app: nginx2
    spec:         
      replicas: 3
      selector:
        matchLabels:
          app: nginx
      template:
        metadata:
          labels:
            app: nginx2
        spec:
          containers:
          - name: nginx
            image: nginx:1.7.9
            resources:
              limits:
                memory: 20480k
                cpu: 0.7
              requests:
                memory: 0.5g
                cpu: 0.5
            ports:
            - containerPort: 80
'''.trim()
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        openshiftTemplate: '''$openshiftTemplate''',
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

    }
}