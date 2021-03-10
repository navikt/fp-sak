package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;


import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.sorterEtterFom;
import static no.nav.foreldrepenger.regler.uttak.felles.Virkedager.beregnAntallVirkedager;
import static no.nav.foreldrepenger.regler.uttak.felles.Virkedager.plusVirkedager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;

class JusterFordelingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(JusterFordelingTjeneste.class);

    /**
     * Skyver perioder basert på antall virkedager i mellom familiehendelsene.
     * Gradering, utsettelse, opphold og hull mellom perioder flyttes ikke.
     */
    List<OppgittPeriodeEntitet> juster(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse, LocalDate nyFamiliehendelse) {
        List<OppgittPeriodeEntitet> justert = sorterEtterFom(oppgittePerioder);
        if (finnesOverlapp(oppgittePerioder)) {
            LOG.warn("Finnes overlapp i oppgitte perioder fra søknad. Sannsynligvis feil i søknadsdialogen. "
                + "Hvis periodene ikke kan slås sammen faller behandlingen ut til manuell behandling");
            //Justering støtter ikke overlapp
        } else if (gammelFamiliehendelse != null && nyFamiliehendelse != null) {
            //flytter til mandag
            gammelFamiliehendelse = flyttFraHelgTilMandag(gammelFamiliehendelse);
            nyFamiliehendelse = flyttFraHelgTilMandag(nyFamiliehendelse);
            if (!gammelFamiliehendelse.equals(nyFamiliehendelse)) {
                justert = justerVedEndringAvFamilieHendelse(oppgittePerioder, gammelFamiliehendelse, nyFamiliehendelse);
            }
            exceptionHvisOverlapp(justert);
        }
        return slåSammenLikePerioder(justert);
    }

    private List<OppgittPeriodeEntitet> justerVedEndringAvFamilieHendelse(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse, LocalDate nyFamiliehendelse) {
        List<OppgittPeriodeEntitet> oppgittPerioder = sorterEtterFom(oppgittePerioder);
        return sorterEtterFom(justerPerioder(oppgittPerioder, gammelFamiliehendelse, nyFamiliehendelse));
    }

    private List<OppgittPeriodeEntitet> slåSammenLikePerioder(List<OppgittPeriodeEntitet> perioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();

        int i = 0;
        while (i < perioder.size()) {
            int j = i + 1;
            OppgittPeriodeEntitet slåttSammen = perioder.get(i);
            if (i < perioder.size() - 1) {
                //Hvis ikke hull mellom periodene skal vi se om de er like for å så slå de sammen
                while (j < perioder.size()) {
                    OppgittPeriodeEntitet nestePeriode = perioder.get(j);
                    if (!erHullMellom(slåttSammen.getTom(), nestePeriode.getFom()) && erLikBortsettFraTidsperiode(slåttSammen, nestePeriode)) {
                        slåttSammen = slåSammen(slåttSammen, nestePeriode);
                    } else {
                        break;
                    }
                    j++;
                }
            }
            resultat.add(slåttSammen);
            i = j;
        }
        return resultat;
    }

    private boolean erLikBortsettFraTidsperiode(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        //begrunnelse ikke viktig å se på
        return Objects.equals(periode1.isArbeidstaker(), periode2.isArbeidstaker()) &&
            Objects.equals(periode1.isFrilanser(), periode2.isFrilanser()) &&
            Objects.equals(periode1.isSelvstendig(), periode2.isSelvstendig()) &&
            Objects.equals(periode1.isFlerbarnsdager(), periode2.isFlerbarnsdager()) &&
            Objects.equals(periode1.isSamtidigUttak(), periode2.isSamtidigUttak()) &&
            Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver()) &&
            Objects.equals(periode1.getMorsAktivitet(), periode2.getMorsAktivitet()) &&
            Objects.equals(periode1.isVedtaksperiode(), periode2.isVedtaksperiode()) &&
            Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType()) &&
            Objects.equals(periode1.getPeriodeVurderingType(), periode2.getPeriodeVurderingType()) &&
            Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent()) &&
            Objects.equals(periode1.getMottattDato(), periode2.getMottattDato()) &&
            Objects.equals(periode1.getÅrsak(), periode2.getÅrsak()) &&
            Objects.equals(periode1.getArbeidsprosent(), periode2.getArbeidsprosent());
    }

    private OppgittPeriodeEntitet slåSammen(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return kopier(periode1, periode1.getFom(), periode2.getTom());
    }

    private LocalDate flyttFraHelgTilMandag(LocalDate dato) {
        if (erLørdag(dato)) {
            return dato.plusDays(2);
        } else if (erSøndag(dato)) {
            return dato.plusDays(1);
        }
        return dato;
    }

    private boolean erHelg(LocalDate dato) {
        return erLørdag(dato) || erSøndag(dato);
    }

    private boolean erSøndag(LocalDate dato) {
        return dato.getDayOfWeek().equals(DayOfWeek.SUNDAY);
    }

    private boolean erLørdag(LocalDate dato) {
        return dato.getDayOfWeek().equals(DayOfWeek.SATURDAY);
    }

    private List<OppgittPeriodeEntitet> justerPerioder(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse, LocalDate nyFamiliehendelse) {
        List<OppgittPeriodeEntitet> fjernetHelgerFraStartOgSluttAvPerioder = fjernHelgerFraStartOgSlutt(oppgittePerioder);
        List<OppgittPeriodeEntitet> justerte;
        if (nyFamiliehendelse.isAfter(gammelFamiliehendelse)) {
            justerte = justerVedFødselEtterTermin(fjernetHelgerFraStartOgSluttAvPerioder, gammelFamiliehendelse, nyFamiliehendelse);
        } else {
            justerte = justerVedFødselFørTermin(fjernetHelgerFraStartOgSluttAvPerioder, gammelFamiliehendelse, nyFamiliehendelse);
        }

        return fjernHullPerioder(justerte);
    }

    private List<OppgittPeriodeEntitet> fjernHullPerioder(List<OppgittPeriodeEntitet> oppgittPerioder) {
        return oppgittPerioder.stream().filter(p -> !(p instanceof JusterPeriodeHull)).collect(Collectors.toList()); //NOSONAR
    }

    private List<OppgittPeriodeEntitet> fjernHelgerFraStartOgSlutt(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream()
            .filter(p -> !erHelg(p))
            .map(p -> kopier(p, flyttFraHelgTilMandag(p.getFom()), flyttFraHelgTilFredag(p.getTom())))
            .collect(Collectors.toList());
    }

    private boolean erHelg(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) == 0;
    }

    private LocalDate flyttFraHelgTilFredag(LocalDate dato) {
        if (erLørdag(dato)) {
            return dato.minusDays(1);
        } else if (erSøndag(dato)) {
            return dato.minusDays(2);
        }
        return dato;
    }

    private List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse, LocalDate nyFamiliehendelse) {
        List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        int virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);

        List<OppgittPeriodeEntitet> justertePerioder = new ArrayList<>();
        for (OppgittPeriodeEntitet oppgittPeriode : oppgittePerioder) {
            List<OppgittPeriodeEntitet> justert = flyttPeriodeTilVenstre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            justertePerioder.addAll(justert);
        }
        List<OppgittPeriodeEntitet> sortert = sorterEtterFom(justertePerioder);
        if (førsteUttaksdatoErFlyttet(oppgittePerioder, sortert)) {
            if (søktOmPerioderFørFamiliehendelse(oppgittePerioder, gammelFamiliehendelse)) {
                sortert = beholdStartdatoForUttak(sortert, oppgittePerioder.get(0).getFom(), nyFamiliehendelse);
            }
            if (søktOmPerioderEtterFamiliehendelse(oppgittePerioder, gammelFamiliehendelse)) {
                sortert = beholdSluttdatoForUttak(sortert, oppgittePerioder.get(oppgittePerioder.size() - 1).getTom());
            }
            sortert = fyllHullSkaptAvIkkeFlyttbarePerioder(sortert, oppgittePerioder);
        }
        return sortert;
    }

    private List<OppgittPeriodeEntitet> fyllHullSkaptAvIkkeFlyttbarePerioder(List<OppgittPeriodeEntitet> justertePerioder,
                                                                             List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();

        for (int i = 0; i < justertePerioder.size() - 1; i++) {
            resultat.add(justertePerioder.get(i));
            if (erHullMellom(justertePerioder.get(i).getTom(), justertePerioder.get(i + 1).getFom())) {
                LocalDate førsteVirkedagIHull = plusVirkedager(justertePerioder.get(i).getTom(), 1);
                LocalDate sisteVirkedagIHull = minusVirkedag(justertePerioder.get(i + 1).getFom());
                //sjekker om hullet vi finner ikke finnes i oppgitteperioder (hvis det er søkt med hull i perioden skal vi ikke fylle)
                if (!hullFinnesIOppgittePeriode(oppgittePerioder, førsteVirkedagIHull, sisteVirkedagIHull)) {
                    OppgittPeriodeEntitet sisteFlyttbarPeriode = sisteFlyttbarePeriode(resultat);
                    resultat.add(kopier(sisteFlyttbarPeriode, førsteVirkedagIHull, sisteVirkedagIHull));
                }
            }
        }
        resultat.add(justertePerioder.get(justertePerioder.size() - 1));
        return resultat;
    }

    private OppgittPeriodeEntitet sisteFlyttbarePeriode(List<OppgittPeriodeEntitet> resultat) {
        List<OppgittPeriodeEntitet> flyttbarePerioder = flyttbarePerioder(resultat);
        if (flyttbarePerioder.size() == 0) {
            throw new IllegalStateException("Forventer minst en flyttbar periode før hull");
        }
        return flyttbarePerioder.get(flyttbarePerioder.size() - 1);
    }

    private boolean hullFinnesIOppgittePeriode(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate hullFom, LocalDate hullTom) {
        List<OppgittPeriodeEntitet> perioderMedHull = hullPerioder(oppgittePerioder);
        return perioderMedHull.stream().anyMatch(p -> overlapper(p, hullFom) || overlapper(p, hullTom));
    }

    private List<OppgittPeriodeEntitet> flyttbarePerioder(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().filter(this::erPeriodeFlyttbar).collect(Collectors.toList());
    }

    private boolean erHullMellom(LocalDate date1, LocalDate date2) {
        return Virkedager.plusVirkedager(date1, 1).isBefore(date2);
    }

    private int beregnAntallLedigeVirkedager(LocalDate dato1,
                                             LocalDate dato2,
                                             List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
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
        int ledigeVirkedager = 0;
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
        OppgittPeriodeEntitet sisteJustertePeriode = justertePerioder.get(justertePerioder.size() - 1);
        if (erPeriodeFlyttbar(sisteJustertePeriode)) {
            resultat.set(justertePerioder.size() - 1, kopier(sisteJustertePeriode, sisteJustertePeriode.getFom(), sluttdato));
        } else if (!sisteJustertePeriode.getTom().isEqual(sluttdato)){
            OppgittPeriodeEntitet sisteFlyttbarePeriode = sisteFlyttbarePeriode(justertePerioder);
            resultat.add(kopier(sisteFlyttbarePeriode, plusVirkedager(sisteJustertePeriode.getTom(), 1), sluttdato));
        }
        return resultat;
    }

    private boolean søktOmPerioderFørFamiliehendelse(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse) {
        return oppgittePerioder.get(0).getFom().isBefore(gammelFamiliehendelse);
    }

    private List<OppgittPeriodeEntitet> beholdStartdatoForUttak(List<OppgittPeriodeEntitet> justertePerioder, LocalDate startdato, LocalDate nyFamiliehendelse) {
        return justertePerioder.stream()
            .filter(p -> !p.getTom().isBefore(startdato))
            .map(p -> {
                if (p.getFom().isBefore(nyFamiliehendelse) && overlapper(p, startdato)) {
                    return kopier(p, startdato, p.getTom());
                } else {
                    return p;
                }
            }).collect(Collectors.toList());
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeTilVenstre(OppgittPeriodeEntitet oppgittPeriode,
                                                        int antallVirkedagerSomSkalSkyves,
                                                        List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        if (!erPeriodeFlyttbar(oppgittPeriode)) {
            return Collections.singletonList(kopier(oppgittPeriode, oppgittPeriode.getFom(), oppgittPeriode.getTom()));
        }

        LocalDate nyFom = finnNyFomVedFlyttingTilVenstre(oppgittPeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);

        LocalDate nyTom = nyFom;
        int i = 0;
        boolean knekkFunnet = false;
        int antallVirkedager = beregnAntallLedigeVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom(), ikkeFlyttbarePerioder);
        //flytter en og en dag
        while (i < antallVirkedager && !knekkFunnet) {
            if (erLedigVirkedager(ikkeFlyttbarePerioder, plusVirkedager(nyTom, 1))) {
                nyTom = plusVirkedager(nyTom, 1);
                i++;
            } else {
                knekkFunnet = true;
            }
        }

        OppgittPeriodeEntitet nyPeriode = kopier(oppgittPeriode, nyFom, nyTom);
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        resultat.add(nyPeriode);
        if (knekkFunnet) {
            int virkedagerPeriodeFørKnekk = beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom());
            LocalDate fom = plusVirkedager(oppgittPeriode.getFom(), virkedagerPeriodeFørKnekk);
            List<OppgittPeriodeEntitet> perioderEtterKnekk = flyttPeriodeTilVenstre(kopier(oppgittPeriode, fom, oppgittPeriode.getTom()), antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            resultat.addAll(perioderEtterKnekk);
        }

        return resultat;
    }

    private LocalDate finnNyFomVedFlyttingTilVenstre(OppgittPeriodeEntitet oppgittPeriode,
                                                     int antallVirkedagerSomSkalSkyves,
                                                     List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        LocalDate nyFom = oppgittPeriode.getFom();
        int i = 0;
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

    private List<OppgittPeriodeEntitet> justerVedFødselEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate gammelFamiliehendelse, LocalDate nyFamiliehendelse) {
        List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder = ikkeFlyttbarePerioder(oppgittePerioder);
        int virkedagerSomSkalSkyves = beregnAntallLedigeVirkedager(gammelFamiliehendelse, nyFamiliehendelse, ikkeFlyttbarePerioder);

        List<OppgittPeriodeEntitet> justertePerioder = new ArrayList<>();
        for (OppgittPeriodeEntitet oppgittPeriode : oppgittePerioder) {
            List<OppgittPeriodeEntitet> justert = flyttPeriodeTilHøyre(oppgittPeriode, virkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            justertePerioder.addAll(justert);
        }
        justertePerioder = sorterEtterFom(justertePerioder);
        if (førsteUttaksdatoErFlyttet(oppgittePerioder, justertePerioder)) {
            OppgittPeriodeEntitet ekstraPeriode = lagEkstraPeriodeFraOpprinneligUttaksdato(oppgittePerioder, justertePerioder);
            justertePerioder.add(0, ekstraPeriode);
            justertePerioder = fjernPerioderEtterSisteSøkteDato(justertePerioder, oppgittePerioder.get(oppgittePerioder.size() - 1).getTom());
        }
        return justertePerioder;
    }

    private OppgittPeriodeEntitet lagEkstraPeriodeFraOpprinneligUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                           List<OppgittPeriodeEntitet> justertePerioder) {
        UttakPeriodeType uttakPeriodeType = finnUttakPeriodeType(justertePerioder);
        return OppgittPeriodeBuilder.ny()
            .medPeriodeType(uttakPeriodeType)
            .medPeriode(flyttFraHelgTilMandag(oppgittePerioder.get(0).getFom()), flyttFraHelgTilFredag(justertePerioder.get(0).getFom().minusDays(1)))
            .medMottattDato(oppgittePerioder.get(0).getMottattDato())
            .build();
    }

    private boolean førsteUttaksdatoErFlyttet(List<OppgittPeriodeEntitet> oppgittePerioder, List<OppgittPeriodeEntitet> justertePerioder) {
        return !justertePerioder.get(0).getFom().isEqual(oppgittePerioder.get(0).getFom());
    }

    private List<OppgittPeriodeEntitet> flyttPeriodeTilHøyre(OppgittPeriodeEntitet oppgittPeriode,
                                                      int antallVirkedagerSomSkalSkyves,
                                                      List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        if (!erPeriodeFlyttbar(oppgittPeriode)) {
            return Collections.singletonList(kopier(oppgittPeriode, oppgittPeriode.getFom(), oppgittPeriode.getTom()));
        }

        LocalDate nyFom = finnNyFomVedFlyttingTilHøyre(oppgittPeriode, antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);

        LocalDate nyTom = nyFom;
        int i = 0;
        boolean knekkFunnet = false;
        int antallVirkedager = beregnAntallVirkedager(oppgittPeriode.getFom(), oppgittPeriode.getTom());
        //flytter en og en dag
        while (i < antallVirkedager - 1 && !knekkFunnet) {
            if (erLedigVirkedager(ikkeFlyttbarePerioder, plusVirkedager(nyTom, 1))) {
                nyTom = plusVirkedager(nyTom, 1);
                i++;
            } else {
                knekkFunnet = true;
            }
        }

        OppgittPeriodeEntitet nyPeriode = kopier(oppgittPeriode, nyFom, nyTom);
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        resultat.add(nyPeriode);
        if (knekkFunnet) {
            int virkedagerPeriodeFørKnekk = beregnAntallVirkedager(nyPeriode.getFom(), nyPeriode.getTom());
            LocalDate fom = plusVirkedager(oppgittPeriode.getFom(), virkedagerPeriodeFørKnekk);
            List<OppgittPeriodeEntitet> perioderEtterKnekk = flyttPeriodeTilHøyre(kopier(oppgittPeriode, fom, oppgittPeriode.getTom()),
                antallVirkedagerSomSkalSkyves, ikkeFlyttbarePerioder);
            resultat.addAll(perioderEtterKnekk);
        }

        return resultat;
    }

    private LocalDate finnNyFomVedFlyttingTilHøyre(OppgittPeriodeEntitet oppgittPeriode,
                                                   int antallVirkedagerSomSkalSkyves,
                                                   List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder) {
        LocalDate nyFom = oppgittPeriode.getFom();
        int i = 0;
        //flytter en og en dag
        while (i < antallVirkedagerSomSkalSkyves) {
            nyFom = plusVirkedager(nyFom, 1);
            if (erLedigVirkedager(ikkeFlyttbarePerioder, nyFom)) {
                i++;
            }
        }
        return nyFom;
    }

    private boolean erLedigVirkedager(List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder, LocalDate nyFom) {
        return finnOverlappendePeriode(nyFom, ikkeFlyttbarePerioder).isEmpty();
    }

    private List<OppgittPeriodeEntitet> ikkeFlyttbarePerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> ikkeFlyttbare = oppgittePerioder.stream()
            .filter(periode -> !erPeriodeFlyttbar(periode))
            .collect(Collectors.toList());
        ikkeFlyttbare.addAll(hullPerioder(oppgittePerioder));
        return ikkeFlyttbare;
    }

    private List<OppgittPeriodeEntitet> hullPerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        List<OppgittPeriodeEntitet> hull = new ArrayList<>();
        for (int i = 1; i < oppgittePerioder.size(); i++) {
            LocalDate nesteVirkedag = plusVirkedager(oppgittePerioder.get(i - 1).getTom(), 1);
            LocalDate startDatoNestePeriode = oppgittePerioder.get(i).getFom();
            if (!nesteVirkedag.isEqual(startDatoNestePeriode)) {
                hull.add(new JusterPeriodeHull(nesteVirkedag, Virkedager.plusVirkedager(nesteVirkedag,
                    beregnAntallVirkedager(nesteVirkedag, startDatoNestePeriode) - 2)));
            }
        }
        return hull;
    }

    private Optional<OppgittPeriodeEntitet> finnOverlappendePeriode(LocalDate dato, List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().filter(periode -> (periode.getFom().equals(dato) || periode.getFom().isBefore(dato)
            && (periode.getTom().equals(dato) || periode.getTom().isAfter(dato)))).findFirst();
    }

    private OppgittPeriodeEntitet kopier(OppgittPeriodeEntitet oppgittPeriode, LocalDate nyFom, LocalDate nyTom) {
        return OppgittPeriodeBuilder.fraEksisterende(oppgittPeriode).medPeriode(nyFom, nyTom).build();
    }

    private List<OppgittPeriodeEntitet> fjernPerioderEtterSisteSøkteDato(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate sisteSøkteDato) {
        return oppgittePerioder.stream().filter(p -> p.getFom().isBefore(sisteSøkteDato) || p.getFom().isEqual(sisteSøkteDato)).map(p -> {
            if (p.getTom().isAfter(sisteSøkteDato)) {
                return OppgittPeriodeBuilder.fraEksisterende(p).medPeriode(p.getFom(), sisteSøkteDato).build();
            }
            return p;
        }).collect(Collectors.toList());
    }

    private UttakPeriodeType finnUttakPeriodeType(List<OppgittPeriodeEntitet> oppgittPerioder) {
        for (OppgittPeriodeEntitet oppgittPeriode : oppgittPerioder) {
            if (oppgittPeriode.getPeriodeType().equals(UttakPeriodeType.FORELDREPENGER)) {
                return UttakPeriodeType.FORELDREPENGER;
            }
        }
        return UttakPeriodeType.FELLESPERIODE;
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

    private void exceptionHvisOverlapp(List<OppgittPeriodeEntitet> perioder) {
        if (finnesOverlapp(perioder)) {
            throw new IllegalStateException("Utviklerfeil: Overlappende perioder etter justering " + perioder);
        }
    }

    private boolean finnesOverlapp(List<OppgittPeriodeEntitet> oppgittPerioder) {
        for (int i = 0; i < oppgittPerioder.size(); i++) {
            for (int j = i + 1; j < oppgittPerioder.size(); j++) {
                if (overlapper(oppgittPerioder.get(i), oppgittPerioder.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean overlapper(OppgittPeriodeEntitet periode, LocalDate dato) {
        return periode.getFom().isEqual(dato) || periode.getTom().isEqual(dato) || (periode.getFom().isBefore(dato) && periode.getTom().isAfter(dato));
    }

    private boolean overlapper(OppgittPeriodeEntitet p1, OppgittPeriodeEntitet p2) {
        return new SimpleLocalDateInterval(p1.getFom(), p1.getTom()).overlapper(new SimpleLocalDateInterval(p2.getFom(), p2.getTom()));
    }

    /**
     * Intern bruk for å håndtere ikke søkte perioder som hull
     */
    private static class JusterPeriodeHull extends OppgittPeriodeEntitet {
        private final LocalDate fom;
        private final LocalDate tom;

        private JusterPeriodeHull(LocalDate fom, LocalDate tom) {
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
