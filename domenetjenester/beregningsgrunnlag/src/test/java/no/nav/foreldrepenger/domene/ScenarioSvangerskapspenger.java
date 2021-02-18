package no.nav.foreldrepenger.domene;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;

public class ScenarioSvangerskapspenger extends AbstractTestScenario<ScenarioSvangerskapspenger> {

    private ScenarioSvangerskapspenger() {
        super(FagsakYtelseType.SVANGERSKAPSPENGER, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }

    public static ScenarioSvangerskapspenger nyttScenario() {
        return new ScenarioSvangerskapspenger();
    }

}
