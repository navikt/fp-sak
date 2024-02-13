package no.nav.foreldrepenger.dokumentbestiller;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;

import static org.assertj.core.api.Assertions.assertThat;

class BrevForhåndsvisningTest {

    @Test
    void positiv() {
        var dokumentMal = DokumentMalType.FRITEKSTBREV;
        var fritekst = "fritekst";
        var tittel = "tittel";
        var revurderingÅrsak = RevurderingVarslingÅrsak.ANNET;
        var brevType = BrevForhandsvisning.BrevType.AUTOMATISK;
        var forhåndsvisning = new BrevForhandsvisning(UUID.randomUUID(), dokumentMal, fritekst, tittel, revurderingÅrsak, brevType);
        assertThat(forhåndsvisning.behandlingUuid()).isNotNull();
        assertThat(forhåndsvisning.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(forhåndsvisning.fritekst()).isNotNull().isEqualTo(fritekst);
        assertThat(forhåndsvisning.tittel()).isNotNull().isEqualTo(tittel);
        assertThat(forhåndsvisning.revurderingÅrsak()).isNotNull().isEqualTo(revurderingÅrsak);
        assertThat(forhåndsvisning.brevType()).isNotNull().isEqualTo(brevType);
    }

    @Test
    void positiv_builder() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        var bestilling = BrevForhandsvisning.builder()
            .medBehandlingUuid(behandlingUuid)
            .medDokumentMal(dokumentMal)
            .medBrevType(BrevForhandsvisning.BrevType.AUTOMATISK)
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.fritekst()).isNull();
        assertThat(bestilling.tittel()).isNull();
        assertThat(bestilling.revurderingÅrsak()).isNull();
    }

    @Test
    void positiv_builder_full() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        var fritekst = "fritekst";
        var tittel = "tittel";
        var revurderingÅrsak = RevurderingVarslingÅrsak.ANNET;
        var brevType = BrevForhandsvisning.BrevType.OVERSTYRT;
        var bestilling = BrevForhandsvisning.builder()
            .medBehandlingUuid(behandlingUuid)
            .medDokumentMal(dokumentMal)
            .medFritekst(fritekst)
            .medTittel(tittel)
            .medRevurderingÅrsak(revurderingÅrsak)
            .medBrevType(brevType)
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.fritekst()).isNotNull().isEqualTo(fritekst);
        assertThat(bestilling.tittel()).isNotNull().isEqualTo(tittel);
        assertThat(bestilling.revurderingÅrsak()).isNotNull().isEqualTo(revurderingÅrsak);
        assertThat(bestilling.brevType()).isNotNull().isEqualTo(brevType);
    }
}
