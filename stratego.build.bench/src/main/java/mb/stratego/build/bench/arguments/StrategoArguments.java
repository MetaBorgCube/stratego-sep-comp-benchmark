package mb.stratego.build.bench.arguments;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=", commandDescription = "Benchmark a Stratego project")
public class StrategoArguments extends SpoofaxArguments {
    @Parameter(names = {"--main-file", "-m"}, description = "The main file of the project", converter = PathConverter.class, required = true)
    public Path strategoMainFile;
    @Parameter(names = {"--package-name", "-p"}, description = "The package of the Java code that will be produced", required = true)
    public String packageName;
    @Parameter(names = {"--include-dirs", "-I"}, description = "Directories from which the compiler can search for modules")
    public List<String> includeDirs = new ArrayList<>();

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
