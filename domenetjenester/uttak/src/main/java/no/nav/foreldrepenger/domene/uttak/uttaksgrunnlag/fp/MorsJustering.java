package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.erHelg;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilFredag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilMandag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.erHullMellom;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.kopier;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.sorterEtterFom;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager.beregnAntallVirkedager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.PerioderUtenHelgUtil;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;

class MorsJustering implements ForelderFødselJustering {

    private final LocalDate gammelFamiliehendelse;
    private final LocalDate nyFamiliehendelse;

    MorsJustering(LocalDate gammelFamiliehendelse, LocalDate nyFamiliehendelse) {
        this.gammelFamiliehendelse = gammelFamiliehendelse;
        this.nyFamiliehendelse = nyFamiliehendelse;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        oppgittePerioder = fyllHull(oppgittePerioder);
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        var virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);
        List<OppgittPeriodeEntitet> justertePerioder = new ArrayList<>();
        for (var oppgittPeriode : oppgittePerioder) {
            if (virkedagerSomSkalSkyves > 0) {
                var justert = flyttPeriodeTilHøyre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
                virkedagerSomSkalSkyves -= antallDagerJustertInnIOppholdSomErForbeholdtMorEtterFødsel(oppgittPeriode, ikkeFlyttbarePerioder);

                if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FELLESPERIODE) && kanFellesperiodenReduseres(oppgittPeriode, justertePerioder)) {
                    var justertFellesperiode = reduserJusterteFellesperioder(justert, virkedagerSomSkalSkyves);
                    virkedagerSomSkalSkyves -= antallDagerFellesperiodeErRedusert(justert, justertFellesperiode);
                    justert = justertFellesperiode;
                }

                justertePerioder.addAll(justert);
            } else {
                justertePerioder.add(oppgittPeriode);
            }

        }
        justertePerioder = new ArrayList<>(sorterEtterFom(justertePerioder));
        var beholdtPerioder = beholdPerioderFraOpprinneligUttaksdato(oppgittePerioder, justertePerioder);
        justertePerioder.addAll(beholdtPerioder);
        justertePerioder = fjernPerioderEtterSisteSøkteDato(sorterEtterFom(justertePerioder), oppgittePerioder.getLast().getTom());
        return fjernHullPerioder(justertePerioder);
    }

    private boolean kanFellesperiodenReduseres(OppgittPeriodeEntitet oppgittPeriode, List<OppgittPeriodeEntitet> justertePerioder) {
        return erPeriodeFlyttbar(oppgittPeriode) && justertePerioder.stream()
            .anyMatch(p -> p.getTom().isAfter(TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse)));
    }

    private int antallDagerJustertInnIOppholdSomErForbeholdtMorEtterFødsel(OppgittPeriodeEntitet oppgittPeriode, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        return oppgittPeriodeSomOverlapperMedHullForbeholdMorEtterFødsel(oppgittPeriode, ikkeFlyttbarePerioder).stream()
            .mapToInt(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()))
            .sum();
    }

    private static int antallDagerFellesperiodeErRedusert(List<OppgittPeriodeEntitet> justert, List<OppgittPeriodeEntitet> justertOgRedusert) {
        var antallVirkedagerEtterJustering = justert.stream().mapToInt(p -> beregnAntallVirkedager(p.getFom(), p.getTom())).sum();
        var antallVirkedagerEtterReduksjon = justertOgRedusert.stream().mapToInt(p -> beregnAntallVirkedager(p.getFom(), p.getTom())).sum();
        return antallVirkedagerEtterJustering - antallVirkedagerEtterReduksjon;
    }


    /**
     * Metoden tar inn fellesperiode som er justert i forkant til en eller flere justerte fellesperioden (f.eks. på grunn av hull)
     * Her går vi fra høyre til venstre og reduserer med {@param antallDagerSomKanReduseres} så langt det lar seg gjøre.
     * *
     * Eksempeler: Fellesperiode som havner i periode forbeholdt mor: x, antallDagerSomKanReduseres: 4
     * *
     * Case 1: Klarte å redusere 3 virkedager
     *  input:  xxx-  --
     *  output: xxx
     *  antallDagerSomKanReduseres_rest: 1
     *  *
     * Case 2: Fjerne siste og redusere andre
     *  input:  ---  --
     *  output: -
     *  antallDagerSomKanReduseres_rest: 0
     *  *
     * Case 3: Kan ikke redusere noe
     *  input:  xxx
     *  output: xxx
     *  antallDagerSomKanReduseres_rest: 4
     */
    private List<OppgittPeriodeEntitet> reduserJusterteFellesperioder(List<OppgittPeriodeEntitet> justerteFellesperioder, int antallDagerSomKanReduseres) {
        var periodeSomKanReduseres = new LocalDateTimeline<>(TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse).plusDays(1), Tid.TIDENES_ENDE, null);
        var fellesperiodene = new LocalDateTimeline<>(justerteFellesperioder.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p))
            .toList());
        var justertFellsperiodeSomKanReduseres = fellesperiodene.intersection(periodeSomKanReduseres).stream()
            .map(LocalDateSegment::getValue)
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .toList();

        if (justertFellsperiodeSomKanReduseres.isEmpty()) { // Fellesperioden kan ikke reduseres (ingen del av fellesperioden er etter perioden forbeholdt mor etter fødsel)
            return justerteFellesperioder;
        }

        // Starter fra siste til første periode. Reduseringen skjer fra høyre til venstre og har 3 utfall:
        //  1) Antall dager som kan reduseres er 0 (behold perioden slik det var)
        //  2) Deler av perioden forsvinner pga redusering (ny tom dato)
        //  3) Hele perioden forsvinner pga redusering (fjerne hele)
        List<OppgittPeriodeEntitet> justertFellesperiode = new ArrayList<>();
        for (var periode : justertFellsperiodeSomKanReduseres.reversed()) {
            var antallVirkedagerForPeriode = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
            if (antallDagerSomKanReduseres == 0) {  // 1) Antall dager som kan reduseres er 0 (behold perioden slik det var)
                justertFellesperiode.add(periode);
            } else if (antallVirkedagerForPeriode > antallDagerSomKanReduseres) { // 2) Deler av perioden forsvinner pga redusering (ny tom dato)
                var nyTom = Virkedager.plusVirkedager(periode.getFom(), antallVirkedagerForPeriode - antallDagerSomKanReduseres - 1); // -1 fordi virkedager periode.getFom() ikke er telt med
                justertFellesperiode.add(kopier(periode, periode.getFom(), nyTom));
                antallDagerSomKanReduseres = 0;
            } else { // 3) Hele perioden forsvinner pga redusering (fjerne hele)
                antallDagerSomKanReduseres -= antallVirkedagerForPeriode;
            }
        }

        var restenAvPeriodene = fellesperiodene.disjoint(periodeSomKanReduseres).stream()
            .map(LocalDateSegment::getValue)
            .toList();
        justertFellesperiode.addAll(restenAvPeriodene);
        justertFellesperiode.sort(Comparator.comparing(OppgittPeriodeEntitet::getFom));
        return justertFellesperiode;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        oppgittePerioder = fyllHull(oppgittePerioder);
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        var virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);

        List<OppgittPeriodeEntitet> justertePerioder = new ArrayList<>();
        for (var oppgittPeriode : oppgittePerioder) {
            var justert = flyttPeriodeTilVenstre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            justertePerioder.addAll(justert);
        }
        var sortert = sorterEtterFom(justertePerioder);
        if (søktOmPerioderEtterFamiliehendelse(oppgittePerioder, gammelFamiliehendelse)) {
            sortert = beholdSluttdatoForUttak(sortert, oppgittePerioder.getLast().getTom());
        }
        sortert = fyllHullSkaptAvIkkeFlyttbarePerioder(sortert, oppgittePerioder);
        return fjernHullPerioder(sortert);
    }

    private static List<OppgittPeriodeEntitet> fjernHullPerioder(List<OppgittPeriodeEntitet> oppgittPerioder) {
        return oppgittPerioder.stream().filter(p -> !(p instanceof JusterPeriodeHull)).toList();
    }

    private List<OppgittPeriodeEntitet> fyllHull(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var hull = hullPerioder(oppgittePerioder);
        return sorterEtterFom(Stream.concat(oppgittePerioder.stream(), hull.stream()).toList());
    }

    private List<OppgittPeriodeEntitet> beholdPerioderFraOpprinneligUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                               List<OppgittPeriodeEntitet> justertePerioder) {
        var oppgittTimeline = new LocalDateTimeline<>(
            fjernHullPerioder(oppgittePerioder).stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());
        var justertTimeline = new LocalDateTimeline<>(
            fjernHullPerioder(justertePerioder).stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());

        return oppgittTimeline.disjoint(justertTimeline)
            .stream()
            .filter(p -> beregnAntallVirkedager(p.getFom(), p.getTom()) > 0)
            .map(s -> OppgittPeriodeBuilder.ny()
                .medPeriodeType(finnUttakPeriodeType(justertePerioder))
                .medPeriode(flyttFraHelgTilMandag(s.getFom()), flyttFraHelgTilFredag(s.getTom()))
                .medMottattDato(s.getValue().getMottattDato())
                .medTidligstMottattDato(s.getValue().getTidligstMottattDato().orElse(null))
                .build())
            .toList();
    }

    private UttakPeriodeType finnUttakPeriodeType(List<OppgittPeriodeEntitet> oppgittPerioder) {
        for (var oppgittPeriode : oppgittPerioder) {
            if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FORELDREPENGER)) {
                return UttakPeriodeType.FORELDREPENGER;
            }
        }
        return UttakPeriodeType.FELLESPERIODE;
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeTilHøyre(OppgittPeriodeEntitet oppgittPeriode,
                                                             int antallVirkedagerSomSkalSkyves,
                                                             List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        if (!erPeriodeFlyttbar(oppgittPeriode) || antallVirkedagerSomSkalSkyves <= 0) {
            return Collections.singletonList(kopier(oppgittPeriode, oppgittPeriode.getFom(), oppgittPeriode.getTom()));
        }

        var nyFom = finnNyFomVedFlyttingTilHøyre(oppgittPeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
        var nyTom = nyFom;
        var i = 0;
        var knekkFunnet = false;
        var antallVirkedager = Virkedager.beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());

        //flytter en og en dag
        while (i < antallVirkedager - 1 && !knekkFunnet) {
            var nyTomPlus1Dag = Virkedager.plusVirkedager(nyTom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyTomPlus1Dag) || nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, nyTomPlus1Dag)) {
                nyTom = nyTomPlus1Dag;
                i++;
            } else {
                knekkFunnet = true;
            }
        }

        var nyPeriode = kopier(oppgittPeriode, nyFom, nyTom);
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        resultat.add(nyPeriode);
        if (knekkFunnet) {
            var virkedagerPeriodeFørKnekk = Virkedager.beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom());
            var fom = Virkedager.plusVirkedager(oppgittPeriode.getFom(), virkedagerPeriodeFørKnekk);
            var perioderEtterKnekk = flyttPeriodeTilHøyre(kopier(oppgittPeriode, fom, oppgittPeriode.getTom()), antallVirkedagerSomSkalSkyves,
                ikkeFlyttbarePerioder);
            resultat.addAll(perioderEtterKnekk);
        }

        return resultat;
    }

    private LocalDate finnNyFomVedFlyttingTilHøyre(OppgittPeriodeEntitet oppgittPeriode,
                                                   int antallVirkedagerSomSkalSkyves,
                                                   List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var nyFom = oppgittPeriode.getFom();
        var i = 0;
        //flytter en og en dag
        while (i < antallVirkedagerSomSkalSkyves) {
            nyFom = Virkedager.plusVirkedager(nyFom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom) || nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, nyFom)) {
                i++;
            }
        }
        return nyFom;
    }

    private List<OppgittPeriodeEntitet> fyllHullSkaptAvIkkeFlyttbarePerioder(List<OppgittPeriodeEntitet> justertePerioder,
                                                                             List<OppgittPeriodeEntitet> oppgittePerioder) {
        if (justertePerioder.isEmpty()) {
            throw new IllegalStateException("Må ha minst en justert periode");
        }
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();

        for (var i = 0; i < justertePerioder.size() - 1; i++) {
            resultat.add(justertePerioder.get(i));
            if (erHullMellom(justertePerioder.get(i).getTom(), justertePerioder.get(i + 1).getFom())) {
                var førsteVirkedagIHull = Virkedager.plusVirkedager(justertePerioder.get(i).getTom(), 1);
                var sisteVirkedagIHull = minusVirkedag(justertePerioder.get(i + 1).getFom());
                //sjekker om hullet vi finner ikke finnes i oppgitteperioder (hvis det er søkt med hull i perioden skal vi ikke fylle)
                if (!hullFinnesIOppgittePeriode(oppgittePerioder, førsteVirkedagIHull, sisteVirkedagIHull)) {
                    var sisteFlyttbarPeriode = sisteFlyttbarePeriode(resultat);
                    resultat.add(kopier(sisteFlyttbarPeriode, førsteVirkedagIHull, sisteVirkedagIHull));
                }
            }
        }
        resultat.add(justertePerioder.getLast());
        return resultat;
    }

    private OppgittPeriodeEntitet sisteFlyttbarePeriode(List<OppgittPeriodeEntitet> resultat) {
        var flyttbarePerioder = flyttbarePerioder(resultat);
        if (flyttbarePerioder.isEmpty()) {
            throw new IllegalStateException("Forventer minst en flyttbar periode før hull");
        }
        return flyttbarePerioder.getLast();
    }

    private boolean hullFinnesIOppgittePeriode(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate hullFom, LocalDate hullTom) {
        var perioderMedHull = hullPerioder(oppgittePerioder);
        return perioderMedHull.stream().anyMatch(p -> overlapper(p, hullFom) || overlapper(p, hullTom));
    }

    private List<OppgittPeriodeEntitet> flyttbarePerioder(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().filter(this::erPeriodeFlyttbar).toList();
    }

    private int beregnAntallLedigeVirkedager(LocalDate dato1, LocalDate dato2, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        LocalDate fraDato;
        LocalDate tilDato;
        if (dato1.isBefore(dato2)) {
            fraDato = dato1;
            tilDato = dato2;
        } else {
            fraDato = dato2;
            tilDato = dato1;
        }
        //teller antall virkedager som ikke ligger i ikkeflyttbare perioder
        var ledigeVirkedager = 0;
        while (fraDato.isBefore(tilDato)) {
            if (erLedigVirkedager(ikkeFlyttbarePerioder, fraDato)) {
                ledigeVirkedager++;
            }
            fraDato = Virkedager.plusVirkedager(fraDato, 1);
        }
        return ledigeVirkedager;
    }

    private boolean søktOmPerioderEtterFamiliehendelse(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse) {
        return oppgittePerioder.getLast().getTom().isAfter(gammelFamiliehendelse);
    }

    private List<OppgittPeriodeEntitet> beholdSluttdatoForUttak(List<OppgittPeriodeEntitet> justertePerioder, LocalDate sluttdato) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>(justertePerioder);
        var sisteJustertePeriode = justertePerioder.getLast();
        if (erPeriodeFlyttbar(sisteJustertePeriode)) {
            resultat.set(justertePerioder.size() - 1, kopier(sisteJustertePeriode, sisteJustertePeriode.getFom(), sluttdato));
        } else if (!sisteJustertePeriode.getTom().isEqual(sluttdato)) {
            var sisteFlyttbarePeriode = sisteFlyttbarePeriode(justertePerioder);
            resultat.add(kopier(sisteFlyttbarePeriode, Virkedager.plusVirkedager(sisteJustertePeriode.getTom(), 1), sluttdato));
        }
        return resultat;
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeTilVenstre(OppgittPeriodeEntitet oppgittPeriode,
                                                               int antallVirkedagerSomSkalSkyves,
                                                               List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        if (!erPeriodeFlyttbar(oppgittPeriode)) {
            return List.of(kopier(oppgittPeriode, oppgittPeriode.getFom(), oppgittPeriode.getTom()));
        }
        if (oppgittPeriode.getFom().isBefore(nyFamiliehendelse)) {
            var nyTom = oppgittPeriode.getTom().isBefore(nyFamiliehendelse) ? oppgittPeriode.getTom() : PerioderUtenHelgUtil.justerTomFredag(nyFamiliehendelse.minusDays(1));
            //Forkortes av fødsel
            return List.of(kopier(oppgittPeriode, oppgittPeriode.getFom(), nyTom));
        }
        if (oppgittPeriode.getFom().isBefore(gammelFamiliehendelse) && !oppgittPeriode.getFom().isBefore(nyFamiliehendelse)) {
            //Hele perioden blir borte. Ligger i mellom termin og fødsel
            return List.of();
        }

        var nyFom = finnNyFomVedFlyttingTilVenstre(oppgittPeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);

        var nyTom = nyFom;
        var i = 0;
        var knekkFunnet = false;
        var antallVirkedager = beregnAntallLedigeVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom(), ikkeFlyttbarePerioder);
        //flytter en og en dag
        while (i < antallVirkedager && !knekkFunnet) {
            if (erLedigVirkedager(ikkeFlyttbarePerioder, Virkedager.plusVirkedager(nyTom, 1))) {
                nyTom = Virkedager.plusVirkedager(nyTom, 1);
                i++;
            } else {
                knekkFunnet = true;
            }
        }

        var nyPeriode = kopier(oppgittPeriode, nyFom, nyTom);
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        resultat.add(nyPeriode);
        if (knekkFunnet) {
            var virkedagerPeriodeFørKnekk = beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom());
            var fom = Virkedager.plusVirkedager(oppgittPeriode.getFom(), virkedagerPeriodeFørKnekk);
            var perioderEtterKnekk = flyttPeriodeTilVenstre(kopier(oppgittPeriode, fom, oppgittPeriode.getTom()), antallVirkedagerSomSkalSkyves,
                ikkeFlyttbarePerioder);
            resultat.addAll(perioderEtterKnekk);
        }

        return resultat;
    }

    private LocalDate finnNyFomVedFlyttingTilVenstre(OppgittPeriodeEntitet oppgittPeriode,
                                                     int antallVirkedagerSomSkalSkyves,
                                                     List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var nyFom = oppgittPeriode.getFom();
        var i = 0;
        //flytter en og en dag
        while (i < antallVirkedagerSomSkalSkyves) {
            nyFom = minusVirkedag(nyFom);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom)) {
                i++;
            }
        }
        return nyFom;
    }

    private LocalDate minusVirkedag(LocalDate dato) {
        var resultat = dato;
        var dager = 1;
        while (dager > 0 || erHelg(resultat)) {
            if (!erHelg(resultat)) {
                dager--;
            }
            resultat = resultat.minusDays(1);
        }
        return resultat;
    }

    private boolean erLedigVirkedager(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyDato) {
        return finnOverlappendePeriode(nyDato, ikkeFlyttbarePerioder).isEmpty();
    }

    private boolean nyDatoHavnerIHullForbeholdtMorEtterJustering(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyDato) {
        return hullSomOverlapperMedPeriodeForbeholdMorEtterFødselsjustering(ikkeFlyttbarePerioder).stream()
            .anyMatch(d -> d.getLocalDateInterval().contains(nyDato));
    }

    private LocalDateTimeline<OppgittPeriodeEntitet> oppgittPeriodeSomOverlapperMedHullForbeholdMorEtterFødsel(OppgittPeriodeEntitet oppgittPeriode, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var oppgitt = new LocalDateTimeline<>(oppgittPeriode.getFom(), oppgittPeriode.getTom(), oppgittPeriode);
        return oppgitt.intersection(hullSomOverlapperMedPeriodeForbeholdMorEtterFødselsjustering(ikkeFlyttbarePerioder));
    }

    private LocalDateTimeline<Boolean> hullSomOverlapperMedPeriodeForbeholdMorEtterFødselsjustering(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var periodeMellomTomForbeholdtMorFørOgEtterFødsel = new LocalDateTimeline<>(
            TidsperiodeForbeholdtMor.tilOgMed(gammelFamiliehendelse),
            TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse), true);
        var allHull = new LocalDateTimeline<>(ikkeFlyttbarePerioder.stream()
            .filter(JusterPeriodeHull.class::isInstance)
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());
        return allHull.intersection(periodeMellomTomForbeholdtMorFørOgEtterFødsel);
    }

    private List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream().filter(periode -> !erPeriodeFlyttbar(periode)).toList();
    }

    private List<OppgittPeriodeEntitet> hullPerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> hull = new ArrayList<>();
        for (var i = 1; i < oppgittePerioder.size(); i++) {
            var nesteVirkedag = Virkedager.plusVirkedager(oppgittePerioder.get(i - 1).getTom(), 1);
            var startDatoNestePeriode = oppgittePerioder.get(i).getFom();
            if (!nesteVirkedag.isEqual(startDatoNestePeriode)) {
                var tom = Virkedager.plusVirkedager(nesteVirkedag, Virkedager.beregnAntallVirkedager(nesteVirkedag, startDatoNestePeriode) - 2);
                var tomForbeholdtMor = TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse);
                if (new LocalDateInterval(nesteVirkedag, tom).contains(tomForbeholdtMor) && !tom.isEqual(tomForbeholdtMor)) {
                    hull.add(new JusterPeriodeHull(nesteVirkedag, tomForbeholdtMor));
                    hull.add(new JusterPeriodeHull(tomForbeholdtMor.plusDays(1), tom));
                } else {
                    hull.add(new JusterPeriodeHull(nesteVirkedag, tom));
                }
            }
        }
        return hull;
    }


    private Optional<OppgittPeriodeEntitet> finnOverlappendePeriode(LocalDate dato, List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(periode -> periode.getFom().equals(dato) || periode.getFom().isBefore(dato) && (periode.getTom().equals(dato) || periode.getTom()
                .isAfter(dato)))
            .findFirst();
    }

    private List<OppgittPeriodeEntitet> fjernPerioderEtterSisteSøkteDato(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate sisteSøkteDato) {
        return oppgittePerioder.stream().filter(p -> p.getFom().isBefore(sisteSøkteDato) || p.getFom().isEqual(sisteSøkteDato)).map(p -> {
            if (p.getTom().isAfter(sisteSøkteDato)) {
                return OppgittPeriodeBuilder.fraEksisterende(p).medPeriode(p.getFom(), sisteSøkteDato).build();
            }
            return p;
        }).toList();
    }


    /**
     * Sjekk om perioden er flyttbar. Perioden er ikke flyttbar dersom det er en utsettelse, opphold, en gradert periode
     * eller samtidig uttak
     *
     * @param periode perioden som skal sjekkes.
     * @return true dersom perioden kan flyttes, ellers false.
     */
    private boolean erPeriodeFlyttbar(OppgittPeriodeEntitet periode) {
        //Perioder opprettet i interlogikk
        if (periode instanceof JusterPeriodeHull) {
            return false;
        }
        if (periode.isUtsettelse() || periode.isOpphold() || periode.isGradert()) {
            return false;
        }
        if (periode.isSamtidigUttak()) {
            //Mødrekvote med samtidig uttak skal flyttes
            return UttakPeriodeType.MØDREKVOTE.equals(periode.getPeriodeType());
        }
        return true;
    }

    private boolean overlapper(OppgittPeriodeEntitet periode, LocalDate dato) {
        return periode.getFom().isEqual(dato) || periode.getTom().isEqual(dato) || periode.getFom().isBefore(dato) && periode.getTom().isAfter(dato);
    }

    /**
     * Intern bruk for å håndtere ikke søkte perioder som hull
     */
    static class JusterPeriodeHull extends OppgittPeriodeEntitet {
        private final LocalDate fom;
        private final LocalDate tom;

        JusterPeriodeHull(LocalDate fom, LocalDate tom) {
            this.fom = fom;
            this.tom = tom;
        }

        @Override
        public LocalDate getFom() {
            return fom;
        }

        @Override
        public LocalDate getTom() {
            return tom;
        }


    }
}
