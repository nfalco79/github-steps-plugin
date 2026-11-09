/*
 * Copyright 2026 Nikolas Falco
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

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
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

public class GHCommitsStep extends Step {

    private String credentialsId;
    private Integer prId;
    private String serverURL;
    private final String owner;
    private final String repository;

    @DataBoundConstructor
    public GHCommitsStep(@CheckForNull String owner, @CheckForNull String repository) {
        this.owner = Util.fixEmptyAndTrim(owner);
        if (this.owner == null) {
            throw new Failure(Messages.Steps_requiredParameter("Owner"));
        }
        this.repository = Util.fixEmptyAndTrim(repository);
        if (this.repository == null) {
            throw new Failure(Messages.Steps_requiredParameter("Repository"));
        }
    }

    public String getOwner() {
        return owner;
    }

    public String getRepository() {
        return repository;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    public String getServerURL() {
        return serverURL;
    }

    @DataBoundSetter
    public void setServerURL(String serverURL) {
        this.serverURL = Util.fixEmptyAndTrim(serverURL);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GHCommitsStepExecution(this, context);
    }

    public Integer getPrId() {
        return prId;
    }

    @DataBoundSetter
    public void setPrId(Integer prId) {
        this.prId = prId;
    }

    private static class GHCommitsStepExecution extends SynchronousStepExecution<List<GHPullRequestCommitDetail>> {
        private static final long serialVersionUID = 1L;
        private final transient GHCommitsStep step;

        private GHCommitsStepExecution(GHCommitsStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<GHPullRequestCommitDetail> run() throws Exception {
            if (step.prId == null) {
                // generic commit not yet supported
                throw new Failure(Messages.Steps_requiredParameter("Pull request identifier"));
            }

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

            String endpoint = step.serverURL != null ? step.serverURL : StepConstant.GITHUB_API_ENDPOINT;
            GitHub gitHub = GitHubBuilder.fromEnvironment()
                    .withEndpoint(endpoint)
                    .withAppInstallationToken(token)
                    .build();

            GHRepository repository = gitHub.getRepository(step.owner + "/" + step.repository);
            GHPullRequest pr = repository.getPullRequest(step.prId);

            List<GHPullRequestCommitDetail> result = new ArrayList<>();
            for (GHPullRequestCommitDetail commit : pr.listCommits()) {
                result.add(commit);
            }
            return result;
        }
    }

    @Extension
    public static class DescriptorImpl extends AbstractGHStepDescriptor {
        @Override
        public String getFunctionName() {
            return "ghCommits";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Get commits details of repository in GitHub Enterprise";
        }

        public FormValidation doCheckOwner(@QueryParameter String owner) {
            return FormValidationBuilder.checkOwner(owner);
        }

        public FormValidation doCheckRepository(@QueryParameter String repository) {
            return FormValidationBuilder.checkRepository(repository);
        }

        public FormValidation doCheckPrId(@QueryParameter Integer prId) {
            return FormValidationBuilder.checkPrId(false, prId);
        }

    }
}