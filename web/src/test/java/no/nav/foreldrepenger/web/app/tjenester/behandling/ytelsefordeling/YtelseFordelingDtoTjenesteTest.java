package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.AvklarAnnenforelderHarRettOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.BekreftAleneomsorgOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaTestUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;

class YtelseFordelingDtoTjenesteTest extends EntityManagerAwareTest {

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
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
        new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory())) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling, null), dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    void teste_lag_ytelsefordeling_dto_med_annenforelder_har_rett_perioder() {
        var behandling = opprettBehandling();
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        // Act
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory())).oppdater(dto, new AksjonspunktOppdaterParameter(
            BehandlingReferanse.fra(behandling, null), dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull().isNotEmpty();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderRett()).isTrue();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenforelderUføretrygd()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().bekreftetAnnenForelderRettEØS()).isNull();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenforelderUføretrygd()).isFalse();
        assertThat(ytelseFordelingDtoOpt.get().getRettigheterAnnenforelder().skalAvklareAnnenForelderRettEØS()).isFalse();
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    void teste_lag_register_uføretrygd_ytelsefordeling_dto() {
        var behandling = opprettBehandling();
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        // Act
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory())).oppdater(dto, new AksjonspunktOppdaterParameter(
            BehandlingReferanse.fra(behandling, null), dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(true, LocalDate.now(), LocalDate.now()).build()));
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
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory())).oppdater(dto, new AksjonspunktOppdaterParameter(
            BehandlingReferanse.fra(behandling, null), dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(false, null, null).build()));
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
        new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory())).oppdater(dto, new AksjonspunktOppdaterParameter(
            BehandlingReferanse.fra(behandling, null), dto));
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(false, null, null).build()));

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
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(oppgittPeriode), true, true);

        var behandling = opprettBehandling(fordeling);

        var dto = tjeneste().mapFra(behandling).orElseThrow();
        assertThat(dto.isØnskerJustertVedFødsel()).isTrue();
    }

    private YtelseFordelingDtoTjeneste tjeneste() {
        return new YtelseFordelingDtoTjeneste(ytelseFordelingTjeneste, repositoryProvider.getFagsakRelasjonRepository(),
            uføretrygdRepository, uttakTjeneste);
    }

    private Behandling opprettBehandling() {
        var periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode_1, periode_2), true);
        return opprettBehandling(fordeling);
    }

    private Behandling opprettBehandling(OppgittFordelingEntitet fordeling) {
        // Arrange
        var termindato = LocalDate.now().plusWeeks(16);
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            LocalDate.now().minusDays(20)).medOpprinneligEndringsdato(LocalDate.now().minusDays(20)).build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(rettighet)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100())
            .medFordeling(fordeling);
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("LEGENS ISNDASD")
                .medUtstedtDato(termindato)
                .medTermindato(termindato));

        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        return behandling;
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        var mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
