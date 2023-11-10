package no.nav.foreldrepenger.domene.opptjening;


import java.util.Map;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public class OpptjeningsaktiviteterPerYtelse {

    private static final Map<FagsakYtelseType, Set<OpptjeningAktivitetType>> EKSKLUDERTE_AKTIVITETER_PER_YTELSE = Map.of(
        FagsakYtelseType.FORELDREPENGER, Set.of(
            OpptjeningAktivitetType.FRILOPP,
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD),
        FagsakYtelseType.SVANGERSKAPSPENGER, Set.of(
            OpptjeningAktivitetType.FRILOPP,
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
            OpptjeningAktivitetType.DAGPENGER,
            OpptjeningAktivitetType.ARBEIDSAVKLARING,
            OpptjeningAktivitetType.VENTELØNN_VARTPENGER,
            OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE));

    private final Set<OpptjeningAktivitetType> ekskluderteAktiviteter;

    public OpptjeningsaktiviteterPerYtelse(FagsakYtelseType fagsakYtelseType) {
        ekskluderteAktiviteter = EKSKLUDERTE_AKTIVITETER_PER_YTELSE.get(fagsakYtelseType);
    }

    public boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (OpptjeningAktivitetType.FRILANS.equals(opptjeningAktivitetType)) {
            return harOppgittFrilansISøknad(iayGrunnlag);
        }
        return erRelevantAktivitet(opptjeningAktivitetType);
    }

    boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType) {
        return !ekskluderteAktiviteter.contains(opptjeningAktivitetType);
    }

    private boolean harOppgittFrilansISøknad(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getOppgittOpptjening().stream()
            .flatMap(oppgittOpptjening -> oppgittOpptjening.getAnnenAktivitet().stream())
            .anyMatch(annenAktivitet -> annenAktivitet.getArbeidType().equals(ArbeidType.FRILANSER));
    }
}
