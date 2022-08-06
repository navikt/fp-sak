package no.nav.foreldrepenger.behandlingslager.uttak;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity
@Table(name = "UTTAKSPERIODEGRENSE")
public class Uttaksperiodegrense extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAKSPERIODEGRENSE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "behandling_resultat_id", nullable = false)
    private Behandlingsresultat behandlingsresultat;

    @Column(name = "MOTTATTDATO", nullable = false)
    private LocalDate mottattDato;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Uttaksperiodegrense() {
    }

    public Uttaksperiodegrense(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public Long getId(){
        return id;
    }

    void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public boolean getErAktivt() {
        return aktiv;
    }

    void setAktiv(boolean aktivt) {
        this.aktiv = aktivt;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }
}
