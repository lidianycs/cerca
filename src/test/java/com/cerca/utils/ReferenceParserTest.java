package com.cerca.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceParserTest {
    @ParameterizedTest
    @MethodSource("referenceProvider")
    void testAuthorExtraction(String input, String expectedAuthors) {
        ReferenceParser.ParsedData result = ReferenceParser.parse(input);

        assertEquals(expectedAuthors, result.authors);
    }

    static Stream<Arguments> referenceProvider() {
        return Stream.of(
                Arguments.of(
                        "[80] N. Rytilä, “Addressing end-to-end testing challenges with cypress,” 2025.",
                        "Rytilä, N."
                ),
                Arguments.of(
                        "[100] T. Zhang, Y. Liu, J. Gao, L. P. Gao, and J. Cheng, “Deep learning paper,” Ieee Software, vol. 37, no. 4, pp. 67–74, 2020.",
                        "Zhang, T., Liu, Y., Gao, J., Gao, L. P., Cheng, J."
                ),
                Arguments.of(
                        "[50] Martina Yvonne Feilzer. Doing mixed methods research pragmatically: Implications for the rediscovery of pragmatism as a research paradigm. Journal of mixed methods research, 4(1):6–16, 2010.",
                        "Feilzer, Martina Yvonne"
                ),
                Arguments.of(
                        "[20] B. Yetiştiren, I. Özsoy, M. Ayerdem, and E. Tüzün, “Evaluating the code quality of ai-assisted code generation tools: An empirical study on github copilot, amazon codewhisperer, and chatgpt,” arXiv preprint arXiv:2304.10778, 2023. [Online]. Available: https://arxiv.org/abs/2304.10778",
                        "Yetiştiren, B., Özsoy, I., Ayerdem, M., Tüzün, E."
                )
        );
    }
}
