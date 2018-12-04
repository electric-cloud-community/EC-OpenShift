package com.electriccloud.client.ectool

import com.electriccloud.models.config.ConfigHelper
import groovy.xml.XmlUtil
import io.qameta.allure.Step
import org.apache.log4j.Logger

import java.nio.file.Paths
import java.util.concurrent.TimeoutException

class EctoolApi {


    private static Logger log = Logger.getLogger("appLogger")
    EctoolClient ectoolClient
    def ectoolDebug

    EctoolApi(debugMode = false){
        this.ectoolClient = new EctoolClient()
        this.ectoolDebug = debugMode
    }

    @Step
    String run(Object ... command) {
        String _command = ectoolClient.run(command)
        if(ectoolDebug)
        {
            log.debug(command.toList().toString())
            if(_command.contains('<response')){
                def parsed = new XmlParser().parseText(_command)
                log.debug(XmlUtil.serialize(parsed))
            } else {
                log.debug(_command)
            }
        }
        _command
    }

    @Step
    String ectoolLogin() {
        ConfigHelper.message("ec-tool authorization")
        run 'ectool', 'login', ectoolClient.ecUsername, ectoolClient.ecPassword
    }

    @Step
    String dsl(String dslText) {
        def out = run 'ectool', 'evalDsl', dslText
        log.debug(out)
        out
    }

    @Step
    String dsl(String dslText, json) {
        run 'ectool', 'evalDsl', dslText, '--parameters', json
    }

    @Step
    String dsl(List<String> path) {
        run 'ectool', 'evalDsl', '--dslFile', ConfigHelper.dslPath(path[0], path[1])
    }

    @Step
    String dsl(List<String> path, json) {
        def out = run 'ectool', 'evalDsl', '--dslFile', ConfigHelper.dslPath(path[0], path[1]), '--parameters', json
        log.debug(out)
        out
    }

    // todo remove
    @Step
    def runDsl(plugin, fileName){
        ConfigHelper.message("running dsl")
        def resp = run('ectool', 'evalDsl', '--dslFile', ConfigHelper.dslPath(plugin, fileName))
        waitForRuntime(resp, 30, 2, 'Procedure is successfully finished!')
        ConfigHelper.xml(resp)
    }

    @Step
    def runDsl(plugin, fileName, jsonParams){
        ConfigHelper.message("running dsl")
        def resp = run('ectool', 'evalDsl', '--dslFile', ConfigHelper.dslPath(plugin, fileName), '--parameters', jsonParams)
        waitForRuntime(resp, 30, 2, 'Procedure is successfully finished!')
        ConfigHelper.xml(resp)
    }


    def getJobStatus(jobId) {
        ConfigHelper.xml(dsl (/getJobStatus(jobId: '$jobId')/))
    }

    def getJobSummary(jobId) {
        ConfigHelper.xml(dsl (/getJobSummary(jobId: '$jobId')/))
    }

    def getJobLogs(jobId, workspace) {
        def job = getJobSummary(jobId).job.jobStep.last()
        Paths.get(workspace, job.jobName, job.logFileName).toFile().text
    }


    def waitForRuntime(outcome, seconds = 200, periodSec = 1, message = "Job status: COMPLETED.") {

        def step = 0
        def jobId      = ConfigHelper.xml(outcome).jobId.toString()
        def job        = getJobStatus(jobId)
        def jobStatus  = job.status.toString()
        def jobOutcome = job.outcome.toString()

        ectoolDebug = false

        while (jobStatus != "completed") {
            if(jobOutcome != "error") {
                sleep(periodSec * 1000)
                step++
                log.info("Job status: ${jobStatus = getJobStatus(jobId).status.toString()}; waiting for: ${periodSec * (step)} sec.\r")
            }

            if (seconds <= step * periodSec) {
                throw new TimeoutException("""
Timed Out:
    The job status: ${jobStatus}
    Job is not finished in $seconds seconds!
""",)
            }
        }
        log.info(message)
        ectoolDebug = true

        if(jobOutcome == "error") {
            throw new RuntimeException(
                    """
 Job status: FAILED: ${jobOutcome}
 Job error log:      ${getJobLogs(jobId, ConfigHelper.confStatic('electricFlowConf').server.workspace)}
""",
                    new Throwable("${jobId}"))
        }
    }

    @Step
    def publishArtifact(artifactName, version, artifactsDir, fileName){
        ConfigHelper.message("publishing artifact")
        if (fileName){
            run 'ectool', 'publishArtifactVersion', '--version', version, '--artifactName', artifactName, '--fromDirectory', artifactsDir, '--includePatterns', fileName
        } else {
            run 'ectool', 'publishArtifactVersion', '--version', version, '--artifactName', artifactName, '--fromDirectory', artifactsDir
        }
        log.info('Artifact is successfully published!')
    }

    @Step
    def installPlugin(filePath, fileName){
        ConfigHelper.message("installing ${fileName} plugin")
        ConfigHelper.xml(run('ectool', 'installPlugin', "${filePath}/${fileName}.jar", '--force', 'true'))
    }

    @Step
    def getPlugin(pluginName){
        ConfigHelper.message("get ${pluginName} plugin")
        ConfigHelper.xml(run('ectool', 'getPlugin', "${pluginName}"))
    }

    @Step
    def promotePlugin(pluginName){
        ConfigHelper.message("promoting ${pluginName} plugin")
        ConfigHelper.xml(run('ectool', 'promotePlugin', "${pluginName}"))
    }

    @Step
    def uninstallPlugin(pluginName){
        ConfigHelper.message("removing ${pluginName} plugin")
        ConfigHelper.xml(run ('ectool', 'uninstallPlugin', "${pluginName}"))
    }

    @Step
    def deleteProject(projectName){
        ConfigHelper.message('removing project')
        run 'ectool', 'deleteProject', projectName
    }


    @Step
    def deletePlugin(pluginName, pluginVersion){
        ConfigHelper.message('removing plugin with project')
        run'ectool', 'deletePlugin', "${pluginName}-${pluginVersion}"
        deleteProject "${pluginName}-${pluginVersion}"
        return this
    }


}