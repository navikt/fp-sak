package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ExtendWith(MockitoExtension.class)
class VedtaksbrevUtlederTest {

    @Mock
    private Behandlingsresultat behandlingsresultatMock;
    @Mock
    private BehandlingVedtak behandlingVedtakMock;
    @Mock
    private KlageVurderingResultat klageVurderingResultat;

    @Mock
    private Behandling behandling;
    @Mock
    private KlageRepository klageRepository;

    @BeforeEach
    public void setup() {
        lenient().doReturn(Vedtaksbrev.AUTOMATISK).when(behandlingsresultatMock).getVedtaksbrev();
        lenient().doReturn(false).when(behandlingVedtakMock).isBeslutningsvedtak();
        lenient().doReturn(VedtakResultatType.INNVILGET).when(behandlingVedtakMock).getVedtakResultatType();
        lenient().doReturn(FagsakYtelseType.ENGANGSTØNAD).when(behandling).getFagsakYtelseType();
    }

    @Test
    void skal_velge_positivt_ES() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_INNVILGELSE);
    }

    @Test
    void skal_velge_negativt_ES() {
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.ENGANGSSTØNAD_AVSLAG);
    }

    @Test
    void skal_velge_positivt_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_positivt_SVP() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE);
    }

    @Test
    void skal_velge_opphør_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(true).when(behandlingsresultatMock).isBehandlingsresultatOpphørt();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_OPPHØR);
    }

    @Test
    void skal_velge_avslag_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_AVSLAG);
    }

    @Test
    void skal_velge_annullert_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(BehandlingResultatType.FORELDREPENGER_SENERE).when(behandlingsresultatMock).getBehandlingResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.FORELDREPENGER_ANNULLERT);
    }

    @Test
    void skal_velge_uendret_utfall() {
        doReturn(true).when(behandlingVedtakMock).isBeslutningsvedtak();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.INGEN_ENDRING);
    }

    @Test
    void skal_velge_fritekstbrev() {
        doReturn(Vedtaksbrev.FRITEKST).when(behandlingsresultatMock).getVedtaksbrev();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.FRITEKSTBREV);
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
        doReturn(true).when(behandlingsresultatMock).isBehandlingsresultatOpphørt();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_OPPHØR);
    }

    @Test
    void skal_velge_avslag_Svp() {
        doReturn(FagsakYtelseType.SVANGERSKAPSPENGER).when(behandling).getFagsakYtelseType();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository)).isEqualTo(DokumentMalType.SVANGERSKAPSPENGER_AVSLAG);
    }

}
