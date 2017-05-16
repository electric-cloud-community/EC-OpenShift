import groovy.json.*

$[/myProject/scripts/preamble]


def jobId = "$[/myJob]"
EFClient efClient = new EFClient()
def result = efClient.doHttpGet("/rest/v1.0/jobs/$jobId")

/*
*   Here the jobStep[1] is the first step of provisionCluster procedure i.e. Import Master node
*/
println result.data.job.jobStep[1].liveProcedureStep


def jobStepId = result.data.job.jobStep[1].jobStepId
println jobStepId 

result = efClient.doHttpGet("/rest/v1.0/jobSteps/$jobStepId")

println result.data.jobStep.calledProcedure.jobStep.propertySheet.property[1].value[1]


def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(result.data.jobStep.calledProcedure.jobStep.propertySheet.property[1].value[1])

println object.ip_address
