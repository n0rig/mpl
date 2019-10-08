/**
 * Common build module
 */

println "Build module inside devops-pipeline-base"
if( fileExists('pom.xml') ) {
  MPLModule('Build/Maven.groovy', CFG)
}

if( fileExists('openshift') ) {
  MPLModule('Build/Openshift.groovy', CFG)
}
