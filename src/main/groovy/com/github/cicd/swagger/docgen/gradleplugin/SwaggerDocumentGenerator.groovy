package com.github.cicd.swagger.docgen.gradleplugin

import com.github.cicd.swagger.docgen.GenerateException

/**
 * SwaggerDocumentGenerator
 */
class SwaggerDocumentGenerator {

	ClassLoader classLoader

	public SwaggerDocumentGenerator(ClassLoader classLoader) {
		this.classLoader = classLoader
	}

	public void generateSwaggerDocuments(SwaggerPluginExtension swagger) throws GenerateException {
		GradleDocumentSource documentSource = new GradleDocumentSource(swagger, classLoader)

		documentSource.loadDocuments()
		//documentSource.toDocuments()
		documentSource.toSwaggerDocuments(swagger.getSwaggerUIDocBasePath())
	}
}
