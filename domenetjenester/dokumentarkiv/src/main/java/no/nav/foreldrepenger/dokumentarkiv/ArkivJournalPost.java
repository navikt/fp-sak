package no.nav.foreldrepenger.dokumentarkiv;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class ArkivJournalPost {
    private JournalpostId journalpostId;
    private ArkivDokument hovedDokument;
    private List<ArkivDokument> andreDokument;
    private Kommunikasjonsretning kommunikasjonsretning;
    private String beskrivelse;
    private LocalDateTime tidspunkt;

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    public ArkivDokument getHovedDokument() {
        return hovedDokument;
    }

    public void setHovedDokument(ArkivDokument hovedDokument) {
        this.hovedDokument = hovedDokument;
    }

    public List<ArkivDokument> getAndreDokument() {
        return andreDokument;
    }

    public void setAndreDokument(List<ArkivDokument> andreDokument) {
        this.andreDokument = andreDokument;
    }

    public Kommunikasjonsretning getKommunikasjonsretning() {
        return kommunikasjonsretning;
    }

    public void setKommunikasjonsretning(Kommunikasjonsretning kommunikasjonsretning) {
        this.kommunikasjonsretning = kommunikasjonsretning;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public LocalDateTime getTidspunkt() {
        return tidspunkt;
    }

    public void setTidspunkt(LocalDateTime tidspunkt) {
        this.tidspunkt = tidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArkivJournalPost that = (ArkivJournalPost) o;
        return Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(hovedDokument, that.hovedDokument) &&
            Objects.equals(andreDokument, that.andreDokument) &&
            Objects.equals(kommunikasjonsretning, that.kommunikasjonsretning) &&
            Objects.equals(beskrivelse, that.beskrivelse) &&
            Objects.equals(tidspunkt, that.tidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, hovedDokument, andreDokument, kommunikasjonsretning, beskrivelse, tidspunkt);
    }

    public static class Builder {
        private final ArkivJournalPost arkivJournalPost;

        private Builder() {
            this.arkivJournalPost = new ArkivJournalPost();
            this.arkivJournalPost.setAndreDokument(new ArrayList<>());
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medJournalpostId(JournalpostId journalpostId) {
            this.arkivJournalPost.setJournalpostId(journalpostId);
            return this;
        }

        public Builder medTidspunkt(LocalDateTime tidspunkt) {
            this.arkivJournalPost.setTidspunkt(tidspunkt);
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.arkivJournalPost.setBeskrivelse(beskrivelse);
            return this;
        }

        public Builder medKommunikasjonsretning(Kommunikasjonsretning innUtNotat){
            this.arkivJournalPost.setKommunikasjonsretning(innUtNotat);
            return this;
        }

        public Builder medHoveddokument(ArkivDokument hovedDokument){
            this.arkivJournalPost.setHovedDokument(hovedDokument);
            return this;
        }

        public Builder leggTillVedlegg(ArkivDokument vedlegg){
            this.arkivJournalPost.getAndreDokument().add(vedlegg);
            return this;
        }

        public ArkivJournalPost build() {
            return this.arkivJournalPost;
        }

    }

}
