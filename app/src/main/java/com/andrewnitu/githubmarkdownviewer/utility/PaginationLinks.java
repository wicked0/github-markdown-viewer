package com.andrewnitu.githubmarkdownviewer.utility;

/**
 * Created by Andrew Nitu on 7/30/2017.
 */

public class PaginationLinks {
    private static final String DELIM_LINKS = ","; //$NON-NLS-1$

    private static final String DELIM_LINK_PARAM = ";"; //$NON-NLS-1$

    private static final String META_REL = "rel";
    private static final String META_FIRST = "first";
    private static final String META_PREV = "prev";
    private static final String META_NEXT = "next";
    private static final String META_LAST = "last";

    private String first;
    private String last;
    private String next;
    private String prev;


    public PaginationLinks(String linkHeader) {
        String[] links = linkHeader.split(DELIM_LINKS);
        for (String link : links) {
            String[] segments = link.split(DELIM_LINK_PARAM);
            if (segments.length < 2)
                continue;

            String linkPart = segments[0].trim();
            if (!linkPart.startsWith("<") || !linkPart.endsWith(">")) //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            linkPart = linkPart.substring(1, linkPart.length() - 1);

            for (int i = 1; i < segments.length; i++) {
                String[] rel = segments[i].trim().split("="); //$NON-NLS-1$
                if (rel.length < 2 || !META_REL.equals(rel[0]))
                    continue;

                String relValue = rel[1];
                if (relValue.startsWith("\"") && relValue.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
                    relValue = relValue.substring(1, relValue.length() - 1);

                if (META_FIRST.equals(relValue))
                    first = linkPart;
                else if (META_LAST.equals(relValue))
                    last = linkPart;
                else if (META_NEXT.equals(relValue))
                    next = linkPart;
                else if (META_PREV.equals(relValue))
                    prev = linkPart;
            }
        }
    }

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public String getNext() {
        return next;
    }

    public String getPrev() {
        return prev;
    }
}
