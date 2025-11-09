package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class DokumentBestillingTest {

    private static final Saksnummer SAKSNUMMER = new Saksnummer("123456789");

    @Test
    void positiv() {
        var bestilling = new DokumentBestilling(UUID.randomUUID(), SAKSNUMMER, DokumentMalType.FORELDREPENGER_INNVILGELSE, null, null, null, UUID.randomUUID());
        assertThat(bestilling.behandlingUuid()).isNotNull();
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.dokumentMal()).isNotNull();
        assertThat(bestilling.journalførSom()).isNull();
        assertThat(bestilling.fritekst()).isNull();
        assertThat(bestilling.revurderingÅrsak()).isNull();
    }

    @Test
    void positiv_builder() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        var bestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal)
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.journalførSom()).isNull();
        assertThat(bestilling.fritekst()).isNull();
        assertThat(bestilling.revurderingÅrsak()).isNull();
    }

    @Test
    void exception_builder_mangler_behandling_uuid() {
        var bestillingBuilder = DokumentBestilling.builder();

        var ex = assertThrows(NullPointerException.class, bestillingBuilder::build);
        assertThat(ex.getMessage()).contains("Behandling UUID må være satt");
    }

    @Test
    void exception_builder_mangler_dokument_mal() {
        var behandlingUuid = UUID.randomUUID();

        var bestillingBuilder = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid).medSaksnummer(SAKSNUMMER);

        var ex = assertThrows(NullPointerException.class, bestillingBuilder::build);
        assertThat(ex.getMessage()).contains("Dokument mal må være satt");
    }

    @Test
    void exception_builder_fritekst_uten_journalfør_som_satt() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.FRITEKSTBREV;

        var bestillingBuilder = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal);

        var ex = assertThrows(NullPointerException.class, bestillingBuilder::build);
        assertThat(ex.getMessage()).contains("journalførSom");
    }

    @Test
    void ok_builder_fritekst_med_journalfør_som() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.FRITEKSTBREV;
        var journalførSom = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        var bestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal)
            .medJournalførSom(journalførSom)
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.journalførSom()).isNotNull().isEqualTo(journalførSom);
        assertThat(bestilling.fritekst()).isNull();
        assertThat(bestilling.revurderingÅrsak()).isNull();
    }

    @Test
    void exception_builder_friteksthtml_uten_journalfør_som_satt() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.VEDTAKSBREV_FRITEKST_HTML;

        var bestillingBuilder = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal);

        var ex = assertThrows(NullPointerException.class, bestillingBuilder::build);
        assertThat(ex.getMessage()).contains("journalførSom");
    }

    @Test
    void ok_builder_friteksthtml_med_journalfør_som() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.VEDTAKSBREV_FRITEKST_HTML;
        var journalførSom = DokumentMalType.FORELDREPENGER_INNVILGELSE;

        var bestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal)
            .medJournalførSom(journalførSom)
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.journalførSom()).isNotNull().isEqualTo(journalførSom);
        assertThat(bestilling.fritekst()).isNull();
        assertThat(bestilling.revurderingÅrsak()).isNull();
    }

    @Test
    void exception_builder_innhent_opplysninger_mangler_fritekst() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.INNHENTE_OPPLYSNINGER;

        var bestillingBuilder = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal);

        var ex = assertThrows(NullPointerException.class, bestillingBuilder::build);
        assertThat(ex.getMessage()).contains("Fritekst må være satt");
    }

    @Test
    void ok_builder_innhent_opplysninger_med_fritekst() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.INNHENTE_OPPLYSNINGER;

        var bestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal)
            .medFritekst("test")
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.fritekst()).isNotNull();
        assertThat(bestilling.journalførSom()).isNull();
        assertThat(bestilling.revurderingÅrsak()).isNull();
    }

    @Test
    void exception_builder_varsel_mangler_årsak() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.VARSEL_OM_REVURDERING;

        var bestillingBuilder = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal);

        var ex = assertThrows(NullPointerException.class, bestillingBuilder::build);
        assertThat(ex.getMessage()).contains("Revurdering årsak må være satt.");
    }

    @Test
    void exception_builder_varsel_med_årsak_annet_mangler_fritekst() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.VARSEL_OM_REVURDERING;

        var bestillingBuilder = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal)
            .medRevurderingÅrsak(RevurderingVarslingÅrsak.ANNET);

        var ex = assertThrows(NullPointerException.class, bestillingBuilder::build);
        assertThat(ex.getMessage()).contains("Fritekst må være satt for revurdering årsak Annet");
    }

    @Test
    void ok_builder_varsel_med_årsak_annet_med_fritekst() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.VARSEL_OM_REVURDERING;
        var revurderingÅrsak = RevurderingVarslingÅrsak.ANNET;

        var bestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal)
            .medRevurderingÅrsak(revurderingÅrsak)
            .medFritekst("test")
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.revurderingÅrsak()).isNotNull().isEqualTo(revurderingÅrsak);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.fritekst()).isNotNull();
        assertThat(bestilling.journalførSom()).isNull();
    }

    @Test
    void ok_builder_varsel_med_årsak() {
        var behandlingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.VARSEL_OM_REVURDERING;
        var revurderingÅrsak = RevurderingVarslingÅrsak.OPPTJENING_IKKE_OPPFYLT;

        var bestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandlingUuid)
            .medSaksnummer(SAKSNUMMER)
            .medDokumentMal(dokumentMal)
            .medRevurderingÅrsak(revurderingÅrsak)
            .build();

        assertThat(bestilling.behandlingUuid()).isNotNull().isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isNotNull().isEqualTo(dokumentMal);
        assertThat(bestilling.revurderingÅrsak()).isNotNull().isEqualTo(revurderingÅrsak);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.fritekst()).isNull();
        assertThat(bestilling.journalførSom()).isNull();
    }
}
