package mb.stratego.build.bench;

import mb.pie.api.None;
import mb.pie.api.Pie;
import mb.pie.api.PieBuilder;
import mb.pie.api.PieSession;
import mb.pie.api.Task;
import mb.pie.runtime.PieBuilderImpl;
import mb.pie.runtime.layer.NoopLayer;
import mb.pie.runtime.logger.StreamLogger;
import mb.pie.store.lmdb.LMDBStore;
import mb.pie.taskdefs.guice.GuiceTaskDefs;
import mb.pie.taskdefs.guice.GuiceTaskDefsModule;
import mb.resource.ResourceKey;
import mb.resource.fs.FSPath;
import mb.stratego.build.Library;
import mb.stratego.build.StrIncr;
import mb.stratego.build.StrIncrModule;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.metaborg.core.resource.ResourceChangeKind;
import org.metaborg.core.resource.ResourceUtils;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.util.cmd.Arguments;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final String TMPDIR = System.getProperty("java.io.tmpdir");
    private static final String START_REV = "6add95c8dabfcc5631cd6a300c0f3b38fd9b5de9";
    public static final String GIT_URI = "https://github.com/Apanatshka/sep-comp-bench-project.git";

    public static void main(String[] args) throws Exception {
        if(args.length == 0 || args.length == 1 && args[0].equals("languageProject")) {
            //@formatter:off
            final List<String> subDirs =
                Arrays.asList(
                    "src-gen",
                    "trans",
                    "",
                    "src-gen/nabl2/collection",
                    "target/replicate/strj-includes");
            //@formatter:on
            run("languageProject", "incremental", "incremental", subDirs);
        } else if(args.length == 1 && args[0].equals("tiger")) {
            //@formatter:off
            final List<String> subDirs =
                Arrays.asList(
                    "src-gen",
                    "trans",
                    "",
                    "src-gen/nabl2/collection",
                    "src-gen/nabl2/dynsem",
                    "target/replicate/strj-includes");
            //@formatter:on
            run("tiger", "tiger", "org.metaborg.lang.tiger", subDirs);
        } else {
            run(StrategoArguments.fromArgs(args));
        }
    }

    private static void run(StrategoArguments strategoArguments) throws Exception {
        if(strategoArguments.showHelp) {
            // @formatter:off
            System.out.println("\n"
                + "Options:\n"
                + "   -i f|--input f   Read input from f\n"
                + "   -o f|--output f  Write output to f\n"
                + "   --main f | -m f    Main strategy to compile (default: main)\n"
                + "   --clean            Remove all existing Java files in the output directory\n"
                + "   -I d | --Include d Include modules from directory d\n"
                + "   --stacktrace i | -s i  Enable stacktracing (0 = no, 1 = always [default], 2 = only if assertions"
                    + " (java -ea) enabled for a class)\n"
                + "   -D name=value      Define a constant value strategy\n"
                + "   -sc <on|off>       Assume all term constructors are shared (default: on)\n"
                + "   -O n               Optimization level (0 = no optimization)\n"
                + "\n"
                + "   Library options:\n"
                + "\n"
                + "   -p <name>          Set package name <name> (should be unique for each library)\n"
                + "   -la <name>         Include library in package <name>\n"
                + "   --library | --lib  Build a library instead of an application\n" + "\n"
                + "   Configuration of the Stratego compiler:\n"
                + "\n"
                + "   --ast              Produce abstract syntax tree of packed program\n"
                + "   -F                 Produce core after front-end\n"
                + "   --single-strategy    Generate from a single strategy definition\n"
                + "   --boilerplate        Generate boilerplate (main/interopregister)\n"
                + "   --prefer-str       Prefer .str files over .rtree files\n"
                + "   --default-syntax syn        use syntax syn as default\n"
                + "   --cache-dir <dir>  Maintain a compilation cache in <dir>\n"
                + "   --fusion           Toggle specialize applications of innermost (default: on)\n"
                + "   --asfix            Concrete syntax parts are not imploded\n"
                + "   --Xsep-comp-tool  Compile based on sep-comp-tool (experimental)\n" + "\n"
                + "   General options:\n"
                + "\n"
                + "   -S|--silent      Silent execution (same as --verbose 0)\n"
                + "   --verbose i      Verbosity level i (default 1)\n"
                + "                    ( i as a number or as a verbosity descriptor:\n"
                + "                      emergency, alert, critical, error,\n"
                + "                      warning, notice, info, debug, vomit )\n"
                + "   -k i | --keep i  Keep intermediates (default 0)\n"
                + "   --statistics i   Print statistics (default 0 = none)\n"
                + "   -h | --help        Show help\n"
                + "   -v | --version     Display program's version\n"
                + "   -W,--warning C   Report warnings falling in category C. Categories:\n"
                + "                      all                      all categories\n"
                + "                      no-C                     no warnings in category C\n"
                + "                      debug-arguments          missing build operator [ on ]\n"
                + "                      obsolete-strategy-calls  obsolete strategies [ on ]\n"
                + "                      maybe-unbound-variables  unbound variables [ off ]\n"
                + "                      lower-case-constructors  lower-case constructors [ on ]\n" + "\n"
                + "   -h|-?|--help     Display usage information\n"
                + "   --about          Display information about this program\n"
                + "   --version        Same as --about\n"
                + "\n"
                + "Description:");
            // @formatter:on
            System.exit(0);
        } else if(strategoArguments.showVersion) {
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

        // file system store
        LMDBStore.withLMDBStore(pieBuilder, Paths.get(TMPDIR, "lmdb").toFile());

        pieBuilder.withTaskDefs(guiceTaskDefs);
        // For example purposes, we use verbose logging which will output to stdout.
        pieBuilder.withLogger(StreamLogger.verbose());
        // We always need to do a topDown build first as a clean build.

        File inputFile = Paths.get(strategoArguments.inputFile).toFile();

        List<File> includeDirs = new ArrayList<>(strategoArguments.includeDirs.size());
        for(String includeDir : strategoArguments.includeDirs) {
            final File include = Paths.get(includeDir).toFile();
            includeDirs.add(include);
            discoverDialects(spoofax, include.getAbsolutePath());
        }

        spoofax.languageDiscoveryService
            .languageFromDirectory(spoofax.resourceService.resolve(Main.class.getResource("/stratego.lang/").toURI()));

        List<String> builtinLibs = new ArrayList<>(strategoArguments.builtinLibraries.size());
        for(Library.Builtin builtinLibrary : strategoArguments.builtinLibraries) {
            builtinLibs.add(builtinLibrary.cmdArgString);
        }

        StrIncr strIncr = spoofax.injector.getInstance(StrIncr.class);

        final Path projectLocation =
            Paths.get(strategoArguments.inputFile).toAbsolutePath().normalize().getParent().getParent();

        final List<String> constants = new ArrayList<>(strategoArguments.constants.size());
        for(Map.Entry<String, String> e : strategoArguments.constants.entrySet()) {
            constants.add(e.getKey() + '=' + e.getValue());
        }
        try(final Pie pie = pieBuilder.build()) {
            StrIncr.Input strIncrInput =
                new StrIncr.Input(inputFile, strategoArguments.javaPackageName, includeDirs, builtinLibs,
                    strategoArguments.cacheDir == null ? null : Paths.get(strategoArguments.cacheDir).toFile(),
                    constants, strategoArguments.extraArguments, Paths.get(strategoArguments.outputFile).toFile(),
                    Collections.emptyList(), projectLocation.toFile());
            try(final PieSession session = pie.newSession()) {
                session.requireTopDown(strIncr.createTask(strIncrInput));
            }
        }
    }

    private static void discoverDialects(Spoofax spoofax, String projectLocation) throws FileSystemException {
        final FileObject location = spoofax.resourceService.resolve(projectLocation);
        spoofax.dialectProcessor.update(location, ResourceUtils
            .toChanges(ResourceUtils.find(location, new SpecialIgnoresSelector()), ResourceChangeKind.Create));
    }

    private static void run(String directory, String mainFileName, String packageName, Collection<String> subDirs)
        throws Exception {
        final Spoofax spoofax =
            new Spoofax(new NullEditorConfigBasedProject(), new StrIncrModule(), new GuiceTaskDefsModule());

        spoofax.languageDiscoveryService
            .languageFromDirectory(spoofax.resourceService.resolve(Main.class.getResource("/stratego.lang/").toURI()));

        final GuiceTaskDefs guiceTaskDefs = spoofax.injector.getInstance(GuiceTaskDefs.class);

        // We need to create the PIE runtime, using a PieBuilderImpl.
        final PieBuilder pieBuilder = new PieBuilderImpl();

        // file system store
        //        LMDBStore.withLMDBStore(pieBuilder, Paths.get(TMPDIR, "lmdb").toFile());

        pieBuilder.withTaskDefs(guiceTaskDefs);
        // For example purposes, we use verbose logging which will output to stdout.
        pieBuilder.withLogger(StreamLogger.verbose());

        // We always need to do a topDown build first as a clean build.
        Path gitRepoPath = Paths.get(TMPDIR, "stratego.build.bench");
        //        spoofax.resourceService.resolve(projectLocation.toString())
        //            .copyFrom(spoofax.resourceService.resolve(Main.class.getResource("/git-repo").toURI()),
        //                FileSelectorUtils.all());
        //        {
        //            final Path configPath = projectLocation.resolve(".git/config");
        //            System.out.println("configPath: " + configPath + ", exists: " + Files.exists(configPath));
        //            System.out.println("Text in file:\n" + Files.readAllLines(configPath));
        //            FileBasedConfig c = new FileBasedConfig(configPath.toFile(), FS.detect());
        //            c.load();
        //            System.out.println(c.toText());
        //            c.unset("core", "", "worktree");
        //            System.out.println(c.toText());
        //            c.save();
        //        }
        if(Files.exists(gitRepoPath)) {
            deleteRecursive(gitRepoPath);
        }
        final Git git = Git.cloneRepository().setDirectory(gitRepoPath.toFile()).setURI(GIT_URI).call();
        final Repository repository = git.getRepository();
        git.checkout().setStartPoint(START_REV).setAllPaths(true).call();
        final Path projectLocation = gitRepoPath.resolve(directory);

        final File inputFile = projectLocation.resolve("trans/" + mainFileName + ".str").toFile();

        final String javaPackageName = packageName + ".trans";

        final File outputFile =
            projectLocation.resolve("src-gen/stratego-java/" + javaPackageName.replace('.', '/') + "/Main.java")
                .toFile();

        final List<File> includeDirs = new ArrayList<>(subDirs.size());
        for(String subDir : subDirs) {
            includeDirs.add(projectLocation.resolve(subDir).toFile());
        }

        final File cacheDir = projectLocation.resolve("target/stratego-cache").toFile();

        // @formatter:off
        final List<String> builtinLibs = Arrays.asList(
            "stratego-lib",
            "stratego-sglr",
            "stratego-gpp",
            "stratego-xtc",
            "stratego-aterm",
            "stratego-sdf",
            "strc",
            "java-front"
        );
        // @formatter:on

        final Arguments extraArgs = new Arguments();
        extraArgs.add("-la", packageName + ".strategies");

        final StrIncr strIncr = spoofax.injector.getInstance(StrIncr.class);
        final StrIncr.Input strIncrInput =
            new StrIncr.Input(inputFile, javaPackageName, includeDirs, builtinLibs, cacheDir, Collections.emptyList(),
                extraArgs, outputFile, Collections.emptyList(), projectLocation.toFile());
        final Task<None> compileTask = strIncr.createTask(strIncrInput);
        try(final Pie pie = pieBuilder.build()) {
//            pieBuilder.withLayer((logger, taskDefs) -> new NoopLayer());

            long startTime = System.nanoTime();
            try(final PieSession session = pie.newSession()) {
                session.requireTopDown(compileTask);
            }
            long buildTime = System.nanoTime();
            System.out.println("\"First run took\", " + (buildTime - startTime));
            //            startTime = buildTime;

            // We can do a bottom up build with a changeset

//            pie.setObserver(compileTask, s -> {
//                // FIXME: Use jmh blackhole here to make sure nothing is optimized away
//            });

            try(RevWalk walk = new RevWalk(repository)) {
                final RevCommit startRev = walk.parseCommit(repository.resolve(START_REV));
                final Deque<RevCommit> commits = new ArrayDeque<>();
                walk.markStart(walk.parseCommit(repository.resolve("master")));
                for(RevCommit rev : walk) {
                    if(rev.equals(startRev)) {
                        break;
                    }
                    commits.push(rev);
                }
                RevCommit lastRev = startRev;
                for(RevCommit rev : commits) {
                    git.checkout().setStartPoint(rev).setAllPaths(true).call();

                    final List<DiffEntry> diffEntries;
                    try(final ObjectReader reader = repository.newObjectReader()) {
                        diffEntries = git.diff().setShowNameAndStatusOnly(true)
                            .setOldTree(new CanonicalTreeParser(null, reader, lastRev.getTree().getId())).call();
                    }
                    final Set<ResourceKey> changedResources = new HashSet<>();
                    for(DiffEntry diffEntry : diffEntries) {
                        switch(diffEntry.getChangeType()) {
                            case DELETE:
                                addLocalResource(gitRepoPath, directory, changedResources, diffEntry.getOldPath());
                                break;
                            case ADD:
                            case MODIFY:
                            case COPY:
                                addLocalResource(gitRepoPath, directory, changedResources, diffEntry.getNewPath());
                                break;
                            case RENAME:
                                addLocalResource(gitRepoPath, directory, changedResources, diffEntry.getOldPath());
                                addLocalResource(gitRepoPath, directory, changedResources, diffEntry.getNewPath());
                                break;
                            default:
                                throw new Exception("Unknown ChangeType: " + diffEntry.getChangeType());
                        }
                    }
                    System.out
                        .println("Changeset size between " + lastRev + " and " + rev + ": " + changedResources.size());
                    startTime = System.nanoTime();
                    try(final PieSession session = pie.newSession()) {
                        session.requireBottomUp(changedResources);
                    }
                    buildTime = System.nanoTime();
                    System.out.println("\"From " + lastRev + " to " + rev + " took\", " + (buildTime - startTime));

                    lastRev = rev;
                }

                walk.dispose();
            }
        }
    }

    private static void addLocalResource(Path gitRepoPath, String directory, Set<ResourceKey> changedResources,
        String path) {
        if(path.startsWith(directory)) {
            changedResources.add(new FSPath(gitRepoPath.resolve(path)));
        }
    }

    private static void deleteRecursive(Path projectLocation) throws IOException {
        Files.walkFileTree(projectLocation, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}