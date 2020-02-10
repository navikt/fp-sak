package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Table(name = "OKO_OMPOSTERING_116")
@Entity(name = "Ompostering116")
public class Ompostering116 extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OKO_OMPOSTERING_116")
    private Long id;

    @OneToOne
    @JoinColumn(name = "oko_oppdrag_110_id", nullable = false)
    private Oppdrag110 oppdrag110;

    @Column(name = "om_postering", nullable = false)
    private String omPostering;

    @Column(name = "dato_omposter_fom")
    private LocalDate datoOmposterFom;

    @Column(name = "tidspkt_reg", nullable = false)
    private String tidspktReg;

    @Column(name = "saksbeh_id", nullable = false)
    private String saksbehId;

    Ompostering116() {
        // For hibernate
    }

    public String getOmPostering() {
        return omPostering;
    }

    public LocalDate getDatoOmposterFom() {
        return datoOmposterFom;
    }

    public String getTidspktReg() {
        return tidspktReg;
    }

    public String getSaksbehId() {
        return saksbehId;
    }

    void setOppdrag110(Oppdrag110 oppdrag110) {
        this.oppdrag110 = oppdrag110;
    }

    public Long getId() {
        return id;
    }

    public static class Builder {
        Ompostering116 kladd = new Ompostering116();

        public Builder medOmPostering(String ompostering) {
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

        public Builder medSaksbehId(String saksbehId) {
            kladd.saksbehId = saksbehId;
            return this;
        }

        public Ompostering116 build() {
            verifiser();
            return kladd;
        }

        private void verifiser() {
            Objects.requireNonNull(kladd.omPostering);
            Objects.requireNonNull(kladd.tidspktReg);
            Objects.requireNonNull(kladd.saksbehId);
        }
    }
}
