package mb.stratego.build.bench.strj;

import org.apache.commons.vfs2.FileSelectInfo;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;

/**
 * Ignore the Stratego-Sugar table so we don't get a second (active) implementation of StrategoSugar after we loaded
 * the editor project.
 */
public class SpecialIgnoresSelector extends SpoofaxIgnoresSelector {
    @Override public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
        final String baseName = fileInfo.getFile().getName().getBaseName();
        return baseName.endsWith(".tbl") && !baseName.equals("Stratego-Sugar.tbl");
    }
}
