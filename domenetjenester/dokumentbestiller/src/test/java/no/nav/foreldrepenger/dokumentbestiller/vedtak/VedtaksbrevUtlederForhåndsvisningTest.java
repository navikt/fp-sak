package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.exception.TekniskException;

@ExtendWith(MockitoExtension.class)
class VedtaksbrevUtlederForhåndsvisningTest {

    @Mock
    private Behandling behandling;

    // ES
    @Test
    void skal_velge_positivt_ES() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.INNVILGET, null, false, null)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_INNVILGELSE);
    }

    @Test
    void skal_velge_negativt_ES() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.AVSLÅTT, null, false, null)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_AVSLAG);
    }

    @Test
    void skal_velge_positivt_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        List<KonsekvensForYtelsen> konsekvensForYtelsenList = List.of();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.FORELDREPENGER_ENDRET,
            konsekvensForYtelsenList, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_opphør_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.OPPHØR, null, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_OPPHØR);
    }

    @Test
    void skal_velge_avslag_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        List<KonsekvensForYtelsen> konsekvensForYtelsenList = List.of();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.AVSLÅTT, konsekvensForYtelsenList, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_AVSLAG);
    }

    @Test
    void skal_velge_annullert_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.FORELDREPENGER_SENERE, null, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_ANNULLERT);
    }

    @Test
    void skal_velge_innvilget_FP_om_foreldrepenger_endret_og_ikke_omfordeling() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.FORELDREPENGER_ENDRET, List.of(
            KonsekvensForYtelsen.ENDRING_I_BEREGNING), false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_uendret_utfall() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, null, true, null)).isEqualTo(DokumentMalType.INGEN_ENDRING);
    }

    // Klage
    @Test
    void skal_velge_klage_avvist() {
        when(behandling.getType()).thenReturn(BehandlingType.KLAGE);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, null, null, false, KlageVurdering.AVVIS_KLAGE)).isEqualTo(DokumentMalType.KLAGE_AVVIST);
    }

    @Test
    void skal_velge_klage_medhold() {
        when(behandling.getType()).thenReturn(BehandlingType.KLAGE);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, null, null, false, KlageVurdering.MEDHOLD_I_KLAGE)).isEqualTo(DokumentMalType.KLAGE_OMGJORT);
    }

    // SVP
    @Test
    void skal_velge_opphør_Svp() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.OPPHØR, null, false, null)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_OPPHØR);
    }

    @Test
    void skal_velge_positivt_SVP() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.INNVILGET, null, false, null)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_avslag_Svp() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.AVSLÅTT, null, false, null)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_AVSLAG);
    }

    // Unntakk
    @Test
    void exception_om_vedtak_resultat_type_ikke_støttet() {
        when(behandling.getType()).thenReturn(BehandlingType.INNSYN);
        List<KonsekvensForYtelsen> konsekvensForYtelsenList = List.of();
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, konsekvensForYtelsenList, false, null));
        assertThat(ex.getKode()).contains("FP-666915");
    }

    @Test
    void skal_returnere_exception_om_ytelse_type_mangler_negativ_utfall() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.UDEFINERT);
        List<KonsekvensForYtelsen> konsekvensForYtelsenList = List.of();
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.AVSLÅTT, konsekvensForYtelsenList, false, null));
        assertThat(ex.getKode()).contains("FP-666917");
    }

    @Test
    void skal_returnere_exception_om_ytelse_type_mangler_positiv_utfall() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.UDEFINERT);
        List<KonsekvensForYtelsen> konsekvensForYtelsenList = List.of();
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, BehandlingResultatType.INNVILGET, konsekvensForYtelsenList, false, null));
        assertThat(ex.getKode()).contains("FP-666918");
    }

    @Test
    void exception_ved_manglende_klage_vurdering() {
        when(behandling.getType()).thenReturn(BehandlingType.KLAGE);
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, null, null, false, null));
        assertThat(ex.getKode()).contains("FP-666920");
    }

    @Test
    void exception_ved_ikke_støttet_klage_vurdering() {
        when(behandling.getType()).thenReturn(BehandlingType.KLAGE);
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak(behandling, null, null, false, KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE));
        assertThat(ex.getKode()).contains("FP-666919");
    }
}
