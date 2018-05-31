# carpenter-generator #

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
public class LibraryServiceGeneratedTest {

    @Spy
    @InjectMocks
    private LibraryService testInstance;

    @Mock
    private org.example.webapp.simpleweb.service.IsbnService isbnService;

    @Mock
    private org.example.webapp.simpleweb.service.SubscribeService subscribeService;

    @Test
    public void testGetBooks_1() throws java.lang.Exception {
        doNothing().when(subscribeService).setPlan(any(ExtPlan.class));
        doReturn(CommonDataProvider_24.getClient__1305902843()).when(subscribeService).getClient();
        doNothing().when(subscribeService).sendClientNotification(any(ExtPlan.class));
        testInstance.getBooks();
    }

    @Test
    public void testGetSummaryStr_1() throws java.lang.Exception {
        doReturn(CommonDataProvider_27.getLibrarySummary_899664243()).when(testInstance).getSummary();
        testInstance.getSummaryStr();
    }

    @Test
    public void testGetSummary_1() throws java.lang.Exception {
        doReturn(CommonDataProvider_25.getArrayList_2118523509()).when(testInstance).getBooks();

        doAnswer(new Answer() {
            private int count = 0;
            private String[] values = {
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941(),
                    CommonDataProvider_26.getString_1114439941()
            };
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String result = values[count];
                if(count + 1 < values.length) count++;
                return result;
            }
        }).when(isbnService).getBookISBN(any(Integer.class));

        testInstance.getSummary();
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
