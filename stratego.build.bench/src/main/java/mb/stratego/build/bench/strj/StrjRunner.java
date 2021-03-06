package mb.stratego.build.bench.strj;

import mb.pie.api.Pie;
import mb.pie.api.PieBuilder;
import mb.pie.api.PieSession;
import mb.pie.runtime.PieBuilderImpl;
import mb.pie.runtime.logger.StreamLogger;
import mb.pie.taskdefs.guice.GuiceTaskDefs;
import mb.pie.taskdefs.guice.GuiceTaskDefsModule;
import mb.stratego.build.bench.Main;
import mb.stratego.build.strincr.Library;
import mb.stratego.build.strincr.StrIncr;
import mb.stratego.build.strincr.StrIncrModule;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.core.Spoofax;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StrjRunner {
    private static final String STRJ_USAGE;

    static {
        //@formatter:off
        STRJ_USAGE = "Run with the 'benchmark' subcommand for benchmarking. \n"
            + "By default the strj commandline is emulated for testing purposes. \n" + "\n" + "Options:\n"
            + "   -i f|--input f   Read input from f\n" + "   -o f|--output f  Write output to f\n"
            + "   --main f | -m f    Main strategy to compile (default: main)\n"
            + "   --clean            Remove all existing Java files in the output directory\n"
            + "   -I d | --Include d Include modules from directory d\n"
            + "   --stacktrace i | -s i  Enable stacktracing (0 = no, 1 = always [default], 2 = only if assertions"
            + " (java -ea) enabled for a class)\n" + "   -D name=value      Define a constant value strategy\n"
            + "   -sc <on|off>       Assume all term constructors are shared (default: on)\n"
            + "   -O n               Optimization level (0 = no optimization)\n" + "\n" + "   Library options:\n" + "\n"
            + "   -p <name>          Set package name <name> (should be unique for each library)\n"
            + "   -la <name>         Include library in package <name>\n"
            + "   --library | --lib  Build a library instead of an application\n" + "\n"
            + "   Configuration of the Stratego compiler:\n" + "\n"
            + "   --ast              Produce abstract syntax tree of packed program\n"
            + "   -F                 Produce core after front-end\n"
            + "   --single-strategy    Generate from a single strategy definition\n"
            + "   --boilerplate        Generate boilerplate (main/interopregister)\n"
            + "   --prefer-str       Prefer .str files over .rtree files\n"
            + "   --default-syntax syn        use syntax syn as default\n"
            + "   --cache-dir <dir>  Maintain a compilation cache in <dir>\n"
            + "   --fusion           Toggle specialize applications of innermost (default: on)\n"
            + "   --asfix            Concrete syntax parts are not imploded\n"
            + "   --Xsep-comp-tool  Compile based on sep-comp-tool (experimental)\n" + "\n" + "   General options:\n"
            + "\n" + "   -S|--silent      Silent execution (same as --verbose 0)\n"
            + "   --verbose i      Verbosity level i (default 1)\n"
            + "                    ( i as a number or as a verbosity descriptor:\n"
            + "                      emergency, alert, critical, error,\n"
            + "                      warning, notice, info, debug, vomit )\n"
            + "   -k i | --keep i  Keep intermediates (default 0)\n"
            + "   --statistics i   Print statistics (default 0 = none)\n" + "   -h | --help        Show help\n"
            + "   -v | --version     Display program's version\n"
            + "   -W,--warning C   Report warnings falling in category C. Categories:\n"
            + "                      all                      all categories\n"
            + "                      no-C                     no warnings in category C\n"
            + "                      debug-arguments          missing build operator [ on ]\n"
            + "                      obsolete-strategy-calls  obsolete strategies [ on ]\n"
            + "                      maybe-unbound-variables  unbound variables [ off ]\n"
            + "                      lower-case-constructors  lower-case constructors [ on ]\n" + "\n"
            + "   -h|-?|--help     Display usage information\n"
            + "   --about          Display information about this program\n" + "   --version        Same as --about\n"
            + "\n" + "Description:";
        //@formatter:on
    }

    public static void run(String[] args) throws Exception {
        Arguments arguments = Arguments.fromArgs(args);
        if(arguments.showHelp || args.length == 0 || arguments.outputFile == null || arguments.inputFile == null) {
            System.out.println(STRJ_USAGE);
            System.exit(0);
        } else if(arguments.showVersion) {
            System.out.println("STRJ ${version}\n");
            System.exit(0);
        }

        Spoofax spoofax =
            new Spoofax(new NullEditorSingleFileProject(), new StrIncrModule(), new GuiceTaskDefsModule());

        spoofax.languageDiscoveryService
            .languageFromDirectory(spoofax.resourceService.resolve(Main.class.getResource("/stratego.lang/").toURI()));

        GuiceTaskDefs guiceTaskDefs = spoofax.injector.getInstance(GuiceTaskDefs.class);

        // We need to create the PIE runtime, using a PieBuilderImpl.
        final PieBuilder pieBuilder = new PieBuilderImpl();

        pieBuilder.withTaskDefs(guiceTaskDefs);
        // For example purposes, we use verbose logging which will output to stdout.
        pieBuilder.withLogger(StreamLogger.verbose());
        // We always need to do a topDown build first as a clean build.

        File inputFile = Paths.get(arguments.inputFile).toFile();
        final FileObject inputParent = spoofax.resourceService.resolve(inputFile.getAbsoluteFile().getParentFile());

        List<File> includeDirs = new ArrayList<>(arguments.includeDirs.size());
        for(String includeDir : arguments.includeDirs) {
            final File include = Paths.get(includeDir).toFile();
            includeDirs.add(include);
            Main.discoverDialects(spoofax, spoofax.resourceService.resolve(include), inputParent);
        }

        spoofax.languageDiscoveryService
            .languageFromDirectory(spoofax.resourceService.resolve(Main.class.getResource("/stratego.lang/").toURI()));

        List<String> builtinLibs = new ArrayList<>(arguments.builtinLibraries.size());
        for(Library.Builtin builtinLibrary : arguments.builtinLibraries) {
            builtinLibs.add(builtinLibrary.cmdArgString);
        }

        StrIncr strIncr = spoofax.injector.getInstance(StrIncr.class);

        final Path projectLocation =
            Paths.get(arguments.inputFile).toAbsolutePath().normalize().getParent().getParent();

        final List<String> constants = new ArrayList<>(arguments.constants.size());
        for(Map.Entry<String, String> e : arguments.constants.entrySet()) {
            constants.add(e.getKey() + '=' + e.getValue());
        }
        try(final Pie pie = pieBuilder.build()) {
            StrIncr.Input strIncrInput =
                new StrIncr.Input(inputFile, arguments.javaPackageName, includeDirs, builtinLibs,
                    arguments.cacheDir == null ? null : Paths.get(arguments.cacheDir).toFile(), constants,
                    arguments.extraArguments, Paths.get(arguments.outputFile).toFile(), Collections.emptyList(),
                    projectLocation.toFile());
            try(final PieSession session = pie.newSession()) {
                session.require(strIncr.createTask(strIncrInput));
            }
        }
    }

}
