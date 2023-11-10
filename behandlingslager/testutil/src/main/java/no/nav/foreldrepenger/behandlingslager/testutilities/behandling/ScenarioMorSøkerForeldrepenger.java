package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;

/**
 * Default test scenario builder for Mor søker Foreldrepenger. Kan opprettes for fødsel og brukes til å
 * opprette standard scenarioer.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne klassen.
 */
public class ScenarioMorSøkerForeldrepenger extends AbstractTestScenario<ScenarioMorSøkerForeldrepenger> {

    private ScenarioMorSøkerForeldrepenger() {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
        settDefaultSøknad();

    }

    private ScenarioMorSøkerForeldrepenger(AktørId aktørId) {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, aktørId);
        settDefaultSøknad();
    }

    private ScenarioMorSøkerForeldrepenger(NavBruker navBruker) {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.MORA, navBruker);
        settDefaultSøknad();
    }

    private void settDefaultSøknad() {
            medSøknad()
                .medRelasjonsRolleType(RelasjonsRolleType.MORA)
                .medSøknadsdato(LocalDate.now());
    }

    public static ScenarioMorSøkerForeldrepenger forFødsel() {
        return new ScenarioMorSøkerForeldrepenger();
    }

    public static ScenarioMorSøkerForeldrepenger forFødselUtenSøknad(AktørId aktørId) {
        var scenario = new ScenarioMorSøkerForeldrepenger(aktørId);
        scenario.utenSøknad();
        return scenario;
    }

    public static ScenarioMorSøkerForeldrepenger forFødselMedGittAktørId(AktørId aktørId) {
        return new ScenarioMorSøkerForeldrepenger(aktørId);
    }

    public static ScenarioMorSøkerForeldrepenger forFødselMedGittBruker(NavBruker navBruker) {
        return new ScenarioMorSøkerForeldrepenger(navBruker);
    }

    public static ScenarioMorSøkerForeldrepenger forAdopsjon() {
        return new ScenarioMorSøkerForeldrepenger();
    }

    public ScenarioMorSøkerForeldrepenger medDefaultFordeling(LocalDate førsteuttaksdag) {
        var førFødsel = førFødsel(førsteuttaksdag);
        var mødreKvote = mødrekvote(førsteuttaksdag);
        medFordeling(new OppgittFordelingEntitet(List.of(førFødsel, mødreKvote), true, false));
        return this;
    }

    private OppgittPeriodeEntitet førFødsel(LocalDate førsteuttaksdag) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(førsteuttaksdag, førsteuttaksdag.plusWeeks(3).minusDays(1))
            .build();
    }

    private OppgittPeriodeEntitet mødrekvote(LocalDate førsteuttaksdag) {
        return OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(førsteuttaksdag.plusWeeks(3), førsteuttaksdag.plusWeeks(9).minusDays(1))
                .build();
    }

}
