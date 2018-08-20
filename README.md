# carpenter-generator #

[![Build Status](https://travis-ci.org/tankist88/carpenter-generator.svg?branch=master)](https://travis-ci.org/tankist88/carpenter-generator)
[![Codecov](https://img.shields.io/codecov/c/github/tankist88/carpenter-generator.svg)](https://codecov.io/gh/tankist88/carpenter-generator)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/738e5781107b45d98ef078ae2f88c312)](https://www.codacy.com/project/tankist88/carpenter-generator/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=tankist88/carpenter-generator&amp;utm_campaign=Badge_Grade_Dashboard)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.tankist88/carpenter-collector.svg)](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.github.tankist88%22%20a%3A%22carpenter-generator%22)

Carpenter is a framework for unit test generation from runtime data. It useful for unit test coverage of legacy systems. Also it may be useful for coverage new features.

This module generate unit tests from data collected by carpenter-collector.

Other repositories:

* https://github.com/tankist88/carpenter-core
* https://github.com/tankist88/carpenter-collector

File list:
```text
carpenter.properties
carpenter-generator.jar
```

Example usage: 
```text
java -jar carpenter-generator.jar
```

Example configuration:
```text
# ************************
# Common parameters
# ************************
object.dump.dir=tmp
ut.gen.dir=tmp
data.providers.class.pattern=org.example.util.CommonDataProvider_
# ************************
# Tests will be generated only for classes in this packages
# ************************
test.generation.allowed.packages_1=org.example
# ************************
# Classes in this packages will be ignored by trace collector
# ************************
trace.collect.excluded.packages_1=org.example.webapp.simpleweb.servlet
trace.collect.excluded.packages_2=org.example.webapp.simpleweb.data
# ************************
# Classes in this packages will be ignored by source generator
# ************************
data.providers.excluded.packages_1=net
data.providers.excluded.packages_2=com
data.providers.excluded.packages_3=sun
data.providers.excluded.packages_4=java.lang.ref
data.providers.excluded.packages_5=java.lang.Class.AnnotationData
data.providers.excluded.packages_6=org.eclipse
```

Output unit tests:
```java
@Generated(value = "org.carpenter.generator.TestGenerator")
public class LibraryServiceGeneratedTest {

    @Spy
    @InjectMocks
    private LibraryService testInstance;

    @Mock
    private org.example.webapp.simpleweb.service.IsbnService isbnService;

    @Mock
    private org.example.webapp.simpleweb.service.SubscribeService subscribeService;

    @Generated(value = "org.carpenter.generator.TestGenerator")
    @Test
    public void testGetBooks_1() throws java.lang.Exception {
        doNothing().when(subscribeService).sendClientNotification(any(ExtPlan.class));
        doNothing().when(subscribeService).setPlan(any(ExtPlan.class));
        doReturn(CommonDataProvider_4.getClient__1305902843()).when(subscribeService).getClient();
        testInstance.getBooks();
        verify(subscribeService, atLeastOnce()).sendClientNotification(any(ExtPlan.class));
        verify(subscribeService, atLeastOnce()).setPlan(any(ExtPlan.class));
        verify(subscribeService, atLeastOnce()).getClient();
    }

    @Generated(value = "org.carpenter.generator.TestGenerator")
    @Test
    public void testGetSummaryStr_1() throws java.lang.Exception {
        doReturn(CommonDataProvider_3.getLibrarySummary_899664243()).when(testInstance).getSummary();
        java.lang.String result = testInstance.getSummaryStr();
        verify(testInstance, atLeastOnce()).getSummary();
        assertEquals(result, CommonDataProvider_4.getString_225853195());
    }

    @Generated(value = "org.carpenter.generator.TestGenerator")
    @Test
    public void testGetSummary_1() throws java.lang.Exception {
        doReturn(CommonDataProvider_2.getArrayList_2118523509()).when(testInstance).getBooks();
        doAnswer(new Answer() {
            private int count = 0;
            private String[] values = {
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941(),
                    CommonDataProvider_1.getString_1114439941()
            };
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String result = values[count];
                if(count + 1 < values.length) count++;
                return result;
            }
        }).when(isbnService).getBookISBN(any(Integer.class));
        testInstance.getSummary();
        verify(testInstance, atLeastOnce()).getBooks();
        verify(isbnService, atLeastOnce()).getBookISBN(any(Integer.class));
    }

    @BeforeMethod
    public void init() {
        initMocks(this);
        testInstance.isbnService = isbnService;
        testInstance.subscribeService = subscribeService;
    }
}
```

### Installation ###

```text
mvn clean install
```

### Contacts ###

* Repo owner - Alexey Ustinov (tankist88@gmail.com)
