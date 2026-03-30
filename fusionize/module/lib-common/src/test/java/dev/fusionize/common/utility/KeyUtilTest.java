package dev.fusionize.common.utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyUtilTest {

    @Test
    void shouldGenerateNumericKey() {
        // setup
        long seedZero = 0;
        long seedFive = 5;

        // expectation
        var resultZero = KeyUtil.getNumericKey(seedZero);
        var resultFive = KeyUtil.getNumericKey(seedFive);

        // validation
        assertThat(resultZero).isEqualTo("0");
        assertThat(resultFive).isEqualTo("5");
    }

    @Test
    void shouldGenerateAlphabeticalKey() {
        // setup
        long seedZero = 0;
        long seedOne = 1;

        // expectation
        var resultA = KeyUtil.getAlphabeticalKey(seedZero);
        var resultB = KeyUtil.getAlphabeticalKey(seedOne);

        // validation
        assertThat(resultA).isEqualTo("A");
        assertThat(resultB).isEqualTo("B");
    }

    @Test
    void shouldGenerateAlphaNumericKey_numeric() {
        // setup
        long seed = 0; // 0 % 36 = 0, which is < 10

        // expectation
        var result = KeyUtil.getAlphaNumericKey(seed);

        // validation
        assertThat(result).isEqualTo("0");
    }

    @Test
    void shouldGenerateAlphaNumericKey_alphabetical() {
        // setup
        long seed = 10; // 10 % 36 = 10, which is >= 10

        // expectation
        var result = KeyUtil.getAlphaNumericKey(seed);

        // validation
        assertThat(result).matches("[A-Z]");
    }

    @Test
    void shouldGenerateRandomNumericKey_withCorrectLength() {
        // setup
        int length = 5;

        // expectation
        var result = KeyUtil.getRandomNumericKey(length);

        // validation
        assertThat(result).hasSize(5);
        assertThat(result).matches("[0-9]+");
    }

    @Test
    void shouldGenerateRandomAlphabeticalKey_withCorrectLength() {
        // setup
        int length = 8;

        // expectation
        var result = KeyUtil.getRandomAlphabeticalKey(length);

        // validation
        assertThat(result).hasSize(8);
        assertThat(result).matches("[A-Z]+");
    }

    @Test
    void shouldGenerateRandomAlphaNumericKey_withCorrectLength() {
        // setup
        int length = 10;

        // expectation
        var result = KeyUtil.getRandomAlphaNumericKey(length);

        // validation
        assertThat(result).hasSize(10);
        assertThat(result).matches("[A-Z0-9]+");
    }

    @Test
    void shouldGenerateHash() {
        // setup
        var input = "test";

        // expectation
        var result = KeyUtil.getHash(input);

        // validation
        assertThat(result).isNotNull();
        assertThat(result).hasSize(64);
    }

    @Test
    void shouldGenerateConsistentHash() {
        // setup
        var input = "same";

        // expectation
        var result1 = KeyUtil.getHash(input);
        var result2 = KeyUtil.getHash(input);

        // validation
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void shouldGenerateRandomHash() {
        // setup
        // no setup needed for random hash

        // expectation
        var result = KeyUtil.getRandomHash();

        // validation
        assertThat(result).isNotNull();
        assertThat(result).hasSize(64);
    }

    @Test
    void shouldGenerateLongHash() {
        // setup
        var input = "test";
        int length = 2;

        // expectation
        var result = KeyUtil.getLongHash(input, length);

        // validation
        assertThat(result.length()).isGreaterThan(64);
    }

    @Test
    void shouldGenerateUUID() {
        // setup
        // no setup needed

        // expectation
        var result = KeyUtil.getUUID();

        // validation
        assertThat(result).contains("-");
        assertThat(result).hasSize(36);
    }

    @Test
    void shouldGenerateFlatUUID() {
        // setup
        // no setup needed

        // expectation
        var result = KeyUtil.getFlatUUID();

        // validation
        assertThat(result).doesNotContain("-");
        assertThat(result).hasSize(32);
    }

    @Test
    void shouldGenerateTimestampId() {
        // setup
        var prefix = "TEST";

        // expectation
        var result = KeyUtil.getTimestampId(prefix);

        // validation
        assertThat(result).startsWith("TEST");
    }

    @Test
    void shouldHandleDefaultLength() {
        // setup
        int length = 0;

        // expectation
        var result = KeyUtil.getRandomNumericKey(length);

        // validation
        assertThat(result).hasSize(5);
    }
}
