package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class BekreftTerminbekreftelseOppdatererTest extends EntityManagerAwareTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final LocalDate now = LocalDate.now();
    private final DateTimeFormatter formatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null,
            repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_terminbekreftelse() {
        // Arrange
        LocalDate opprinneligTermindato = LocalDate.now();
        LocalDate avklartTermindato = opprinneligTermindato.plusDays(1);
        LocalDate opprinneligUtstedtDato = LocalDate.now().minusDays(20);
        LocalDate avklartUtstedtDato = opprinneligUtstedtDato.plusDays(1);
        int opprinneligAntallBarn = 1;
        int avklartAntallBarn = 2;

        // Behandling
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(opprinneligTermindato)
                .medUtstedtDato(opprinneligUtstedtDato)
                .medNavnPå("LEGEN MIN"))
            .medAntallBarn(opprinneligAntallBarn);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        Behandling behandling = scenario.lagre(repositoryProvider);
        // Dto
        BekreftTerminbekreftelseAksjonspunktDto dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse",
            avklartTermindato, avklartUtstedtDato, avklartAntallBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        BekreftTerminbekreftelseOppdaterer oppdaterer = new BekreftTerminbekreftelseOppdaterer(repositoryProvider,
            lagMockHistory(),
            skjæringstidspunktTjeneste,
            familieHendelseTjeneste,
            new BekreftTerminbekreftelseValidator(Period.parse("P25D")));

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert

        assertThat(historikkInnslag).hasSize(1);

        HistorikkinnslagDel del = historikkInnslag.get(0);
        List<HistorikkinnslagFelt> feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(3);
        assertFelt(del, HistorikkEndretFeltType.TERMINDATO, opprinneligTermindato.format(formatterer), avklartTermindato.format(formatterer));
        assertFelt(del, HistorikkEndretFeltType.UTSTEDTDATO, opprinneligUtstedtDato.format(formatterer), avklartUtstedtDato.format(formatterer));
        assertFelt(del, HistorikkEndretFeltType.ANTALL_BARN, Integer.toString(opprinneligAntallBarn), Integer.toString(avklartAntallBarn));
    }

    private void assertFelt(HistorikkinnslagDel del, HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        Optional<HistorikkinnslagFelt> feltOpt = del.getEndretFelt(historikkEndretFeltType);
        String feltNavn = historikkEndretFeltType.getKode();
        assertThat(feltOpt).hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).as(feltNavn + ".navn").isEqualTo(feltNavn);
            assertThat(felt.getFraVerdi()).as(feltNavn + ".fraVerdi").isEqualTo(fraVerdi);
            assertThat(felt.getTilVerdi()).as(feltNavn + ".tilVerdi").isEqualTo(tilVerdi);
        });
    }

    @Test
    public void skal_oppdatere_terminbekreftelse() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medNavnPå("LEGEN MIN")
            .medTermindato(now.plusDays(30))
            .medUtstedtDato(now.minusDays(3)))
            .medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        Behandling behandling = scenario.lagre(repositoryProvider);
        BekreftTerminbekreftelseAksjonspunktDto dto = new BekreftTerminbekreftelseAksjonspunktDto(
            "Begrunnelse", now.plusDays(30), now.minusDays(3), 1);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        BekreftTerminbekreftelseOppdaterer oppdaterer = new BekreftTerminbekreftelseOppdaterer(repositoryProvider,
            lagMockHistory(),
            skjæringstidspunktTjeneste,
            familieHendelseTjeneste,
            new BekreftTerminbekreftelseValidator(Period.parse("P25D")));

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregat(behandling.getId());

        final Optional<TerminbekreftelseEntitet> terminbekreftelse = familieHendelseGrunnlag.getGjeldendeVersjon().getTerminbekreftelse();

        assertThat(terminbekreftelse).isPresent();
        assertThat(familieHendelseGrunnlag.getGjeldendeVersjon().getAntallBarn()).isEqualTo(1);
        assertThat(terminbekreftelse.get().getTermindato()).isEqualTo(now.plusDays(30));
        assertThat(terminbekreftelse.get().getUtstedtdato()).isEqualTo(now.minusDays(3));
    }

    @Test
    public void skal_trigge_totrinnsbehandling_når_aksjonspunkt_bekreftes_på_nytt_etter_tilbakehopp() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medNavnPå("LEGEN MIN")
            .medTermindato(now.plusDays(30))
            .medUtstedtDato(now.minusDays(3)))
            .medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);
        var dto = new BekreftTerminbekreftelseAksjonspunktDto("Begrunnelse", now.plusDays(30), now.minusDays(3), 2);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode()).get();
        // Act
        var oppdaterer = new BekreftTerminbekreftelseOppdaterer(repositoryProvider,
            lagMockHistory(),
            skjæringstidspunktTjeneste,
            familieHendelseTjeneste,
            new BekreftTerminbekreftelseValidator(Period.parse("P25D")));

        OppdateringResultat resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert - sjekk at totrinnsbehandling blir satt før tilbakehopp
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();

        // Simuler at aksjonspunkt resettes ved tilbakehopp
        AksjonspunktTestSupport.fjernToTrinnsBehandlingKreves(aksjonspunkt);

        // Act
        oppdaterer = new BekreftTerminbekreftelseOppdaterer(repositoryProvider,
            lagMockHistory(),
            skjæringstidspunktTjeneste,
            familieHendelseTjeneste,
            new BekreftTerminbekreftelseValidator(Period.parse("P25D")));

        OppdateringResultat oppdateringResultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert - sjekk at totrinnsbehandling blir satt etter tilbakehopp
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

}
