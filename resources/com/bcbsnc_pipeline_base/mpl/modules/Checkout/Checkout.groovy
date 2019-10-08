/**
 * Common checkout module
 */

println "Checkout module inside devops-pipeline-base"
if( CFG.'git.url' )
  MPLModule('Checkout/Git.groovy', CFG)
else
  MPLModule('Checkout/Scm.groovy', CFG)
