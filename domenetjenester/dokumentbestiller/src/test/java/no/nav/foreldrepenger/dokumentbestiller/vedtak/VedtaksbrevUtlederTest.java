package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.exception.TekniskException;

@ExtendWith(MockitoExtension.class)
class VedtaksbrevUtlederTest {

    @Mock
    private Behandling behandling;

    // ES
    @Test
    void skal_velge_positivt_ES() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, false, null)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_INNVILGELSE);
    }

    @Test
    void skal_velge_negativt_ES() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.AVSLAG, false, null)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_AVSLAG);
    }

    @Test
    void skal_velge_positivt_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_opphør_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.OPPHØR, VedtakResultatType.AVSLAG, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_OPPHØR);
    }

    @Test
    void skal_velge_avslag_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.AVSLAG, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_AVSLAG);
    }

    @Test
    void skal_velge_annullert_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.FORELDREPENGER_SENERE, VedtakResultatType.INNVILGET, false, null)).isEqualTo(DokumentMalType.FORELDREPENGER_ANNULLERT);
    }

    @Test
    void skal_velge_uendret_utfall() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, true, null)).isEqualTo(DokumentMalType.INGEN_ENDRING);
    }

    // Klage
    @Test
    void skal_velge_klage_avvist() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, null, VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING, false, KlageVurdering.AVVIS_KLAGE)).isEqualTo(DokumentMalType.KLAGE_AVVIST);
    }

    @Test
    void skal_velge_klage_medhold() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, null, VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING, false, KlageVurdering.MEDHOLD_I_KLAGE)).isEqualTo(DokumentMalType.KLAGE_OMGJORT);
    }

    // SVP
    @Test
    void skal_velge_opphør_Svp() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.OPPHØR, VedtakResultatType.AVSLAG, false, null)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_OPPHØR);
    }

    @Test
    void skal_velge_positivt_SVP() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, false, null)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_avslag_Svp() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.AVSLAG, false, null)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_AVSLAG);
    }

    // Unntakk
    @Test
    void exception_om_vedtak_resultat_type_ikke_støttet() {
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, null, VedtakResultatType.VEDTAK_I_ANKEBEHANDLING, false, null));
        assertThat(ex.getKode()).contains("FP-666915");
    }

    @Test
    void skal_returnere_exception_om_ytelse_type_mangler_negativ_utfall() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.UDEFINERT);
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.AVSLAG, false, null));
        assertThat(ex.getKode()).contains("FP-666917");
    }

    @Test
    void skal_returnere_exception_om_ytelse_type_mangler_positiv_utfall() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.UDEFINERT);
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, false, null));
        assertThat(ex.getKode()).contains("FP-666918");
    }

    @Test
    void exception_ved_manglende_klage_vurdering() {
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, null, VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING, false, null));
        assertThat(ex.getKode()).contains("FP-666920");
    }

    @Test
    void exception_ved_ikke_støttet_klage_vurdering() {
        var ex = assertThrows(TekniskException.class, () -> VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, null, VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING, false, KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE));
        assertThat(ex.getKode()).contains("FP-666919");
    }
}
