package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

class BekreftTerminbekreftelseOppdatererTest extends EntityManagerAwareTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE;

    private final LocalDate now = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private Historikkinnslag2Repository historikkRepository;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        historikkRepository = mock(Historikkinnslag2Repository.class);
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_terminbekreftelse() {
        // Arrange
        var opprinneligTermindato = LocalDate.now();
        var avklartTermindato = opprinneligTermindato.plusDays(1);
        var opprinneligUtstedtDato = LocalDate.now().minusDays(20);
        var avklartUtstedtDato = opprinneligUtstedtDato.plusDays(1);
        var opprinneligAntallBarn = 1;
        var avklartAntallBarn = 2;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(opprinneligTermindato)
                .medUtstedtDato(opprinneligUtstedtDato)
                .medNavnPå("LEGEN MIN"))
            .medAntallBarn(opprinneligAntallBarn);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);
        // Dto
        var dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse",
            avklartTermindato, avklartUtstedtDato, avklartAntallBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdaterer = new BekreftTerminbekreftelseOppdaterer(historikkRepository, mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag2.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(4);
        assertThat(historikkinnslag.getTekstlinjer().get(0).getTekst()).contains("Termindato", format(opprinneligTermindato), format(avklartTermindato));
        assertThat(historikkinnslag.getTekstlinjer().get(1).getTekst()).contains("Utstedtdato", format(opprinneligUtstedtDato), format(avklartUtstedtDato));
        assertThat(historikkinnslag.getTekstlinjer().get(2).getTekst()).contains("Antall barn", Integer.toString(opprinneligAntallBarn), Integer.toString(avklartAntallBarn));
        assertThat(historikkinnslag.getTekstlinjer().get(3).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_generere_historikkinnslag_ved_godkjent_terminbekreftelse() {
        // Arrange
        var opprinneligTermindato = LocalDate.now();
        var opprinneligUtstedtDato = LocalDate.now().minusDays(20);
        var opprinneligAntallBarn = 1;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(opprinneligTermindato)
                .medUtstedtDato(opprinneligUtstedtDato)
                .medNavnPå("LEGEN MIN"))
            .medAntallBarn(opprinneligAntallBarn);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);
        // Dto
        var dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse",
            opprinneligTermindato, opprinneligUtstedtDato, opprinneligAntallBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdaterer = new BekreftTerminbekreftelseOppdaterer(historikkRepository, mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag2.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(2);
        assertThat(historikkinnslag.getTekstlinjer().get(0).getTekst()).contains("Terminbekreftelse", "godkjent");
        assertThat(historikkinnslag.getTekstlinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_oppdatere_terminbekreftelse() {
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
        var dto = new BekreftTerminbekreftelseAksjonspunktDto(
            "Begrunnelse", now.plusDays(30), now.minusDays(3), 1);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdaterer = new BekreftTerminbekreftelseOppdaterer(historikkRepository, mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var familieHendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());

        var terminbekreftelse = familieHendelseGrunnlag.getGjeldendeVersjon().getTerminbekreftelse();

        assertThat(terminbekreftelse).isPresent();
        assertThat(familieHendelseGrunnlag.getGjeldendeVersjon().getAntallBarn()).isEqualTo(1);
        assertThat(terminbekreftelse.get().getTermindato()).isEqualTo(now.plusDays(30));
        assertThat(terminbekreftelse.get().getUtstedtdato()).isEqualTo(now.minusDays(3));
    }

    @Test
    void skal_trigge_totrinnsbehandling_når_aksjonspunkt_bekreftes_på_nytt_etter_tilbakehopp() {
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
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon()).get();
        // Act
        var oppdaterer = new BekreftTerminbekreftelseOppdaterer(historikkRepository, mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste);

        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert - sjekk at totrinnsbehandling blir satt før tilbakehopp
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();

        // Simuler at aksjonspunkt resettes ved tilbakehopp
        AksjonspunktTestSupport.fjernToTrinnsBehandlingKreves(aksjonspunkt);

        // Act
        oppdaterer = new BekreftTerminbekreftelseOppdaterer(historikkRepository, mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste);

        var oppdateringResultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto,
            aksjonspunkt));

        // Assert - sjekk at totrinnsbehandling blir satt etter tilbakehopp
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
    }
}
