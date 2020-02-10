package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;


public interface PersonopplysningFeil extends DeklarerteFeil  {

    PersonopplysningFeil FACTORY = FeilFactory.create(PersonopplysningFeil.class);

    @TekniskFeil(feilkode = "FP-124903", feilmelding = "Må basere seg på eksisterende versjon av personopplysning", logLevel = LogLevel.WARN)
    Feil måBasereSegPåEksisterendeVersjon();
}

