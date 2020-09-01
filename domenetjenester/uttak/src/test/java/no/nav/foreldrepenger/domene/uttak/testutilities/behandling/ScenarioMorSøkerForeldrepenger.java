package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

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
public class ScenarioMorSøkerForeldrepenger extends AbstractTestScenario<ScenarioMorSøkerForeldrepenger> {

    private ScenarioMorSøkerForeldrepenger() {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }

    private ScenarioMorSøkerForeldrepenger(AktørId aktørId) {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, aktørId);
    }

    public static ScenarioMorSøkerForeldrepenger forFødsel() {
        return new ScenarioMorSøkerForeldrepenger();
    }

    public static ScenarioMorSøkerForeldrepenger forFødselUtenSøknad(AktørId aktørId) {
        return new ScenarioMorSøkerForeldrepenger(aktørId);
    }

    public static ScenarioMorSøkerForeldrepenger forFødselMedGittAktørId(AktørId aktørId) {
        return new ScenarioMorSøkerForeldrepenger(aktørId);
    }

    public static ScenarioMorSøkerForeldrepenger forAdopsjon() {
        return new ScenarioMorSøkerForeldrepenger();
    }

}
