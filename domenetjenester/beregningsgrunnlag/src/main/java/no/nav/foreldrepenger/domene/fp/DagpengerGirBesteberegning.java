package no.nav.foreldrepenger.domene.fp;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

class DagpengerGirBesteberegning {
    private static final List<Arbeidskategori> ARBEIDSKATEGORI_DAGPENGER = List.of(Arbeidskategori.DAGPENGER,
        Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);

    private DagpengerGirBesteberegning() {
    }

    /**
     * Utleder om det er dagpenger i opptjening på eller intill (en dag før) skjæringstidsptunktet,
     * eller om det finne sykepenger basert på dagpenger som tilfredstilelr det kravet.
     *
     * @param aktiviteter        Opptjeningsaktiviteter
     * @param ytelser            ytelser fra IAY aggregatet
     * @param skjæringstidspunkt skjæringstidspunkt for opptjening
     * @return True dersom det finnes dagpenger på eller dagen før skjæringstidspunktet,
     * eller om det finnes sykepenger basert på dagpenger denne dagen.
     * False om dette ikke finnes.
     */
    static boolean harDagpengerPåEllerIntillSkjæringstidspunkt(OpptjeningAktiviteter aktiviteter,
                                                               Collection<Ytelse> ytelser,
                                                               LocalDate skjæringstidspunkt) {
        var datoSomMåHaDagpenger = finnDatoSomSkalSjekkesForDPEllerSP(skjæringstidspunkt);
        return harDagpengerPåEllerOppTilSkjæringstidspunktet(aktiviteter, datoSomMåHaDagpenger)
            || harSykepengerMedOvergangFraDagpengerPåEllerOppTilSkjæringstidspunktet(ytelser, datoSomMåHaDagpenger);

    }

    private static boolean harSykepengerMedOvergangFraDagpengerPåEllerOppTilSkjæringstidspunktet(Collection<Ytelse> ytelser,
                                                                                                 LocalDate dato) {
        return ytelser.stream()
            .filter(y -> RelatertYtelseType.SYKEPENGER.equals(y.getRelatertYtelseType()))
            .filter(y -> RelatertYtelseTilstand.AVSLUTTET.equals(y.getStatus()) || RelatertYtelseTilstand.LØPENDE.equals(y.getStatus()))
            .filter(y -> y.getYtelseGrunnlag().isPresent())
            .filter(y -> y.getPeriode() != null && y.getPeriode().inkluderer(dato))
            .map(y -> y.getYtelseGrunnlag().get())
            .anyMatch(ytelseGrunnlag -> ytelseGrunnlag.getArbeidskategori()
                .map(ARBEIDSKATEGORI_DAGPENGER::contains)
                .orElse(false));
    }

    private static boolean harDagpengerPåEllerOppTilSkjæringstidspunktet(OpptjeningAktiviteter opptjeningAktiviteter,
                                                                         LocalDate dato) {
        return opptjeningAktiviteter.getOpptjeningPerioder()
            .stream()
            .filter(opptjeningPeriode -> !opptjeningPeriode.periode().getFom().isAfter(dato) && (
                opptjeningPeriode.periode().getTom() == null || !opptjeningPeriode.periode()
                    .getTom()
                    .isBefore(dato)))
            .anyMatch(aktivitet -> aktivitet.opptjeningAktivitetType().equals(OpptjeningAktivitetType.DAGPENGER));
    }

    private static LocalDate finnDatoSomSkalSjekkesForDPEllerSP(LocalDate skjæringstidspunkt) {
        var datoFørStp = skjæringstidspunkt.minusDays(1);
        return VirkedagUtil.tomVirkedag(datoFørStp);
    }
}
