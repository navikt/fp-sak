package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.mockito.ArgumentMatchers.any;

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

public class DokumentmottakerVedleggHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private DokumentmottakerVedlegg dokumentmottakerVedlegg;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private DokumentmottakerFelles dokumentmottakerFellesSpied;

    @Before
    public void setup() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.dokumentmottakerFellesSpied = Mockito.spy(dokumentmottakerFelles);

        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettHistorikkinnslagForVedlegg(Mockito.any(), Mockito.any(), Mockito.any());

        dokumentmottakerVedlegg = new DokumentmottakerVedlegg(
            repositoryProvider,
            dokumentmottakerFellesSpied,
            kompletthetskontroller);
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedIkkeUtløptFristForInnsendingSkalOppretteNyFørstegangsbehandling() {
        //Arrange
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(dokumentmottakerFellesSpied).skalOppretteNyFørstegangsbehandling(any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument vedlegg = dummyVedleggDokument(behandling);

        // Act
        dokumentmottakerVedlegg.mottaDokument(vedlegg, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedUtløptFristForInnsendingSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument vedlegg = dummyVedleggDokument(behandling);

        // Act
        dokumentmottakerVedlegg.mottaDokument(vedlegg, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.never()).opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void gittAvslåttBehandlingMenIkkePgaManglendeDokMedSkalOppretteTaskForÅVurdereDokument() {
        //Arrange
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG,
            VedtakResultatType.AVSLAG,
            DATO_ETTER_INNSENDINGSFRISTEN);
        MottattDokument vedlegg = dummyVedleggDokument(behandling);

        // Act
        dokumentmottakerVedlegg.mottaDokument(vedlegg, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.never()).opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void gittAvslåttBehandlingPgaManglendeDokMedIkkeUtløptFristForInnsendingSkalOppretteTaskForÅVurdereDokumentNårVedleggetErTypeAnnet() {
        //Arrange
        Mockito.doReturn(true).when(dokumentmottakerFellesSpied).skalOppretteNyFørstegangsbehandling(any());
        Mockito.doNothing().when(dokumentmottakerFellesSpied).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
        Behandling behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument vedlegg = dummyVedleggDokumentTypeAnnet(behandling);

        // Act
        dokumentmottakerVedlegg.mottaDokument(vedlegg, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.never()).opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dokumentmottakerFellesSpied, Mockito.times(1)).opprettTaskForÅVurdereDokument(Mockito.any(), Mockito.any(), Mockito.any());
    }

}
