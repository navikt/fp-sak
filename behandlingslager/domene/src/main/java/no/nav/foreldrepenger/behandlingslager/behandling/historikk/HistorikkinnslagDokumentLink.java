package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

import java.util.Objects;

@Entity(name = "HistorikkinnslagDokumentLink")
@Table(name = "HISTORIKKINNSLAG_DOK_LINK")
public class HistorikkinnslagDokumentLink extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG_DOK_LINK")
    private Long id;

    @Column(name = "link_tekst", updatable=false, nullable = false)
    private String linkTekst;

    @ManyToOne(optional = false)
    @JoinColumn(name = "historikkinnslag_id", nullable = false, updatable=false)
    private Historikkinnslag historikkinnslag;

    @Embedded
    @AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id", updatable=false))
    private JournalpostId journalpostId;

    @Column(name = "dokument_id", updatable=false)
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

    public void setHistorikkinnslag(Historikkinnslag historikkinnslag) {
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
        private HistorikkinnslagDokumentLink historikkinnslagDokumentLink;

        public Builder() {
            historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        }

        public Builder medLinkTekst(String linkTekst) {
            historikkinnslagDokumentLink.linkTekst = linkTekst;
            return this;
        }

        public Builder medHistorikkinnslag(Historikkinnslag historikkinnslag) {
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

        public HistorikkinnslagDokumentLink build() {
            return historikkinnslagDokumentLink;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistorikkinnslagDokumentLink that)) {
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
