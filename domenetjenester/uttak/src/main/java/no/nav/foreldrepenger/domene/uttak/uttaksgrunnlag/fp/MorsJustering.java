package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor.tilOgMed;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.erHelg;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilFredag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilMandag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.erHullMellom;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.kopier;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.sorterEtterFom;
import static no.nav.foreldrepenger.regler.uttak.felles.PerioderUtenHelgUtil.helgBlirFredag;
import static no.nav.foreldrepenger.regler.uttak.felles.Virkedager.beregnAntallVirkedager;
import static no.nav.foreldrepenger.regler.uttak.felles.Virkedager.plusVirkedager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.fpsak.tidsserie.LocalDateInterval;

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
        var hullPåSluttenAvTidsperiodeForbeholdtMor = ikkeFlyttbarePerioder.stream()
            .filter(p -> p instanceof JusterPeriodeHull)
            .filter(p -> tilOgMed(gammelFamiliehendelse.plusDays(1)).isEqual(p.getFom()))
            .findFirst();
        var virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);

        List<OppgittPeriodeEntitet> justertePerioder = new ArrayList<>();
        for (var oppgittPeriode : oppgittePerioder) {
            var virkedagerSomSkalSkyvePeriode = hullPåSluttenAvTidsperiodeForbeholdtMor.map(p -> oppgittPeriode.getTom().isBefore(p.getFom())).orElse(true)
                ? virkedagerSomSkalSkyves : virkedagerSomSkalSkyves - beregnAntallVirkedager(hullPåSluttenAvTidsperiodeForbeholdtMor.get().getFom(), hullPåSluttenAvTidsperiodeForbeholdtMor.get().getTom());
            var justert = flyttPeriodeTilHøyre(oppgittPeriode, virkedagerSomSkalSkyvePeriode, ikkeFlyttbarePerioder);
            justertePerioder.addAll(justert);
        }
        justertePerioder = sorterEtterFom(justertePerioder);
        if (førsteUttaksdatoErFlyttet(oppgittePerioder, justertePerioder)) {
            var ekstraPeriode = lagEkstraPeriodeFraOpprinneligUttaksdato(oppgittePerioder, justertePerioder);
            justertePerioder.add(0, ekstraPeriode);
            justertePerioder = fjernPerioderEtterSisteSøkteDato(justertePerioder, oppgittePerioder.get(oppgittePerioder.size() - 1).getTom());
        }
        return fjernHullPerioder(justertePerioder);
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
            sortert = beholdSluttdatoForUttak(sortert, oppgittePerioder.get(oppgittePerioder.size() - 1).getTom());
        }
        sortert = fyllHullSkaptAvIkkeFlyttbarePerioder(sortert, oppgittePerioder);
        return fjernHullPerioder(sortert);
    }

    private static List<OppgittPeriodeEntitet> fjernHullPerioder(List<OppgittPeriodeEntitet> oppgittPerioder) {
        return oppgittPerioder.stream().filter(p -> !(p instanceof JusterPeriodeHull)).collect(Collectors.toList()); //NOSONAR
    }

    private List<OppgittPeriodeEntitet> fyllHull(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var hull = hullPerioder(oppgittePerioder);
        return sorterEtterFom(Stream.concat(oppgittePerioder.stream(), hull.stream()).toList());
    }

    private OppgittPeriodeEntitet lagEkstraPeriodeFraOpprinneligUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                           List<OppgittPeriodeEntitet> justertePerioder) {
        var uttakPeriodeType = finnUttakPeriodeType(justertePerioder);
        var first = oppgittePerioder.get(0);
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(uttakPeriodeType)
            .medPeriode(flyttFraHelgTilMandag(first.getFom()), flyttFraHelgTilFredag(justertePerioder.get(0).getFom().minusDays(1)))
            .medMottattDato(first.getMottattDato())
            .medTidligstMottattDato(first.getTidligstMottattDato().orElse(null))
            .build();
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
        var antallVirkedager = beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());
        //flytter en og en dag
        while (i < antallVirkedager - 1 && !knekkFunnet) {
            if (erLedigVirkedager(ikkeFlyttbarePerioder, plusVirkedager(nyTom, 1))) {
                nyTom = plusVirkedager(nyTom, 1);
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
            var fom = plusVirkedager(oppgittPeriode.getFom(), virkedagerPeriodeFørKnekk);
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
            nyFom = plusVirkedager(nyFom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom)) {
                i++;
            }
        }
        return nyFom;
    }

    private List<OppgittPeriodeEntitet> fyllHullSkaptAvIkkeFlyttbarePerioder(List<OppgittPeriodeEntitet> justertePerioder,
                                                                             List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();

        for (var i = 0; i < justertePerioder.size() - 1; i++) {
            resultat.add(justertePerioder.get(i));
            if (erHullMellom(justertePerioder.get(i).getTom(), justertePerioder.get(i + 1).getFom())) {
                var førsteVirkedagIHull = plusVirkedager(justertePerioder.get(i).getTom(), 1);
                var sisteVirkedagIHull = minusVirkedag(justertePerioder.get(i + 1).getFom());
                //sjekker om hullet vi finner ikke finnes i oppgitteperioder (hvis det er søkt med hull i perioden skal vi ikke fylle)
                if (!hullFinnesIOppgittePeriode(oppgittePerioder, førsteVirkedagIHull, sisteVirkedagIHull)) {
                    var sisteFlyttbarPeriode = sisteFlyttbarePeriode(resultat);
                    resultat.add(kopier(sisteFlyttbarPeriode, førsteVirkedagIHull, sisteVirkedagIHull));
                }
            }
        }
        resultat.add(justertePerioder.get(justertePerioder.size() - 1));
        return resultat;
    }

    private OppgittPeriodeEntitet sisteFlyttbarePeriode(List<OppgittPeriodeEntitet> resultat) {
        var flyttbarePerioder = flyttbarePerioder(resultat);
        if (flyttbarePerioder.size() == 0) {
            throw new IllegalStateException("Forventer minst en flyttbar periode før hull");
        }
        return flyttbarePerioder.get(flyttbarePerioder.size() - 1);
    }

    private boolean hullFinnesIOppgittePeriode(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate hullFom, LocalDate hullTom) {
        var perioderMedHull = hullPerioder(oppgittePerioder);
        return perioderMedHull.stream().anyMatch(p -> overlapper(p, hullFom) || overlapper(p, hullTom));
    }

    private List<OppgittPeriodeEntitet> flyttbarePerioder(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().filter(this::erPeriodeFlyttbar).collect(Collectors.toList());
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
            fraDato = plusVirkedager(fraDato, 1);
        }
        return ledigeVirkedager;
    }

    private boolean søktOmPerioderEtterFamiliehendelse(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse) {
        return oppgittePerioder.get(oppgittePerioder.size() - 1).getTom().isAfter(gammelFamiliehendelse);
    }

    private List<OppgittPeriodeEntitet> beholdSluttdatoForUttak(List<OppgittPeriodeEntitet> justertePerioder, LocalDate sluttdato) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>(justertePerioder);
        var sisteJustertePeriode = justertePerioder.get(justertePerioder.size() - 1);
        if (erPeriodeFlyttbar(sisteJustertePeriode)) {
            resultat.set(justertePerioder.size() - 1, kopier(sisteJustertePeriode, sisteJustertePeriode.getFom(), sluttdato));
        } else if (!sisteJustertePeriode.getTom().isEqual(sluttdato)) {
            var sisteFlyttbarePeriode = sisteFlyttbarePeriode(justertePerioder);
            resultat.add(kopier(sisteFlyttbarePeriode, plusVirkedager(sisteJustertePeriode.getTom(), 1), sluttdato));
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
            var nyTom = oppgittPeriode.getTom().isBefore(nyFamiliehendelse) ? oppgittPeriode.getTom() :
                helgBlirFredag(nyFamiliehendelse.minusDays(1));
            //Forkortes av fødsel
            return List.of(kopier(oppgittPeriode, oppgittPeriode.getFom(), nyTom));
        }
        if (oppgittPeriode.getFom().isBefore(gammelFamiliehendelse) && oppgittPeriode.getFom().isAfter(nyFamiliehendelse)) {
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
            if (erLedigVirkedager(ikkeFlyttbarePerioder, plusVirkedager(nyTom, 1))) {
                nyTom = plusVirkedager(nyTom, 1);
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
            var fom = plusVirkedager(oppgittPeriode.getFom(), virkedagerPeriodeFørKnekk);
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


    private boolean førsteUttaksdatoErFlyttet(List<OppgittPeriodeEntitet> oppgittePerioder, List<OppgittPeriodeEntitet> justertePerioder) {
        return !justertePerioder.get(0).getFom().isEqual(oppgittePerioder.get(0).getFom());
    }

    private boolean erLedigVirkedager(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyFom) {
        var overlappendeIkkeFlyttbar = finnOverlappendePeriode(nyFom, ikkeFlyttbarePerioder);
        if (overlappendeIkkeFlyttbar.isEmpty()) {
            return true;
        }
        return overlappendeIkkeFlyttbar.get() instanceof JusterPeriodeHull &&
            gammelFamiliehendelse.isBefore(nyFamiliehendelse) && overlappendeIkkeFlyttbar.get().getFom().isEqual(tilOgMed(gammelFamiliehendelse.plusDays(1)));
    }

    private List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream().filter(periode -> !erPeriodeFlyttbar(periode)).collect(Collectors.toList());
    }

    private List<OppgittPeriodeEntitet> hullPerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> hull = new ArrayList<>();
        for (var i = 1; i < oppgittePerioder.size(); i++) {
            var nesteVirkedag = plusVirkedager(oppgittePerioder.get(i - 1).getTom(), 1);
            var startDatoNestePeriode = oppgittePerioder.get(i).getFom();
            if (!nesteVirkedag.isEqual(startDatoNestePeriode)) {
                var tom = plusVirkedager(nesteVirkedag, beregnAntallVirkedager(nesteVirkedag, startDatoNestePeriode) - 2);
                var tomForbeholdtMor = tilOgMed(nyFamiliehendelse);
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
            .filter(periode -> (periode.getFom().equals(dato) || periode.getFom().isBefore(dato) && (periode.getTom().equals(dato) || periode.getTom()
                .isAfter(dato))))
            .findFirst();
    }

    private List<OppgittPeriodeEntitet> fjernPerioderEtterSisteSøkteDato(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate sisteSøkteDato) {
        return oppgittePerioder.stream().filter(p -> p.getFom().isBefore(sisteSøkteDato) || p.getFom().isEqual(sisteSøkteDato)).map(p -> {
            if (p.getTom().isAfter(sisteSøkteDato)) {
                return OppgittPeriodeBuilder.fraEksisterende(p).medPeriode(p.getFom(), sisteSøkteDato).build();
            }
            return p;
        }).collect(Collectors.toList());
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
        return periode.getFom().isEqual(dato) || periode.getTom().isEqual(dato) || (periode.getFom().isBefore(dato) && periode.getTom()
            .isAfter(dato));
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
