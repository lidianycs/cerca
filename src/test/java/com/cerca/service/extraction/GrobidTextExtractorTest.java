package com.cerca.service.extraction;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GrobidTextExtractorTest {
    @ParameterizedTest
    @MethodSource("referenceProvider")
    void testAuthorExtraction(String input, String expectedAuthors) {
        GrobidTextExtractor textParser = new GrobidTextExtractor();
        ParsedData result = textParser.parse(input);

        assertEquals(expectedAuthors, result.authors());
    }

    @ParameterizedTest
    @MethodSource("referenceProvider")
    void testTitleExtraction(String input, String ignored, String expectedTitle) {
        GrobidTextExtractor textParser = new GrobidTextExtractor();
        ParsedData result = textParser.parse(input);

        assertEquals(expectedTitle, result.title());
    }

    static Stream<Arguments> referenceProvider() {
        return Stream.of(
                Arguments.of(
                        "[80] N. Rytilä, “Addressing end-to-end testing challenges with cypress,” 2025.",
                        "Rytilä",
                        "Addressing end-to-end testing challenges with cypress"
                ),
                Arguments.of(
                        "[100] T. Zhang, Y. Liu, J. Gao, L. P. Gao, and J. Cheng, “Deep learning paper,” Ieee Software, vol. 37, no. 4, pp. 67–74, 2020.",
                        "Zhang, Liu, Gao, Gao, Cheng",
                        "Deep learning paper"
                ),
                Arguments.of(
                        "[50] Martina Yvonne Feilzer. Doing mixed methods research pragmatically: Implications for the rediscovery of pragmatism as a research paradigm. Journal of mixed methods research, 4(1):6–16, 2010.",
                        "Feilzer",
                        "Doing mixed methods research pragmatically: Implications for the rediscovery of pragmatism as a research paradigm"
                ),
                Arguments.of(
                        "[20] B. Yetiştiren, I. Özsoy, M. Ayerdem, and E. Tüzün, “Evaluating the code quality of ai-assisted code generation tools: An empirical study on github copilot, amazon codewhisperer, and chatgpt,” arXiv preprint arXiv:2304.10778, 2023. [Online]. Available: https://arxiv.org/abs/2304.10778",
                        "Yetiştiren, Özsoy, Ayerdem, Tüzün",
                        "Evaluating the code quality of ai-assisted code generation tools: An empirical study on github copilot, amazon codewhisperer, and chatgpt"
                ),
                Arguments.of(
                        "[39] Charity Majors, Liz Fong-Jones, and George Miranda. Observability engineering. \" O’Reilly Media, Inc.\", 2022.",
                        "Majors, Fong-Jones, Miranda",
                        "Observability engineering"
                ),
                Arguments.of(
                        "[27] R. Cavalcante, L. Oliveira, and A. Santos, “Developers’ perceptions of ai programming assistants: A case study of copilot, chatgpt, and gemini,” in Proceedings of the 2025 International Conference on Software Maintenance and Evolution (ICSME). IEEE, 2025.",
                        "Cavalcante, Oliveira, Santos",
                        "Developers’ perceptions of ai programming assistants: A case study of copilot, chatgpt, and gemini"
                )
        );
    }
}
