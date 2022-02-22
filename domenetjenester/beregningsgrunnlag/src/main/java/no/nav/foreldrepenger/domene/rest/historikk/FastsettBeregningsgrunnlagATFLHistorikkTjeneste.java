package no.nav.foreldrepenger.domene.rest.historikk;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FastsettBeregningsgrunnlagATFLHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FastsettBeregningsgrunnlagATFLHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                           ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                           InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             FastsettBeregningsgrunnlagATFLDto dto,
                             BeregningsgrunnlagEndring beregningsgrunnlagEndring) {

        var andelEndringerFørstePeriode = beregningsgrunnlagEndring.getAndelerFørstePeriode();

        var atEndringer = andelEndringerFørstePeriode.stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .collect(Collectors.toList());

        var flEndringer = andelEndringerFørstePeriode.stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .collect(Collectors.toList());

        lagHistorikkInnslag(dto, param, atEndringer, flEndringer);
    }


    private void lagHistorikkInnslag(FastsettBeregningsgrunnlagATFLDto dto,
                                     AksjonspunktOppdaterParameter param,
                                     List<BeregningsgrunnlagPrStatusOgAndelEndring> arbeidstakerList,
                                     List<BeregningsgrunnlagPrStatusOgAndelEndring> frilanserList) {

        oppdaterVedEndretVerdi(param.getBehandlingId(), dto.getInntektPrAndelList(), arbeidstakerList, frilanserList, dto.getInntektFrilanser());

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private void oppdaterVedEndretVerdi(Long behandlingId,
                                        List<InntektPrAndelDto> overstyrtList,
                                        List<BeregningsgrunnlagPrStatusOgAndelEndring> arbeidstakerList,
                                        List<BeregningsgrunnlagPrStatusOgAndelEndring> frilanserList,
                                        Integer inntektFrilanser) {

        historikkAdapter.tekstBuilder().medResultat(HistorikkResultatType.BEREGNET_AARSINNTEKT);

        if (inntektFrilanser != null && !frilanserList.isEmpty()) {
            historikkAdapter.tekstBuilder()
                .medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT,
                    frilanserList.get(0).getInntektEndring().flatMap(BeløpEndring::getFraBeløp).orElse(null),
                    frilanserList.get(0).getInntektEndring().map(BeløpEndring::getTilBeløp).orElse(null));
        }

        if (overstyrtList != null) {
            oppdaterForOverstyrt(behandlingId, overstyrtList, arbeidstakerList);
        }

    }

    private void oppdaterForOverstyrt(Long behandlingId,
                                      List<InntektPrAndelDto> overstyrtList,
                                      List<BeregningsgrunnlagPrStatusOgAndelEndring> arbeidstakerList) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        for (var endring : arbeidstakerList) {
            var overstyrt = overstyrtList.stream().filter(andelInntekt -> andelInntekt.getAndelsnr().equals(endring.getAndelsnr())).findFirst();
            if (overstyrt.isPresent()) {
                var visningsNavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(endring.getAktivitetStatus(),
                    endring.getArbeidsgiver(), Optional.ofNullable(endring.getArbeidsforholdRef()), arbeidsforholOverstyringer);
                historikkAdapter.tekstBuilder()
                    .medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, visningsNavn,
                        endring.getInntektEndring().flatMap(BeløpEndring::getFraBeløp).map(BigDecimal::intValue).orElse(null),
                        overstyrt.get().getInntekt());
            }
        }
    }

}
