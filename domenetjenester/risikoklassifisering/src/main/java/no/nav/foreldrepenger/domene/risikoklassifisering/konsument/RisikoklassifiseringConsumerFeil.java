package no.nav.foreldrepenger.domene.risikoklassifisering.konsument;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface RisikoklassifiseringConsumerFeil extends DeklarerteFeil {

    RisikoklassifiseringConsumerFeil FACTORY = FeilFactory.create(RisikoklassifiseringConsumerFeil.class);

    @TekniskFeil(feilkode = "FP-65747", feilmelding = "Klarte ikke deserialisere for klasse %s", logLevel = WARN)
    Feil klarteIkkeDeserialisere(String className, Exception e);
}
