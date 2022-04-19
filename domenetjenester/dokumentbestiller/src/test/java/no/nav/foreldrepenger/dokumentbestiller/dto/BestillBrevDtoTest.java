package no.nav.foreldrepenger.dokumentbestiller.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;

class BestillBrevDtoTest {

    @Test
    void deserialize() {
        var dokumentMal = DokumentMalType.FRITEKSTBREV;
        var årsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;

        String json = """
        {
            "brevmalkode":"%s",
            "navn":"sdf",
            "arsakskode":"%s"
        }""".formatted(dokumentMal.getKode(), årsak.getKode());

        BestillBrevDto bean = StandardJsonConfig.fromJson(json, BestillBrevDto.class);
        assertEquals(dokumentMal, bean.getBrevmalkode());
        assertEquals(årsak, bean.getÅrsakskode());
    }

    @Test
    void serialize() {
        var behandlingId = 12L;
        var uuid = "5ffbf59b-76d5-4c78-bd27-6c84e0d445a3";
        var dokumentMal = DokumentMalType.FRITEKSTBREV;
        var arsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;

        var brev = new BestillBrevDto(behandlingId, UUID.fromString(uuid), dokumentMal, null, arsak);
        var etterRoundtrip = StandardJsonConfig.fromJson(StandardJsonConfig.toJson(brev), BestillBrevDto.class);

        assertEquals(brev.getBehandlingId(), etterRoundtrip.getBehandlingId());
        assertEquals(behandlingId, etterRoundtrip.getBehandlingId());
        assertEquals(brev.getBrevmalkode(), etterRoundtrip.getBrevmalkode());
        assertEquals(dokumentMal, brev.getBrevmalkode());
        assertEquals(brev.getÅrsakskode(), etterRoundtrip.getÅrsakskode());
        assertEquals(arsak, etterRoundtrip.getÅrsakskode());
        assertEquals(brev.getFritekst(), etterRoundtrip.getFritekst());
        assertNull(etterRoundtrip.getFritekst());
        assertEquals(brev.getBehandlingUuid(), etterRoundtrip.getBehandlingUuid());
        assertEquals(uuid, etterRoundtrip.getBehandlingUuid().toString());
    }

}
