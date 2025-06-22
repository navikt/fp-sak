package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.InntektskategoriKlassekodeMapper;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;


@Entity(name = "BeregningsresultatAndel")
@Table(name = "BR_ANDEL")
public class BeregningsresultatAndel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_ANDEL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "br_periode_id", nullable = false, updatable = false)
    @JsonBackReference
    private BeregningsresultatPeriode beregningsresultatPeriode;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "bruker_er_mottaker", nullable = false)
    private Boolean brukerErMottaker;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Convert(converter = OpptjeningAktivitetType.KodeverdiConverter.class)
    @Column(name="arbeidsforhold_type", nullable = false)
    private OpptjeningAktivitetType arbeidsforholdType;

    @Column(name = "dagsats", nullable = false)
    private Integer dagsats;

    @Column(name = "stillingsprosent", nullable = false)
    private BigDecimal stillingsprosent;

    @Column(name = "utbetalingsgrad", nullable = false)
    private BigDecimal utbetalingsgrad;

    @Column(name = "dagsats_fra_bg", nullable = false)
    private Integer dagsatsFraBg;

    @Convert(converter=AktivitetStatus.KodeverdiConverter.class)
    @Column(name="aktivitet_status", nullable = false)
    private AktivitetStatus aktivitetStatus;

    @Convert(converter=Inntektskategori.KodeverdiConverter.class)
    @Column(name="inntektskategori", nullable = false)
    private Inntektskategori inntektskategori;

    public BeregningsresultatAndel() {
    }

    public Long getId() {
        return id;
    }

    public BeregningsresultatPeriode getBeregningsresultatPeriode() {
        return beregningsresultatPeriode;
    }

    public boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    public String getArbeidsforholdIdentifikator() {
        return arbeidsgiver == null ? null : arbeidsgiver.getIdentifikator();
    }

    public boolean erArbeidsgiverPrivatperson() {
        return getArbeidsgiver().map(Arbeidsgiver::erAktørId).orElse(false);
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public boolean skalTilBrukerEllerPrivatperson() {
        return brukerErMottaker || this.erArbeidsgiverPrivatperson();
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public int getDagsats() {
        return dagsats;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public int getDagsatsFraBg() {
        return dagsatsFraBg;
    }

    public int getUtledetDagsatsFraBg() {
        if (getDagsats() == 0 || utbetalingsgrad.compareTo(BigDecimal.ZERO) == 0) {
            return dagsatsFraBg;
        } else if (getDagsats() == getDagsatsFraBg() && utbetalingsgrad.compareTo(BigDecimal.valueOf(100)) < 0) {
            // Tilfelle der utbetalingsgrad er mindre enn 100% men dagsatsFraBg er lik dagsats. Regn ut dagsatsFraBg
            var utledet = BigDecimal.valueOf(dagsats).multiply(BigDecimal.valueOf(100)).divide(utbetalingsgrad, 0, RoundingMode.HALF_UP);
            return utledet.intValue();
        } else {
            return dagsatsFraBg;
        }
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    /**
     * Returnerer en aktivitetsnøkkel som kan brukes til å identifisere like andeler
     * men som ikke skiller på andeler hos samme arbeidsgiver på forskjellige arbeidsforhold.
     * @return Nøkkel med Aktivitetstatus og arbeidsgiver
     */
    public AktivitetOgArbeidsgiverNøkkel getAktivitetOgArbeidsgiverNøkkel() {
        return new AktivitetOgArbeidsgiverNøkkel(this);
    }

    @Override
    public String toString() {
        return "BeregningsresultatAndel{" +
            "brukerErMottaker=" + brukerErMottaker +
            ", arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", dagsats=" + dagsats +
            ", aktivitetStatus=" + aktivitetStatus +
            ", inntektskategori=" + inntektskategori +
            '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsresultatAndel other)) {
            return false;
        }
        return Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver())
            && Objects.equals(this.getArbeidsforholdRef(), other.getArbeidsforholdRef())
            && Objects.equals(this.getArbeidsforholdType(), other.getArbeidsforholdType())
            && Objects.equals(this.getAktivitetStatus(), other.getAktivitetStatus())
            && Objects.equals(this.getInntektskategori(), other.getInntektskategori())
            && Objects.equals(this.erBrukerMottaker(), other.erBrukerMottaker())
            && Objects.equals(this.getDagsats(), other.getDagsats())
            && Objects.equals(this.getStillingsprosent(), other.getStillingsprosent())
            && Objects.equals(this.getUtbetalingsgrad(), other.getUtbetalingsgrad())
            && Objects.equals(this.getDagsatsFraBg(), other.getDagsatsFraBg());
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukerErMottaker, arbeidsgiver, arbeidsforholdRef, arbeidsforholdType, dagsats, aktivitetStatus, dagsatsFraBg, inntektskategori);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatAndel eksisterendeBeregningsresultatAndel) {
        return new Builder(eksisterendeBeregningsresultatAndel);
    }

    public static class Builder {

        private BeregningsresultatAndel beregningsresultatAndelMal;

        public Builder() {
            beregningsresultatAndelMal = new BeregningsresultatAndel();
            beregningsresultatAndelMal.arbeidsforholdType = OpptjeningAktivitetType.UDEFINERT;
        }

        public Builder(BeregningsresultatAndel eksisterendeBeregningsresultatAndel) {
            beregningsresultatAndelMal = eksisterendeBeregningsresultatAndel;
        }

        public Builder medBrukerErMottaker(boolean brukerErMottaker) {
            beregningsresultatAndelMal.brukerErMottaker = brukerErMottaker;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            beregningsresultatAndelMal.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            beregningsresultatAndelMal.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
            beregningsresultatAndelMal.arbeidsforholdType = arbeidsforholdType;
            return this;
        }

        public Builder medDagsats(int dagsats) {
            beregningsresultatAndelMal.dagsats = dagsats;
            return this;
        }

        public Builder medStillingsprosent(BigDecimal stillingsprosent) {
            beregningsresultatAndelMal.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            beregningsresultatAndelMal.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medDagsatsFraBg(int dagsatsFraBg) {
            beregningsresultatAndelMal.dagsatsFraBg = dagsatsFraBg;
            return this;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            beregningsresultatAndelMal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            beregningsresultatAndelMal.inntektskategori = inntektskategori;
            return this;
        }

        public BeregningsresultatAndel build(BeregningsresultatPeriode beregningsresultatPeriode) {
            beregningsresultatAndelMal.beregningsresultatPeriode = beregningsresultatPeriode;
            verifyStateForBuild();
            beregningsresultatAndelMal.getBeregningsresultatPeriode().addBeregningsresultatAndel(beregningsresultatAndelMal);
            return beregningsresultatAndelMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsresultatAndelMal.aktivitetStatus, "aktivitetStatus");
            Objects.requireNonNull(beregningsresultatAndelMal.beregningsresultatPeriode, "beregningsresultatPeriode");
            Objects.requireNonNull(beregningsresultatAndelMal.brukerErMottaker, "brukerErMottaker");
            Objects.requireNonNull(beregningsresultatAndelMal.stillingsprosent, "stillingsprosent");
            verifyUtbetalingsgrad(beregningsresultatAndelMal.utbetalingsgrad);
            Objects.requireNonNull(beregningsresultatAndelMal.inntektskategori, "inntektskategori");
            Objects.requireNonNull(beregningsresultatAndelMal.dagsatsFraBg, "dagsatsFraBg");
            Objects.requireNonNull(beregningsresultatAndelMal.dagsats, "dagsats");
            if (!beregningsresultatAndelMal.brukerErMottaker) {
                Objects.requireNonNull(beregningsresultatAndelMal.arbeidsgiver, "virksomhet");
            }
            InntektskategoriKlassekodeMapper.verifyInntektskategori(beregningsresultatAndelMal.inntektskategori);
        }

        private void verifyUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            Objects.requireNonNull(utbetalingsgrad, "utbetalingsgrad");
            var mellomGyldigIntervall = utbetalingsgrad.compareTo(BigDecimal.ZERO) >= 0 &&
                utbetalingsgrad.compareTo(BigDecimal.valueOf(100)) <= 0;
            if (!mellomGyldigIntervall) {
                throw new IllegalStateException("Utviklerfeil: Utbetalingsgrad må være mellom 0 og 100");
            }
        }

    }
}
