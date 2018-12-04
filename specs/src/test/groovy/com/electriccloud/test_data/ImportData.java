package com.electriccloud.test_data;

import com.electriccloud.procedures.OpenshiftTestBase;
import org.testng.annotations.DataProvider;

public class ImportData extends OpenshiftTestBase {




    @DataProvider(name = "importData")
    public Object[][] getImportData(){
        return new Object[][] {
                {
                    serviceName,null,projectName,environmentName,"",false,null,
                        "Either specify all the parameters required to identify the OpenShift-backed ElectricFlow cluster"
                },
                {
                    serviceName,null,projectName,environmentName,"my-cluster",false,null,
                        "Cluster \'my-cluster\' does not exist in \'" + environmentName + "\' environment!"
                },
                {
                    serviceName,null,projectName,"",clusterName,false,null,
                        "Either specify all the parameters required to identify the OpenShift-backed ElectricFlow cluster"
                },
                {
                    serviceName,null,projectName,"my-environment",clusterName,false,null,
                        "Environment \'my-environment\' does not exist in project \'" + projectName + "\'"
                },
                {
                    serviceName,null,"Default",environmentName,clusterName,false,null,
                        "Environment \'" + environmentName + "\' does not exist in project \'Default\'"
                },
                {
                    "nginx-service-invalid",null,projectName,environmentName,clusterName,false,null,
                        "Caught: expected a single document in the stream"
                },
                {
                    applicationName,null,projectName,environmentName,"",true,applicationName,
                        "Either specify all the parameters required to identify the OpenShift-backed ElectricFlow cluster"
                },
                {
                    applicationName,null,projectName,environmentName,"my-cluster",true,applicationName,
                        "Cluster \'my-cluster\' does not exist in \'" + environmentName + "\' environment!"
                },
                {
                    applicationName,null,projectName,"",clusterName,true,applicationName,
                        "Either specify all the parameters required to identify the OpenShift-backed ElectricFlow cluster"
                },
                {
                    applicationName,null,projectName,"my-environment",clusterName,true,applicationName,
                        "Environment \'my-environment\' does not exist in project \'" + projectName + "\'"
                },
                {
                    applicationName,null,"Default",environmentName,clusterName,true,applicationName,
                        "Environment \'" + environmentName + "\' does not exist in project \'Default\'"
                },
                {
                    "nginx-service-invalid",null,projectName,environmentName,clusterName,true,applicationName,
                        "Caught: expected a single document in the stream"
                },
                {
                    applicationName,null,projectName,environmentName,clusterName,true,"",
                        "Application name is required for creating application-scoped microservices"
                }
        };
    }




}
