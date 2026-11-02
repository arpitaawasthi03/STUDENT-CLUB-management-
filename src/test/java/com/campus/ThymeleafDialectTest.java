package com.campus;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThymeleafDialectTest {

    @Test
    public void testThymeleafFormBinding() {
        TemplateEngine engine = new TemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        engine.setTemplateResolver(resolver);

        String template = "<form th:object=\"${student}\">" +
                "<input type=\"text\" name=\"studentName\" th:value=\"*{studentName}\" />" +
                "</form>";

        Map<String, String> student = new HashMap<>();
        student.put("studentName", "John Doe");

        Context ctx = new Context();
        ctx.setVariable("student", student);

        String result = engine.process(template, ctx);
        System.out.println("Rendered: " + result);
        assertTrue(result.contains("John Doe"));
    }

    @Test
    public void testThFieldInStandardDialect() {
        TemplateEngine engine = new TemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        engine.setTemplateResolver(resolver);

        String template = "<form th:object=\"${student}\">" +
                "<input type=\"text\" th:field=\"*{studentName}\" />" +
                "</form>";

        Map<String, String> student = new HashMap<>();
        student.put("studentName", "John Doe");

        Context ctx = new Context();
        ctx.setVariable("student", student);

        try {
            String result = engine.process(template, ctx);
            System.out.println("th:field result: " + result);
        } catch (Exception e) {
            System.out.println("th:field failed as expected in pure standard dialect: " + e.getMessage());
        }
    }
}
