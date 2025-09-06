package no.nav.foreldrepenger.domene.feed;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Table(name = "UTGAAENDE_HENDELSE")
@DiscriminatorColumn(name = "OUTPUT_FEED_KODE")
@Entity(name = "UtgåendeHendelse")
public abstract class UtgåendeHendelse extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTGAAENDE_HENDELSE_ID")
    @Column(name = "ID")
    private Long id;

    @Column(name = "TYPE", nullable = false)
    private String type;

    @Lob
    @Column(name = "PAYLOAD", nullable = false)
    private String payload;

    @Column(name = "AKTOER_ID", nullable = false)
    private Long aktørId;

    @Column(name = "SEKVENSNUMMER", nullable = false)
    private long sekvensnummer;

    @Column(name = "KILDE_ID")
    private String kildeId;

    UtgåendeHendelse() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public String getAktørId() {
        return aktørId.toString();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = Long.parseLong(aktørId);
    }

    public long getSekvensnummer() {
        return sekvensnummer;
    }

    protected void setSekvensnummer(long sekvensnummer) {
        this.sekvensnummer = sekvensnummer;
    }

    public String getKildeId() {
        return kildeId;
    }

    public void setKildeId(String kildeId) {
        this.kildeId = kildeId;
    }

}
