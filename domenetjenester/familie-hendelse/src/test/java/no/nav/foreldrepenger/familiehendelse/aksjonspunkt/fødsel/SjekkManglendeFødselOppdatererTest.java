package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.SjekkManglendeFødselAksjonspunktDto;
import no.nav.vedtak.exception.FunksjonellException;

@CdiDbAwareTest
class SjekkManglendeFødselOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FaktaFødselTjeneste faktaFødselTjeneste;
    @Inject
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private SjekkManglendeFødselOppdaterer oppdaterer;

    @BeforeEach
    void setUp() {
        this.oppdaterer = new SjekkManglendeFødselOppdaterer(familieHendelseTjeneste, faktaFødselTjeneste);
    }

    @Test
    void skal_avklare_at_manglende_fødsel_ikke_kan_dokumenters() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var antallBarnSøknad = 1;
        var fødselsdatoFraSøknad = now();
        var famHendelseBuilder = scenario.medSøknadHendelse();
        famHendelseBuilder.medTerminbekreftelse(famHendelseBuilder.getTerminbekreftelseBuilder().medTermindato(fødselsdatoFraSøknad))
            .medFødselsDato(fødselsdatoFraSøknad)
            .medAntallBarn(antallBarnSøknad);
        var behandling = scenario.lagre(repositoryProvider);
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", null);
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var familieHendelseSamling = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(familieHendelseSamling).satisfies(h -> {
            assertThat(h.getSøknadVersjon()).satisfies(s -> {
                assertThat(s.getAntallBarn()).isEqualTo(antallBarnSøknad);
                assertThat(s.getBarna()).hasSize(antallBarnSøknad).map(UidentifisertBarn::getFødselsdato).containsExactly(fødselsdatoFraSøknad);
            });
            assertThat(h.getBekreftetVersjon()).isEmpty();
            assertThat(h.getOverstyrtVersjon()).hasValueSatisfying(o -> {
                assertThat(o.getAntallBarn()).isEqualTo(antallBarnSøknad);
                assertThat(o.getBarna()).isEmpty();
                assertThat(o.getType()).isEqualTo(FamilieHendelseType.TERMIN);
            });
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ Nei.", "begrunnelse.");
    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_fødsel_ikke_er_registrert_i_freg() {
        var antallBarnSøknad = 1;
        var fødselsdatoFraSøknad = now().minusDays(1);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad).medAntallBarn(antallBarnSøknad);
        var behandling = scenario.lagre(repositoryProvider);
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", List.of(new DokumentertBarnDto(fødselsdatoFraSøknad, null)));
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(antallBarnSøknad);
            assertThat(h.getBarna()).hasSize(antallBarnSøknad).map(UidentifisertBarn::getFødselsdato).containsExactly(fødselsdatoFraSøknad);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ Ja.", "__Antall barn:__ 1.", "begrunnelse.");
    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_søknad_og_freg_har_forskjellig_antall_barn() {
        var antallBarnSøknad = 3;
        var antallBarnFReg = 2;
        var fødselsdatoFraPDL = now().minusDays(1);
        var fødselsdatoFraSøknad = now().minusDays(10);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad, antallBarnSøknad).medAntallBarn(antallBarnSøknad);
        scenario.medBekreftetHendelse()
            .tilbakestillBarn()
            .leggTilBarn(fødselsdatoFraPDL)
            .leggTilBarn(fødselsdatoFraPDL)
            .medAntallBarn(antallBarnFReg);
        var behandling = scenario.lagre(repositoryProvider);
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse",
            List.of(new DokumentertBarnDto(fødselsdatoFraPDL, null), new DokumentertBarnDto(fødselsdatoFraPDL, null)));
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(antallBarnFReg);
            assertThat(h.getBarna()).hasSize(antallBarnFReg)
                .map(UidentifisertBarn::getFødselsdato)
                .containsExactly(fødselsdatoFraPDL, fødselsdatoFraPDL);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ Ja.", "__Antall barn:__ 2.", "begrunnelse.");
    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_barn_ikke_eksisterer_i_freg_og_sbh_oppgir_fler_barn_enn_det_er_søkt_om() {
        var opprinneligFødseldato = now();
        var avklartFødseldato = opprinneligFødseldato.plusDays(1);
        var antallBarnSøknad = 1;
        var antallBarnSBH = 2;
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medSøknadHendelse().medFødselsDato(opprinneligFødseldato).medAntallBarn(antallBarnSøknad);
        scenario.lagre(repositoryProvider);
        var behandling = scenario.getBehandling();
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse",
            List.of(new DokumentertBarnDto(avklartFødseldato, null), new DokumentertBarnDto(avklartFødseldato, null)));
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(antallBarnSBH);
            assertThat(h.getBarna()).hasSize(antallBarnSBH)
                .map(UidentifisertBarn::getFødselsdato)
                .containsExactly(avklartFødseldato, avklartFødseldato);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ Ja.", "__Antall barn__ er endret fra 1 til __2__.",
            String.format("__Barn 1__ er endret fra f. %s til __f. %s__.", format(opprinneligFødseldato), format(avklartFødseldato)),
            String.format("__Barn 2__ er satt til __f. %s__.", format(avklartFødseldato)), "begrunnelse.");
    }

    @Test
    void skal_avklare_manglende_fødsel_hvor_barn_ikke_eksisterer_i_freg_og_barn_er_død() {
        var fødselsdatoFraSøknad = now();
        var dødsdatoFraSBH = now();
        var antallBarnSøknad = 1;
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad).medAntallBarn(antallBarnSøknad);
        scenario.lagre(repositoryProvider);
        var behandling = scenario.getBehandling();
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", List.of(new DokumentertBarnDto(fødselsdatoFraSøknad, dødsdatoFraSBH)));
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).first().satisfies(b -> {
                assertThat(b.getFødselsdato()).isEqualTo(fødselsdatoFraSøknad);
                assertThat(b.getDødsdato()).hasValue(dødsdatoFraSBH);
            });
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ Ja.", "__Antall barn:__ 1.",
            String.format("__Barn__ er endret fra f. %s til __f. %s - d. %s__.", format(fødselsdatoFraSøknad), format(fødselsdatoFraSøknad),
                format(dødsdatoFraSBH)), "begrunnelse.");
    }

    @Test
    void skal_oppdatere_fødsel_13m_gir_oppdater_grunnlag() {
        var fødselsdatoFraSøknad = now().minusDays(3);
        var fødselsdatoFraSBH = now().minusMonths(13);
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now()).build();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", List.of(new DokumentertBarnDto(fødselsdatoFraSBH, null)));
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        var resultat = new SjekkManglendeFødselOppdaterer(familieHendelseTjeneste, faktaFødselTjeneste).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        assertThat(resultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);

        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).map(UidentifisertBarn::getFødselsdato).containsExactly(fødselsdatoFraSBH);
        });

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getTekstLinjer()).containsExactly("__Er barnet født?__ Ja.", "__Antall barn:__ 1.",
            String.format("__Barn__ er endret fra f. %s til __f. %s__.", format(fødselsdatoFraSøknad), format(fødselsdatoFraSBH)), "begrunnelse.");

    }

    @Test
    void skal_oppdatere_antall_barn_basert_på_saksbehandlers_oppgitte_antall() {
        var fødselsdatoFraSøknad = now().minusDays(3);
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now());
        scenario.medSøknadHendelse().medFødselsDato(fødselsdatoFraSøknad, 2).medAntallBarn(2);
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagre(repositoryProvider);
        var dto = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", List.of(new DokumentertBarnDto(fødselsdatoFraSøknad, null)));
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull().satisfies(h -> {
            assertThat(h.getAntallBarn()).isEqualTo(1);
            assertThat(h.getBarna()).hasSize(1).map(UidentifisertBarn::getFødselsdato).first().isEqualTo(fødselsdatoFraSøknad);
        });

        var historikkinnslag2 = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag2.getTekstLinjer()).containsExactly("__Er barnet født?__ Ja.", "__Antall barn__ er endret fra 2 til __1__.",
            String.format("__Barn 2__ __f. %s__ er fjernet.", format(fødselsdatoFraSøknad)), "begrunnelse.");

    }

    @Test
    void skal_hive_exception_når_dto_inneholder_feil() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medSøknad().medSøknadsdato(now());
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(now());
        var behandling = scenario.lagre(repositoryProvider);
        var dtoDødFørFødsel = new SjekkManglendeFødselAksjonspunktDto("begrunnelse", List.of(new DokumentertBarnDto(now(), now().minusDays(1))));
        var aksjonspunkt = behandling.getAksjonspunktFor(SJEKK_MANGLENDE_FØDSEL);
        var ref = BehandlingReferanse.fra(behandling);

        var param = new AksjonspunktOppdaterParameter(ref, dtoDødFørFødsel, aksjonspunkt);
        assertThatExceptionOfType(FunksjonellException.class).isThrownBy(
                () -> oppdaterer.oppdater(dtoDødFørFødsel, param))
            .withMessage("FP-076345: Dødsdato før fødselsdato");
    }
}
