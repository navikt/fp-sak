package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;

/**
 * Default test scenario builder for Mor søker Engangsstønad. Kan opprettes for fødsel eller adopsjon og brukes til å
 * opprette standard scenarioer.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
public class ScenarioMorSøkerEngangsstønad extends AbstractTestScenario<ScenarioMorSøkerEngangsstønad> {

    private ScenarioMorSøkerEngangsstønad(AktørId aktørId) {
        super(FagsakYtelseType.ENGANGSTØNAD, RelasjonsRolleType.MORA, aktørId);
    }

    public static ScenarioMorSøkerEngangsstønad forFødsel(AktørId aktørId) {
        return new ScenarioMorSøkerEngangsstønad(aktørId);
    }

}
