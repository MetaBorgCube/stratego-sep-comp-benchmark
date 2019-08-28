package mb.stratego.build.bench.arguments;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Parameters(separators = "=", commandDescription = "Benchmark a Stratego project")
public class StrategoArguments extends SpoofaxArguments {
    @Parameter(names = "--main-file", description = "The main file of the project", converter = PathConverter.class, required = true)
    public Path strategoMainPath;
    @Parameter(names = {"--package-name", "-p"}, description = "The package of the Java code that will be produced", required = true)
    public String packageName;
    @Parameter(names = {"--include-dir", "-I"}, description = "Directory to be searched when resolving imports (can be used multiple times)")
    public List<String> includeDirs = new ArrayList<>();
    @Parameter(names = "--main-strategy")
    public @Nullable String mainStrategy = "main";
    @DynamicParameter(names = "-D")
    public Map<String, String> dynamicParameters = new HashMap<>();
    @Parameter(names = {"--output-file", "--str-output-file"}, converter = PathConverter.class)
    public Path strOutputPath;

    public static StrategoArguments from(SpoofaxArguments args) {
        StrategoArguments result = new StrategoArguments();
        if(args.preprocessScript == null) {
            result.preprocessScript = "mvn generate-sources -nsu";
        } else {
            result.preprocessScript = args.preprocessScript + " && mvn generate-sources -nsu";
        }
        result.gitDir = args.gitDir;
        result.projectDir = args.projectDir;
        result.startCommitHash = args.startCommitHash;
        result.endCommitHash = args.endCommitHash;

        // Set to null so we build as library instead of executable
        result.mainStrategy = null;

        return result;
    }
}
