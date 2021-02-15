package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class DagpengerGirBesteberegning {
    private static final List<Arbeidskategori> ARBEIDSKATEGORI_DAGPENGER = List.of(Arbeidskategori.DAGPENGER, Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);

    /**
     * Utleder om det er dagpenger i opptjening på eller intill (en dag før) skjæringstidsptunktet,
     * eller om det finne sykepenger basert på dagpenger som tilfredstilelr det kravet.
     * @param aktiviteter Opptjeningsaktiviteter
     * @param ytelser ytelser fra IAY aggregatet
     * @param skjæringstidspunkt skjæringstidspunkt for opptjening
     * @return True dersom det finnes dagpenger på eller dagen før skjæringstidspunktet,
     * eller om det finnes sykepenger basert på dagpenger denne dagen.
     * False om dette ikke finnes.
     */
    public static boolean harDagpengerPåEllerIntillSkjæringstidspunkt(OpptjeningAktiviteter aktiviteter, Collection<Ytelse> ytelser, LocalDate skjæringstidspunkt) {
        LocalDate datoSomMåHaDagpenger = skjæringstidspunkt.minusDays(1);
        return harDagpengerPåEllerOppTilSkjæringstidspunktet(aktiviteter, datoSomMåHaDagpenger)
            || harSykepengerMedOvergangFraDagpengerPåEllerOppTilSkjæringstidspunktet(ytelser, datoSomMåHaDagpenger);

    }

    private static boolean harSykepengerMedOvergangFraDagpengerPåEllerOppTilSkjæringstidspunktet(Collection<Ytelse> ytelser, LocalDate dato) {
        return ytelser.stream().filter(y -> y.getRelatertYtelseType().equals(RelatertYtelseType.SYKEPENGER))
            .filter(y -> y.getYtelseGrunnlag().isPresent())
            .filter(y -> y.getPeriode() != null && y.getPeriode().inkluderer(dato))
            .map(y -> y.getYtelseGrunnlag().get())
            .anyMatch(ytelseGrunnlag -> ytelseGrunnlag.getArbeidskategori()
                .map(ARBEIDSKATEGORI_DAGPENGER::contains).orElse(false));
    }

    private static boolean harDagpengerPåEllerOppTilSkjæringstidspunktet(OpptjeningAktiviteter opptjeningAktiviteter, LocalDate dato) {
        return opptjeningAktiviteter.getOpptjeningPerioder().stream()
            .filter(opptjeningPeriode -> opptjeningPeriode.getPeriode().getFom().isBefore(dato) &&
                (opptjeningPeriode.getPeriode().getTom() == null || !opptjeningPeriode.getPeriode().getTom().isBefore(dato)))
            .anyMatch(aktivitet -> aktivitet.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.DAGPENGER));
    }

}
