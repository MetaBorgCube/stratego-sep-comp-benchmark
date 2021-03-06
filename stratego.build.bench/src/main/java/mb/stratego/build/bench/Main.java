package mb.stratego.build.bench;

import mb.pie.api.ExecException;
import mb.pie.api.Logger;
import mb.pie.api.None;
import mb.pie.api.Pie;
import mb.pie.api.PieBuilder;
import mb.pie.api.PieSession;
import mb.pie.api.Task;
import mb.pie.runtime.PieBuilderImpl;
import mb.pie.runtime.exec.Stats;
import mb.pie.runtime.logger.NoopLogger;
import mb.pie.runtime.logger.StreamLogger;
import mb.pie.taskdefs.guice.GuiceTaskDefs;
import mb.pie.taskdefs.guice.GuiceTaskDefsModule;
import mb.resource.ResourceKey;
import mb.resource.fs.FSPath;
import mb.stratego.build.bench.arguments.BenchArguments;
import mb.stratego.build.bench.arguments.SpoofaxArguments;
import mb.stratego.build.bench.arguments.StrategoArguments;
import mb.stratego.build.bench.strj.NullEditorSingleFileProject;
import mb.stratego.build.bench.strj.SpecialIgnoresSelector;
import mb.stratego.build.bench.strj.StrjRunner;
import mb.stratego.build.strincr.Backend;
import mb.stratego.build.strincr.BuildStats;
import mb.stratego.build.strincr.StrIncr;
import mb.stratego.build.strincr.StrIncrModule;
import mb.stratego.build.util.ResourceAgentTracker;
import mb.stratego.build.util.StrIncrContext;
import mb.stratego.build.util.StrategoExecutor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.paths.ILanguagePathService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.resource.ResourceChangeKind;
import org.metaborg.core.resource.ResourceUtils;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.metaborg.spoofax.meta.core.build.SpoofaxLangSpecCommonPaths;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigService;
import org.metaborg.util.cmd.Arguments;
import org.metaborg.util.functions.CheckedFunction2;
import org.spoofax.interpreter.terms.IStrategoList;
import org.strategoxt.strj.main_strj_0_0;
import org.strategoxt.strj.strj;
import com.google.common.collect.Sets;

public class Main {
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String START_REV = "6add95c8dabfcc5631cd6a300c0f3b38fd9b5de9";
    private static final String GIT_URI = "https://github.com/Apanatshka/sep-comp-bench-project.git";
    private static final String[] GIT_RESET_HEAD = { "git", "reset", "HEAD", "." };
    private static final String[] GIT_CHECKOUT_HEAD = { "git", "checkout", "--", "." };
    @SuppressWarnings("NullableProblems") private static String[] GIT_CHECKOUT_START_COMMIT;
    // @formatter:off
    private static final String CSV_BATCH_HEADER = "\"commit (SHA-1)\","
        + "\"changeset size (no. of files)\","
        + "\"Stratego compile time (ns)\","
        + "\"memory in use after build/GC (B)\",";
    private static final String CSV_HEADER = CSV_BATCH_HEADER
        + "\"Java compile time (ns)\"";
    private static final String CSV_WITH_STATS_HEADER = CSV_HEADER + ","
        + "\"PIE requires\","
        + "\"PIE executions\","
        + "\"PIE fileReqs\","
        + "\"PIE fileGens\","
        + "\"PIE callReqs\","
        + BuildStats.CSV_HEADER;
    private static final String CSV_WITH_STATS_HEADER2 = CSV_HEADER + ","
        + "\"PIE requires\","
        + "\"PIE executions\","
        + "\"PIE fileReqs\","
        + "\"PIE fileGens\","
        + "\"PIE callReqs\","
        + BuildStats.CSV_HEADER2;
    // @formatter:on
    private static Map<FileObject, Set<FileObject>> lastTableFiles = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if(args.length > 0 && args[0].equals("benchmark")) {
            final SpoofaxArguments arguments = BenchArguments.parse(Arrays.copyOfRange(args, 1, args.length));
            GIT_CHECKOUT_START_COMMIT = new String[] { "git", "checkout", arguments.startCommitHash };
            if(arguments instanceof StrategoArguments) {
                final StrategoArguments strategoArguments = (StrategoArguments) arguments;
                if(strategoArguments.useOldBatchCompiler) {
                    benchBatch(strategoArguments,
                        new Spoofax(new NullEditorSingleFileProject(), new StrIncrModule(), new GuiceTaskDefsModule()));
                } else {
                    bench(strategoArguments,
                        new Spoofax(new NullEditorSingleFileProject(), new StrIncrModule(), new GuiceTaskDefsModule()));
                }
            } else {
                bench(arguments,
                    new Spoofax(new NullEditorConfigBasedProject(), new StrIncrModule(), new GuiceTaskDefsModule()));
            }
        } else {
            StrjRunner.run(args);
        }
    }

    private static void benchBatch(StrategoArguments arguments, Spoofax spoofax) throws Exception {
        final Path gitRepoPath = arguments.gitDir.toAbsolutePath().normalize();
        final File gitRepoFile = gitRepoPath.toFile();
        final Git git = Git.open(gitRepoFile);
        resetRepository(gitRepoFile);
        final Repository repository = git.getRepository();
        final Path projectLocation = arguments.projectDir.toAbsolutePath().normalize();

        final Arguments args = new Arguments();
        args.add("-i", projectLocation.resolve(arguments.strategoMainPath).toFile());

        final String javaPackageName = arguments.packageName + ".trans";
        args.add("-p", javaPackageName);

        final File outputFile;
        if(arguments.strOutputPath != null) {
            outputFile = arguments.strOutputPath.toFile();
        } else {
            outputFile =
                projectLocation.resolve("src-gen/stratego-java/" + javaPackageName.replace('.', '/') + "/Main.java")
                    .toFile();
        }
        args.add("-o", outputFile);

        for(String subDir : arguments.includeDirs) {
            final File include = projectLocation.resolve(subDir).normalize().toFile();
            args.add("-I", include);
        }

        final File cacheDir = projectLocation.resolve("target/stratego-cache").toFile();
        args.add("--cache-dir", cacheDir);

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
        for(String lib : builtinLibs) {
            args.add("-la", lib);
        }

        args.add("-clean");
        if(arguments.mainStrategy != null) {
            args.add("-m", arguments.mainStrategy);
        } else {
            args.add("--library");
        }

        for(Map.Entry<String, String> stringStringEntry : arguments.dynamicParameters.entrySet()) {
            args.add("-D", stringStringEntry.getKey() + "=\"" + stringStringEntry.getValue() + "\"");
        }

        final Logger logger = new NoopLogger();

        final IStrategoList input = Backend.buildInput(args, "strj");

        // WARMUP

        System.err.println("WARMUP");
        commitWalk(repository, arguments.startCommitHash, arguments.endCommitHash, arguments.skipCommits, 3,
            (RevCommit lastRev, RevCommit rev) -> {
                System.err.println("BUILD FOR COMMIT " + rev.name());
                Runtime.getRuntime().exec(new String[] { "git", "checkout", "--force", rev.name() }, null, gitRepoFile)
                    .waitFor();

                runPreprocessScript(arguments.preprocessScript, projectLocation.toFile(), null);
                
                final StrIncrContext strContext = new StrIncrContext();
                strj.init(strContext);
                final StrategoExecutor.ExecutionResult result = Backend
                    .runLocallyUniqueStringStrategy(logger, true, nullResourceAgent(spoofax.resourceService), main_strj_0_0.instance,
                        input, strContext);
                if(!result.success) {
                    throw new RuntimeException("Compilation failed");
                }
                return null;
            });

        // MEASUREMENTS

        System.err.println("MEASUREMENTS");

        try(final PrintWriter log = new PrintWriter(
            new BufferedWriter(new FileWriter(gitRepoPath.resolve("../bench.csv").toFile())))) {
            log.println(CSV_BATCH_HEADER);
            for(int iteration = 1; iteration <= arguments.benchmarkIterations; iteration++) {
                System.err.println("ITERATION " + iteration + "/" + arguments.benchmarkIterations);
                // Reset repo
                resetRepository(gitRepoFile);

                // BUILDS
                commitWalk(repository, arguments.startCommitHash, arguments.endCommitHash, arguments.skipCommits,
                    (RevCommit lastRev, RevCommit rev) -> {
                        System.err.println("BUILD FOR COMMIT " + rev.name());
                        Runtime.getRuntime()
                            .exec(new String[] { "git", "checkout", "--force", rev.name() }, null, gitRepoFile)
                            .waitFor();

                        // BUILD
                        {
                            final ChangesFromDiff changesFromDiff =
                                changesFromDiff(gitRepoPath, git, repository, lastRev, rev);
                            runPreprocessScript(arguments.preprocessScript, projectLocation.toFile(), null);
                            Stats.reset();
                            BuildStats.reset();

                            forceGc();
                            final StrIncrContext strContext = new StrIncrContext();
                            strj.init(strContext);
                            final StrategoExecutor.ExecutionResult result = Backend
                                .runLocallyUniqueStringStrategy(logger, true, nullResourceAgent(spoofax.resourceService),
                                    main_strj_0_0.instance, input, strContext);
                            if(!result.success) {
                                throw new RuntimeException("Compilation failed");
                            }

                            log.print("\"" + rev.name() + "\",");
                            log.print(changesFromDiff.strategoChangeSize + ",");
                            log.print(result.time + ",");
                        }

                        // MEMORY CHECK
                        {
                            forceGc();
                            log.println(getAllocatedMemory());
                        }

                        // JAVA COMPILATION is excluded
                        /*
                        Normally WebDSL is compiled into multiple libraries. This isn't easy to do from Java while
                         running all the Stratego compiler calls in this same JVM. So we build WebDSL as 1 codebase.
                         But then the strj compiler will generate too many field in the Main class and the Java compiler
                         will reject that. Therefore we exclude Java compilation.
                         */

                        return null;
                    });
            }
        }
    }

    private static ResourceAgentTracker nullResourceAgent(IResourceService resourceService) {
        final FileObject base = resourceService.resolve(System.getProperty("user.dir"));
        return new ResourceAgentTracker(resourceService, base, new NullOutputStream(), new NullOutputStream());
    }

    private static void bench(StrategoArguments arguments, Spoofax spoofax) throws Exception {
        // Load the Stratego language project to use for parsing Stratego code in stratego.build.StrIncrFront
        spoofax.languageDiscoveryService
            .languageFromDirectory(spoofax.resourceService.resolve(Main.class.getResource("/stratego.lang/").toURI()));

        // We need to create the PIE runtime, using a PieBuilderImpl.
        final PieBuilder pieBuilder =
            new PieBuilderImpl().withTaskDefs(spoofax.injector.getInstance(GuiceTaskDefs.class));
        //        pieBuilder.withLogger(StreamLogger.verbose());

        final Path gitRepoPath = arguments.gitDir.toAbsolutePath().normalize();
        final File gitRepoFile = gitRepoPath.toFile();
        final Git git = Git.open(gitRepoFile);
        resetRepository(gitRepoFile);
        final Repository repository = git.getRepository();
        final Path projectLocation = arguments.projectDir.toAbsolutePath().normalize();

        final File inputFile = projectLocation.resolve(arguments.strategoMainPath).toFile();

        final String javaPackageName = arguments.packageName + ".trans";

        final File outputFile;
        if(arguments.strOutputPath != null) {
            outputFile = arguments.strOutputPath.toFile();
        } else {
            outputFile =
                projectLocation.resolve("src-gen/stratego-java/" + javaPackageName.replace('.', '/') + "/Main.java")
                    .toFile();
        }

        final List<File> includeDirs = new ArrayList<>(arguments.includeDirs.size());
        for(String subDir : arguments.includeDirs) {
            final File include = projectLocation.resolve(subDir).normalize().toFile();
            includeDirs.add(include);
            discoverDialects(spoofax, spoofax.resourceService.resolve(include),
                spoofax.resourceService.resolve(projectLocation.toFile()));
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
        if(arguments.mainStrategy != null) {
            extraArgs.add("-m", arguments.mainStrategy);
        } else {
            extraArgs.add("--library");
        }

        final List<String> constants = new ArrayList<>();
        for(Map.Entry<String, String> stringStringEntry : arguments.dynamicParameters.entrySet()) {
            constants.add(stringStringEntry.getKey() + "=" + stringStringEntry.getValue());
        }

        final StrIncr strIncr = spoofax.injector.getInstance(StrIncr.class);
        final StrIncr.Input strIncrInput =
            new StrIncr.Input(inputFile, javaPackageName, includeDirs, builtinLibs, cacheDir, constants, extraArgs,
                outputFile, Collections.emptyList(), projectLocation.toFile());
        final Task<None> compileTask = strIncr.createTask(strIncrInput);

        // WARMUP

        System.err.println("WARMUP");

        try(final Pie pie = pieBuilder.build()) {
            runPreprocessScript(arguments.preprocessScript, projectLocation.toFile(), spoofax);
            Stats.reset();
            BuildStats.reset();
            System.err.print("CLEAN BUILD (" + arguments.startCommitHash + ")");
            try(final PieSession session = pie.newSession()) {
                session.require(compileTask);
            }
            System.err.println();

            try(final PrintWriter log = new PrintWriter(
                new BufferedWriter(new FileWriter(gitRepoPath.resolve("../input-stats.csv").toFile())))) {
                log.println(
                    "\"Strategy\",\"No. of modules defining it\",\"Total no. of definitions\",\"Max no. of definitions per module\",\"Mean no. of definitions per module\"");
                for(Map.Entry<String, List<Integer>> modsStrats : BuildStats.modulesDefiningStrategy.entrySet()) {
                    final List<Integer> defNumbers = modsStrats.getValue();
                    final int definingModules = defNumbers.size();
                    int sum = 0;
                    int max = Integer.MIN_VALUE;
                    for(Integer number : defNumbers) {
                        sum += number;
                        if(number > max) {
                            max = number;
                        }
                    }
                    final int mean = sum / definingModules;
                    log.println(modsStrats.getKey() + "," + definingModules + "," + sum + "," + max + "," + mean);
                }
            }
            commitWalk(repository, arguments.startCommitHash, arguments.endCommitHash, arguments.skipCommits, 3,
                (RevCommit lastRev, RevCommit rev) -> {
                    System.err.print("INCREMENTAL BUILD FOR COMMIT " + rev.name());
                    Runtime.getRuntime()
                        .exec(new String[] { "git", "checkout", "--force", rev.name() }, null, gitRepoFile).waitFor();

                    final ChangesFromDiff changesFromDiff = changesFromDiff(gitRepoPath, git, repository, lastRev, rev);
                    final Set<ResourceKey> changedResources = changesFromDiff.strategoFileChanges;
                    try {
                        final List<ResourceKey> preprocessChanged =
                            runPreprocessScript(arguments.preprocessScript, projectLocation.toFile(), spoofax);
                        changedResources.addAll(preprocessChanged);
                    } catch(IOException e) {
                        System.err.println("... failed: " + e.getMessage());
                        return null;
                    }
                    Stats.reset();
                    BuildStats.reset();
                    try(final PieSession session = pie.newSession()) {
                        session.updateAffectedBy(changedResources);
                    } catch(ExecException e) {
                        System.err.print("... failed: " + e.getMessage());
                    }
                    System.err.println();
                    return null;
                });
        }

        System.err.println("MEASUREMENTS");

        // MEASUREMENTS
        try(final PrintWriter log = new PrintWriter(
            new BufferedWriter(new FileWriter(gitRepoPath.resolve("../bench.csv").toFile()))); final PrintWriter log2 = new PrintWriter(
            new BufferedWriter(new FileWriter(gitRepoPath.resolve("../bench2.csv").toFile())))) {
            log.println(CSV_WITH_STATS_HEADER);
            log2.println(CSV_WITH_STATS_HEADER2);
            for(int iteration = 1; iteration <= arguments.benchmarkIterations; iteration++) {
                System.err.println("ITERATION " + iteration + "/" + arguments.benchmarkIterations);
                // Reset repo
                resetRepository(gitRepoFile);

                try(final Pie pie = pieBuilder.build()) {
                    System.err.println("CLEAN BUILD (" + arguments.startCommitHash + ")");
                    final long pieTime;
                    // CLEAN BUILD (topdown)
                    {
                        runPreprocessScript(arguments.preprocessScript, projectLocation.toFile(), spoofax);
                        Stats.reset();
                        BuildStats.reset();

                        forceGc();
                        final long startTime = System.nanoTime();
                        try(final PieSession session = pie.newSession()) {
                            session.require(compileTask);
                        }
                        final long buildTime = System.nanoTime();

                        log.print("\"" + arguments.startCommitHash + "\",");
                        log2.print("\"" + arguments.startCommitHash + "\",");
                        log.print(BuildStats.executedFrontTasks + ",");
                        log2.print(BuildStats.executedFrontTasks + ",");
                        pieTime = buildTime - startTime;
                        log.print(pieTime + ",");
                        log2.print(pieTime + ",");
                    }

                    // MEMORY CHECK
                    {
                        forceGc();
                        log.print(getAllocatedMemory() + ",");
                        log2.print(getAllocatedMemory() + ",");
                    }

                    // JAVA COMPILATION
                    {
                        String[] cmdarray =
                            javacArguments(arguments, BuildStats.generatedJavaFiles).toArray(new String[0]);

                        final long startTime = System.nanoTime();
                        Runtime.getRuntime().exec(cmdarray, null, gitRepoFile).waitFor();
                        final long buildTime = System.nanoTime();

                        BuildStats.generatedJavaFiles.clear();
                        final long javaTime = buildTime - startTime;
                        log.print(javaTime + ",");
                        log2.print(javaTime + ",");
                        log.print(statsString() + "\n");
                        log2.print(statsString2(pieTime) + "\n");
                    }

                    // INCREMENTAL BUILDS
                    commitWalk(repository, arguments.startCommitHash, arguments.endCommitHash, arguments.skipCommits,
                        (RevCommit lastRev, RevCommit rev) -> {
                            System.err.print("INCREMENTAL BUILD FOR COMMIT " + rev.name());
                            Runtime.getRuntime()
                                .exec(new String[] { "git", "checkout", "--force", rev.name() }, null, gitRepoFile)
                                .waitFor();
                            final ChangesFromDiff changesFromDiff =
                                changesFromDiff(gitRepoPath, git, repository, lastRev, rev);
                            final long executedFrontTasks;
                            final long incrTime;
                            final long allocatedMemory;
                            final long javaTime;
                            final String statsString;

                            // INCREMENTAL BUILD (bottomup)
                            {
                                final Set<ResourceKey> changedResources = changesFromDiff.strategoFileChanges;
                                try {
                                    runPreprocessScript(arguments.preprocessScript, projectLocation.toFile(), spoofax);
                                } catch(IOException e) {
                                    System.err.println("... failed: " + e.getMessage());
                                    return null;
                                }
                                Stats.reset();
                                BuildStats.reset();

                                forceGc();
                                final long startTime = System.nanoTime();
                                try(final PieSession session = pie.newSession()) {
                                    session.updateAffectedBy(changedResources);
                                } catch(ExecException e) {
                                    System.err.println("... failed: " + e.getMessage());
                                    return null;
                                }
                                System.err.println();
                                final long buildTime = System.nanoTime();

                                // sanity check
                                executedFrontTasks = BuildStats.executedFrontTasks;
                                if(changesFromDiff.strategoChangeSize != BuildStats.executedFrontTasks) {
                                    System.err.println("Inconsistency in " + rev.name() + "\n" + "Changeset size was: "
                                        + changesFromDiff.strategoChangeSize + "\n" + "Read files was: "
                                        + BuildStats.executedFrontTasks);
                                }
                                incrTime = buildTime - startTime;
                            }

                            // MEMORY CHECK
                            {
                                forceGc();
                                allocatedMemory = getAllocatedMemory();
                            }

                            // JAVA COMPILATION
                            {
                                BuildStats.generatedJavaFiles.addAll(changesFromDiff.javaFileChanges);
                                String[] cmdarray =
                                    javacArguments(arguments, BuildStats.generatedJavaFiles).toArray(new String[0]);

                                final long startTime = System.nanoTime();
                                Runtime.getRuntime().exec(cmdarray, null, gitRepoFile).waitFor();
                                final long buildTime = System.nanoTime();

                                javaTime = buildTime - startTime;
                                statsString = statsString();
                            }

                            log.print("\"" + rev.name() + "\",");
                            log.print(executedFrontTasks + ",");
                            log.print(incrTime + ",");
                            log.print(allocatedMemory + ",");
                            log.print(javaTime + ",");
                            log.print(statsString + "\n");
                            log2.print("\"" + rev.name() + "\",");
                            log2.print(executedFrontTasks + ",");
                            log2.print(incrTime + ",");
                            log2.print(allocatedMemory + ",");
                            log2.print(javaTime + ",");
                            log2.print(statsString2(incrTime) + "\n");

                            return null;
                        });
                }
            }
        }
    }

    private static ArrayList<String> javacArguments(StrategoArguments arguments, Set<String> generatedJavaFiles) {
        ArrayList<String> args = new ArrayList<>(generatedJavaFiles.size() + 10);
        args.add("javac");
        args.add("-source");
        args.add("5");
        args.add("-target");
        args.add("5");
        args.add("-nowarn");
        args.add("-cp");
        args.add(arguments.classPath);
        args.add("-d");
        args.add(arguments.javaOutputDir.toString());
        args.addAll(generatedJavaFiles);
        return args;
    }

    private static long getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private static void resetRepository(File gitRepoFile) throws InterruptedException, IOException {
        Runtime.getRuntime().exec(GIT_RESET_HEAD, null, gitRepoFile).waitFor();
        Runtime.getRuntime().exec(GIT_CHECKOUT_HEAD, null, gitRepoFile).waitFor();
        Runtime.getRuntime().exec(GIT_CHECKOUT_START_COMMIT, null, gitRepoFile).waitFor();
    }

    @SuppressWarnings("UnusedAssignment") private static void forceGc() {
        @Nullable Object obj = new Object();
        final WeakReference ref = new WeakReference<>(obj);
        obj = null;
        while(ref.get() != null) {
            System.gc();
        }
        System.runFinalization();
    }

    private static String statsString() {
        // @formatter:off
        return Stats.requires
            + "," + Stats.executions
            + "," + Stats.fileReqs
            + "," + Stats.fileGens
            + "," + Stats.callReqs
            + "," + BuildStats.csv()
            ;
        // @formatter:on
    }

    private static String statsString2(long totalTime) {
        // @formatter:off
        return Stats.requires
            + "," + Stats.executions
            + "," + Stats.fileReqs
            + "," + Stats.fileGens
            + "," + Stats.callReqs
            + "," + BuildStats.csv2(totalTime)
            ;
        // @formatter:on
    }

    private static List<ResourceKey> runPreprocessScript(@Nullable String preprocessScript, File projectLocation,
        @Nullable Spoofax spoofax) throws InterruptedException, IOException {
        return runPreprocessScript(preprocessScript, false, projectLocation, spoofax);
    }

    private static List<ResourceKey> runPreprocessScript(@Nullable String preprocessScript,
        @SuppressWarnings("SameParameterValue") boolean verbose, File projectLocation, @Nullable Spoofax spoofax)
        throws InterruptedException, IOException {
        if(preprocessScript != null) {
            if(preprocessScript.length() == 0)
                throw new IllegalArgumentException("Empty command");

            final StringTokenizer st = new StringTokenizer(preprocessScript);
            final String[] cmdarray = new String[st.countTokens()];
            for(int i = 0; st.hasMoreTokens(); i++)
                cmdarray[i] = st.nextToken();
            final ProcessBuilder processBuilder =
                new ProcessBuilder(cmdarray).directory(projectLocation).redirectErrorStream(true);
            if(verbose) {
                processBuilder.inheritIO();
            }
            final Process process = processBuilder.start();
            final int exitCode = process.waitFor();
            if(exitCode != 0) {
                throw new IOException("\nPrepreprocess script failed with exit code: " + exitCode);
            }
            if(spoofax != null) {
                final FileObject projectLocationFO = spoofax.resourceService.resolve(projectLocation);
                discoverDialects(spoofax, projectLocationFO, projectLocationFO);
            }
            final ArrayList<ResourceKey> result = new ArrayList<>();
            try(Scanner s = new Scanner(process.getInputStream())) {
                s.forEachRemaining(string -> {
                    result.add(new FSPath(string));
                });
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static void commitWalk(Repository repository, String startCommitHash, String endCommitHash,
        List<String> skipCommits, CheckedFunction2<RevCommit, RevCommit, Void, Exception> consumer) throws Exception {
        commitWalk(repository, startCommitHash, endCommitHash, skipCommits, Integer.MAX_VALUE, consumer);
    }

    private static void commitWalk(Repository repository, String startCommitHash, String endCommitHash,
        List<String> skipCommits, int maxCommits, CheckedFunction2<RevCommit, RevCommit, Void, Exception> consumer)
        throws Exception {
        try(RevWalk walk = new RevWalk(repository)) {
            final RevCommit startRev = walk.parseCommit(repository.resolve(startCommitHash));
            final Deque<RevCommit> commits = new ArrayDeque<>();
            walk.markStart(walk.parseCommit(repository.resolve(endCommitHash)));
            for(RevCommit rev : walk) {
                if(rev.equals(startRev)) {
                    break;
                }
                commits.push(rev);
            }
            RevCommit lastRev = startRev;
            commitloop:
            for(RevCommit rev : commits) {
                for(String skipCommit : skipCommits) {
                    if(rev.name().startsWith(skipCommit)) {
                        continue commitloop;
                    }
                }
                if(maxCommits <= 0) {
                    break;
                }
                consumer.apply(lastRev, rev);
                lastRev = rev;
                maxCommits--;
            }

            walk.dispose();
        }
    }

    private static ChangesFromDiff changesFromDiff(Path gitRepoPath, Git git, Repository repository, RevCommit lastRev,
        RevCommit rev) throws MetaborgException, IOException, GitAPIException {
        final List<DiffEntry> diffEntries;
        try(final ObjectReader reader = repository.newObjectReader()) {
            diffEntries = git.diff().setShowNameAndStatusOnly(true)
                .setOldTree(new CanonicalTreeParser(null, reader, lastRev.getTree().getId()))
                .setNewTree(new CanonicalTreeParser(null, reader, rev.getTree().getId())).call();
        }
        if(diffEntries.size() > 100) {
            System.err.println("From " + lastRev.name() + " to " + rev.name() + " there's a large diff. ");
        }
        final Set<FSPath> changedStrategoFiles = new HashSet<>();
        final List<String> changedJavaFiles = new ArrayList<>();
        for(DiffEntry diffEntry : diffEntries) {
            final String oldPath = diffEntry.getOldPath();
            final String newPath = diffEntry.getNewPath();
            switch(diffEntry.getChangeType()) {
                case DELETE:
                    if(oldPath.endsWith(".str")) {
                        changedStrategoFiles.add(new FSPath(gitRepoPath.resolve(oldPath)));
                    }
                    break;
                case ADD:
                case MODIFY:
                case COPY:
                    if(newPath.endsWith(".str")) {
                        changedStrategoFiles.add(new FSPath(gitRepoPath.resolve(newPath)));
                    } else if(newPath.endsWith(".java")) {
                        changedJavaFiles.add(gitRepoPath.resolve(newPath).toString());
                    }
                    break;
                case RENAME:
                    if(oldPath.endsWith(".str")) {
                        changedStrategoFiles.add(new FSPath(gitRepoPath.resolve(oldPath)));
                        changedStrategoFiles.add(new FSPath(gitRepoPath.resolve(newPath)));
                    } else if(newPath.endsWith(".java")) {
                        changedJavaFiles.add(gitRepoPath.resolve(newPath).toString());
                    }
                    break;
                default:
                    throw new MetaborgException("Unknown ChangeType: " + diffEntry.getChangeType());
            }
        }
        return new ChangesFromDiff(changedStrategoFiles, changedJavaFiles);
    }

    private static void bench(SpoofaxArguments args, Spoofax spoofax) throws Exception {
        // Initialize stratego arguments with spoofax arguments (adding to the mvn package step to the preprocess script)
        StrategoArguments arguments = StrategoArguments.from(args);
        // Find the rest of the arguments in the metaborg.yaml file
        extractFromYaml(arguments, spoofax);
        bench(arguments, spoofax);
    }

    private static void extractFromYaml(StrategoArguments arguments, Spoofax spoofax)
        throws IOException, ConfigurationException, MetaborgException {
        SpoofaxMeta spoofaxMeta = new SpoofaxMeta(spoofax);
        IResourceService resourceService = spoofax.resourceService;
        ILanguagePathService languagePathService = spoofax.languagePathService;
        ISpoofaxLanguageSpecConfigService languageSpecConfigService = spoofaxMeta.languageSpecConfigService;
        final FileObject projectDir = resourceService.resolve(arguments.projectDir.toFile());
        SpoofaxLangSpecCommonPaths paths = new SpoofaxLangSpecCommonPaths(projectDir);

        final @Nullable IProject project = spoofax.projectService.get(projectDir);
        final @Nullable ISpoofaxLanguageSpecConfig specConfig = languageSpecConfigService.get(projectDir).config();
        if(specConfig == null || project == null) {
            throw new IllegalArgumentException(
                "Could not find Spoofax language configuration at " + arguments.projectDir);
        }
        final String languageName = specConfig.name();
        arguments.strategoMainPath = Objects.requireNonNull(resourceService
            .localPath(paths.findStrMainFile(languagePathService.sourcePaths(project, languageName), languageName)))
            .toPath();
        arguments.packageName = specConfig.packageName();

        for(FileObject fileObject : languagePathService.sourceAndIncludePaths(project, languageName)) {
            arguments.includeDirs.add(Objects.requireNonNull(resourceService.localPath(fileObject)).toString());
        }

        arguments.strOutputPath =
            Objects.requireNonNull(resourceService.localPath(paths.strSrcGenJavaTransDir(languageName))).toPath();

        arguments.javaOutputDir =
            Objects.requireNonNull(resourceService.localPath(paths.strTargetClassesTransDir(languageName))).toPath();
    }

    private static void benchIncremental() throws Exception {
        //@formatter:off
        final List<String> subDirs =
            Arrays.asList(
                "src-gen",
                "trans",
                "",
                "src-gen/nabl2/collection",
                "target/replicate/strj-includes");
        //@formatter:on
        bench("languageProject", "incremental", "incremental", subDirs);
    }

    private static void benchTiger() throws Exception {
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
        bench("tiger", "tiger", "org.metaborg.lang.tiger", subDirs);
    }

    private static void bench(String directory, String mainFileName, String packageName, Collection<String> subDirs)
        throws Exception {
        final Spoofax spoofax =
            new Spoofax(new NullEditorConfigBasedProject(), new StrIncrModule(), new GuiceTaskDefsModule());

        // Load the Stratego language project to use for parsing Stratego code in stratego.build.StrIncrFront
        spoofax.languageDiscoveryService
            .languageFromDirectory(spoofax.resourceService.resolve(Main.class.getResource("/stratego.lang/").toURI()));

        final GuiceTaskDefs guiceTaskDefs = spoofax.injector.getInstance(GuiceTaskDefs.class);

        // We need to create the PIE runtime, using a PieBuilderImpl.
        final PieBuilder pieBuilder = new PieBuilderImpl();

        pieBuilder.withTaskDefs(guiceTaskDefs);
        // For example purposes, we use verbose logging which will output to stdout.
        pieBuilder.withLogger(StreamLogger.verbose());

        // We always need to do a topDown build first as a clean build.
        Path gitRepoPath = Paths.get(TMP_DIR, "stratego.build.bench");
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
            final File include = projectLocation.resolve(subDir).normalize().toFile();
            includeDirs.add(include);
            discoverDialects(spoofax, spoofax.resourceService.resolve(include),
                spoofax.resourceService.resolve(projectLocation.toFile()));
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

        final org.metaborg.util.cmd.Arguments extraArgs = new org.metaborg.util.cmd.Arguments();
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
                session.require(compileTask);
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

                    final ChangesFromDiff changesFromDiff = changesFromDiff(gitRepoPath, git, repository, lastRev, rev);
                    System.out.println(
                        "Changeset size between " + lastRev + " and " + rev + ": " + changesFromDiff.strategoFileChanges
                            .size());
                    startTime = System.nanoTime();
                    try(final PieSession session = pie.newSession()) {
                        session.updateAffectedBy(changesFromDiff.strategoFileChanges);
                    }
                    buildTime = System.nanoTime();
                    System.out.println("\"From " + lastRev + " to " + rev + " took\", " + (buildTime - startTime));

                    lastRev = rev;
                }

                walk.dispose();
            }
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

    public static void discoverDialects(Spoofax spoofax, FileObject searchLocation, FileObject projectLocation)
        throws FileSystemException {
        final Set<FileObject> lastTableFiles = Main.lastTableFiles.getOrDefault(searchLocation, Collections.emptySet());
        final HashSet<FileObject> tableFiles =
            Sets.newHashSet(ResourceUtils.find(searchLocation, new SpecialIgnoresSelector()));
        spoofax.dialectProcessor.update(projectLocation,
            ResourceUtils.toChanges(Sets.difference(tableFiles, lastTableFiles), ResourceChangeKind.Create));
        spoofax.dialectProcessor.update(projectLocation,
            ResourceUtils.toChanges(Sets.difference(lastTableFiles, tableFiles), ResourceChangeKind.Delete));
        spoofax.dialectProcessor.update(projectLocation,
            ResourceUtils.toChanges(Sets.intersection(lastTableFiles, tableFiles), ResourceChangeKind.Modify));
        Main.lastTableFiles.put(searchLocation, tableFiles);
    }

    private static class ChangesFromDiff {
        final Set<ResourceKey> strategoFileChanges;
        final int strategoChangeSize;
        final List<String> javaFileChanges;

        private ChangesFromDiff(Set<FSPath> strategoFileChanges, List<String> javaFileChanges) {
            this.strategoChangeSize = strategoFileChanges.size();
            Set<ResourceKey> changesWithDirs = new HashSet<>(strategoFileChanges);
            for(FSPath resourceKey : strategoFileChanges) {
                changesWithDirs.add(resourceKey.getParent());
            }
            this.strategoFileChanges = changesWithDirs;
            this.javaFileChanges = javaFileChanges;
        }
    }
}
