package no.nav.foreldrepenger.domene.prosess.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.FORELDREPENGER;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;

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
public class ScenarioForeldrepenger extends AbstractTestScenario<ScenarioForeldrepenger> {

    private ScenarioForeldrepenger(RelasjonsRolleType relasjonRolle, NavBrukerKjønn kjønn) {
        super(FORELDREPENGER, relasjonRolle, kjønn);
    }

    public static ScenarioForeldrepenger nyttScenario() {
        return new ScenarioForeldrepenger(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }
}
