package no.nav.foreldrepenger.behandlingslager.behandling;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entitetsklasse for mottatte dokument.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */

@Entity(name = "MottattDokument")
@Table(name = "MOTTATT_DOKUMENT")
public class MottattDokument extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MOTTATT_DOKUMENT")
    private Long id;

    @Embedded
    @AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id"))
    private JournalpostId journalpostId;

    @Column(name = "forsendelse_id")
    private UUID forsendelseId;

    @Column(name = "journal_enhet")
    private String journalEnhet;

    @Convert(converter = DokumentTypeId.KodeverdiConverter.class)
    @Column(name="type", nullable = false)
    private DokumentTypeId dokumentTypeId = DokumentTypeId.UDEFINERT;

    @Convert(converter = DokumentKategori.KodeverdiConverter.class)
    @Column(name="dokument_kategori", nullable = false)
    private DokumentKategori dokumentKategori = DokumentKategori.UDEFINERT;

    @Column(name = "behandling_id", updatable = false)
    private Long behandlingId;

    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;

    @Column(name = "mottatt_tidspunkt")
    private LocalDateTime mottattTidspunkt;

    @Column(name = "kanalreferanse")
    private String kanalreferanse;

    @Lob
    @Column(name = "xml_payload")
    private String xmlPayload;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "elektronisk_registrert", nullable = false)
    private boolean elektroniskRegistrert;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    MottattDokument() {
        // Hibernate
    }

    MottattDokument(MottattDokument mottatteDokument) {
        this.journalpostId = mottatteDokument.journalpostId;
        this.forsendelseId = mottatteDokument.forsendelseId;
        this.journalEnhet = mottatteDokument.journalEnhet;
        this.dokumentTypeId = mottatteDokument.dokumentTypeId;
        this.dokumentKategori = mottatteDokument.dokumentKategori;
        this.behandlingId = mottatteDokument.behandlingId;
        this.mottattDato = mottatteDokument.mottattDato;
        this.mottattTidspunkt = mottatteDokument.mottattTidspunkt;
        this.kanalreferanse = mottatteDokument.kanalreferanse;
        this.xmlPayload = mottatteDokument.xmlPayload;
        this.elektroniskRegistrert = mottatteDokument.elektroniskRegistrert;
        this.fagsakId = mottatteDokument.fagsakId;
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public DokumentTypeId getDokumentType() {
        return dokumentTypeId;
    }

    public DokumentKategori getDokumentKategori() {
        return dokumentKategori;
    }

    public Optional<String> getJournalEnhet() {
        return Optional.ofNullable(journalEnhet);
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

    public String getKanalreferanse() {
        return kanalreferanse;
    }

    public String getPayloadXml() {
        return xmlPayload;
    }

    public boolean getElektroniskRegistrert() {
        return elektroniskRegistrert;
    }

    void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    void setDokumentKategori(DokumentKategori dokumentKategori) {
        this.dokumentKategori = dokumentKategori;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    void setMottattTidspunkt(LocalDateTime mottattTidspunkt) {
        this.mottattTidspunkt = mottattTidspunkt;
    }

    public void setKanalreferanse(String kanalreferanse) {
        this.kanalreferanse = kanalreferanse;
    }

    void setElektroniskRegistrert(boolean elektroniskRegistrert) {
        this.elektroniskRegistrert = elektroniskRegistrert;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public UUID getForsendelseId() {
        return forsendelseId;
    }

    public void setForsendelseId(UUID forsendelseId) {
        this.forsendelseId = forsendelseId;
    }

    public void setJournalEnhet(String enhet) {
        this.journalEnhet = enhet;
    }

    public boolean erSøknadsDokument() {
        return dokumentTypeId != null && (getDokumentType().erSøknadType() || getDokumentType().erEndringsSøknadType())
            || DokumentKategori.SØKNAD.equals(dokumentKategori);
    }

    public boolean erUstrukturertDokument() {
        return xmlPayload == null;
    }

    public static class Builder {
        private MottattDokument mottatteDokumentMal;

        public Builder() {
            mottatteDokumentMal = new MottattDokument();
        }

        public Builder(MottattDokument mottatteDokument) {
            if (mottatteDokument != null) {
                mottatteDokumentMal = new MottattDokument(mottatteDokument);
            } else {
                mottatteDokumentMal = new MottattDokument();
            }
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medDokumentType(DokumentTypeId dokumentTypeId) {
            mottatteDokumentMal.dokumentTypeId = dokumentTypeId == null ? DokumentTypeId.UDEFINERT : dokumentTypeId;
            return this;
        }

        public Builder medDokumentKategori(DokumentKategori dokumentKategori) {
            mottatteDokumentMal.dokumentKategori = dokumentKategori;
            return this;
        }

        public Builder medJournalPostId(JournalpostId journalPostId) {
            mottatteDokumentMal.journalpostId = journalPostId;
            return this;
        }

        public Builder medJournalFørendeEnhet(String journalEnhet) {
            mottatteDokumentMal.journalEnhet = journalEnhet;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            mottatteDokumentMal.behandlingId = behandlingId;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            mottatteDokumentMal.mottattDato = mottattDato;
            return this;
        }

        public Builder medMottattTidspunkt(LocalDateTime mottattTidspunkt) {
            mottatteDokumentMal.mottattTidspunkt = mottattTidspunkt;
            return this;
        }

        public Builder medKanalreferanse(String kanalreferanse) {
            mottatteDokumentMal.kanalreferanse = kanalreferanse;
            return this;
        }

        public Builder medXmlPayload(String xmlPayload) {
            mottatteDokumentMal.xmlPayload = xmlPayload;
            return this;
        }

        public Builder medElektroniskRegistrert(boolean elektroniskRegistrert) {
            mottatteDokumentMal.elektroniskRegistrert = elektroniskRegistrert;
            return this;
        }

        public Builder medFagsakId(Long fagsakId) {
            mottatteDokumentMal.fagsakId = fagsakId;
            return this;
        }

        public Builder medForsendelseId(UUID forsendelseId) {
            mottatteDokumentMal.forsendelseId = forsendelseId;
            return this;
        }

        public Builder medId(Long mottattDokumentId) {
            mottatteDokumentMal.id = mottattDokumentId;
            return this;
        }

        public MottattDokument build() {
            Objects.requireNonNull(mottatteDokumentMal.fagsakId, "Trenger fagsak id for å opprette MottatteDokument.");
            return mottatteDokumentMal;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MottattDokument other)) {
            return false;
        }
        return Objects.equals(this.dokumentTypeId, other.dokumentTypeId)
            && Objects.equals(this.dokumentKategori, other.dokumentKategori)
            && Objects.equals(this.journalpostId, other.journalpostId)
            && Objects.equals(this.xmlPayload, other.xmlPayload)
            && Objects.equals(this.elektroniskRegistrert, other.elektroniskRegistrert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentTypeId, dokumentKategori, journalpostId, xmlPayload, elektroniskRegistrert);
    }
}
