package nu.validator.htmlparser.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

public class Html5libTest {

    private final Path testDir;

    public Html5libTest() throws URISyntaxException {
        this.testDir = Path.of(Html5libTest.class.getResource("/html5lib-tests").toURI());
    }

    public void testEncoding() throws Exception {
        Files.walkFileTree(testDir.resolve("encoding"), new TestVisitor(true, false, file -> new EncodingTester(Files.newInputStream(file)).runTests()));
        if(EncodingTester.exitStatus != 0) {
            assert false : "Encoding test failed";
        }
    }

    public void testTokenizer() throws Exception {
        Files.walkFileTree(testDir.resolve("tokenizer"), new TestVisitor(true, true, file -> new TokenizerTester(Files.newInputStream(file)).runTests()));
        if(TokenizerTester.exitStatus != 0) {
            assert false : "Tokenizer test failed";
        }
    }

    public void testTree() throws Exception {
        Files.walkFileTree(testDir.resolve("tree-construction"), new TestVisitor(true, false, file -> new TreeTester(Files.newInputStream(file)).runTests()));
        if(TreeTester.exitStatus != 0) {
            assert false : "Tree test failed";
        }
    }

    private interface TestConsumer extends Consumer<Path> {

        @Override
        default void accept(Path t) {
            try {
                acceptTest(t);
            } catch(Throwable e) {
                throw new AssertionError(e);
            }
        }

        void acceptTest(Path t) throws Throwable;

    }

    private static class TestVisitor extends SimpleFileVisitor<Path> {

        private final boolean skipScripted;
        private final boolean requireTestExtension;
        private final TestConsumer runner;

        private TestVisitor(boolean skipScripted, boolean requireTestExtension, TestConsumer runner) {
            this.skipScripted = skipScripted;
            this.requireTestExtension = requireTestExtension;
            this.runner = runner;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (skipScripted && dir.getFileName().equals(Path.of("scripted"))) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!requireTestExtension || file.getFileName().toString().endsWith(".test")) {
                runner.accept(file);
            }
            return FileVisitResult.CONTINUE;
        }
    }

}
