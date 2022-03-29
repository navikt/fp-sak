package no.nav.foreldrepenger.dokumentbestiller.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;

class BestillBrevDtoTest {

    @Test
    void deserialize() {
        var dokumentMalType = DokumentMalType.FRITEKSTBREV;
        var årsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;

        String json = "{\"brevmalkode\":\"" + dokumentMalType.getKode() + "\",\"navn\":\"sdf\",\"arsakskode\":\"" + årsak.getKode() + "\"}";

        BestillBrevDto bean = StandardJsonConfig.fromJson(json, BestillBrevDto.class);
        assertEquals(dokumentMalType, bean.getBrevmalkode());
        assertEquals(årsak, bean.getÅrsakskode());
    }

    @Test
    void serialize() {
        var behandlingId = 12L;
        var uuid = "5ffbf59b-76d5-4c78-bd27-6c84e0d445a3";
        var dokumentMalType = DokumentMalType.FRITEKSTBREV;
        var arsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;

        String expected = "{\n" + "  \"behandlingId\" : " + behandlingId + ",\n" + "  \"behandlingUuid\" : \"" + uuid +"\",\n"
            + "  \"brevmalkode\" : \"" + dokumentMalType.getKode() + "\",\n" + "  \"arsakskode\" : \"" + arsak.getKode() + "\"\n" + "}";

        var brev = new BestillBrevDto(behandlingId, UUID.fromString(uuid), dokumentMalType, null, arsak);
        var serialized = StandardJsonConfig.toJson(brev);
        assertEquals(expected, serialized);
    }

}
