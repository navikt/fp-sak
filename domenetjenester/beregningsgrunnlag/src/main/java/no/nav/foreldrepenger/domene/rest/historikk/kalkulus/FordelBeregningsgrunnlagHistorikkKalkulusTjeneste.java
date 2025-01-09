package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkUtil.lagEndringsoppsummeringForHistorikk;
import static no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkUtil.leggTilArbeidsforholdHistorikkinnslag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkUtil;

@ApplicationScoped
public class FordelBeregningsgrunnlagHistorikkKalkulusTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    FordelBeregningsgrunnlagHistorikkKalkulusTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                             HistorikkinnslagRepository historikkinnslagRepository,
                                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public OppdateringResultat lagHistorikk(FordelBeregningsgrunnlagDto dto,
                                            Optional<OppdaterBeregningsgrunnlagResultat> endringsaggregat,
                                            AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        var endredePerioder = endringsaggregat.flatMap(OppdaterBeregningsgrunnlagResultat::getBeregningsgrunnlagEndring)
            .map(BeregningsgrunnlagEndring::getBeregningsgrunnlagPeriodeEndringer)
            .orElse(List.of());
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        for (var endretPeriode : dto.getEndretBeregningsgrunnlagPerioder()) {
            var korrektPeriodeFom = FordelBeregningsgrunnlagHistorikkUtil.getKorrektPeriodeEndring(endredePerioder, endretPeriode)
                .getPeriode().getFom();
            linjeBuilder = lagHistorikk(endretPeriode, korrektPeriodeFom, arbeidsforholdOverstyringer);
        }

        if (dto.getBegrunnelse() != null) {
            linjeBuilder.add(new HistorikkinnslagLinjeBuilder().tekst(dto.getBegrunnelse()));
        }
        var historikkinnslagBuilder = FordelBeregningsgrunnlagHistorikkUtil.lagHistorikkInnslag(param, linjeBuilder);
        historikkinnslagBuilder.ifPresent(builder -> historikkinnslagRepository.lagre(builder.build()));

        return OppdateringResultat.utenOverhopp();
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikk(FordelBeregningsgrunnlagPeriodeDto endretPeriode,
                                                            LocalDate korrektPeriodeFom,
                                                            List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        for (var endretAndel : endretPeriode.getAndeler()) {
            var endring = lagEndringsoppsummeringForHistorikk(endretAndel).build();
            var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(endring.getAktivitetStatus(),
                endring.getArbeidsgiver(), endring.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
            linjeBuilder.addAll(leggTilArbeidsforholdHistorikkinnslag(endring, korrektPeriodeFom, arbeidsforholdInfo));
        }
        return linjeBuilder;
    }
}
