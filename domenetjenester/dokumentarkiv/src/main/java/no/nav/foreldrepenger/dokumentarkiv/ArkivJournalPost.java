package no.nav.foreldrepenger.dokumentarkiv;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class ArkivJournalPost {
    private JournalpostId journalpostId;
    private ArkivDokument hovedDokument;
    private List<ArkivDokument> andreDokument = new ArrayList<>();
    private Kommunikasjonsretning kommunikasjonsretning;
    private String beskrivelse;
    private LocalDateTime tidspunkt;

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public ArkivDokument getHovedDokument() {
        return hovedDokument;
    }

    public List<ArkivDokument> getAndreDokument() {
        return andreDokument;
    }

    public Kommunikasjonsretning getKommunikasjonsretning() {
        return kommunikasjonsretning;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public LocalDateTime getTidspunkt() {
        return tidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ArkivJournalPost) o;
        return Objects.equals(journalpostId, that.journalpostId) && Objects.equals(hovedDokument, that.hovedDokument) && Objects.equals(andreDokument,
            that.andreDokument) && Objects.equals(kommunikasjonsretning, that.kommunikasjonsretning) && Objects.equals(beskrivelse, that.beskrivelse)
            && Objects.equals(tidspunkt, that.tidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, hovedDokument, andreDokument, kommunikasjonsretning, beskrivelse, tidspunkt);
    }

    public static class Builder {
        private final ArkivJournalPost arkivJournalPost;

        private Builder() {
            this.arkivJournalPost = new ArkivJournalPost();
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medJournalpostId(JournalpostId journalpostId) {
            this.arkivJournalPost.journalpostId = journalpostId;
            return this;
        }

        public Builder medTidspunkt(LocalDateTime tidspunkt) {
            this.arkivJournalPost.tidspunkt = tidspunkt;
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.arkivJournalPost.beskrivelse = beskrivelse;
            return this;
        }

        public Builder medKommunikasjonsretning(Kommunikasjonsretning innUtNotat) {
            this.arkivJournalPost.kommunikasjonsretning = innUtNotat;
            return this;
        }

        public Builder medHoveddokument(ArkivDokument hovedDokument) {
            this.arkivJournalPost.hovedDokument = hovedDokument;
            return this;
        }

        public Builder medAndreDokument(List<ArkivDokument> vedlegg) {
            this.arkivJournalPost.getAndreDokument().addAll(vedlegg);
            return this;
        }

        public Builder leggTillVedlegg(ArkivDokument vedlegg) {
            this.arkivJournalPost.getAndreDokument().add(vedlegg);
            return this;
        }

        public ArkivJournalPost build() {
            return this.arkivJournalPost;
        }

    }

}
