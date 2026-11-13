package br.com.rotasegura.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUrisTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/api/areas/1          | /api/areas/1",
            "/api/areas/a%20b      | /api/areas/a%20b",
            "/a b/api/areas        | /a%20b/api/areas",
            "/[x]/api/areas        | /%5Bx%5D/api/areas",
            "/a{b}/api/areas       | /a%7Bb%7D/api/areas",
            "/a\"b/api/areas       | /a%22b/api/areas",
            "/a%zz/api/areas       | /a%25zz/api/areas",
            "/várzea/api/areas     | /várzea/api/areas",
            // Sem "/" no começo e com ":": nem codificando vira URI, então cai no "/"
            ":x/api/areas          | /",
            "1:x/api/areas         | /",
            "a b:c/api/areas       | /"
    })
    void neverThrowsAndEncodesOnlyWhatIsInvalid(String requestUri, String expected) {
        assertThat(RequestUris.toUri(requestUri)).hasToString(expected);
    }
}
