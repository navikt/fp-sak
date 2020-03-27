package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.AvklarAnnenforelderHarRettOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaTestUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakToTrinnsTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerOppgittFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

public class YtelseFordelingDtoTjenesteImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(repoRule.getEntityManager()));
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste = new FaktaUttakToTrinnsTjeneste(ytelseFordelingTjeneste);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste =  mock(ArbeidsgiverHistorikkinnslag.class);
    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste = new FørsteUttaksdatoTjenesteImpl(ytelseFordelingTjeneste, new ForeldrepengerUttakTjeneste(repositoryProvider.getUttakRepository()));
    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste = new KontrollerOppgittFordelingTjeneste(ytelseFordelingTjeneste,
        repositoryProvider, førsteUttaksdatoTjeneste);
    private YtelsesFordelingRepository fordelingRepository = new YtelsesFordelingRepository(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    @Before
    public void setUp() {
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        faktaUttakHistorikkTjeneste = new FaktaUttakHistorikkTjeneste(lagMockHistory(), arbeidsgiverHistorikkinnslagTjeneste,
            ytelseFordelingTjeneste, inntektArbeidYtelseTjeneste);
    }

    @Test
    public void teste_lag_ytelsefordeling_dto() {
        Behandling behandling = opprettBehandling(AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
        BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto dto = new BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto(
            "begrunnelse");
        dto.setAleneomsorg(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        new BekreftAleneomsorgOppdaterer(repositoryProvider, lagMockHistory(), ytelseFordelingTjeneste) {
        }
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Optional<YtelseFordelingDto> ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAleneOmsorgPerioder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAleneOmsorgPerioder()).hasSize(1);
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    public void teste_lag_ytelsefordeling_dto_med_annenforelder_har_rett_perioder() {
        Behandling behandling = opprettBehandling(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
        AvklarAnnenforelderHarRettDto dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        new AvklarAnnenforelderHarRettOppdaterer(kontrollerOppgittFordelingTjeneste, faktaUttakHistorikkTjeneste, faktaUttakToTrinnsTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Optional<YtelseFordelingDto> ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRett()).isTrue();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRettPerioder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRettPerioder()).hasSize(1);
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    private YtelseFordelingDtoTjeneste tjeneste() {
        return new YtelseFordelingDtoTjeneste(ytelseFordelingTjeneste, repositoryProvider.getFagsakRelasjonRepository(), førsteUttaksdatoTjeneste);
    }

    private Behandling opprettBehandling(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        // Arrange
        LocalDate termindato = LocalDate.now().plusWeeks(16);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("LEGENS ISNDASD")
                .medUtstedtDato(termindato)
                .medTermindato(termindato));

        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, false, true);
        scenario.medOppgittRettighet(rettighet);
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(LocalDate.now().minusDays(20))
            .medOpprinneligEndringsdato(LocalDate.now().minusDays(20))
            .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        scenario.medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        scenario.leggTilAksjonspunkt(aksjonspunktDefinisjon,
            BehandlingStegType.VURDER_UTTAK);
        Behandling behandling = scenario.lagre(repositoryProvider);
        final OppgittPeriodeEntitet periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        final OppgittPeriodeEntitet periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        fordelingRepository.lagre(behandling.getId(), new OppgittFordelingEntitet(List.of(periode_1, periode_2), true));
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        return behandling;
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
