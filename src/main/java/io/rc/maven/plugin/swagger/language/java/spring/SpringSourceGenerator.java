package io.rc.maven.plugin.swagger.language.java.spring;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.rc.maven.plugin.swagger.mojo.CodegenInfo;
import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.ClientOpts;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.DefaultGenerator;
import io.swagger.models.Model;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * User:        raffaelecamanzo
 * Date:        14/11/2016
 *
 * Description:
 * Invokes the Swagger codegen and generates the output files
 *
 */
public class SpringSourceGenerator {

	private static Logger LOG = LoggerFactory.getLogger(SpringSourceGenerator.class);

	private Map<String, CodegenConfig> configMap = new HashMap<String, CodegenConfig>();
	private List<CodegenInfo> codegenInfos;
	private String language;
	private File outputDirectory;
	private boolean enableBuilderSupport;
	private boolean excludeSupportingFiles;
	private Map<String, Object> additionalProperties;

	public static SpringSourceGeneratorBuilder builder() {
		return new SpringSourceGeneratorBuilder();
	}

	public void generate() throws Exception {
		if(!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		prepare();

		boolean apiGenerated = false;
		for(CodegenInfo codegenInfo : codegenInfos) {
			LOG.info("Generating code for language: {}", language);

			final CodegenConfig codegenConfig = getCodegenConfig(language);
			codegenConfig.additionalProperties().putAll(additionalProperties);
			if(codegenConfig instanceof CustomSpringConfig) {

				if(!Strings.isNullOrEmpty(codegenInfo.getApiPackage())) {
					((CustomSpringConfig) codegenConfig).setApiPackage(codegenInfo.getApiPackage());
				}
				if(!Strings.isNullOrEmpty(codegenInfo.getModelPackage())) {
					((CustomSpringConfig) codegenConfig).setModelPackage(codegenInfo.getModelPackage());
				}

				if(enableBuilderSupport) {
					LOG.info("BuilderSupport enabled");
					if(((CustomSpringConfig) codegenConfig).isBuilderSupported()) {
						LOG.info("and supported by : " + language);
						((CustomSpringConfig) codegenConfig).enableBuilderSupport();
					} else {
						LOG.info("but not supported by : " + language);
					}
				}

				if(excludeSupportingFiles) {
					LOG.info("Excluding supporting files");
					((CustomSpringConfig) codegenConfig).excludeSupportingFiles();
				}
			}

			final ClientOptInput clientOptInput = new ClientOptInput();
			final ClientOpts clientOpts = new ClientOpts();

			clientOptInput.setConfig(codegenConfig);
			clientOptInput.getConfig().setOutputDir(outputDirectory.getAbsolutePath());

			Swagger swagger = new SwaggerParser().read(codegenInfo.getFileName());

			if(codegenInfo.isSkipApi() || Strings.isNullOrEmpty(codegenInfo.getApiPackage())) {
				LOG.info("API GEN DISABLED!");
				swagger.setPaths(new HashMap<String, Path>(0));
			} else if(apiGenerated) {
				throw new Exception("Multiple API definitions not allowed!!!");
			} else {
				apiGenerated = true;
			}

			if(codegenInfo.isSkipModel() || Strings.isNullOrEmpty(codegenInfo.getModelPackage())) {
				LOG.info("MODEL GEN DISABLED!");
				swagger.setDefinitions(new HashMap<String, Model>(0));
			}

			try {
				clientOptInput.opts(clientOpts).swagger(swagger);
				final DefaultGenerator generator = (DefaultGenerator) new DefaultGenerator().opts(clientOptInput);
				final List<File> generatedFiles = generator.generate();
				LOG.info("{} generated Files", generatedFiles.size());
			} catch (final Exception e) {
				throw new Exception(e.getMessage(), e);
			}
		}
	}

	public String getOutputDirectoryPath() {
		return outputDirectory.getAbsolutePath();
	}


	protected void prepare() {
		final List<CodegenConfig> codegenServices = getCodegenServices();

		for(final CodegenConfig config : codegenServices) {
			LOG.debug("Found codegen config {} with class {}", config.getName(), config.getClass().getName());
			configMap.put(config.getName(), config);
		}
	}

	private CodegenConfig getCodegenConfig(final String name) {
		if(configMap.containsKey(name))
			return configMap.get(name);

		throw new RuntimeException("No codegen available for the given name: "+name);
	}

	private List<CodegenConfig> getCodegenServices() {
		final ServiceLoader<CodegenConfig> loader = ServiceLoader.load(CodegenConfig.class);
		final List<CodegenConfig> output = new ArrayList<CodegenConfig>();
		final Iterator<CodegenConfig> iter = loader.iterator();
		while(iter.hasNext()) {
			output.add(iter.next());
		}

		return output;
	}

	public static class SpringSourceGeneratorBuilder {

		private List<CodegenInfo> codegenInfos;
		private String language;
		private File outputDirectory;
		private boolean enableBuilderSupport = false;
		private boolean excludeSupportingFiles = false;
		private Map<String, Object> additionalProperties =  ImmutableMap.of();

		public SpringSourceGeneratorBuilder withCodegenInfos(final List<CodegenInfo> codegenInfos) {
			this.codegenInfos = codegenInfos;
			return this;
		}

		public SpringSourceGeneratorBuilder forLanguage(final String languageDelimiter) {
			this.language = languageDelimiter;
			return this;
		}

		public SpringSourceGeneratorBuilder writeStubTo(final File outputDirectory) {
			this.outputDirectory = outputDirectory;
			return this;
		}

		public SpringSourceGeneratorBuilder enableBuilderSupport(final boolean enableBuilderSupport) {
			this.enableBuilderSupport = enableBuilderSupport;
			return this;
		}

		public SpringSourceGeneratorBuilder excludeSupportingFiles(final boolean excludeSupportingFiles) {
			this.excludeSupportingFiles = excludeSupportingFiles;
			return this;
		}

		public SpringSourceGeneratorBuilder additionalProperties(Map<String, Object> properties) {
			this.additionalProperties = properties;
			return this;
		}

		public SpringSourceGenerator build() {
			final SpringSourceGenerator generator = new SpringSourceGenerator();

			generator.codegenInfos = this.codegenInfos;
			generator.language = this.language;
			generator.outputDirectory = this.outputDirectory;
			generator.enableBuilderSupport = this.enableBuilderSupport;
			generator.additionalProperties = this.additionalProperties;
			generator.excludeSupportingFiles = this.excludeSupportingFiles;

			return generator;
		}
	}
}
