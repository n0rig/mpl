//
// Copyright (c) 2018 Grid Dynamics International, Inc. All Rights Reserved
// https://www.griddynamics.com
//
// Classification level: Public
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Id: $
// @Project:     MPL
// @Description: Shared Jenkins Modular Pipeline Library
//

import com.devops_pipeline_base.mpl.Helper
import com.devops_pipeline_base.mpl.MPLManager
import com.devops_pipeline_base.mpl.MPLModuleException

import java.nio.file.Paths
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.libs.LibrariesAction

import hudson.model.Run
import hudson.FilePath

/**
 * Finding module implementation and executing it with specified configuration
 *
 * Logic:
 *   Module finding: workspace --> nested lib 2 --> nested lib 1 --> MPL library
 *   Loop protection: There is no way to run currently active module again
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 * @param name  used to determine the module name, by default it's current stage name (ex. "Maven Build")
 * @param config   module configuration to override. Will update the common module configuration
 */
def call(String name = null, Map config = null) {

  if( name == null )
    throw new MPLModuleException("No module name provided!")

  if( config == null )
    config = MPLManager.instance.moduleConfig(name)

  // Trace of the running modules to find loops
  // Also to make ability to use lib module from overridden one
  def active_modules = MPLManager.instance.getActiveModules()

  // Determining the module source file and location
  def override_folder_name = MPLManager.instance.getOverrideFolderName()
  def module_path = null
  def module_name = null
  def override_path = null

  if (name.contains(".groovy")) {
    module_name = "${name.replaceAll("\\s","")}".toString()
    module_path = "modules/${module_name}".toString()
    override_path = "${override_folder_name}/${module_path}".toString()
  } else {
    throw new MPLModuleException("The full module path is required (e.g. build/make.groovy)!")
  }

  // Reading module definition from workspace or from the library resources
  def module_src = null

  if(env.NODE_NAME != null && fileExists(override_path) && (! active_modules.contains(override_path)) ) {
    println "[!] Executing Module '${module_name}' from local directory '${override_path}'."
    module_src = readFile(override_path)
    module_path = override_path
  } else {
    // Searching for the not executed module from the loaded libraries
    module_src = Helper.getModulesList(module_path).find { it ->
      module_path = "library:${it.first()}".toString()
      ! active_modules.contains(module_path)
    }?.last()

    if (module_src != null) {
      println "[!] Executing Module '${module_name}' from '${module_path.split("/")[0]}'."
    }
  }

  if( ! module_src )
    throw new MPLModuleException("Unable to find the module '${module_name}'! Cannot execute missing module! Check case-sensitivity and if file exists.")

  try {
    MPLManager.instance.pushActiveModule(module_path)
    Helper.runModule(module_src, module_path, [CFG: Helper.flatten(config)])
  }
  catch( ex ) {
    def newex = new MPLModuleException("Found error during execution of the module '${module_path}':\n${ex}")
    newex.setStackTrace(Helper.getModuleStack(ex))
    throw newex
  }
  finally {
    MPLManager.instance.modulePostStepsRun(module_path)
    def errors = MPLManager.instance.getPostStepsErrors(module_path)
    if( errors ) {
      for( int e in errors )
        println "Module '${module_name}' got error during execution of poststep from module '${e.module}': ${e.error}"
      def newex = new MPLModuleException("Found error during execution poststeps for the module '${module_path}'")
      newex.setStackTrace(Helper.getModuleStack(newex))
      throw newex
    }
    MPLManager.instance.popActiveModule()
  }
}
