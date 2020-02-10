package no.nav.foreldrepenger.kompletthet;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface KompletthetFeil extends DeklarerteFeil {

    KompletthetFeil FACTORY = FeilFactory.create(KompletthetFeil.class);

    @TekniskFeil(feilkode = "FP-912911", feilmelding = "Mer enn en implementasjon funnet av Kompletthetsjekker for fagsakYtelseType=%s og behandlingType=%s", logLevel = WARN)
    Feil flereImplementasjonerAvKompletthetsjekker(String fagsakYtelseType, String behandlingType);

    @TekniskFeil(feilkode = "FP-912910", feilmelding = "Fant ingen implementasjon av Kompletthetsjekker for fagsakYtelseType=%s og behandlingType=%s", logLevel = WARN)
    Feil ingenImplementasjonerAvKompletthetssjekker(String fagsakYtelseType, String behandlingType);

}
