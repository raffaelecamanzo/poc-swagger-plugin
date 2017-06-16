package io.rc.maven.plugin.swagger.language.java.spring;

import io.rc.maven.plugin.swagger.mojo.CodegenInfo;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
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
import java.net.MalformedURLException;
import java.net.URL;
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
	private List<String> excludedModels;


	public static SpringSourceGeneratorBuilder builder() {
		return new SpringSourceGeneratorBuilder();
	}

	public void generate() throws Exception {
		if(!getOutputDirectory().exists()) {
			getOutputDirectory().mkdirs();
		}

		prepare();

		boolean apiGenerated = false;
		for(CodegenInfo codegenInfo : codegenInfos) {
			LOG.info("Generating code for language: {}", language);

			checkModelPackage(codegenInfo);
			checkFileExists(codegenInfo);

			final CodegenConfig codegenConfig = getCodegenConfig(language);
			if(codegenConfig == null) {
				throw new Exception("No CodegenConfig-Implementation found for " + language);
			}
			codegenConfig.additionalProperties().putAll(additionalProperties);
			if(codegenConfig instanceof CustomSpringConfig) {

				// config
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
			} else if(!excludedModels.isEmpty()) {
				final Iterator<Map.Entry<String, Model>> it = swagger.getDefinitions().entrySet().iterator();
				while(it.hasNext()) {
					final Map.Entry<String, Model> entry = it.next();
					if(excludedModels.contains(entry.getKey())) {
						LOG.info("REMOVED {} from MODEL generation", entry.getKey());
						it.remove();
					}
				}
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
		return getOutputDirectory().getAbsolutePath();
	}

	protected void checkModelPackage(CodegenInfo codegenInfo) {
		if(codegenInfo.getModelPackage() == null || codegenInfo.getModelPackage().trim().isEmpty()) {
			LOG.info("No 'modelPackage' was specified, use configured 'apiPackage' ({}) for YAML file {} ", codegenInfo.getApiPackage(), codegenInfo.getFileName());
			codegenInfo.setModelPackage(codegenInfo.getApiPackage());
		}
	}

	protected void checkFileExists(CodegenInfo codegenInfo) throws Exception {
		try {
			URL url = new URL(codegenInfo.getFileName());
			String prot = url.getProtocol();
			if ((!"https".equals(prot)) || (!"http".equals(prot))) {
				LOG.info("'fileName' should use 'http' or 'https'");
			}
			//return;
		} catch (MalformedURLException e) {
			LOG.info("'fileName' seems not be an valid URL, check file exist");
			final File file = new File(codegenInfo.getFileName());
			if(!file.exists()) {
				LOG.info("The 'fileName' does not exists at : " + codegenInfo.getFileName());
				throw new Exception("The 'fileName' does not exists at : " + codegenInfo.getFileName());
			}
		}
	}

	public File getOutputDirectory() {
		return this.outputDirectory;
	}

	protected void prepare() {
		final List<CodegenConfig> codegenServices = getCodegenServices();

		for(final CodegenConfig config : codegenServices) {
			LOG.debug("Found codegen config {} with class {}", config.getName(), config.getClass().getName());
			configMap.put(config.getName(), config);
		}
	}

	private CodegenConfig getCodegenConfig(final String name) {
		if(configMap.containsKey(name)) {
			return configMap.get(name);
		} else {
			try {
				LOG.info("Loading custom class: {}", name);

				final Class<?> customClass = Class.forName(name);
				LOG.info("Custom class {} loaded", name);
				return (CodegenConfig) customClass.newInstance();
			} catch (final Exception e) {
				throw new RuntimeException("can't load config-class for '" + name + "'", e);
			}
		}
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
		private List<String> excludedModels = new ArrayList<String>(0);
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

		public SpringSourceGeneratorBuilder withModelsExcluded(final List<String> excludedResources) {
			this.excludedModels = excludedResources;
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
			generator.excludedModels = this.excludedModels;
			generator.enableBuilderSupport = this.enableBuilderSupport;
			generator.additionalProperties = this.additionalProperties;
			generator.excludeSupportingFiles = this.excludeSupportingFiles;

			return generator;
		}
	}
}
