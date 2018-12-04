package dsl.flow

def names = args.params,
        replicas = names.replicas.toString(),
        sourceVolume = names.sourceVolume,
        targetVolume = names.targetVolume,
        isCanary = names.isCanary.toString(),
        routeHost = names.routeHost,
        servType = names.servType


project 'openshiftProj', {

    service 'nginx-service', {
        defaultCapacity = replicas.toString()
        maxCapacity = (replicas + 1).toString()
        minCapacity = '1'
        volume = sourceVolume

        container 'nginx-container', {
            description = ''
            cpuCount = '0.1'
            cpuLimit = '2'
            imageName = 'tomaskral/nonroot-nginx'
            imageVersion = 'latest'
            memoryLimit = '255'
            memorySize = '128'
            serviceName = 'nginx-service'
            volumeMount = targetVolume

            environmentVariable 'NGINX_PORT', {
                type = 'string'
                value = '8080'
            }

            port 'http', {
                containerName = 'nginx-container'
                containerPort = '8080'
                projectName = 'openshiftProj'
                serviceName = 'nginx-service'
            }
        }

        environmentMap 'openshiftEnvMapping', {
            environmentName = 'openshift-environment'
            environmentProjectName = 'openshiftProj'
            projectName = 'openshiftProj'
            serviceName = 'nginx-service'

            serviceClusterMapping 'openshiftClusterMapping', {
                actualParameter = [
                        'canaryDeployment': isCanary,
                        'numberOfCanaryReplicas': replicas.toString(),
                        'requestType': 'update',
                        'routeHostname': routeHost,
                        'routeName': 'nginx-route',
                        'routePath': '/',
                        'routeTargetPort': 'servicehttpnginx-container01530626345623',
                        'serviceType': servType,
                        'sessionAffinity': 'None'
                ]
                clusterName = 'kube-cluster'
                environmentMapName = 'openshiftEnvMapping'
                serviceName = 'nginx-service'
                serviceMapDetail 'nginx-container', {
                    serviceMapDetailName = 'e8adf780-494b-11e8-8ee4-00155d01ef00'
                    serviceClusterMappingName = 'openshiftClusterMapping'
                }
            }
        }

        port '_servicehttpnginx-container01530626345623', {
            listenerPort = '81'
            projectName = 'openshiftProj'
            serviceName = 'nginx-service'
            subcontainer = 'nginx-container'
            subport = 'http'
        }

        process 'Deploy', {
            processType = 'DEPLOY'
            serviceName = 'nginx-service'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'deploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = null
                errorHandling = 'failProcedure'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'service'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = null
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = null
                subserviceProcess = null
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null
                property 'ec_deploy', {
                    ec_notifierStatus = '0'
                }
            }

            property 'ec_deploy', {
                ec_notifierStatus = '0'
            }
        }

        process 'Undeploy', {
            processType = 'UNDEPLOY'
            serviceName = 'nginx-service'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'Undeploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'service'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = null
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = 'nginx-service'
                subserviceProcess = null
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null

                property 'ec_deploy', {
                    ec_notifierStatus = '0'
                }
            }
            property 'ec_deploy', {
                ec_notifierStatus = '0'
            }
        }
        property 'ec_deploy', {
            ec_notifierStatus = '0'
        }
        jobCounter = '51'
    }
}