
import com.devops_pipeline_base.mpl.Helper
import com.devops_pipeline_base.mpl.MPLManager
import com.devops_pipeline_base.mpl.MPLPipelineException

/**
 * Finding pipeline implementation and executing it with specified configuration
 *
 * Logic:
 *   Pipeline finding: workspace --> nested lib 2 --> nested lib 1 --> MPL library
 *   Loop protection: There is no way to run currently active pipeline again
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 * @param name  used to determine the pipeline name, by default it's current stage name (ex. "Maven Build")
 * @param cfg   pipeline configuration to override. Will update the common pipeline configuration
 */
def call(String name = null, Map pipeline_configuration = [:], Map defaults = [:], Map overrides = [:]) {

  if( name == null )
    throw new MPLPipelineException("No pipeline name provided!")

  def MPL = MPLPipelineConfig(pipeline_configuration, defaults, overrides)

  // Trace of the running pipelines to find loops
  // Also to make ability to use lib pipeline from overridden one
  def active_pipelines = MPLManager.instance.getActivePipelines()

  // Determining the pipeline source file and location
  def override_folder_name = MPLManager.instance.getOverrideFolderName()
  def pipeline_path = null
  def pipeline_name = null
  def override_path = null

  if (name.contains(".groovy")) {
    pipeline_name = "${name.replaceAll("\\s","")}".toString()
    pipeline_path = "pipelines/${pipeline_name}".toString()
    override_path = "${override_folder_name}/${pipeline_path}".toString()
  } else {
    throw new MPLPipelineException("The full pipeline path is required (e.g. build/make.groovy)!")
  }

  // Reading pipeline definition from workspace or from the library resources
  def pipeline_src = null

  def file = new File(override_path)

  // Ensure the pipeline we are about to execute has not been executed before.
  // This prevents the possibility of an infinite loop.
  if(! active_pipelines.contains(override_path)) {
    // Check if there exists a local "override" for this specific pipeline.
    node() {
      if(fileExists(override_path)) {
        println "[!] Executing Pipeline '${pipeline_name}' from local directory '${override_path}'."
        pipeline_src = readFile(override_path)
        pipeline_path = override_path
      }
    }
  }

  if (pipeline_src == null) {
    // Ensure the pipeline we are about to execute has not been executed before.
    // This prevents the possibility of an infinite loop.
    pipeline_src = Helper.getPipelinesList(pipeline_path).reverse().find { it ->
      pipeline_path = "library:${it.first()}".toString()
      ! active_pipelines.contains(pipeline_path)
    }?.last()
  }

  if( ! pipeline_src )
    throw new MPLPipelineException("Unable to find the pipeline '${pipeline_name}'! Cannot execute missing pipeline! Check case-sensitivity and if file exists.")

  try {
    MPLManager.instance.pushActivePipeline(pipeline_path)

    println "[!] Executing Pipeline '${pipeline_name}' from '${pipeline_path.split("/")[0]}'."
    Helper.runPipeline(pipeline_src, pipeline_path, [MPL: MPL])
  }
  catch( ex ) {
    def newex = new MPLPipelineException("Found error during execution of the pipeline '${pipeline_path}':\n${ex}")
    newex.setStackTrace(Helper.getPipelineStack(ex))
    throw newex
  }
  finally {
    MPLManager.instance.popActivePipeline()
  }
}
