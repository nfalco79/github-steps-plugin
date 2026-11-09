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

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Failure;
import hudson.model.Run;
import hudson.util.FormValidation;

public class GHPullRequestStep extends Step {

    private String credentialsId;
    private Integer prId;
    private String serverUrl;
    private final String owner;
    private final String repository;

    @DataBoundConstructor
    public GHPullRequestStep(@CheckForNull String owner, @CheckForNull String repository, @CheckForNull Integer prId) {
        this.owner = Util.fixEmptyAndTrim(owner);
        if (this.owner == null) {
            throw new Failure(Messages.Steps_requiredParameter("Owner"));
        }
        this.repository = Util.fixEmptyAndTrim(repository);
        if (this.repository == null) {
            throw new Failure(Messages.Steps_requiredParameter("Repository"));
        }
        this.prId = prId;
        if (this.prId == null) {
            throw new Failure(Messages.Steps_requiredParameter("Pull request identifier"));
        }
    }

    public String getOwner() {
        return owner;
    }

    public String getRepository() {
        return repository;
    }

    public Integer getPrId() {
        return prId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        this.serverUrl = Util.fixEmptyAndTrim(serverUrl);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GHPullRequestStepExecution(this, context);
    }

    private static class GHPullRequestStepExecution extends SynchronousStepExecution<GHPullRequest> {
        private static final long serialVersionUID = 1L;
        private final transient GHPullRequestStep step;

        private GHPullRequestStepExecution(GHPullRequestStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected GHPullRequest run() throws Exception {
            String token = null;
            if (step.credentialsId != null) {
                Run<?, ?> run = getContext().get(Run.class);
                StandardUsernamePasswordCredentials c = CredentialsProvider.findCredentialById(step.credentialsId, StandardUsernamePasswordCredentials.class, run);
                if (c != null) {
                    token = c.getPassword().getPlainText();
                }
            }
            if (token == null) {
                throw new Failure("GitHub credentials are required");
            }

            String endpoint = step.serverUrl != null ? step.serverUrl : StepConstant.GITHUB_API_ENDPOINT;
            GitHub gitHub = GitHubBuilder.fromEnvironment()
                    .withEndpoint(endpoint)
                    .withAppInstallationToken(token)
                    .build();

            GHRepository repository = gitHub.getRepository(step.owner + "/" + step.repository);
            return repository.getPullRequest(step.prId);
        }
    }

    @Extension
    public static class DescriptorImpl extends AbstractGHStepDescriptor {
        @Override
        public String getFunctionName() {
            return "ghPullRequest";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Get pull request details of repository in GitHub";
        }

        public FormValidation doCheckOwner(@QueryParameter String owner) {
            return FormValidationBuilder.checkOwner(owner);
        }

        public FormValidation doCheckRepository(@QueryParameter String repository) {
            return FormValidationBuilder.checkRepository(repository);
        }

        public FormValidation doCheckPrId(@QueryParameter Integer prId) {
            return FormValidationBuilder.checkPrId(true, prId);
        }
    }
}
