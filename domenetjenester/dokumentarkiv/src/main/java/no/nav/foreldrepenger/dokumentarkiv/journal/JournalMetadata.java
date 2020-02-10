package no.nav.foreldrepenger.dokumentarkiv.journal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.dokumentarkiv.ArkivFilType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class JournalMetadata {

    public enum Journaltilstand {
        MIDLERTIDIG,
        UTGAAR,
        ENDELIG
    }

    private JournalpostId journalpostId;
    private String dokumentId;
    private VariantFormat variantFormat;
    private Journaltilstand journaltilstand;
    private DokumentTypeId dokumentType;
    private DokumentKategori dokumentKategori;
    private ArkivFilType arkivFilType;
    private boolean erHoveddokument;
    private LocalDate forsendelseMottatt;
    private List<String> brukerIdentListe;

    private JournalMetadata() {
        // skjult
    }

    public static Builder builder() {
        return new Builder();
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public VariantFormat getVariantFormat() {
        return variantFormat;
    }

    public DokumentTypeId getDokumentType() {
        return dokumentType;
    }

    public DokumentKategori getDokumentKategori() {
        return dokumentKategori;
    }

    public ArkivFilType getArkivFilType() {
        return arkivFilType;
    }

    public Journaltilstand getJournaltilstand() {
        return journaltilstand;
    }

    public boolean getErHoveddokument() {
        return erHoveddokument;
    }

    public List<String> getBrukerIdentListe() {
        if (brukerIdentListe == null) {
            brukerIdentListe = new ArrayList<>();
        }
        return brukerIdentListe;
    }

    public LocalDate getForsendelseMottatt() {
        return forsendelseMottatt;
    }

    public static class Builder {
        private JournalpostId journalpostId;
        private String dokumentId;
        private VariantFormat variantFormat;
        private DokumentTypeId dokumentType;
        private DokumentKategori dokumentKategori;
        private ArkivFilType arkivFilType;
        private Journaltilstand journaltilstand;
        private boolean erHoveddokument;
        private LocalDate forsendelseMottatt;
        private List<String> brukerIdentListe;

        public Builder medJournalpostId(JournalpostId journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public Builder medDokumentId(String dokumentId) {
            this.dokumentId = dokumentId;
            return this;
        }

        public Builder medVariantFormat(VariantFormat variantFormat) {
            this.variantFormat = variantFormat;
            return this;
        }

        public Builder medDokumentType(DokumentTypeId dokumentType) {
            this.dokumentType = dokumentType;
            return this;
        }

        public Builder medDokumentKategori(DokumentKategori dokumentKategori) {
            this.dokumentKategori = dokumentKategori;
            return this;
        }

        public Builder medArkivFilType(ArkivFilType arkivFilType) {
            this.arkivFilType = arkivFilType;
            return this;
        }

        public Builder medJournaltilstand(Journaltilstand journaltilstand) {
            this.journaltilstand = journaltilstand;
            return this;
        }

        public Builder medErHoveddokument(boolean erHoveddokument) {
            this.erHoveddokument = erHoveddokument;
            return this;
        }

        public Builder medForsendelseMottatt(LocalDate forsendelseMottatt) {
            this.forsendelseMottatt = forsendelseMottatt;
            return this;
        }

        public Builder medBrukerIdentListe(List<String> brukerIdentListe) {
            this.brukerIdentListe = brukerIdentListe;
            return this;
        }

        public JournalMetadata build() {
            JournalMetadata jmd = new JournalMetadata();
            jmd.journalpostId = this.journalpostId;
            jmd.dokumentId = this.dokumentId;
            jmd.variantFormat = this.variantFormat;
            jmd.dokumentType = this.dokumentType;
            jmd.dokumentKategori = this.dokumentKategori;
            jmd.arkivFilType = this.arkivFilType;
            jmd.journaltilstand = this.journaltilstand;
            jmd.erHoveddokument = this.erHoveddokument;
            jmd.forsendelseMottatt = this.forsendelseMottatt;
            jmd.brukerIdentListe = this.brukerIdentListe;
            return jmd;
        }
    }
}
