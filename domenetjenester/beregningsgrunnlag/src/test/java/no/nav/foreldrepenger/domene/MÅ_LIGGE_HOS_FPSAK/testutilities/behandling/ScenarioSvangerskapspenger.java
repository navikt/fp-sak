package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class ScenarioSvangerskapspenger extends AbstractTestScenario<ScenarioSvangerskapspenger> {

    private ScenarioSvangerskapspenger() {
        super(FagsakYtelseType.SVANGERSKAPSPENGER, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }

    public static ScenarioSvangerskapspenger nyttScenario() {
        return new ScenarioSvangerskapspenger();
    }

}
