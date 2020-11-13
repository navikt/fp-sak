package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class ScenarioMedMorSøkerForeldrepenger extends AbstractTestScenario<ScenarioMedMorSøkerForeldrepenger> {

    private ScenarioMedMorSøkerForeldrepenger() {
        super(FagsakYtelseType.FORELDREPENGER, RelasjonsRolleType.MEDMOR);
    }

    public static ScenarioMedMorSøkerForeldrepenger forFødsel() {
        return new ScenarioMedMorSøkerForeldrepenger();
    }
}
