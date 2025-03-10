package no.nav.foreldrepenger.kompletthet.impl.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.Innsendingsvalg;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerImpl;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerSøknadTjeneste;
import no.nav.foreldrepenger.kompletthet.impl.ManglendeVedleggTjeneste;

class KompletthetssjekkerManglendeVedleggTest {

    private static final Saksnummer SAKSNUMMER  = new Saksnummer("9999");

    private final DokumentArkivTjeneste dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
    private final SøknadRepository søknadRepository = mock(SøknadRepository.class);
    private final Kompletthetsjekker kompletthetssjekker = lagKompletthetssjekkerEngangsstønad(dokumentArkivTjeneste, søknadRepository);

    private final DokumentTypeId dokumentTypeIdDokumentasjonAvTerminEllerFødsel = DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL;
    private final DokumentTypeId dokumentTypeIdDokumentasjonAvOmsorgsovertakelse = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;
    private final DokumentTypeId dokumentTypeIdUdefinert = DokumentTypeId.UDEFINERT;

    private static Kompletthetsjekker lagKompletthetssjekkerEngangsstønad(DokumentArkivTjeneste dokumentArkivTjeneste, SøknadRepository søknadRepository) {
        var repositoryProvider = mock(BehandlingRepositoryProvider.class);
        when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        var manglendeVedleggTjeneste = new ManglendeVedleggTjeneste(repositoryProvider, dokumentArkivTjeneste);
        var kompletthetsjekkerSøknad = new KompletthetsjekkerSøknadTjeneste(repositoryProvider, manglendeVedleggTjeneste);
        return new KompletthetsjekkerImpl(repositoryProvider.getBehandlingRepository(), kompletthetsjekkerSøknad, null);
    }

    @Test
    void skal_regne_søknaden_som_komplett_når_JournalTjeneste_har_alle_dokumentene() {
        // Arrange
        Set<DokumentTypeId> dokumentTypeIds = new HashSet<>();
        dokumentTypeIds.add(dokumentTypeIdDokumentasjonAvTerminEllerFødsel);
        dokumentTypeIds.add(dokumentTypeIdUdefinert);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(), any())).thenReturn(dokumentTypeIds);

        var behandling = lagBehandling();

        var påkrevdSøknadVedlegg = new SøknadVedleggEntitet.Builder()
            .medSkjemanummer(dokumentTypeIdDokumentasjonAvTerminEllerFødsel.getKode())
            .medErPåkrevdISøknadsdialog(true)
            .medInnsendingsvalg(Innsendingsvalg.LASTET_OPP)
            .build();
        var annetSøknadVedlegg = new SøknadVedleggEntitet.Builder()
            .medSkjemanummer(dokumentTypeIdUdefinert.getKode())
            .medErPåkrevdISøknadsdialog(false)
            .medInnsendingsvalg(Innsendingsvalg.LASTET_OPP)
            .build();
        var søknad = new SøknadEntitet.Builder().leggTilVedlegg(påkrevdSøknadVedlegg).leggTilVedlegg(annetSøknadVedlegg).build();
        reset(søknadRepository);
        when(søknadRepository.hentSøknad(behandling.getId())).thenReturn(søknad);
        when(søknadRepository.hentSøknadHvisEksisterer(behandling.getId())).thenReturn(Optional.ofNullable(søknad));

        // Act
        var manglendeVedlegg = kompletthetssjekker.utledAlleManglendeVedleggForForsendelse(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    void skal_regne_søknaden_som_ukomplett_når_JournalTjeneste_ikke_har_alle_dokumentene() {
        // Arrange
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(), any())).thenReturn(Collections.singleton(dokumentTypeIdUdefinert));

        var behandling = lagBehandling();

        var påkrevdSøknadVedlegg = new SøknadVedleggEntitet.Builder()
            .medSkjemanummer(dokumentTypeIdDokumentasjonAvTerminEllerFødsel.getOffisiellKode())
            .medErPåkrevdISøknadsdialog(true)
            .medInnsendingsvalg(Innsendingsvalg.SEND_SENERE)
            .build();
        var annetSøknadVedlegg = new SøknadVedleggEntitet.Builder()
            .medSkjemanummer(dokumentTypeIdUdefinert.getOffisiellKode())
            .medErPåkrevdISøknadsdialog(false)
            .medInnsendingsvalg(Innsendingsvalg.LASTET_OPP)
            .build();
        var søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true).leggTilVedlegg(påkrevdSøknadVedlegg).leggTilVedlegg(annetSøknadVedlegg).build();
        reset(søknadRepository);
        when(søknadRepository.hentSøknad(behandling.getId())).thenReturn(søknad);
        when(søknadRepository.hentSøknadHvisEksisterer(behandling.getId())).thenReturn(Optional.ofNullable(søknad));
        // Act
        var manglendeVedlegg = kompletthetssjekker.utledAlleManglendeVedleggForForsendelse(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isNotEmpty();
    }

    @Test
    void skal_også_håndtere_at_dokumentene_kommer_i_motsatt_rekkefølge_på_søknaden() {
        // Arrange
        Set<DokumentTypeId> dokumentTypeIds = new HashSet<>();
        dokumentTypeIds.add(dokumentTypeIdDokumentasjonAvTerminEllerFødsel);
        dokumentTypeIds.add(dokumentTypeIdUdefinert);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(), any())).thenReturn(dokumentTypeIds);

        var behandling = lagBehandling();

        var påkrevdSøknadVedlegg1 = new SøknadVedleggEntitet.Builder()
            .medSkjemanummer(dokumentTypeIdDokumentasjonAvTerminEllerFødsel.getOffisiellKode())
            .medErPåkrevdISøknadsdialog(true)
            .medInnsendingsvalg(Innsendingsvalg.SEND_SENERE)
            .build();
        var påkrevdSøknadVedlegg2 = new SøknadVedleggEntitet.Builder()
            .medSkjemanummer(dokumentTypeIdDokumentasjonAvOmsorgsovertakelse.getOffisiellKode())
            .medErPåkrevdISøknadsdialog(true)
            .medInnsendingsvalg(Innsendingsvalg.LASTET_OPP)
            .build();
        var søknad = new SøknadEntitet.Builder().medElektroniskRegistrert(true).leggTilVedlegg(påkrevdSøknadVedlegg2).leggTilVedlegg(påkrevdSøknadVedlegg1).build();
        reset(søknadRepository);
        when(søknadRepository.hentSøknad(behandling.getId())).thenReturn(søknad);
        when(søknadRepository.hentSøknadHvisEksisterer(behandling.getId())).thenReturn(Optional.ofNullable(søknad));
        // Act
        var manglendeVedlegg = kompletthetssjekker.utledAlleManglendeVedleggForForsendelse(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isNotEmpty();
    }

    @Test
    void skal_regne_søknaden_som_komplett_hvis_den_ikke_inneholder_vedlegg() {
        // Arrange
        var behandling = lagBehandling();
        var søknad = new SøknadEntitet.Builder().build();
        reset(søknadRepository);
        when(søknadRepository.hentSøknad(behandling.getId())).thenReturn(søknad);
        when(søknadRepository.hentSøknadHvisEksisterer(any())).thenReturn(Optional.ofNullable(søknad));
        // Act
        var manglendeVedlegg = kompletthetssjekker.utledAlleManglendeVedleggForForsendelse(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medSaksnummer(SAKSNUMMER);
        return scenario.lagMocked();
    }
}
