package no.nav.foreldrepenger.domene.entiteter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "BGAndelArbeidsforhold")
@Table(name = "BG_ANDEL_ARBEIDSFORHOLD")
public class BGAndelArbeidsforhold extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_ANDEL_ARBEIDSFORHOLD")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @OneToOne(optional = false)
    @JoinColumn(name = "bg_andel_id", nullable = false, updatable = false)
    private BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Column(name = "refusjonskrav_pr_aar")
    private BigDecimal refusjonskravPrÅr;

    @Column(name = "saksbehandlet_refusjon_pr_aar")
    private BigDecimal saksbehandletRefusjonPrÅr;

    @Column(name = "fordelt_refusjon_pr_aar")
    private BigDecimal fordeltRefusjonPrÅr;

    @Column(name = "manuelt_fordelt_refusjon_pr_aar")
    private BigDecimal manueltFordeltRefusjonPrÅr;

    @Column(name = "naturalytelse_bortfalt_pr_aar")
    private BigDecimal naturalytelseBortfaltPrÅr;

    @Column(name = "naturalytelse_tilkommet_pr_aar")
    private BigDecimal naturalytelseTilkommetPrÅr;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "tidsbegrenset_arbeidsforhold")
    private Boolean erTidsbegrensetArbeidsforhold;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "loennsendring_i_perioden")
    private Boolean lønnsendringIBeregningsperioden;

    @Column(name = "arbeidsperiode_fom")
    private LocalDate arbeidsperiodeFom;

    @Column(name = "arbeidsperiode_tom")
    private LocalDate arbeidsperiodeTom;

    public BGAndelArbeidsforhold(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        this.arbeidsforholdRef = bgAndelArbeidsforhold.arbeidsforholdRef;
        this.arbeidsgiver = bgAndelArbeidsforhold.arbeidsgiver;
        this.arbeidsperiodeFom = bgAndelArbeidsforhold.arbeidsperiodeFom;
        this.arbeidsperiodeTom = bgAndelArbeidsforhold.arbeidsperiodeTom;
        this.erTidsbegrensetArbeidsforhold = bgAndelArbeidsforhold.erTidsbegrensetArbeidsforhold;
        this.fordeltRefusjonPrÅr = bgAndelArbeidsforhold.fordeltRefusjonPrÅr;
        this.lønnsendringIBeregningsperioden = bgAndelArbeidsforhold.lønnsendringIBeregningsperioden;
        this.naturalytelseBortfaltPrÅr = bgAndelArbeidsforhold.naturalytelseBortfaltPrÅr;
        this.refusjonskravPrÅr = bgAndelArbeidsforhold.refusjonskravPrÅr;
        this.saksbehandletRefusjonPrÅr = bgAndelArbeidsforhold.saksbehandletRefusjonPrÅr;
        this.naturalytelseTilkommetPrÅr = bgAndelArbeidsforhold.naturalytelseTilkommetPrÅr;
        this.manueltFordeltRefusjonPrÅr = bgAndelArbeidsforhold.manueltFordeltRefusjonPrÅr;
    }

    public BGAndelArbeidsforhold() {
    }

    public Long getId() {
        return id;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    public BigDecimal getRefusjonskravPrÅr() {
        return refusjonskravPrÅr;
    }

    public Optional<BigDecimal> getNaturalytelseBortfaltPrÅr() {
        return Optional.ofNullable(naturalytelseBortfaltPrÅr);
    }

    public Optional<BigDecimal> getNaturalytelseTilkommetPrÅr() {
        return Optional.ofNullable(naturalytelseTilkommetPrÅr);
    }

    public Boolean getErTidsbegrensetArbeidsforhold() {
        return erTidsbegrensetArbeidsforhold;
    }

    public Boolean erLønnsendringIBeregningsperioden() {
        return lønnsendringIBeregningsperioden;
    }

    public LocalDate getArbeidsperiodeFom() {
        return arbeidsperiodeFom;
    }

    public Optional<LocalDate> getArbeidsperiodeTom() {
        return Optional.ofNullable(arbeidsperiodeTom);
    }

    public DatoIntervallEntitet getArbeidsperiode() {
        if (arbeidsperiodeTom == null) {
            return DatoIntervallEntitet.fraOgMed(arbeidsperiodeFom);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsperiodeFom, arbeidsperiodeTom);
    }

    public String getArbeidsforholdOrgnr() {
        return getArbeidsgiver().getOrgnr();
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public BigDecimal getSaksbehandletRefusjonPrÅr() {
        return saksbehandletRefusjonPrÅr;
    }

    public BigDecimal getFordeltRefusjonPrÅr() {
        return fordeltRefusjonPrÅr;
    }

    public BigDecimal getManueltFordeltRefusjonPrÅr() {
        return manueltFordeltRefusjonPrÅr;
    }

    void setBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
        this.beregningsgrunnlagPrStatusOgAndel = beregningsgrunnlagPrStatusOgAndel;
    }

    /**
     * Refusjonskrav settes på forskjellige steder i beregning dersom aksjonspunkt oppstår.
     * Først settes refusjonskravPrÅr, deretter saksbehandletRefusjonPrÅr,
     * deretter fordeltRefusjonPrÅr og til slutt manueltFordeltRefusjonPrÅr.
     * Det er det sist avklarte beløpet som til en hver tid skal være gjeldende.
     * @return returnerer det refusjonsbeløpet som skal være gjeldende
     */
    public BigDecimal getGjeldendeRefusjon() {
        if (manueltFordeltRefusjonPrÅr != null) {
            return manueltFordeltRefusjonPrÅr;
        }
        if (fordeltRefusjonPrÅr != null) {
            return fordeltRefusjonPrÅr;
        }
        if (saksbehandletRefusjonPrÅr != null) {
            return saksbehandletRefusjonPrÅr;
        }
        return refusjonskravPrÅr;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BGAndelArbeidsforhold)) {
            return false;
        }
        var other = (BGAndelArbeidsforhold) obj;
        return Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver())
                && Objects.equals(this.arbeidsforholdRef, other.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArbeidsgiver(), arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                "id=" + id + ", " //$NON-NLS-2$
                + "orgnr=" + getArbeidsforholdOrgnr() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "arbeidsgiver=" + arbeidsgiver + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "arbeidsforholdRef=" + arbeidsforholdRef + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "naturalytelseBortfaltPrÅr=" + naturalytelseBortfaltPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "naturalytelseTilkommetPrÅr=" + naturalytelseTilkommetPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "refusjonskravPrÅr=" + refusjonskravPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "arbeidsperiodeFom=" + arbeidsperiodeFom //$NON-NLS-1$
                + "arbeidsperiodeTom=" + arbeidsperiodeTom //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        return bgAndelArbeidsforhold == null ? new Builder() : new Builder(bgAndelArbeidsforhold);
    }

    public static class Builder {
        private BGAndelArbeidsforhold bgAndelArbeidsforhold;

        private Builder() {
            bgAndelArbeidsforhold = new BGAndelArbeidsforhold();
        }

        private Builder(BGAndelArbeidsforhold eksisterendeBGAndelArbeidsforhold) {
            bgAndelArbeidsforhold = eksisterendeBGAndelArbeidsforhold;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            bgAndelArbeidsforhold.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(String arbeidsforholdRef) {
            return medArbeidsforholdRef(arbeidsforholdRef==null?null:InternArbeidsforholdRef.ref(arbeidsforholdRef));
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            bgAndelArbeidsforhold.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medNaturalytelseBortfaltPrÅr(BigDecimal naturalytelseBortfaltPrÅr) {
            bgAndelArbeidsforhold.naturalytelseBortfaltPrÅr = naturalytelseBortfaltPrÅr;
            return this;
        }

        public Builder medNaturalytelseTilkommetPrÅr(BigDecimal naturalytelseTilkommetPrÅr) {
            bgAndelArbeidsforhold.naturalytelseTilkommetPrÅr = naturalytelseTilkommetPrÅr;
            return this;
        }

        public Builder medRefusjonskravPrÅr(BigDecimal refusjonskravPrÅr) {
            bgAndelArbeidsforhold.refusjonskravPrÅr = refusjonskravPrÅr;
            return this;
        }

        public Builder medSaksbehandletRefusjonPrÅr(BigDecimal saksbehandletRefusjonPrÅr) {
            bgAndelArbeidsforhold.saksbehandletRefusjonPrÅr = saksbehandletRefusjonPrÅr;
            return this;
        }

        public Builder medFordeltRefusjonPrÅr(BigDecimal fordeltRefusjonPrÅr) {
            bgAndelArbeidsforhold.fordeltRefusjonPrÅr = fordeltRefusjonPrÅr;
            return this;
        }

        public Builder medManueltFordeltRefusjonPrÅr(BigDecimal manueltFordeltRefusjonPrÅr) {
            bgAndelArbeidsforhold.manueltFordeltRefusjonPrÅr = manueltFordeltRefusjonPrÅr;
            return this;
        }

        public BGAndelArbeidsforhold.Builder medTidsbegrensetArbeidsforhold(Boolean erTidsbegrensetArbeidsforhold) {
            bgAndelArbeidsforhold.erTidsbegrensetArbeidsforhold = erTidsbegrensetArbeidsforhold;
            return this;
        }

        public Builder medLønnsendringIBeregningsperioden(Boolean lønnsendringIBeregningsperioden) {
            bgAndelArbeidsforhold.lønnsendringIBeregningsperioden = lønnsendringIBeregningsperioden;
            return this;
        }

        public Builder medArbeidsperiodeFom(LocalDate arbeidsperiodeFom) {
            bgAndelArbeidsforhold.arbeidsperiodeFom = arbeidsperiodeFom;
            return this;
        }

        public Builder medArbeidsperiodeTom(LocalDate arbeidsperiodeTom) {
            bgAndelArbeidsforhold.arbeidsperiodeTom = arbeidsperiodeTom;
            return this;
        }

        BGAndelArbeidsforhold build(BeregningsgrunnlagPrStatusOgAndel andel) {
            Objects.requireNonNull(bgAndelArbeidsforhold.arbeidsgiver, "arbeidsgiver");
            andel.setBgAndelArbeidsforhold(bgAndelArbeidsforhold);
            return bgAndelArbeidsforhold;
        }
    }
}
