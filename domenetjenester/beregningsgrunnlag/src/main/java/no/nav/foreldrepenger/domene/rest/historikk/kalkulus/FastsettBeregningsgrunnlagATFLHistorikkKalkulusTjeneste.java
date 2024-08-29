package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                                   ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    /**
     * For bergninger som skjer i fp-kalkulus
     * @param param
     * @param dto
     * @param oppdaterBeregningsgrunnlagResultat
     */
    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             FastsettBeregningsgrunnlagATFLDto dto,
                             OppdaterBeregningsgrunnlagResultat oppdaterBeregningsgrunnlagResultat) {
        var bgPerioder = oppdaterBeregningsgrunnlagResultat.getBeregningsgrunnlagEndring()
            .map(BeregningsgrunnlagEndring::getBeregningsgrunnlagPeriodeEndringer)
            .orElse(Collections.emptyList());
        var andelerIFørstePeriode = bgPerioder.stream()
            .min(Comparator.comparing(bgp -> bgp.getPeriode().getFom()))
            .map(BeregningsgrunnlagPeriodeEndring::getBeregningsgrunnlagPrStatusOgAndelEndringer)
            .orElse(Collections.emptyList());

        var atAndeler = andelerIFørstePeriode.stream().filter(a -> a.getAktivitetStatus().erArbeidstaker()).toList();
        var flAndeler = andelerIFørstePeriode.stream().filter(a -> a.getAktivitetStatus().erFrilanser()).toList();

        oppdaterEndringer(param.getBehandlingId(), dto.getInntektPrAndelList(), atAndeler, flAndeler,
            dto.getInntektFrilanser());

        ferdigStillHistorikkInnslag(dto, param);
    }

    private void oppdaterEndringer(Long behandlingId,
                                   List<InntektPrAndelDto> inntektPrAndelList,
                                   List<BeregningsgrunnlagPrStatusOgAndelEndring> atAndeler,
                                   List<BeregningsgrunnlagPrStatusOgAndelEndring> flAndeler,
                                   Integer inntektFrilanser) {
        if (atAndeler.stream()
            .noneMatch(bgpsa -> bgpsa.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))) {
            historikkAdapter.tekstBuilder().medResultat(HistorikkResultatType.BEREGNET_AARSINNTEKT);
        }

        if (inntektFrilanser != null && !flAndeler.isEmpty()) {
            historikkAdapter.tekstBuilder()
                .medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT, null, inntektFrilanser);
        }

        if (inntektPrAndelList != null) {
            oppdaterEndringVedOverstyrt(behandlingId, atAndeler);
        }
    }

    /**
     * For bergninger som skjer i fp-sak
     * @param param
     * @param dto
     */

    private void ferdigStillHistorikkInnslag(FastsettBeregningsgrunnlagATFLDto dto, AksjonspunktOppdaterParameter param) {
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private void oppdaterEndringVedOverstyrt(Long behandlingId,
                                             List<BeregningsgrunnlagPrStatusOgAndelEndring> arbeidstakerList) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getArbeidsforholdOverstyringer();
        for (var endretAndel : arbeidstakerList) {
            if (endretAndel != null && endretAndel.getInntektEndring().isPresent()) {
                var visningsNavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                    endretAndel.getAktivitetStatus(),
                    endretAndel.getArbeidsgiver(),
                    Optional.ofNullable(endretAndel.getArbeidsforholdRef()),
                    arbeidsforholOverstyringer);
                var fra = endretAndel.getInntektEndring().get().getFraBeløp();
                var til = endretAndel.getInntektEndring().get().getTilBeløp();
                if (fra.isEmpty() || fra.get().compareTo(til) != 0) {
                    historikkAdapter.tekstBuilder()
                        .medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, visningsNavn, null,
                            til.intValue());
                }
            }
        }    }

}
