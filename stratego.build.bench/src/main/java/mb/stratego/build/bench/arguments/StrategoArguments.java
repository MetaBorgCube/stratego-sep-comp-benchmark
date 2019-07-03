package mb.stratego.build.bench.arguments;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
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
    public String mainStrategy = "main";
    @DynamicParameter(names = "-D")
    public Map<String, String> dynamicParameters = new HashMap<>();
    @Parameter(names = "--output-file", converter = PathConverter.class)
    public Path outputPath;

    public static StrategoArguments from(SpoofaxArguments args) {
        StrategoArguments result = new StrategoArguments();
        if(args.preprocessScript == null) {
            result.preprocessScript = "mvn package";
        } else {
            result.preprocessScript = args.preprocessScript + " && mvn package";
        }
        result.gitDir = args.gitDir;
        result.projectDir = args.projectDir;
        result.startCommitHash = args.startCommitHash;
        result.endCommitHash = args.endCommitHash;
        return result;
    }
}
