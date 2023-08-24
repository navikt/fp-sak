package no.nav.foreldrepenger.dokumentbestiller.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BestillBrevDtoTest {

    @Test
    void serdes() {
        var behandlingId = 12L;
        var uuid = "5ffbf59b-76d5-4c78-bd27-6c84e0d445a3";
        var dokumentMal = DokumentMalType.FRITEKSTBREV;
        var arsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;

        var brev = new BestillBrevDto(behandlingId, UUID.fromString(uuid), dokumentMal, null, arsak);

        var json = DefaultJsonMapper.toJson(brev);

        var etterRoundtrip = DefaultJsonMapper.fromJson(json, BestillBrevDto.class);

        assertEquals(brev.getBehandlingId(), etterRoundtrip.getBehandlingId());
        assertEquals(behandlingId, etterRoundtrip.getBehandlingId());
        assertEquals(brev.getBrevmalkode(), etterRoundtrip.getBrevmalkode());
        assertEquals(dokumentMal, brev.getBrevmalkode());
        assertEquals(brev.getArsakskode(), etterRoundtrip.getArsakskode());
        assertEquals(arsak, etterRoundtrip.getArsakskode());
        assertEquals(brev.getFritekst(), etterRoundtrip.getFritekst());
        assertNull(etterRoundtrip.getFritekst());
        assertEquals(brev.getBehandlingUuid(), etterRoundtrip.getBehandlingUuid());
        assertEquals(uuid, etterRoundtrip.getBehandlingUuid().toString());
    }

}
