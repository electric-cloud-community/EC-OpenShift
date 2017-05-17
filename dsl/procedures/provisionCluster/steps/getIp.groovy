import groovy.json.*

$[/myProject/scripts/preamble]


def jobId = "$[/myJob]"
EFClient efClient = new EFClient()

def result = efClient.doHttpGet("/rest/v1.0/jobs/$jobId")

println result.data.job.propertySheet.property[0].propertyName
println result.data.job.propertySheet.property[0].propertySheetId

def propertySheetId = result.data.job.propertySheet.property[0].propertySheetId
result = efClient.doHttpGet("/rest/v1.0/propertySheets/$propertySheetId")

println result.data.propertySheet.property[0].propertyName
println result.data.propertySheet.property[0].propertySheetId

propertySheetId = result.data.propertySheet.property[0].propertySheetId

result = efClient.doHttpGet("/rest/v1.0/propertySheets/$propertySheetId")

println result.data.propertySheet.property[0].propertyName
println result.data.propertySheet.property[0].propertySheetId
println result.data.propertySheet.property[0].value

def esx_vm = result.data.propertySheet.property[0].value

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(esx_vm)


def openShiftMasterIP = object.ip_address
println openShiftMasterIP

def payload = [:]
payload << [
        propertyName: "OpenShiftMasterIP",
        value: openShiftMasterIP,
        jobId: jobId
]

efClient.doHttpPost("/rest/v1.0/properties", /* request body */ payload)