package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragKvitteringTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;

public class OpphørUtil {

    static Set<ØkonomiKodeKlassifik> finnKlassekoderSomIkkeErOpphørt(List<Oppdrag110> oppdrag110Liste) {
        return førsteAktiveDatoPrKodeKlassifik(oppdrag110Liste, false, null).keySet();
    }

    private static Map<ØkonomiKodeKlassifik, LocalDate> førsteAktiveDatoPrKodeKlassifik(List<Oppdrag110> oppdrag110Liste, boolean kunBruker, String refunderesId) {
        Map<ØkonomiKodeKlassifik, LocalDate> tidligsteDato = new HashMap<>();

        for (Oppdrag110 oppdrag110 : sorterEtterOpprettetTidspunk(oppdrag110Liste)) {
            for (Oppdragslinje150 linje : sorterEtterDelytelseId(oppdrag110.getOppdragslinje150Liste())) {
                if (kunBruker && linje.getUtbetalesTilId() == null) {
                    continue;
                }
                if (!kunBruker && refunderesId != null && linje.getRefusjonsinfo156() != null && !Objects.equals(linje.getRefusjonsinfo156().getRefunderesId(), refunderesId)) {
                    continue;
                }
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

    static boolean erBrukerAllredeFullstendigOpphørt(OppdragInput behandlingInfo) {
        List<Oppdrag110> oppdragForBruker = behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(OpphørUtil::gjelderBruker)
            .filter(o -> o.venterKvittering() || OppdragKvitteringTjeneste.harPositivKvittering(o))
            .collect(Collectors.toList());

        return førsteAktiveDatoPrKodeKlassifik(oppdragForBruker, true, null).isEmpty();
    }

    static boolean erArbeidsgiverAllredeFullstendigOpphørt(OppdragInput behandlingInfo, String refunderesId) {
        List<Oppdrag110> oppdragForAG = behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(o -> gjelderArbeidsgiver(o, refunderesId))
            .filter(o -> o.venterKvittering() || OppdragKvitteringTjeneste.harPositivKvittering(o))
            .collect(Collectors.toList());

        return førsteAktiveDatoPrKodeKlassifik(oppdragForAG, false, refunderesId).isEmpty();
    }

    private static boolean gjelderBruker(Oppdrag110 oppdrag110) {
        return oppdrag110.getOppdragslinje150Liste().stream().anyMatch(ol -> ol.getUtbetalesTilId() != null);
    }

    private static boolean gjelderArbeidsgiver(Oppdrag110 oppdrag110, String refunderesId) {
        return oppdrag110.getOppdragslinje150Liste().stream().anyMatch(ol -> ol.getRefusjonsinfo156() != null && Objects.equals(ol.getRefusjonsinfo156().getRefunderesId(), refunderesId));
    }

}

