package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

@MappedSuperclass
public abstract class Aktør extends BaseEntitet {

    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", unique=true, nullable = false, updatable = false))
    private AktørId aktørId;

    @SuppressWarnings("unused")
    Aktør() {
        // For Hibernate
    }

    public Aktør(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Aktør other)
                || !(getClass().isAssignableFrom(object.getClass()) || object.getClass().isAssignableFrom(getClass()))) {
            return false;
        }
        return Objects.equals(other.aktørId, this.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

}
