/*
 * Copyright 2023 Nikolas Falco
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.nfalco79.jenkins.plugins.github.steps;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Util;
import hudson.util.FormValidation;

public class FormValidationBuilder {
    public static FormValidation checkOwner(@CheckForNull String owner) {
        if (Util.fixEmptyAndTrim(owner) == null) {
            return FormValidation.error(Messages.Steps_requiredParameter("workspace name"));
        }
        return FormValidation.ok();
    }

    public static FormValidation checkRepository(@CheckForNull String repository) {
        if (Util.fixEmptyAndTrim(repository) == null) {
            return FormValidation.error(Messages.Steps_requiredParameter("repository name"));
        }
        return FormValidation.ok();
    }

    public static FormValidation checkPrId(boolean required, @CheckForNull Integer prId) {
        if (prId == null && required) {
            return FormValidation.error(Messages.Steps_requiredParameter("pull request identifier"));
        }
        if (prId != null && prId <= 0) {
            return FormValidation.error(Messages.GHPullRequestStep_invalidPullRequestId());
        }
        return FormValidation.ok();
    }

}