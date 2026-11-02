package com.campus;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Embedded Tomcat server launcher.
 * Starts the Campus Club Membership Management System on port 8080.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================================");
        System.out.println("  CAMPUS CLUB MEMBERSHIP MANAGEMENT SYSTEM");
        System.out.println("  Hibernate ORM (Many-to-Many) | Servlets | Filters | Thymeleaf | MySQL");
        System.out.println("======================================================================");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.setBaseDir("temp-tomcat");

        // Force connector creation
        tomcat.getConnector();

        String webappDirLocation = new File("src/main/webapp").getAbsolutePath();
        Context ctx = tomcat.addWebapp("", webappDirLocation);
        ctx.setParentClassLoader(Main.class.getClassLoader());
        if (ctx instanceof org.apache.catalina.core.StandardContext) {
            ((org.apache.catalina.core.StandardContext) ctx).setDelegate(true);
        }



        System.out.println("Configuring app with webapp directory: " + webappDirLocation);
        System.out.println("Starting embedded Tomcat on http://localhost:" + PORT + " ...");

        tomcat.start();
        logger.info("Campus Club System started successfully on http://localhost:{}", PORT);

        System.out.println("\n>>> Server is ACTIVE! Open http://localhost:8080/ in your browser <<<");
        System.out.println("Endpoints:");
        System.out.println("  - Login:           http://localhost:8080/auth/login");
        System.out.println("  - Register:        http://localhost:8080/student/register");
        System.out.println("  - Student List:    http://localhost:8080/student/list");
        System.out.println("  - Club List:       http://localhost:8080/club/list");
        System.out.println("  - Create Club:     http://localhost:8080/club/add");
        System.out.println("  - Dashboard:       http://localhost:8080/student/dashboard");
        System.out.println("  - Review:          http://localhost:8080/membership/review");
        System.out.println("  - My Clubs:        http://localhost:8080/membership/my-clubs");
        System.out.println("======================================================================\n");

        tomcat.getServer().await();
    }
}
