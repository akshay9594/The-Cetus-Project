package cetus.openai.mappers;

import cetus.hir.ForLoop;
import cetus.hir.Traversable;

public class CodeToIRUtils {

    private static Traversable mapCode(String code) {
        return new ForLoop(null, null, null, null);
    }

}
