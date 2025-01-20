package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilFredag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilMandag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

public class OppgittPeriodeUtil {

    private OppgittPeriodeUtil() {
        //Forhindrer instanser
    }

    public static boolean finnesOverlapp(List<OppgittPeriodeEntitet> oppgittPerioder) {
        for (var i = 0; i < oppgittPerioder.size(); i++) {
            for (var j = i + 1; j < oppgittPerioder.size(); j++) {
                if (overlapper(oppgittPerioder.get(i), oppgittPerioder.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean overlapper(OppgittPeriodeEntitet p1, OppgittPeriodeEntitet p2) {
        return new SimpleLocalDateInterval(p1.getFom(), p1.getTom()).overlapper(new SimpleLocalDateInterval(p2.getFom(), p2.getTom()));
    }

    static List<OppgittPeriodeEntitet> sorterEtterFom(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).toList();
    }

    /**
     * Finn første dato fra søknad som ikke er en utsettelse.
     *
     * @param oppgittePerioder
     * @return første dato fra søknad som ikke er en utsettelse.
     */
    static Optional<LocalDate> finnFørsteSøkteUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream()
            .filter(p -> Årsak.UKJENT.equals(p.getÅrsak()) || !p.isOpphold())
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    static Optional<LocalDate> finnFørsteSøknadsdato(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder());
    }

    public static List<OppgittPeriodeEntitet> slåSammenLikePerioder(List<OppgittPeriodeEntitet> perioder) {
        return slåSammenLikePerioder(perioder, false);
    }

    public static List<OppgittPeriodeEntitet> slåSammenLikePerioder(List<OppgittPeriodeEntitet> perioder, boolean ignorerTidligstMottattDato) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();
        perioder = sorterEtterFom(perioder); // Må være sortert

        var i = 0;
        while (i < perioder.size()) {
            var j = i + 1;
            var slåttSammen = perioder.get(i);
            if (i < perioder.size() - 1) {
                //Hvis ikke hull mellom periodene skal vi se om de er like for å så slå de sammen
                while (j < perioder.size()) {
                    var nestePeriode = perioder.get(j);
                    if (!erHullMellom(slåttSammen.getTom(), nestePeriode.getFom()) && erLikBortsettFraTidsperiode(slåttSammen, nestePeriode,
                        ignorerTidligstMottattDato)) {
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

    private static boolean erLikBortsettFraTidsperiode(OppgittPeriodeEntitet periode1,
                                                       OppgittPeriodeEntitet periode2,
                                                       boolean ignorerTidligstMottattDato) {
        //begrunnelse ikke viktig å se på
        return Objects.equals(periode1.getGraderingAktivitetType(), periode2.getGraderingAktivitetType()) &&
            Objects.equals(periode1.isFlerbarnsdager(), periode2.isFlerbarnsdager()) &&
            Objects.equals(periode1.isSamtidigUttak(), periode2.isSamtidigUttak()) &&
            Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver()) &&
            Objects.equals(periode1.getMorsAktivitet(), periode2.getMorsAktivitet()) &&
            Objects.equals(periode1.isVedtaksperiode(), periode2.isVedtaksperiode()) &&
            Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType()) &&
            Objects.equals(periode1.getDokumentasjonVurdering(), periode2.getDokumentasjonVurdering()) &&
            Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent()) &&
            Objects.equals(periode1.getMottattDato(), periode2.getMottattDato()) &&
            (ignorerTidligstMottattDato || Objects.equals(periode1.getTidligstMottattDato(), periode2.getTidligstMottattDato())) &&
            Objects.equals(periode1.getÅrsak(), periode2.getÅrsak()) &&
            Objects.equals(periode1.getArbeidsprosentSomStillingsprosent(), periode2.getArbeidsprosentSomStillingsprosent());
    }

    private static OppgittPeriodeEntitet slåSammen(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return kopier(periode1, periode1.getFom(), periode2.getTom());
    }

    static boolean erHullMellom(LocalDate date1, LocalDate date2) {
        return Virkedager.plusVirkedager(date1, 1).isBefore(date2);
    }

    static OppgittPeriodeEntitet kopier(OppgittPeriodeEntitet oppgittPeriode, LocalDate nyFom, LocalDate nyTom) {
        var nyTidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(nyFom, nyTom);
        nyTidsperiode = nyTidsperiode.erHelg() ? nyTidsperiode : DatoIntervallEntitet.fraOgMedTilOgMed(flyttFraHelgTilMandag(nyFom), flyttFraHelgTilFredag(nyTom));
        return OppgittPeriodeBuilder.fraEksisterende(oppgittPeriode)
            .medPeriode(nyTidsperiode)
            .build();
    }
}
