package com.bulletdev.jhasher.core.identify;

import java.util.List;

public record HashContext(
        String hashType,
        List<String> commonSources,
        String strengthRating,
        String strengthNote,
        List<String> attackNotes
) {}
