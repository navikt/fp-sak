package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
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
    void skal_avklare_manglende_fødsel_hvor_fødsel_ikke_er_registrert_i_freg() {
        // Arrange
        var antallBarnSøknad = 1;

        var fødselsdatoFraPDL = LocalDate.now().minusDays(1);
        var fødselsDatoFraSøknad = fødselsdatoFraPDL.minusDays(10);

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDatoFraSøknad, antallBarnSøknad).medAntallBarn(antallBarnSøknad);

        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);

        // Dto
        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(fødselsDatoFraSøknad, null));
        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly(
            "__Finnes det dokumentasjon på at barnet er født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __1__.",
            "Barn er hentet fra søknad.", "begrunnelse.");

        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();
    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_søknad_og_freg_har_forskjellig_antall_barn() {
        // Arrange
        var antallBarnOpprinnelig = 3;
        var antallBarnFReg = 2;

        var fødselsdatoFraPDL = LocalDate.now().minusDays(1);
        var fødselsDatoFraSøknad = fødselsdatoFraPDL.minusDays(10);

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDatoFraSøknad, antallBarnOpprinnelig).medAntallBarn(antallBarnOpprinnelig);
        scenario.medBekreftetHendelse()
            .tilbakestillBarn()
            .leggTilBarn(fødselsdatoFraPDL)
            .leggTilBarn(fødselsdatoFraPDL)
            .medAntallBarn(antallBarnFReg);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);

        // Dto
        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(fødselsdatoFraPDL, null), new UidentifisertBarnDto(fødselsdatoFraPDL, null));
        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Finnes det dokumentasjon på at barnet er født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __2__.", "Barn er hentet fra Folkergisteret.", "begrunnelse.");

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_barn_ikke_eksisterer_i_freg_og_sbh_oppgir_fler_barn_enn_det_er_søkt_om() {
        // Arrange
        var opprinneligFødseldato = LocalDate.now();
        var avklartFødseldato = opprinneligFødseldato.plusDays(1);
        var opprinneligAntallBarn = 1;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(opprinneligFødseldato).medAntallBarn(opprinneligAntallBarn);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(avklartFødseldato, null), new UidentifisertBarnDto(avklartFødseldato, null));
        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Finnes det dokumentasjon på at barnet er født?__ er satt til __Ja__.",
            "__Antall barn__ er endret fra 1 til __2__.",
            String.format("__Barn 1__ er endret fra f. %s til __f. %s__.", format(opprinneligFødseldato), format(avklartFødseldato)),
            String.format("__Barn 2__ er satt til __f. %s__.", format(avklartFødseldato)), "Barn er endret manuelt.", "begrunnelse.");

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_avklare_manglende_fødsel_for_uregistrert_barn_med_død() {
        // Arrange
        var fødselsdatoSøknad = LocalDate.now();
        var avklartDødsdato = fødselsdatoSøknad.plusDays(1);
        var antallBarnSøknad = 1;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoSøknad).medAntallBarn(antallBarnSøknad);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var uidentifisertBarn = List.of(new UidentifisertBarnDto(fødselsdatoSøknad, avklartDødsdato));

        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifisertBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Finnes det dokumentasjon på at barnet er født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __1__.",
            String.format("__Barn__ er endret fra f. %s til __f. %s - d. %s__.", format(fødselsdatoSøknad), format(fødselsdatoSøknad),
                format(avklartDødsdato)), "Barn er endret manuelt.", "begrunnelse.");

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_hive_exception_ved_ugyldig_dødsdato() {
        // Arrange
        var fødselsdatoSøknad = LocalDate.now();
        var antallBarnSøknad = 1;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoSøknad).medAntallBarn(antallBarnSøknad);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var uidentifisertBarnFeil = List.of(new UidentifisertBarnDto(fødselsdatoSøknad, fødselsdatoSøknad.minusDays(2)));

        var dtoFeil = new SjekkManglendeFodselDto("begrunnelse", true, uidentifisertBarnFeil);
        var aksjonspunkt = behandling.getAksjonspunktFor(dtoFeil.getAksjonspunktDefinisjon());

        // Ulovlig dato
        assertThrows(FunksjonellException.class,
            () -> new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
                repositoryProvider.getHistorikkinnslagRepository()).oppdater(dtoFeil,
                new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dtoFeil, aksjonspunkt)));
    }

    @Test
    void skal_avklare_manglende_fødsel() {
        var fødselsdatoSøknad = now.minusDays(3);
        var fødselsdatoNy = now.minusDays(6);
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoSøknad).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(fødselsdatoNy, null));

        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).map(UidentifisertBarn::getFødselsdato).first().isEqualTo(fødselsdatoNy);
        });


        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Finnes det dokumentasjon på at barnet er født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __1__.",
            String.format("__Barn__ er endret fra f. %s til __f. %s__.", format(fødselsdatoSøknad), format(fødselsdatoNy)), "Barn er endret manuelt.",
            "begrunnelse.");

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_oppdatere_fødsel_13m_gir_oppdater_grunnlag() {
        var fødselsdatoSøknad = now.minusDays(3);
        var fødselsdatoNy = now.minusMonths(13);

        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoSøknad).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(fødselsdatoNy, null));

        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(resultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).map(UidentifisertBarn::getFødselsdato).first().isEqualTo(fødselsdatoNy);
        });


        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Finnes det dokumentasjon på at barnet er født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __1__.",
            String.format("__Barn__ er endret fra f. %s til __f. %s__.", format(fødselsdatoSøknad), format(fødselsdatoNy)), "Barn er endret manuelt.",
            "begrunnelse.");

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_trigge_totrinnskontroll_dersom_steget_utføres_to_ganger() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(now.minusDays(3)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(now.minusDays(6), null));

        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();

        // Simuler at aksjonspunkt resettes
        AksjonspunktTestSupport.fjernToTrinnsBehandlingKreves(aksjonspunkt);

        // Act
        var oppdateringResultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_oppdatere_antall_barn_basert_på_saksbehandlers_oppgitte_antall() {
        var fødselsdatoSøknad = now.minusDays(3);
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoSøknad, 2).medAntallBarn(2);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(fødselsdatoSøknad, null));
        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).map(UidentifisertBarn::getFødselsdato).first().isEqualTo(fødselsdatoSøknad);
        });

        var historikkinnslag2 = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag2.getTekstLinjer()).containsExactly("__Finnes det dokumentasjon på at barnet er født?__ er satt til __Ja__.",
            "__Antall barn__ er endret fra 2 til __1__.", "Barn er endret manuelt.", "begrunnelse.");

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_hive_exception_når_dokumentasjon_foreligger_og_fødselsdato_er_tom() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(now);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var uidentifiserteBarn = List.of(new UidentifisertBarnDto(null, null));

        var dto = new SjekkManglendeFodselDto("begrunnelse", true, uidentifiserteBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        var oppdaterer = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository());
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt);
        assertThrows(KanIkkeUtledeGjeldendeFødselsdatoException.class, () -> oppdaterer.oppdater(dto, param));
    }

    @Test
    void skal_sortere_fødselstatus() {

        var fødselStatuser = List.of(lagFødselStatus(LocalDate.now(), null), lagFødselStatus(LocalDate.now().minusDays(1), LocalDate.now()),
            lagFødselStatus(LocalDate.now().minusDays(1), null), lagFødselStatus(LocalDate.now().minusDays(2), null),
            lagFødselStatus(LocalDate.now().plusDays(33), null));
        var fødselStatuser2 = List.of(lagFødselStatus(LocalDate.now(), null), lagFødselStatus(LocalDate.now().minusDays(1), null),
            lagFødselStatus(LocalDate.now().minusDays(1), LocalDate.now()), lagFødselStatus(LocalDate.now().minusDays(2), null),
            lagFødselStatus(LocalDate.now().plusDays(33), null));
        var result1 = fødselStatuser.stream().sorted().toList();
        var result2 = fødselStatuser2.stream().sorted().toList();

        assertThat(result1).isEqualTo(result2);
    }

    private SjekkManglendeFødselOppdaterer.FødselStatus lagFødselStatus(LocalDate fødselsdato, LocalDate dødsdato) {
        return new SjekkManglendeFødselOppdaterer.FødselStatus(new UidentifisertBarnDto(fødselsdato, dødsdato));
    }
}
