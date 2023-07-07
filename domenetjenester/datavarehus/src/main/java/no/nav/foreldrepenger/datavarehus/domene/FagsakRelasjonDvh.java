package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

@Entity(name = "FagsakRelasjonDvh")
@Table(name = "FAGSAK_RELASJON_DVH")
public class FagsakRelasjonDvh extends DvhBaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAGSAK_RELASJON_DVH")
    @Column(name = "TRANS_ID")
    private Long id;

    @Column(name = "FAGSAK_EN_ID", nullable = false)
    private Long fagsakNrEn;

    @Column(name = "FAGSAK_TO_ID")
    private Long fagsakNrTo;

    @AttributeOverride(name = "verdi", column = @Column(name = "dekningsgrad", nullable = false))
    @Embedded
    private Dekningsgrad dekningsgrad;

    @Column(name = "AVSLUTTNINGSDATO")
    private LocalDate avsluttningsdato;


    FagsakRelasjonDvh() {
        // hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FagsakRelasjonDvh castOther)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        return Objects.equals(fagsakNrEn, castOther.fagsakNrEn)
            && Objects.equals(fagsakNrTo, castOther.fagsakNrTo)
            && Objects.equals(dekningsgrad, castOther.dekningsgrad)
            && Objects.equals(avsluttningsdato, castOther.avsluttningsdato);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fagsakNrEn, fagsakNrTo, dekningsgrad, avsluttningsdato);
    }

    public static class Builder {

        private Long fagsakNrEn;
        private Long fagsakNrTo;
        private Dekningsgrad dekningsgrad;
        private LocalDate avsluttningsdato;
        private LocalDateTime funksjonellTid;
        private String endretAv;


        public Builder fagsakNrEn(Long fagsakNrEn) {
            this.fagsakNrEn = fagsakNrEn;
            return this;
        }

        public Builder fagsakNrTo(Long fagsakNrTo) {
            this.fagsakNrTo = fagsakNrTo;
            return this;
        }

        public Builder dekningsgrad(Dekningsgrad dekningsgrad) {
            this.dekningsgrad = dekningsgrad;
            return this;
        }

        public Builder avsluttningsdato(LocalDate avsluttningsdato) {
            this.avsluttningsdato = avsluttningsdato;
            return this;
        }


        public Builder funksjonellTid(LocalDateTime funksjonellTid) {
            this.funksjonellTid = funksjonellTid;
            return this;
        }

        public Builder endretAv(String endretAv) {
            this.endretAv = endretAv;
            return this;
        }

        public FagsakRelasjonDvh build() {
            var fagsakRelasjonDvh = new FagsakRelasjonDvh();
            fagsakRelasjonDvh.fagsakNrEn = fagsakNrEn;
            fagsakRelasjonDvh.fagsakNrTo = fagsakNrTo;
            fagsakRelasjonDvh.dekningsgrad = dekningsgrad;
            fagsakRelasjonDvh.avsluttningsdato = avsluttningsdato;
            fagsakRelasjonDvh.setFunksjonellTid(funksjonellTid);
            fagsakRelasjonDvh.setEndretAv(endretAv);
            return fagsakRelasjonDvh;
        }
    }
}
