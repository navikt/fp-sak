package no.nav.foreldrepenger.dokumentbestiller.formidling;

import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingV2Dto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;

public interface Dokument {

    /**
     * @deprecated Erstattes av bestill med V3 kontrakt
     * Produserer og journalfører et dokument definert i dtoen.
     * @param dokumentbestillingV2Dto
     */
    @Deprecated(forRemoval = true)
    void bestill(DokumentbestillingV2Dto dokumentbestillingV2Dto);

    /**
     * Produserer og journalfører et dokument definert i dtoen.
     * @param dokumentBestillingDto
     */
    void bestill(DokumentBestillingDto dokumentBestillingDto);

    /**
     * @deprecated Erstattes av forhåndsvis med V3 kontrakt
     * Forhåndsviser et dokument definert i dtoen.
     * @param dokumentbestillingDto
     */
    @Deprecated(forRemoval = true)
    byte[] forhåndsvis(DokumentbestillingDto dokumentbestillingDto);

    /**
     * Forhåndsviser et dokument definert i dtoen.
     * @param dokumentForhåndsvisDto
     */
    byte[] forhåndsvis(DokumentForhåndsvisDto dokumentForhåndsvisDto);
}
