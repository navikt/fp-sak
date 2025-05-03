package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
class DokumentmottakerKlageTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;

    @Inject
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    private LegacyESBeregningRepository beregningRepository;

    @Inject
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    private KlageVurderingTjeneste klageVurderingTjeneste;

    @Inject
    private KlageRepository klageRepository;

    @Inject
    private Behandlingsoppretter behandlingsoppretter;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private DokumentmottakerFelles dokumentmottakerFelles;
    private DokumentmottakerKlage dokumentmottaker;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void oppsett() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        var mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        var behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        historikkinnslagTjeneste = mock(HistorikkinnslagTjeneste.class);
        klageVurderingTjeneste = mock(KlageVurderingTjeneste.class);
        var enhet = new OrganisasjonsEnhet("4806", "Nav Drammen");
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, behandlingRevurderingTjeneste, taskTjeneste, behandlendeEnhetTjeneste,
            historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter, mock(TomtUttakTjeneste.class));
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);
        var behOpprettTjeneste = new BehandlingOpprettingTjeneste(behandlingskontrollTjeneste, behandlendeEnhetTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), taskTjeneste);

        dokumentmottaker = new DokumentmottakerKlage(repositoryProvider, behOpprettTjeneste, dokumentmottakerFelles,
                klageVurderingTjeneste);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    void skal_starte_behandling_av_klage() {
        // Arrange
        var behandling = byggAvsluttetSøknadsbehandlingForFødsel(1);
        var fagsak = behandling.getFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.KLAGE_DOKUMENT;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(dokumentmottaker).startBehandlingAvKlage(mottattDokument, fagsak);
    }

    @Test
    void skal_vurdere_dokument_ved_ettersendelse() {
        // Arrange
        var behandling = byggAvsluttetSøknadsbehandlingForFødsel(1);
        var fagsak = behandling.getFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.KLAGE_ETTERSENDELSE;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    void skal_vurdere_dokument_ved_tidligere_klage() {
        // Arrange
        var behandling = byggAvsluttetKlage();
        var fagsak = behandling.getFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.KLAGE_DOKUMENT;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    void skal_vurdere_dokument_fra_tidligere() {
        // Arrange
        var behandling = byggAvsluttetKlage();
        var fagsak = behandling.getFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.KLAGE_DOKUMENT;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(fagsak, behandling.getId(), mottattDokument, BehandlingÅrsakType.UDEFINERT, false);

        // Assert
        verify(dokumentmottaker).opprettFraTidligereAvsluttetBehandling(fagsak, behandling.getId(), mottattDokument, BehandlingÅrsakType.UDEFINERT,
                false);
    }

    private Behandling byggAvsluttetKlage() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBekreftetHendelse()
                .medAntallBarn(1).medFødselsDato(LocalDate.now());
        var scenarioKlage = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        return scenarioKlage.lagre(repositoryProvider, klageRepository);
    }

    private Behandling byggAvsluttetSøknadsbehandlingForFødsel(int antallBarn) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBekreftetHendelse()
                .medAntallBarn(antallBarn).medFødselsDato(LocalDate.now());

        var behandling = scenario
                .medBehandlingStegStart(BehandlingStegType.FATTE_VEDTAK)
                .lagre(repositoryProvider);
        var kontekst = new BehandlingskontrollKontekst(behandling, behandlingRepository.taSkriveLås(behandling));

        Behandlingsresultat.builderForInngangsvilkår()
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(behandling);
        VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .buildFor(behandling);
        var behandlingsresultat = behandling.getBehandlingsresultat();
        LegacyESBeregningsresultat.builder()
                .medBeregning(new LegacyESBeregning(48500L, 1L, 48500L, LocalDateTime.now()))
                .buildFor(behandling, behandlingsresultat);
        var vedtak = BehandlingVedtak.builder()
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medBehandlingsresultat(behandlingsresultat)
                .medIverksettingStatus(IverksettingStatus.IVERKSATT)
                .medVedtakstidspunkt(LocalDateTime.now())
                .medAnsvarligSaksbehandler("VL")
                .build();
        behandling.avsluttBehandling();
        var lås = kontekst.getSkriveLås();
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        beregningRepository.lagre(behandlingsresultat.getBeregningResultat(), lås);
        behandlingRepository.lagre(behandling, lås);
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, lås);
        return behandling;
    }

    private Fagsak nyMorFødselFagsak() {
        return ScenarioMorSøkerEngangsstønad.forFødselUtenSøknad().lagreFagsak(repositoryProvider);
    }
}
