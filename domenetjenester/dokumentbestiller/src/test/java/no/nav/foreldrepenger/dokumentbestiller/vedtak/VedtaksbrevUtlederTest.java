package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

public class VedtaksbrevUtlederTest {

    private Behandlingsresultat behandlingsresultatMock = Mockito.mock(Behandlingsresultat.class);
    private BehandlingVedtak behandlingVedtakMock = Mockito.mock(BehandlingVedtak.class);
    private KlageVurderingResultat klageVurderingResultat = Mockito.mock(KlageVurderingResultat.class);
    private Behandling behandling = Mockito.mock(Behandling.class);

    private KlageRepository klageRepository = Mockito.mock(KlageRepository.class);
    private AnkeRepository ankeRepository = Mockito.mock(AnkeRepository.class);

    @Before
    public void setup() {
        doReturn(Vedtaksbrev.AUTOMATISK).when(behandlingsresultatMock).getVedtaksbrev();
        doReturn(false).when(behandlingVedtakMock).isBeslutningsvedtak();
        doReturn(VedtakResultatType.INNVILGET).when(behandlingVedtakMock).getVedtakResultatType();
        doReturn(FagsakYtelseType.ENGANGSTØNAD).when(behandling).getFagsakYtelseType();
        doReturn(Optional.of(klageVurderingResultat)).when(klageRepository).hentGjeldendeKlageVurderingResultat(behandling);
    }

    @Test
    public void skal_velge_positivt_ES() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository, ankeRepository)).isEqualTo(DokumentMalType.POSITIVT_VEDTAK_DOK);
    }

    @Test
    public void skal_velge_negativt_ES() {
        doReturn(BehandlingResultatType.AVSLÅTT).when(behandlingsresultatMock).getBehandlingResultatType();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository, ankeRepository)).isEqualTo(DokumentMalType.AVSLAGSVEDTAK_DOK);
    }

    @Test
    public void skal_velge_positivt_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository, ankeRepository)).isEqualTo(DokumentMalType.INNVILGELSE_FORELDREPENGER_DOK);
    }

    @Test
    public void skal_velge_opphør_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(true).when(behandlingsresultatMock).isBehandlingsresultatOpphørt();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository, ankeRepository)).isEqualTo(DokumentMalType.OPPHØR_DOK);
    }

    @Test
    public void skal_velge_avslag_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(BehandlingResultatType.AVSLÅTT).when(behandlingsresultatMock).getBehandlingResultatType();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository, ankeRepository)).isEqualTo(DokumentMalType.AVSLAG_FORELDREPENGER_DOK);
    }

    @Test
    public void skal_velge_uendret_utfall() {
        doReturn(true).when(behandlingVedtakMock).isBeslutningsvedtak();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository, ankeRepository)).isEqualTo(DokumentMalType.UENDRETUTFALL_DOK);
    }

    @Test
    public void skal_velge_fritekst() {
        doReturn(Vedtaksbrev.FRITEKST).when(behandlingsresultatMock).getVedtaksbrev();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository, ankeRepository)).isEqualTo(DokumentMalType.FRITEKST_DOK);
    }

    @Test
    public void skal_velge_riktig_klagemal() {
        doReturn(KlageVurdering.STADFESTE_YTELSESVEDTAK).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_STADFESTET);

        doReturn(KlageVurdering.AVVIS_KLAGE).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_AVVIST);

        doReturn(KlageVurdering.OPPHEVE_YTELSESVEDTAK).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_HJEMSENDT);

        doReturn(KlageVurdering.MEDHOLD_I_KLAGE).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_OMGJØRING);
    }

}
