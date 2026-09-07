package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

public final class OppgittPeriodeRelevans {

    private static final Set<UtsettelseÅrsak> UTSETTELSER_SOM_KREVER_SAKSBEHANDLING = Set.of(UtsettelseÅrsak.SYKDOM,
        UtsettelseÅrsak.INSTITUSJON_BARN, UtsettelseÅrsak.INSTITUSJON_SØKER);

    private OppgittPeriodeRelevans() {
    }

    /**
     * Fjerner utsettelser og opphold som ikke skal saksbehandles. Beholdes:
     * <ul>
     *     <li>Alle perioder som krever sammenhengende uttak (før 2021-regelverket), uendret.</li>
     *     <li>Uttak og overføringer.</li>
     *     <li>Utsettelser som krever dokumentasjon: sykdom, innleggelse, institusjon, eller oppgitt morsaktivitet.</li>
     *     <li>Første periode: utsettelse eller opphold gjøres om til {@link UtsettelseÅrsak#FRI}. Ved endringssøknad brukes
     *     dette når søker vil fjerne allerede innvilget uttak, og perioden må vurderes. Gjelder uavhengig av kilde,
     *     men har bare betydning for endringssøknad.</li>
     * </ul>
     */
    public static List<OppgittPeriodeEntitet> relevantePerioder(List<OppgittPeriodeEntitet> perioder) {
        var førsteFom = perioder.stream().map(OppgittPeriodeEntitet::getFom).min(LocalDate::compareTo).orElse(null);
        return perioder.stream()
            .map(periode -> normaliserFørstePeriode(periode, erFørstePeriode(periode, førsteFom)))
            .filter(periode -> erRelevant(periode, erFørstePeriode(periode, førsteFom)))
            .toList();
    }

    private static boolean erFørstePeriode(OppgittPeriodeEntitet periode, LocalDate førsteFom) {
        return periode.getFom().equals(førsteFom);
    }

    private static OppgittPeriodeEntitet normaliserFørstePeriode(OppgittPeriodeEntitet periode, boolean erFørstePeriode) {
        if (!erFørstePeriode || UtsettelseCore2021.kreverSammenhengendeUttak(periode) || skalBeholdeÅrsak(periode)) {
            return periode;
        }
        if (periode.isUtsettelse() || periode.isOpphold()) {
            return OppgittPeriodeBuilder.ny()
                .medPeriode(periode.getFom(), periode.getTom())
                .medÅrsak(UtsettelseÅrsak.FRI)
                .medMorsAktivitet(periode.getMorsAktivitet())
                .medDokumentasjonVurdering(periode.getDokumentasjonVurdering())
                .medMottattDato(periode.getMottattDato())
                .medTidligstMottattDato(periode.getTidligstMottattDato().orElse(null))
                .medPeriodeKilde(periode.getPeriodeKilde())
                .build();
        }
        return periode;
    }

    public static boolean erRelevant(OppgittPeriodeEntitet periode, boolean erFørstePeriode) {
        if (UtsettelseCore2021.kreverSammenhengendeUttak(periode) || (!periode.isUtsettelse() && !periode.isOpphold())) {
            return true;
        }
        return (erFørstePeriode && UtsettelseÅrsak.FRI.equals(periode.getÅrsak()))
            || (periode.getÅrsak() instanceof UtsettelseÅrsak utsettelse
                && (UTSETTELSER_SOM_KREVER_SAKSBEHANDLING.contains(utsettelse)
                    || MorsAktivitet.forventerDokumentasjon(periode.getMorsAktivitet())));
    }

    static boolean erRelevantUavhengigAvPlassering(OppgittPeriodeEntitet periode) {
        return erRelevant(periode, false);
    }

    private static boolean skalBeholdeÅrsak(OppgittPeriodeEntitet periode) {
        return UtsettelseÅrsak.FRI.equals(periode.getÅrsak()) || UTSETTELSER_SOM_KREVER_SAKSBEHANDLING.contains(periode.getÅrsak());
    }
}
