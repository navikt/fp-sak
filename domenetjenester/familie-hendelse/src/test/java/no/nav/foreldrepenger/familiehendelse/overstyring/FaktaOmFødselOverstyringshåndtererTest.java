package no.nav.foreldrepenger.familiehendelse.overstyring;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FaktaFødselTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OverstyringFaktaOmFødselDto;

@CdiDbAwareTest
class FaktaOmFødselOverstyringshåndtererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private HistorikkinnslagRepository historikkinnslagRepository;
    @Inject
    private FaktaFødselTjeneste faktaFødselTjeneste;
    @Inject
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private FaktaOmFødselOverstyringshåndterer oppdaterer;

    @BeforeEach
    void setUp() {
        oppdaterer = new FaktaOmFødselOverstyringshåndterer(historikkinnslagRepository, faktaFødselTjeneste, familieHendelseTjeneste);
    }

    @Test
    void skal_oppdatere_termindato() {
        // Arrange
        var termindato = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var behandling = scenario.lagre(repositoryProvider);

        var ref = BehandlingReferanse.fra(behandling);
        var termindatoFraDto = LocalDate.now().plusDays(1);
        var begrunnelse = "Endrer termindato på grunn av feil i søknad.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindatoFraDto, null);

        // Act
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato).orElseThrow()).as(
            "Termindato skal være oppdatert").isEqualTo(termindatoFraDto);
        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.",
                "__Termindato__ er endret fra " + format(termindato) + " til __" + format(termindatoFraDto) + "__.", "__Er barnet født?__ Nei.",
                begrunnelse);
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_ett_barn_legges_til() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelse(scenario, termindato, fødselsdato);

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        scenario.medOverstyrtHendelse().medAntallBarn(1).leggTilBarn(fødselsdato); // overstyrt versjon inneholder 1 barn fra bekreftet hendelse

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        // barnDtoListe inneholder gjeldende barn fra register i tillegg til det som skal legges til. Dette vil overstyre fra overstyrt versjon som inneholder 1 barn fra bekreftet hendelse.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, null));
        var begrunnelse = "Legger til ekstra barn i overstyring.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();


        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn før overstyring")
            .isEqualTo(1);
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn etter overstyring")
            .isEqualTo(2);

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
                "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                    + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
                "__Barn 2__ er satt til __f. " + format(fødselsdato) + "__.", begrunnelse);
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_tre_barn_legges_til_med_to_forskjellige_fødselsdatoer() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();
        var fødselsdato2 = fødselsdato.plusDays(1);

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelse(scenario, termindato, fødselsdato);

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        scenario.medOverstyrtHendelse().medAntallBarn(1).leggTilBarn(fødselsdato); // overstyrt versjon inneholder 1 barn fra bekreftet hendelse

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        // barnDtoListe inneholder gjeldende barn fra register i tillegg til det som skal legges til. Det vil si 1 barn fra bekreftet hendelse og 3 barn som skal legges til.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, null),
            new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato2, null));
        var begrunnelse = "Legger til ekstra barn i overstyring.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn før overstyring")
            .isEqualTo(1);
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn etter overstyring")
            .isEqualTo(4);

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
                "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                    + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
                "__Barn 2__ er satt til __f. " + format(fødselsdato) + "__.", "__Barn 3__ er satt til __f. " + format(fødselsdato) + "__.",
                "__Barn 4__ er satt til __f. " + format(fødselsdato2) + "__.", begrunnelse);
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_to_barn_legges_til_hvor_to_er_i_overstyrt_fra_før() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();
        var fødselsdato2 = fødselsdato.plusDays(1);

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelse(scenario, termindato, fødselsdato);

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        scenario.medOverstyrtHendelse()
            .medAntallBarn(2)
            .leggTilBarn(fødselsdato)
            .leggTilBarn(fødselsdato); // overstyrt versjon inneholder 1 barn fra bekreftet hendelse

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        // barnDtoListe inneholder gjeldende barn fra register i tillegg til det som skal legges til. Det vil si 1 barn fra bekreftet hendelse og 3 barn som skal legges til.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, null),
            new DokumentertBarnDto(fødselsdato, fødselsdato), new DokumentertBarnDto(fødselsdato2, null));
        var begrunnelse = "Legger til ekstra barn i overstyring.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn før overstyring")
            .isEqualTo(2);
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn etter overstyring")
            .isEqualTo(4);

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
                "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                    + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
                "__Barn 3__ er satt til __f. " + format(fødselsdato) + " - d. " + format(fødselsdato) + "__.",
                "__Barn 4__ er satt til __f. " + format(fødselsdato2) + "__.", begrunnelse);
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_barn_fjernes() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelse(scenario, termindato, fødselsdato);

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        // Overstyrt versjon inneholder 1 barn fra bekreftet hendelse og det som skal fjernes. Dersom det kun inneholder 1 barn fra bekreftet hendelse, blir de andre barna fjernet fra overstyring.
        scenario.medOverstyrtHendelse().medAntallBarn(3).leggTilBarn(fødselsdato).leggTilBarn(fødselsdato).leggTilBarn(fødselsdato);

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        // barnDtoListe inneholder gjeldende barn fra register og fra overstyrt. Denne vil overstyre fra overstyrt versjon som inneholder 3 barn (1 barn fra register og 2 barn fra overstyring).
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null));
        var begrunnelse = "Fjernet 2 barn fra overstyring.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn før overstyring")
            .isEqualTo(3);
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn etter overstyring")
            .isEqualTo(1);

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
                "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                    + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
                "__Barn 2__ __f. " + format(fødselsdato) + "__ er fjernet.", "__Barn 3__ __f. " + format(fødselsdato) + "__ er fjernet.",
                begrunnelse);
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_tre_barn_fjernes_med_to_forskjellige_fødselsdatoer() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();
        var fødselsdato2 = fødselsdato.plusDays(1);

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelse(scenario, termindato, fødselsdato);

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        // Overstyrt versjon inneholder 1 barn fra bekreftet hendelse og 3 andre barn som kan overstyres.
        scenario.medOverstyrtHendelse()
            .medAntallBarn(4)
            .leggTilBarn(fødselsdato)
            .leggTilBarn(fødselsdato)
            .leggTilBarn(fødselsdato2)
            .leggTilBarn(fødselsdato2);

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null));
        var begrunnelse = "Fjernet 3 barn fra overstyring.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn før overstyring")
            .isEqualTo(4);
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn etter overstyring")
            .isEqualTo(1);

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
                "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                    + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
                "__Barn 2__ __f. " + format(fødselsdato) + "__ er fjernet.", "__Barn 3__ __f. " + format(fødselsdato2) + "__ er fjernet.",
                "__Barn 4__ __f. " + format(fødselsdato2) + "__ er fjernet.", begrunnelse);

    }

    @Test
    void skal_oppdatere_og_lage_historikk_ved_endret_dødsdato() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusDays(1);

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelse(scenario, termindato, fødselsdato);

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        // Overstyrt versjon inneholder 1 barn fra bekreftet og 1 andre barn som kan overstyres.
        scenario.medOverstyrtHendelse().medAntallBarn(2).leggTilBarn(fødselsdato).leggTilBarn(fødselsdato);

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        // barnDtoListe inneholder gjeldende barn. Et barn fra bekreftet og et barn som skal overstyres.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, dødsdato));
        var begrunnelse = "Endret på dødsdato.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElseThrow()).hasSize(2)
            .satisfiesExactlyInAnyOrder(barn -> {
                assertThat(barn.getFødselsdato()).isEqualTo(fødselsdato);
                assertThat(barn.getDødsdato()).isEmpty();
            }, barn -> {
                assertThat(barn.getFødselsdato()).isEqualTo(fødselsdato);
                assertThat(barn.getDødsdato()).contains(dødsdato);
            });

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.", "__Antall barn:__ " + barnDtoListe.size() + ".",
                "__Barn 2__ er endret fra f. " + format(fødselsdato) + " til __f. " + format(fødselsdato) + " - d. " + format(dødsdato) + "__.",
                begrunnelse);
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_barn_fjernes_og_legges_til_et_nytt_barn_samtidig() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();
        var fødselsdato2 = fødselsdato.plusDays(1);

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelse(scenario, termindato, fødselsdato);

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        // Overstyrt versjon inneholder 1 barn fra bekreftet hendelse og 1 barn som kan overstyres.
        scenario.medOverstyrtHendelse().medAntallBarn(2).leggTilBarn(fødselsdato).leggTilBarn(fødselsdato);

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        // barnDtoListe inneholder gjeldende barn fra register. Barn med fødselsdato er fjernet og barn med fødselsdato2 er lagt til.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato2, fødselsdato2));
        var begrunnelse = "Fjernet 1 barn med fødselsdato og lagt til 1 barn med fødselsdato2.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElseThrow()).as("Barn før overstyring")
            .hasSize(2)
            .satisfiesExactlyInAnyOrder(barn -> {
                assertThat(barn.getFødselsdato()).isEqualTo(fødselsdato);
                assertThat(barn.getDødsdato()).isEmpty();
            }, barn -> {
                assertThat(barn.getFødselsdato()).isEqualTo(fødselsdato);
                assertThat(barn.getDødsdato()).isEmpty();
            });

        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElseThrow()).as(
            "Barn etter overstyring hvor barn2 har fødselsdato2 med dødsdato satt").hasSize(2).satisfiesExactlyInAnyOrder(barn -> {
            assertThat(barn.getFødselsdato()).isEqualTo(fødselsdato);
            assertThat(barn.getDødsdato()).isEmpty();
        }, barn -> {
            assertThat(barn.getFødselsdato()).isEqualTo(fødselsdato2);
            assertThat(barn.getDødsdato()).contains(fødselsdato2);
        });

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsExactly("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.", "__Antall barn:__ " + barnDtoListe.size() + ".",
                "__Barn 2__ er endret fra f. " + format(fødselsdato) + " til __f. " + format(fødselsdato2) + " - d. " + format(fødselsdato2) + "__.",
                begrunnelse);
    }

    private void byggSøknadhendelse(AbstractTestScenario<?> scenario, LocalDate termindato, LocalDate fødselsdato) {
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(termindato.minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
    }
}
