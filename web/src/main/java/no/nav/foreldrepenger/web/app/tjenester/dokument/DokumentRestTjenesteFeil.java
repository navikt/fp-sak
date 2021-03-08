package no.nav.foreldrepenger.web.app.tjenester.dokument;

import static no.nav.vedtak.feil.LogLevel.ERROR;

import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.ManglerTilgangFeil;

public interface DokumentRestTjenesteFeil extends DeklarerteFeil {
    DokumentRestTjenesteFeil FACTORY = FeilFactory.create(DokumentRestTjenesteFeil.class);

    @ManglerTilgangFeil(feilkode = "FP-909799", feilmelding = "Applikasjon har ikke tilgang til tjeneste.", logLevel = ERROR)
    Feil applikasjonHarIkkeTilgangTilHentJournalpostListeTjeneste(ManglerTilgangException sikkerhetsbegrensning);

    @ManglerTilgangFeil(feilkode = "FP-463438", feilmelding = "Applikasjon har ikke tilgang til tjeneste.", logLevel = ERROR)
    Feil applikasjonHarIkkeTilgangTilHentDokumentTjeneste(ManglerTilgangException sikkerhetsbegrensning);
}
