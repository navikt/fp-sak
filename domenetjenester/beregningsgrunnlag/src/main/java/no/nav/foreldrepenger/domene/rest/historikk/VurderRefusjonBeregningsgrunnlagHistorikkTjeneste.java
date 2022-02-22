package no.nav.foreldrepenger.domene.rest.historikk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonoverstyringEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonoverstyringPeriodeEndring;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonAndelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
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

    public OppdateringResultat lagHistorikk(VurderRefusjonBeregningsgrunnlagDto dto,
                                            AksjonspunktOppdaterParameter param,
                                            RefusjonoverstyringEndring refusjonoverstyringEndring) {
        var behandlingId = param.getBehandlingId();
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        var tekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        for (var fastsattAndel : dto.getFastsatteAndeler()) {
            var endringForAndel = finnRefusjonEndringForAndel(refusjonoverstyringEndring, fastsattAndel);
            endringForAndel.ifPresent(refusjonoverstyringPeriodeEndring -> leggTilArbeidsforholdHistorikkinnslag(tekstBuilder, fastsattAndel,
                refusjonoverstyringPeriodeEndring.getFastsattRefusjonFomEndring().getFraVerdi(),
                refusjonoverstyringPeriodeEndring.getFastsattDelvisRefusjonFørDatoEndring().getFraBeløp(), arbeidsforholdOverstyringer));
        }

        lagHistorikkInnslag(dto, param, tekstBuilder);

        return OppdateringResultat.utenOveropp();
    }

    private Optional<RefusjonoverstyringPeriodeEndring> finnRefusjonEndringForAndel(RefusjonoverstyringEndring refusjonoverstyringEndring,
                                                                                    VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
        return refusjonoverstyringEndring.getRefusjonperiodeEndringer()
            .stream()
            .filter(refusjonEndring -> matcherAG(refusjonEndring.getArbeidsgiver(), fastsattAndel) && matcherReferanse(
                refusjonEndring.getArbeidsforholdRef(), fastsattAndel))
            .findFirst();
    }

    private boolean matcherReferanse(InternArbeidsforholdRef arbeidsforholdRef, VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel) {
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
        Optional<InternArbeidsforholdRef> ref = fastsattAndel.getInternArbeidsforholdRef() == null ? Optional.empty() : Optional.of(
            InternArbeidsforholdRef.ref(fastsattAndel.getInternArbeidsforholdRef()));
        var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER,
            Optional.of(ag), ref, arbeidsforholdOverstyringer);
        var fraStartdato = forrigeRefusjonsstart.orElse(null);
        var tilStartdato = fastsattAndel.getFastsattRefusjonFom();
        historikkBuilder.medEndretFelt(HistorikkEndretFeltType.NY_STARTDATO_REFUSJON, arbeidsforholdInfo, fraStartdato, tilStartdato);
        if (fastsattAndel.getDelvisRefusjonPrMndFørStart() != null && fastsattAndel.getDelvisRefusjonPrMndFørStart() != 0) {
            var fraBeløpPrMnd = forrigeDelvisRefusjonPrÅr.map(forrigeDelvisRef -> forrigeDelvisRef.divide(MÅNEDER_I_ÅR, RoundingMode.HALF_EVEN))
                .map(BigDecimal::intValue)
                .orElse(null);
            var tilBeløpPrMnd = fastsattAndel.getDelvisRefusjonPrMndFørStart();
            historikkBuilder.medEndretFelt(HistorikkEndretFeltType.DELVIS_REFUSJON_FØR_STARTDATO, arbeidsforholdInfo, fraBeløpPrMnd, tilBeløpPrMnd);
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
        var erBegrunnelseSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt) {
            var erBegrunnelseEndret = param.erBegrunnelseEndret();
            if (erBegrunnelseEndret) {
                var erSkjermlenkeSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
                tekstBuilder.medBegrunnelse(begrunnelse, true);
                if (!erSkjermlenkeSatt) {
                    tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
                }
                tekstBuilder.ferdigstillHistorikkinnslagDel();
            }
        }
    }

    private void settSkjermlenkeOmIkkeSatt(List<HistorikkinnslagDel> historikkDeler, HistorikkInnslagTekstBuilder tekstBuilder) {
        var erSkjermlenkeSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt && !historikkDeler.isEmpty()) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_FORDELING);
        }
    }

}
