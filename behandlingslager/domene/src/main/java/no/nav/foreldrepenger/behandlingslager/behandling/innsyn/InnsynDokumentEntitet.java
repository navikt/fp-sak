package no.nav.foreldrepenger.behandlingslager.behandling.innsyn;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "InnsynDokument")
@Table(name = "INNSYN_DOKUMENT")
public class InnsynDokumentEntitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNSYN_DOKUMENT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "innsyn_id", nullable = false, updatable = false)
    private InnsynEntitet innsyn;

    @Column(name = "journalpost_id", nullable = false)
    private JournalpostId journalpostId;

    @Column(name = "dokument_id", nullable = false)
    private String dokumentId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "fikk_innsyn", nullable = false)
    private boolean fikkInnsyn;

    @SuppressWarnings("unused")
    private InnsynDokumentEntitet() {
        // for hibernate
    }

    public InnsynDokumentEntitet(boolean fikkInnsyn, JournalpostId journalpostId, String dokumentId) {
        this.fikkInnsyn = fikkInnsyn;
        this.journalpostId = journalpostId;
        this.dokumentId = dokumentId;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(journalpostId, dokumentId);
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

    public boolean isFikkInnsyn() {
        return fikkInnsyn;
    }

    public InnsynEntitet getInnsyn() {
        return innsyn;
    }

    void setInnsyn(InnsynEntitet innsyn) {
        this.innsyn = innsyn;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InnsynDokumentEntitet that)) {
            return false;
        }
        return Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(dokumentId, that.dokumentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, dokumentId);
    }

    @Override
    public String toString() {
        return "InnsynDokumentEntitet{" +
            "id=" + id +
            ", journalpostId='" + journalpostId + '\'' +
            ", dokumentId='" + dokumentId + '\'' +
            ", fikkInnsyn=" + fikkInnsyn +
            '}';
    }
}
