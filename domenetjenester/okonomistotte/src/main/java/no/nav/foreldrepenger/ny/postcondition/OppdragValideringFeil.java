package no.nav.foreldrepenger.ny.postcondition;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface OppdragValideringFeil extends DeklarerteFeil {
    OppdragValideringFeil FACTORY = FeilFactory.create(OppdragValideringFeil.class);

    @TekniskFeil(feilkode = "FP-767898", feilmelding = "Validering av oppdrag feilet: %s", logLevel = LogLevel.WARN)
    Feil valideringsfeil(String detaljer);

    @TekniskFeil(feilkode = "FP-577348", feilmelding = "Oppdaget mindre forskjell mellom tilkjent ytelse oppdrag: %s", logLevel = LogLevel.INFO)
    Feil minorValideringsfeil(String detaljer);

}
