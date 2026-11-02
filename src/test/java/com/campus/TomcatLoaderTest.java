package com.campus;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TomcatLoaderTest {

    @Test
    public void testTomcatContext() throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(0);
        tomcat.setBaseDir("temp-tomcat-test");
        tomcat.getConnector();

        Context ctx = tomcat.addWebapp("", new File("src/main/webapp").getAbsolutePath());
        if (ctx instanceof StandardContext) {
            ((StandardContext) ctx).setDelegate(true);
        }
        ctx.setParentClassLoader(Main.class.getClassLoader());

        tomcat.start();
        assertNotNull(ctx.getLoader());
        System.out.println("Delegate value: " + ((StandardContext) ctx).getDelegate());
        System.out.println("Loader class: " + ctx.getLoader().getClass().getName());
        tomcat.stop();
        tomcat.destroy();
    }
}
