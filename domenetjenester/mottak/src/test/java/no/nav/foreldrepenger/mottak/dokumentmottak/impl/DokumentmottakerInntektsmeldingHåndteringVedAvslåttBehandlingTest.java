package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

class DokumentmottakerInntektsmeldingHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private DokumentmottakerInntektsmelding dokumentmottakerInntektsmelding;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private DokumentmottakerFelles dokumentmottakerFellesSpied;

    @BeforeEach
    void setUp() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.dokumentmottakerFellesSpied = Mockito.spy(dokumentmottakerFelles);

        dokumentmottakerInntektsmelding = new DokumentmottakerInntektsmelding(
            dokumentmottakerFellesSpied,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            repositoryProvider.getBehandlingRepository(),
                behandlingRevurderingTjeneste,
            fpUttakTjeneste);
    }

    @Test
    void gittAvslåttBehandlingPgaManglendeDokMedIkkeUtløptFristForInnsendingSkalOppretteNyFørstegangsbehandling() {
        //Arrange
        var nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(dokumentmottakerFellesSpied).skalOppretteNyFørstegangsbehandling(any());

        var avslåttBehandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        var inntektsmelding = dummyInntektsmeldingDokument(avslåttBehandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, avslåttBehandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(any(), any(), any());
    }

    @Test
    void gittAvslåttBehandlingPgaManglendeDokMedUtløptFristForInnsendingSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(any(), any(), any());
        var behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        var inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(any(), any(), any(), anyBoolean());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

    @Test
    void gittAvslåttBehandlingMenIkkePgaManglendeDokMedSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(any(), any(), any());
        var behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        var inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(any(), any(), any(), anyBoolean());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

    @Test
    void gittHenlagtBehandlingSkalOppretteVurderDokumentInntilVidere() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(any(), any(), any());
        var behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.HENLAGT_SØKNAD_MANGLER,
            null,
            VedtakResultatType.UDEFINERT,
            DATO_ETTER_INNSENDINGSFRISTEN);
        var inntektsmelding = dummyInntektsmeldingDokument(behandling);
        Mockito.doReturn(Boolean.TRUE).when(dokumentmottakerFellesSpied).harFagsakMottattSøknadTidligere(any());

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(any(), any(), any(), anyBoolean());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

}
