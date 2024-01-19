package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ExtendWith(MockitoExtension.class)
class VedtaksbrevUtlederTest {

    @Mock
    private KlageVurderingResultat klageVurderingResultat;

    @Mock
    private Behandling behandling;
    @Mock
    private KlageRepository klageRepository;

    @Test
    void skal_velge_positivt_ES() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, false, klageRepository)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_INNVILGELSE);
    }

    @Test
    void skal_velge_negativt_ES() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.AVSLAG, false, klageRepository)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_AVSLAG);
    }

    @Test
    void skal_velge_positivt_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, false, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_positivt_SVP() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, false, klageRepository)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_opphør_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.OPPHØR, VedtakResultatType.AVSLAG, false, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_OPPHØR);
    }

    @Test
    void skal_velge_avslag_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.AVSLAG, false, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_AVSLAG);
    }

    @Test
    void skal_velge_annullert_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.FORELDREPENGER_SENERE, VedtakResultatType.INNVILGET, false, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_ANNULLERT);
    }

    @Test
    void skal_velge_uendret_utfall() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.INNVILGET, true, klageRepository)).isEqualTo(DokumentMalType.INGEN_ENDRING);
    }
    @Test
    void skal_velge_riktig_klagemal() {
        doReturn(Optional.of(klageVurderingResultat)).when(klageRepository).hentGjeldendeKlageVurderingResultat(behandling);

        doReturn(KlageVurdering.AVVIS_KLAGE).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_AVVIST);

        doReturn(KlageVurdering.MEDHOLD_I_KLAGE).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_OMGJORT);
    }


    @Test
    void skal_velge_opphør_Svp() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.OPPHØR, VedtakResultatType.AVSLAG, false, klageRepository)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_OPPHØR);
    }

    @Test
    void skal_velge_avslag_Svp() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, BehandlingResultatType.IKKE_FASTSATT, VedtakResultatType.AVSLAG, false, klageRepository)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_AVSLAG);
    }

}
