package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import java.time.LocalDate;

/**
 * Default test scenario builder for Mor søker Engangsstønad. Kan opprettes for
 * fødsel eller adopsjon og brukes til å opprette standard scenarioer.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt
 * innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne klassen.
 */
public class IAYScenarioBuilder extends AbstractIAYTestScenario<IAYScenarioBuilder> {

    private IAYScenarioBuilder(FagsakYtelseType ytelseType, RelasjonsRolleType rolle, NavBrukerKjønn kjønn) {
        super(ytelseType, rolle, kjønn);
        medSøknad()
                .medRelasjonsRolleType(rolle)
                .medSøknadsdato(LocalDate.now());
    }

    public static IAYScenarioBuilder morSøker(FagsakYtelseType ytelseType) {
        return new IAYScenarioBuilder(ytelseType, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }
}
