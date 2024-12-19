package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Linje;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFodselDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ExtendWith(MockitoExtension.class)
class SjekkManglendeFødselOppdatererTest extends EntityManagerAwareTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;

    private final LocalDate now = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FamiliehendelseEventPubliserer familiehendelseEventPubliserer;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(familiehendelseEventPubliserer, repositoryProvider.getFamilieHendelseRepository());

    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_antall_barn() {
        // Arrange
        var antallBarnOpprinnelig = 2;
        var antallBarnPDLGjelderBekreftet = false;

        var fødselsdatoFraPDL = LocalDate.now().minusDays(1);
        var fødselsDatoFraSøknad = fødselsdatoFraPDL.minusDays(10);

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDatoFraSøknad, antallBarnOpprinnelig).medAntallBarn(antallBarnOpprinnelig);
        scenario.medBekreftetHendelse()
            .tilbakestillBarn()
            .leggTilBarn(fødselsdatoFraPDL)
            .leggTilBarn(fødselsdatoFraPDL)
            .leggTilBarn(fødselsdatoFraPDL)
            .medAntallBarn(3);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);
        // Dto
        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(fødselsDatoFraSøknad, null), new UidentifisertBarnDto(
            fødselsDatoFraSøknad, null)};

        // Dto
        var dto = new SjekkManglendeFodselDto("begrunnelse", true, antallBarnPDLGjelderBekreftet, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act

        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();

        // Assert
        assertThat(historikkinnslag.getLinjer().stream().map(Historikkinnslag2Linje::getTekst)).anyMatch(
            t -> t.contains("Bruk antall fra søknad"));
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_fødsel() {
        // Arrange
        var opprinneligFødseldato = LocalDate.now();
        var avklartFødseldato = opprinneligFødseldato.plusDays(1);
        var opprinneligAntallBarn = 1;
        var avklartAntallBarn = 2;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(opprinneligFødseldato).medAntallBarn(opprinneligAntallBarn);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(avklartFødseldato, null), new UidentifisertBarnDto(
            avklartFødseldato, null)};

        // Dto

        var dto = new SjekkManglendeFodselDto("begrunnelse", true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();

        // Assert
        assertThat(historikkinnslag.getLinjer().stream().map(Historikkinnslag2Linje::getTekst)).anyMatch(
                t -> t.contains("Fødselsdato") && t.contains(format(opprinneligFødseldato)) && t.contains(format(avklartFødseldato)))
            .anyMatch(t -> t.contains("Antall barn") && t.contains(String.valueOf(avklartAntallBarn)))
            .anyMatch(t -> t.contains(dto.getBegrunnelse()));
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_manglende_fødsel_med_død() {
        // Arrange
        var opprinneligFødseldato = LocalDate.now();
        var avklartDødsdato = opprinneligFødseldato.plusDays(1);
        var opprinneligAntallBarn = 1;
        var avklartAntallBarn = 1;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(opprinneligFødseldato).medAntallBarn(opprinneligAntallBarn);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        var uidentifisertBarnFeil = new UidentifisertBarnDto[]{new UidentifisertBarnDto(opprinneligFødseldato, opprinneligFødseldato.minusDays(2))};
        var uidentifisertBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(opprinneligFødseldato, avklartDødsdato)};

        // Dto

        var dtoFeil = new SjekkManglendeFodselDto("begrunnelse", true, false, List.of(uidentifisertBarnFeil));
        var dto = new SjekkManglendeFodselDto("begrunnelse", true, false, List.of(uidentifisertBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Ulovlig dato
        assertThrows(FunksjonellException.class,
            () -> new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
                repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dtoFeil,
                new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dtoFeil, aksjonspunkt)));

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();

        // Assert
        assertThat(historikkinnslag.getLinjer().stream().map(Historikkinnslag2Linje::getTekst)).anyMatch(
                t -> t.contains("Dødsdato") && t.contains(format(avklartDødsdato)))
            .anyMatch(t -> t.contains("Antall barn") && t.contains(String.valueOf(avklartAntallBarn)))
            .anyMatch(t -> t.contains(dto.getBegrunnelse()));
    }

    @Test
    void skal_oppdatere_fødsel() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(now.minusDays(3)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(now.minusDays(6), null)};

        var dto = new SjekkManglendeFodselDto("Begrunnelse", true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(1);
        var fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(now.minusDays(6)));
    }

    @Test
    void skal_oppdatere_fødsel_13m_gir_oppdater_grunnlag() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(now.minusDays(3)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(now.minusMonths(13), null)};

        var dto = new SjekkManglendeFodselDto("Begrunnelse", true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(resultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(1);
        var fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(now.minusMonths(13)));
    }

    @Test
    void skal_trigge_totrinnskontroll_dersom_steget_utføres_to_ganger() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(now.minusDays(3)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(now.minusDays(6), null)};

        var dto = new SjekkManglendeFodselDto("Begrunnelse", true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();

        // Simuler at aksjonspunkt resettes
        AksjonspunktTestSupport.fjernToTrinnsBehandlingKreves(aksjonspunkt);

        // Act
        var oppdateringResultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_oppdatere_antall_barn_basert_på_saksbehandlers_oppgitte_antall() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(now.minusDays(3), 2).medAntallBarn(2);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(now.minusDays(3), null)};
        var dto = new SjekkManglendeFodselDto("Begrunnelse", true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(1);
        var fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(now.minusDays(3)));
    }

    @Test
    void skal_oppdatere_antall_barn_basert_på_pdl_dersom_flagg_satt() {
        // Arrange
        var fødselsdatoFraPDL = now.minusDays(1);

        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medAntallBarn(2).medFødselsDato(now, 2);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        scenario.medBekreftetHendelse()
            .tilbakestillBarn()
            .leggTilBarn(fødselsdatoFraPDL)
            .leggTilBarn(fødselsdatoFraPDL)
            .leggTilBarn(fødselsdatoFraPDL)
            .medAntallBarn(3);
        var behandling = scenario.lagre(repositoryProvider);
        var dto = new SjekkManglendeFodselDto("Begrunnelse", true, true, new ArrayList<UidentifisertBarnDto>());
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(3);
        var fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(fødselsdatoFraPDL));
    }

    @Test
    void skal_hive_exception_når_dokumentasjon_foreligger_og_fødselsdato_er_tom() {
        // Arrange

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(now);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(null, null)};

        var dto = new SjekkManglendeFodselDto("Begrunnelse", true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        var oppdaterer = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslag2Repository());
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt);
        assertThrows(KanIkkeUtledeGjeldendeFødselsdatoException.class, () -> oppdaterer.oppdater(dto, param));
    }

}
