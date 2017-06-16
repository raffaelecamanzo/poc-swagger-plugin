package io.rc.maven.plugin.swagger.language.java.spring;

/**
 * User:        raffaelecamanzo
 * Date:        15/11/2016
 *
 * Description:
 * Custom Spring codegen interface (in order to support custom operations)
 *
 */
public interface CustomSpringConfig {

	void setApiPackage(String apiPackage);
	void setModelPackage(String modelPackage);
	boolean isBuilderSupported();
	void enableBuilderSupport();
	void excludeSupportingFiles();
}
