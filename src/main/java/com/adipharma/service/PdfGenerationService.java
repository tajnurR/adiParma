package com.adipharma.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class PdfGenerationService {

    private final TemplateEngine templateEngine;
    private final String baseUri;

    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.baseUri = resolveBaseUri();
    }

    public byte[] generateFromTemplate(String templateName, Map<String, Object> model) {
        String html = renderTemplate(templateName, model);
        return generateFromHtml(html);
    }

    public String renderTemplate(String templateName, Map<String, Object> model) {
        Context context = new Context(Locale.ENGLISH);
        if (model != null) {
            context.setVariables(model);
        }
        return templateEngine.process(templateName, context);
    }

    public byte[] generateFromHtml(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate PDF document.", ex);
        }
    }

    private String resolveBaseUri() {
        try {
            return new ClassPathResource("templates/").getURL().toString();
        } catch (IOException ex) {
            return "";
        }
    }
}
