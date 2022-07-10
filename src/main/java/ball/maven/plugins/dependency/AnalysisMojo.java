package ball.maven.plugins.dependency;
/*-
 * ##########################################################################
 * Dependency Maven Plugin
 * %%
 * Copyright (C) 2022 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

/**
 * {@link org.apache.maven.plugin.Mojo} to provide analysis of dependencies.
 *
 * {@injected.fields}
 *
 * {@maven.plugin.fields}
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@Mojo(name = "analysis", requiresDependencyResolution = TEST,
      defaultPhase = GENERATE_RESOURCES, requiresProject = true)
@NoArgsConstructor @ToString @Slf4j
public class AnalysisMojo extends AbstractDependencyMojo {
    private static final String COMPILE = "compile";
    private static final String PROVIDED = "provided";
    private static final String RUNTIME = "runtime";
    private static final String SYSTEM = "system";
    private static final String TEST = "test";
    private static final String INCLUDE_SCOPE = COMPILE + "," + RUNTIME;
    private static final String EXCLUDE_SCOPE = EMPTY;

    @Parameter(defaultValue = INCLUDE_SCOPE, property = "dependency.includeScope")
    private String includeScope = INCLUDE_SCOPE;

    @Parameter(defaultValue = EXCLUDE_SCOPE, property = "dependency.excludeScope")
    private String excludeScope = EXCLUDE_SCOPE;

    @Inject private MavenProject project = null;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
    }

    private Set<String> getScope() {
        Pattern pattern = Pattern.compile("[\\p{Punct}\\p{Space}]+");
        TreeSet<String> scope =
            pattern.splitAsStream(includeScope)
            .filter(StringUtils::isNotBlank)
            .collect(toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

        pattern.splitAsStream(excludeScope)
            .filter(StringUtils::isNotBlank)
            .forEach(t -> scope.remove(t));

        if (scope.isEmpty()) {
            log.warn("Specified scope is empty");
            log.debug("    includeScope = {}", includeScope);
            log.debug("    excludeScope = {}", excludeScope);
        }

        return scope;
    }
}
