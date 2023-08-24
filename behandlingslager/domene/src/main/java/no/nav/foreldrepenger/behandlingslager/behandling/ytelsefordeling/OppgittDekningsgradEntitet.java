package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

import java.util.Objects;

@Entity(name = "YtelseDekningsgrad")
@Table(name = "SO_DEKNINGSGRAD")
public class OppgittDekningsgradEntitet extends BaseEntitet {

    public static final int HUNDRE_PROSENT = 100;
    public static final int ÅTTI_PROSENT = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SO_DEKNINGSGRAD")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "dekningsgrad", nullable = false)
    @Max(value = 100)
    @Min(value = 0)
    @ChangeTracked
    private int dekningsgrad;

    OppgittDekningsgradEntitet() {
        // Hibernate
    }

    private OppgittDekningsgradEntitet(int dekningsgrad) {
        this.dekningsgrad = dekningsgrad;
    }

    public static OppgittDekningsgradEntitet bruk80() {
        return new OppgittDekningsgradEntitet(ÅTTI_PROSENT);
    }

    public static OppgittDekningsgradEntitet bruk100() {
        return new OppgittDekningsgradEntitet(HUNDRE_PROSENT);
    }

    public int getDekningsgrad() {
        return dekningsgrad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (OppgittDekningsgradEntitet) o;
        return Objects.equals(dekningsgrad, that.dekningsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dekningsgrad);
    }

    @Override
    public String toString() {
        return "OppgittDekningsgrad{" +
            "id=" + id +
            ", dekningsgrad=" + dekningsgrad +
            '}';
    }
}
