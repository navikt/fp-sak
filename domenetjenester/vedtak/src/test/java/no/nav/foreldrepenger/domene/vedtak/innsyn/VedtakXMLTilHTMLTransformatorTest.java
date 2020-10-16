package no.nav.foreldrepenger.domene.vedtak.innsyn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.exception.TekniskException;

public class VedtakXMLTilHTMLTransformatorTest {

    @Test
    public void skal_få_exception_ved_transformasjon_av_helt_feil_XML() {
        assertThrows(TekniskException.class, () -> VedtakXMLTilHTMLTransformator.transformer("tull", 1L));
    }

    @Test
    public void skal_transformere_XML_til_HTML_ugyldig_v2() throws Exception {
        String inputXML = les("/eksempel_vedtakXML_ugyldig_v2.xml");
        assertThrows(TekniskException.class, () -> VedtakXMLTilHTMLTransformator.transformer(inputXML, 1L));
    }

    @Test
    public void skal_transformere_XML_til_HTML_v2() throws Exception {
        String inputXML = les("/eksempel_vedtakXML_engangstønad_validert.xml");
        assertDoesNotThrow(() -> VedtakXMLTilHTMLTransformator.transformer(inputXML, 1L));
    }

    @Test
    public void skal_transformere_XML_til_HTML_v2_fp() throws Exception {
        String inputXML = les("/eksempel-vedtak-fp.xml");
        assertDoesNotThrow(() -> VedtakXMLTilHTMLTransformator.transformer(inputXML, 1L));
    }

    @Test
    public void skal_transformere_XML_til_HTML() throws Exception {
        String forventet = les("/eksempel-vedtakHTML.html");
        String inputXML = les("/eksempel-vedtakXML.xml");
        String resultat = VedtakXMLTilHTMLTransformator.transformer(inputXML, 1L);

        assertThat(cleanWhitespace(resultat)).isEqualTo(cleanWhitespace(forventet));
    }

    @Test
    public void skal_transformere_XML_til_HTML_for_tilfelle_med_verge() throws Exception {
        String forventet = les("/eksempel-vedtak-es-fødsel-verge.html");
        String inputXML = les("/eksempel-vedtak-es-fødsel-verge.xml");
        String resultat = cleanWhitespace(VedtakXMLTilHTMLTransformator.transformer(inputXML, 1L));
        assertThat(resultat).isEqualTo(cleanWhitespace(forventet));
    }

    @Test
    public void skal_transformere_XML_til_HTML_for_tilfelle_med_omsorgsovertakelse_og_familierelasjoner() throws Exception {
        String forventet = les("/eksempel-vedtak-es-omsorgsovertakelse-barn.html");
        String inputXML = les("/eksempel-vedtak-es-omsorgsovertakelse-barn.xml");
        String resultat = cleanWhitespace(VedtakXMLTilHTMLTransformator.transformer(inputXML, 1L));
        assertThat(resultat).isEqualTo(cleanWhitespace(forventet));
    }

    private String les(String filnavn) throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(filnavn);
                Scanner scanner = new Scanner(resource, "UTF-8")) {
            scanner.useDelimiter("\\A");
            if (!scanner.hasNext()) {
                throw new IllegalStateException("Finner ikke fil " + filnavn);
            }
            return scanner.next();
        }
    }

    private String cleanWhitespace(String str) {
        return str.replaceAll("\\r\\n", "") // sammenligner ikke eol
            .replaceAll("\\s", ""); // sammenligner ikke heller annen whitespace (diff i genrering mellom Java8/10)
    }

}
