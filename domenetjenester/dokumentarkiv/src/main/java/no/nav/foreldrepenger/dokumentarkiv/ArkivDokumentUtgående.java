package no.nav.foreldrepenger.dokumentarkiv;

import no.nav.foreldrepenger.domene.typer.JournalpostId;

public record ArkivDokumentUtgående(String tittel, JournalpostId journalpostId, String dokumentId) {

}
