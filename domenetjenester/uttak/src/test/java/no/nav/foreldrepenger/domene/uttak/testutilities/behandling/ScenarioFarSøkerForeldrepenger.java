package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

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
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.FARA);

    }

    private ScenarioFarSøkerForeldrepenger(AktørId aktørId) {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.FARA, aktørId);
    }

    public static ScenarioFarSøkerForeldrepenger forFødsel() {
        return new ScenarioFarSøkerForeldrepenger();
    }

    public static ScenarioFarSøkerForeldrepenger forFødselMedGittAktørId(AktørId aktørId) {
        return new ScenarioFarSøkerForeldrepenger(aktørId);
    }
}
