package mb.stratego.build.bench.arguments;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=", commandDescription = "Benchmark a Spoofax project")
public class SpoofaxArguments {
    @Parameter(names = {"--preprocess"}, description = "The preprocess script is run after every checkout of a commit. ")
    public @Nullable String preprocessScript;
    @SuppressWarnings("NullableProblems") @Parameter(required = true, names={"--git-dir"}, converter = PathConverter.class)
    public Path gitDir;
    @SuppressWarnings("NullableProblems") @Parameter(required = true, names={"--project-dir"}, converter = PathConverter.class)
    public Path projectDir;
    @SuppressWarnings("NullableProblems") @Parameter(required = true, names={"--start-commit-hash"})
    public String startCommitHash;
    @SuppressWarnings("NullableProblems") @Parameter(required = true, names={"--end-commit-hash"})
    public String endCommitHash;
}
