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
import ball.annotation.CompileTimeCheck;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import javax.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
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
    private static Comparator<? super Artifact> COMPARATOR =
        Comparator.comparing(ArtifactUtils::key, String.CASE_INSENSITIVE_ORDER);

    @CompileTimeCheck
    private static final Pattern PATTERN =
        Pattern.compile("(?i)^(META-INF/versions/[\\p{Digit}]+/)?(?<path>.*)[.]class$");

    @Inject private MavenProject project = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<String> scope = getScope();
        Map<Artifact,Set<String>> map =
            project.getArtifacts().stream()
            .filter(t -> scope.contains(t.getScope()))
            .collect(toMap(k -> k, v -> classesIn(v), (t, u) -> u, () -> new TreeMap<>(COMPARATOR)));
        List<Artifact> list = new LinkedList<>(map.keySet());

        while (! list.isEmpty()) {
            Artifact artifact0 = list.remove(0);
            Set<String> set0 = map.get(artifact0);

            for (Artifact artifactN : list) {
                Set<String> setN = map.get(artifactN);

                if (! Collections.disjoint(set0, setN)) {
                    log.info("");

                    if (set0.equals(setN)) {
                        log.info("{} and {} contain the same class entries", artifact0, artifactN);
                    } else if (set0.contains(setN)) {
                        log.info("{} fully contains {} class entries", artifact0, artifactN);
                    } else if (setN.contains(set0)) {
                        log.info("{} fully contains {} class entries", artifactN, artifact0);
                    } else {
                        Set<String> common = new TreeSet<>();

                        common.addAll(set0);
                        common.retainAll(setN);

                        log.info("{} and {} contain {} common class entries", artifact0, artifactN, common.size());
                    }
                } else {
                    log.debug("");
                    log.debug("{} and {} contain disjoint class entries", artifact0, artifactN);
                }
            }
        }
    }

    private Set<String> classesIn(Artifact artifact) {
        URL url = toURL(artifact);
        Set<String> classes = Collections.emptySet();

        try {
            JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();

            classes =
                jar.stream()
                .map(JarEntry::getName)
                .map(PATTERN::matcher)
                .filter(t -> t.matches())
                .map(t -> t.group("path"))
                .map(t -> t.replaceAll("/", "."))
                .collect(toSet());
        } catch (ZipException exception) {
        } catch (IOException exception) {
            log.debug("{}: {}", artifact, exception.getMessage(), exception);
        }

        return classes;
    }

    private URL toURL(Artifact artifact) {
        URL url = null;

        try {
            url = new URI("jar", artifact.getFile().toURI().toASCIIString() + "!/", null).toURL();
        } catch(URISyntaxException | MalformedURLException exception) {
            log.debug("{}: {}", artifact, exception.getMessage(), exception);
            throw new IllegalStateException(exception);
        }

        return url;
    }
}
