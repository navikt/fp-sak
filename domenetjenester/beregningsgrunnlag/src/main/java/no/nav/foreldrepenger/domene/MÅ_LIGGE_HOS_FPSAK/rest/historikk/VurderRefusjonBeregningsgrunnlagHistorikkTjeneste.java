package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;

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
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagHistorikkTjeneste {

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
        for (VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel : dto.getFastsatteAndeler()) {
            Optional<BeregningRefusjonPeriodeEntitet> forrigeOverstyring = finnForrigeOverstyringForArbeidsforhold(fastsattAndel, forrigeOverstyringer);
            leggTilArbeidsforholdHistorikkinnslag(tekstBuilder, fastsattAndel, forrigeOverstyring, arbeidsforholdOverstyringer);
        }

        lagHistorikkInnslag(dto, param, tekstBuilder);

        return OppdateringResultat.utenOveropp();
    }

    private Optional<BeregningRefusjonPeriodeEntitet> finnForrigeOverstyringForArbeidsforhold(VurderRefusjonAndelBeregningsgrunnlagDto fastsattAndel, List<BeregningRefusjonOverstyringEntitet> forrigeOverstyringer) {
        List<BeregningRefusjonPeriodeEntitet> refusjonsperioderHosSammeAG = forrigeOverstyringer.stream()
            .filter(os -> matcherAG(os.getArbeidsgiver(), fastsattAndel))
            .findFirst()
            .map(BeregningRefusjonOverstyringEntitet::getRefusjonPerioder)
            .orElse(Collections.emptyList());
        return refusjonsperioderHosSammeAG.stream().filter(rp -> matcherReferanse(rp.getArbeidsforholdRef(), fastsattAndel.getInternArbeidsforholdRef())).findFirst();
    }

    private boolean matcherReferanse(InternArbeidsforholdRef arbeidsforholdRef, String internArbeidsforholdRef) {
        return Objects.equals(arbeidsforholdRef.getReferanse(), internArbeidsforholdRef);
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
                                                       Optional<BeregningRefusjonPeriodeEntitet> forrigeOverstyring,
                                                       List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        Arbeidsgiver ag;
        if (fastsattAndel.getArbeidsgiverAktoerId() != null) {
            ag = Arbeidsgiver.person(new AktørId(fastsattAndel.getArbeidsgiverAktoerId()));
        } else {
            ag = Arbeidsgiver.virksomhet(fastsattAndel.getArbeidsgiverOrgnr());
        }
        Optional<InternArbeidsforholdRef> ref = fastsattAndel.getInternArbeidsforholdRef() == null ? Optional.empty() : Optional.of(InternArbeidsforholdRef.ref(fastsattAndel.getInternArbeidsforholdRef()));
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER, Optional.of(ag), ref, arbeidsforholdOverstyringer);
        HistorikkEndretFeltType endretFeltType = HistorikkEndretFeltType.NY_STARTDATO_REFUSJON;
        LocalDate fraVerdi = forrigeOverstyring.map(BeregningRefusjonPeriodeEntitet::getStartdatoRefusjon).orElse(null);
        LocalDate tilVerdi = fastsattAndel.getFastsattRefusjonFom();
        historikkBuilder.medEndretFelt(endretFeltType, arbeidsforholdInfo, fraVerdi, tilVerdi);
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
