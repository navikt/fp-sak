package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;

@ApplicationScoped
public class FastsettBeregningsgrunnlagATFLHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Historikkinnslag2Repository historikkRepo;

    FastsettBeregningsgrunnlagATFLHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                           InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                           Historikkinnslag2Repository historikkRepo) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkRepo = historikkRepo;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             FastsettBeregningsgrunnlagATFLDto dto,
                             BeregningsgrunnlagEntitet forrigeGrunnlag) {
        var førstePeriode = forrigeGrunnlag.getBeregningsgrunnlagPerioder().getFirst();

        var atAndeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .toList();

        var flAndeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .toList();

        lagHistorikkInnslag(dto, param, atAndeler, flAndeler);
    }


    private void lagHistorikkInnslag(FastsettBeregningsgrunnlagATFLDto dto,
                                     AksjonspunktOppdaterParameter param,
                                     List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList,
                                     List<BeregningsgrunnlagPrStatusOgAndel> frilanserList) {
        var historikkBuilder = new Historikkinnslag2.Builder();

        oppdaterVedEndretVerdi(param.getBehandlingId(), dto.getInntektPrAndelList(), arbeidstakerList, frilanserList,
            dto.getInntektFrilanser(), historikkBuilder);

        var ref = param.getRef();
        historikkBuilder.addTekstlinje(dto.getBegrunnelse())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(ref.fagsakId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkRepo.lagre(historikkBuilder.build());
    }

    private void oppdaterVedEndretVerdi(Long behandlingId,
                                        List<InntektPrAndelDto> overstyrtList,
                                        List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList,
                                        List<BeregningsgrunnlagPrStatusOgAndel> frilanserList,
                                        Integer inntektFrilanser,
                                        Historikkinnslag2.Builder historikkBuilder) {
        if (arbeidstakerList.stream()
            .noneMatch(bgpsa -> bgpsa.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))) {
            historikkBuilder.addTekstlinje("Grunnlag for beregnet årsinntekt:");
        }

        if (inntektFrilanser != null && !frilanserList.isEmpty()) {
            historikkBuilder.addTekstlinje(HistorikkinnslagTekstlinjeBuilder.fraTilEquals("Frilansinntekt", null, inntektFrilanser));
        }

        if (overstyrtList != null) {
            oppdaterForOverstyrt(behandlingId, overstyrtList, arbeidstakerList, historikkBuilder);
        }

    }

    private void oppdaterForOverstyrt(Long behandlingId,
                                      List<InntektPrAndelDto> overstyrtList,
                                      List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList,
                                      Historikkinnslag2.Builder historikkBuilder) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getArbeidsforholdOverstyringer();
        for (var prStatus : arbeidstakerList) {
            var overstyrt = overstyrtList.stream()
                .filter(andelInntekt -> andelInntekt.getAndelsnr().equals(prStatus.getAndelsnr()))
                .findFirst();
            if (overstyrt.isPresent()) {
                var visningsNavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                    prStatus.getAktivitetStatus(), prStatus.getArbeidsgiver(), prStatus.getArbeidsforholdRef(),
                    arbeidsforholOverstyringer);
                var textBuilder = new HistorikkinnslagTekstlinjeBuilder();
                historikkBuilder.addTekstlinje(textBuilder.til(String.format("Inntekt fra %s", visningsNavn), overstyrt.get().getInntekt()));
            }
        }
    }

}
