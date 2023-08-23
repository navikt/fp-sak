package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.Objects;

@Immutable
@Table(name = "OKO_OMPOSTERING_116")
@Entity(name = "Ompostering116")
public class Ompostering116 extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OKO_OMPOSTERING_116")
    private Long id;

    @OneToOne
    @JoinColumn(name = "oko_oppdrag_110_id", nullable = false)
    private Oppdrag110 oppdrag110;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "om_postering", nullable = false)
    private Boolean omPostering;

    @Column(name = "dato_omposter_fom")
    private LocalDate datoOmposterFom;

    @Column(name = "tidspkt_reg", nullable = false)
    private String tidspktReg;

    protected Ompostering116() {
        // For hibernate
    }

    public Boolean getOmPostering() {
        return omPostering;
    }

    public LocalDate getDatoOmposterFom() {
        return datoOmposterFom;
    }

    public String getTidspktReg() {
        return tidspktReg;
    }

    void setOppdrag110(Oppdrag110 oppdrag110) {
        this.oppdrag110 = oppdrag110;
    }

    public Long getId() {
        return id;
    }

    public static class Builder {
        Ompostering116 kladd = new Ompostering116();

        public Builder medOmPostering(Boolean ompostering) {
            kladd.omPostering = ompostering;
            return this;
        }

        public Builder medDatoOmposterFom(LocalDate datoOmposterFom) {
            kladd.datoOmposterFom = datoOmposterFom;
            return this;
        }

        public Builder medTidspktReg(String tidspktReg) {
            kladd.tidspktReg = tidspktReg;
            return this;
        }

        public Ompostering116 build() {
            verifiser();
            return kladd;
        }

        private void verifiser() {
            Objects.requireNonNull(kladd.omPostering);
            Objects.requireNonNull(kladd.tidspktReg);
        }
    }
}
