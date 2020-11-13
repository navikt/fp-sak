package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class ScenarioMorSøkerSvangerskapspenger extends AbstractTestScenario<ScenarioMorSøkerSvangerskapspenger> {

    private ScenarioMorSøkerSvangerskapspenger() {
        super(FagsakYtelseType.SVANGERSKAPSPENGER, RelasjonsRolleType.MORA);
    }

    public static ScenarioMorSøkerSvangerskapspenger forSvangerskapspenger() {
        return new ScenarioMorSøkerSvangerskapspenger();
    }

}
