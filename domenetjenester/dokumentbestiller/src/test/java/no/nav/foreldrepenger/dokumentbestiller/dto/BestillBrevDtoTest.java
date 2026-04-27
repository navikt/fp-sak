package no.nav.foreldrepenger.dokumentbestiller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class BestillBrevDtoTest {

    @Test
    void serdes() {
        var uuid = "5ffbf59b-76d5-4c78-bd27-6c84e0d445a3";
        var behandlingUuid = UUID.fromString(uuid);
        var dokumentMal = DokumentMalType.FRITEKSTBREV;
        var årsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;

        var brev = new BestillDokumentDto(behandlingUuid, dokumentMal, null, årsak);

        var json = DefaultJsonMapper.toJson(brev);

        var etterRoundtrip = DefaultJsonMapper.fromJson(json, BestillDokumentDto.class);

        assertThat(brev.brevmalkode()).isEqualTo(etterRoundtrip.brevmalkode());
        assertThat(dokumentMal).isEqualTo(brev.brevmalkode());
        assertThat(brev.årsakskode()).isEqualTo(etterRoundtrip.årsakskode());
        assertThat(årsak).isEqualTo(etterRoundtrip.årsakskode());
        assertThat(brev.fritekst()).isEqualTo(etterRoundtrip.fritekst());
        assertThat(etterRoundtrip.fritekst()).isNull();
        assertThat(brev.behandlingUuid()).isEqualTo(etterRoundtrip.behandlingUuid());
        assertThat(uuid).isEqualTo(etterRoundtrip.behandlingUuid().toString());
    }

}
