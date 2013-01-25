package org.mayocat.shop.views.rhino.handlebars;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import org.mayocat.shop.views.Template;
import org.mayocat.shop.views.TemplateEngine;
import org.mayocat.shop.views.rhino.AbstractRhinoEngine;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * @version $Id$
 */
public class HandlebarsEngine extends AbstractRhinoEngine implements TemplateEngine
{
    private static final String HANDLEBARS_FILENAME = "handlebars.js";

    private static final String HANDLEBARS_HELPERS_FILENAME = "helpers.js";

    private static final String HANDLEBARS_FILEPATH = "javascripts/" + HANDLEBARS_FILENAME;

    private static final String HANDLEBARS_HELPERS_FILEPATH = "javascripts/" + HANDLEBARS_HELPERS_FILENAME;

    public HandlebarsEngine() throws IOException
    {
        super(HANDLEBARS_FILENAME, getResourceReader(HANDLEBARS_FILEPATH));
    }

    @Override
    protected void initialize()
    {
        try {
            Reader helpersReader = getResourceReader(HANDLEBARS_HELPERS_FILEPATH);
            Context engineContext = Context.enter();
            engineContext.setOptimizationLevel(9);
            try {
                Scriptable globalScope = getScope();
                engineContext.evaluateReader(globalScope,
                        helpersReader,
                        HANDLEBARS_HELPERS_FILENAME,
                        0,
                        null);
            } finally {
                Context.exit();
            }
        } catch (IOException ex) {
            throw new RuntimeException("ERROR : Unable to load engine resource: ", ex);
        }
    }

    @Override
    public void register(Template template)
    {
        Context context = Context.enter();
        try {

            Scriptable compileScope = context.newObject(getScope());
            compileScope.setParentScope(getScope());
            compileScope.put("source", compileScope, template.getContent());
            compileScope.put("name", compileScope, template.getName());

            try {
                context.evaluateString(compileScope,
                        "templates[name] = Handlebars.compile(source);",
                        "JHBSCompiler",
                        0,
                        null);

                if (template.isPartial()) {
                    context.evaluateString(compileScope,
                            "Handlebars.registerPartial(name, templates[name]);",
                            "JHBSPartialRegister",
                            0,
                            null);
                }
            } catch (JavaScriptException e) {
                // Fail hard on any compile time error for dust templates
                throw new RuntimeException(e);
            }
        } finally {
            Context.exit();
        }
    }

    @Override
    public String render(String templateName, String json)
    {
        Context context = Context.enter();
        try {
            StringWriter stringWriter = new StringWriter();
            Scriptable renderScope = context.newObject(getScope());
            renderScope.setParentScope(getScope());
            renderScope.put("writer", renderScope, stringWriter);
            renderScope.put("name", renderScope, templateName);
            renderScope.put("json", renderScope, json);

            try {
                context.evaluateString(renderScope,
                        "writer.write( templates[name](JSON.parse(json)) )",
                        "JHBSRenderer",
                        0,
                        null);
                return stringWriter.toString();
            } catch (JavaScriptException e) {
                // Fail hard on any compile time error for dust templates
                throw new RuntimeException(e);
            }
        } finally {
            Context.exit();
        }
    }

    private static Reader getResourceReader(String resource) throws IOException
    {
        return Resources.newReaderSupplier(
                Resources.getResource(resource), Charsets.UTF_8
        ).getInput();
    }
}