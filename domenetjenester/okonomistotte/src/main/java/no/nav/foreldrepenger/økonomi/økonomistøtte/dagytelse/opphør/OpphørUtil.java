package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;

public class OpphørUtil {

    public static Set<ØkonomiKodeKlassifik> finnKlassekoderSomIkkeErOpphørt(OppdragInput input) {
        List<Oppdrag110> oppdrag110Liste = input.getAlleTidligereOppdrag110();
        return finnKlassekoderSomIkkeErOpphørt(oppdrag110Liste);
    }

    static Set<ØkonomiKodeKlassifik> finnKlassekoderSomIkkeErOpphørt(List<Oppdrag110> oppdrag110Liste) {
        return førsteAktiveDatoPrKodeKlassifik(oppdrag110Liste).keySet();
    }

    private static Map<ØkonomiKodeKlassifik, LocalDate> førsteAktiveDatoPrKodeKlassifik(List<Oppdrag110> oppdrag110Liste) {
        Map<ØkonomiKodeKlassifik, LocalDate> tidligsteDato = new HashMap<>();

        for (Oppdrag110 oppdrag110 : sorterEtterOpprettetTidspunk(oppdrag110Liste)) {
            for (Oppdragslinje150 linje : sorterEtterDelytelseId(oppdrag110.getOppdragslinje150Liste())) {
                ØkonomiKodeKlassifik kodeKlassifik = ØkonomiKodeKlassifik.fraKode(linje.getKodeKlassifik());
                LocalDate tidligste = tidligsteDato.get(kodeKlassifik);
                if (linje.gjelderOpphør()) {
                    LocalDate opphørsdato = linje.getDatoStatusFom();
                    if (!opphørsdato.isAfter(tidligste)) {
                        tidligsteDato.remove(kodeKlassifik);
                    }
                }
                if (!linje.gjelderOpphør()) {
                    LocalDate fom = linje.getDatoVedtakFom();
                    if (tidligste == null || fom.isBefore(tidligste)) {
                        tidligsteDato.put(kodeKlassifik, fom);
                    }
                }
            }
        }
        return tidligsteDato;
    }

    private static List<Oppdragslinje150> sorterEtterDelytelseId(Collection<Oppdragslinje150> input) {
        return input.stream()
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId))
            .collect(Collectors.toList());
    }

    private static List<Oppdrag110> sorterEtterOpprettetTidspunk(Collection<Oppdrag110> input) {
        return input.stream()
            .sorted(Comparator.comparing(Oppdrag110::getOpprettetTidspunkt))
            .collect(Collectors.toList());
    }
}

