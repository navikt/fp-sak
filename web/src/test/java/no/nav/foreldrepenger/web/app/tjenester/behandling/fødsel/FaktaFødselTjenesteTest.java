package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.FødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FaktaFødselTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();
    private static final LocalDate UTSTEDTDATO = LocalDate.now().minusMonths(1);
    private BehandlingRepositoryProvider repositoryProvider;
    private FaktaFødselTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var fhTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        tjeneste = new FaktaFødselTjeneste(fhTjeneste);
    }

    @Test
    void skal_kunne_hente_fakta_om_fødsel_med_både_overstyrt_og_bekreftet_barn() {
        // Hvis det finnes ett barn i overstyrt og et annet barn i bekreftet, skal gjeldende liste inneholde begge barnene:
        // Ett barn fra overstyrt (kanOverstyres = true) og ett barn fra FREG (kanOverstyres = false).

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO, 1);

        scenario.medBekreftetHendelse()
                .medAntallBarn(1)
                .leggTilBarn(FØDSELSDATO);

        scenario.medOverstyrtHendelse()
                .medAntallBarn(1)
                .leggTilBarn(FØDSELSDATO);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.termindato())
                .extracting(FødselDto.Gjeldende.Termindato::termindato,
                        FødselDto.Gjeldende.Termindato::kilde,
                        FødselDto.Gjeldende.Termindato::kanOverstyres)
                .containsExactly(TERMINDATO, Kilde.SØKNAD, true);

        assertThat(gjeldende.barn())
                .hasSize(2)
                .extracting(
                        b -> b.barn().getFodselsdato(),
                        FødselDto.Gjeldende.Barn::kilde,
                        FødselDto.Gjeldende.Barn::kanOverstyres)
                .containsExactlyInAnyOrder(
                        tuple(FØDSELSDATO, Kilde.SAKSBEHANDLER, true),
                        tuple(FØDSELSDATO, Kilde.FOLKEREGISTER, false)
                );
    }

    @Test
    void skal_hente_fakta_om_fødsel_med_bekreftet_barn_og_overstyrt_dødfødt_barn() {
        // Hvis det finnes et dødfødt barn i overstyring og et annet levende barn i bekreftet, skal gjeldende liste inneholde begge barnene:
        // Ett barn fra overstyring (kanOverstyres = true) og ett barn fra FREG (kanOverstyres = false).
        // Her er det f.eks tvillinger som er født i utlandet, hvor én overlever og det andre barnet er dødfødt, og mor har dødsattest for det dødfødte barnet.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO, 2);

        scenario.medBruker(AktørId.dummy())
                .medSøknad()
                .medMottattDato(LocalDate.now().minusWeeks(2));

        scenario.medSøknadAnnenPart()
                .medAktørId(AktørId.dummy());

        scenario.medBekreftetHendelse()
                .medAntallBarn(1)
                .leggTilBarn(FØDSELSDATO);

        var dødsdato = LocalDate.now().plusDays(2);
        scenario.medOverstyrtHendelse()
                .medAntallBarn(1)
                .leggTilBarn(FØDSELSDATO, dødsdato); // Dødfødt barn

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        // Sjekk at termindato er korrekt og kan overstyres fra søknad
        assertThat(gjeldende.termindato())
                .extracting(FødselDto.Gjeldende.Termindato::termindato,
                        FødselDto.Gjeldende.Termindato::kilde,
                        FødselDto.Gjeldende.Termindato::kanOverstyres)
                .containsExactly(TERMINDATO, Kilde.SØKNAD, true);

        // Sjekk at utstedtdato er korrekt
        assertThat(gjeldende.utstedtdato())
                .extracting(
                        FødselDto.Gjeldende.Utstedtdato::utstedtdato,
                        FødselDto.Gjeldende.Utstedtdato::kilde
                        )
                        .containsExactly(UTSTEDTDATO, Kilde.SØKNAD);

        // Sjekk at begge barn (overstyrt og bekreftet) er med
        assertThat(gjeldende.barn()).hasSize(2);

        // Sjekk at barn har riktige verdier for fødselsdato, dødsdato, kilde og kanOverstyres
        assertThat(gjeldende.barn())
                .extracting(
                        b -> b.barn().getFodselsdato(),
                        b -> b.barn().getDodsdato(),
                        FødselDto.Gjeldende.Barn::kilde,
                        FødselDto.Gjeldende.Barn::kanOverstyres)
                .containsExactlyInAnyOrder(
                        tuple(FØDSELSDATO, dødsdato, Kilde.SAKSBEHANDLER, true),
                        tuple(FØDSELSDATO, null, Kilde.FOLKEREGISTER, false)
                );
    }

    @Test
    void skal_kunne_hente_fakta_om_fødsel_med_overstyrt_barn_termindato_og_ikke_fra_søknad() {
        // Hvis det finnes barn både i overstyrt og søknad, skal gjeldende liste kun inneholde overstyrte barn med kanOverstyres = true.
        // Dette er f.eks. tilfelle når mor søker feil antall barn og termindato. Da har saksbehandler mulighet til å overstyre til riktig antall barn og termindato.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO.minusWeeks(1), 2);

        var overstyrthendelse = scenario.medOverstyrtHendelse();
        overstyrthendelse
                .medAntallBarn(1)
                .medTerminbekreftelse(overstyrthendelse
                        .getTerminbekreftelseBuilder()
                        .medTermindato(TERMINDATO)
                        .medUtstedtDato(UTSTEDTDATO)
                        .medNavnPå("LEGEN LEGESEN"))
                .leggTilBarn(FØDSELSDATO);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.termindato())
                .extracting(
                        FødselDto.Gjeldende.Termindato::termindato,
                        FødselDto.Gjeldende.Termindato::kilde,
                        FødselDto.Gjeldende.Termindato::kanOverstyres)
                .containsExactly(TERMINDATO, Kilde.SAKSBEHANDLER, true);
        assertThat(gjeldende.barn()).hasSize(1);
        assertThat(gjeldende.barn())
                .extracting(
                        b -> b.barn().getFodselsdato(),
                        b -> b.barn().getDodsdato(),
                        FødselDto.Gjeldende.Barn::kilde,
                        FødselDto.Gjeldende.Barn::kanOverstyres
                )
                .containsExactlyInAnyOrder(
                        tuple(FØDSELSDATO, null, Kilde.SAKSBEHANDLER, true)
                );
    }

    @Test
    void skal_kunne_hente_fakta_om_fødsel_med_kun_søknadsbarn_når_kun_disse_finnes() {
        // Dersom det kun finnes barn fra søknad, skal gjeldende liste inneholde disse barna med kanOverstyres = true.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        // Søker om foreldrepenger for 2 barn med termindato, hvor ingen av barna er født ennå.
        var søknadHendelse = scenario.medSøknadHendelse();
        søknadHendelse
                .medAntallBarn(2)
                .medTerminbekreftelse(scenario.medSøknadHendelse()
                        .getTerminbekreftelseBuilder()
                        .medTermindato(TERMINDATO)
                        .medNavnPå("LEGEN LEGESEN")
                        .medUtstedtDato(UTSTEDTDATO));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.barn()).isEmpty(); // Ingen barn er født ennå, så listen skal være tom
        assertThat(gjeldende.antallBarn()).isEqualTo(2); // Mor har søkt om foreldrepenger for 2 barn, men ingen av barna er født ennå
        assertThat(gjeldende.termindato())
                .extracting(
                        FødselDto.Gjeldende.Termindato::termindato,
                        FødselDto.Gjeldende.Termindato::kilde,
                        FødselDto.Gjeldende.Termindato::kanOverstyres)
                .containsExactly(TERMINDATO, Kilde.SØKNAD, true);

        assertThat(gjeldende.utstedtdato())
                .extracting(
                        FødselDto.Gjeldende.Utstedtdato::utstedtdato,
                        FødselDto.Gjeldende.Utstedtdato::kilde
                )
                .containsExactly(UTSTEDTDATO, Kilde.SØKNAD);
    }

    @Test
    void skal_hente_fakta_om_fødsel_med_kun_bekreftet_barn_når_kun_disse_finnes() {
        // Når det finnes både bekreftede og søknadsbarn, skal gjeldende liste kun inneholde bekreftede barn med kanOverstyres = false.
        // Det skal fortsatt være mulig å legge til eller fjerne barn via overstyring.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO, 2);

        scenario.medBekreftetHendelse()
                .medAntallBarn(2)
                .leggTilBarn(FØDSELSDATO)
                .leggTilBarn(FØDSELSDATO.plusDays(1));

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.barn()).hasSize(2);
        assertThat(gjeldende.termindato())
                .extracting(
                        FødselDto.Gjeldende.Termindato::termindato,
                        FødselDto.Gjeldende.Termindato::kilde,
                        FødselDto.Gjeldende.Termindato::kanOverstyres)
                .containsExactly(TERMINDATO, Kilde.SØKNAD, true);

        assertThat(gjeldende.barn())
                .extracting(
                        b -> b.barn().getFodselsdato(),
                        b -> b.barn().getDodsdato(),
                        FødselDto.Gjeldende.Barn::kilde,
                        FødselDto.Gjeldende.Barn::kanOverstyres)
                .containsExactlyInAnyOrder(
                        tuple(FØDSELSDATO, null, Kilde.FOLKEREGISTER, false),
                        tuple(FØDSELSDATO.plusDays(1), null, Kilde.FOLKEREGISTER, false)
                );
    }

    @Test
    void skal_hente_fakta_om_fødsel_med_register_barn_og_overstyrbar_termindato() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var søknadHendelse = scenario.medSøknadHendelse();
        søknadHendelse.medAntallBarn(1)
                .medFødselsDato(FØDSELSDATO)
                .medTerminbekreftelse(scenario.medSøknadHendelse()
                        .getTerminbekreftelseBuilder()
                        .medTermindato(TERMINDATO)
                        .medNavnPå("LEGEN LEGESEN")
                        .medUtstedtDato(UTSTEDTDATO));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var bekreftetHendelse = scenario.medBekreftetHendelse();
        bekreftetHendelse.medFødselsDato(FØDSELSDATO).medAntallBarn(1);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.termindato().termindato()).isEqualTo(TERMINDATO);
        assertThat(gjeldende.termindato().kanOverstyres()).isTrue();
        assertThat(gjeldende.barn()).hasSize(1);
        assertThat(gjeldende.barn().getFirst().barn().getFodselsdato()).isEqualTo(FØDSELSDATO);
        assertThat(gjeldende.barn().getFirst().kilde()).isEqualTo(Kilde.FOLKEREGISTER);
        assertThat(gjeldende.barn().getFirst().kanOverstyres()).isFalse();
    }

    private void byggSøknadhendelseTermin(AbstractTestScenario<?> scenario, LocalDate termindato, int antallBarn) {
        var søknadshendelse = scenario.medSøknadHendelse();
        søknadshendelse.medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(UTSTEDTDATO))
                .medFødselsDato(FØDSELSDATO)
                .medAntallBarn(antallBarn);
        if (antallBarn == 2) {
            søknadshendelse.leggTilBarn(FØDSELSDATO);
        }
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
    }

}
