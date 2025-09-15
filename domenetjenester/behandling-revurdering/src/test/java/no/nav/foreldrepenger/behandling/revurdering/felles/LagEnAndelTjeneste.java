package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.ARBEIDSFORHOLDLISTE;
import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.ORGNR;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;

public class LagEnAndelTjeneste implements LagAndelTjeneste {

    @Override
    public List<BeregningsgrunnlagPrStatusOgAndel> lagAndeler(boolean medOppjustertDagsat,
                                                              boolean skalDeleAndelMellomArbeidsgiverOgBruker) {
        var ds = new Dagsatser(medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker);
        var bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(0))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        var andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(240000))
            .medBruttoPrÅr(BigDecimal.valueOf(240000))
            .medRedusertBrukersAndelPrÅr(ds.getDagsatsBruker())
            .medDagsatsBruker(ds.getDagsatsBruker().longValue())
            .medRedusertRefusjonPrÅr(ds.getDagsatsArbeidstaker())
            .medDagsatsArbeidsgiver(ds.getDagsatsArbeidstaker().longValue())
            .medKilde(AndelKilde.PROSESS_START)
            .build();
        return List.of(andel);
    }
}
