package dev.fusionize.common.template;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemplateParserFreeMaker {

    private final Configuration cfg;

    private TemplateParserFreeMaker(Configuration cfg) {
        this.cfg = cfg;
    }

    /**
     * Parse and process the given template with provided model.
     */
    public void parse(Map<String, Object> model, String template, Writer out)
            throws IOException, TemplateParserException {
        try {
            Template temp = cfg.getTemplate(template);
            temp.process(model, out);
        } catch (TemplateException | ParseException e) {
            throw new TemplateParserException(e.getMessage(), e);
        }
    }

    /**
     * Builder for TemplateParserFreeMaker
     */
    public static class Builder {
        private final List<TemplateLoader> loaders = new ArrayList<>();
        private String encoding = "UTF-8";
        private boolean logTemplateExceptions = false;
        private boolean wrapUncheckedExceptions = true;

        public Builder withFileTemplateDir(String templateDir) throws IOException {
            loaders.add(new FileTemplateLoader(new File(templateDir)));
            return this;
        }

        public Builder withUrlTemplate(URL baseUrl) {
            loaders.add(new URLTemplateLoader() {
                @Override
                protected URL getURL(String name) {
                    try {
                        // Strip locale suffixes like _en_US.ftl if file not found
                        URL resolved = new URL(baseUrl, name);
                        if (new File(resolved.getPath()).exists()) return resolved;

                        String cleaned = name.replaceAll("_[a-zA-Z]{2}(_[a-zA-Z]{2})?\\.ftl$", ".ftl");
                        return new URL(baseUrl, cleaned);
                    } catch (Exception e) {
                        return null;
                    }
                }
            });
            return this;
        }


        public Builder withStringTemplate(String name, String content) {
            StringTemplateLoader stringLoader = new StringTemplateLoader();
            stringLoader.putTemplate(name, content);
            loaders.add(stringLoader);
            return this;
        }

        public Builder withEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder logTemplateExceptions(boolean value) {
            this.logTemplateExceptions = value;
            return this;
        }

        public Builder wrapUncheckedExceptions(boolean value) {
            this.wrapUncheckedExceptions = value;
            return this;
        }

        public TemplateParserFreeMaker build() throws IOException {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);

            if (loaders.isEmpty()) {
                throw new IllegalStateException("At least one template loader must be provided.");
            }

            TemplateLoader[] loaderArray = loaders.toArray(new TemplateLoader[0]);
            MultiTemplateLoader multiLoader = new MultiTemplateLoader(loaderArray);

            cfg.setTemplateLoader(multiLoader);
            cfg.setDefaultEncoding(encoding);
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(logTemplateExceptions);
            cfg.setWrapUncheckedExceptions(wrapUncheckedExceptions);
            cfg.setFallbackOnNullLoopVariable(false);

            return new TemplateParserFreeMaker(cfg);
        }
    }
}
