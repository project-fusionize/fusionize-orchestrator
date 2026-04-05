package dev.fusionize.workflow.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextResourceReferenceTest {

    @Test
    void shouldBuildWithAllFields() {
        // setup
        var storage = "s3";
        var referenceKey = "ref-key-1";
        var name = "document.pdf";
        var mime = "application/pdf";
        var size = 1024L;

        // expectation
        var ref = ContextResourceReference.builder()
                .withStorage(storage)
                .withReferenceKey(referenceKey)
                .withName(name)
                .withMime(mime)
                .withSize(size)
                .build();

        // validation
        assertThat(ref.getStorage()).isEqualTo(storage);
        assertThat(ref.getReferenceKey()).isEqualTo(referenceKey);
        assertThat(ref.getName()).isEqualTo(name);
        assertThat(ref.getMime()).isEqualTo(mime);
        assertThat(ref.getSize()).isEqualTo(size);
    }

    @Test
    void shouldReturnToString() {
        // setup
        var ref = ContextResourceReference.builder()
                .withStorage("local")
                .withReferenceKey("key-2")
                .withName("image.png")
                .withMime("image/png")
                .withSize(2048L)
                .build();

        // expectation
        var result = ref.toString();

        // validation
        assertThat(result).contains("local");
        assertThat(result).contains("key-2");
        assertThat(result).contains("image.png");
        assertThat(result).contains("image/png");
        assertThat(result).contains("2048");
        assertThat(result).startsWith("FileStorageReference{");
    }

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var ref = new ContextResourceReference();

        // expectation
        ref.setStorage("azure");
        ref.setReferenceKey("ref-3");
        ref.setName("file.txt");
        ref.setMime("text/plain");
        ref.setSize(512L);

        // validation
        assertThat(ref.getStorage()).isEqualTo("azure");
        assertThat(ref.getReferenceKey()).isEqualTo("ref-3");
        assertThat(ref.getName()).isEqualTo("file.txt");
        assertThat(ref.getMime()).isEqualTo("text/plain");
        assertThat(ref.getSize()).isEqualTo(512L);
    }
}
