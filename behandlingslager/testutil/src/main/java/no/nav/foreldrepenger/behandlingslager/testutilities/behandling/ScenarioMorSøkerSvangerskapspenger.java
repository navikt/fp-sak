package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import java.time.LocalDate;
import java.time.Month;

public class ScenarioMorSøkerSvangerskapspenger extends AbstractTestScenario<ScenarioMorSøkerSvangerskapspenger> {

    private ScenarioMorSøkerSvangerskapspenger() {
        super(FagsakYtelseType.SVANGERSKAPSPENGER, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
        medSøknad().medRelasjonsRolleType(RelasjonsRolleType.MORA)
            .medSøknadsdato(LocalDate.of(2019, Month.JANUARY, 1));
    }

    public static ScenarioMorSøkerSvangerskapspenger forSvangerskapspenger() {
        return new ScenarioMorSøkerSvangerskapspenger();
    }

}
