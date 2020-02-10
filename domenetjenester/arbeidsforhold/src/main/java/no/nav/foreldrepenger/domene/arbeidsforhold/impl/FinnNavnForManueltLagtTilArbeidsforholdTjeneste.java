package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;

public class FinnNavnForManueltLagtTilArbeidsforholdTjeneste {

    private FinnNavnForManueltLagtTilArbeidsforholdTjeneste() {
        // Skjul konstruktør
    }

    /** Henter informasjon om manuelt lagt til arbeidsforhold (får tildelt kunstig orgnummer)
     *
     * @param overstyringer Arbeidsforholdoverstyringer
     * @return ArbeidsgiverOpplysninger
     */
    public static Optional<ArbeidsgiverOpplysninger> finnNavnTilManueltLagtTilArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer
            .stream()
            .findFirst()
            .map(arbeidsforhold -> new ArbeidsgiverOpplysninger(arbeidsforhold.getArbeidsgiver().getOrgnr(), arbeidsforhold.getArbeidsgiverNavn()));
    }
}
