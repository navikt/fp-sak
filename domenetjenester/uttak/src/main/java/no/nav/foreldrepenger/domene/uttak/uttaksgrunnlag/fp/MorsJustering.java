package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.erHelg;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilFredag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilMandag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.sorterEtterFom;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager.beregnAntallVirkedager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.PerioderUtenHelgUtil;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
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
        var justertePerioder = flyttPerioderTilHøyre(oppgittePerioder);
        justertePerioder = fyllHullSomOppstårMellomFødselOgTermin(oppgittePerioder, justertePerioder);
        justertePerioder = fjernPerioderEtterSisteSøkteDato(justertePerioder, oppgittePerioder.getLast().getTom());
        return justertePerioder;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var justertePerioder = flyttPerioderTilVenstre(oppgittePerioder);
        if (justertePerioder.isEmpty()) {
            throw new IllegalStateException("Må ha minst en justert periode");
        }
        if (justertePerioder.stream().noneMatch(MorsJustering::erPeriodeFlyttbar) || justertePerioder.stream().allMatch(p -> p.getPeriodeType().equals(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL))) {
            return justertePerioder;
        }
        justertePerioder = fyllHullSkaptAvJusteringIPeriodeForbeholdMor(justertePerioder, oppgittePerioder);
        justertePerioder = fyllHullMellomFødselOgFørsteOpprinneligeUttaksdato(justertePerioder, oppgittePerioder);
        justertePerioder = fyllResterendeHullOgBeholdSluttdato(justertePerioder, oppgittePerioder);
        return justertePerioder;
    }

    private List<OppgittPeriodeEntitet> flyttPerioderTilHøyre(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var oppgittePerioderMedHull = fyllHull(oppgittePerioder);
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioderMedHull);
        var virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);
        var justertePerioder = new ArrayList<OppgittPeriodeEntitet>();
        for (var oppgittPeriode : oppgittePerioderMedHull) {
            if (virkedagerSomSkalSkyves > 0 && erPeriodeFlyttbar(oppgittPeriode)) {
                var justert = flyttPeriodeHøyre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder, true);

                if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FELLESPERIODE) && kanFellesperiodenReduseres(oppgittPeriode, justert)) {
                    var justertFellesperiode = reduserJusterteFellesperioder(justert, virkedagerSomSkalSkyves);
                    virkedagerSomSkalSkyves -= antallDagerFellesperiodeErRedusert(justert, justertFellesperiode);
                    justert = justertFellesperiode;
                }

                justertePerioder.addAll(justert);
            } else if (oppgittPeriode instanceof JusterPeriodeHull) { // Hull brukes bare til justering og reduksjon av forskyvning. Skal ikke legge til i justeringen
                virkedagerSomSkalSkyves -= antallSpisteVirkedagerIHull(oppgittPeriode, justertePerioder);
            } else {
                justertePerioder.add(oppgittPeriode);
            }
        }
        return justertePerioder.stream().toList();
    }

    private static int antallSpisteVirkedagerIHull(OppgittPeriodeEntitet oppgittPeriode, List<OppgittPeriodeEntitet> justertePerioder) {
        var tidligereJustertePerioder = tilLocalDateTimeLine(justertePerioder);
        var gjeldende = new LocalDateTimeline<>(oppgittPeriode.getFom(), oppgittPeriode.getTom(), oppgittPeriode);
        return gjeldende.intersection(tidligereJustertePerioder).stream()
            .mapToInt(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()))
            .sum();
    }

    private boolean kanFellesperiodenReduseres(OppgittPeriodeEntitet oppgittPeriode, List<OppgittPeriodeEntitet> justertePerioder) {
        return erPeriodeFlyttbar(oppgittPeriode) && justertePerioder.stream()
            .anyMatch(p -> p.getTom().isAfter(TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse)));
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
        var fellesperiodene = tilLocalDateTimeLine(justerteFellesperioder);
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
            var antallVirkedagerForPeriode = beregnAntallVirkedager(periode.getFom(), periode.getTom());
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


    /**
     * Her har vi 4 håndteringer:
     *  1) Er hele perioden mellom fødsel og termin?                                             Perioden blir spist opp/fjernet
     *  2) Strekker perioden seg fra før fødsel til mellom fødsel og termin?                     Avkort til dagen før fødsel
     *  3) Er perioden etter termin?                                                             Flytt til venstre
     *  4) Ikke justerbar, 0 virkedagerSomSkalSkyves igjen eller hele perioden er før fødsel?    Bevares som de er.
     */
    private List<OppgittPeriodeEntitet> flyttPerioderTilVenstre(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var oppgittePerioderMedHull = fyllHullEtterTermin(oppgittePerioder);
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioderMedHull);
        var virkedagerSomSkalSkyves = Virkedager.beregnAntallVirkedager(nyFamiliehendelse, gammelFamiliehendelse) - 1;
        var justertePerioder = new ArrayList<OppgittPeriodeEntitet>();
        for (var oppgittPeriode : oppgittePerioderMedHull) {
            if (erPeriodeFlyttbar(oppgittPeriode) && virkedagerSomSkalSkyves > 0 && !oppgittPeriode.getTom().isBefore(nyFamiliehendelse)) {
                if (!oppgittPeriode.getFom().isBefore(nyFamiliehendelse) && oppgittPeriode.getTom().isBefore(gammelFamiliehendelse)) {
                    continue; // Case 1: Fjernes
                }

                // Case 2: Avkortes
                if (oppgittPeriode.getFom().isBefore(gammelFamiliehendelse)) {
                    var nyTom = PerioderUtenHelgUtil.justerTomFredag(nyFamiliehendelse.minusDays(1));
                    justertePerioder.add(kopier(oppgittPeriode, oppgittPeriode.getFom(), nyTom));

                // Case 3: Skyves til venstre
                } else {
                    var justert = flyttPeriodeVenstre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
                    virkedagerSomSkalSkyves -= redusertForskyvning(oppgittPeriode, justert, ikkeFlyttbarePerioder, virkedagerSomSkalSkyves);
                    justertePerioder.addAll(justert);
                }
            // Case 4: Ikke justerbar, 0 virkedagerSomSkalSkyves igjen eller hele perioden er før fødsel
            } else if (!(oppgittPeriode instanceof JusterPeriodeHull)) {
                justertePerioder.add(oppgittPeriode);
            }
        }
        return sorterEtterFom(justertePerioder);
    }

    private static int redusertForskyvning(OppgittPeriodeEntitet oppgittPeriode,
                                           List<OppgittPeriodeEntitet> justert,
                                           List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder,
                                           int virkedagerSomSkalSkyves) {
        var fomDatoOppgitt = oppgittPeriode.getFom();
        var justertTimeline = tilLocalDateTimeLine(justert);
        var ikkeFlyttbareTimeline = tilLocalDateTimeLine(ikkeFlyttbarePerioder);
        var periodeForskjøvet = new LocalDateTimeline<>(justertTimeline.getMinLocalDate(), fomDatoOppgitt, true);
        var antallVirkedagerForskøvet = periodeForskjøvet.disjoint(ikkeFlyttbareTimeline).stream()
            .mapToInt(p -> beregnAntallVirkedager(p.getFom(), p.getTom()))
            .sum();

        var differanseIForskyvning = virkedagerSomSkalSkyves - antallVirkedagerForskøvet;
        return virkedagerSomSkalSkyves - differanseIForskyvning;
    }

    private static List<OppgittPeriodeEntitet> fyllHull(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var hull = hullPerioder(oppgittePerioder);
        return sorterEtterFom(Stream.concat(oppgittePerioder.stream(), hull.stream()).toList());
    }

    private List<OppgittPeriodeEntitet> fyllHullEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var hull = hullPerioderEtterTermin(oppgittePerioder);
        return sorterEtterFom(Stream.concat(oppgittePerioder.stream(), hull.stream())
            .toList());
    }

    private static List<OppgittPeriodeEntitet> hullPerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var oppgittTimeline = tilLocalDateTimeLine(oppgittePerioder);
        var sjekkPeriode = new LocalDateTimeline<>(oppgittTimeline.getMinLocalDate(), oppgittTimeline.getMaxLocalDate(), true);
        return sjekkPeriode.disjoint(oppgittTimeline).stream()
            .map(seg -> new JusterPeriodeHull(seg.getFom(), seg.getTom()))
            .map(OppgittPeriodeEntitet.class::cast)
            .toList();
    }

    private List<OppgittPeriodeEntitet> hullPerioderEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var oppgittTimeline = tilLocalDateTimeLine(oppgittePerioder);
        if (oppgittTimeline.getMaxLocalDate().isBefore(gammelFamiliehendelse)) {
            return List.of();
        }
        var gjeldendeIntervall = new LocalDateTimeline<>(gammelFamiliehendelse, oppgittTimeline.getMaxLocalDate(), true);
        return gjeldendeIntervall.disjoint(oppgittTimeline).stream()
            .map(seg -> new JusterPeriodeHull(seg.getFom(), seg.getTom()))
            .map(OppgittPeriodeEntitet.class::cast)
            .toList();
    }

    private static List<OppgittPeriodeEntitet> fyllHullSomOppstårMellomFødselOgTermin(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                               List<OppgittPeriodeEntitet> justertePerioder) {
        var resultat = new ArrayList<>(justertePerioder);
        var oppgittTimeline = tilLocalDateTimeLine(oppgittePerioder);
        var justertTimeline = tilLocalDateTimeLine(justertePerioder);
        var periodeSomSkalPaddes = new LocalDateTimeline<>(oppgittTimeline.getMinLocalDate(), justertTimeline.getMinLocalDate(), true);

        var padding = oppgittTimeline.disjoint(justertTimeline)
            .intersection(periodeSomSkalPaddes)
            .stream()
            .filter(p -> beregnAntallVirkedager(p.getFom(), p.getTom()) > 0)
            .map(s -> OppgittPeriodeBuilder.ny()
                .medPeriodeType(finnUttakPeriodeType(justertePerioder))
                .medPeriode(flyttFraHelgTilMandag(s.getFom()), flyttFraHelgTilFredag(s.getTom()))
                .medMottattDato(s.getValue().getMottattDato())
                .medTidligstMottattDato(s.getValue().getTidligstMottattDato().orElse(null))
                .build())
            .toList();
        resultat.addAll(padding);
        return sorterEtterFom(resultat);
    }

    private static UttakPeriodeType finnUttakPeriodeType(List<OppgittPeriodeEntitet> oppgittPerioder) {
        for (var oppgittPeriode : oppgittPerioder) {
            if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FORELDREPENGER)) {
                return UttakPeriodeType.FORELDREPENGER;
            }
        }
        return UttakPeriodeType.FELLESPERIODE;
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeVenstre(OppgittPeriodeEntitet oppgittPeriode, int antallVirkedagerSomSkalSkyves, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        var gjeldendePeriode = oppgittPeriode;
        var antallVirkedager = beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());
        var nyFom = finnNyFomVedFlyttingVenstre(oppgittPeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
        var nyTom = flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriodeIKKKE(nyFom, antallVirkedager, ikkeFlyttbarePerioder);
        var nyPeriode = kopier(gjeldendePeriode, nyFom, nyTom);
        resultat.add(nyPeriode);

        antallVirkedager -= beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom());
        if (antallVirkedager > 0) { // knekker
            var fom = nyPeriode.getTom().plusDays(1);
            var tom = Virkedager.plusVirkedager(nyPeriode.getTom(), antallVirkedager);
            gjeldendePeriode = kopier(oppgittPeriode, fom, tom);
            resultat.addAll(flyttPeriodeHøyre(gjeldendePeriode, 1, ikkeFlyttbarePerioder, false));
        }

        return resultat;
    }

    private LocalDate finnNyFomVedFlyttingVenstre(OppgittPeriodeEntitet oppgittPeriode, int antallVirkedagerSomSkalSkyves, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var nyFom = oppgittPeriode.getFom();
        var sisteLedigVirkedag = nyFom;
        var i = 0;
        //flytter en og en dag
        while (i < antallVirkedagerSomSkalSkyves) {
            nyFom = minusVirkedag(nyFom);
            if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FELLESPERIODE) && !nyFom.isAfter(TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse))) {
                return sisteLedigVirkedag;
            }
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom)) {
                i++;
                sisteLedigVirkedag = nyFom;
            }
        }
        return sisteLedigVirkedag;
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeHøyre(OppgittPeriodeEntitet oppgittPeriode,
                                                          int antallVirkedagerSomSkalSkyves,
                                                          List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder,
                                                          boolean skalJustereInnIPeriodeForbeholdtMor) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        var gjeldendePeriode = oppgittPeriode;
        var antallVirkedager = beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());

        while (antallVirkedager > 0) {
            var nyFom = finnNyFomVedFlyttingTilHøyre(gjeldendePeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder, skalJustereInnIPeriodeForbeholdtMor);
            var nyTom = flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriode(nyFom, antallVirkedager, ikkeFlyttbarePerioder, skalJustereInnIPeriodeForbeholdtMor);
            var nyPeriode = kopier(gjeldendePeriode, nyFom, nyTom);
            resultat.add(nyPeriode);

            antallVirkedager -= beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom());
            if (antallVirkedager > 0) {
                var fom = Virkedager.plusVirkedager(gjeldendePeriode.getFom(), beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom()));
                gjeldendePeriode = kopier(oppgittPeriode, fom, gjeldendePeriode.getTom());
            }
        }
        return resultat;
    }

    private LocalDate finnNyFomVedFlyttingTilHøyre(OppgittPeriodeEntitet oppgittPeriode, int antallVirkedagerSomSkalSkyves, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder,
                                                   boolean skalJustereInnIPeriodeForbeholdtMor) {
        var nyFom = oppgittPeriode.getFom();
        var i = 0;
        //flytter en og en dag
        while (i < antallVirkedagerSomSkalSkyves) {
            nyFom = Virkedager.plusVirkedager(nyFom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom) || (skalJustereInnIPeriodeForbeholdtMor && nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, nyFom))) {
                i++;
            }
        }
        return nyFom;
    }


    private List<OppgittPeriodeEntitet> fyllHullSkaptAvJusteringIPeriodeForbeholdMor(List<OppgittPeriodeEntitet> justertePerioder,
                                                                                     List<OppgittPeriodeEntitet> oppgittePerioder) {
        // Fyller hull i perioden forbehold mor som er før termin
        var forbeholdtMorTOM = TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse);
        var paddFremTilOgMedDato = forbeholdtMorTOM.isAfter(gammelFamiliehendelse) ? gammelFamiliehendelse.minusDays(1) : forbeholdtMorTOM;
        var periodeSomErFørTerminOgInneforPeriodenForbeholdMorEtterFødsel = new LocalDateTimeline<>(nyFamiliehendelse, paddFremTilOgMedDato, true);
        var justertTimeline = tilLocalDateTimeLine(justertePerioder);
        var fylteHull = periodeSomErFørTerminOgInneforPeriodenForbeholdMorEtterFødsel.disjoint(justertTimeline).stream()
            .filter(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()) > 0)
            .map(seg -> kopier(mødrekvoteEllerForeldrepengerFraFlyttbarPeriode(justertePerioder), seg.getFom(), seg.getTom()))
            .toList();

        var resultat = new ArrayList<>(justertePerioder);
        resultat.addAll(fylteHull);

        // Fyller hull i perioden forbehold mor etter termin bare hvis det i utgangspunktet er søkt om uttak i denne perioden (uttsettelser beholdes i sin helhet)
        if (forbeholdtMorTOM.isAfter(gammelFamiliehendelse)) {
            var oppgittTimeline = tilLocalDateTimeLine(oppgittePerioder);
            var fraTerminTilTOMForbeholdtMor = new LocalDateTimeline<>(gammelFamiliehendelse, forbeholdtMorTOM, true);
            var oppgittPeriodeEtterTerminInnenforPeriodeForbeholdtMor = oppgittTimeline.intersection(fraTerminTilTOMForbeholdtMor);
            var fylteHullEtterTerminInneforPeriodenForbeholdtMor = oppgittPeriodeEtterTerminInnenforPeriodeForbeholdtMor
                .disjoint(justertTimeline).stream()
                .filter(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()) > 0)
                .map(seg -> kopier(mødrekvoteEllerForeldrepengerFraFlyttbarPeriode(justertePerioder), seg.getFom(), seg.getTom()))
                .toList();
            resultat.addAll(fylteHullEtterTerminInneforPeriodenForbeholdtMor);
        }

        return sorterEtterFom(resultat);
    }

    private static OppgittPeriodeEntitet mødrekvoteEllerForeldrepengerFraFlyttbarPeriode(List<OppgittPeriodeEntitet> justertePerioder) {
        return justertePerioder.stream()
            .filter(MorsJustering::erPeriodeFlyttbar)
            .filter(p -> p.getPeriodeType().equals(UttakPeriodeType.MØDREKVOTE) || p.getPeriodeType().equals(UttakPeriodeType.FORELDREPENGER))
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .orElseGet(() -> mødrekvoteFraFlyttbarFellesperiode(justertePerioder));
    }

    private static OppgittPeriodeEntitet mødrekvoteFraFlyttbarFellesperiode(List<OppgittPeriodeEntitet> justertePerioder) {
        return justertePerioder.stream()
            .filter(MorsJustering::erPeriodeFlyttbar)
            .filter(p -> p.getPeriodeType().equals(UttakPeriodeType.FELLESPERIODE))
            .findFirst()
            .map(p -> OppgittPeriodeBuilder.fraEksisterende(p).medPeriodeType(UttakPeriodeType.MØDREKVOTE).build())
            .orElseThrow(() -> new IllegalStateException("Forventer minst en flyttbar periode som kan fylle hull før termin!"));
    }


    private List<OppgittPeriodeEntitet> fyllHullMellomFødselOgFørsteOpprinneligeUttaksdato(List<OppgittPeriodeEntitet> justertePerioder, List<OppgittPeriodeEntitet> oppgittePerioder) {
        var førsteUttaksdato = oppgittePerioder.getFirst().getFom();
        if (!nyFamiliehendelse.isBefore(førsteUttaksdato)) {
            return justertePerioder;
        }

        var justertTimeline = tilLocalDateTimeLine(justertePerioder);
        var periodeSomSkalPaddes = new LocalDateTimeline<>(nyFamiliehendelse, førsteUttaksdato.minusDays(1), true);
        var periodeMedHull = periodeSomSkalPaddes.disjoint(justertTimeline).stream()
            .filter(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()) > 0)
            .map(seg -> kopier(sisteFlyttbarePeriode(justertePerioder), seg.getFom(), seg.getTom()))
            .toList();

        var resultat = new ArrayList<>(justertePerioder);
        resultat.addAll(periodeMedHull);
        return sorterEtterFom(resultat);
    }

    private List<OppgittPeriodeEntitet> fyllResterendeHullOgBeholdSluttdato(List<OppgittPeriodeEntitet> justertePerioder, List<OppgittPeriodeEntitet> oppgittePerioder) {

        var oppgittTimeline = tilLocalDateTimeLine(oppgittePerioder);
        var justertTimeline = tilLocalDateTimeLine(justertePerioder);
        var periodeMedHull = oppgittTimeline.disjoint(justertTimeline).stream()
            .filter(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()) > 0)
            .map(seg -> kopier(sisteFlyttbarePeriode(justertePerioder), seg.getFom(), seg.getTom()))
            .toList();

        var resultat = new ArrayList<>(justertePerioder);
        resultat.addAll(periodeMedHull);
        return sorterEtterFom(resultat);
    }

    private static LocalDateTimeline<OppgittPeriodeEntitet> tilLocalDateTimeLine(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return new LocalDateTimeline<>(oppgittePerioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());
    }

    private static OppgittPeriodeEntitet sisteFlyttbarePeriode(List<OppgittPeriodeEntitet> justerte) {
        return justerte.stream()
            .filter(MorsJustering::erPeriodeFlyttbar)
            .filter(p -> !p.getPeriodeType().equals(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL))
            .max(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .orElseThrow(() -> new IllegalStateException("Forventer minst en flyttbar periode å fylle hull med!"));
    }

    private int beregnAntallLedigeVirkedager(LocalDate dato1, LocalDate dato2, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        LocalDate fraDato = dato1;
        LocalDate tilDato = dato2;
        //teller antall virkedager som ikke ligger i ikkeflyttbare perioder
        var ledigeVirkedager = 0;
        while (fraDato.isBefore(tilDato)) {
            if (erLedigVirkedager(ikkeFlyttbarePerioder, fraDato) || nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, fraDato)) {
                ledigeVirkedager++;
            }
            fraDato = Virkedager.plusVirkedager(fraDato, 1);
        }
        return ledigeVirkedager;
    }


    private LocalDate flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriodeIKKKE(LocalDate initiellTom, int antallVirkedager, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var nyTom = initiellTom;

        //flytter en og en dag frem til ikke ledig virkedag
        var i = 0;
        while (i < antallVirkedager - 1) {
            var nyTomPlus1Dag = Virkedager.plusVirkedager(nyTom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyTomPlus1Dag)) {
                nyTom = nyTomPlus1Dag;
                i++;
            } else {
                break;
            }
        }
        return nyTom;
    }

    private LocalDate flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriode(LocalDate initiellTom, int antallVirkedager, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder,
                                                                                  boolean skalJustereInnIPeriodeForbeholdtMor) {
        var nyTom = initiellTom;

        //flytter en og en dag frem til ikke ledig virkedag
        var i = 0;
        while (i < antallVirkedager - 1) {
            var nyTomPlus1Dag = Virkedager.plusVirkedager(nyTom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyTomPlus1Dag) || (skalJustereInnIPeriodeForbeholdtMor && nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, nyTomPlus1Dag))) {
                nyTom = nyTomPlus1Dag;
                i++;
            } else {
                break;
            }
        }
        return nyTom;
    }

    private static LocalDate minusVirkedag(LocalDate dato) {
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

    private static boolean erLedigVirkedager(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyDato) {
        return !overlapper(nyDato, ikkeFlyttbarePerioder);
    }

    private static boolean overlapper(LocalDate dato, List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().anyMatch(p -> !dato.isBefore(p.getFom()) && !dato.isAfter(p.getTom()));
    }

    private boolean nyDatoHavnerIHullForbeholdtMorEtterJustering(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyDato) {
        return hullSomOverlapperMedPeriodeForbeholdMorEtterFødselsjustering(ikkeFlyttbarePerioder).stream()
            .anyMatch(d -> d.getLocalDateInterval().contains(nyDato));
    }


    private LocalDateTimeline<Boolean> hullSomOverlapperMedPeriodeForbeholdMorEtterFødselsjustering(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        LocalDate fraDato;
        if (gammelFamiliehendelse.isBefore(nyFamiliehendelse)) {
            fraDato =TidsperiodeForbeholdtMor.tilOgMed(gammelFamiliehendelse);
        } else {
            fraDato = nyFamiliehendelse;
        }
        var tilDato = TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse);
        var periodeMellomTomForbeholdtMorFørOgEtterFødsel = new LocalDateTimeline<>(fraDato, tilDato, true);
        var allHull = new LocalDateTimeline<>(ikkeFlyttbarePerioder.stream()
            .filter(JusterPeriodeHull.class::isInstance)
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());
        return allHull.intersection(periodeMellomTomForbeholdtMorFørOgEtterFødsel);
    }

    private List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream().filter(periode -> !erPeriodeFlyttbar(periode)).toList();
    }

    private List<OppgittPeriodeEntitet> fjernPerioderEtterSisteSøkteDato(List<OppgittPeriodeEntitet> justertePerioder, LocalDate sisteSøkteDato) {
        return sorterEtterFom(justertePerioder).stream().filter(p -> p.getFom().isBefore(sisteSøkteDato) || p.getFom().isEqual(sisteSøkteDato)).map(p -> {
            if (p.getTom().isAfter(sisteSøkteDato)) {
                return OppgittPeriodeBuilder.fraEksisterende(p).medPeriode(p.getFom(), sisteSøkteDato).build();
            }
            return p;
        }).toList();
    }

    private static OppgittPeriodeEntitet kopier(OppgittPeriodeEntitet oppgittPeriode, LocalDate nyFom, LocalDate nyTom) {
        return OppgittPeriodeBuilder.fraEksisterende(oppgittPeriode)
            .medPeriode(flyttFraHelgTilMandag(nyFom), flyttFraHelgTilFredag(nyTom))
            .build();
    }


    /**
     * Sjekk om perioden er flyttbar. Perioden er ikke flyttbar dersom det er en utsettelse, opphold, en gradert periode
     * eller samtidig uttak
     *
     * @param periode perioden som skal sjekkes.
     * @return true dersom perioden kan flyttes, ellers false.
     */
    private static boolean erPeriodeFlyttbar(OppgittPeriodeEntitet periode) {
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

    /**
     * Intern bruk for å håndtere ikke søkte perioder som hull
     */
    private static class JusterPeriodeHull extends OppgittPeriodeEntitet {
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

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            if (!super.equals(o))
                return false;
            JusterPeriodeHull that = (JusterPeriodeHull) o;
            return Objects.equals(fom, that.fom) && Objects.equals(tom, that.tom);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), fom, tom);
        }
    }
}
