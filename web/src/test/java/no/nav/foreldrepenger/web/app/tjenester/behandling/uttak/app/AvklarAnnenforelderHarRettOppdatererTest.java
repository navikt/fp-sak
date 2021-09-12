package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.AvklarAnnenforelderHarRettOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjenesteImpl;

public class AvklarAnnenforelderHarRettOppdatererTest extends EntityManagerAwareTest {

    private static final AksjonspunktDefinisjon AKSONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT;

    private BehandlingRepositoryProvider repositoryProvider;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private HistorikkInnslagTekstBuilder tekstBuilder;

    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste;
    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(
            new YtelsesFordelingRepository(entityManager));
        historikkApplikasjonTjeneste = mock(HistorikkTjenesteAdapter.class);
        tekstBuilder = new HistorikkInnslagTekstBuilder();
        var arbeidsgiverHistorikkinnslagTjeneste = mock(ArbeidsgiverHistorikkinnslag.class);
        var inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
        faktaUttakToTrinnsTjeneste = new FaktaUttakToTrinnsTjeneste(ytelseFordelingTjeneste);
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        var førsteUttaksdatoTjeneste = new FørsteUttaksdatoTjenesteImpl(
            ytelseFordelingTjeneste, uttakTjeneste);
        kontrollerOppgittFordelingTjeneste = new KontrollerOppgittFordelingTjeneste(ytelseFordelingTjeneste,
            repositoryProvider, førsteUttaksdatoTjeneste);
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        faktaUttakHistorikkTjeneste = new FaktaUttakHistorikkTjeneste(lagMockHistory(),
            arbeidsgiverHistorikkinnslagTjeneste, ytelseFordelingTjeneste, inntektArbeidYtelseTjeneste);
    }

    @Test
    public void skal_opprette_historikkinnslag_ved_endring() {
        //Scenario med avklar fakta annen forelder har rett
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());

        oppdaterer().oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        //assert
        assertThat(historikkinnslagDeler).hasSize(1);
        var del = historikkinnslagDeler.get(0);
        var rettOpt = del.getEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER);
        assertThat(rettOpt).hasValueSatisfying(rett -> {
            assertThat(rett.getNavn()).isEqualTo(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER.getKode());
            assertThat(rett.getFraVerdi()).isNull();
            assertThat(rett.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_RETT.getKode());
        });
        assertThat(del.getSkjermlenke()).hasValueSatisfying(
            skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK.getKode()));
        assertThat(del.getBegrunnelse()).hasValueSatisfying(
            begrunnelse -> assertThat(begrunnelse).isEqualTo("Har rett"));
    }

    @Test
    public void skal_sette_totrinns_ved_avkreft_søkers_opplysning() {
        //Scenario med avklar fakta annen forelder har ikke rett
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon()).get();

        var resultat = oppdaterer().oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        //assert
        assertThat(behandling.harAksjonspunktMedType(AKSONSPUNKT_DEF)).isTrue();
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    private AvklarAnnenforelderHarRettOppdaterer oppdaterer() {
        return new AvklarAnnenforelderHarRettOppdaterer(kontrollerOppgittFordelingTjeneste, faktaUttakHistorikkTjeneste,
            faktaUttakToTrinnsTjeneste);
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        Mockito.when(historikkApplikasjonTjeneste.tekstBuilder()).thenReturn(tekstBuilder);
        return historikkApplikasjonTjeneste;
    }
}
