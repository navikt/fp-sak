package no.nav.foreldrepenger.behandlingskontroll.testutilities;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Default test scenario builder for Behandlingskontroll enhetstester.
 */
public class TestScenario extends AbstractTestScenario<TestScenario> {

    private TestScenario(FagsakYtelseType ytelseType) {
        super(ytelseType, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }
    
    public static TestScenario forYtelseType(FagsakYtelseType ytelseType) {
        return new TestScenario(ytelseType);
    }

    public static TestScenario forEngangsstønad() {
        return forYtelseType(FagsakYtelseType.ENGANGSTØNAD);
    }

    public static TestScenario forForeldrepenger() {
        return forYtelseType(FagsakYtelseType.FORELDREPENGER);
    }

}
