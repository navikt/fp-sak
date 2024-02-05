package no.nav.foreldrepenger.domene.opptjening;


import java.util.Map;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class OpptjeningsaktiviteterPerYtelse {

    private static final Map<FagsakYtelseType, Set<OpptjeningAktivitetType>> EKSKLUDERTE_AKTIVITETER_PER_YTELSE = Map.of(
        FagsakYtelseType.FORELDREPENGER, Set.of(
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD),
        FagsakYtelseType.SVANGERSKAPSPENGER, Set.of(
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
    boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType) {
        return !ekskluderteAktiviteter.contains(opptjeningAktivitetType);
    }
}
