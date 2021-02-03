package no.nav.foreldrepenger.økonomistøtte.dagytelse.fp;

import static no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragskontrollTjenesteTestBase.I_ÅR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;

class OppdragskontrollTestVerktøy {

    private OppdragskontrollTestVerktøy() {
    }

    static List<Oppdragslinje150> getOppdragslinje150MedKlassekode(Oppdragskontroll oppdrag, String klassekode) {
        List<Oppdragslinje150> alleOppdr150Liste = getOppdragslinje150Liste(oppdrag);
        return getOppdragslinje150MedKlassekode(alleOppdr150Liste, klassekode);
    }

    static List<Oppdragslinje150> getOppdragslinje150MedKlassekode(List<Oppdragslinje150> opp150Liste, String klassekode) {
        return opp150Liste.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(klassekode))
            .collect(Collectors.toList());
    }

    static Oppdrag110 getOppdrag110ForBruker(List<Oppdrag110> oppdrag110Liste) {
        return oppdrag110Liste.stream()
            .filter(oppdrag110 -> ØkonomiKodeFagområde.FP.name().equals(oppdrag110.getKodeFagomrade()))
            .findFirst().get();
    }

    static Oppdrag110 getOppdrag110ForArbeidsgiver(List<Oppdrag110> oppdrag110Liste, String virksomhetOrgnr) {
        return getOpp150ListeForEnVirksomhet(oppdrag110Liste, virksomhetOrgnr).get(0).getOppdrag110();
    }

    static Oppdragslinje150 getOpp150ForEnVirksomhet(List<Oppdragslinje150> oppdragslinje150Liste, String virksomhetOrgnr) {
        return oppdragslinje150Liste.stream()
            .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156().getRefunderesId().equals(endreTilElleveSiffer(virksomhetOrgnr)))
            .findFirst().get();
    }

    static List<Oppdragslinje150> getOpp150ListeForEnVirksomhet(List<Oppdrag110> oppdrag110Liste, String virksomhetOrgnr) {
        return oppdrag110Liste.stream()
            .filter(oppdrag110 -> ØkonomiKodeFagområde.FPREF.name().equals(oppdrag110.getKodeFagomrade()))
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156().getRefunderesId().equals(endreTilElleveSiffer(virksomhetOrgnr)))
            .collect(Collectors.toList());
    }

    static List<Oppdragslinje150> getOpp150ListeForBruker(List<Oppdrag110> oppdrag110Liste) {
        return oppdrag110Liste.stream()
            .filter(oppdrag110 -> ØkonomiKodeFagområde.FP.name().equals(oppdrag110.getKodeFagomrade()))
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
    }

    static void verifiserKjedingForOppdragslinje150(List<Oppdragslinje150> originaltOppdr150ListeAT, List<Oppdragslinje150> originaltOppdr150ListeFL) {
        for (Oppdragslinje150 opp150FL : originaltOppdr150ListeFL) {
            assertThat(originaltOppdr150ListeAT).allSatisfy(opp150AT -> {
                assertThat(opp150AT.getDelytelseId()).isNotEqualTo(opp150FL.getDelytelseId());
                assertThat(opp150AT.getRefDelytelseId()).isNotEqualTo(opp150FL.getDelytelseId());
                assertThat(opp150FL.getRefDelytelseId()).isNotEqualTo(opp150AT.getDelytelseId());
            });
        }
    }

    static void verifiserOppdr150SomAndelerSlåSammen(Oppdragskontroll originaltOppdrag, Oppdragskontroll revurderingOppdrag) {
        List<Oppdragslinje150> originaltoppdr150Liste = getOppdragslinje150Liste(originaltOppdrag);
        List<Oppdragslinje150> originaltOppdr150ListeBruker = originaltoppdr150Liste.stream().filter(opp150 -> opp150.getOppdrag110().getKodeFagomrade().equals(ØkonomiKodeFagområde.FP.name()))
            .filter(opp150 -> !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik())).collect(Collectors.toList());
        List<Oppdragslinje150> revurderingOppdr150Liste = getOppdragslinje150Liste(revurderingOppdrag);
        List<Oppdragslinje150> revurderingOppdr150ListeBruker = revurderingOppdr150Liste.stream().filter(opp150 -> opp150.getOppdrag110().getKodeFagomrade().equals(ØkonomiKodeFagområde.FP.name()))
            .filter(opp150 -> !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik())).filter(opp150 -> opp150.getKodeEndringLinje().equals(ØkonomiKodeEndringLinje.NY.name()))
            .filter(opp150 -> opp150.getDatoVedtakFom().equals(OppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(8))).collect(Collectors.toList());

        assertThat(originaltOppdr150ListeBruker).hasSize(1);
        assertThat(revurderingOppdr150ListeBruker).hasSize(1);
        assertThat(originaltOppdr150ListeBruker.get(0).getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        assertThat(revurderingOppdr150ListeBruker.get(0).getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        assertThat(originaltOppdr150ListeBruker.get(0).getSats()).isEqualTo(3000L);
        assertThat(revurderingOppdr150ListeBruker.get(0).getSats()).isEqualTo(3100L);
    }

    static void verifiserOppdr150MedNyKlassekode(List<Oppdragslinje150> opp150RevurdListe) {
        List<Oppdragslinje150> opp150Liste = opp150RevurdListe.stream()
            .filter(oppdr150 -> !oppdr150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()) && !oppdr150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()))
            .filter(oppdr150 -> oppdr150.getKodeEndringLinje().equals(ØkonomiKodeEndringLinje.NY.name())).collect(Collectors.toList());
        List<String> klasseKodeListe = opp150Liste.stream().map(Oppdragslinje150::getKodeKlassifik).distinct().collect(Collectors.toList());
        assertThat(klasseKodeListe).containsOnly(ØkonomiKodeKlassifik.FPATAL.getKodeKlassifik(), ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik());
        assertThat(opp150Liste).anySatisfy(opp150 -> assertThat(opp150.getRefDelytelseId()).isNull());
    }

    static void verifiserDelYtelseOgFagsystemIdForEnKlassekode(List<Oppdragslinje150> opp150RevurderingListe, List<Oppdragslinje150> opp150OriginalListe) {
        Oppdragslinje150 førsteOpp150IRevurderingKjede = opp150RevurderingListe.stream().min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).get();
        assertThat(førsteOpp150IRevurderingKjede.getRefDelytelseId()).isNotNull();
        assertThat(førsteOpp150IRevurderingKjede.getRefFagsystemId()).isNotNull();
        assertThat(opp150OriginalListe).anySatisfy(opp150Original -> assertThat(opp150Original.getDelytelseId()).isEqualTo(førsteOpp150IRevurderingKjede.getRefDelytelseId()));
    }

    static void verifiserDelYtelseOgFagsystemIdForFlereKlassekode(List<Oppdragslinje150> opp150RevurderingListe, List<Oppdragslinje150> opp150OriginalListe) {
        opp150OriginalListe.removeIf(opp150 -> opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik())
            || opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()));
        List<String> klassekodeListe = opp150OriginalListe.stream().map(Oppdragslinje150::getKodeKlassifik).distinct().collect(Collectors.toList());
        klassekodeListe.forEach(kode -> {
            if (opp150RevurderingListe.stream().anyMatch(opp150 -> opp150.getKodeKlassifik().equals(kode))) {
                Oppdragslinje150 førsteOpp150IKjede = opp150RevurderingListe.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(kode))
                    .min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).get();
                assertThat(opp150OriginalListe).anySatisfy(opp150 -> assertThat(opp150.getDelytelseId()).isEqualTo(førsteOpp150IKjede.getRefDelytelseId()));
            }
        });
    }

    static List<Oppdragslinje150> getOppdragslinje150Liste(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
    }

    static boolean erOpp150ForFeriepenger(Oppdragslinje150 opp150) {
        return opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()) ||
            opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik());
    }

    static String endreTilElleveSiffer(String id) {
        if (id.length() == 11) {
            return id;
        } else {
            return "00" + id;
        }
    }

    static void verifiserAvstemming(Oppdragskontroll oppdragRevurdering) {
        List<Avstemming> avstemmingRevurdList = oppdragRevurdering.getOppdrag110Liste().stream()
            .map(Oppdrag110::getAvstemming).collect(Collectors.toList());

        assertThat(avstemmingRevurdList).isNotEmpty();
        assertThat(avstemmingRevurdList).hasSameSizeAs(oppdragRevurdering.getOppdrag110Liste());
        for (Avstemming avstemmingRevurd : avstemmingRevurdList) {
            assertThat(avstemmingRevurd.getKodekomponent()).isEqualTo(ØkonomiKodekomponent.VLFP.getKodekomponent());
        }
    }

    static void verifiserGrad170(List<Oppdragslinje150> opp150RevurderingList, Oppdragskontroll originaltOppdrag) {
        List<Oppdragslinje150> originaltOpp150Liste = getOppdragslinje150Liste(originaltOppdrag);

        for (Oppdragslinje150 opp150Revurdering : opp150RevurderingList) {
            if (!erOpp150ForFeriepenger(opp150Revurdering)) {
                assertThat(opp150Revurdering.getGrad170Liste()).isNotNull();
                assertThat(opp150Revurdering.getGrad170Liste()).isNotEmpty();
            } else {
                assertThat(opp150Revurdering.getGrad170Liste()).isEmpty();
            }
            Oppdragslinje150 originaltOpp150 = originaltOpp150Liste.stream().
                filter(opp150 -> opp150.getDelytelseId().equals(opp150Revurdering.getDelytelseId())).findFirst().orElse(null);
            if (originaltOpp150 != null && !erOpp150ForFeriepenger(originaltOpp150)) {
                Grad170 grad170Revurdering = opp150Revurdering.getGrad170Liste().get(0);
                Grad170 grad170Originalt = originaltOpp150.getGrad170Liste().get(0);
                assertThat(grad170Revurdering.getTypeGrad()).isEqualTo(grad170Originalt.getTypeGrad());
                assertThat(grad170Revurdering.getGrad()).isEqualTo(grad170Originalt.getGrad());
            }
        }
    }

    static void verifiserAttestant180(List<Oppdragslinje150> opp150List) {
        assertThat(opp150List).allSatisfy(opp150 ->
            assertThat(opp150.getAttestant180Liste()).isNotEmpty());
    }

    static void verifiserRefusjonInfo156(List<Oppdrag110> opp110RevurderingList, Oppdragskontroll originaltOppdrag) {

        List<Oppdragslinje150> opp150RevurderingList = opp110RevurderingList.stream().filter(opp110 -> opp110.getKodeFagomrade().equals(ØkonomiKodeFagområde.FPREF.name()))
            .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        List<Oppdragslinje150> originaltOpp150Liste = getOppdragslinje150Liste(originaltOppdrag);

        for (Oppdragslinje150 opp150Revurdering : opp150RevurderingList) {
            Oppdragslinje150 originaltOpp150 = originaltOpp150Liste.stream().
                filter(opp150 -> opp150.getDelytelseId().equals(opp150Revurdering.getRefDelytelseId())).findFirst().orElse(null);
            if (originaltOpp150 != null) {
                Refusjonsinfo156 refusjonsinfo156Originalt = originaltOpp150.getRefusjonsinfo156();
                Refusjonsinfo156 refusjonsinfo156Revurdering = opp150Revurdering.getRefusjonsinfo156();
                assertThat(refusjonsinfo156Revurdering.getMaksDato()).isEqualTo(refusjonsinfo156Originalt.getMaksDato());
                assertThat(refusjonsinfo156Revurdering.getRefunderesId()).isEqualTo(refusjonsinfo156Originalt.getRefunderesId());
                assertThat(refusjonsinfo156Revurdering.getDatoFom()).isEqualTo(refusjonsinfo156Originalt.getDatoFom());
            }
        }
    }

    static void verifiserOppdragslinje150ForHverKlassekode(Oppdragskontroll oppdragOriginalt, Oppdragskontroll oppdragRevurdering) {
        List<Oppdragslinje150> originaltOppdr150ListeAT = getOppdragslinje150MedKlassekode(oppdragOriginalt, ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        List<Oppdragslinje150> originaltOppdr150ListeFL = getOppdragslinje150MedKlassekode(oppdragOriginalt, ØkonomiKodeKlassifik.FPATFRI.getKodeKlassifik());
        List<Oppdragslinje150> revurderingOppdr150ListeAT = getOppdragslinje150MedKlassekode(oppdragRevurdering, ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        List<Oppdragslinje150> revurderingOppdr150ListeFL = getOppdragslinje150MedKlassekode(oppdragRevurdering, ØkonomiKodeKlassifik.FPATFRI.getKodeKlassifik());
        verifiserKjedingForOppdragslinje150(originaltOppdr150ListeAT, originaltOppdr150ListeFL);
        verifiserKjedingForOppdragslinje150(revurderingOppdr150ListeAT, revurderingOppdr150ListeFL);
    }

    static void verifiserOpphørsdatoen(Oppdragskontroll originaltOppdrag, Oppdragskontroll oppdragRevurdering) {
        List<Oppdragslinje150> originaltOppdr150Liste = originaltOppdrag.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream()).collect(Collectors.toList());
        List<Oppdragslinje150> oppdr150OpphørtListe = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(opp150 -> opp150.gjelderOpphør() && !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik())).collect(Collectors.toList());
        for (Oppdragslinje150 opp150Opphørt : oppdr150OpphørtListe) {
            String klassekode = opp150Opphørt.getKodeKlassifik();
            LocalDate førsteDatoVedtakFom = originaltOppdr150Liste.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(klassekode)).min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom))
                .map(Oppdragslinje150::getDatoVedtakFom).get();
            assertThat(opp150Opphørt.getDatoStatusFom()).isEqualTo(førsteDatoVedtakFom);
        }
    }

    static LocalDate finnFørsteDatoVedtakFom(List<Oppdragslinje150> originaltOpp150Liste, Oppdragslinje150 originaltOpp150) {
        if (originaltOpp150.getOppdrag110().getKodeFagomrade().equals(ØkonomiKodeFagområde.FP.name())) {
            return originaltOpp150Liste.stream().filter(opp150 -> !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik())
                && !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik())).min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).map(Oppdragslinje150::getDatoVedtakFom).get();
        } else {
            String refunderesId = originaltOpp150.getRefusjonsinfo156().getRefunderesId();
            return originaltOpp150Liste.stream().filter(opp150 -> opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik()))
                .filter(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId().equals(refunderesId)).min(Comparator.comparing(Oppdragslinje150::getDatoVedtakFom)).map(Oppdragslinje150::getDatoVedtakFom).get();
        }
    }

    static boolean opp150MedGradering(Oppdragslinje150 oppdragslinje150) {
        boolean erBrukerEllerVirksomhet = oppdragslinje150.getOppdrag110().getKodeFagomrade().equals(ØkonomiKodeFagområde.FP.name()) ||
            oppdragslinje150.getRefusjonsinfo156().getRefunderesId().equals("00789123456");
        boolean gjelderFeriepenger = oppdragslinje150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()) ||
            oppdragslinje150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik());
        return erBrukerEllerVirksomhet && !gjelderFeriepenger;
    }


    protected static void verifiserOppdr150SomErNy(List<Oppdragslinje150> opp150RevurdListe, List<Oppdragslinje150> originaltOpp150Liste, int gradering) {
        List<Oppdragslinje150> opp150NyList = opp150RevurdListe.stream()
            .filter(oppdr150 -> oppdr150.getKodeEndringLinje().equals(ØkonomiKodeEndringLinje.NY.name()))
            .collect(Collectors.toList());

        List<Oppdragslinje150> opp150List = new ArrayList<>();
        assertThat(opp150NyList).isNotEmpty();
        for (Oppdragslinje150 opp150Ny : opp150NyList) {
            assertThat(opp150Ny.getKodeStatusLinje()).isNull();
            assertThat(opp150Ny.getDatoStatusFom()).isNull();
            assertThat(originaltOpp150Liste).allMatch(opp150 -> !opp150.getDelytelseId().equals(opp150Ny.getDelytelseId()));
            if (opp150Ny.getRefDelytelseId() != null) {
                assertThat(opp150RevurdListe).anySatisfy(opp150 ->
                    assertThat(opp150.getDelytelseId()).isEqualTo(opp150Ny.getRefDelytelseId()));
                Oppdragslinje150 oppdr150 = opp150RevurdListe.stream()
                    .filter(opp150 -> opp150.getDelytelseId().equals(opp150Ny.getRefDelytelseId()))
                    .findFirst()
                    .orElse(null);
                assertThat(oppdr150).isNotNull();
                assertThat(opp150Ny.getKodeKlassifik()).isEqualTo(oppdr150.getKodeKlassifik());
                opp150List.add(oppdr150);
            }
            if (opp150Ny.getOppdrag110().getKodeFagomrade().equals(ØkonomiKodeFagområde.FPREF.name())) {
                assertThat(opp150Ny.getRefusjonsinfo156()).isNotNull();
            }
            if (opp150MedGradering(opp150Ny)) {
                assertThat(opp150Ny.getGrad170Liste().get(0).getGrad()).isEqualTo(gradering);
            }
            if (!erOpp150ForFeriepenger(opp150Ny)) {
                assertThat(opp150Ny.getGrad170Liste()).isNotEmpty();
                assertThat(opp150Ny.getGrad170Liste()).isNotNull();
            } else {
                assertThat(opp150Ny.getGrad170Liste()).isEmpty();
                assertThat(opp150Ny.getRefFagsystemId()).isNull();
                assertThat(opp150Ny.getRefDelytelseId()).isNull();
            }
        }
        List<String> statusList = opp150List.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .map(Oppdragslinje150::getKodeStatusLinje)
            .collect(Collectors.toList());
        assertThat(statusList).containsOnly(ØkonomiKodeStatusLinje.OPPH.name());
    }

    protected static void verifiserOppdr150SomErOpphørt(List<Oppdragslinje150> opp150RevurdListe, List<Oppdragslinje150> originaltOpp150Liste, LocalDate endringsdato,
                                                        boolean medFeriePenger, boolean medFlereKlassekode, boolean opphFomEtterStp) {

        List<LocalDate> opphørsdatoVerdierForFeriepenger = Arrays.asList(LocalDate.of(I_ÅR + 1, 5, 1), LocalDate.of(I_ÅR + 2, 5, 1));
        for (Oppdragslinje150 opp150Revurd : opp150RevurdListe) {
            Oppdragslinje150 originaltOpp150 = originaltOpp150Liste.stream()
                .filter(oppdragslinje150 -> oppdragslinje150.getDelytelseId().equals(opp150Revurd.getDelytelseId()))
                .findFirst().orElse(null);
            if (medFlereKlassekode) {
                List<String> kodeKlassifikForrigeListe = getKodeklassifikIOppdr150Liste(originaltOpp150Liste);
                List<String> kodeKlassifikRevurderingListe = getKodeklassifikKunForOpp150MedOpph(opp150RevurdListe);
                assertThat(kodeKlassifikRevurderingListe).containsOnlyElementsOf(opphFomEtterStp ? Collections.singletonList(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik())
                    : kodeKlassifikForrigeListe);
            }
            if (originaltOpp150 != null) {
                verifiserOpphørForrigeOppdrag(originaltOpp150Liste, endringsdato, opphørsdatoVerdierForFeriepenger, opp150Revurd, originaltOpp150);
            }
        }
        if (medFeriePenger) {
            verifiserFeriepenger(opp150RevurdListe);
        }

        Oppdragskontroll originaltOppdrag = originaltOpp150Liste.get(0).getOppdrag110().getOppdragskontroll();
        List<Oppdrag110> oppdrag110RevurderingList = originaltOppdrag.getOppdrag110Liste();
        verifiserGrad170(opp150RevurdListe, originaltOppdrag);
        verifiserRefusjonInfo156(oppdrag110RevurderingList, originaltOppdrag);
    }

    static List<String> getKodeklassifikKunForOpp150MedOpph(List<Oppdragslinje150> opp150RevurdListe) {
        return opp150RevurdListe.stream()
            .filter(opp150 -> !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik())
                && !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()))
            .filter(Oppdragslinje150::gjelderOpphør)
            .map(Oppdragslinje150::getKodeKlassifik)
            .distinct()
            .collect(Collectors.toList());
    }

    static List<String> getKodeklassifikIOppdr150Liste(List<Oppdragslinje150> originaltOpp150Liste) {
        return originaltOpp150Liste.stream()
            .filter(opp150 -> !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik())
                && !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()))
            .map(Oppdragslinje150::getKodeKlassifik)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Verifiser at opphørslinje vi har laget for å opphøre forrige ytelse har samme verdier som korresponderende oppdraglinje i tidligere behandling.<br>
     * Forventer ulik
     * <ul>
     * <li>KodeStatusLinje. Denne skal være OPPH</li>
     * <li>KodeEndringLinje. Denne skal være ENDR</li>
     * </ul>
     *
     * @param originaltOpp150Liste
     * @param endringsdato
     * @param opphørsdatoVerdierForFeriepenger
     * @param opp150Revurd
     * @param originaltOpp150
     */
    private static void verifiserOpphørForrigeOppdrag(List<Oppdragslinje150> originaltOpp150Liste, LocalDate endringsdato, List<LocalDate> opphørsdatoVerdierForFeriepenger, Oppdragslinje150 opp150Revurd, Oppdragslinje150 originaltOpp150) {
        assertThat(opp150Revurd.getDelytelseId()).isEqualTo(originaltOpp150.getDelytelseId());
        assertThat(opp150Revurd.getKodeEndringLinje()).isEqualTo(ØkonomiKodeEndringLinje.ENDR.name());
        assertThat(opp150Revurd.getKodeStatusLinje()).isEqualTo(ØkonomiKodeStatusLinje.OPPH.name());
        assertThat(opp150Revurd.getRefDelytelseId()).isNull();
        assertThat(opp150Revurd.getRefFagsystemId()).isNull();
        if (erOpp150ForFeriepenger(opp150Revurd)) {
            assertThat(opp150Revurd.getDatoStatusFom()).isIn(opphørsdatoVerdierForFeriepenger);
        } else {
            LocalDate førsteDatoVedtakFom = finnFørsteDatoVedtakFom(originaltOpp150Liste, originaltOpp150);
            LocalDate datoStatusFom = førsteDatoVedtakFom.isAfter(endringsdato) ? førsteDatoVedtakFom : endringsdato;
            assertThat(opp150Revurd.getDatoStatusFom()).isEqualTo(datoStatusFom);
        }
        assertThat(opp150Revurd.getSats()).isEqualTo(originaltOpp150.getSats());
        assertThat(opp150Revurd.getTypeSats()).isEqualTo(originaltOpp150.getTypeSats());
        assertThat(opp150Revurd.getBrukKjoreplan()).isEqualTo(originaltOpp150.getBrukKjoreplan());
    }

    private static void verifiserFeriepenger(List<Oppdragslinje150> opp150RevurdListe) {
        assertThat(opp150RevurdListe).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()));
        assertThat(opp150RevurdListe).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()));
        assertThat(opp150RevurdListe).anySatisfy(opp150 ->
            assertThat(opp150.getTypeSats()).isEqualTo(OppdragskontrollTjenesteTestBase.TYPE_SATS_FP_FERIEPG));
        List<Oppdragslinje150> opp150FeriepgBrukerList = opp150RevurdListe.stream().filter(o150 -> o150.getUtbetalesTilId() != null)
            .filter(opp150 -> opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()))
            .filter(Oppdragslinje150::gjelderOpphør).collect(Collectors.toList());
        List<Oppdragslinje150> opp150ArbeidsgiverList = opp150RevurdListe.stream()
            .filter(opp150 -> opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik())).collect(Collectors.toList());
        assertThat(opp150FeriepgBrukerList).anySatisfy(opp150 ->
            assertThat(opp150.getKodeStatusLinje()).isEqualTo(ØkonomiKodeStatusLinje.OPPH.name()));
        assertThat(opp150ArbeidsgiverList).anySatisfy(opp150 ->
            assertThat(opp150.getKodeStatusLinje()).isEqualTo(ØkonomiKodeStatusLinje.OPPH.name()));
    }
}
