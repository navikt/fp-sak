package no.nav.foreldrepenger.dokumentbestiller.formidling;

import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingV2Dto;

public interface Brev {

    /**
     * Produserer og journalfører et dokument definert i dtoen.
     * @param dokumentbestillingV2Dto
     */
    void bestill(DokumentbestillingV2Dto dokumentbestillingV2Dto);

}
