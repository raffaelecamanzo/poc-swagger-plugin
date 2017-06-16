package io.rc.maven.plugin.swagger.mojo;

import io.rc.maven.plugin.swagger.language.java.spring.SpringSourceGenerator;
import com.google.common.collect.ImmutableMap;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User:        raffaelecamanzo
 * Date:        23/11/2016
 *
 * Description:
 * Maven plugin Mojo
 *
 */
@Mojo(
		name = "poc-swagger", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true, threadSafe = false,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class PocSwaggerCodegenMojo extends AbstractMojo {

	private static final Logger LOG = LoggerFactory.getLogger(PocSwaggerCodegenMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/custom-spring-codegen")
	private File outputDirectory;

	@Parameter(required = true)
	private List<CodegenInfo> codegenInfos;

	@Parameter(required = true, defaultValue = "custom-spring")
	private String language;

	@Parameter(defaultValue = "false")
	private boolean enableBuilderSupport = false;

	@Parameter(defaultValue = "false")
	private boolean excludeSupportingFiles = false;

	@Parameter
	private Map<String,Object> additionalProperties = ImmutableMap.of();

	@Parameter
	private ArrayList<String> excludedModels = new ArrayList<String>();

	public void execute() throws MojoExecutionException, MojoFailureException {

		final SpringSourceGenerator generator = SpringSourceGenerator.builder().withCodegenInfos(codegenInfos)
				.forLanguage(language)
				.writeStubTo(outputDirectory)
				.withModelsExcluded(excludedModels)
				.additionalProperties(additionalProperties)
				.enableBuilderSupport(enableBuilderSupport)
				.excludeSupportingFiles(excludeSupportingFiles)
				.build();

		try {
			generator.generate();

			project.addCompileSourceRoot(generator.getOutputDirectoryPath());
		} catch (final Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

	}
}
