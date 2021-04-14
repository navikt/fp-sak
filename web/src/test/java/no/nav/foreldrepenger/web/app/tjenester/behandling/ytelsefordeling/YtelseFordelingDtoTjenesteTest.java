package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
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

public class YtelseFordelingDtoTjenesteTest extends EntityManagerAwareTest {

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private final ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = mock(
        ArbeidsgiverHistorikkinnslag.class);
    private BehandlingRepositoryProvider repositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste;
    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste;
    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste;
    private YtelsesFordelingRepository fordelingRepository;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        faktaUttakToTrinnsTjeneste = new FaktaUttakToTrinnsTjeneste(ytelseFordelingTjeneste);
        førsteUttaksdatoTjeneste = new FørsteUttaksdatoTjenesteImpl(ytelseFordelingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
        kontrollerOppgittFordelingTjeneste = new KontrollerOppgittFordelingTjeneste(ytelseFordelingTjeneste,
            repositoryProvider, førsteUttaksdatoTjeneste);
        fordelingRepository = new YtelsesFordelingRepository(entityManager);
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        faktaUttakHistorikkTjeneste = new FaktaUttakHistorikkTjeneste(lagMockHistory(),
            arbeidsgiverHistorikkinnslagTjeneste, ytelseFordelingTjeneste, inntektArbeidYtelseTjeneste);
    }

    @Test
    public void teste_lag_ytelsefordeling_dto() {
        var behandling = opprettBehandling(AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
        var dto = new BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto(
            "begrunnelse");
        dto.setAleneomsorg(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        new BekreftAleneomsorgOppdaterer(repositoryProvider, lagMockHistory(), ytelseFordelingTjeneste) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAleneOmsorgPerioder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAleneOmsorgPerioder()).hasSize(1);
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    @Test
    public void teste_lag_ytelsefordeling_dto_med_annenforelder_har_rett_perioder() {
        var behandling = opprettBehandling(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        new AvklarAnnenforelderHarRettOppdaterer(kontrollerOppgittFordelingTjeneste, faktaUttakHistorikkTjeneste,
            faktaUttakToTrinnsTjeneste).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        var ytelseFordelingDtoOpt = tjeneste().mapFra(behandling);
        assertThat(ytelseFordelingDtoOpt).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRett()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRett()).isTrue();
        assertThat(
            ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRettPerioder()).isNotNull();
        assertThat(ytelseFordelingDtoOpt.get().getAnnenforelderHarRettDto().getAnnenforelderHarRettPerioder()).hasSize(
            1);
        assertThat(ytelseFordelingDtoOpt.get().getEndringsdato()).isEqualTo(LocalDate.now().minusDays(20));
        assertThat(ytelseFordelingDtoOpt.get().getGjeldendeDekningsgrad()).isEqualTo(100);
    }

    private YtelseFordelingDtoTjeneste tjeneste() {
        return new YtelseFordelingDtoTjeneste(ytelseFordelingTjeneste, repositoryProvider.getFagsakRelasjonRepository(),
            førsteUttaksdatoTjeneste);
    }

    private Behandling opprettBehandling(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        // Arrange
        var termindato = LocalDate.now().plusWeeks(16);
        var rettighet = new OppgittRettighetEntitet(false, false, true);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            LocalDate.now().minusDays(20)).medOpprinneligEndringsdato(LocalDate.now().minusDays(20)).build();
        var periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(rettighet)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100())
            .medFordeling(new OppgittFordelingEntitet(List.of(periode_1, periode_2), true));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("LEGENS ISNDASD")
                .medUtstedtDato(termindato)
                .medTermindato(termindato));

        scenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.VURDER_UTTAK);
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
