package no.nav.foreldrepenger.behandlingslager.behandling.beregning;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
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

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "BeregningsresultatFeriepengerPrÅr")
@Table(name = "BR_FERIEPENGER_PR_AAR")
public class BeregningsresultatFeriepengerPrÅr extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_FERIEPENGER_PR_AAR")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "br_feriepenger_id", nullable = false, updatable = false)
    private BeregningsresultatFeriepenger beregningsresultatFeriepenger;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beregningsresultat_andel_id")
    private BeregningsresultatAndel beregningsresultatAndel;

    @Convert(converter=AktivitetStatus.KodeverdiConverter.class)
    @Column(name="aktivitet_status")
    private AktivitetStatus aktivitetStatus;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "bruker_er_mottaker", nullable = false)
    private Boolean brukerErMottaker;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Column(name = "opptjeningsaar", nullable = false)
    private LocalDate opptjeningsår;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "aarsbeloep", nullable = false))
    @ChangeTracked
    private Beløp årsbeløp;

    public BeregningsresultatFeriepengerPrÅr() {
    }

    public Long getId() {
        return id;
    }

    public BeregningsresultatFeriepenger getBeregningsresultatFeriepenger() {
        return beregningsresultatFeriepenger;
    }

    public BeregningsresultatAndel getBeregningsresultatAndel() {
        return beregningsresultatAndel;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
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
        return getArbeidsgiver().filter(Arbeidsgiver::erAktørId).isPresent();
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public boolean skalTilBrukerEllerPrivatperson() {
        return brukerErMottaker || this.erArbeidsgiverPrivatperson();
    }

    public LocalDate getOpptjeningsår() {
        return opptjeningsår;
    }

    public int getOpptjeningsåret() {
        return opptjeningsår.getYear();
    }

    public Beløp getÅrsbeløp() {
        return årsbeløp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsresultatFeriepengerPrÅr other)) {
            return false;
        }
        return Objects.equals(this.getOpptjeningsår(), other.getOpptjeningsår())
            && Objects.equals(this.getÅrsbeløp(), other.getÅrsbeløp())
            && Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver())
            && Objects.equals(this.getArbeidsforholdRef(), other.getArbeidsforholdRef())
            && Objects.equals(this.getAktivitetStatus(), other.getAktivitetStatus())
            && Objects.equals(this.erBrukerMottaker(), other.erBrukerMottaker());
    }

    @Override
    public int hashCode() {
        return Objects.hash(opptjeningsår, årsbeløp, brukerErMottaker, arbeidsgiver, arbeidsforholdRef, aktivitetStatus);
    }

    @Override
    public String toString() {
        return "BRFerPrÅr{" +
            "brFerie=" + beregningsresultatFeriepenger +
            ", opptjeningsår=" + opptjeningsår +
            ", årsbeløp=" + årsbeløp +
            ", brukerErMottaker=" + brukerErMottaker +
            ", arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", aktivitetStatus=" + aktivitetStatus +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsresultatFeriepengerPrÅr beregningsresultatFeriepengerPrÅrMal;

        public Builder() {
            beregningsresultatFeriepengerPrÅrMal = new BeregningsresultatFeriepengerPrÅr();
        }

        public Builder medOpptjeningsår(int opptjeningsår) {
            beregningsresultatFeriepengerPrÅrMal.opptjeningsår = LocalDate.of(opptjeningsår, 12, 31);
            return this;
        }

        public Builder medOpptjeningsår(LocalDate opptjeningsår) {
            beregningsresultatFeriepengerPrÅrMal.opptjeningsår = opptjeningsår;
            return this;
        }

        public Builder medÅrsbeløp(int årsbeløp) {
            beregningsresultatFeriepengerPrÅrMal.årsbeløp = new Beløp(BigDecimal.valueOf(årsbeløp));
            return this;
        }

        public Builder medÅrsbeløp(Long årsbeløp) {
            beregningsresultatFeriepengerPrÅrMal.årsbeløp = new Beløp(BigDecimal.valueOf(årsbeløp));
            return this;
        }

        public BeregningsresultatFeriepengerPrÅr build(BeregningsresultatFeriepenger beregningsresultatFeriepenger, BeregningsresultatAndel beregningsresultatAndel) {
            beregningsresultatFeriepengerPrÅrMal.beregningsresultatFeriepenger = beregningsresultatFeriepenger;
            BeregningsresultatFeriepenger.builder(beregningsresultatFeriepenger).leggTilBeregningsresultatFeriepengerPrÅr(beregningsresultatFeriepengerPrÅrMal);
            beregningsresultatFeriepengerPrÅrMal.beregningsresultatAndel = beregningsresultatAndel;
            beregningsresultatFeriepengerPrÅrMal.brukerErMottaker = beregningsresultatAndel.erBrukerMottaker();
            beregningsresultatFeriepengerPrÅrMal.aktivitetStatus = beregningsresultatAndel.getAktivitetStatus();
            beregningsresultatAndel.getArbeidsgiver().ifPresent(a -> beregningsresultatFeriepengerPrÅrMal.arbeidsgiver = a);
            beregningsresultatFeriepengerPrÅrMal.arbeidsforholdRef = beregningsresultatAndel.getArbeidsforholdRef();
            BeregningsresultatAndel.builder(beregningsresultatAndel).leggTilBeregningsresultatFeriepengerPrÅr(beregningsresultatFeriepengerPrÅrMal);
            verifyStateForBuild();
            return beregningsresultatFeriepengerPrÅrMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.beregningsresultatFeriepenger, "beregningsresultatFeriepenger");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.beregningsresultatAndel, "beregningsresultatAndel");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.aktivitetStatus, "aktivitetStatus");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.brukerErMottaker, "brukerErMottaker");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.opptjeningsår, "opptjeningsår");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.årsbeløp, "årsbeløp");
        }
    }
}
