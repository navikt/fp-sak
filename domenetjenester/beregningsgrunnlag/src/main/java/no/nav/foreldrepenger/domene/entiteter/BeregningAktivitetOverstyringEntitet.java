package no.nav.foreldrepenger.domene.entiteter;

import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@Entity(name = "BeregningAktivitetOverstyring")
@Table(name = "BG_AKTIVITET_OVERSTYRING")
public class BeregningAktivitetOverstyringEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_AKTIVITET_OVERSTYRING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "fom"))
    @AttributeOverride(name = "tomDato", column = @Column(name = "tom"))
    private ÅpenDatoIntervallEntitet periode;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Convert(converter = BeregningAktivitetHandlingType.KodeverdiConverter.class)
    @Column(name = "handling_type", nullable = false)
    private BeregningAktivitetHandlingType handlingType;

    @Convert(converter = OpptjeningAktivitetType.KodeverdiConverter.class)
    @Column(name = "opptjening_aktivitet_type", nullable = false)
    private OpptjeningAktivitetType opptjeningAktivitetType;

    @JsonBackReference
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ba_overstyringer_id", nullable = false, updatable = false)
    private BeregningAktivitetOverstyringerEntitet overstyringerEntitet;

    public BeregningAktivitetOverstyringEntitet(BeregningAktivitetOverstyringEntitet beregningAktivitetOverstyringEntitet) {
        this.arbeidsforholdRef = beregningAktivitetOverstyringEntitet.getArbeidsforholdRef();
        this.arbeidsgiver = beregningAktivitetOverstyringEntitet.getArbeidsgiver().orElse(null);
        this.handlingType = beregningAktivitetOverstyringEntitet.getHandling();
        this.opptjeningAktivitetType = beregningAktivitetOverstyringEntitet.getOpptjeningAktivitetType();
        this.periode = beregningAktivitetOverstyringEntitet.getPeriode();
    }

    protected BeregningAktivitetOverstyringEntitet() {

    }

    public BeregningAktivitetHandlingType getHandling() {
        return handlingType;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }

    public ÅpenDatoIntervallEntitet getPeriode() {
        return periode;
    }

    public BeregningAktivitetNøkkel getNøkkel() {
        return BeregningAktivitetNøkkel.builder()
            .medArbeidsgiverIdentifikator(getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null))
            .medArbeidsforholdRef(arbeidsforholdRef != null ? arbeidsforholdRef.getReferanse() : null)
            .medOpptjeningAktivitetType(opptjeningAktivitetType)
            .medFom(periode.getFomDato())
            .build();
    }

    void setBeregningAktivitetOverstyringer(BeregningAktivitetOverstyringerEntitet overstyringerEntitet) {
        this.overstyringerEntitet = overstyringerEntitet;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningAktivitetOverstyringEntitet kladd;

        private Builder() {
            kladd = new BeregningAktivitetOverstyringEntitet();
        }

        public Builder medPeriode(ÅpenDatoIntervallEntitet periode) {
            kladd.periode = periode;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType opptjeningAktivitetType) {
            kladd.opptjeningAktivitetType = opptjeningAktivitetType;
            return this;
        }

        public Builder medHandling(BeregningAktivitetHandlingType beregningAktivitetHandlingType) {
            kladd.handlingType = beregningAktivitetHandlingType;
            return this;
        }

        public BeregningAktivitetOverstyringEntitet build() {
            return kladd;
        }
    }
}
