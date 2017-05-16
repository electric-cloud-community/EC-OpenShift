import groovy.json.*

$[/myProject/scripts/preamble]


def jobId = "$[/myJob]"
EFClient efClient = new EFClient()
def result = efClient.doHttpGet("/rest/v1.0/jobs/$jobId")
println result.data.job.jobStep[1].liveProcedureStep


def jobStepId = result.data.job.jobStep[1].jobStepId
println jobStepId 

result = efClient.doHttpGet("/rest/v1.0/jobSteps/$jobStepId")

println "----STEP Desc----"
println result.data
println "----STEP Desc----"

println result.data.calledProcedure.jobStep.propertySheet.property[1]
