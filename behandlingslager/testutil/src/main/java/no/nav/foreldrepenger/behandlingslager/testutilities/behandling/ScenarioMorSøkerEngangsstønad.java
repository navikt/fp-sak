package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.util.FPDateUtil;

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

    private ScenarioMorSøkerEngangsstønad() {
        super(FagsakYtelseType.ENGANGSTØNAD, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
        medSøknad()
            .medRelasjonsRolleType(RelasjonsRolleType.MORA)
            .medSøknadsdato(FPDateUtil.iDag());
    }

    public static ScenarioMorSøkerEngangsstønad forFødselUtenSøknad() {
        ScenarioMorSøkerEngangsstønad scenario = new ScenarioMorSøkerEngangsstønad();
        scenario.utenSøknad();
        return scenario;
    }

    public static ScenarioMorSøkerEngangsstønad forAdopsjonUtenSøknad() {
        ScenarioMorSøkerEngangsstønad scenario = new ScenarioMorSøkerEngangsstønad();
        scenario.utenSøknad();
        return scenario;
    }

    public static ScenarioMorSøkerEngangsstønad forFødsel() {
        return new ScenarioMorSøkerEngangsstønad();
    }

    public static ScenarioMorSøkerEngangsstønad forAdopsjon() {
        return new ScenarioMorSøkerEngangsstønad();
    }

    public BehandlingLås taSkriveLåsForBehandling() {
        return mockBehandlingRepository().taSkriveLås(getBehandling());
    }

}
