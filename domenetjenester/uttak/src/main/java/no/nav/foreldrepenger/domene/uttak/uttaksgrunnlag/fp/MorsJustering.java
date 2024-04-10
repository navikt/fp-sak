package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.erHelg;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilFredag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilMandag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.kopier;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.sorterEtterFom;
import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager.beregnAntallVirkedager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(MorsJustering.class);

    private final LocalDate gammelFamiliehendelse;
    private final LocalDate nyFamiliehendelse;

    MorsJustering(LocalDate gammelFamiliehendelse, LocalDate nyFamiliehendelse) {
        this.gammelFamiliehendelse = gammelFamiliehendelse;
        this.nyFamiliehendelse = nyFamiliehendelse;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var justertePerioder = flyttPerioderTilHøyre(oppgittePerioder);
        justertePerioder = fyllHullSomOppstårPgaJustering(oppgittePerioder, justertePerioder);
        justertePerioder = fjernPerioderEtterSisteSøkteDato(justertePerioder, oppgittePerioder.getLast().getTom());
        return justertePerioder;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        LOG.info("Fødselsjusterer ved fødsel før termin {} {} {}", gammelFamiliehendelse, nyFamiliehendelse, oppgittePerioder);
        var justertePerioder = flyttPerioderTilVenstre(oppgittePerioder);
        LOG.info("Flyttet perioder til venstre {}", justertePerioder);

        if (justertePerioder.isEmpty()) {
            throw new IllegalStateException("Må ha minst en justert periode");
        }
        if (justertePerioder.stream().noneMatch(MorsJustering::erPeriodeFlyttbar) || justertePerioder.stream().allMatch(p -> p.getPeriodeType().equals(FORELDREPENGER_FØR_FØDSEL))) {
            return justertePerioder;
        }
        justertePerioder = fyllHullSomOppstårPgaJustering(oppgittePerioder, justertePerioder);
        LOG.info("Flyttet perioder fylt hull oppstått pga justering {}", justertePerioder);
        justertePerioder = fyllHullSomOppstårFørFørsteOpprinneligeUttaksdatoOgFødsel(oppgittePerioder, justertePerioder);
        LOG.info("Flyttet perioder fylt hull før første {}", justertePerioder);
        return justertePerioder;
    }

    private List<OppgittPeriodeEntitet> fyllHullSomOppstårFørFørsteOpprinneligeUttaksdatoOgFødsel(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                                                  List<OppgittPeriodeEntitet> justertePerioder) {
        var førsteOpprinneligeUttaksdato = oppgittePerioder.getFirst().getFom();
        if (!førsteOpprinneligeUttaksdato.isAfter(nyFamiliehendelse)) {
            return justertePerioder;
        }

        var kopierbarMødrekvote = førsteFlyttbarePeriode(oppgittePerioder);
        var intervallMellomFødselOgFørsteOpprinneligeUttaksdato = new LocalDateTimeline<>(nyFamiliehendelse, førsteOpprinneligeUttaksdato.minusDays(1), kopierbarMødrekvote);
        var justertTimeline = tilLocalDateTimeLine(justertePerioder);
        var fylteHull = intervallMellomFødselOgFørsteOpprinneligeUttaksdato.disjoint(justertTimeline).stream()
            .filter(MorsJustering::harVirkedager)
            .map(seg -> kopier(seg.getValue(), seg.getFom(), seg.getTom()))
            .toList();

        var resultat = new ArrayList<>(justertePerioder);
        resultat.addAll(fylteHull);
        return sorterEtterFom(resultat);
    }

    private static OppgittPeriodeEntitet førsteFlyttbarePeriode(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return førsteFlyttbarePeriodeAvType(oppgittePerioder, Set.of(MØDREKVOTE, FORELDREPENGER))
            .orElseGet(() -> førsteFlyttbarePeriodeAvType(oppgittePerioder, Set.of(FELLESPERIODE))
                .map(p -> OppgittPeriodeBuilder.fraEksisterende(p).medPeriodeType(MØDREKVOTE).build())
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fødselsjustering prøver å fylle periode mellom fødsel og første opprinnelig uttaksdato uten flyttbare perioder")));
    }

    private List<OppgittPeriodeEntitet> fyllHullSomOppstårPgaJustering(List<OppgittPeriodeEntitet> oppgittePerioder, List<OppgittPeriodeEntitet> justertePerioder) {
        var oppgittTimeline = tilLocalDateTimeLine(oppgittePerioder);
        var justertTimeline = tilLocalDateTimeLine(justertePerioder);
        var hullSomHarOppstått = oppgittTimeline.disjoint(justertTimeline);
        if (hullSomHarOppstått.isEmpty()) {
            return justertePerioder;
        }

        var resultat = new ArrayList<>(justertePerioder);

        // 1) Før fødsel fylles alltid med fellesperiode eller foreldrepenger (arv fra segment)
        var intervallFørFødsel = new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, nyFamiliehendelse.minusDays(1));
        var hullFørFødsel = hullSomHarOppstått.intersection(intervallFørFødsel).stream()
            .filter(MorsJustering::harVirkedager)
            .map(segment -> nyPeriodeFraFørsteFlyttbarePeriode(segment, oppgittePerioder, Set.of(FELLESPERIODE, FORELDREPENGER)))
            .toList();
        resultat.addAll(hullFørFødsel);

        // 2) Etter fødsel og frem til 6 uker etter fødsel (eventuelt frem til termin om det er lenger) fylles med MK eller FOR
        var forbeholdtMorEtterFødselTom = TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse);
        var etterFødselTom = forbeholdtMorEtterFødselTom.isAfter(gammelFamiliehendelse) ? forbeholdtMorEtterFødselTom : gammelFamiliehendelse.minusDays(1);
        var intervallForbeholdtMorEtterFødsel = new LocalDateInterval(nyFamiliehendelse, etterFødselTom);
        var hullForboldtMorEtterFødsel = hullSomHarOppstått.intersection(intervallForbeholdtMorEtterFødsel).stream()
            .filter(MorsJustering::harVirkedager)
            .map(segment -> nyPeriodeFraFørsteFlyttbarePeriode(segment, oppgittePerioder, Set.of(MØDREKVOTE, FORELDREPENGER)))
            .toList();
        resultat.addAll(hullForboldtMorEtterFødsel);

        // 3) Etter fødsel og etter 6 fylles med siste flyttbare periode.
        var intervallEtterUkeneForbeholdtMor = new LocalDateInterval(etterFødselTom.plusDays(1), LocalDateInterval.TIDENES_ENDE);
        var hullEtterUkeneForbeholdtMor = hullSomHarOppstått.intersection(intervallEtterUkeneForbeholdtMor).stream()
            .filter(MorsJustering::harVirkedager)
            .map(segment -> nyPeriodeFraSisteFlyttbarePeriode(segment, oppgittePerioder))
            .toList();
        resultat.addAll(hullEtterUkeneForbeholdtMor);
        return sorterEtterFom(resultat);
    }

    private static OppgittPeriodeEntitet nyPeriodeFraSisteFlyttbarePeriode(LocalDateSegment<OppgittPeriodeEntitet> manglendeSegment, List<OppgittPeriodeEntitet> oppgittePerioder) {
        return sisteFlyttbarePeriode(oppgittePerioder)
            .map(p -> nyPeriodeFra(manglendeSegment, p.getPeriodeType()))
            .orElse(nyPeriode(manglendeSegment, oppgittePerioder, Set.of()));
    }

    private static OppgittPeriodeEntitet nyPeriodeFraFørsteFlyttbarePeriode(LocalDateSegment<OppgittPeriodeEntitet> manglendeSegment, List<OppgittPeriodeEntitet> oppgittePerioder, Set<UttakPeriodeType> tillatteTyper) {
        return førsteFlyttbarePeriodeAvType(oppgittePerioder, tillatteTyper)
            .map(p -> nyPeriodeFra(manglendeSegment, p.getPeriodeType()))
            .orElse(nyPeriode(manglendeSegment, oppgittePerioder, tillatteTyper));
    }

    private static Optional<OppgittPeriodeEntitet> førsteFlyttbarePeriodeAvType(List<OppgittPeriodeEntitet> perioder, Set<UttakPeriodeType> type) {
        return perioder.stream()
            .filter(MorsJustering::erPeriodeFlyttbar)
            .filter(p -> type.contains(p.getPeriodeType()))
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom));
    }

    private static Optional<OppgittPeriodeEntitet> sisteFlyttbarePeriode(List<OppgittPeriodeEntitet> justerte) {
        return justerte.stream()
            .filter(MorsJustering::erPeriodeFlyttbar)
            .filter(p -> !p.getPeriodeType().equals(FORELDREPENGER_FØR_FØDSEL))
            .max(Comparator.comparing(OppgittPeriodeEntitet::getFom));
    }

    private static OppgittPeriodeEntitet nyPeriodeFra(LocalDateSegment<OppgittPeriodeEntitet> manglendeSegment, UttakPeriodeType periodeType) {
        return OppgittPeriodeBuilder.fraEksisterende(manglendeSegment.getValue())
            .medPeriodeType(periodeType)
            .medPeriode(flyttFraHelgTilMandag(manglendeSegment.getFom()), flyttFraHelgTilFredag(manglendeSegment.getTom()))
            .build();
    }

    private static OppgittPeriodeEntitet nyPeriode(LocalDateSegment<OppgittPeriodeEntitet> manglendeSegment, List<OppgittPeriodeEntitet> oppgittePerioder, Set<UttakPeriodeType> tillatteTyper) {
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(finnUttakPeriodeType(oppgittePerioder, tillatteTyper))
            .medPeriode(flyttFraHelgTilMandag(manglendeSegment.getFom()), flyttFraHelgTilFredag(manglendeSegment.getTom()))
            .medMottattDato(manglendeSegment.getValue().getMottattDato())
            .medTidligstMottattDato(manglendeSegment.getValue().getTidligstMottattDato().orElse(null))
            .build();
    }

    private static UttakPeriodeType finnUttakPeriodeType(List<OppgittPeriodeEntitet> oppgittePerioder, Set<UttakPeriodeType> tillatteTyper) {
        if (oppgittePerioder.stream().map(OppgittPeriodeEntitet::getPeriodeType).anyMatch(FORELDREPENGER::equals)) {
            return FORELDREPENGER;
        }
        return tillatteTyper.stream()
            .filter(type -> !FORELDREPENGER.equals(type))
            .findFirst()
            .orElse(FELLESPERIODE);
    }

    private static boolean harVirkedager(LocalDateSegment<OppgittPeriodeEntitet> p) {
        return beregnAntallVirkedager(p.getFom(), p.getTom()) > 0;
    }

    private static int antallVirkedager(List<OppgittPeriodeEntitet> opprinneligeFFFperioder) {
        return opprinneligeFFFperioder.stream().mapToInt(p -> beregnAntallVirkedager(p.getFom(), p.getTom())).sum();
    }

    private List<OppgittPeriodeEntitet> flyttPerioderTilHøyre(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        var virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);
        var justertePerioder = new ArrayList<OppgittPeriodeEntitet>();
        var antallDagerTilgjengeligForReduksjon = 0;
        for (var oppgittPeriode : oppgittePerioder) {
            if (antallDagerTilgjengeligForReduksjon > 0 && erPeriodeFlyttbar(oppgittPeriode)) {
                var antallDagerSkøvetInnIHullFørDato = Math.min(antallDagerSkøvetInnIHullFørDato(oppgittPeriode.getFom(), justertePerioder, ikkeFlyttbarePerioder), antallDagerTilgjengeligForReduksjon);
                antallDagerTilgjengeligForReduksjon -= antallDagerSkøvetInnIHullFørDato;
                virkedagerSomSkalSkyves -= antallDagerSkøvetInnIHullFørDato;
            }

            if (virkedagerSomSkalSkyves > 0 && erPeriodeFlyttbar(oppgittPeriode)) {
                var justert = flyttPeriodeHøyre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
                if (oppgittPeriode.getPeriodeType().equals(FELLESPERIODE) && kanFellesperiodenReduseres(justert)) {
                    var justertFellesperiode = reduserJusterteFellesperioder(justert, virkedagerSomSkalSkyves);
                    virkedagerSomSkalSkyves -= antallDagerFellesperiodeErRedusert(justert, justertFellesperiode);
                    justert = justertFellesperiode;
                }
                antallDagerTilgjengeligForReduksjon += antallSpisteVirkedagerIHull(justert, ikkeFlyttbarePerioder);
                justertePerioder.addAll(justert);
            } else {
                justertePerioder.add(oppgittPeriode);
            }
        }
        return justertePerioder.stream().toList();
    }

    private static int antallDagerSkøvetInnIHullFørDato(LocalDate fom, List<OppgittPeriodeEntitet> justertePerioder, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var ikkeFlyttbarTimeline = tilLocalDateTimeLine(ikkeFlyttbarePerioder);
        var tidligereJustertePerioder = tilLocalDateTimeLine(justertePerioder);
        var gjeldendeIntervall = new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, fom);
        return ikkeFlyttbarTimeline.intersection(tidligereJustertePerioder).intersection(gjeldendeIntervall).stream()
            .mapToInt(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()))
            .sum();
    }

    private static int antallSpisteVirkedagerIHull(List<OppgittPeriodeEntitet> justertePerioder, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        var justertTimeline = tilLocalDateTimeLine(justertePerioder);
        var ikkeFlyttbareTimeline = tilLocalDateTimeLine(ikkeFlyttbarePerioder);
        return ikkeFlyttbareTimeline.intersection(justertTimeline).stream()
            .mapToInt(seg -> beregnAntallVirkedager(seg.getFom(), seg.getTom()))
            .sum();
    }

    private boolean kanFellesperiodenReduseres(List<OppgittPeriodeEntitet> justertePerioder) {
        return justertePerioder.stream().anyMatch(p -> p.getTom().isAfter(TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse)));
    }

    private static int antallDagerFellesperiodeErRedusert(List<OppgittPeriodeEntitet> justert, List<OppgittPeriodeEntitet> justertOgRedusert) {
        var antallVirkedagerEtterJustering = antallVirkedager(justert);
        var antallVirkedagerEtterReduksjon = antallVirkedager(justertOgRedusert);
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
        var periodeSomKanReduseres = new LocalDateInterval(TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse).plusDays(1), Tid.TIDENES_ENDE);
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
     * Her har vi 6 håndteringer:
     *  1) FORELDREPENGER_FØR_FØDSEL legger vi på til slutt
     *  2) Perioder som slutter før F-3U beholds uendret
     *  3) Perioder som starter før F-3U, men slutter etter F-3U avkortes til F-3U minus en dag
     *  4) Perioder som er mellom F-3U og T fjernes.
     *  5) Perioder som er etter termin justeres antall virkedager som er mellom termin og fødsel
     *  6) Perioder beholdes uendret dersom de ikke er flyttbare eller om vi er ferdig med forskyvning (virkedagerSomSkalSkyves==0)
     */
    private List<OppgittPeriodeEntitet> flyttPerioderTilVenstre(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        var virkedagerSomSkalSkyves = Virkedager.beregnAntallVirkedager(nyFamiliehendelse, gammelFamiliehendelse) - 1;
        var justertePerioder = new ArrayList<OppgittPeriodeEntitet>();
        for (var oppgittPeriode : oppgittePerioder) {
            if (!erPeriodeFlyttbar(oppgittPeriode) || virkedagerSomSkalSkyves == 0) {
                // 6) Perioder beholdes uendret dersom de ikke er flyttbare eller om vi er ferdig med forskyvning (virkedagerSomSkalSkyves==0)
                justertePerioder.add(oppgittPeriode);
            } else {
                // 1) FORELDREPENGER_FØR_FØDSEL legger vi på til slutt
                if (oppgittPeriode.getPeriodeType().equals(FORELDREPENGER_FØR_FØDSEL)) {
                    // Legges til etterpå
                }

                // 2) Perioder som slutter før F-3U beholds uendret
                else if (oppgittPeriode.getTom().isBefore(TidsperiodeForbeholdtMor.fraOgMed(nyFamiliehendelse))) {
                    justertePerioder.add(oppgittPeriode);
                }

                // 3) Perioder som starter før F-3U, men slutter etter F-3U avkortes til F-3U minus en dag
                else if (oppgittPeriode.getFom().isBefore(TidsperiodeForbeholdtMor.fraOgMed(nyFamiliehendelse))) {
                    var nyTom = PerioderUtenHelgUtil.justerTomFredag(TidsperiodeForbeholdtMor.fraOgMed(nyFamiliehendelse).minusDays(1));
                    justertePerioder.add(kopier(oppgittPeriode, oppgittPeriode.getFom(), nyTom));
                }

                // 4) Perioder som er mellom F-3U og T fjernes.
                else if (!oppgittPeriode.getFom().isBefore(TidsperiodeForbeholdtMor.fraOgMed(nyFamiliehendelse)) && oppgittPeriode.getTom().isBefore(gammelFamiliehendelse)) {
                    // Fjernes
                }

                //5) Perioder som er etter termin justeres antall virkedager som er mellom termin og fødsel
                else {
                    var justert = flyttPeriodeVenstre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
                    virkedagerSomSkalSkyves -= differansenMellomAntallDagerSomBleForskøvetMotAntallDagerSomSkulleJusteres(oppgittPeriode, justert, ikkeFlyttbarePerioder, virkedagerSomSkalSkyves);
                    justertePerioder.addAll(justert);
                }
            }
        }

        var resultat = leggTilFFFkonto(justertePerioder, oppgittePerioder); // 1) FORELDREPENGER_FØR_FØDSEL legger vi på til slutt
        return sorterEtterFom(resultat);
    }

    private static int differansenMellomAntallDagerSomBleForskøvetMotAntallDagerSomSkulleJusteres(OppgittPeriodeEntitet oppgittPeriode,
                                                                                           List<OppgittPeriodeEntitet> justert,
                                                                                           List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder,
                                                                                           int virkedagerSomSkalSkyves) {
        var fomDatoOppgitt = oppgittPeriode.getFom();
        var justertTimeline = tilLocalDateTimeLine(justert);
        var ikkeFlyttbareTimeline = tilLocalDateTimeLine(ikkeFlyttbarePerioder);
        var periodeForskjøvet = new LocalDateTimeline<>(justertTimeline.getMinLocalDate(), fomDatoOppgitt, true);
        var antallVirkedagerSomBleForskøvet = periodeForskjøvet.disjoint(ikkeFlyttbareTimeline).stream()
            .mapToInt(p -> beregnAntallVirkedager(p.getFom(), p.getTom()) - 1)
            .sum(); // Teller ikke med tom
        return virkedagerSomSkalSkyves - antallVirkedagerSomBleForskøvet;
    }

    private List<OppgittPeriodeEntitet> leggTilFFFkonto(List<OppgittPeriodeEntitet> justertePerioder, List<OppgittPeriodeEntitet> oppgittePerioder) {
        var opprinneligeFFFperioder = oppgittePerioder.stream()
            .filter(p -> p.getPeriodeType().equals(FORELDREPENGER_FØR_FØDSEL))
            .toList();
        if (opprinneligeFFFperioder.isEmpty()) {
            return justertePerioder;
        }

        var antallTilgjengeligeFFFDager = antallVirkedager(opprinneligeFFFperioder);
        var intervalForbeholdMorForeldrepengerFørFødsel = new LocalDateInterval(TidsperiodeForbeholdtMor.fraOgMed(nyFamiliehendelse), nyFamiliehendelse.minusDays(1));
        var oppgittTimelineUtenUtsettelserOgOpphold = tilLocalDateTimeLine(oppgittePerioder.stream().filter(p -> !(p.isUtsettelse() || p.isOpphold())).toList());
        var perioderHvorFFFKanFlyttesTil = oppgittTimelineUtenUtsettelserOgOpphold.intersection(intervalForbeholdMorForeldrepengerFørFødsel);

        var justerteFFFPerioder = new ArrayList<OppgittPeriodeEntitet>();
        for (var seg : perioderHvorFFFKanFlyttesTil.stream().sorted(Comparator.comparing(LocalDateSegment::getFom, Comparator.reverseOrder())).toList()) { // Fyller på fra venstre til høyre til enten det er brukt opp eller ikke plass lenger
            if (antallTilgjengeligeFFFDager == 0) {
                break;
            }

            if (beregnAntallVirkedager(seg.getFom(), seg.getTom()) <= antallTilgjengeligeFFFDager) {
                antallTilgjengeligeFFFDager -= beregnAntallVirkedager(seg.getFom(), seg.getTom());
                justerteFFFPerioder.add(OppgittPeriodeBuilder.fraEksisterende(opprinneligeFFFperioder.getFirst())
                    .medPeriode(flyttFraHelgTilMandag(seg.getFom()), flyttFraHelgTilFredag(seg.getTom()))
                    .build());
            } else {
                // Ikke nok tilgjengelige dager for FFF for å fylle hele perioden
                justerteFFFPerioder.add(OppgittPeriodeBuilder.fraEksisterende(opprinneligeFFFperioder.getFirst())
                    .medPeriode(minusVirkedager(seg.getTom(), antallTilgjengeligeFFFDager), seg.getTom())
                    .build());
                antallTilgjengeligeFFFDager = 0;
            }
        }

        var antallTapteFFF = antallVirkedager(opprinneligeFFFperioder) - antallVirkedager(justerteFFFPerioder);
        if (antallTapteFFF > 0) {
            LOG.info("Justering ved fødsel før termin førte til at mor tapte {} dager med FFF", antallTapteFFF);
        }

        var resultat = new ArrayList<>(justertePerioder);
        resultat.addAll(justerteFFFPerioder);
        return sorterEtterFom(resultat);
    }

    private static LocalDate minusVirkedager(LocalDate dato, int virkedager) {
        while(virkedager > 0) {
            dato = minusVirkedag(dato);
            --virkedager;
        }
        return dato;
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

    private static List<OppgittPeriodeEntitet> hullFra(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var oppgittTimeline = tilLocalDateTimeLine(oppgittePerioder);
        var sjekkPeriode = new LocalDateTimeline<>(oppgittTimeline.getMinLocalDate(), oppgittTimeline.getMaxLocalDate(), true);
        return sjekkPeriode.disjoint(oppgittTimeline).stream()
            .map(seg -> new JusterPeriodeHull(seg.getFom(), seg.getTom()))
            .map(OppgittPeriodeEntitet.class::cast)
            .toList();
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeVenstre(OppgittPeriodeEntitet oppgittPeriode, int antallVirkedagerSomSkalSkyves, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        var gjeldendePeriode = oppgittPeriode;
        var antallVirkedager = beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());

        while (antallVirkedager > 0) {
            var nyFom = finnNyFomVedFlyttingVenstre(gjeldendePeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            var nyTom = flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriode(nyFom, antallVirkedager, ikkeFlyttbarePerioder, false);
            var nyPeriode = kopier(gjeldendePeriode, nyFom, nyTom);
            resultat.add(nyPeriode);

            antallVirkedager -= beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom());
            if (antallVirkedager > 0) {
                var fomRest = Virkedager.plusVirkedager(gjeldendePeriode.getFom(), beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom()));
                var tomRest = gjeldendePeriode.getTom();
                gjeldendePeriode = kopier(oppgittPeriode, fomRest, tomRest);
            }
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
            if (periodeSomIkkeErMødrekvoteEllerForeldrepengerHavnerInnenforUkeneForbeholdtMorEtterFødsel(oppgittPeriode, nyFom)) {
                return sisteLedigVirkedag; // Sjekkes først siden vi ikke ønsker å justere lenger inn i perioden forbeholdt mor
            }

            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom)) {
                i++;
                sisteLedigVirkedag = nyFom;
            }

            if (nyFom.isEqual(nyFamiliehendelse)) { // Må være etter erLedigVirkedager for å få med siste ledige virkedag i forskyvningen
                return sisteLedigVirkedag;
            }
        }
        return sisteLedigVirkedag;
    }

    private boolean periodeSomIkkeErMødrekvoteEllerForeldrepengerHavnerInnenforUkeneForbeholdtMorEtterFødsel(OppgittPeriodeEntitet oppgittPeriode, LocalDate nyFom) {
        if (oppgittPeriode.getPeriodeType().equals(MØDREKVOTE) || oppgittPeriode.getPeriodeType().equals(FORELDREPENGER)) {
            return false;
        }

        return !nyFom.isAfter(TidsperiodeForbeholdtMor.tilOgMed(nyFamiliehendelse));
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeHøyre(OppgittPeriodeEntitet oppgittPeriode, int antallVirkedagerSomSkalSkyves, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        var gjeldendePeriode = oppgittPeriode;
        var antallVirkedager = beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());

        while (antallVirkedager > 0) {
            var nyFom = finnNyFomVedFlyttingTilHøyre(gjeldendePeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            var nyTom = flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriode(nyFom, antallVirkedager, ikkeFlyttbarePerioder, true);
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

    private static LocalDateTimeline<OppgittPeriodeEntitet> tilLocalDateTimeLine(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return new LocalDateTimeline<>(oppgittePerioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p)).toList());
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

    private LocalDate flyttTomDatoAntallVirkedagerEllerFremTilIkkeFlyttbarPeriode(LocalDate initiellTom, int antallVirkedager, List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, boolean skalJustereInnIPeriodeForbeholdtMor) {
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
        var periodeMellomTomForbeholdtMorFørOgEtterFødsel = new LocalDateInterval(fraDato, tilDato);
        var allHull = new LocalDateTimeline<>(ikkeFlyttbarePerioder.stream()
            .filter(JusterPeriodeHull.class::isInstance)
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());
        return allHull.intersection(periodeMellomTomForbeholdtMorFørOgEtterFødsel);
    }

    private List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var hull = hullFra(oppgittePerioder);
        if (nyFamiliehendelse.isBefore(gammelFamiliehendelse)) {
            var hullTidslinje = tilLocalDateTimeLine(hull);
            var intervallEtterTermin = new LocalDateInterval(gammelFamiliehendelse, LocalDateInterval.TIDENES_ENDE);
            hull = hullTidslinje.intersection(intervallEtterTermin).stream()
                .filter(MorsJustering::harVirkedager)
                .map(seg -> kopier(seg.getValue(), seg.getFom(), seg.getTom()))
                .toList();
        }

        var ikkeFlyttbarePerioderFraOppgittePerioder = oppgittePerioder.stream()
            .filter(periode -> !erPeriodeFlyttbar(periode))
            .toList();
        return Stream.concat(hull.stream(), ikkeFlyttbarePerioderFraOppgittePerioder.stream())
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .toList();
    }

    private List<OppgittPeriodeEntitet> fjernPerioderEtterSisteSøkteDato(List<OppgittPeriodeEntitet> justertePerioder, LocalDate sisteSøkteDato) {
        return sorterEtterFom(justertePerioder).stream().filter(p -> p.getFom().isBefore(sisteSøkteDato) || p.getFom().isEqual(sisteSøkteDato)).map(p -> {
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
    private static boolean erPeriodeFlyttbar(OppgittPeriodeEntitet periode) {
        if (periode.isUtsettelse() || periode.isOpphold() || periode.isGradert()) {
            return false;
        }
        if (periode.isSamtidigUttak()) {
            //Mødrekvote med samtidig uttak skal flyttes
            return MØDREKVOTE.equals(periode.getPeriodeType());
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
