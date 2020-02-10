package no.nav.foreldrepenger.domene.vedtak.observer;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface PubliserVedtattYtelseHendelseFeil extends DeklarerteFeil {
    PubliserVedtattYtelseHendelseFeil FEILFACTORY = FeilFactory.create(PubliserVedtattYtelseHendelseFeil.class); //$NON-NLS-1$

    @TekniskFeil(feilkode = "FP-190495", feilmelding = "Kunne ikke serialisere til json.", logLevel = LogLevel.WARN)
    Feil kanIkkeSerialisere(JsonProcessingException e);
}
