package no.nav.foreldrepenger.domene.arbeidsgiver.rest;

import org.junit.Test;

public class EregRestTest {

    @Test
    public void roundtrip_mapping_dto_til_grunnlag_til_dto() {
        // Arrange
        String json ="{\n \"organisasjonsnummer\":\"992323515\",\n" +
            " \"type\":\"Virksomhet\",\n" +
            " \"navn\":{\"redigertnavn\":\"ELKJØP HAUGESUND BERGESENTERET\",\n" +
            " \"navnelinje1\":\"ELKJØP HAUGESUND BERGESENTERET\",\n" +
            " \"navnelinje2\":\"  \",\n" +
            " \"navnelinje3\":\"AVD SVEIO\"\n" +
            "},\n" +
            " \"organisasjonDetaljer\":null,\n" +
            " \"virksomhetDetaljer\":null\n" +
            "}";

        var org = JsonMapper.fromJson(json, OrganisasjonEReg.class);
        var navn = org.getNavn();
        var orgnr = org.getOrganisasjonsnummer();
    }
}
