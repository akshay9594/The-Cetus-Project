package cetus.utils;

import java.util.List;

import cetus.hir.ArrayAccess;

public class DataReuseAnalysis {

    public static void printRefGroup(List<List<ArrayAccess>> refGroup) {
        System.out.println("#### Ref group ####\n");

        for (int i = 0; i < refGroup.size(); i++) {

            List<ArrayAccess> references = refGroup.get(i);
            String referencesStr = "";
            for (int j = 0; j < references.size(); j++) {
                ArrayAccess reference = references.get(j);
                referencesStr += reference;
                if (j != references.size() - 1) {
                    referencesStr += ", ";
                }

            }

            System.out.println("{ " + referencesStr + " }");

        }

        System.out.println("\n#### END RefGroup ####\n");
    }

}
