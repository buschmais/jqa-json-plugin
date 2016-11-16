package org.jqassistant.jqassistant.plugin.json.parser;

import com.buschmais.jqassistant.plugins.json.impl.parser.JSONLexer;
import com.buschmais.jqassistant.plugins.json.impl.parser.JSONParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class JSONParsingTestSuiteIT {

    private T input;

    /**
     * List of files we accept even if this file should not be accepted according
     * to the "JSON Parsing Test Suite". We didn't remove this files from the
     * file set because to be able to update the test set later.
     */
    private static List<String> FILES_WE_ACCEPT = asList(
        // Comments are file for us at any position
        "n_object_trailing_comment_slash_open.json",
        "n_object_trailing_comment.json",
        "n_structure_object_with_comment.json",
        // A single space is for us the same as an empty file
        "n_single_space.json",
        // Empty files are fine for us.
        "n_structure_no_data.json"
    );

    @Parameterized.Parameters
    public static List<Object[]> data() throws URISyntaxException {
        URL resource = JSONParsingTestSuiteIT.class.getResource("/json_parsing_test_suite");
        File directory = new File(resource.toURI());

        File[] jsons = directory.listFiles(f -> f.isFile() && f.getName().endsWith(".json"));

        return Stream.of(jsons)
                     .map(T::new)
                     .collect(Collectors.toList())
                     .stream()
                     .peek(t -> {
                         boolean isAcceptable = t.isAcceptable();
                         boolean shouldBeAccepted = FILES_WE_ACCEPT.contains(t.getFile().getName());
                         t.setAcceptable(isAcceptable || shouldBeAccepted);
                     })
                     .map(t -> new Object[]{t})
                     .collect(Collectors.toList());
    }

    public JSONParsingTestSuiteIT(T i) {
        input = i;
    }

    @Test
    public void canParseValidJSONFile() throws Exception {
        boolean passed = true;

        try (InputStream inputStream = Files.newInputStream(input.getFile().toPath())) {
            BaseErrorListener errorListener = new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                        int charPos, String msg, RecognitionException e) {
                    String message = String.format("Failed to parse %s at %d:%d. Parser failed with: %s",
                                                   input.getFile().getName(), line, charPos, msg);
                    throw new IllegalStateException(message);
                }
            };

            JSONLexer l = new JSONLexer(new ANTLRInputStream(inputStream));
            JSONParser p = new JSONParser(new CommonTokenStream(l));
            p.removeErrorListeners();
            p.addErrorListener(errorListener);
            l.removeErrorListeners();
            l.addErrorListener(errorListener);

            p.document();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            passed = false;
        }

        if (input.isAcceptable()) {
            assertThat("File " + input.getFile().getName() + " should be accepted.", passed, is(true));
        } else {
            assertThat("File " + input.getFile().getName() + " should not be accepted.", passed, is(false));
        }
    }

    static class T {

        private boolean acceptable;
        private File file;

        public T(File f) {
            file = f;
            acceptable = f.getName().startsWith("y_") || f.getName().startsWith("i_");
        }

        public void setAcceptable(boolean isAcceptable) {
            this.acceptable = isAcceptable;
        }

        public boolean isAcceptable() {
            return acceptable;
        }

        public File getFile() {
            return file;
        }
    }

 }