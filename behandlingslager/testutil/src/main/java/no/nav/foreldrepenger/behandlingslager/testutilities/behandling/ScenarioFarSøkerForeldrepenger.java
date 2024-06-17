package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
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
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
public class ScenarioFarSøkerForeldrepenger extends AbstractTestScenario<ScenarioFarSøkerForeldrepenger> {

    private ScenarioFarSøkerForeldrepenger() {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.FARA, NavBrukerKjønn.MANN);
        settDefaultSøknad();

    }

    private ScenarioFarSøkerForeldrepenger(AktørId aktørId) {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.FARA, NavBrukerKjønn.MANN, aktørId);
        settDefaultSøknad();
    }

    private void settDefaultSøknad() {
        medSøknad().medRelasjonsRolleType(RelasjonsRolleType.FARA).medSøknadsdato(LocalDate.now());
    }

    public static ScenarioFarSøkerForeldrepenger forFødsel() {
        return new ScenarioFarSøkerForeldrepenger();
    }

    public static ScenarioFarSøkerForeldrepenger forFødselUtenSøknad(AktørId aktørId) {
        var scenario = new ScenarioFarSøkerForeldrepenger(aktørId);
        scenario.utenSøknad();
        return scenario;
    }

    public static ScenarioFarSøkerForeldrepenger forFødselMedGittAktørId(AktørId aktørId) {
        return new ScenarioFarSøkerForeldrepenger(aktørId);
    }

    public static ScenarioFarSøkerForeldrepenger forAdopsjon() {
        return new ScenarioFarSøkerForeldrepenger();
    }

}
