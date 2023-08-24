package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(name = "SoeknadPerioder")
@Table(name = "YF_FORDELING")
public class OppgittFordelingEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_YF_FORDELING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "oppgittFordeling")
    @BatchSize(size = 25)
    @ChangeTracked
    private List<OppgittPeriodeEntitet> søknadsPerioder;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "annenForelderErInformert", nullable = false)
    @ChangeTracked
    private boolean erAnnenForelderInformert;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "oensker_justert_ved_foedsel")
    private Boolean ønskerJustertVedFødsel;

    protected OppgittFordelingEntitet() {
    }

    public OppgittFordelingEntitet(List<OppgittPeriodeEntitet> søknadsPerioder, boolean erAnnenForelderInformert, boolean ønskerJustertVedFødsel) {
        this.søknadsPerioder = new ArrayList<>();
        for (var oppgittPeriode : søknadsPerioder) {
            oppgittPeriode.setOppgittFordeling(this);
            this.søknadsPerioder.add(oppgittPeriode);
        }
        this.erAnnenForelderInformert = erAnnenForelderInformert;
        this.ønskerJustertVedFødsel = ønskerJustertVedFødsel;
    }

    public OppgittFordelingEntitet(List<OppgittPeriodeEntitet> søknadsPerioder, boolean erAnnenForelderInformert) {
        this(søknadsPerioder, erAnnenForelderInformert, false);
    }

    public List<OppgittPeriodeEntitet> getPerioder() {
        return new ArrayList<>(søknadsPerioder);
    }

    public boolean getErAnnenForelderInformert() {
        return erAnnenForelderInformert;
    }

    public boolean ønskerJustertVedFødsel() {
        return ønskerJustertVedFødsel != null && ønskerJustertVedFødsel;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OppgittFordelingEntitet that)) {
            return false;
        }
        return Objects.equals(søknadsPerioder, that.søknadsPerioder) &&
                Objects.equals(erAnnenForelderInformert, that.erAnnenForelderInformert) &&
                Objects.equals(ønskerJustertVedFødsel, that.ønskerJustertVedFødsel)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(søknadsPerioder, erAnnenForelderInformert, ønskerJustertVedFødsel);
    }
}
