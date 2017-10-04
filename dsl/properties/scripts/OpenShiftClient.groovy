
public class OpenShiftClient extends KubernetesClient {

    def createOrUpdatePlatformSpecificResources(String clusterEndPoint, String namespace, def serviceDetails, String accessToken) {
        if (OFFLINE) return null
        createOrUpdateRoute(clusterEndPoint, namespace, serviceDetails, accessToken)
    }

    def createOrUpdateRoute(String clusterEndPoint, String namespace, def serviceDetails, String accessToken) {
        String routeName = getServiceParameter(serviceDetails, 'routeName')

        if (!routeName) {
            //bail out - not creating route if weren't asked to
            return null
        }

        def response = doHttpGet(clusterEndPoint,
                "/oapi/v1/namespaces/${namespace}/routes/${routeName}",
                accessToken, /*failOnErrorCode*/ false)
        if (response.status == 200){
            logger INFO, "Route $routeName found in $namespace, updating route ..."
            createOrUpdateRoute(/*existingRoute*/ response.data, routeName, clusterEndPoint, namespace, serviceDetails, accessToken)
        } else if (response.status == 404){
            logger INFO, "Route $routeName does not exist in $namespace, creating route ..."
            createOrUpdateRoute(/*existingRoute*/ null, routeName, clusterEndPoint, namespace, serviceDetails, accessToken)
        } else {
            handleError("Route check failed. ${response.statusLine}")
        }
    }


    def createOrUpdateRoute(def existingRoute, String routeName, String clusterEndPoint, String namespace, def serviceDetails, String accessToken) {
        String routeHostname = getServiceParameter(serviceDetails, 'routeHostname')
        String routePath = getServiceParameter(serviceDetails, 'routePath', '/')
        String routeTargetPort = getServiceParameter(serviceDetails, 'routeTargetPort')

        if (!routeHostname) {
            handleError("Hostname for the route not specified.")
        }

        def payload = buildRoutePayload(routeName, routeHostname, routePath, routeTargetPort, serviceDetails, existingRoute)

        def createRoute = existingRoute == null
        doHttpRequest(createRoute ? POST : PUT,
                clusterEndPoint,
                createRoute?
                        "/oapi/v1/namespaces/${namespace}/routes" :
                        "/oapi/v1/namespaces/${namespace}/routes/${routeName}",
                ['Authorization' : accessToken],
                /*failOnErrorCode*/ true,
                payload)
    }

    String buildRoutePayload(String routeName, String routeHostname, String routePath, String routeTargetPort, def serviceDetails, def existingRoute) {
        String serviceName = formatName(serviceDetails.serviceName)
        def json = new JsonBuilder()
        def result = json{
            kind "Route"
            apiVersion "v1"
            metadata {
                name routeName
            }
            spec {
                host routeHostname
                path routePath
                to {
                    kind "Service"
                    name serviceName
                }
                if (routeTargetPort) {
                    port {
                        targetPort routeTargetPort
                    }
                }
            }
        }
        // build the final payload by merging with the existing
        // route definition
        def payload = existingRoute
        if (payload) {
            payload = mergeObjs(payload, result)
        } else {
            payload = result
        }

        return (new JsonBuilder(payload)).toPrettyString()
    }
}