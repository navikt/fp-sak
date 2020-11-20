package no.nav.foreldrepenger.økonomi.ny.postcondition;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface OppdragValideringFeil extends DeklarerteFeil {
    OppdragValideringFeil FACTORY = FeilFactory.create(OppdragValideringFeil.class);

    @TekniskFeil(feilkode = "FP-767898", feilmelding = "Validering av oppdrag feilet: %s", logLevel = LogLevel.WARN, exceptionClass = OppdragValideringException.class)
    Feil valideringsfeil(String detaljer);

}
