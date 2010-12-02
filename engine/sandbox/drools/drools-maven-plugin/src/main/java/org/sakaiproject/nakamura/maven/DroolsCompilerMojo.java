/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.nakamura.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.drools.KnowledgeBase;
import org.drools.RuleBase;
import org.drools.RuleBaseConfiguration;
import org.drools.RuleBaseFactory;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.compiler.PackageBuilder;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.compiler.PackageBuilderErrors;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.rule.Package;
import org.drools.runtime.StatelessKnowledgeSession;
import org.drools.util.DroolsStreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @goal compile-rules
 */
public class DroolsCompilerMojo extends AbstractMojo {

  /**
   * @parameter default-value={"**\/*"}
   */
  protected String[] includes;
  /**
   * @parameter default-value={}
   */
  protected String[] excludes;
  /**
   * @parameter default-value="${basedir}/src/main/rules"
   */
  protected File rulesdir;

  /**
   * The output directory for bundles.
   * 
   * @parameter default-value="${project.build.directory}/classes"
   */
  protected File outputDirectory;
  /**
   * The output directory for bundles.
   * 
   * @parameter default-value=
   *            "SLING-INF/content/var/rules/${project.groupId}/${project.artifactId}/${project.version}/${project.artifactId}-${project.version}.pkg"
   */
  protected String packageOutputName;

  /**
   * The Maven project.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  protected MavenProject project;

  /**
   * Used to look up Artifacts in the remote repository.
   * 
   * @component
   */
  protected ArtifactFactory factory;

  /**
   * Used to look up Artifacts in the remote repository.
   * 
   * @component
   */
  protected ArtifactResolver resolver;

  /**
   * Location of the local repository.
   * 
   * @parameter expression="${localRepository}"
   * @readonly
   * @required
   */
  protected ArtifactRepository local;

  /**
   * List of Remote Repositories used by the resolver.
   * 
   * @parameter expression="${project.remoteArtifactRepositories}"
   * @readonly
   * @required
   */
  protected List<?> remoteRepos;

  public void execute() throws MojoExecutionException {
    // find all the rules items and load them into a package
    try {

      // Need to load the build classpath
      @SuppressWarnings("unchecked")
      List<Dependency> dependencies = project.getDependencies();
      List<URL> url = new ArrayList<URL>();
      url.add(outputDirectory.toURI().toURL());
      for (Dependency d : dependencies) {
        String scope = d.getScope();
        if (!Artifact.SCOPE_TEST.equals(scope)) {
          Artifact artifact = getArtifact(d.getGroupId(), d.getArtifactId(),
              d.getVersion(), d.getType(), d.getClassifier());
          url.add(artifact.getFile().toURI().toURL());
        }
      }

      URL[] classpath = url.toArray(new URL[url.size()]);

      URLClassLoader uc = new URLClassLoader(classpath, this.getClass().getClassLoader()) {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
          getLog().debug("Loading Class for compile [" + name + "]");
          Class<?> c = super.loadClass(name);
          getLog().debug("Loading Class for compile [" + name + "] found [" + c + "]");
          return c;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
          getLog().debug("Finding Class for compile [" + name + "]");
          Class<?> c = super.findClass(name);
          getLog().debug("Finding Class for compile [" + name + "] found [" + c + "]");
          return c;
        }
      };
      URLClassLoader uc2 = new URLClassLoader(classpath, this.getClass().getClassLoader()) {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
          getLog().debug("Loading Class for runtime [" + name + "]");
          Class<?> c = super.loadClass(name);
          getLog().debug("Loading Class for runtime [" + name + "] found [" + c + "]");
          return c;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
          getLog().debug("Finding Class for runtime [" + name + "]");
          Class<?> c = super.findClass(name);
          getLog().debug("Finding Class for runtime [" + name + "] found [" + c + "]");
          return c;
        }
      };
      getLog().info(
          "Package Class loader is using classpath " + Arrays.toString(uc.getURLs()));

      listClassloader("  ", uc);

      PackageBuilderConfiguration packageBuilderConfiguration = new PackageBuilderConfiguration(
          uc);
      PackageBuilder pb = new PackageBuilder(packageBuilderConfiguration);


      DirectoryScanner ds = new DirectoryScanner();
      ds.setIncludes(includes);
      ds.setExcludes(excludes);
      ds.setBasedir(rulesdir);
      ds.setCaseSensitive(true);
      ds.scan();

      String[] files = ds.getIncludedFiles();
      for (String file : files) {
        File f = new File(rulesdir, file);
        Reader reader = new FileReader(f);
        try {
          if (file.endsWith(".drl")) {
            getLog().info("Adding Rules " + f);
            pb.addPackageFromDrl(reader);
          } else if (file.endsWith(".xml")) {
            getLog().info("Adding Package definition " + f);
            pb.addPackageFromXml(reader);
          } else if (file.endsWith(".rf")) {
            getLog().info("Adding Rule Flow " + f);
            pb.addRuleFlow(reader);
          } else {
            getLog().info("Ignored Resource " + f);
          }

        } finally {
          reader.close();
        }
      }

      pb.compileAll();
      PackageBuilderErrors errors = pb.getErrors();
      if (errors.size() > 0) {
        for (KnowledgeBuilderError kberr : errors) {
          getLog().error(kberr.toString());
        }
        throw new MojoExecutionException("Package is not valid");

      }
      org.drools.rule.Package p = pb.getPackage();
      if (!p.isValid()) {
        getLog().error("Package is not valid ");
        throw new MojoExecutionException("Package is not valid");
      }

      File outputFile = getOutputFile();
      getLog().info("Saving compiled package to  " + outputFile.getPath());
      outputFile.getParentFile().mkdirs();
      FileOutputStream fout = new FileOutputStream(outputFile);
      DroolsStreamUtils.streamOut(fout, p);
      fout.close();

      getLog().info("Testing Compiled package " + outputFile.getPath());

      File inputFile = getOutputFile();
      FileInputStream fin = new FileInputStream(inputFile);

      RuleBaseConfiguration config = new RuleBaseConfiguration(uc2);
      RuleBase ruleBase = RuleBaseFactory.newRuleBase(config);
      Object o = DroolsStreamUtils.streamIn(fin, uc);

      ruleBase.addPackage((Package) o);
      KnowledgeBase kb = new KnowledgeBaseImpl(ruleBase);

      @SuppressWarnings("unused")
      StatelessKnowledgeSession session = kb.newStatelessKnowledgeSession();

      getLog().info("Testing passed ");

    } catch (Exception e) {
      getLog().error(e);
      throw new MojoExecutionException(e.getMessage());
    }

  }

  /**
   * Get a resolved Artifact from the coordinates provided
   * 
   * @return the artifact, which has been resolved.
   * @throws MojoExecutionException
   */
  protected Artifact getArtifact(String groupId, String artifactId, String version,
      String type, String classifier) throws MojoExecutionException {
    Artifact artifact;
    VersionRange vr;

    try {
      vr = VersionRange.createFromVersionSpec(version);
    } catch (InvalidVersionSpecificationException e) {
      vr = VersionRange.createFromVersion(version);
    }

    if (StringUtils.isEmpty(classifier)) {
      artifact = factory.createDependencyArtifact(groupId, artifactId, vr, type, null,
          Artifact.SCOPE_COMPILE);
    } else {
      artifact = factory.createDependencyArtifact(groupId, artifactId, vr, type,
          classifier, Artifact.SCOPE_COMPILE);
    }
    try {
      resolver.resolve(artifact, remoteRepos, local);
    } catch (ArtifactResolutionException e) {
      throw new MojoExecutionException("Unable to resolve artifact.", e);
    } catch (ArtifactNotFoundException e) {
      throw new MojoExecutionException("Unable to find artifact.", e);
    }
    return artifact;
  }

  private void listClassloader(String indent, ClassLoader uc) {
    if (uc != null) {
      if (uc instanceof URLClassLoader) {
        URLClassLoader urlC = (URLClassLoader) uc;
        getLog().debug(
            indent + "Classloader : " + uc + " " + Arrays.toString(urlC.getURLs()));
      } else {
        getLog().debug(indent + "Classloader : " + uc);
      }
      listClassloader(indent + "  ", uc.getParent());
    }

  }

  private File getOutputFile() {
    return new File(outputDirectory, packageOutputName);
  }
}
