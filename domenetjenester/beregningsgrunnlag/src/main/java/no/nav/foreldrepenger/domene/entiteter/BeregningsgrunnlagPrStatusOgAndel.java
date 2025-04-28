package no.nav.foreldrepenger.domene.entiteter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "BeregningsgrunnlagPrStatusOgAndel")
@Table(name = "BG_PR_STATUS_OG_ANDEL")
public class BeregningsgrunnlagPrStatusOgAndel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PR_STATUS_OG_ANDEL")
    private Long id;

    @Column(name = "andelsnr", nullable = false)
    private Long andelsnr;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "bg_periode_id", nullable = false, updatable = false)
    private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;

    @Convert(converter= AktivitetStatus.KodeverdiConverter.class)
    @Column(name="aktivitet_status", nullable = false)
    private AktivitetStatus aktivitetStatus;

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "beregningsperiode_fom"))
    @AttributeOverride(name = "tomDato", column = @Column(name = "beregningsperiode_tom"))
    private ÅpenDatoIntervallEntitet beregningsperiode;

    @Convert(converter = OpptjeningAktivitetType.KodeverdiConverter.class)
    @Column(name="arbeidsforhold_type", nullable = false)
    private OpptjeningAktivitetType arbeidsforholdType;

    @Column(name = "brutto_pr_aar")
    private BigDecimal bruttoPrÅr;

    @Column(name = "overstyrt_pr_aar")
    private BigDecimal overstyrtPrÅr;

    @Column(name = "avkortet_pr_aar")
    private BigDecimal avkortetPrÅr;

    @Column(name = "redusert_pr_aar")
    private BigDecimal redusertPrÅr;

    @Column(name = "beregnet_pr_aar")
    private BigDecimal beregnetPrÅr;

    @Column(name = "fordelt_pr_aar")
    private BigDecimal fordeltPrÅr;

    @Column(name = "manuelt_fordelt_pr_aar")
    private BigDecimal manueltFordeltPrÅr;

    @Column(name = "maksimal_refusjon_pr_aar")
    private BigDecimal maksimalRefusjonPrÅr;

    @Column(name = "avkortet_refusjon_pr_aar")
    private BigDecimal avkortetRefusjonPrÅr;

    @Column(name = "redusert_refusjon_pr_aar")
    private BigDecimal redusertRefusjonPrÅr;

    @Column(name = "avkortet_brukers_andel_pr_aar")
    private BigDecimal avkortetBrukersAndelPrÅr;

    @Column(name = "redusert_brukers_andel_pr_aar")
    private BigDecimal redusertBrukersAndelPrÅr;

    @Column(name = "dagsats_bruker")
    private Long dagsatsBruker;

    @Column(name = "dagsats_arbeidsgiver")
    private Long dagsatsArbeidsgiver;

    @Column(name = "pgi_snitt")
    private BigDecimal pgiSnitt;

    @Column(name = "pgi1")
    private BigDecimal pgi1;

    @Column(name = "pgi2")
    private BigDecimal pgi2;

    @Column(name = "pgi3")
    private BigDecimal pgi3;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "aarsbeloep_tilstoetende_ytelse"))
    @ChangeTracked
    private Beløp årsbeløpFraTilstøtendeYtelse;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "ny_i_arbeidslivet")
    private Boolean nyIArbeidslivet;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "fastsatt_av_saksbehandler", nullable = false)
    private Boolean fastsattAvSaksbehandler = false;

    @Column(name = "besteberegning_pr_aar")
    private BigDecimal besteberegningPrÅr;

    @Convert(converter= Inntektskategori.KodeverdiConverter.class)
    @Column(name="inntektskategori", nullable = false)
    private Inntektskategori inntektskategori = Inntektskategori.UDEFINERT;

    @Convert(converter=Inntektskategori.KodeverdiConverter.class)
    @Column(name="inntektskategori_manuell_fordeling")
    private Inntektskategori inntektskategoriManuellFordeling;

    @Convert(converter=Inntektskategori.KodeverdiConverter.class)
    @Column(name="inntektskategori_fordeling")
    private Inntektskategori inntektskategoriAutomatiskFordeling;

    @Convert(converter= AndelKilde.KodeverdiConverter.class)
    @Column(name="kilde", nullable = false)
    private AndelKilde kilde = AndelKilde.PROSESS_START;

    @OneToOne(mappedBy = "beregningsgrunnlagPrStatusOgAndel", cascade = CascadeType.PERSIST)
    private BGAndelArbeidsforhold bgAndelArbeidsforhold;

    @Column(name = "dagsats_tilstoetende_ytelse")
    private Long orginalDagsatsFraTilstøtendeYtelse;

    @OneToOne(mappedBy = "beregningsgrunnlagPrStatusOgAndel", cascade = CascadeType.PERSIST)
    private BeregningsgrunnlagFrilansAndel beregningsgrunnlagFrilansAndel;

    @OneToOne(mappedBy = "beregningsgrunnlagPrStatusOgAndel", cascade = CascadeType.PERSIST)
    private BeregningsgrunnlagArbeidstakerAndel beregningsgrunnlagArbeidstakerAndel;

    public BeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
        this.beregnetPrÅr = beregningsgrunnlagPrStatusOgAndel.getBeregnetPrÅr();
        this.aktivitetStatus = beregningsgrunnlagPrStatusOgAndel.getAktivitetStatus();
        this.andelsnr = beregningsgrunnlagPrStatusOgAndel.getAndelsnr();
        this.arbeidsforholdType = beregningsgrunnlagPrStatusOgAndel.getArbeidsforholdType();
        this.avkortetBrukersAndelPrÅr = beregningsgrunnlagPrStatusOgAndel.getAvkortetBrukersAndelPrÅr();
        this.avkortetPrÅr = beregningsgrunnlagPrStatusOgAndel.getAvkortetPrÅr();
        this.avkortetRefusjonPrÅr = beregningsgrunnlagPrStatusOgAndel.getAvkortetRefusjonPrÅr();
        this.beregningsperiode = beregningsgrunnlagPrStatusOgAndel.beregningsperiode;
        this.besteberegningPrÅr = beregningsgrunnlagPrStatusOgAndel.getBesteberegningPrÅr();
        this.bruttoPrÅr = beregningsgrunnlagPrStatusOgAndel.getBruttoPrÅr();
        this.dagsatsArbeidsgiver = beregningsgrunnlagPrStatusOgAndel.getDagsatsArbeidsgiver();
        this.dagsatsBruker = beregningsgrunnlagPrStatusOgAndel.getDagsatsBruker();
        this.fastsattAvSaksbehandler = beregningsgrunnlagPrStatusOgAndel.getFastsattAvSaksbehandler();
        this.fordeltPrÅr = beregningsgrunnlagPrStatusOgAndel.getFordeltPrÅr();
        this.manueltFordeltPrÅr = beregningsgrunnlagPrStatusOgAndel.getManueltFordeltPrÅr();
        this.inntektskategori = beregningsgrunnlagPrStatusOgAndel.getInntektskategori();
        this.inntektskategoriAutomatiskFordeling = beregningsgrunnlagPrStatusOgAndel.getInntektskategoriAutomatiskFordeling();
        this.inntektskategoriManuellFordeling = beregningsgrunnlagPrStatusOgAndel.getInntektskategoriManuellFordeling();
        this.kilde = beregningsgrunnlagPrStatusOgAndel.getKilde();
        this.maksimalRefusjonPrÅr = beregningsgrunnlagPrStatusOgAndel.getMaksimalRefusjonPrÅr();
        this.nyIArbeidslivet = beregningsgrunnlagPrStatusOgAndel.getNyIArbeidslivet();
        this.orginalDagsatsFraTilstøtendeYtelse = beregningsgrunnlagPrStatusOgAndel.getOrginalDagsatsFraTilstøtendeYtelse();
        this.overstyrtPrÅr = beregningsgrunnlagPrStatusOgAndel.getOverstyrtPrÅr();
        this.pgi1 = beregningsgrunnlagPrStatusOgAndel.getPgi1();
        this.pgi2 = beregningsgrunnlagPrStatusOgAndel.getPgi2();
        this.pgi3 = beregningsgrunnlagPrStatusOgAndel.getPgi3();
        this.pgiSnitt = beregningsgrunnlagPrStatusOgAndel.getPgiSnitt();
        this.redusertBrukersAndelPrÅr = beregningsgrunnlagPrStatusOgAndel.getRedusertBrukersAndelPrÅr();
        this.redusertPrÅr = beregningsgrunnlagPrStatusOgAndel.getRedusertPrÅr();
        this.redusertRefusjonPrÅr = beregningsgrunnlagPrStatusOgAndel.getRedusertRefusjonPrÅr();
        this.årsbeløpFraTilstøtendeYtelse = beregningsgrunnlagPrStatusOgAndel.getÅrsbeløpFraTilstøtendeYtelse();
        beregningsgrunnlagPrStatusOgAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::new)
            .ifPresent(this::setBgAndelArbeidsforhold);
        beregningsgrunnlagPrStatusOgAndel.getBeregningsgrunnlagArbeidstakerAndel()
            .map(BeregningsgrunnlagArbeidstakerAndel::new)
            .ifPresent(this::setBeregningsgrunnlagArbeidstakerAndel);
        beregningsgrunnlagPrStatusOgAndel.getBeregningsgrunnlagFrilansAndel()
            .map(BeregningsgrunnlagFrilansAndel::new)
            .ifPresent(this::setBeregningsgrunnlagFrilansAndel);
    }

    public BeregningsgrunnlagPrStatusOgAndel() {
    }

    public Long getId() {
        return id;
    }

    public BeregningsgrunnlagPeriode getBeregningsgrunnlagPeriode() {
        return beregningsgrunnlagPeriode;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public LocalDate getBeregningsperiodeFom() {
        return beregningsperiode != null ? beregningsperiode.getFomDato() : null;
    }

    public LocalDate getBeregningsperiodeTom() {
        return beregningsperiode != null ? beregningsperiode.getTomDato() : null;
    }

    public Optional<Boolean> mottarYtelse() {
        if (beregningsgrunnlagFrilansAndel != null) {
            return Optional.ofNullable(beregningsgrunnlagFrilansAndel.getMottarYtelse());
        }
        if (beregningsgrunnlagArbeidstakerAndel != null) {
            return Optional.ofNullable(beregningsgrunnlagArbeidstakerAndel.getMottarYtelse());
        }
        return Optional.empty();
    }

    public Optional<Boolean> erNyoppstartet() {
        if (beregningsgrunnlagFrilansAndel != null) {
            return Optional.ofNullable(beregningsgrunnlagFrilansAndel.getNyoppstartet());
        }
        return Optional.empty();
    }

    public boolean gjelderSammeArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel that) {
        if (!Objects.equals(this.getAktivitetStatus(), AktivitetStatus.ARBEIDSTAKER) || !Objects.equals(that.getAktivitetStatus(), AktivitetStatus.ARBEIDSTAKER)) {
            return false;
        }
        return gjelderSammeArbeidsforhold(that.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver),
            that.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef).orElse(InternArbeidsforholdRef.nullRef()));
    }

    public boolean gjelderSammeArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return gjelderSammeArbeidsforhold(Optional.ofNullable(arbeidsgiver), arbeidsforholdRef);
    }

    private boolean gjelderSammeArbeidsforhold(Optional<Arbeidsgiver> arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        var bgAndelArbeidsforholdOpt = getBgAndelArbeidsforhold();
        if (!Objects.equals(getAktivitetStatus(), AktivitetStatus.ARBEIDSTAKER) || bgAndelArbeidsforholdOpt.isEmpty()) {
            return false;
        }
        return Objects.equals(this.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver), arbeidsgiver)
                && bgAndelArbeidsforholdOpt.get().getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public BigDecimal getBruttoPrÅr() {
        return bruttoPrÅr;
    }

    public BigDecimal getOverstyrtPrÅr() {
        return overstyrtPrÅr;
    }

    public BigDecimal getAvkortetPrÅr() {
        return avkortetPrÅr;
    }

    public BigDecimal getRedusertPrÅr() {
        return redusertPrÅr;
    }

    public BigDecimal getBeregnetPrÅr() {
        return beregnetPrÅr;
    }

    public BigDecimal getFordeltPrÅr() {
        return fordeltPrÅr;
    }

    public BigDecimal getManueltFordeltPrÅr() {
        return manueltFordeltPrÅr;
    }

    public BigDecimal getMaksimalRefusjonPrÅr() {
        return maksimalRefusjonPrÅr;
    }

    public BigDecimal getAvkortetRefusjonPrÅr() {
        return avkortetRefusjonPrÅr;
    }

    public BigDecimal getRedusertRefusjonPrÅr() {
        return redusertRefusjonPrÅr;
    }

    public BigDecimal getAvkortetBrukersAndelPrÅr() {
        return avkortetBrukersAndelPrÅr;
    }

    public BigDecimal getRedusertBrukersAndelPrÅr() {
        return redusertBrukersAndelPrÅr;
    }

    public Boolean getNyIArbeidslivet() {
        return nyIArbeidslivet;
    }

    public Boolean getFastsattAvSaksbehandler() {
        return fastsattAvSaksbehandler == null ? Boolean.FALSE : fastsattAvSaksbehandler;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Inntektskategori getInntektskategoriManuellFordeling() {
        return inntektskategoriManuellFordeling;
    }

    public Inntektskategori getInntektskategoriAutomatiskFordeling() {
        return inntektskategoriAutomatiskFordeling;
    }

    // Rekkefølge er viktig
    public Inntektskategori getGjeldendeInntektskategori() {
        if (inntektskategoriManuellFordeling != null) {
            return inntektskategoriManuellFordeling;
        } else if (inntektskategoriAutomatiskFordeling != null) {
            return inntektskategoriAutomatiskFordeling;
        }
        return inntektskategori;
    }

    public AndelKilde getKilde() {
        return kilde;
    }

    public BigDecimal getBruttoInkludertNaturalYtelser() {
        var naturalytelseBortfalt = getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr).orElse(BigDecimal.ZERO);
        var naturalYtelseTilkommet = getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseTilkommetPrÅr).orElse(BigDecimal.ZERO);
        var brutto = bruttoPrÅr != null ? bruttoPrÅr : BigDecimal.ZERO;
        return brutto.add(naturalytelseBortfalt).subtract(naturalYtelseTilkommet);
    }

    public Long getDagsatsBruker() {
        return dagsatsBruker;
    }

    public Long getDagsatsArbeidsgiver() {
        return dagsatsArbeidsgiver;
    }

    public Long getDagsats() {
        if (dagsatsBruker == null) {
            return dagsatsArbeidsgiver;
        }
        if (dagsatsArbeidsgiver == null) {
            return dagsatsBruker;
        }
        return dagsatsBruker + dagsatsArbeidsgiver;
    }

    public BigDecimal getPgiSnitt() {
        return pgiSnitt;
    }

    public BigDecimal getPgi1() {
        return pgi1;
    }

    public BigDecimal getPgi2() {
        return pgi2;
    }

    public BigDecimal getPgi3() {
        return pgi3;
    }

    public Beløp getÅrsbeløpFraTilstøtendeYtelse() {
        return årsbeløpFraTilstøtendeYtelse;
    }

    public BigDecimal getÅrsbeløpFraTilstøtendeYtelseVerdi() {
        return Optional.ofNullable(getÅrsbeløpFraTilstøtendeYtelse())
                .map(Beløp::getVerdi).orElse(BigDecimal.ZERO);
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public BigDecimal getBesteberegningPrÅr() {
        return besteberegningPrÅr;
    }

    public Boolean erLagtTilAvSaksbehandler() {
        return AndelKilde.SAKSBEHANDLER_FORDELING.equals(kilde) || AndelKilde.SAKSBEHANDLER_KOFAKBER.equals(kilde);
    }

    public Optional<BGAndelArbeidsforhold> getBgAndelArbeidsforhold() {
        return Optional.ofNullable(bgAndelArbeidsforhold);
    }

    public Long getOrginalDagsatsFraTilstøtendeYtelse() {
        return orginalDagsatsFraTilstøtendeYtelse;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        var beregningArbeidsforhold = getBgAndelArbeidsforhold();
        return beregningArbeidsforhold.map(BGAndelArbeidsforhold::getArbeidsgiver);
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        var beregningArbeidsforhold = getBgAndelArbeidsforhold();
        return beregningArbeidsforhold.map(BGAndelArbeidsforhold::getArbeidsforholdRef);
    }

    public Optional<BeregningsgrunnlagFrilansAndel> getBeregningsgrunnlagFrilansAndel() {
        return Optional.ofNullable(beregningsgrunnlagFrilansAndel);
    }

    public Optional<BeregningsgrunnlagArbeidstakerAndel> getBeregningsgrunnlagArbeidstakerAndel() {
        return Optional.ofNullable(beregningsgrunnlagArbeidstakerAndel);
    }

    void setBgAndelArbeidsforhold(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        bgAndelArbeidsforhold.setBeregningsgrunnlagPrStatusOgAndel(this);
        this.bgAndelArbeidsforhold = bgAndelArbeidsforhold;
    }

    void setBeregningsgrunnlagFrilansAndel(BeregningsgrunnlagFrilansAndel beregningsgrunnlagFrilansAndel) {
        beregningsgrunnlagFrilansAndel.setBeregningsgrunnlagPrStatusOgAndel(this);
        this.beregningsgrunnlagFrilansAndel = beregningsgrunnlagFrilansAndel;
    }

    void setBeregningsgrunnlagArbeidstakerAndel(BeregningsgrunnlagArbeidstakerAndel beregningsgrunnlagArbeidstakerAndel) {
        beregningsgrunnlagArbeidstakerAndel.setBeregningsgrunnlagPrStatusOgAndel(this);
        this.beregningsgrunnlagArbeidstakerAndel = beregningsgrunnlagArbeidstakerAndel;
    }

    void setBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        this.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsgrunnlagPrStatusOgAndel other)) {
            return false;
        }
        // Endring av denne har store konsekvenser for matching av andeler
        // Resultat av endringer må testes manuelt
        return Objects.equals(this.getAktivitetStatus(), other.getAktivitetStatus())
                && Objects.equals(this.getInntektskategori(), other.getInntektskategori())
                && Objects.equals(this.getInntektskategoriAutomatiskFordeling(), other.getInntektskategoriAutomatiskFordeling())
                && Objects.equals(this.getInntektskategoriManuellFordeling(), other.getInntektskategoriManuellFordeling())
                && Objects.equals(this.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver),
                    other.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
                && Objects.equals(this.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef),
                    other.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef))
                && Objects.equals(this.getArbeidsforholdType(), other.getArbeidsforholdType());
    }


    @Override
    public int hashCode() {
        return Objects.hash(aktivitetStatus,
            inntektskategori,
            getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver),
            getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef),
            arbeidsforholdType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
                "id=" + id + ", "
                + "beregningsgrunnlagPeriode=" + beregningsgrunnlagPeriode + ", "
                + "aktivitetStatus=" + aktivitetStatus + ", "
                + "beregningsperiode=" + beregningsperiode + ", "
                + "arbeidsforholdType=" + arbeidsforholdType + ", "
                + "maksimalRefusjonPrÅr=" + maksimalRefusjonPrÅr + ", "
                + "avkortetRefusjonPrÅr=" + avkortetRefusjonPrÅr + ", "
                + "redusertRefusjonPrÅr=" + redusertRefusjonPrÅr + ", "
                + "avkortetBrukersAndelPrÅr=" + avkortetBrukersAndelPrÅr + ", "
                + "redusertBrukersAndelPrÅr=" + redusertBrukersAndelPrÅr + ", "
                + "beregnetPrÅr=" + beregnetPrÅr + ", "
                + "fordeltPrÅr=" + fordeltPrÅr + ", "
                + "overstyrtPrÅr=" + overstyrtPrÅr + ", "
                + "bruttoPrÅr=" + bruttoPrÅr + ", "
                + "avkortetPrÅr=" + avkortetPrÅr + ", "
                + "redusertPrÅr=" + redusertPrÅr + ", "
                + "dagsatsBruker=" + dagsatsBruker + ", "
                + "dagsatsArbeidsgiver=" + dagsatsArbeidsgiver + ", "
                + "pgiSnitt=" + pgiSnitt + ", "
                + "pgi1=" + pgi1 + ", "
                + "pgi2=" + pgi2 + ", "
                + "pgi3=" + pgi3 + ", "
                + "årsbeløpFraTilstøtendeYtelse=" + årsbeløpFraTilstøtendeYtelse
                + "besteberegningPrÅr=" + besteberegningPrÅr
                + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsgrunnlagPrStatusOgAndel eksisterendeBGPrStatusOgAndel) {
        if (eksisterendeBGPrStatusOgAndel.getId() != null) {
            throw new IllegalArgumentException("Utviklerfeil: Kan ikke bygge på en allerede lagret andel.");
        }
        return new Builder(eksisterendeBGPrStatusOgAndel);
    }
    public static class Builder {

        /** Når det er built kan ikke denne builderen brukes til annet enn å returnere samme objekt. */
        private boolean built;

        private BeregningsgrunnlagPrStatusOgAndel kladd;

        private Builder() {
            kladd = new BeregningsgrunnlagPrStatusOgAndel();
            kladd.arbeidsforholdType = OpptjeningAktivitetType.UDEFINERT;
        }

        private Builder(BeregningsgrunnlagPrStatusOgAndel eksisterendeBGPrStatusOgAndelMal) {
            kladd = eksisterendeBGPrStatusOgAndelMal;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            verifiserKanModifisere();
            kladd.aktivitetStatus = Objects.requireNonNull(aktivitetStatus, "aktivitetStatus");
            if (OpptjeningAktivitetType.UDEFINERT.equals(kladd.arbeidsforholdType)) {
                if (AktivitetStatus.ARBEIDSTAKER.equals(aktivitetStatus)) {
                    kladd.arbeidsforholdType = OpptjeningAktivitetType.ARBEID;
                } else if (AktivitetStatus.FRILANSER.equals(aktivitetStatus)) {
                    kladd.arbeidsforholdType = OpptjeningAktivitetType.FRILANS;
                }
            }
            return this;
        }

        public Builder medBeregningsperiode(LocalDate beregningsperiodeFom, LocalDate beregningsperiodeTom) {
            verifiserKanModifisere();
            kladd.beregningsperiode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningsperiodeFom, beregningsperiodeTom);
            return this;
        }

        public Builder medArbforholdType(OpptjeningAktivitetType arbforholdType) {
            verifiserKanModifisere();
            kladd.arbeidsforholdType = arbforholdType;
            return this;
        }

        public Builder medOverstyrtPrÅr(BigDecimal overstyrtPrÅr) {
            verifiserKanModifisere();
            kladd.overstyrtPrÅr = overstyrtPrÅr;
            if (overstyrtPrÅr != null && kladd.besteberegningPrÅr == null && kladd.fordeltPrÅr == null) {
                kladd.bruttoPrÅr = overstyrtPrÅr;
                if (kladd.getBeregningsgrunnlagPeriode() != null) {
                    kladd.beregningsgrunnlagPeriode.updateBruttoPrÅr();
                }
            }
            return this;
        }

        public Builder medFordeltPrÅr(BigDecimal fordeltPrÅr) {
            verifiserKanModifisere();
            kladd.fordeltPrÅr = fordeltPrÅr;
            if (fordeltPrÅr != null) {
                kladd.bruttoPrÅr = fordeltPrÅr;
                if (kladd.getBeregningsgrunnlagPeriode() != null) {
                    kladd.beregningsgrunnlagPeriode.updateBruttoPrÅr();
                }
            }
            return this;
        }

        public Builder medManueltFordeltPrÅr(BigDecimal manueltFordeltPrÅr) {
            verifiserKanModifisere();
            kladd.manueltFordeltPrÅr = manueltFordeltPrÅr;
            if (manueltFordeltPrÅr != null) {
                kladd.bruttoPrÅr = manueltFordeltPrÅr;
                if (kladd.getBeregningsgrunnlagPeriode() != null) {
                    kladd.beregningsgrunnlagPeriode.updateBruttoPrÅr();
                }
            }
            return this;
        }

        public Builder medAvkortetPrÅr(BigDecimal avkortetPrÅr) {
            verifiserKanModifisere();
            kladd.avkortetPrÅr = avkortetPrÅr;
            return this;
        }

        public Builder medRedusertPrÅr(BigDecimal redusertPrÅr) {
            verifiserKanModifisere();
            kladd.redusertPrÅr = redusertPrÅr;
            return this;
        }

        public Builder medMaksimalRefusjonPrÅr(BigDecimal maksimalRefusjonPrÅr) {
            verifiserKanModifisere();
            kladd.maksimalRefusjonPrÅr = maksimalRefusjonPrÅr;
            return this;
        }

        public Builder medAvkortetRefusjonPrÅr(BigDecimal avkortetRefusjonPrÅr) {
            verifiserKanModifisere();
            kladd.avkortetRefusjonPrÅr = avkortetRefusjonPrÅr;
            return this;
        }

        public Builder medRedusertRefusjonPrÅr(BigDecimal redusertRefusjonPrÅr) {
            verifiserKanModifisere();
            kladd.redusertRefusjonPrÅr = redusertRefusjonPrÅr;
            kladd.dagsatsArbeidsgiver = redusertRefusjonPrÅr == null ?
                null : redusertRefusjonPrÅr.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
            return this;
        }

        public Builder medAvkortetBrukersAndelPrÅr(BigDecimal avkortetBrukersAndelPrÅr) {
            verifiserKanModifisere();
            kladd.avkortetBrukersAndelPrÅr = avkortetBrukersAndelPrÅr;
            return this;
        }

        public Builder medRedusertBrukersAndelPrÅr(BigDecimal redusertBrukersAndelPrÅr) {
            verifiserKanModifisere();
            kladd.redusertBrukersAndelPrÅr = redusertBrukersAndelPrÅr;
            kladd.dagsatsBruker = redusertBrukersAndelPrÅr == null ?
                null : redusertBrukersAndelPrÅr.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
            return this;
        }

        public Builder medBeregnetPrÅr(BigDecimal beregnetPrÅr) {
            verifiserKanModifisere();
            kladd.beregnetPrÅr = beregnetPrÅr;
            if (kladd.fordeltPrÅr == null && kladd.overstyrtPrÅr == null) {
                kladd.bruttoPrÅr = beregnetPrÅr;
                if (kladd.getBeregningsgrunnlagPeriode() != null) {
                    kladd.beregningsgrunnlagPeriode.updateBruttoPrÅr();
                }
            }
            return this;
        }

        public Builder medPgi(BigDecimal pgiSnitt, List<BigDecimal> pgiListe) {
            verifiserKanModifisere();
            kladd.pgiSnitt = pgiSnitt;
            kladd.pgi1 = pgiListe.isEmpty() ? null : pgiListe.get(0);
            kladd.pgi2 = pgiListe.isEmpty() ? null : pgiListe.get(1);
            kladd.pgi3 = pgiListe.isEmpty() ? null : pgiListe.get(2);
            return this;
        }

        public Builder medÅrsbeløpFraTilstøtendeYtelse(BigDecimal årsbeløpFraTilstøtendeYtelse) {
            verifiserKanModifisere();
            kladd.årsbeløpFraTilstøtendeYtelse = new Beløp(årsbeløpFraTilstøtendeYtelse);
            return this;
        }

        public Builder medNyIArbeidslivet(Boolean nyIArbeidslivet) {
            verifiserKanModifisere();
            kladd.nyIArbeidslivet = nyIArbeidslivet;
            return this;
        }

        public Builder medMottarYtelse(Boolean mottarYtelse, AktivitetStatus aktivitetStatus) {
            verifiserKanModifisere();
            kladd.aktivitetStatus = aktivitetStatus;
            if (kladd.aktivitetStatus.erFrilanser()) {
                if (kladd.beregningsgrunnlagFrilansAndel == null) {
                    kladd.beregningsgrunnlagFrilansAndel = BeregningsgrunnlagFrilansAndel.builder()
                            .medMottarYtelse(mottarYtelse)
                            .build(kladd);
                } else {
                    BeregningsgrunnlagFrilansAndel.builder(kladd.beregningsgrunnlagFrilansAndel)
                    .medMottarYtelse(mottarYtelse);
                }
            } else if (kladd.aktivitetStatus.erArbeidstaker()) {
                if (kladd.beregningsgrunnlagArbeidstakerAndel == null) {
                    kladd.beregningsgrunnlagArbeidstakerAndel = BeregningsgrunnlagArbeidstakerAndel.builder()
                            .medMottarYtelse(mottarYtelse)
                            .build(kladd);
                } else {
                    BeregningsgrunnlagArbeidstakerAndel.builder(kladd.beregningsgrunnlagArbeidstakerAndel)
                    .medMottarYtelse(mottarYtelse);
                }
            }
            return this;
        }

        public Builder medNyoppstartet(Boolean nyoppstartet, AktivitetStatus aktivitetStatus) {
            verifiserKanModifisere();
            kladd.aktivitetStatus = aktivitetStatus;
            if (kladd.aktivitetStatus.erFrilanser()) {
                if (kladd.beregningsgrunnlagFrilansAndel == null) {
                    kladd.beregningsgrunnlagFrilansAndel = BeregningsgrunnlagFrilansAndel.builder()
                            .medNyoppstartet(nyoppstartet)
                            .build(kladd);
                } else {
                    BeregningsgrunnlagFrilansAndel.builder(kladd.beregningsgrunnlagFrilansAndel)
                    .medNyoppstartet(nyoppstartet);
                }
            } else {
                throw new IllegalArgumentException("Andel må vere frilans for å sette nyoppstartet");
            }
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            verifiserKanModifisere();
            kladd.inntektskategori = inntektskategori;
            return this;
        }

        public Builder medInntektskategoriManuellFordeling(Inntektskategori inntektskategoriManuellFordeling) {
            verifiserKanModifisere();
            kladd.inntektskategoriManuellFordeling = inntektskategoriManuellFordeling;
            return this;
        }

        public Builder medInntektskategoriAutomatiskFordeling(Inntektskategori inntektskategoriAutomatiskFordeling) {
            verifiserKanModifisere();
            kladd.inntektskategoriAutomatiskFordeling = inntektskategoriAutomatiskFordeling;
            return this;
        }

        public Builder medFastsattAvSaksbehandler(Boolean fastsattAvSaksbehandler) {
            verifiserKanModifisere();
            kladd.fastsattAvSaksbehandler = fastsattAvSaksbehandler;
            return this;
        }

        public Builder medBesteberegningPrÅr(BigDecimal besteberegningPrÅr) {
            verifiserKanModifisere();
            kladd.besteberegningPrÅr = besteberegningPrÅr;
            if (besteberegningPrÅr != null && kladd.fordeltPrÅr == null) {
                kladd.bruttoPrÅr = besteberegningPrÅr;
                if (kladd.getBeregningsgrunnlagPeriode() != null) {
                    kladd.beregningsgrunnlagPeriode.updateBruttoPrÅr();
                }
            }
            return this;
        }

        public Builder medAndelsnr(Long andelsnr) {
            verifiserKanModifisere();
            kladd.andelsnr = andelsnr;
            return this;
        }

        public Builder medKilde(AndelKilde kilde) {
            verifiserKanModifisere();
            kladd.kilde = kilde;
            return this;
        }

        public Builder medBGAndelArbeidsforhold(BGAndelArbeidsforhold.Builder bgAndelArbeidsforholdBuilder) {
            verifiserKanModifisere();
            kladd.bgAndelArbeidsforhold = bgAndelArbeidsforholdBuilder.build(kladd);
            return this;
        }

        public Builder medOrginalDagsatsFraTilstøtendeYtelse(Long orginalDagsatsFraTilstøtendeYtelse) {
            verifiserKanModifisere();
            kladd.orginalDagsatsFraTilstøtendeYtelse = orginalDagsatsFraTilstøtendeYtelse;
            return this;
        }

        public BeregningsgrunnlagPrStatusOgAndel build(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            if(built) {
                return kladd;
            }
            verifyStateForBuild();
            if (kladd.andelsnr == null) {
                finnOgSettAndelsnr(beregningsgrunnlagPeriode);
            }
            // TODO (EspenVelsvik): Ikke mod input!
            beregningsgrunnlagPeriode.addBeregningsgrunnlagPrStatusOgAndel(kladd);
            beregningsgrunnlagPeriode.updateBruttoPrÅr();
            verifiserAndelsnr();
            built = true;
            return kladd;
        }

        private void finnOgSettAndelsnr(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            verifiserKanModifisere();
            Long forrigeAndelsnr = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                    .mapToLong(BeregningsgrunnlagPrStatusOgAndel::getAndelsnr)
                    .max()
                    .orElse(0L);
            kladd.andelsnr = forrigeAndelsnr + 1L;
        }

        private void verifiserAndelsnr() {
            Set<Long> andelsnrIBruk = new HashSet<>();
            kladd.beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getAndelsnr)
            .forEach(andelsnr -> {
                if (andelsnrIBruk.contains(andelsnr)) {
                    throw new IllegalStateException("Utviklerfeil: Kan ikke bygge andel. Andelsnr eksisterer allerede på en annen andel i samme bgPeriode.");
                }
                andelsnrIBruk.add(andelsnr);
            });
        }

        private void verifiserKanModifisere() {
            if(built) {
                throw new IllegalStateException("Er allerede bygd, kan ikke oppdatere videre: " + this.kladd);
            }
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(kladd.aktivitetStatus, "aktivitetStatus");
            if (kladd.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)
                && kladd.getArbeidsforholdType().equals(OpptjeningAktivitetType.ARBEID)) {
                Objects.requireNonNull(kladd.bgAndelArbeidsforhold, "bgAndelArbeidsforhold");
            }
        }
    }
}
