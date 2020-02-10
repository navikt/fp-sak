package no.nav.foreldrepenger.domene.medlem;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.vedtak.konfig.Tid;

/**
 * Gjenbrukbar logikk for behandling av data fra MEDL.
 */
@ApplicationScoped
public class MedlemskapPerioderTjeneste {

    public MedlemskapPerioderTjeneste() {
    }

    // FP VK 2.13 Maskinell avklaring

    public boolean brukerMaskineltAvklartSomIkkeMedlem(PersonopplysningerAggregat personopplysningerAggregat,
                                                       Set<MedlemskapPerioderEntitet> medlemskapPerioder, LocalDate skjæringstidspunkt) {

        List<MedlemskapDekningType> dekningTyper = finnGyldigeDekningstyper(medlemskapPerioder, skjæringstidspunkt);

        // Premiss alternativ 1: Bruker er registert med dekningstype som er klassifisert som Ikke medlem
        boolean erPeriodeRegistrertSomIkkeMedlem = erRegistrertSomIkkeMedlem(dekningTyper);

        // Premiss alternativ 2: Bruker er registert med dekningstype "Unntatt", og ikke er bosatt med statsb. USA/PNG
        boolean erPeriodeRegistrertSomUnntatt = erRegistrertSomUnntatt(dekningTyper);
        boolean harStatsborgerskapUsaEllerPng = harStatsborgerskapUsaEllerPng(personopplysningerAggregat);

        boolean erIkkeUsaEllerPngOgUntatt = !harStatsborgerskapUsaEllerPng
            && erPeriodeRegistrertSomUnntatt;

        // Sammenstill premisser
        return erPeriodeRegistrertSomIkkeMedlem || erIkkeUsaEllerPngOgUntatt;
    }

    public boolean erRegistrertSomUnntatt(List<MedlemskapDekningType> dekningTyper) {
        return dekningTyper.stream()
            .anyMatch(MedlemskapDekningType.DEKNINGSTYPE_ER_MEDLEM_UNNTATT::contains);
    }

    public boolean erRegistrertSomIkkeMedlem(List<MedlemskapDekningType> dekningTyper) {
        return dekningTyper.stream()
            .anyMatch(MedlemskapDekningType.DEKNINGSTYPE_ER_IKKE_MEDLEM::contains);
    }

    // FP VK 2.2 Maskinell avklaring

    public boolean brukerMaskineltAvklartSomFrivilligEllerPliktigMedlem(Set<MedlemskapPerioderEntitet> medlemskapPerioder,
                                                                        LocalDate vurderingsdato) {
        List<MedlemskapDekningType> dekningTyper = finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return erRegistrertSomFrivilligMedlem(dekningTyper);
    }

    public boolean erRegistrertSomFrivilligMedlem(List<MedlemskapDekningType> dekningTyper) {
        return dekningTyper.stream()
            .anyMatch(MedlemskapDekningType.DEKNINGSTYPE_ER_FRIVILLIG_MEDLEM::contains);
    }

    public boolean erRegistrertSomAvklartMedlemskap(List<MedlemskapDekningType> dekningTyper) {
        return dekningTyper.stream()
            .anyMatch(MedlemskapDekningType.DEKNINGSTYPER::contains);
    }

    public boolean erRegistrertSomUavklartMedlemskap(List<MedlemskapDekningType> dekningTyper) {
        return dekningTyper.stream()
            .anyMatch(MedlemskapDekningType.DEKNINGSTYPE_ER_UAVKLART::contains);
    }

    public List<MedlemskapDekningType> finnGyldigeDekningstyper(Collection<MedlemskapPerioderEntitet> medlemskapPerioder,
                                                                LocalDate skjæringsdato) {
        return medlemskapPerioder.stream()
            .filter(periode -> erDatoInnenforLukketPeriode(periode.getFom(), periode.getTom(), skjæringsdato)
                && periode.getDekningType() != null
                && !MedlemskapKildeType.LAANEKASSEN.equals(periode.getKildeType())
                && !MedlemskapType.UNDER_AVKLARING.equals(periode.getMedlemskapType()))
            .map(MedlemskapPerioderEntitet::getDekningType)
            .collect(toList());
    }

    /**
     * Sært, men USA og Papua Ny-Guinea særbehandles.
     *
     * @param personopplysningerAggregat
     */

    public boolean harStatsborgerskapUsaEllerPng(PersonopplysningerAggregat personopplysningerAggregat) {
        if (personopplysningerAggregat == null) {
            return false;
        }
        List<StatsborgerskapEntitet> statsborgerskapListe = personopplysningerAggregat.getStatsborgerskapFor(personopplysningerAggregat.getSøker().getAktørId());
        for (StatsborgerskapEntitet statsborgerskap : statsborgerskapListe) {
            if (Landkoder.USA.equals(statsborgerskap.getStatsborgerskap()) || Landkoder.PNG.equals(statsborgerskap.getStatsborgerskap())) {
                return true;
            }
        }
        return false;
    }

    public boolean erStatusUtvandret(PersonopplysningerAggregat bruker) {
        return bruker != null && bruker.getSøker() != null
            && bruker.getPersonstatusFor(bruker.getSøker().getAktørId()) != null
            && bruker.getPersonstatusFor(bruker.getSøker().getAktørId()).getPersonstatus() != null
            && bruker.getPersonstatusFor(bruker.getSøker().getAktørId()).getPersonstatus().equals(PersonstatusType.UTVA);
    }

    /**
     * Bestemme om søknadens termindato, fødselsdato eller dato for omsorgsovertakelse er
     * i periode som er under avklaring eller ikke har start eller sluttdato
     */
    public boolean harPeriodeUnderAvklaring(Set<MedlemskapPerioderEntitet> medlemskapPerioder, LocalDate skjæringsdato) {
        boolean periodeUnderAvklaring = medlemskapPerioder.stream()
            .anyMatch(periode -> erDatoInnenforLukketPeriode(periode.getFom(), periode.getTom(), skjæringsdato)
                && (MedlemskapType.UNDER_AVKLARING.equals(periode.getMedlemskapType()) || MedlemskapKildeType.LAANEKASSEN.equals(periode.getKildeType())));
        boolean åpenPeriode = medlemskapPerioder.stream()
            .anyMatch(periode -> erDatoInnenforÅpenPeriode(periode.getFom(), periode.getTom(), skjæringsdato));
        return (periodeUnderAvklaring || åpenPeriode);
    }

    private boolean erDatoInnenforLukketPeriode(LocalDate periodeFom, LocalDate periodeTom, LocalDate dato) {
        return (dato != null && !periodeFom.equals(Tid.TIDENES_BEGYNNELSE) && !periodeTom.equals(Tid.TIDENES_ENDE)
            && (dato.isAfter(periodeFom) || dato.isEqual(periodeFom))
            && (dato.isBefore(periodeTom) || dato.isEqual(periodeTom)));
    }

    private boolean erDatoInnenforÅpenPeriode(LocalDate periodeFom, LocalDate periodeTom, LocalDate dato) {
        return (dato != null
            && ((periodeTom.equals(Tid.TIDENES_ENDE) && (dato.isAfter(periodeFom) || dato.isEqual(periodeFom)))
                || (periodeFom.equals(Tid.TIDENES_BEGYNNELSE) && (dato.isBefore(periodeTom) || dato.isEqual(periodeTom)))));
    }
}
