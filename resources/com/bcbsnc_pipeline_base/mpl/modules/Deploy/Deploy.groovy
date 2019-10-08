/**
 * Common deploy module
 */

println "Deploy module inside devops-pipeline-base"
MPLModule('Deploy/Openshift.groovy', CFG)
