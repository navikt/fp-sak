package no.nav.foreldrepenger.domene.fp;

import no.nav.folketrygdloven.kalkulator.modell.iay.InntektDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektspostDtoBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.modell.BesteberegningMånedsgrunnlagEntitet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BesteberegningAvvikskontrollererTest {
    private static final LocalDate STP = LocalDate.of(2021,8,1);
    private static final Long BEHANDLING_ID = 9999999L;

    @Test
    public void skal_teste_ingen_avvik() {
        String orgnr = "999999999";
        InntektDto inntektFraRegister = InntektDtoBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver.virksomhet(orgnr))
            .leggTilInntektspost(lagPost(månederFørSTP(1), 250))
            .leggTilInntektspost(lagPost(månederFørSTP(2), 250))
            .leggTilInntektspost(lagPost(månederFørSTP(3), 250))
            .leggTilInntektspost(lagPost(månederFørSTP(4), 250))
            .leggTilInntektspost(lagPost(månederFørSTP(5), 250))
            .leggTilInntektspost(lagPost(månederFørSTP(6), 250))
            .leggTilInntektspost(lagPost(månederFørSTP(7), 250))
            .build();

        Set<BesteberegningMånedsgrunnlagEntitet> bbMåneder = Set.of(lagBBMåned(orgnr, månederFørSTP(1), 250),
            lagBBMåned(orgnr, månederFørSTP(2), 250),
            lagBBMåned(orgnr, månederFørSTP(3), 250));
        Map<Arbeidsgiver, BigDecimal> avvikPrAG = BesteberegningAvvikskontrollerer.finnArbeidsgivereMedAvvikendeInntekt(BEHANDLING_ID, STP, bbMåneder,
            Collections.singletonList(inntektFraRegister));

        assertThat(avvikPrAG).hasSize(1);
        BigDecimal avvik = avvikPrAG.get(Arbeidsgiver.virksomhet(orgnr));
        assertThat(avvik.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    /**
     * Snittlønn 300
     * Max inntekt 1000
     * Diff 700
     * Forventet avvik = (700 / 300) * 100 = 233
     */
    @Test
    public void skal_teste_at_vi_får_over_25_prosent_avvik() {
        String orgnr = "999999999";
        InntektDto inntektFraRegister = InntektDtoBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver.virksomhet(orgnr))
            .leggTilInntektspost(lagPost(månederFørSTP(1), 200))
            .leggTilInntektspost(lagPost(månederFørSTP(2), 1000))
            .leggTilInntektspost(lagPost(månederFørSTP(3), 200))
            .leggTilInntektspost(lagPost(månederFørSTP(4), 200))
            .leggTilInntektspost(lagPost(månederFørSTP(5), 200))
            .leggTilInntektspost(lagPost(månederFørSTP(6), 200))
            .leggTilInntektspost(lagPost(månederFørSTP(7), 200))
            .leggTilInntektspost(lagPost(månederFørSTP(8), 200))
            .build();

        Set<BesteberegningMånedsgrunnlagEntitet> bbMåneder = Set.of(lagBBMåned(orgnr, månederFørSTP(1), 250),
            lagBBMåned(orgnr, månederFørSTP(2), 1000),
            lagBBMåned(orgnr, månederFørSTP(3), 250));
        Map<Arbeidsgiver, BigDecimal> avvikPrAG = BesteberegningAvvikskontrollerer.finnArbeidsgivereMedAvvikendeInntekt(BEHANDLING_ID, STP, bbMåneder,
            Collections.singletonList(inntektFraRegister));

        assertThat(avvikPrAG).hasSize(1);
        BigDecimal avvik = avvikPrAG.get(Arbeidsgiver.virksomhet(orgnr));
        assertThat(avvik).isEqualByComparingTo(BigDecimal.valueOf(233));
    }


    private BesteberegningMånedsgrunnlagEntitet lagBBMåned(String orgnr, LocalDate fom, int i) {
        return BesteberegningMånedsgrunnlagEntitet.ny()
            .medPeriode(fom.withDayOfMonth(1), fom.with(TemporalAdjusters.lastDayOfMonth()))
            .leggTilInntekt(BesteberegningInntektEntitet.ny()
                .medInntekt(BigDecimal.valueOf(i))
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet(orgnr))
                .build())
            .build();
    }

    private LocalDate månederFørSTP(int i) {
        return STP.minusMonths(i);
    }

    private InntektspostDtoBuilder lagPost(LocalDate fom, int beløp) {
        return InntektspostDtoBuilder.ny()
            .medPeriode(fom.withDayOfMonth(1), fom.with(TemporalAdjusters.lastDayOfMonth()))
            .medBeløp(BigDecimal.valueOf(beløp));
    }

}
