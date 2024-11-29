package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static java.time.LocalDate.now;
import static java.time.LocalDate.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.AvklarAnnenforelderHarRettOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.BekreftAleneomsorgOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaTestUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;

class YtelseFordelingDtoTjenesteTest extends EntityManagerAwareTest {

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private BehandlingRepositoryProvider repositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private final UføretrygdRepository uføretrygdRepository = mock(UføretrygdRepository.class);

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
    }

    @Test
    void teste_lag_ytelsefordeling_dto() {
        var behandling = opprettBehandling();
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse");
        dto.setAleneomsorg(true);
        // Act
        new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste), repositoryProvider.getHistorikkinnslag2Repository()) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    void teste_lag_ytelsefordeling_dto_med_annenforelder_har_rett_perioder() {
        var behandling = opprettBehandling();
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        // Act
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderRett()).isTrue();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderUføretrygd()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenForelderRettEØS()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenforelderUføretrygd()).isFalse();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenForelderRettEØS()).isFalse();
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    void teste_lag_register_uføretrygd_ytelsefordeling_dto() {
        var behandling = opprettBehandling();
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        // Act
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId())
            .medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(true, now(), now())
            .build()));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderUføretrygd()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenForelderRettEØS()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenforelderUføretrygd()).isFalse();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenForelderRettEØS()).isFalse();
    }

    @Test
    void teste_lag_uavklart_register_uføretrygd_ytelsefordeling_dto() {
        var behandling = opprettBehandling();
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        // Act
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId())
            .medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(false, null, null)
            .build()));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderUføretrygd()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenForelderRettEØS()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenforelderUføretrygd()).isTrue();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenForelderRettEØS()).isFalse();
    }

    @Test
    void teste_lag_uavklart_register_uføretrygd_ytelsefordeling_tidligere_avklart_dto() {
        var behandling = opprettBehandling();
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(behandling.getId(), false, null, true);
        // Act
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId())
            .medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(false, null, null)
            .build()));

        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderUføretrygd()).isTrue();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenForelderRettEØS()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenforelderUføretrygd()).isFalse();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenForelderRettEØS()).isFalse();
    }

    @Test
    void skal_hente_ønsker_justert_fordeling_fra_yf() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny().medPeriode(now().minusDays(10), now()).medPeriodeType(UttakPeriodeType.FEDREKVOTE).build();
        var fordeling = new OppgittFordelingEntitet(List.of(oppgittPeriode), true, true);

        var behandling = opprettBehandling(fordeling);

        var dto = tjeneste().mapFra(behandling).orElseThrow();
        assertThat(dto.isØnskerJustertVedFødsel()).isTrue();
    }

    @Test
    void førsteUttaksdato_skal_være_lik_første_søkte_dag_i_endringssøknad_hvis_tidligere_enn_innvilget_vedtak() {
        var førstegangsUttak = new UttakResultatPerioderEntitet().leggTilPeriode(
            new UttakResultatPeriodeEntitet.Builder(of(2023, 11, 16), of(2023, 12, 16)).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build());
        var førstegangsScenario = ScenarioFarSøkerEngangsstønad.forFødsel().medUttak(førstegangsUttak);
        var førstegangsBehandling = førstegangsScenario.lagre(repositoryProvider);

        var endringssøknadFom = of(2023, 10, 10);
        var endringssøknadPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(endringssøknadFom, endringssøknadFom.plusMonths(2))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(new OppgittFordelingEntitet(List.of(endringssøknadPeriode), true))
            .lagre(repositoryProvider);

        var førsteUttaksdato = tjeneste().finnFørsteUttaksdato(revurdering);

        assertThat(førsteUttaksdato).isEqualTo(endringssøknadFom);
    }

    private YtelseFordelingDtoTjeneste tjeneste() {
        var dekningsgradTjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        return new YtelseFordelingDtoTjeneste(ytelseFordelingTjeneste, dekningsgradTjeneste, uføretrygdRepository, uttakTjeneste);
    }

    private Behandling opprettBehandling() {
        var periode_1 = OppgittPeriodeBuilder.ny().medPeriode(now().minusDays(10), now()).medPeriodeType(UttakPeriodeType.FORELDREPENGER).build();
        var periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(now().minusDays(20), now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode_1, periode_2), true);
        return opprettBehandling(fordeling);
    }

    private Behandling opprettBehandling(OppgittFordelingEntitet fordeling) {
        // Arrange
        var termindato = now().plusWeeks(16);
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(now().minusDays(20))
            .medOpprinneligEndringsdato(now().minusDays(20))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(rettighet)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medOppgittDekningsgrad(Dekningsgrad._100)
            .medFordeling(fordeling);
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("LEGENS ISNDASD")
                .medUtstedtDato(termindato)
                .medTermindato(termindato));

        return scenario.lagre(repositoryProvider);
    }
}
