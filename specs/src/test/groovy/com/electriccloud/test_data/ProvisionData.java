package com.electriccloud.test_data;

import com.electriccloud.procedures.OpenshiftTestBase;
import org.testng.annotations.DataProvider;


public class ProvisionData extends OpenshiftTestBase {

    @DataProvider(name = "invalidData")
    public Object[][] getProvisionData(){
        return new Object[][] {
                {
                    "test", environmentName, clusterName,
                        "NoSuchProject: Project 'test' does not exist"
                },
                {
                    "Default", environmentName, clusterName,
                        "NoSuchEnvironment: Environment '" + environmentName + "' does not exist in project 'Default'"
                },
                {
                    projectName, "test", clusterName,
                        "NoSuchEnvironment: Environment 'test' does not exist in project '" + projectName + "'"
                },
                {
                    projectName, environmentName, "test-cluster",
                        "NoSuchCluster: Cluster 'test-cluster' does not exist in environment '" + environmentName + "'"
                }

        };
    }


}
