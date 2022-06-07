package no.nav.foreldrepenger.domene.vedtak.fagsakstatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusTjeneste;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OppdaterFagsakStatusTjenesteTest {

    @Mock
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    FagsakRelasjonRepository fagsakRelasjonRepository;
    @Mock
    BehandlingRepository behandlingRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;



    private Behandling behandling;
    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private final Behandlingsresultat behandlingsresultatInnvilget = new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).build();
    private final Behandlingsresultat behandlingsresultatAvslått = new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).build();
    private final Behandlingsresultat behandlingsresultatOpphørt = new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR).build();


    @BeforeEach
    public void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBekreftetHendelse().medFødselsDato(LocalDate.now().minusDays(1));
        behandling = scenario.lagMocked();

        BehandlingRepositoryProvider behandlingRepositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();

        oppdaterFagsakStatusTjeneste = new OppdaterFagsakStatusTjeneste(behandlingRepositoryProvider.getFagsakRepository(), fagsakStatusEventPubliserer, behandlingsresultatRepository, behandlingRepository, fagsakRelasjonRepository, familieHendelseRepository);
    }

    @Test
    public void oppdater_fasakstatus_når_engangsstønad() {
        var behandlingES = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagMocked();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(behandlingES.getFagsakId())).thenReturn(Collections.emptyList());
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingES.getFagsakId())).thenReturn(Optional.of(behandlingES));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandlingES.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET);
    }


    @Test
    public void ikke_oppdater_fasakstatus_når_andre_behandligner_er_åpne() {
        var åpenBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagMocked();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(behandling.getFagsakId())).thenReturn(List.of(åpenBehandling));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId())).thenReturn(Optional.of(behandling));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.INGEN_OPPDATERING);
    }

    @Test
    public void oppdater_fagsakstatus_til_avsluttet_når_behandling_er_null() {
        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), null);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET);
    }

    @Test
    public void ikke_avslutt_fasakstatus_når_harYtelse_og_avslutningsdato() {
        Long fagsakId = behandling.getFagsakId();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId)).thenReturn(Collections.emptyList());
        Mockito.when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsresultatInnvilget));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId)).thenReturn(Optional.of(behandling));
        Mockito.when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(lagfamilieHendelse()));
        Mockito.when(fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak()))
            .thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), null, null, null, Dekningsgrad._80, null, LocalDate.of(2099, 12, 31))));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_LØPENDE);
    }

    @Test
    public void avslutt_fasakstatus_når_harYtelse_og_ingen_avslutningsdato() {
        Long fagsakId = behandling.getFagsakId();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId)).thenReturn(Collections.emptyList());
        Mockito.when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsresultatInnvilget));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId)).thenReturn(Optional.of(behandling));
        Mockito.when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(lagfamilieHendelse()));
        Mockito.when(fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsakId)).thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), null, null, null, Dekningsgrad._80, null, null)));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET);
    }

    @Test
    public void avslutt_fasakstatus_når_harYtelse_og_avslått() {
        Long fagsakId = behandling.getFagsakId();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId)).thenReturn(Collections.emptyList());
        Mockito.when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsresultatAvslått));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId)).thenReturn(Optional.of(behandling));
        Mockito.when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(lagfamilieHendelse()));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET);
    }

    @Test
    public void ikke_avslutt_sak_når_enkeltopphør_og_ikke_er_koblet_til_annen_part() {
        Long fagsakId = behandling.getFagsakId();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId)).thenReturn(Collections.emptyList());
        Mockito.when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsresultatOpphørt));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId)).thenReturn(Optional.of(behandling));
        Mockito.when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(lagfamilieHendelse()));
        Mockito.when(fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak()))
            .thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), null, null, null, Dekningsgrad._80, null, LocalDate.now().plusMonths(2))));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_LØPENDE);
    }

    @Test
    public void avslutt_sak_når_enkeltopphør_og_er_koblet_til_annen_part() {
        Long fagsakId = behandling.getFagsakId();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId)).thenReturn(Collections.emptyList());
        Mockito.when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsresultatOpphørt));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId)).thenReturn(Optional.of(behandling));
        Mockito.when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(lagfamilieHendelse()));
        Mockito.when(fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak()))
            .thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), behandling.getFagsak(), null, null, Dekningsgrad._80, null, LocalDate.now().plusMonths(2))));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET);
    }

    @Test
    public void ikke_avslutt_sak_når_opphører_hele_sakskomplekset_feks_pga_alle_barna_er_døde() {
        Long fagsakId = behandling.getFagsakId();

        Mockito.when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId)).thenReturn(Collections.emptyList());
        Mockito.when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsresultatOpphørt));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId)).thenReturn(Optional.of(behandling));
        Mockito.when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(lagfamilieHendelseDød()));
        Mockito.when(fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak()))
            .thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), behandling.getFagsak(), null, null, Dekningsgrad._80, null, LocalDate.of(2099, 12, 31))));

        var resultat = oppdaterFagsakStatusTjeneste.oppdaterFagsakStatusNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        assertThat(resultat).isEqualTo(FagsakStatusOppdateringResultat.FAGSAK_LØPENDE);
    }


    private FamilieHendelseGrunnlagEntitet lagfamilieHendelse() {
        var build = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .medAntallBarn(1)
            .medFødselsDato(LocalDate.now().minusDays(1));
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty()).medBekreftetVersjon(build).build();
    }

    private FamilieHendelseGrunnlagEntitet lagfamilieHendelseDød() {
        var build = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .medAntallBarn(1)
            .leggTilBarn(new UidentifisertBarnEntitet(1, LocalDate.now().minusDays(1), LocalDate.now()));
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty()).medBekreftetVersjon(build).build();
    }

}
