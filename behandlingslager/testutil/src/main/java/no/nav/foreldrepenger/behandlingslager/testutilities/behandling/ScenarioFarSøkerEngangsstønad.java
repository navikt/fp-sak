package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Default test scenario builder for Far søker Engangsstønad. Kan opprettes for fødsel eller adopsjon og brukes til å
 * opprette standard scenarioer.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
public class ScenarioFarSøkerEngangsstønad extends AbstractTestScenario<ScenarioFarSøkerEngangsstønad> {

    private ScenarioFarSøkerEngangsstønad() {
        super(FagsakYtelseType.ENGANGSTØNAD, RelasjonsRolleType.FARA, NavBrukerKjønn.MANN);
        medSøknad().medRelasjonsRolleType(RelasjonsRolleType.FARA).medSøknadsdato(LocalDate.now());

    }

    public static ScenarioFarSøkerEngangsstønad forFødselUtenSøknad() {
        var scenario = new ScenarioFarSøkerEngangsstønad();
        scenario.utenSøknad();
        return scenario;
    }

    public static ScenarioFarSøkerEngangsstønad forAdopsjonUtenSøknad() {
        var scenario = new ScenarioFarSøkerEngangsstønad();
        scenario.utenSøknad();
        return scenario;
    }

    public static ScenarioFarSøkerEngangsstønad forFødsel() {
        return new ScenarioFarSøkerEngangsstønad();
    }

    public static ScenarioFarSøkerEngangsstønad forAdopsjon() {
        return new ScenarioFarSøkerEngangsstønad();
    }

}
