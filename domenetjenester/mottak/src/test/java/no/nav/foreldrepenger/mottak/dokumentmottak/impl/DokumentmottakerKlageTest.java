package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.klage.KlageFormkravTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class DokumentmottakerKlageTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    private LegacyESBeregningRepository beregningRepository;

    @Inject
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    private KlageFormkravTjeneste klageFormkravTjeneste;

    @Inject
    private KlageRepository klageRepository;

    @Inject
    private Behandlingsoppretter behandlingsoppretter;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    private DokumentmottakerFelles dokumentmottakerFelles;
    private DokumentmottakerKlage dokumentmottaker;
    private BehandlingRepository behandlingRepository;

    @Before
    public void oppsett() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        MottatteDokumentTjeneste mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        historikkinnslagTjeneste = mock(HistorikkinnslagTjeneste.class);
        klageFormkravTjeneste = mock(KlageFormkravTjeneste.class);
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("4806", "NAV Drammen");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);

        BehandlingskontrollTjeneste behandlingskontrollTjeneste = DokumentmottakTestUtil.lagBehandlingskontrollTjenesteMock(serviceProvider);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, prosessTaskRepository,
            behandlendeEnhetTjeneste, historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter);
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);
        var behOpprettTjeneste = new BehandlingOpprettingTjeneste(behandlingskontrollTjeneste, behandlendeEnhetTjeneste, mock(HistorikkRepository.class), prosessTaskRepository);

        dokumentmottaker = new DokumentmottakerKlage(repositoryProvider, behOpprettTjeneste, dokumentmottakerFelles,
                klageFormkravTjeneste);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    public void skal_starte_behandling_av_klage() {
        // Arrange
        Behandling behandling = byggAvsluttetSøknadsbehandlingForFødsel(1);
        Fagsak fagsak = behandling.getFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.KLAGE_DOKUMENT;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(dokumentmottaker).startBehandlingAvKlage(mottattDokument, fagsak);
    }

    @Test
    public void skal_vurdere_dokument_ved_ettersendelse() {
        // Arrange
        Behandling behandling = byggAvsluttetSøknadsbehandlingForFødsel(1);
        Fagsak fagsak = behandling.getFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.KLAGE_ETTERSENDELSE;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    public void skal_vurdere_dokument_ved_tidligere_klage() {
        // Arrange
        Behandling behandling = byggAvsluttetKlage();
        Fagsak fagsak = behandling.getFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.KLAGE_DOKUMENT;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    public void skal_vurdere_dokument_fra_tidligere() {
        // Arrange
        Behandling behandling = byggAvsluttetKlage();
        Fagsak fagsak = behandling.getFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.KLAGE_DOKUMENT;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(fagsak, behandling.getId(), mottattDokument, BehandlingÅrsakType.UDEFINERT, false);

        // Assert
        verify(dokumentmottaker).opprettFraTidligereAvsluttetBehandling(fagsak, behandling.getId(), mottattDokument, BehandlingÅrsakType.UDEFINERT, false);
    }

    @Test
    public void skal_kaste_feil_når_det_kommer_inn_klage_og_det_ikke_finnes_en_vanlig_behandlig() {
        // Arrange
        Fagsak fagsak = nyMorFødselFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.KLAGE_DOKUMENT;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);
    }

    private Behandling byggAvsluttetKlage() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBekreftetHendelse()
            .medAntallBarn(1).medFødselsDato(LocalDate.now());
        ScenarioKlageEngangsstønad scenarioKlage = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        return scenarioKlage.lagre(repositoryProvider, klageRepository);
    }

    private Behandling byggAvsluttetSøknadsbehandlingForFødsel(int antallBarn) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBekreftetHendelse()
            .medAntallBarn(antallBarn).medFødselsDato(LocalDate.now());

        Behandling behandling = scenario
            .medBehandlingStegStart(BehandlingStegType.FATTE_VEDTAK)
            .lagre(repositoryProvider);
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));

        Behandlingsresultat.builderForInngangsvilkår()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);
        VilkårResultat.builder()
            .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT,
                null, new Properties(), null, false, false, "", "")
            .medVilkårResultatType(VilkårResultatType.INNVILGET)
            .buildFor(behandling);
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        LegacyESBeregningsresultat.builder()
            .medBeregning(new LegacyESBeregning(48500L, 1L, 48500L, LocalDateTime.now()))
            .buildFor(behandling, behandlingsresultat);
        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medVedtakstidspunkt(LocalDateTime.now())
            .medAnsvarligSaksbehandler("VL")
            .build();
        behandling.avsluttBehandling();
        BehandlingLås lås = kontekst.getSkriveLås();
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
