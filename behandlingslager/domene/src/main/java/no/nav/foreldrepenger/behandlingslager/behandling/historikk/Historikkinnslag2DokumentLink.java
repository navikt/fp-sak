package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@Entity(name = "Historikkinnslag2DokumentLink")
@Table(name = "HISTORIKKINNSLAG2_DOK_LINK")
public class Historikkinnslag2DokumentLink extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG2_DOK_LINK")
    private Long id;

    @Column(name = "link_tekst", updatable=false, nullable = false)
    private String linkTekst;

    @ManyToOne(optional = false)
    @JoinColumn(name = "historikkinnslag_id", nullable = false)
    private Historikkinnslag2 historikkinnslag;

    @Embedded
    @AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id"))
    private JournalpostId journalpostId;

    @Column(name = "dokument_id")
    private String dokumentId;

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(journalpostId, dokumentId, linkTekst);
    }

    public String getLinkTekst() {
        return linkTekst;
    }

    public void setLinkTekst(String tag) {
        this.linkTekst = tag;
    }

    public void setHistorikkinnslag(Historikkinnslag2 historikkinnslag) {
        this.historikkinnslag = historikkinnslag;
    }

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

    public static class Builder {
        private Historikkinnslag2DokumentLink historikkinnslagDokumentLink;

        public Builder() {
            historikkinnslagDokumentLink = new Historikkinnslag2DokumentLink();
        }

        public Builder medLinkTekst(String linkTekst) {
            historikkinnslagDokumentLink.linkTekst = linkTekst;
            return this;
        }

        public Builder medHistorikkinnslag(Historikkinnslag2 historikkinnslag) {
            historikkinnslagDokumentLink.historikkinnslag = historikkinnslag;
            return this;
        }

        public Builder medJournalpostId(JournalpostId journalpostId) {
            historikkinnslagDokumentLink.journalpostId = journalpostId;
            return this;
        }

        public Builder medDokumentId(String dokumentId) {
            historikkinnslagDokumentLink.dokumentId = dokumentId;
            return this;
        }

        public Historikkinnslag2DokumentLink build() {
            return historikkinnslagDokumentLink;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Historikkinnslag2DokumentLink that)) {
            return false;
        }
        return
            Objects.equals(getLinkTekst(), that.getLinkTekst()) &&
            Objects.equals(historikkinnslag, that.historikkinnslag) &&
            Objects.equals(getJournalpostId(), that.getJournalpostId()) &&
            Objects.equals(getDokumentId(), that.getDokumentId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLinkTekst(), historikkinnslag, getJournalpostId(), getDokumentId());
    }
}
