package com.darkona.feathersoffatigue;

import dev.darkona.jdlib.testkit.SourceContracts;
import org.junit.jupiter.api.Test;

class SourceContractTest {

    @Test
    void noCommentsInThePublicRepository() {
        SourceContracts.of("src/main/java", "src/test/java", "src/gametest/java").assertNoComments();
    }

    @Test
    void theApiHasOnlyJavadoc() {
        SourceContracts.of("src/api/java").assertOnlyJavadoc();
    }
}
