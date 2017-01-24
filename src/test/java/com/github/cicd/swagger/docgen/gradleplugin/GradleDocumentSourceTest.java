package com.github.cicd.swagger.docgen.gradleplugin;

import com.github.cicd.swagger.docgen.AbstractDocumentSource;
import com.github.cicd.swagger.docgen.TypeUtils;
import com.github.cicd.swagger.docgen.gradleplugin.GradleDocumentSource;
import com.github.cicd.swagger.docgen.gradleplugin.SwaggerPluginExtension;
import com.github.cicd.swagger.docgen.mustache.*;
import com.google.common.base.Predicate;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiModelProperty;

import org.reflections.Reflections;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import sample.api.car.CarResourceV1;
import sample.api.car.CarResourceV2;
import sample.model.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static junit.framework.Assert.*;

/**
 * GradleDocumentSourceTest
 */
public class GradleDocumentSourceTest {
    public static final String CAR_PACKAGE = "sample.api.car";
    public static final String GARAGE_PACKAGE = "sample.api.garage";

    SwaggerPluginExtension swagger;

    @BeforeClass
    private void prepare() {
        swagger = new SwaggerPluginExtension();

        swagger.setApiVersion("1.0");
        swagger.setBasePath("http://example.com");
        swagger.setEndPoints(new String[]{CAR_PACKAGE, GARAGE_PACKAGE});
        swagger.setOutputPath("sample.html");
        swagger.setOutputTemplate("https://github.com/kongchen/api-doc-template/blob/master/v1.1/html.mustache");
        swagger.setSwaggerDirectory(null);
    }

    @Test
    public void testIssue17() throws Exception {
        String[] locations = swagger.getEndPoints();

        try {
            swagger.setEndPoints(new String[]{"issue17"});
            AbstractDocumentSource documentSource = new GradleDocumentSource(swagger, this.getClass().getClassLoader());
            documentSource.loadDocuments();
            OutputTemplate outputTemplate = new OutputTemplate(documentSource);
            Assert.assertEquals(outputTemplate.getDataTypes().size(), 1);
            Assert.assertEquals(outputTemplate.getDataTypes().iterator().next().getName(), "Child");
        } finally {
            // set back
            swagger.setEndPoints(locations);
        }
    }

    @Test
    public void testReflectionsUsage() throws MalformedURLException {

        URL[] urls = {
            new File("target/classes/test").toURI().toURL()
        };
        URLClassLoader classLoader = new URLClassLoader(urls, null);
        Reflections reflections = new Reflections(classLoader, CAR_PACKAGE);
        Set<Class<?>> c = reflections.getTypesAnnotatedWith(Api.class);

        assertNotNull(c);
        assertTrue(c.size() > 0);

        assertFalse(filter(c, new Predicate<Class<?>>() {
            public boolean apply(Class<?> cls) {
                return cls.getSimpleName().equals(CarResourceV1.class.getSimpleName());
            }
        }).isEmpty());
        assertFalse(filter(c, new Predicate<Class<?>>() {
            public boolean apply(Class<?> cls) {
                return cls.getSimpleName().equals(CarResourceV2.class.getSimpleName());
            }
        }).isEmpty());
    }

    @Test
    public void test() throws Exception {
        AbstractDocumentSource documentSource = new GradleDocumentSource(swagger, this.getClass().getClassLoader());
        documentSource.loadDocuments();
        OutputTemplate outputTemplate = new OutputTemplate(documentSource);
        assertEquals(swagger.getApiVersion(), outputTemplate.getApiVersion());
        assertEquals(3, outputTemplate.getApiDocuments().size());
        for (MustacheDocument doc : outputTemplate.getApiDocuments()) {
            if (doc.getIndex() == 1) {
                Assert.assertEquals(doc.getResourcePath(), "/car");
                for (MustacheApi api : doc.getApis()) {
                    assertTrue(api.getUrl().startsWith(swagger.getBasePath()));
                    assertFalse(api.getPath().contains("{format}"));
                    for (MustacheOperation op : api.getOperations()) {
                        if (op.getOpIndex() == 2) {

                            Assert.assertEquals(op.getParameters().size(), 4);

                            Assert.assertEquals("ETag", op.getResponseHeader().getParas().get(0).getName());

                            Assert.assertEquals("carId",
                                    op.getRequestPath().getParas().get(0).getName());
                            Assert.assertEquals("1.0 to 10.0",
                                    op.getRequestPath().getParas().get(0).getAllowableValue());

                            Assert.assertEquals("e",
                                    op.getRequestQuery().getParas().get(0).getName());

                            Assert.assertEquals("Accept",
                                    op.getRequestHeader().getParas().get(0).getName());
                            Assert.assertEquals("MediaType",
                                    op.getRequestHeader().getParas().get(0).getType());
                            Assert.assertEquals("application/json, application/*",
                                    op.getRequestHeader().getParas().get(0).getAllowableValue());
                            Assert.assertEquals(op.getErrorResponses().size(), 2);
                            Assert.assertEquals(op.getErrorResponses().get(0).message(), "Invalid ID supplied");
                            Assert.assertEquals(op.getErrorResponses().get(0).code(), 400);
                            Assert.assertEquals(op.getErrorResponses().get(1).code(), 404);
                            Assert.assertEquals(op.getAuthorizations().get(0).getType(), "oauth2");
                            Assert.assertEquals(op.getAuthorizations().get(0).getAuthorizationScopes().get(0).description(), "car1 des get");
                        }
                        if (op.getOpIndex() == 1) {
                            Assert.assertEquals(op.getSummary(), "search cars");
                        }
                    }
                }
            }
            if (doc.getIndex() == 2) {
                Assert.assertEquals(doc.getResourcePath(), "/v2/car");
            }
            if (doc.getIndex() == 3) {
                Assert.assertEquals(doc.getResourcePath(), "/garage");
            }

        }


        assertEquals(8, outputTemplate.getDataTypes().size());
        List<MustacheDataType> typeList = new LinkedList<>();
        for (MustacheDataType type : outputTemplate.getDataTypes()) {
            typeList.add(type);
        }
        Collections.sort(typeList, new Comparator<MustacheDataType>() {

            @Override
            public int compare(MustacheDataType o1, MustacheDataType o2) {

                return o1.getName().compareTo(o2.getName());
            }
        });
        assertDataTypeInList(typeList, 0, Address.class);
        assertDataTypeInList(typeList, 1, sample.model.Car.class);
        assertDataTypeInList(typeList, 2, Customer.class);
        assertDataTypeInList(typeList, 3, Email.class);
        assertDataTypeInList(typeList, 4, ForGeneric.class);
        assertDataTypeInList(typeList, 5, G1.class);
        assertDataTypeInList(typeList, 6, G2.class);
        assertDataTypeInList(typeList, 7, sample.model.v2.Car.class);
    }

    private void assertDataTypeInList(List<MustacheDataType> typeList, int indexInList,
                                      Class<?> aClass) throws NoSuchMethodException, NoSuchFieldException {
        MustacheDataType dataType = typeList.get(indexInList);
        XmlRootElement root = aClass.getAnnotation(XmlRootElement.class);
        if (root == null) {
            assertEquals(dataType.getName(), aClass.getSimpleName());
        } else {
            assertEquals(dataType.getName(), root.name());
        }

        for (MustacheItem item : dataType.getItems()) {

            String name = item.getName();
            ApiModelProperty a;

            Field f;
            try {
                f = aClass.getDeclaredField(name);
                a = f.getAnnotation(ApiModelProperty.class);
                if (a == null) {
                    a = getApiProperty(aClass, name);
                }
            } catch (NoSuchFieldException e) {
                a = getApiProperty(aClass, name);
            }

            if (a == null) {
                return;
            }
            String type = a.dataType();
            if (type.equals("")) {
                // need to get true data type
                type = getActualDataType(aClass, name);
            }

            assertEquals(aClass.toString() + " type", type, item.getType());
            assertEquals(aClass.toString() + " required", a.required(), item.isRequired());
            assertEquals(aClass.toString() + " value", a.value(), nullToEmpty(item.getDescription()));
            assertEquals(aClass.toString() + " allowableValues", stringToList(a.allowableValues(), ","), stringToList(item.getAllowableValue(), ","));
        }
    }

    private String getActualDataType(Class<?> aClass, String name) throws NoSuchFieldException {
        String t = null;
        Class<?> type = null;
        Field f;
        boolean isArray = false;
        ParameterizedType parameterizedType = null;
        for (Method _m : aClass.getMethods()) {
            XmlElement ele = _m.getAnnotation(XmlElement.class);
            if (ele == null) {
                continue;
            }
            if (ele.name().equals(name)) {
                t = ele.type().getSimpleName();
                if (!t.equals("DEFAULT")) {
                    break;
                }
                type = _m.getReturnType();
                Type gType = _m.getGenericReturnType();
                if (gType instanceof ParameterizedType) {
                    parameterizedType = (ParameterizedType) gType;
                }
                break;
            }
        }
        if (type == null && t == null) {
            for (Field _f : aClass.getDeclaredFields()) {
                XmlElement ele = _f.getAnnotation(XmlElement.class);
                if (ele == null) {
                    continue;
                }
                if (ele.name().equals(name)) {
                    type = _f.getType();
                    break;
                }
            }
        }
        if (type == null) {
            f = aClass.getDeclaredField(name);
            type = f.getType();
            if (Collection.class.isAssignableFrom(type)) {
                parameterizedType = (ParameterizedType) f.getGenericType();
            }
        }
        if (parameterizedType != null) {
            Class<?> genericType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            t = genericType.getSimpleName();
            isArray = true;
        } else {
            t = type.getSimpleName();
        }

        t = toPrimitive(t);
        return isArray ? TypeUtils.AsArrayType(t) : t;
    }

    private String toPrimitive(String type) {
        if (type.equals("Byte")) {
            return "byte";
        }
        if (type.equals("Short")) {
            return "short";
        }
        if (type.equals("Integer")) {
            return "int";
        }
        if (type.equals("Long")) {
            return "long";
        }
        if (type.equals("Float")) {
            return "float";
        }
        if (type.equals("Double")) {
            return "double";
        }
        if (type.equals("Boolean")) {
            return "boolean";
        }
        if (type.equals("Character")) {
            return "char";
        }
        if (type.equals("String")) {
            return "string";
        }
        return type;
    }

    private ApiModelProperty getApiProperty(Class<?> aClass, String name) {
        ApiModelProperty a = null;
        for (Field _f : aClass.getDeclaredFields()) {
            XmlElement ele = _f.getAnnotation(XmlElement.class);
            if (ele == null) {
                continue;
            }
            if (ele.name().equals(name)) {
                a = _f.getAnnotation(ApiModelProperty.class);
                break;
            }
        }
        for (Method _m : aClass.getMethods()) {
            XmlElement ele = _m.getAnnotation(XmlElement.class);
            if (ele == null) {
                continue;
            }
            if (ele.name().equals(name)) {
                a = _m.getAnnotation(ApiModelProperty.class);
                break;
            }
        }
        return a;
    }

    private String nullToEmpty(String item) {
        return item == null ? "" : item;
    }

    // helper function so that we ignore any spaces we trim off or add when we build a string
    private List<String> stringToList(String srcStr, String token) {
        if (srcStr == null) {
            return null;
        }

        List<String> lst = new ArrayList<>();
        String[] array = srcStr.split(token);

        for (String str : array) {
            lst.add(str.trim());
        }
        return lst;

    }
}
