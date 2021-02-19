package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderRefusjonAndelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagHistorikkTjeneste {
    private static final BigDecimal MÅNEDER_I_ÅR = BigDecimal.valueOf(12);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    VurderRefusjonBeregningsgrunnlagHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                             HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public OppdateringResultat lagHistorikk(VurderRefusjonBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        Long behandlingId = param.getBehandlingId();
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        HistorikkInnslagTekstBuilder tekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        List<BeregningRefusjonOverstyringEntitet> forrigeOverstyringer = forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getRefusjonOverstyringer)
            .map(BeregningRefusjonOverstyringerEntitet::getRefusjonOverstyringer)
            .orElse(Collections.emptyList());
        Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        for (VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel : dto.getFastsatteAndeler()) {
            Optional<LocalDate> forrigeRefusjonsstart = finnForrigeRefusjonsstartForArbeidsforhold(fastsattAndel, forrigeOverstyringer);
            Optional<BigDecimal> forrigeDelvisRefusjonPrÅr = forrigeRefusjonsstart.isEmpty()
                ? Optional.empty()
                : finnForrigeDelvisRefusjon(fastsattAndel, forrigeRefusjonsstart.get(), forrigeBeregningsgrunnlag);
            leggTilArbeidsforholdHistorikkinnslag(tekstBuilder, fastsattAndel, forrigeRefusjonsstart, forrigeDelvisRefusjonPrÅr, arbeidsforholdOverstyringer);
        }

        lagHistorikkInnslag(dto, param, tekstBuilder);

        return OppdateringResultat.utenOveropp();
    }

    private Optional<BigDecimal> finnForrigeDelvisRefusjon(VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel,
                                                           LocalDate forrigeRefusjonsstart,
                                                           Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag) {
        List<BeregningsgrunnlagPeriode> forrigeBGPerioder = forrigeBeregningsgrunnlag
            .map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList());
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIForrugeGrunnlagFørRefusjonstart = forrigeBGPerioder.stream()
            .filter(bgp -> bgp.getBeregningsgrunnlagPeriodeFom().isBefore(forrigeRefusjonsstart))
            .findFirst()
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .orElse(Collections.emptyList());
        Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeMatchendeAndel = andelerIForrugeGrunnlagFørRefusjonstart.stream()
            .filter(andel -> andel.getArbeidsgiver().isPresent() && matcherAG(andel.getArbeidsgiver().get(), fastsattAndel)
            && matcherReferanse(andel.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()), fastsattAndel))
            .findFirst();

        // Hvis saksbehandletRefusjonPrÅr var > 0 i denne andelen som ligger i en periode før forrige startdato for refusjon
        // betyr det at det var tidligere innvilget delvis refusjon
        Optional<BigDecimal> forrigeSaksbehandletRefusjonPrÅr = forrigeMatchendeAndel
            .flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .map(BGAndelArbeidsforhold::getSaksbehandletRefusjonPrÅr);
        if (forrigeSaksbehandletRefusjonPrÅr.isPresent() && forrigeSaksbehandletRefusjonPrÅr.get().compareTo(BigDecimal.ZERO) > 0) {
            return forrigeSaksbehandletRefusjonPrÅr;
        } else {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> finnForrigeRefusjonsstartForArbeidsforhold(VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel, List<BeregningRefusjonOverstyringEntitet> forrigeOverstyringer) {
        List<BeregningRefusjonPeriodeEntitet> refusjonsperioderHosSammeAG = forrigeOverstyringer.stream()
            .filter(os -> matcherAG(os.getArbeidsgiver(), fastsattAndel))
            .findFirst()
            .map(BeregningRefusjonOverstyringEntitet::getRefusjonPerioder)
            .orElse(Collections.emptyList());
        Optional<BeregningRefusjonPeriodeEntitet> first = refusjonsperioderHosSammeAG.stream().filter(rp -> matcherReferanse(rp.getArbeidsforholdRef(), fastsattAndel)).findFirst();
        return first.map(BeregningRefusjonPeriodeEntitet::getStartdatoRefusjon);
    }

    private boolean matcherReferanse(InternArbeidsforholdRef arbeidsforholdRef, VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
        return Objects.equals(arbeidsforholdRef.getReferanse(), fastsattAndel.getInternArbeidsforholdRef());
    }

    private boolean matcherAG(Arbeidsgiver arbeidsgiver, VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
        if (fastsattAndel.getArbeidsgiverOrgnr() != null) {
            return fastsattAndel.getArbeidsgiverOrgnr().equals(arbeidsgiver.getIdentifikator());
        } else {
            return Objects.equals(fastsattAndel.getArbeidsgiverAktoerId(), arbeidsgiver.getIdentifikator());
        }
    }

    private void leggTilArbeidsforholdHistorikkinnslag(HistorikkInnslagTekstBuilder historikkBuilder,
                                                       VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel,
                                                       Optional<LocalDate> forrigeRefusjonsstart,
                                                       Optional<BigDecimal> forrigeDelvisRefusjonPrÅr,
                                                       List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        Arbeidsgiver ag;
        if (fastsattAndel.getArbeidsgiverAktoerId() != null) {
            ag = Arbeidsgiver.person(new AktørId(fastsattAndel.getArbeidsgiverAktoerId()));
        } else {
            ag = Arbeidsgiver.virksomhet(fastsattAndel.getArbeidsgiverOrgnr());
        }
        Optional<InternArbeidsforholdRef> ref = fastsattAndel.getInternArbeidsforholdRef() == null ? Optional.empty() : Optional.of(InternArbeidsforholdRef.ref(fastsattAndel.getInternArbeidsforholdRef()));
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER, Optional.of(ag), ref, arbeidsforholdOverstyringer);
        LocalDate fraStartdato = forrigeRefusjonsstart.orElse(null);
        LocalDate tilStartdato = fastsattAndel.getFastsattRefusjonFom();
        historikkBuilder.medEndretFelt(HistorikkEndretFeltType.NY_STARTDATO_REFUSJON, arbeidsforholdInfo, fraStartdato, tilStartdato);
        if (fastsattAndel.getDelvisRefusjonPrMndFørStart() != null && fastsattAndel.getDelvisRefusjonPrMndFørStart() != 0) {
            Integer fraBeløpPrMnd = forrigeDelvisRefusjonPrÅr.map(forrigeDelvisRef -> forrigeDelvisRef.divide(MÅNEDER_I_ÅR, RoundingMode.HALF_EVEN))
                .map(BigDecimal::intValue)
                .orElse(null);
            Integer tilBeløpPrMnd = fastsattAndel.getDelvisRefusjonPrMndFørStart();
            historikkBuilder.medEndretFelt(HistorikkEndretFeltType.DELVIS_REFUSJON_FØR_STARTDATO, arbeidsforholdInfo, fraBeløpPrMnd, tilBeløpPrMnd);
        }
        if (!historikkBuilder.erSkjermlenkeSatt()) {
            historikkBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
        }
        historikkBuilder.ferdigstillHistorikkinnslagDel();
    }

    private void lagHistorikkInnslag(VurderRefusjonBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param, HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder.ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> historikkDeler = tekstBuilder.getHistorikkinnslagDeler();
        settBegrunnelse(param, historikkDeler, tekstBuilder, dto.getBegrunnelse());
        settSkjermlenkeOmIkkeSatt(historikkDeler, tekstBuilder);
    }

    private void settBegrunnelse(AksjonspunktOppdaterParameter param, List<HistorikkinnslagDel> historikkDeler, HistorikkInnslagTekstBuilder tekstBuilder, String begrunnelse) {
        boolean erBegrunnelseSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt) {
            boolean erBegrunnelseEndret = param.erBegrunnelseEndret();
            if (erBegrunnelseEndret) {
                boolean erSkjermlenkeSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
                tekstBuilder.medBegrunnelse(begrunnelse, true);
                if (!erSkjermlenkeSatt) {
                    tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
                }
                tekstBuilder.ferdigstillHistorikkinnslagDel();
            }
        }
    }

    private void settSkjermlenkeOmIkkeSatt(List<HistorikkinnslagDel> historikkDeler, HistorikkInnslagTekstBuilder tekstBuilder) {
        boolean erSkjermlenkeSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt && !historikkDeler.isEmpty()) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
        }
    }

}
