package com.electriccloud.client.api

import com.electriccloud.client.HttpClient
import io.fabric8.openshift.client.DefaultOpenShiftClient
import io.fabric8.openshift.client.OpenShiftClient
import io.fabric8.openshift.client.OpenShiftConfigBuilder

class OpenshiftApi extends HttpClient {

    OpenShiftClient client
    def username
    def config

    OpenshiftApi(username, password, token, serviceAccount, clusterEndpoint) {

        log.info("Connecting to OpenShift cluster: ${clusterEndpoint}")
        log.info("Service Account: ${username}")

        this.username = username
        this.config = new OpenShiftConfigBuilder()
                .withTrustCerts(true)
                .withUsername(username)
                .withPassword(password)
                .withUserAgent(serviceAccount)
                .withMasterUrl(clusterEndpoint)
                .withOpenShiftUrl("${clusterEndpoint}/oapi/v1")
                .withOauthToken(token)
                .build()
        this.client = new DefaultOpenShiftClient(config)
    }


}

