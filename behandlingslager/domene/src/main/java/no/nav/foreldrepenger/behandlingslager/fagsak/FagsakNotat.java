package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "FagsakNotat")
@Table(name = "FAGSAK_NOTAT")
public class FagsakNotat extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAGSAK_NOTAT")
    private Long id;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    // Placeholder i fall man ønsker en skjuling (feilregistrer)
    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "notat", nullable = false)
    private String notat;

    public FagsakNotat() {
        // For Hibernate
    }

    FagsakNotat(Long fagsakId, String notat) {
        Objects.requireNonNull(notat, "tomt notat");
        this.fagsakId = fagsakId;
        this.notat = notat;
    }


    public Long getId() {
        return id;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public String getNotat() {
        return notat;
    }

    void setAktiv(boolean aktivt) {
        this.aktiv = aktivt;
    }

    public boolean getErAktiv() {
        return aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FagsakNotat that)) {
            return false;
        }
        return fagsakId.equals(that.fagsakId) && Objects.equals(notat, that.notat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, notat);
    }
}
