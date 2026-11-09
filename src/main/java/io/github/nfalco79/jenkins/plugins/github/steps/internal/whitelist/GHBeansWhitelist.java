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
package io.github.nfalco79.jenkins.plugins.github.steps.internal.whitelist;

import java.io.IOException;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.Extension;

@Restricted(NoExternalUse.class)
@Extension
public final class GHBeansWhitelist extends ProxyWhitelist {

    public GHBeansWhitelist() throws IOException {
        super(StaticWhitelist.from(GHBeansWhitelist.class.getResource("github-beans-whitelist")));
    }
}
