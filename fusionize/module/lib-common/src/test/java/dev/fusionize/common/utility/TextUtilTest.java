package dev.fusionize.common.utility;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TextUtilTest {

    /** -------------------- camelCase -------------------- **/

    @Test
    void testCamelCase_basic() {
        assertEquals("userName", TextUtil.camelCase("User Name"));
    }

    @Test
    void testCamelCase_multipleSpaces() {
        assertEquals("multipleSpacesHere", TextUtil.camelCase("  multiple   spaces  here "));
    }

    @Test
    void testCamelCase_alreadyCamel() {
        assertEquals("alreadyCamelCase", TextUtil.camelCase("alreadyCamelCase"));
    }

    /** -------------------- kebabCase -------------------- **/

    @Test
    void testKebabCase_basic() {
        assertEquals("user-name", TextUtil.kebabCase("User Name"));
    }

    @Test
    void testKebabCase_multipleSpaces() {
        assertEquals("multi-space-test", TextUtil.kebabCase("MULTI   space   TEST"));
    }

    @Test
    void testKebabCase_alreadyKebab() {
        assertEquals("already-kebab-case", TextUtil.kebabCase("already-kebab-case"));
    }

    /** -------------------- snakeCase -------------------- **/

    @Test
    void testSnakeCase_basic() {
        assertEquals("user_name", TextUtil.snakeCase("User Name"));
    }

    @Test
    void testSnakeCase_multipleSpaces() {
        assertEquals("multi_space_test", TextUtil.snakeCase("  MULTI   space   TEST  "));
    }

    /** -------------------- fromCamelCase -------------------- **/

    @Test
    void testFromCamelCase_basic() {
        assertEquals("user name", TextUtil.fromCamelCase("userName"));
    }

    @Test
    void testFromCamelCase_multiWord() {
        assertEquals("hello world test", TextUtil.fromCamelCase("helloWorldTest"));
    }

    /** -------------------- fromKebabCase -------------------- **/

    @Test
    void testFromKebabCase_basic() {
        assertEquals("user name", TextUtil.fromKebabCase("user-name"));
    }

    @Test
    void testFromKebabCase_multi() {
        assertEquals("hello world test", TextUtil.fromKebabCase("hello-world-test"));
    }

    /** -------------------- fromSnakeCase -------------------- **/

    @Test
    void testFromSnakeCase_basic() {
        assertEquals("user name", TextUtil.fromSnakeCase("user_name"));
    }

    @Test
    void testFromSnakeCase_multi() {
        assertEquals("hello world test", TextUtil.fromSnakeCase("hello_world_test"));
    }

    /** -------------------- matchesFlexible (TRUE cases) -------------------- **/

    @Test
    void testMatchesFlexible_camel_vs_kebab() {
        assertTrue(TextUtil.matchesFlexible("userName", "user-name"));
    }

    @Test
    void testMatchesFlexible_title_vs_camel() {
        assertTrue(TextUtil.matchesFlexible("User Name", "userName"));
    }

    @Test
    void testMatchesFlexible_kebab_vs_camel() {
        assertTrue(TextUtil.matchesFlexible("user-name", "UserName"));
    }

    @Test
    void testMatchesFlexible_kebab_vs_snake() {
        assertTrue(TextUtil.matchesFlexible("user-name", "user_name"));
    }

    @Test
    void testMatchesFlexible_kebab_vs_snakeCapital() {
        assertTrue(TextUtil.matchesFlexible("user-name", "USER_NAME"));
    }

    @Test
    void testMatchesFlexible_snakeCapital_vs_kebab() {
        assertTrue(TextUtil.matchesFlexible("USER_NAME", "user-name"));
    }

    @Test
    void testMatchesFlexible_spaces_vs_kebab() {
        assertTrue(TextUtil.matchesFlexible("  user   name ", "user-name"));
    }

    @Test
    void testMatchesFlexible_camel_vs_spaced() {
        assertTrue(TextUtil.matchesFlexible("HelloWorldTest", "hello world test"));
    }

    @Test
    void testMatchesFlexible_multiSpaces_vs_snake() {
        assertTrue(TextUtil.matchesFlexible("  user   name  ", "user_name"));
    }

    /** -------------------- matchesFlexible (FALSE cases) -------------------- **/

    @Test
    void testMatchesFlexible_notMatching() {
        assertFalse(TextUtil.matchesFlexible("userName", "user"));
    }

    @Test
    void testMatchesFlexible_partialDoesNotMatch() {
        assertFalse(TextUtil.matchesFlexible("account Id", "account"));
    }

    @Test
    void testMatchesFlexible_completelyDifferent() {
        assertFalse(TextUtil.matchesFlexible("hello world", "bye world"));
    }
}
