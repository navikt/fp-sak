package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;

import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
class GrunnbeløpReguleringTaskTest {

    private static final LocalDate TERMINDATO = LocalDate.now().plusWeeks(3);
    private static final Beløp EKSISTERENDE_G = new Beløp(100000);
    private static final LocalDate EKSISTERENDE_STP_B = TERMINDATO.minusMonths(1);

    @Mock
    private BehandlingFlytkontroll flytkontroll;
    @Mock
    private BehandlendeEnhetTjeneste enhetsTjeneste;
    @Mock
    private BehandlingProsesseringTjeneste prosesseringTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;
    @Mock
    private SatsRepository satsRepository;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(Beregningsgrunnlag.builder().medGrunnbeløp(EKSISTERENDE_G).medSkjæringstidspunkt(EKSISTERENDE_STP_B).build())
            .build(BeregningsgrunnlagTilstand.FASTSATT);
        lenient().when(beregningTjeneste.hent(any())).thenReturn(Optional.of(gr));
        lenient().when(satsRepository.finnEksaktSats(eq(BeregningSatsType.GRUNNBELØP), any()))
            .thenReturn(new BeregningSats(BeregningSatsType.GRUNNBELØP, DatoIntervallEntitet.fraOgMedTilOgMed(TERMINDATO.minusYears(1), TERMINDATO),
                EKSISTERENDE_G.getVerdi().longValue() + 1000));
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_når_avsluttet_behandling() {
        var behandling = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET);
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Test"));
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkterForAvsluttetBehandling(any()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdatoGrunnbeløp(TERMINDATO.minusWeeks(3)).build());

        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());

        var task = createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_SATS_REGULERING);
    }

    private void assertRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
                behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        var behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        var årsaker = behandlingÅrsaker.stream()
                .map(BehandlingÅrsak::getBehandlingÅrsakType)
                .toList();
        assertThat(årsaker).contains(behandlingÅrsakType);
    }

    private void assertIngenRevurdering(Fagsak fagsak) {
        var revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
                fagsak.getId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Har revurdering: " + fagsak.getId()).isNotPresent();
    }

    private GrunnbeløpReguleringTask createTask() {
        return new GrunnbeløpReguleringTask(repositoryProvider,
            skjæringstidspunktTjeneste, prosesseringTjeneste, beregningTjeneste, satsRepository, enhetsTjeneste, flytkontroll);
    }

    @Test
    void skal_ikke_opprette_revurdering_dersom_åpen_behandling_på_fagsak() {
        var behandling = opprettRevurderingsKandidat(BehandlingStatus.UTREDES);

        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());

        var task = createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling.getFagsak());
    }

    @Test
    void skal_køe_revurdering_dersom_åpen_berørt_på_fagsak() {
        var behandling = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET);
        when(flytkontroll.nyRevurderingSkalVente(any())).thenReturn(true);
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Test"));
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkterForAvsluttetBehandling(any()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdatoGrunnbeløp(TERMINDATO.minusWeeks(3)).build());

        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());

        var task = createTask();
        task.doTask(prosessTaskData);

        var regulering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
                behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(regulering.filter(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING))).isPresent();
        verify(flytkontroll).settNyRevurderingPåVent(regulering.get());
    }

    @Test
    void skal_ikke_opprette_revurdering_dersom_skal_ha_gammel_sats() {
        var behandling = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkterForAvsluttetBehandling(any()))
            .thenReturn(Skjæringstidspunkt.builder().medFørsteUttaksdatoGrunnbeløp(TERMINDATO.minusYears(2)).build());
        when(satsRepository.finnEksaktSats(eq(BeregningSatsType.GRUNNBELØP), any()))
            .thenReturn(new BeregningSats(BeregningSatsType.GRUNNBELØP, DatoIntervallEntitet.fraOgMedTilOgMed(EKSISTERENDE_STP_B.minusYears(1), EKSISTERENDE_STP_B),
                EKSISTERENDE_G.getVerdi().longValue()));

        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());

        var task = createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling.getFagsak());
    }

    @Test
    void skal_opprette_revurdering_ved_manuell_oppretting() {
        var behandling = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET);
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Test"));

        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(GrunnbeløpReguleringTask.MANUELL_KEY, "true");

        var task = createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_SATS_REGULERING);
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status) {

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medSøknadDato(TERMINDATO.minusDays(20));

        scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                        .medNavnPå("Lege Legesen")
                        .medTermindato(TERMINDATO)
                        .medUtstedtDato(TERMINDATO.minusDays(40)))
                .medAntallBarn(1);

        scenario.medBekreftetHendelse()
                .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                        .medNavnPå("Lege Legesen")
                        .medTermindato(TERMINDATO)
                        .medUtstedtDato(TERMINDATO.minusDays(40)))
                .medAntallBarn(1);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT);
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);

        scenario.medBehandlingVedtak()
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medVedtakstidspunkt(TERMINDATO.minusWeeks(2).atStartOfDay())
                .medAnsvarligSaksbehandler("Severin Saksbehandler")
                .build();

        scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));

        var behandling = scenario.lagre(repositoryProvider);
        behandling.setStatus(status);

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        repositoryProvider.getOpptjeningRepository()
                .lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);

        return behandlingRepository.hentBehandling(behandling.getId());
    }

}
