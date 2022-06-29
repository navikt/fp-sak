package no.nav.foreldrepenger.domene.modell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BeregningsgrunnlagPrStatusOgAndel {

    private Long andelsnr;
    private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;
    private AktivitetStatus aktivitetStatus;
    private ÅpenDatoIntervallEntitet beregningsperiode;
    private OpptjeningAktivitetType arbeidsforholdType;
    private BigDecimal bruttoPrÅr;
    private BigDecimal overstyrtPrÅr;
    private BigDecimal avkortetPrÅr;
    private BigDecimal redusertPrÅr;
    private BigDecimal beregnetPrÅr;
    private BigDecimal fordeltPrÅr;
    private BigDecimal maksimalRefusjonPrÅr;
    private BigDecimal avkortetRefusjonPrÅr;
    private BigDecimal redusertRefusjonPrÅr;
    private BigDecimal avkortetBrukersAndelPrÅr;
    private BigDecimal redusertBrukersAndelPrÅr;
    private Long dagsatsBruker;
    private Long dagsatsArbeidsgiver;
    private BigDecimal pgiSnitt;
    private BigDecimal pgi1;
    private BigDecimal pgi2;
    private BigDecimal pgi3;
    private Beløp årsbeløpFraTilstøtendeYtelse;
    private Boolean fastsattAvSaksbehandler = Boolean.FALSE;
    private Inntektskategori inntektskategori = Inntektskategori.UDEFINERT;
    private Boolean lagtTilAvSaksbehandler = Boolean.FALSE;
    private BGAndelArbeidsforhold bgAndelArbeidsforhold;
    private Long orginalDagsatsFraTilstøtendeYtelse;

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

    public boolean gjelderInntektsmeldingFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        Optional<BGAndelArbeidsforhold> bgAndelArbeidsforholdOpt = getBgAndelArbeidsforhold();
        if (!Objects.equals(getAktivitetStatus(), AktivitetStatus.ARBEIDSTAKER) || bgAndelArbeidsforholdOpt.isEmpty()) {
            return false;
        }
        if (!Objects.equals(this.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver), Optional.of(arbeidsgiver))) {
            return false;
        }
        LocalDate stpBeregning = this.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getSkjæringstidspunkt();
        boolean slutterFørStp = this.bgAndelArbeidsforhold.getArbeidsperiodeTom()
                .map(d -> d.isBefore(stpBeregning)).orElse(false);
        if (slutterFørStp) {
            return false;
        }
        return  bgAndelArbeidsforholdOpt.get().getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
    }

    private boolean gjelderSammeArbeidsforhold(Optional<Arbeidsgiver> arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        Optional<BGAndelArbeidsforhold> bgAndelArbeidsforholdOpt = getBgAndelArbeidsforhold();
        if (!Objects.equals(getAktivitetStatus(), AktivitetStatus.ARBEIDSTAKER) || bgAndelArbeidsforholdOpt.isEmpty()) {
            return false;
        }
        return Objects.equals(this.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver), arbeidsgiver)
                && bgAndelArbeidsforholdOpt.get().getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
    }

    public boolean matchUtenInntektskategori(BeregningsgrunnlagPrStatusOgAndel other) {
        return Objects.equals(this.getAktivitetStatus(), other.getAktivitetStatus())
                && Objects.equals(this.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver), other.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
                && Objects.equals(this.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef), other.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef))
                && Objects.equals(this.getArbeidsforholdType(), other.getArbeidsforholdType());
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

    public Boolean getFastsattAvSaksbehandler() {
        return fastsattAvSaksbehandler;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public BigDecimal getBruttoInkludertNaturalYtelser() {
        BigDecimal naturalytelseBortfalt = getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr).orElse(BigDecimal.ZERO);
        BigDecimal naturalYtelseTilkommet = getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseTilkommetPrÅr).orElse(BigDecimal.ZERO);
        BigDecimal brutto = bruttoPrÅr != null ? bruttoPrÅr : BigDecimal.ZERO;
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

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public Optional<BGAndelArbeidsforhold> getBgAndelArbeidsforhold() {
        return Optional.ofNullable(bgAndelArbeidsforhold);
    }

    public Long getOrginalDagsatsFraTilstøtendeYtelse() {
        return orginalDagsatsFraTilstøtendeYtelse;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        Optional<BGAndelArbeidsforhold> beregningArbeidsforhold = getBgAndelArbeidsforhold();
        return beregningArbeidsforhold.map(BGAndelArbeidsforhold::getArbeidsgiver);
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        Optional<BGAndelArbeidsforhold> beregningArbeidsforhold = getBgAndelArbeidsforhold();
        return beregningArbeidsforhold.map(BGAndelArbeidsforhold::getArbeidsforholdRef);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagPrStatusOgAndel)) {
            return false;
        }
        // Endring av denne har store konsekvenser for matching av andeler
        // Resultat av endringer må testes manuelt
        BeregningsgrunnlagPrStatusOgAndel other = (BeregningsgrunnlagPrStatusOgAndel) obj;
        return Objects.equals(this.getAktivitetStatus(), other.getAktivitetStatus())
                && Objects.equals(this.getInntektskategori(), other.getInntektskategori())
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
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
                + "beregningsgrunnlagPeriode=" + beregningsgrunnlagPeriode + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "aktivitetStatus=" + aktivitetStatus + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "beregningsperiode=" + beregningsperiode + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "arbeidsforholdType=" + arbeidsforholdType + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "maksimalRefusjonPrÅr=" + maksimalRefusjonPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "avkortetRefusjonPrÅr=" + avkortetRefusjonPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "redusertRefusjonPrÅr=" + redusertRefusjonPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "avkortetBrukersAndelPrÅr=" + avkortetBrukersAndelPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "redusertBrukersAndelPrÅr=" + redusertBrukersAndelPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "beregnetPrÅr=" + beregnetPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "fordeltPrÅr=" + fordeltPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "overstyrtPrÅr=" + overstyrtPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "bruttoPrÅr=" + bruttoPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "avkortetPrÅr=" + avkortetPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "redusertPrÅr=" + redusertPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "dagsatsBruker=" + dagsatsBruker + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "dagsatsArbeidsgiver=" + dagsatsArbeidsgiver + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "pgiSnitt=" + pgiSnitt + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "pgi1=" + pgi1 + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "pgi2=" + pgi2 + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "pgi3=" + pgi3 + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "årsbeløpFraTilstøtendeYtelse=" + årsbeløpFraTilstøtendeYtelse //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsgrunnlagPrStatusOgAndel eksisterendeBGPrStatusOgAndel) {
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
            if (overstyrtPrÅr != null && kladd.fordeltPrÅr == null) {
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

        public Builder medBruttoPrÅr(BigDecimal bruttoPrÅr) {
            kladd.bruttoPrÅr = bruttoPrÅr;
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

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            verifiserKanModifisere();
            kladd.inntektskategori = inntektskategori;
            return this;
        }

        public Builder medFastsattAvSaksbehandler(Boolean fastsattAvSaksbehandler) {
            verifiserKanModifisere();
            kladd.fastsattAvSaksbehandler = fastsattAvSaksbehandler == null?false: fastsattAvSaksbehandler;
            return this;
        }

        public Builder medAndelsnr(Long andelsnr) {
            verifiserKanModifisere();
            kladd.andelsnr = andelsnr;
            return this;
        }

        public Builder nyttAndelsnr(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            verifiserKanModifisere();
            finnOgSettAndelsnr(beregningsgrunnlagPeriode);
            return this;
        }

        public Builder medLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
            verifiserKanModifisere();
            kladd.lagtTilAvSaksbehandler = Boolean.TRUE.equals(lagtTilAvSaksbehandler);
            return this;
        }

        public Builder medBGAndelArbeidsforhold(BGAndelArbeidsforhold.Builder bgAndelArbeidsforholdBuilder) {
            verifiserKanModifisere();
            kladd.bgAndelArbeidsforhold = bgAndelArbeidsforholdBuilder.build();
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
            kladd.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
            verifyStateForBuild();
            if (kladd.andelsnr == null) {
                // TODO (OleSandbu): Ikke mod input!
                finnOgSettAndelsnr(beregningsgrunnlagPeriode);
            }
            // TODO (OleSandbu): Ikke mod input!
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
            Objects.requireNonNull(kladd.beregningsgrunnlagPeriode, "beregningsgrunnlagPeriode");
            Objects.requireNonNull(kladd.aktivitetStatus, "aktivitetStatus");
            if (kladd.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)
                && kladd.getArbeidsforholdType().equals(OpptjeningAktivitetType.ARBEID)) {
                Objects.requireNonNull(kladd.bgAndelArbeidsforhold, "bgAndelArbeidsforhold");
            }
        }
    }
}
