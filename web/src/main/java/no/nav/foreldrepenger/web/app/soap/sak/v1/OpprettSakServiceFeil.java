package no.nav.foreldrepenger.web.app.soap.sak.v1;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;

interface OpprettSakServiceFeil extends DeklarerteFeil {
    OpprettSakServiceFeil FACTORY = FeilFactory.create(OpprettSakServiceFeil.class);

    @FunksjonellFeil(feilkode = "FP-785354", feilmelding = "Kan ikke opprette sak basert på oppgitt dokument", løsningsforslag = "Journalføre dokument på annen sak", logLevel = LogLevel.WARN)
    Feil ikkeStøttetDokumentType();

    @FunksjonellFeil(feilkode = "FP-785356", feilmelding = "Dokument og valgt ytelsetype i uoverenstemmelse", løsningsforslag = "Velg ytelsetype som samstemmer med dokument", logLevel = LogLevel.WARN)
    Feil inkonsistensTemaVsDokument();
}
