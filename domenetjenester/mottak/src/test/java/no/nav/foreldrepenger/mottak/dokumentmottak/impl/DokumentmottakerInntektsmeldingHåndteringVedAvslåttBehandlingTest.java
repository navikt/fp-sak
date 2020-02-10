package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.mockito.Mockito.any;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerFelles;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerInntektsmelding;

public class DokumentmottakerInntektsmeldingHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private DokumentmottakerInntektsmelding dokumentmottakerInntektsmelding;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private DokumentmottakerFelles dokumentmottakerFellesSpied;

    @Before
    public void setup() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.dokumentmottakerFellesSpied = Mockito.spy(dokumentmottakerFelles);

        dokumentmottakerInntektsmelding = new DokumentmottakerInntektsmelding(
            dokumentmottakerFellesSpied,
            mottatteDokumentTjeneste,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            repositoryProvider);
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedIkkeUtløptFristForInnsendingSkalOppretteNyFørstegangsbehandling() {
        //Arrange
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doReturn(nyBehandling).when(dokumentmottakerFellesSpied).opprettNyFørstegangFraAvslag(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(dokumentmottakerFellesSpied).skalOppretteNyFørstegangsbehandling(any());

        Behandling avslåttBehandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(avslåttBehandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, avslåttBehandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettNyFørstegangFraAvslag(any(), any(), any());
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedUtløptFristForInnsendingSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(any(), any(), any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(any(), any(), any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

    @Test
    public void gittAvslåttBehandlingMenIkkePgaManglendeDokMedSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(any(), any(), any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(any(), any(), any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

    @Test
    public void gittHenlagtBehandlingSkalOppretteVurderDokumentInntilVidere() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(any(), any(), any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.MANGLER_BEREGNINGSREGLER,
            null,
            VedtakResultatType.UDEFINERT,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument inntektsmelding = dummyInntektsmeldingDokument(behandling);

        // Act
        dokumentmottakerInntektsmelding.mottaDokument(inntektsmelding, behandling.getFagsak(), inntektsmelding.getDokumentType(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.never()).opprettNyFørstegangsbehandling(any(), any(), any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(any(), any(), any());
    }

}
