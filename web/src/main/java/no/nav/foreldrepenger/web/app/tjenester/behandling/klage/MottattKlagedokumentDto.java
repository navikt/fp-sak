package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.time.LocalDate;
import java.time.LocalDateTime;

class MottattKlagedokumentDto {

    private String journalpostId;
    private String dokumentTypeId;
    private String dokumentKategori;
    private Long behandlingId;
    private LocalDate mottattDato;
    private LocalDateTime mottattTidspunkt;
    private String xmlPayload;
    private boolean elektroniskRegistrert;
    private Long fagsakId;

    public MottattKlagedokumentDto() {
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public void setDokumentTypeId(String dokumentTypeId) {
        this.dokumentTypeId = dokumentTypeId;
    }

    public void setDokumentKategori(String dokumentKategori) {
        this.dokumentKategori = dokumentKategori;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public void setMottattTidspunkt(LocalDateTime mottattTidspunkt) {
        this.mottattTidspunkt = mottattTidspunkt;
    }

    public void setXmlPayload(String xmlPayload) {
        this.xmlPayload = xmlPayload;
    }

    public void setElektroniskRegistrert(boolean elektroniskRegistrert) {
        this.elektroniskRegistrert = elektroniskRegistrert;
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentTypeId() {
        return dokumentTypeId;
    }

    public String getDokumentKategori() {
        return dokumentKategori;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public String getXmlPayload() {
        return xmlPayload;
    }

    public boolean isElektroniskRegistrert() {
        return elektroniskRegistrert;
    }

    public Long getFagsakId() {
        return fagsakId;
    }
}
