package no.nav.foreldrepenger.web.app.tjenester.dokument.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;

import java.time.LocalDate;

public class MottattDokumentDto {

    private final LocalDate mottattDato;
    private final DokumentTypeId dokumentTypeId;
    private final DokumentKategori dokumentKategori;

    public MottattDokumentDto(MottattDokument mottattDokument) {
        this.mottattDato = mottattDokument.getMottattDato();
        this.dokumentTypeId = mottattDokument.getDokumentType();
        this.dokumentKategori = mottattDokument.getDokumentKategori();
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public DokumentTypeId getDokumentTypeId() {
        return dokumentTypeId;
    }

    public DokumentKategori getDokumentKategori() {
        return dokumentKategori;
    }
}
