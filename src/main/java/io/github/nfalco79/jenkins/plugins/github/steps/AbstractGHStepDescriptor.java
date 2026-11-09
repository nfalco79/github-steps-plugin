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

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.springframework.security.core.Authentication;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public abstract class AbstractGHStepDescriptor extends StepDescriptor {
    @Override
    public Set<? extends Class<?>> getRequiredContext() {
        return Set.of(Run.class, FilePath.class);
    }

    @POST
    public FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item projectOrFolder,
                                               @QueryParameter String credentialsId,
                                               @QueryParameter(fixEmpty = true, required = false) String serverURL) {
        if ((projectOrFolder == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)) ||
                (projectOrFolder != null && !projectOrFolder.hasPermission(Item.EXTENDED_READ)
                        && !projectOrFolder.hasPermission(CredentialsProvider.USE_ITEM))) {
            return FormValidation.ok();
        }
        if (StringUtils.isBlank(credentialsId)) {
            return FormValidation.ok();
        }

        if (serverURL == null) {
            serverURL = StepConstant.GITHUB_API_ENDPOINT;
        }
        List<DomainRequirement> domainRequirement = URIRequirementBuilder.fromUri(serverURL).build();
        Authentication authentication = getAuthentication(projectOrFolder);
        CredentialsMatcher matcher = CredentialsMatchers.withId(credentialsId);
        if (CredentialsProvider.listCredentialsInItem(StandardUsernamePasswordCredentials.class, projectOrFolder, authentication, domainRequirement, matcher).isEmpty()) {
            return FormValidation.error(Messages.Steps_invalidCredentialsId());
        }
        return FormValidation.ok();
    }

    @POST
    public ListBoxModel doFillCredentialsIdItems(final @CheckForNull @AncestorInPath ItemGroup<?> context,
                                                 final @CheckForNull @AncestorInPath Item projectOrFolder,
                                                 @QueryParameter String credentialsId,
                                                 @QueryParameter(fixEmpty = true, required = false) String serverURL) {
        Permission permToCheck = projectOrFolder == null ? Jenkins.ADMINISTER : Item.CONFIGURE;
        AccessControlled contextToCheck = projectOrFolder == null ? Jenkins.get() : projectOrFolder;
        credentialsId = StringUtils.trimToEmpty(credentialsId);

        // If we're on the global page and we don't have administer
        // permission or if we're in a project or folder
        // and we don't have configure permission there
        if (!contextToCheck.hasPermission(permToCheck)) {
            return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
        }

        if (serverURL == null) {
            serverURL = StepConstant.GITHUB_API_ENDPOINT;
        }
        Authentication authentication = getAuthentication(projectOrFolder);
        List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(serverURL).build();
        CredentialsMatcher either = CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class);
        Class<StandardCredentials> type = StandardCredentials.class;

        return new StandardListBoxModel() //
                .includeMatchingAs(authentication, context, type, domainRequirements, either) //
                .includeEmptyValue();
    }

    @NonNull
    protected Authentication getAuthentication(Item item) {
        return item instanceof Queue.Task task ? Tasks.getAuthenticationOf2(task) : ACL.SYSTEM2;
    }
}
