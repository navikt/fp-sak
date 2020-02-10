package no.nav.foreldrepenger.mottak.publiserer.producer;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface PubliserPersistertDokumentHendelseFeil extends DeklarerteFeil {
    PubliserPersistertDokumentHendelseFeil FEILFACTORY = FeilFactory.create(PubliserPersistertDokumentHendelseFeil.class); //$NON-NLS-1$

    @TekniskFeil(feilkode = "FP-190496", feilmelding = "Kunne ikke serialisere til json.", logLevel = LogLevel.WARN)
    Feil kanIkkeSerialisere(JsonProcessingException e);
}
