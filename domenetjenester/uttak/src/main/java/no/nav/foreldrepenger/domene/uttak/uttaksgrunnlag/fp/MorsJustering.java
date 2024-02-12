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
        var justertePerioder = flyttPerioderTilHøyre(oppgittePerioder);
        justertePerioder = new ArrayList<>(sorterEtterFom(justertePerioder));
        justertePerioder.addAll(beholdPerioderFraOpprinneligUttaksdato(oppgittePerioder, justertePerioder));
        justertePerioder = fjernPerioderEtterSisteSøkteDato(sorterEtterFom(justertePerioder), oppgittePerioder.getLast().getTom());
        justertePerioder = fjernHull(justertePerioder);
        return justertePerioder;
    }

    private List<OppgittPeriodeEntitet> flyttPerioderTilHøyre(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        var virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);
        List<OppgittPeriodeEntitet> justertePerioder = new ArrayList<>();
        for (var oppgittPeriode : oppgittePerioder) {
            if (virkedagerSomSkalSkyves > 0 && erPeriodeFlyttbar(oppgittPeriode)) {
                var justert = flyttPeriode(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);

                if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FELLESPERIODE) && kanFellesperiodenReduseres(oppgittPeriode, justert)) {
                    var justertFellesperiode = reduserJusterteFellesperioder(justert, virkedagerSomSkalSkyves);
                    virkedagerSomSkalSkyves -= antallDagerFellesperiodeErRedusert(justert, justertFellesperiode);
                    justert = justertFellesperiode;
                }

                justertePerioder.addAll(justert);
            } else {
                if (oppgittPeriode instanceof JusterPeriodeHull && antallSpisteVirkedagerIHull(oppgittPeriode, justertePerioder) > 0) {
                    var antallSpisteVirkedager = antallSpisteVirkedagerIHull(oppgittPeriode, justertePerioder);
                    if (Virkedager.beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom()) > antallSpisteVirkedager) {
                        justertePerioder.add(kopier(oppgittPeriode, Virkedager.plusVirkedager(oppgittPeriode.getFom(), antallSpisteVirkedager), oppgittPeriode.getTom()));
                    }
                    virkedagerSomSkalSkyves -= antallSpisteVirkedager;
                } else {
                    justertePerioder.add(oppgittPeriode);
                }
            }
        }
        return justertePerioder;
    }

    private static int antallSpisteVirkedagerIHull(OppgittPeriodeEntitet oppgittPeriode, List<OppgittPeriodeEntitet> justertePerioder) {
        var tidligereJustertePerioder = new LocalDateTimeline<>(justertePerioder.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p))
            .toList());
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


    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        oppgittePerioder = fyllHull(oppgittePerioder);
        var justertePerioder = flyttPerioderTilVenstre(oppgittePerioder);

        // Fyll hulll som havner innenfor 6 ukene og som er søkt om i utgangspunktet. Resten blir MSP..
        if (søktOmPerioderEtterFamiliehendelse(oppgittePerioder, gammelFamiliehendelse)) {
            justertePerioder = beholdSluttdatoForUttak(justertePerioder, oppgittePerioder.getLast().getTom());
        }
        justertePerioder = fyllHullSkaptAvIkkeFlyttbarePerioder(justertePerioder, oppgittePerioder);
        justertePerioder = fjernHull(justertePerioder);
        return justertePerioder;
    }

    /**
     * Her har vi 4 håndteringer:
     *  1) Er hele perioden mellom fødsel og termin?                                             Perioden blir spist opp/fjernet
     *  2) Strekker perioden seg fra før fødsel til mellom fødsel og termin?                     Avkort til dagen før fødsel
     *  3) Er perioden etter termin?                                                             Flytt til venstre
     *  4) Ikke justerbar, 0 virkedagerSomSkalSkyves igjen eller hele perioden er før fødsel?    Bevares som de er.
     */
    private List<OppgittPeriodeEntitet> flyttPerioderTilVenstre(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        var virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);
        List<OppgittPeriodeEntitet> justertePerioder = new ArrayList<>();
        for (var oppgittPeriode : oppgittePerioder) {
            if (erPeriodeFlyttbar(oppgittPeriode) && virkedagerSomSkalSkyves > 0 && !oppgittPeriode.getTom().isBefore(nyFamiliehendelse)) {
                if (!oppgittPeriode.getFom().isBefore(nyFamiliehendelse) && oppgittPeriode.getTom().isBefore(gammelFamiliehendelse)) {
                    continue; // Case 1: Fjernes
                }

                // Case 2: Avkortes
                if (oppgittPeriode.getFom().isBefore(gammelFamiliehendelse)) {
                    var nyTom = PerioderUtenHelgUtil.justerTomFredag(nyFamiliehendelse.minusDays(1));
                    justertePerioder.add(kopier(oppgittPeriode, oppgittPeriode.getFom(), nyTom));
                } else { // Case 3: Skyves til venstre
                    var justert = flyttPeriode(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);

                    // Ikke flytt fellesperiode lenger inn i periode forbehold mor etter fødsel
                    var antallDagerFellesperiodeSomBlirFlyttetInnIPeriodeForbeholdMorEtterFødsel = antallDagerFellesperiodeSomBlirFlyttetInnIPeriodeForbeholdMorEtterFødsel(oppgittPeriode, justert);
                    if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FELLESPERIODE) && antallDagerFellesperiodeSomBlirFlyttetInnIPeriodeForbeholdMorEtterFødsel > 0) {
                        virkedagerSomSkalSkyves -= antallDagerFellesperiodeSomBlirFlyttetInnIPeriodeForbeholdMorEtterFødsel;
                        justert = flyttPeriode(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
                    }
                    justertePerioder.addAll(justert);
                }
            } else {
                // Case 4: Ikke justerbar, 0 virkedagerSomSkalSkyves igjen eller hele perioden er før fødsel
                justertePerioder.add(oppgittPeriode);
            }
        }
        return sorterEtterFom(justertePerioder);
    }


    /**
     * Vi skal bare trekke fra fellesperiode som havner innenfor periode forbeholdt mor etter fødsel OG som ikke var innenfor dette intervallet i utgangspunktet
     * ---|--fffffffff
     * -|--fffffffff
     *   xxxxxx (periode forbehold mor etter fødsel)
     *     ffff (fellesperiode som havner innefor de første 6 etter)
     *     ff   (fellesperiode som flyttes inn i periode forbehold mor)
     */
    private int antallDagerFellesperiodeSomBlirFlyttetInnIPeriodeForbeholdMorEtterFødsel(OppgittPeriodeEntitet opprinneligPeriode, List<OppgittPeriodeEntitet> justertePerioder) {
        var opprinnelig = new LocalDateTimeline<>(opprinneligPeriode.getFom(), opprinneligPeriode.getTom(), true);
        var justert = new LocalDateTimeline<>(justertePerioder.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());
        var andelAvPeriodenSkøvetTilVenstre = justert.disjoint(opprinnelig);

        var periodeForbeholdtMorEtterFødsel = new LocalDateTimeline<>(nyFamiliehendelse, TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse), true);
        return andelAvPeriodenSkøvetTilVenstre.intersection(periodeForbeholdtMorEtterFødsel).stream()
            .mapToInt(p -> beregnAntallVirkedager(p.getFom(), p.getTom()))
            .sum();
    }

    private static List<OppgittPeriodeEntitet> fjernHull(List<OppgittPeriodeEntitet> oppgittPerioder) {
        return oppgittPerioder.stream().filter(p -> !(p instanceof JusterPeriodeHull)).toList();
    }

    private List<OppgittPeriodeEntitet> fyllHull(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var hull = hullPerioder(oppgittePerioder);
        return sorterEtterFom(Stream.concat(oppgittePerioder.stream(), hull.stream()).toList());
    }

    private List<OppgittPeriodeEntitet> beholdPerioderFraOpprinneligUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                               List<OppgittPeriodeEntitet> justertePerioder) {
        var oppgittTimeline = new LocalDateTimeline<>(
            fjernHull(oppgittePerioder).stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());
        var justertTimeline = new LocalDateTimeline<>(
            fjernHull(justertePerioder).stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());

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

    private List<OppgittPeriodeEntitet> flyttPeriode(OppgittPeriodeEntitet oppgittPeriode,
                                                     int antallVirkedagerSomSkalSkyves,
                                                     List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        if (nyFamiliehendelse.isAfter(gammelFamiliehendelse)) {
            return flyttPeriode(Retning.HØYRE, oppgittPeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
        } else {
            return flyttPeriode(Retning.VENSTRE, oppgittPeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
        }
    }

    private List<OppgittPeriodeEntitet> flyttPeriode(Retning retning,
                                                     OppgittPeriodeEntitet oppgittPeriode,
                                                     int antallVirkedagerSomSkalSkyves,
                                                     List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        var gjeldendePeriode = oppgittPeriode;
        var antallVirkedager = beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());

        while (antallVirkedager > 0) {
            var nyFom = finnNyFomVedFlytting(retning, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder, gjeldendePeriode);
            var nyTom = flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriode(nyFom, antallVirkedager, ikkeFlyttbarePerioder);
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

    private LocalDate finnNyFomVedFlytting(Retning retning, int antallVirkedagerSomSkalSkyves, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, OppgittPeriodeEntitet gjeldendePeriode) {
        return switch (retning) {
            case HØYRE -> finnNyFomVedFlyttingTilHøyre(gjeldendePeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            case VENSTRE -> finnNyFomVedFlyttingTilVenstre(gjeldendePeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
        };
    }

    private LocalDate finnNyFomVedFlyttingTilHøyre(OppgittPeriodeEntitet oppgittPeriode, int antallVirkedagerSomSkalSkyves, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
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
            if (erLedigVirkedager(ikkeFlyttbarePerioder, fraDato) || nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, fraDato)) {
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

    private LocalDate flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriode(LocalDate initiellTom, int antallVirkedager, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var nyTom = initiellTom;

        //flytter en og en dag frem til ikke ledig virkedag
        var i = 0;
        while (i < antallVirkedager - 1) {
            var nyTomPlus1Dag = Virkedager.plusVirkedager(nyTom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyTomPlus1Dag) || nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, nyTomPlus1Dag)) {
                nyTom = nyTomPlus1Dag;
                i++;
            } else {
                break;
            }
        }
        return nyTom;
    }


    private LocalDate finnNyFomVedFlyttingTilVenstre(OppgittPeriodeEntitet oppgittPeriode,
                                                     int antallVirkedagerSomSkalSkyves,
                                                     List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var nyFom = oppgittPeriode.getFom();
        var i = 0;
        //flytter en og en dag
        while (i < antallVirkedagerSomSkalSkyves) {
            nyFom = minusVirkedag(nyFom);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom) || nyDatoHavnerIHullForbeholdtMorEtterJustering(ikkeFlyttbarePerioder, nyFom)) {
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

    private static boolean erLedigVirkedager(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyDato) {
        return finnOverlappendePeriode(nyDato, ikkeFlyttbarePerioder).isEmpty();
    }

    private boolean nyDatoHavnerIHullForbeholdtMorEtterJustering(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyDato) {
        return hullSomOverlapperMedPeriodeForbeholdMorEtterFødselsjustering(ikkeFlyttbarePerioder).stream()
            .anyMatch(d -> d.getLocalDateInterval().contains(nyDato));
    }


    private LocalDateTimeline<Boolean> hullSomOverlapperMedPeriodeForbeholdMorEtterFødselsjustering(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        LocalDate fraDato;
        LocalDate tilDato;
        if (gammelFamiliehendelse.isBefore(nyFamiliehendelse)) {
            fraDato =TidsperiodeForbeholdtMor.tilOgMed(gammelFamiliehendelse);
            tilDato =TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse);
        } else {
            fraDato = nyFamiliehendelse;
            tilDato = TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse);
        }
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

    private List<OppgittPeriodeEntitet> hullPerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> hull = new ArrayList<>();
        for (var i = 1; i < oppgittePerioder.size(); i++) {
            var nesteVirkedag = Virkedager.plusVirkedager(oppgittePerioder.get(i - 1).getTom(), 1);
            var startDatoNestePeriode = oppgittePerioder.get(i).getFom();
            if (!nesteVirkedag.isEqual(startDatoNestePeriode)) {
                var tom = Virkedager.plusVirkedager(nesteVirkedag, beregnAntallVirkedager(nesteVirkedag, startDatoNestePeriode) - 2);
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


    private static Optional<OppgittPeriodeEntitet> finnOverlappendePeriode(LocalDate dato, List<OppgittPeriodeEntitet> perioder) {
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

    private enum Retning {
        HØYRE,
        VENSTRE
    }
}
