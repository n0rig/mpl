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

import com.devops_pipeline_base.mpl.MPLManager

/**
 * Easy way to set additional path with library modules
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 * @param path  path to the library modules
 */
def call(String path) {
  MPLManager.instance.addModulesLoadPath(path)
  MPLManager.instance.addPipelinesLoadPath(path)
}
