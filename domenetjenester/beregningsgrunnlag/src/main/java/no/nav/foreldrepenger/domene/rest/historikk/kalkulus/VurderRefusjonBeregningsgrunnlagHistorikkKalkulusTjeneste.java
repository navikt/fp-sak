package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.DatoEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonoverstyringEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonoverstyringPeriodeEndring;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonAndelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste {
    private static final BigDecimal MÅNEDER_I_ÅR = BigDecimal.valueOf(12);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                     HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                                     InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(VurderRefusjonBeregningsgrunnlagDto dto,
                             AksjonspunktOppdaterParameter param,
                             OppdaterBeregningsgrunnlagResultat endringsaggregat) {
        var behandlingId = param.getBehandlingId();
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getArbeidsforholdOverstyringer();
        var tekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        for (var fastsattAndel : dto.getFastsatteAndeler()) {
            var forrigeOverstyringer = endringsaggregat.getRefusjonoverstyringEndring().map(RefusjonoverstyringEndring::getRefusjonperiodeEndringer)
                .orElse(List.of());
            var forrigeFastsattAndel = forrigeOverstyringer.stream()
                .filter(os -> matcherAG(os.getArbeidsgiver(), fastsattAndel))
                .filter(os -> matcherReferanse(os.getArbeidsforholdRef(), fastsattAndel))
                .findFirst();
            var forrigeRefusjonsstart = finnForrigeRefusjonsstartForArbeidsforhold(forrigeFastsattAndel);
            Optional<BigDecimal> forrigeDelvisRefusjonPrÅr = finnForrigeDelvisRefusjon(forrigeFastsattAndel);
            leggTilArbeidsforholdHistorikkinnslag(tekstBuilder, fastsattAndel, forrigeRefusjonsstart,
                forrigeDelvisRefusjonPrÅr, arbeidsforholdOverstyringer);
        }

        lagHistorikkInnslag(dto, param, tekstBuilder);
    }

    private Optional<BigDecimal> finnForrigeDelvisRefusjon(Optional<RefusjonoverstyringPeriodeEndring> forrigeMatchendeAndel) {
        // Hvis saksbehandletRefusjonPrÅr var > 0 i denne andelen som ligger i en periode før forrige startdato for refusjon
        // betyr det at det var tidligere innvilget delvis refusjon
        var forrigeSaksbehandletRefusjonPrÅr = forrigeMatchendeAndel
            .map(RefusjonoverstyringPeriodeEndring::getFastsattDelvisRefusjonFørDatoEndring)
            .flatMap(BeløpEndring::getFraBeløp);
        if (forrigeSaksbehandletRefusjonPrÅr.isPresent()
            && forrigeSaksbehandletRefusjonPrÅr.get().compareTo(BigDecimal.ZERO) > 0) {
            return forrigeSaksbehandletRefusjonPrÅr;
        }
        return Optional.empty();
    }

    private Optional<LocalDate> finnForrigeRefusjonsstartForArbeidsforhold(Optional<RefusjonoverstyringPeriodeEndring> refusjonoverstyringEndring) {
        var refusjonsendringHosSammeArbeidsforhold =
            refusjonoverstyringEndring.map(RefusjonoverstyringPeriodeEndring::getFastsattRefusjonFomEndring);
        return refusjonsendringHosSammeArbeidsforhold.map(DatoEndring::getFraVerdi);
    }

    private boolean matcherReferanse(InternArbeidsforholdRef arbeidsforholdRef,
                                     VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
        return Objects.equals(arbeidsforholdRef.getReferanse(), fastsattAndel.getInternArbeidsforholdRef());
    }

    private boolean matcherAG(Arbeidsgiver arbeidsgiver, VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
        if (fastsattAndel.getArbeidsgiverOrgnr() != null) {
            return fastsattAndel.getArbeidsgiverOrgnr().equals(arbeidsgiver.getIdentifikator());
        }
        return Objects.equals(fastsattAndel.getArbeidsgiverAktoerId(), arbeidsgiver.getIdentifikator());
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
        Optional<InternArbeidsforholdRef> ref =
            fastsattAndel.getInternArbeidsforholdRef() == null ? Optional.empty() : Optional.of(
                InternArbeidsforholdRef.ref(fastsattAndel.getInternArbeidsforholdRef()));
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            AktivitetStatus.ARBEIDSTAKER, Optional.of(ag), ref, arbeidsforholdOverstyringer);
        var fraStartdato = forrigeRefusjonsstart.orElse(null);
        var tilStartdato = fastsattAndel.getFastsattRefusjonFom();
        historikkBuilder.medEndretFelt(HistorikkEndretFeltType.NY_STARTDATO_REFUSJON, arbeidsforholdInfo, fraStartdato,
            tilStartdato);
        if (fastsattAndel.getDelvisRefusjonPrMndFørStart() != null
            && fastsattAndel.getDelvisRefusjonPrMndFørStart() != 0) {
            var fraBeløpPrMnd = forrigeDelvisRefusjonPrÅr.map(
                    forrigeDelvisRef -> forrigeDelvisRef.divide(MÅNEDER_I_ÅR, RoundingMode.HALF_EVEN))
                .map(BigDecimal::intValue)
                .orElse(null);
            var tilBeløpPrMnd = fastsattAndel.getDelvisRefusjonPrMndFørStart();
            historikkBuilder.medEndretFelt(HistorikkEndretFeltType.DELVIS_REFUSJON_FØR_STARTDATO, arbeidsforholdInfo,
                fraBeløpPrMnd, tilBeløpPrMnd);
        }
        if (!historikkBuilder.erSkjermlenkeSatt()) {
            historikkBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
        }
        historikkBuilder.ferdigstillHistorikkinnslagDel();
    }

    private void lagHistorikkInnslag(VurderRefusjonBeregningsgrunnlagDto dto,
                                     AksjonspunktOppdaterParameter param,
                                     HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder.ferdigstillHistorikkinnslagDel();
        var historikkDeler = tekstBuilder.getHistorikkinnslagDeler();
        settBegrunnelse(param, historikkDeler, tekstBuilder, dto.getBegrunnelse());
        settSkjermlenkeOmIkkeSatt(historikkDeler, tekstBuilder);
    }

    private void settBegrunnelse(AksjonspunktOppdaterParameter param,
                                 List<HistorikkinnslagDel> historikkDeler,
                                 HistorikkInnslagTekstBuilder tekstBuilder,
                                 String begrunnelse) {
        var erBegrunnelseSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt) {
            var erBegrunnelseEndret = param.erBegrunnelseEndret();
            if (erBegrunnelseEndret) {
                var erSkjermlenkeSatt = historikkDeler.stream()
                    .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
                tekstBuilder.medBegrunnelse(begrunnelse, true);
                if (!erSkjermlenkeSatt) {
                    tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
                }
                tekstBuilder.ferdigstillHistorikkinnslagDel();
            }
        }
    }

    private void settSkjermlenkeOmIkkeSatt(List<HistorikkinnslagDel> historikkDeler,
                                           HistorikkInnslagTekstBuilder tekstBuilder) {
        var erSkjermlenkeSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt && !historikkDeler.isEmpty()) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
        }
    }
}
