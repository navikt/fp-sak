package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static no.nav.foreldrepenger.økonomistøtte.oppdrag.NyOppdragskontrollTjenesteTestBase.I_ÅR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;

public class OppdragskontrollTestVerktøy {

    private OppdragskontrollTestVerktøy() {
    }

    public static List<Oppdragslinje150> getOppdragslinje150MedKlassekode(Oppdragskontroll oppdrag, KodeKlassifik klassekode) {
        var alleOppdr150Liste = getOppdragslinje150Liste(oppdrag);
        return getOppdragslinje150MedKlassekode(alleOppdr150Liste, klassekode);
    }

    public static List<Oppdragslinje150> getOppdragslinje150MedKlassekode(List<Oppdragslinje150> opp150Liste, KodeKlassifik klassekode) {
        return opp150Liste.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(klassekode))
                .toList();
    }

    public static Oppdrag110 getOppdrag110ForBruker(List<Oppdrag110> oppdrag110Liste) {
        return oppdrag110Liste.stream()
                .filter(oppdrag110 -> KodeFagområde.FP.equals(oppdrag110.getKodeFagomrade()))
                .findFirst().get();
    }

    public static Oppdrag110 getOppdrag110ForArbeidsgiver(List<Oppdrag110> oppdrag110Liste, String virksomhetOrgnr) {
        return getOpp150ListeForEnVirksomhet(oppdrag110Liste, virksomhetOrgnr).get(0).getOppdrag110();
    }

    public static Oppdragslinje150 getOpp150ForEnVirksomhet(List<Oppdragslinje150> oppdragslinje150Liste, String virksomhetOrgnr) {
        return oppdragslinje150Liste.stream()
                .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156().getRefunderesId().equals(endreTilElleveSiffer(virksomhetOrgnr)))
                .findFirst().get();
    }

    public static List<Oppdragslinje150> getOpp150ListeForEnVirksomhet(List<Oppdrag110> oppdrag110Liste, String virksomhetOrgnr) {
        return oppdrag110Liste.stream()
                .filter(oppdrag110 -> KodeFagområde.FPREF.equals(oppdrag110.getKodeFagomrade()))
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156().getRefunderesId().equals(endreTilElleveSiffer(virksomhetOrgnr)))
                .toList();
    }

    public static List<Oppdragslinje150> getOpp150ListeForBruker(List<Oppdrag110> oppdrag110Liste) {
        return oppdrag110Liste.stream()
                .filter(oppdrag110 -> KodeFagområde.FP.equals(oppdrag110.getKodeFagomrade()))
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .toList();
    }

    public static void verifiserKjedingForOppdragslinje150(List<Oppdragslinje150> originaltOppdr150ListeAT, List<Oppdragslinje150> originaltOppdr150ListeFL) {
        for (var opp150FL : originaltOppdr150ListeFL) {
            assertThat(originaltOppdr150ListeAT).isNotEmpty().allSatisfy(opp150AT -> {
                assertThat(opp150AT.getDelytelseId()).isNotEqualTo(opp150FL.getDelytelseId());
                assertThat(opp150AT.getRefDelytelseId()).isNotEqualTo(opp150FL.getDelytelseId());
                assertThat(opp150FL.getRefDelytelseId()).isNotEqualTo(opp150AT.getDelytelseId());
            });
        }
    }

    public static void verifiserOppdr150SomAndelerSlåSammen(Oppdragskontroll originaltOppdrag, Oppdragskontroll revurderingOppdrag) {
        var originaltoppdr150Liste = getOppdragslinje150Liste(originaltOppdrag);
        var originaltOppdr150ListeBruker = originaltoppdr150Liste.stream().filter(opp150 -> opp150.getOppdrag110().getKodeFagomrade().equals(KodeFagområde.FP))
                .filter(opp150 -> !opp150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER)).toList();
        var revurderingOppdr150Liste = getOppdragslinje150Liste(revurderingOppdrag);
        var revurderingOppdr150ListeBruker = revurderingOppdr150Liste.stream().filter(opp150 -> opp150.getOppdrag110().getKodeFagomrade().equals(KodeFagområde.FP))
                .filter(opp150 -> !opp150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER)).filter(opp150 -> opp150.getKodeEndringLinje().equals(KodeEndringLinje.NY))
                .filter(opp150 -> opp150.getDatoVedtakFom().equals(LocalDate.now().plusDays(8))).toList();

        assertThat(originaltOppdr150ListeBruker).hasSize(1);
        assertThat(revurderingOppdr150ListeBruker).hasSize(1);
        assertThat(originaltOppdr150ListeBruker.get(0).getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(revurderingOppdr150ListeBruker.get(0).getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(originaltOppdr150ListeBruker.get(0).getSats()).isEqualTo(Sats.på(3000L));
        assertThat(revurderingOppdr150ListeBruker.get(0).getSats()).isEqualTo(Sats.på(3100L));
    }

    public static void verifiserOppdr150MedNyKlassekode(List<Oppdragslinje150> opp150RevurdListe) {
        var opp150Liste = opp150RevurdListe.stream()
                .filter(oppdr150 -> !oppdr150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER) && !oppdr150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG))
                .filter(oppdr150 -> oppdr150.getKodeEndringLinje().equals(KodeEndringLinje.NY)).toList();
        var klasseKodeListe = opp150Liste.stream().map(Oppdragslinje150::getKodeKlassifik).distinct().toList();
        assertThat(klasseKodeListe).containsOnly(KodeKlassifik.FPF_DAGPENGER, KodeKlassifik.FPF_REFUSJON_AG);
        assertThat(opp150Liste).anySatisfy(opp150 -> assertThat(opp150.getRefDelytelseId()).isNull());
    }

    public static void verifiserDelYtelseOgFagsystemIdForEnKlassekode(List<Oppdragslinje150> opp150RevurderingListe, List<Oppdragslinje150> opp150OriginalListe) {
        var førsteOpp150IRevurderingKjede = opp150RevurderingListe.stream().min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).get();
        assertThat(førsteOpp150IRevurderingKjede.getRefDelytelseId()).isNotNull();
        assertThat(førsteOpp150IRevurderingKjede.getRefFagsystemId()).isNotNull();
        assertThat(opp150OriginalListe).anySatisfy(opp150Original -> assertThat(opp150Original.getDelytelseId()).isEqualTo(førsteOpp150IRevurderingKjede.getRefDelytelseId()));
    }

    public static void verifiserDelYtelseOgFagsystemIdForFlereKlassekode(List<Oppdragslinje150> opp150RevurderingListe, List<Oppdragslinje150> opp150OriginalListe) {
        opp150OriginalListe.removeIf(opp150 -> opp150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER)
                || opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG));
        var klassekodeListe = opp150OriginalListe.stream().map(Oppdragslinje150::getKodeKlassifik).distinct().toList();
        klassekodeListe.forEach(kode -> {
            if (opp150RevurderingListe.stream().anyMatch(opp150 -> opp150.getKodeKlassifik().equals(kode))) {
                var førsteOpp150IKjede = opp150RevurderingListe.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(kode) && !opp150.gjelderOpphør())
                        .min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).get();
                assertThat(opp150OriginalListe).anySatisfy(opp150 -> assertThat(opp150.getDelytelseId()).isEqualTo(førsteOpp150IKjede.getRefDelytelseId()));
            }
        });
    }

    public static List<Oppdragslinje150> getOppdragslinje150Liste(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .toList();
    }

    public static boolean erOpp150ForFeriepenger(Oppdragslinje150 opp150) {
        return opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG) ||
                opp150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER);
    }

    public static String endreTilElleveSiffer(String id) {
        if (id.length() == 11) {
            return id;
        }
        return "00" + id;
    }

    public static void verifiserAvstemming(Oppdragskontroll oppdragRevurdering) {
        var avstemmingRevurdList = oppdragRevurdering.getOppdrag110Liste().stream()
                .map(Oppdrag110::getAvstemming).toList();

        assertThat(avstemmingRevurdList).isNotEmpty();
        assertThat(avstemmingRevurdList).hasSameSizeAs(oppdragRevurdering.getOppdrag110Liste());
        for (var avstemmingRevurd : avstemmingRevurdList) {
            assertThat(avstemmingRevurd.getNøkkel()).isNotEmpty();
            assertThat(avstemmingRevurd.getTidspunkt()).isNotEmpty();
            assertThat(avstemmingRevurd.getNøkkel()).isEqualTo(avstemmingRevurd.getTidspunkt());
        }
    }

    public static void verifiserGrad(List<Oppdragslinje150> opp150RevurderingList, Oppdragskontroll originaltOppdrag) {
        var originaltOpp150Liste = getOppdragslinje150Liste(originaltOppdrag);

        for (var opp150Revurdering : opp150RevurderingList) {
            if (!erOpp150ForFeriepenger(opp150Revurdering)) {
                assertThat(opp150Revurdering.getUtbetalingsgrad()).isNotNull();
            } else {
                assertThat(opp150Revurdering.getUtbetalingsgrad()).isNull();
            }
            var originaltOpp150 = originaltOpp150Liste.stream().
                    filter(opp150 -> opp150.getDelytelseId().equals(opp150Revurdering.getDelytelseId())).findFirst().orElse(null);
            if (originaltOpp150 != null && !erOpp150ForFeriepenger(originaltOpp150)) {
                var utbetalingsgradRevurdering = opp150Revurdering.getUtbetalingsgrad();
                var utbetalingsgradOriginalt = originaltOpp150.getUtbetalingsgrad();
                assertThat(utbetalingsgradRevurdering.getVerdi()).isEqualTo(utbetalingsgradOriginalt.getVerdi());
            }
        }
    }

    public static void verifiserRefusjonInfo156(List<Oppdrag110> opp110RevurderingList, Oppdragskontroll originaltOppdrag) {

        var opp150RevurderingList = opp110RevurderingList.stream().filter(opp110 -> opp110.getKodeFagomrade().equals(KodeFagområde.FPREF))
                .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
                .toList();

        var originaltOpp150Liste = getOppdragslinje150Liste(originaltOppdrag);

        for (var opp150Revurdering : opp150RevurderingList) {
            var originaltOpp150 = originaltOpp150Liste.stream().
                    filter(opp150 -> opp150.getDelytelseId().equals(opp150Revurdering.getRefDelytelseId())).findFirst().orElse(null);
            if (originaltOpp150 != null) {
                var refusjonsinfo156Originalt = originaltOpp150.getRefusjonsinfo156();
                var refusjonsinfo156Revurdering = opp150Revurdering.getRefusjonsinfo156();
                assertThat(refusjonsinfo156Revurdering.getMaksDato()).isEqualTo(refusjonsinfo156Originalt.getMaksDato());
                assertThat(refusjonsinfo156Revurdering.getRefunderesId()).isEqualTo(refusjonsinfo156Originalt.getRefunderesId());
                assertThat(refusjonsinfo156Revurdering.getDatoFom()).isEqualTo(refusjonsinfo156Originalt.getDatoFom());
            }
        }
    }

    public static void verifiserOppdragslinje150ForHverKlassekode(Oppdragskontroll oppdragOriginalt, Oppdragskontroll oppdragRevurdering) {
        var originaltOppdr150ListeAT = getOppdragslinje150MedKlassekode(oppdragOriginalt, KodeKlassifik.FPF_ARBEIDSTAKER);
        var originaltOppdr150ListeFL = getOppdragslinje150MedKlassekode(oppdragOriginalt, KodeKlassifik.FPF_FRILANSER);
        var revurderingOppdr150ListeAT = getOppdragslinje150MedKlassekode(oppdragRevurdering, KodeKlassifik.FPF_ARBEIDSTAKER);
        var revurderingOppdr150ListeFL = getOppdragslinje150MedKlassekode(oppdragRevurdering, KodeKlassifik.FPF_FRILANSER);
        verifiserKjedingForOppdragslinje150(originaltOppdr150ListeAT, originaltOppdr150ListeFL);
        verifiserKjedingForOppdragslinje150(revurderingOppdr150ListeAT, revurderingOppdr150ListeFL);
    }

    public static void verifiserOpphørsdatoen(Oppdragskontroll originaltOppdrag, Oppdragskontroll oppdragRevurdering) {
        var originaltOppdr150Liste = originaltOppdrag.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream()).toList();
        var oppdr150OpphørtListe = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
                .stream()).filter(opp150 -> opp150.gjelderOpphør() && !opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_REFUSJON_AG)).toList();
        for (var opp150Opphørt : oppdr150OpphørtListe) {
            var klassekode = opp150Opphørt.getKodeKlassifik();
            var førsteDatoVedtakFom = originaltOppdr150Liste.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(klassekode)).min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom))
                    .map(Oppdragslinje150::getDatoVedtakFom).get();
            assertThat(opp150Opphørt.getDatoStatusFom()).isEqualTo(førsteDatoVedtakFom);
        }
    }

    public static LocalDate finnFørsteDatoVedtakFom(List<Oppdragslinje150> originaltOpp150Liste, Oppdragslinje150 originaltOpp150) {
        if (originaltOpp150.getOppdrag110().getKodeFagomrade().equals(KodeFagområde.FP)) {
            return originaltOpp150Liste.stream().filter(opp150 -> !opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_REFUSJON_AG)
                    && !opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG)).min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).map(Oppdragslinje150::getDatoVedtakFom).get();
        }
        var refunderesId = originaltOpp150.getRefusjonsinfo156().getRefunderesId();
        return originaltOpp150Liste.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_REFUSJON_AG))
                .filter(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId().equals(refunderesId)).min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).map(Oppdragslinje150::getDatoVedtakFom).get();
    }

    public static boolean opp150MedGradering(Oppdragslinje150 oppdragslinje150) {
        var erBrukerEllerVirksomhet = oppdragslinje150.getOppdrag110().getKodeFagomrade().equals(KodeFagområde.FP) ||
                oppdragslinje150.getRefusjonsinfo156().getRefunderesId().equals("00789123456");
        var gjelderFeriepenger = oppdragslinje150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER) ||
                oppdragslinje150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG);
        return erBrukerEllerVirksomhet && !gjelderFeriepenger;
    }


    public static void verifiserOppdr150SomErNy(List<Oppdragslinje150> opp150RevurdListe, List<Oppdragslinje150> originaltOpp150Liste, List<Integer> gradering) {
        var opp150NyList = opp150RevurdListe.stream()
                .filter(oppdr150 -> oppdr150.getKodeEndringLinje().equals(KodeEndringLinje.NY))
                .toList();

        assertThat(opp150NyList).isNotEmpty();
        for (var opp150Ny : opp150NyList) {
            assertThat(opp150Ny.getKodeStatusLinje()).isNull();
            assertThat(opp150Ny.getDatoStatusFom()).isNull();
            assertThat(originaltOpp150Liste).isNotEmpty().allMatch(opp150 -> !opp150.getDelytelseId().equals(opp150Ny.getDelytelseId()));
            if (opp150Ny.getRefDelytelseId() != null) {
                assertThat(opp150RevurdListe).anySatisfy(opp150 ->
                        assertThat(opp150.getDelytelseId()).isEqualTo(opp150Ny.getRefDelytelseId()));
                var oppdr150 = opp150RevurdListe.stream()
                        .filter(opp150 -> opp150.getDelytelseId().equals(opp150Ny.getRefDelytelseId()))
                        .findFirst()
                        .orElse(null);
                assertThat(oppdr150).isNotNull();
                assertThat(opp150Ny.getKodeKlassifik()).isEqualTo(oppdr150.getKodeKlassifik());
            }
            if (opp150Ny.getOppdrag110().getKodeFagomrade().equals(KodeFagområde.FPREF)) {
                assertThat(opp150Ny.getRefusjonsinfo156()).isNotNull();
            }
            if (opp150MedGradering(opp150Ny)) {
                assertThat(opp150Ny.getUtbetalingsgrad().getVerdi()).isIn(gradering);
            }
            if (!erOpp150ForFeriepenger(opp150Ny)) {
                assertThat(opp150Ny.getUtbetalingsgrad()).isNotNull();
            } else {
                assertThat(opp150Ny.getUtbetalingsgrad()).isNull();
                assertThat(opp150Ny.getRefFagsystemId()).isNull();
                assertThat(opp150Ny.getRefDelytelseId()).isNull();
            }
        }
    }

    public static void verifiserOppdr150SomErOpphørt(List<Oppdragslinje150> opp150RevurdListe, List<Oppdragslinje150> originaltOpp150Liste,
                                                     boolean medFeriePenger, boolean medFlereKlassekode, boolean opphFomEtterStp) {

        var opphørsdatoVerdierForFeriepenger = Arrays.asList(LocalDate.of(I_ÅR + 1, 5, 1), LocalDate.of(I_ÅR + 2, 5, 1));
        for (var opp150Revurd : opp150RevurdListe) {
            var originaltOpp150 = originaltOpp150Liste.stream()
                    .filter(oppdragslinje150 -> oppdragslinje150.getDelytelseId().equals(opp150Revurd.getDelytelseId()))
                    .findFirst().orElse(null);
            if (medFlereKlassekode) {
                var kodeKlassifikForrigeListe = getKodeklassifikIOppdr150Liste(originaltOpp150Liste);
                var kodeKlassifikRevurderingListe = getKodeklassifikKunForOpp150MedOpph(opp150RevurdListe);
                assertThat(kodeKlassifikRevurderingListe).containsAnyElementsOf(opphFomEtterStp ? Collections.singletonList(KodeKlassifik.FPF_REFUSJON_AG)
                        : kodeKlassifikForrigeListe);
            }
            if (originaltOpp150 != null) {
                verifiserOpphørForrigeOppdrag(originaltOpp150Liste, opphørsdatoVerdierForFeriepenger, opp150Revurd, originaltOpp150);
            }
        }
        if (medFeriePenger) {
            verifiserFeriepenger(opp150RevurdListe);
        }

        var originaltOppdrag = originaltOpp150Liste.get(0).getOppdrag110().getOppdragskontroll();
        var oppdrag110RevurderingList = originaltOppdrag.getOppdrag110Liste();
        verifiserGrad(opp150RevurdListe, originaltOppdrag);
        verifiserRefusjonInfo156(oppdrag110RevurderingList, originaltOppdrag);
    }

    public static List<KodeKlassifik> getKodeklassifikKunForOpp150MedOpph(List<Oppdragslinje150> opp150RevurdListe) {
        return opp150RevurdListe.stream()
                .filter(opp150 -> !opp150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER)
                        && !opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG))
                .filter(Oppdragslinje150::gjelderOpphør)
                .map(Oppdragslinje150::getKodeKlassifik)
                .distinct()
                .toList();
    }

    public static List<KodeKlassifik> getKodeklassifikIOppdr150Liste(List<Oppdragslinje150> originaltOpp150Liste) {
        return originaltOpp150Liste.stream()
                .filter(opp150 -> !opp150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER)
                        && !opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG))
                .map(Oppdragslinje150::getKodeKlassifik)
                .distinct()
                .toList();
    }

    /**
     * Verifiser at opphørslinje vi har laget for å opphøre forrige ytelse har samme verdier som korresponderende oppdraglinje i tidligere behandling.<br>
     * Forventer ulik
     * <ul>
     * <li>KodeStatusLinje. Denne skal være OPPH</li>
     * <li>KodeEndringLinje. Denne skal være ENDR</li>
     * </ul>
     *  @param originaltOpp150Liste
     *
     * @param opphørsdatoVerdierForFeriepenger
     * @param opp150Revurd
     * @param originaltOpp150
     */
    private static void verifiserOpphørForrigeOppdrag(List<Oppdragslinje150> originaltOpp150Liste, List<LocalDate> opphørsdatoVerdierForFeriepenger, Oppdragslinje150 opp150Revurd, Oppdragslinje150 originaltOpp150) {
        assertThat(opp150Revurd.getDelytelseId()).isEqualTo(originaltOpp150.getDelytelseId());
        assertThat(opp150Revurd.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.ENDR);
        assertThat(opp150Revurd.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPH);
        assertThat(opp150Revurd.getRefDelytelseId()).isNull();
        assertThat(opp150Revurd.getRefFagsystemId()).isNull();
        if (erOpp150ForFeriepenger(opp150Revurd)) {
            assertThat(opp150Revurd.getDatoStatusFom()).isIn(opphørsdatoVerdierForFeriepenger);
        } else {
            var førsteDatoVedtakFom = finnFørsteDatoVedtakFom(originaltOpp150Liste, originaltOpp150);
            assertThat(opp150Revurd.getDatoStatusFom()).isAfterOrEqualTo(førsteDatoVedtakFom);
        }
        assertThat(opp150Revurd.getSats()).isEqualTo(originaltOpp150.getSats());
        assertThat(opp150Revurd.getTypeSats()).isEqualTo(originaltOpp150.getTypeSats());
    }

    private static void verifiserFeriepenger(List<Oppdragslinje150> opp150RevurdListe) {
        assertThat(opp150RevurdListe).anySatisfy(opp150 ->
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));
        assertThat(opp150RevurdListe).anySatisfy(opp150 ->
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
        assertThat(opp150RevurdListe).anySatisfy(opp150 ->
                assertThat(opp150.getTypeSats()).isEqualTo(TypeSats.ENG));
        var opp150FeriepgBrukerList = opp150RevurdListe.stream().filter(o150 -> o150.getUtbetalesTilId() != null)
                .filter(opp150 -> opp150.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER))
                .filter(Oppdragslinje150::gjelderOpphør).toList();
        var opp150ArbeidsgiverList = opp150RevurdListe.stream()
                .filter(opp150 -> opp150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG)).toList();
        assertThat(opp150FeriepgBrukerList).anySatisfy(opp150 ->
                assertThat(opp150.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPH));
        assertThat(opp150ArbeidsgiverList).anySatisfy(opp150 ->
                assertThat(opp150.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPH));
    }
}
