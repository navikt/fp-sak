package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;


import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.finnesOverlapp;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.kopier;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.sorterEtterFom;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

final class JusterFordelingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(JusterFordelingTjeneste.class);

    private JusterFordelingTjeneste() {
    }

    /**
     * Skyver perioder basert på antall virkedager i mellom familiehendelsene.
     * Gradering, utsettelse, opphold og hull mellom perioder flyttes ikke.
     */
    static List<OppgittPeriodeEntitet> justerForFamiliehendelse(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                LocalDate gammelFamiliehendelse,
                                                                LocalDate nyFamiliehendelse,
                                                                RelasjonsRolleType relasjonsRolleType,
                                                                boolean ønskerJustertVedFødsel) {
        var justert = sorterEtterFom(oppgittePerioder);
        if (finnesOverlapp(oppgittePerioder)) {
            LOG.warn("Finnes overlapp i oppgitte perioder fra søknad. Sannsynligvis feil i søknadsdialogen. "
                + "Hvis periodene ikke kan slås sammen faller behandlingen ut til manuell behandling");
            //Justering støtter ikke overlapp
        } else if (gammelFamiliehendelse != null && nyFamiliehendelse != null) {
            //flytter til mandag
            gammelFamiliehendelse = flyttFraHelgTilMandag(gammelFamiliehendelse);
            nyFamiliehendelse = flyttFraHelgTilMandag(nyFamiliehendelse);
            if (!gammelFamiliehendelse.equals(nyFamiliehendelse)) {
                justert = justerVedEndringAvFamilieHendelse(oppgittePerioder, gammelFamiliehendelse, nyFamiliehendelse, relasjonsRolleType, ønskerJustertVedFødsel);
            }
            exceptionHvisOverlapp(justert);
        }
        return slåSammenLikePerioder(justert);
    }

    private static List<OppgittPeriodeEntitet> justerVedEndringAvFamilieHendelse(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                                 LocalDate gammelFamiliehendelse,
                                                                                 LocalDate nyFamiliehendelse,
                                                                                 RelasjonsRolleType relasjonsRolleType,
                                                                                 boolean ønskerJustertVedFødsel) {
        var oppgittPerioder = sorterEtterFom(oppgittePerioder);
        return sorterEtterFom(justerPerioder(oppgittPerioder, gammelFamiliehendelse, nyFamiliehendelse, relasjonsRolleType, ønskerJustertVedFødsel));
    }

    private static List<OppgittPeriodeEntitet> justerPerioder(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                              LocalDate gammelFamiliehendelse,
                                                              LocalDate nyFamiliehendelse,
                                                              RelasjonsRolleType relasjonsRolleType,
                                                              boolean ønskerJustertVedFødsel) {
        var fjernetHelgerFraStartOgSluttAvPerioder = fjernHelgerFraStartOgSlutt(oppgittePerioder);
        var splitetPåFødsel = splitPåDato(gammelFamiliehendelse, fjernetHelgerFraStartOgSluttAvPerioder);
        var justering = RelasjonsRolleType.erMor(relasjonsRolleType) ?
            new MorsJustering(gammelFamiliehendelse, nyFamiliehendelse) :
            new FarsJustering(gammelFamiliehendelse, nyFamiliehendelse, ønskerJustertVedFødsel);
        if (nyFamiliehendelse.isAfter(gammelFamiliehendelse)) {
            return justering.justerVedFødselEtterTermin(splitetPåFødsel);
        }
        return justering.justerVedFødselFørTermin(splitetPåFødsel);
    }

    private static List<OppgittPeriodeEntitet> splitPåDato(LocalDate dato,
                                                           List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().flatMap(p -> {
            if (p.getTidsperiode().inkluderer(dato) && p.getFom().isBefore(dato)) {
                var p1 = kopier(p, p.getFom(), dato.minusDays(1));
                var p2 = kopier(p, dato, p.getTom());
                return Stream.of(p1, p2);
            }
            return Stream.of(p);
        }).toList();
    }

    private static List<OppgittPeriodeEntitet> fjernHelgerFraStartOgSlutt(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream()
            .filter(p -> !erHelg(p))
            .map(p -> kopier(p, flyttFraHelgTilMandag(p.getFom()), flyttFraHelgTilFredag(p.getTom())))
            .toList();
    }

    static LocalDate flyttFraHelgTilFredag(LocalDate dato) {
        if (erLørdag(dato)) {
            return dato.minusDays(1);
        }
        if (erSøndag(dato)) {
            return dato.minusDays(2);
        }
        return dato;
    }

    static boolean erHelg(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) == 0;
    }

    private static void exceptionHvisOverlapp(List<OppgittPeriodeEntitet> perioder) {
        if (finnesOverlapp(perioder)) {
            throw new IllegalStateException("Utviklerfeil: Overlappende perioder etter justering " + perioder);
        }
    }

    static LocalDate flyttFraHelgTilMandag(LocalDate dato) {
        if (erLørdag(dato)) {
            return dato.plusDays(2);
        }
        if (erSøndag(dato)) {
            return dato.plusDays(1);
        }
        return dato;
    }

    static boolean erHelg(LocalDate dato) {
        return erLørdag(dato) || erSøndag(dato);
    }

    private static boolean erSøndag(LocalDate dato) {
        return dato.getDayOfWeek().equals(DayOfWeek.SUNDAY);
    }

    private static boolean erLørdag(LocalDate dato) {
        return dato.getDayOfWeek().equals(DayOfWeek.SATURDAY);
    }


}
