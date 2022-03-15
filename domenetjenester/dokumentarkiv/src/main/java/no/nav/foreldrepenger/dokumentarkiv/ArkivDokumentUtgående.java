package no.nav.foreldrepenger.dokumentarkiv;

import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class ArkivDokumentUtgående {
    private JournalpostId journalpostId;
    private String dokumentId;
    private String tittel;

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (ArkivDokumentUtgående) o;
        return Objects.equals(dokumentId, that.dokumentId) &&
            Objects.equals(tittel, that.tittel) &&
            Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentId, tittel, journalpostId);
    }

    public static class Builder {
        private final ArkivDokumentUtgående arkivDokument;

        private Builder() {
            this.arkivDokument = new ArkivDokumentUtgående();
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medJournalpostId(JournalpostId journalpostId) {
            this.arkivDokument.setJournalpostId(journalpostId);
            return this;
        }

        public Builder medDokumentId(String dokumentId) {
            this.arkivDokument.setDokumentId(dokumentId);
            return this;
        }

        public Builder medTittel(String tittel) {
            this.arkivDokument.setTittel(tittel);
            return this;
        }

        public ArkivDokumentUtgående build() {
            return this.arkivDokument;
        }

    }
}
