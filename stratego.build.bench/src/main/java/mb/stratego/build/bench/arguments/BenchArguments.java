package mb.stratego.build.bench.arguments;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import java.io.File;

@Parameters(separators = "=", commandDescription = "Pass default strj arguments to use the separate compiler as a normal batch compiler (for testing purposes)")
public class BenchArguments {
    private SpoofaxArguments parsedArguments = new SpoofaxArguments();
    private StrategoArguments strategoArguments = new StrategoArguments();

    private void setParsedCommandArguments(String command) {
        System.out.println(command);
        switch(command) {
            case "stratego":
                parsedArguments = strategoArguments;
                break;
            case "spoofax":
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static SpoofaxArguments parse(String... args) {
        JCommander jc = new JCommander();

        final BenchArguments benchArguments = new BenchArguments();
        jc.addObject(benchArguments);
        jc.setAcceptUnknownOptions(true);
        jc.addCommand("spoofax", benchArguments.parsedArguments);
        jc.addCommand("stratego", benchArguments.strategoArguments);

        try {
            jc.parse(args);
        } catch(ParameterException e) {
            System.err.println("Could not parse parameters " + e);
            jc.usage();
            System.exit(1);
        }

        if(benchArguments.help) {
            jc.usage();
            System.exit(0);
        }

        if(benchArguments.exit) {
            System.err.println("Exiting immediately for testing purposes");
            System.exit(0);
        }

        String command = jc.getParsedCommand();
        if(command == null) {
            jc.usage();
            System.exit(1);
        }

        benchArguments.setParsedCommandArguments(command);

        if(!benchArguments.parsedArguments.projectDir.toAbsolutePath().normalize()
            .startsWith(benchArguments.parsedArguments.gitDir.toAbsolutePath().normalize() + File.separator)) {
            System.err.println("Given PROJECT_DIR is not a subdirectory of the given GIT_DIR. ");
        }

        return benchArguments.parsedArguments;
    }

    @Parameter(names = { "--help", "-h" }, description = "Shows usage help", help = true)
    private boolean help;

    @Parameter(names = { "--exit" }, description = "Immediately exit, used for testing purposes", hidden = true)
    private boolean exit;
}
