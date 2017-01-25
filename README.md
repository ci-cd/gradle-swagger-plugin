gradle-swagger-plugin
=====================

This project is forked from dave-ellis/gradle-swagger-plugin. (See https://github.com/dave-ellis/gradle-swagger-plugin)

# Usage

## Gradle
```
    buildscript {
        repositories {
            mavenLocal()
            maven { url "http://repo.maven.apache.org/maven2" }
        }
        dependencies {
            classpath group: 'com.github.ci-cd', name: 'gradle-swagger-plugin', version: '1.0'
        }
    }

    apply plugin: 'maven'
    apply plugin: 'swagger'
    apply plugin: 'java'
    
    dependencies {
    	compile group: 'com.wordnik', name: 'swagger-annotations', version: '1.3.10'
    	...
    }
    
    swagger {
        endPoints = [
                'com.foo.bar.apis',
                'com.foo.bar.apis.internal.Resource'
        ]
        apiVersion = version
        basePath = 'http://www.example.com/rest'
        swaggerUIDocBasePath = '/swagger-ui/rest-api-docs'
        swaggerDirectory = "${buildDir}/site/api-docs"
    }
```


* The swagger block currently only allows the definition of one `apiSource`.
* Java classes containing Swagger's annotation `@Api`, or Java packages containing those classes can be configured in `endPoints` list.
* *Note:* Works only with `swagger-annotations` artifact from `com.wordnik` group ID.
* `outputTemplate` is the path of the mustache template file.
* `outputPath` is the path of your output file, not existed parent directory of the file will be created.
* If `swaggerDirectory` is configured, the plugin will also generate a Swagger resource listing suitable for feeding to swagger-ui.
    * `useOutputFlatStructure` indicates whether swagger output will be created in sub-directories by path defined in
      `@com.wordnik.swagger.annotations.Api#value` (false), or the filename will be the path with replaced slashes to
      underscores (true). Default: true

