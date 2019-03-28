package io.spring.nohttp.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.resources.TextResource;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * @author Rob Winch
 */
public class NoHttpPlugin implements Plugin<Project> {
	private Project project;

	@Override
	public void apply(Project project) {
		this.project = project;
		project.getPluginManager().apply("checkstyle");
		Configuration checkstyleConfiguration = project.getConfigurations().getByName("checkstyle");

		Configuration noHttpConfiguration = project.getConfigurations().create("nohttp");
		checkstyleConfiguration.extendsFrom(noHttpConfiguration);

		configureDefaultDependenciesForProject(noHttpConfiguration);
		createCheckstyleTaskForProject(checkstyleConfiguration);
	}

	private void createCheckstyleTaskForProject(Configuration configuration) {
		Project project = this.project;
		Checkstyle checkstyleTask = project
				.getTasks().create("nohttpCheckstyle", Checkstyle.class);

		checkstyleTask.setSource(project.fileTree(project.getProjectDir(), new Action<ConfigurableFileTree>() {
			@Override
			public void execute(ConfigurableFileTree files) {
				files.exclude("**/build/**");
				files.exclude(".git/**");
				files.exclude(".gradle/**");
				files.exclude(".idea/**");
				files.exclude("**/*.class");
			}
		}));
		checkstyleTask.setClasspath(project.files());
		checkstyleTask.setClasspath(configuration);
		ConventionMapping taskMapping = checkstyleTask.getConventionMapping();
		taskMapping.map("config", new Callable<TextResource>() {
			@Override
			public TextResource call() throws Exception {
				Logger logger = project.getLogger();
				File defaultCheckstyleFile = project.file("config/checkstyle/nohttp/nohttp-checkstyle.xml");
				if (defaultCheckstyleFile.exists()) {
					logger.debug("Found default checkstyle configuration, so configuring checkstyleTask to use it");
					return project.getResources().getText().fromFile(defaultCheckstyleFile);
				}
				logger.debug("No checkstyle configuration provided, so using the default.");
				URL resource = getClass().getResource(
						"/io/spring/nohttp/checkstyle/default-nohttp-checkstyle.xml");
				return project.getResources().getText().fromUri(resource);
			}
		});
	}

	private void configureDefaultDependenciesForProject(Configuration configuration) {
		configuration.defaultDependencies(new Action<DependencySet>() {
			@Override
			public void execute(DependencySet dependencies) {
				dependencies.add(NoHttpPlugin.this.project.getDependencies().create("io.spring.nohttp:nohttp-checkstyle:0.0.1.BUILD-SNAPSHOT"));
			}
		});
	}
}
