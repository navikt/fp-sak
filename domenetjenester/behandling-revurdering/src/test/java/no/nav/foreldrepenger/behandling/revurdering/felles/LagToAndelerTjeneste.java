package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.ARBEIDSFORHOLDLISTE;
import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.ORGNR;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;

public class LagToAndelerTjeneste implements LagAndelTjeneste {

    @Override
    public List<BeregningsgrunnlagPrStatusOgAndel> lagAndeler(boolean medOppjustertDagsat,
                                                              boolean skalDeleAndelMellomArbeidsgiverOgBruker) {
        var dagsatser = Arrays.asList(new Dagsatser(true, skalDeleAndelMellomArbeidsgiverOgBruker),
            new Dagsatser(false, skalDeleAndelMellomArbeidsgiverOgBruker));
        var bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(0))
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        var andel1 = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAndelsnr(1L)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(240000))
            .medDagsatsBruker(dagsatser.get(0).getDagsatsBruker().longValue())
            .medDagsatsArbeidsgiver(dagsatser.get(0).getDagsatsArbeidstaker().longValue())
            .medRedusertBrukersAndelPrÅr(dagsatser.get(0).getDagsatsBruker())
            .medRedusertRefusjonPrÅr(dagsatser.get(0).getDagsatsArbeidstaker())
            .medBruttoPrÅr(BigDecimal.valueOf(240000))
            .medKilde(AndelKilde.PROSESS_START)
            .build();
        var bga2 = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(1))
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        var andel2 = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga2)
            .medAndelsnr(2L)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(240000))
            .medRedusertBrukersAndelPrÅr(dagsatser.get(1).getDagsatsBruker())
            .medDagsatsBruker(dagsatser.get(1).getDagsatsBruker().longValue())
            .medDagsatsArbeidsgiver(dagsatser.get(1).getDagsatsArbeidstaker().longValue())
            .medRedusertRefusjonPrÅr(dagsatser.get(1).getDagsatsArbeidstaker())
            .medBruttoPrÅr(BigDecimal.valueOf(240000))
            .medKilde(AndelKilde.PROSESS_START)
            .build();
        return Arrays.asList(andel1, andel2);
    }
}
