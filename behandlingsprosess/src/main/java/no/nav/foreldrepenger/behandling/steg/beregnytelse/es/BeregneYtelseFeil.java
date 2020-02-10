package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;

interface BeregneYtelseFeil extends DeklarerteFeil {

    BeregneYtelseFeil FACTORY = FeilFactory.create(BeregneYtelseFeil.class);

    @FunksjonellFeil(feilkode = "FP-110705",
        feilmelding = "Kan ikke beregne ytelse. Finner ikke barn som har rett til ytelse i behandlingsgrunnlaget.",
        løsningsforslag = "Sjekk avklarte fakta i behandlingen. Oppdater fakta slik at det finnes barn " +
            "med rett til støtte, eller sett behandling til avslått.",
        logLevel = LogLevel.WARN)
    Feil beregningsstegIkkeStøttetForBehandling();
}
