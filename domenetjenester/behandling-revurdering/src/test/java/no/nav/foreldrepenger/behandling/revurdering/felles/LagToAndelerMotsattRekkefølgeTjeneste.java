package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingBehandlingsresultatutlederTest.ARBEIDSFORHOLDLISTE;
import static no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingBehandlingsresultatutlederTest.ORGNR;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;

public class LagToAndelerMotsattRekkefølgeTjeneste implements LagAndelTjeneste {

    @Override
    public void lagAndeler(BeregningsgrunnlagPeriode periode,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker) {
        List<Dagsatser> dagsatser = Arrays.asList(new Dagsatser(true, skalDeleAndelMellomArbeidsgiverOgBruker),
                new Dagsatser(false, skalDeleAndelMellomArbeidsgiverOgBruker));
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(1))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBeregnetPrÅr(BigDecimal.valueOf(240000))
                .medRedusertBrukersAndelPrÅr(dagsatser.get(1).getDagsatsBruker())
                .medRedusertRefusjonPrÅr(dagsatser.get(1).getDagsatsArbeidstaker())
                .build(periode);
        BGAndelArbeidsforhold.Builder bga2 = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(0))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga2)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBeregnetPrÅr(BigDecimal.valueOf(240000))
                .medRedusertBrukersAndelPrÅr(dagsatser.get(0).getDagsatsBruker())
                .medRedusertRefusjonPrÅr(dagsatser.get(0).getDagsatsArbeidstaker())
                .build(periode);
    }
}
