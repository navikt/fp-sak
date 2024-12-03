package no.nav.foreldrepenger.dokumentbestiller.formidling;

import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;

public interface Dokument {

    /**
     * Produserer og journalfører et dokument definert i dtoen.
     * @param dokumentBestillingDto
     */
    void bestill(DokumentBestillingDto dokumentBestillingDto);

    /**
     * Forhåndsviser et dokument definert i dtoen.
     * @param dokumentForhåndsvisDto
     */
    byte[] forhåndsvis(DokumentForhåndsvisDto dokumentForhåndsvisDto);
}
