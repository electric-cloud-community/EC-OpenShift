def projName = args.projectName
def params = args.params

project projName, {
  procedure 'Discover', {
    params.each {k, v ->
      formalParameter k, defaultValue: '', {
        type = 'textarea'
      }
    }
    step 'Discover', {
      subproject = '/plugins/EC-OpenShift/project'
      subprocedure = 'Discover'

      params.each { k, v ->
        actualParameter k, value: '$[' + k + ']'
      }
    }
  }
}
