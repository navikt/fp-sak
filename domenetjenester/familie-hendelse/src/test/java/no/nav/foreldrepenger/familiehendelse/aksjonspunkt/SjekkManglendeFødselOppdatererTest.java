package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFødselAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.foreldrepenger.familiehendelse.modell.FødselStatus;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ExtendWith(MockitoExtension.class)
class SjekkManglendeFødselOppdatererTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FamiliehendelseEventPubliserer familiehendelseEventPubliserer;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(familiehendelseEventPubliserer, repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    void skal_avklare_at_manglende_fødsel_ikke_kan_dokumenters() {
        // Arrange
        var antallBarnSøknad = 1;
        var fødselsdatoFraSøknad = now();

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad).medAntallBarn(antallBarnSøknad);

        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);

        // Dto
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", false, null);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var familieHendelseSamling = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(familieHendelseSamling).satisfies(h -> {
            assertThat(h.getSøknadVersjon()).satisfies(s -> {
                assertThat(s.getAntallBarn()).isEqualTo(antallBarnSøknad);
                assertThat(s.getBarna()).hasSize(antallBarnSøknad).map(UidentifisertBarn::getFødselsdato).containsExactly(fødselsdatoFraSøknad);
            });
            assertThat(h.getBekreftetVersjon()).isEmpty();
            assertThat(h.getOverstyrtVersjon()).isEmpty();
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ er satt til __Nei__.", "begrunnelse.");

    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_fødsel_ikke_er_registrert_i_freg() {
        // Arrange
        var antallBarnSøknad = 1;
        var fødselsdatoFraSøknad = now().minusDays(1);

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad).medAntallBarn(antallBarnSøknad);

        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);

        // Dto
        var barn = List.of(new DokumentertBarnDto(fødselsdatoFraSøknad, null));
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, barn);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(antallBarnSøknad);
            assertThat(h.getBarna()).hasSize(antallBarnSøknad).map(UidentifisertBarn::getFødselsdato).containsExactly(fødselsdatoFraSøknad);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __1__.", "begrunnelse.");

    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_søknad_og_freg_har_forskjellig_antall_barn() {
        // Arrange
        var antallBarnSøknad = 3;
        var antallBarnFReg = 2;

        var fødselsdatoFraPDL = now().minusDays(1);
        var fødselsdatoFraSøknad = now().minusDays(10);

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad, antallBarnSøknad).medAntallBarn(antallBarnSøknad);
        scenario.medBekreftetHendelse()
            .tilbakestillBarn()
            .leggTilBarn(fødselsdatoFraPDL)
            .leggTilBarn(fødselsdatoFraPDL)
            .medAntallBarn(antallBarnFReg);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);

        // Dto
        var barn = List.of(new DokumentertBarnDto(fødselsdatoFraPDL, null), new DokumentertBarnDto(fødselsdatoFraPDL, null));
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, barn);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(antallBarnFReg);
            assertThat(h.getBarna()).hasSize(antallBarnFReg)
                .map(UidentifisertBarn::getFødselsdato)
                .containsExactly(fødselsdatoFraPDL, fødselsdatoFraPDL);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __2__.", "begrunnelse.");
    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_barn_ikke_eksisterer_i_freg_og_sbh_oppgir_fler_barn_enn_det_er_søkt_om() {
        // Arrange
        var opprinneligFødseldato = now();
        var avklartFødseldato = opprinneligFødseldato.plusDays(1);
        var antallBarnSøknad = 1;
        var antallBarnSBH = 2;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(opprinneligFødseldato).medAntallBarn(antallBarnSøknad);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var barn = List.of(new DokumentertBarnDto(avklartFødseldato, null), new DokumentertBarnDto(avklartFødseldato, null));
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, barn);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(antallBarnSBH);
            assertThat(h.getBarna()).hasSize(antallBarnSBH)
                .map(UidentifisertBarn::getFødselsdato)
                .containsExactly(avklartFødseldato, avklartFødseldato);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ er satt til __Ja__.",
            "__Antall barn__ er endret fra 1 til __2__.",
            String.format("__Barn 1__ er endret fra f. %s til __f. %s__.", format(opprinneligFødseldato), format(avklartFødseldato)),
            String.format("__Barn 2__ er satt til __f. %s__.", format(avklartFødseldato)), "begrunnelse.");

    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_barn_ikke_eksisterer_i_freg_og_barn_er_død() {
        // Arrange
        var fødselsdatoFraSøknad = now();
        var dødsdatoFraSBH = now();
        var antallBarnSøknad = 1;

        // Behandling
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad).medAntallBarn(antallBarnSøknad);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var barn = List.of(new DokumentertBarnDto(fødselsdatoFraSøknad, dødsdatoFraSBH));

        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, barn);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).first().satisfies(b -> {
                assertThat(b.getFødselsdato()).isEqualTo(fødselsdatoFraSøknad);
                assertThat(b.getDødsdato()).hasValue(dødsdatoFraSBH);
            });
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __1__.",
            String.format("__Barn__ er endret fra f. %s til __f. %s - d. %s__.", format(fødselsdatoFraSøknad), format(fødselsdatoFraSøknad),
                format(dødsdatoFraSBH)), "begrunnelse.");

    }

    @Test
    void skal_oppdatere_fødsel_13m_gir_oppdater_grunnlag() {
        var fødselsdatoFraSøknad = now().minusDays(3);
        var fødselsdatoFraSBH = now().minusMonths(13);

        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now()).build();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var barn = List.of(new DokumentertBarnDto(fødselsdatoFraSBH, null));

        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, barn);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        // Act
        var resultat = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(resultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);

        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).map(UidentifisertBarn::getFødselsdato).containsExactly(fødselsdatoFraSBH);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ er satt til __Ja__.",
            "__Antall barn__ som brukes i behandlingen: __1__.",
            String.format("__Barn__ er endret fra f. %s til __f. %s__.", format(fødselsdatoFraSøknad), format(fødselsdatoFraSBH)), "begrunnelse.");

    }

    @Test
    void skal_oppdatere_antall_barn_basert_på_saksbehandlers_oppgitte_antall() {
        var fødselsdatoFraSøknad = now().minusDays(3);

        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now());
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad, 2).medAntallBarn(2);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var barn = List.of(new DokumentertBarnDto(fødselsdatoFraSøknad, null));
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, barn);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        // Act
        new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).map(UidentifisertBarn::getFødselsdato).first().isEqualTo(fødselsdatoFraSøknad);
        });

        var historikkinnslag2 = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag2.getTekstLinjer()).containsExactly("__Er barnet født?__ er satt til __Ja__.",
            "__Antall barn__ er endret fra 2 til __1__.", String.format("__Barn 2__ __f. %s__ er fjernet.", format(fødselsdatoFraSøknad)), "begrunnelse.");

    }

    @Test
    void skal_hive_exception_når_dto_inneholder_feil() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now());
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(now());
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);

        var oppdaterer = new SjekkManglendeFødselOppdaterer(mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste,
            repositoryProvider.getHistorikkinnslagRepository(), repositoryProvider.getBehandlingRepository());

        var dtoManglerBarn = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, List.of());
        var dtoForMangeBarn = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, Collections.nCopies(10, new DokumentertBarnDto(now(), null)));
        var dtoDødFørFødsel = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", true, List.of(new DokumentertBarnDto(now(), now().minusDays(1))));

        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dtoManglerBarn, aksjonspunkt);
        assertThatExceptionOfType(FunksjonellException.class).isThrownBy(() -> oppdaterer.oppdater(dtoManglerBarn, param))
            .withMessage("FP-076343:Mangler barn");
        assertThatExceptionOfType(FunksjonellException.class).isThrownBy(() -> oppdaterer.oppdater(dtoForMangeBarn, param))
            .withMessage("FP-076347:For mange barn");
        assertThatExceptionOfType(FunksjonellException.class).isThrownBy(() -> oppdaterer.oppdater(dtoDødFørFødsel, param))
            .withMessage("FP-076345:Dødsdato før fødselsdato");
    }

    @Test
    void skal_sortere_barn_på_fødselstatus() {

        var barn1 = Stream.of(lagFødselStatus(now(), null), lagFødselStatus(now().minusDays(1), now()), lagFødselStatus(now().minusDays(1), null),
            lagFødselStatus(now().minusDays(2), null), lagFødselStatus(now().plusDays(33), null)).sorted().toList();

        var barn2 = Stream.of(lagFødselStatus(now(), null), lagFødselStatus(now().minusDays(1), null), lagFødselStatus(now().minusDays(1), now()),
            lagFødselStatus(now().minusDays(2), null), lagFødselStatus(now().plusDays(33), null)).sorted().toList();

        assertThat(barn1).isEqualTo(barn2);
    }

    private FødselStatus lagFødselStatus(LocalDate fødselsdato, LocalDate dødsdato) {
        return new FødselStatus(new DokumentertBarnDto(fødselsdato, dødsdato));
    }
}
