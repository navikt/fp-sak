package no.nav.foreldrepenger.dokumentbestiller.formidling;

import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingV2Dto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;

public interface Brev {

    /**
     * Produserer og journalfører et dokument definert i dtoen.
     * @param dokumentbestillingV2Dto
     */
    void bestill(DokumentbestillingV2Dto dokumentbestillingV2Dto);

    /**
     * Produserer og journalfører et dokument definert i dtoen.
     * @param dokumentBestillingDto
     */
    void bestill(DokumentBestillingDto dokumentBestillingDto);

    /**
     * Forhåndsviser et dokument definert i dtoen.
     * @param dokumentbestillingDto
     */
    byte[] forhåndsvis(DokumentbestillingDto dokumentbestillingDto);

    /**
     * Forhåndsviser et dokument definert i dtoen.
     * @param dokumentForhåndsvisDto
     */
    byte[] forhåndsvis(DokumentForhåndsvisDto dokumentForhåndsvisDto);
}
