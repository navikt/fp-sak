
package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class AvklarOmErBosatt {
    //Setter den til 364 for å unngå skuddårproblemer, (365 og 366 blir da "større" enn et år)
    private static final int ANTALL_DAGER_I_ÅRET = 364;

    private static final Set<PersonstatusType> STATUS_UTEN_AVKLARINGSBEHOV = Set.of(PersonstatusType.BOSA, PersonstatusType.DØD);

    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    public AvklarOmErBosatt(BehandlingRepositoryProvider repositoryProvider,
                     MedlemskapPerioderTjeneste medlemskapPerioderTjeneste,
                     PersonopplysningTjeneste personopplysningTjeneste) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.medlemskapPerioderTjeneste = medlemskapPerioderTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public Optional<MedlemResultat> utled(BehandlingReferanse ref, LocalDate vurderingsdato) {
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        if (harPersonstatusSomSkalAvklares(ref, personopplysninger, vurderingsdato)) {
            return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
        }
        if (søkerHarSøktPåTerminOgSkalOppholdeSegIUtlandetImerEnn12M(ref, vurderingsdato)) {
            return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
        }
        if (harBrukerTilknytningHjemland(ref) == NEI) {
            return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
        }
        if (harBrukerUtenlandskPostadresse(ref, personopplysninger, vurderingsdato) == NEI) {
            return Optional.empty();
        }
        if (erFrivilligMedlemEllerIkkeMedlem(ref, vurderingsdato) == NEI) {
            return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
        }
        return Optional.empty();
    }

    private boolean harPersonstatusSomSkalAvklares(BehandlingReferanse ref, PersonopplysningerAggregat personopplysninger, LocalDate vurderingsdato) {
        var personstatus = Optional.ofNullable(personopplysninger.getPersonstatusFor(ref.aktørId(), SimpleLocalDateInterval.enDag(vurderingsdato)))
            .map(PersonstatusEntitet::getPersonstatus).orElse(PersonstatusType.UDEFINERT);
        return !STATUS_UTEN_AVKLARINGSBEHOV.contains(personstatus);
    }

    private boolean søkerHarSøktPåTerminOgSkalOppholdeSegIUtlandetImerEnn12M(BehandlingReferanse ref, LocalDate vurderingsdato) {
        var grunnlag = familieHendelseRepository.hentAggregat(ref.behandlingId());
        if (grunnlag.getGjeldendeVersjon().getTerminbekreftelse().isPresent()) {
            var medlemskapAggregat = medlemskapRepository.hentMedlemskap(ref.behandlingId());
            var oppgittTilknytning = medlemskapAggregat.flatMap(MedlemskapAggregat::getOppgittTilknytning).orElseThrow(IllegalStateException::new);

            var fremtidigeOpphold = oppgittTilknytning.getOpphold()
                .stream()
                .filter(opphold -> !opphold.isTidligereOpphold() && !opphold.getLand().equals(Landkoder.NOR))
                .map(o -> finnSegment(vurderingsdato, o.getPeriodeFom(), o.getPeriodeTom()))
                .toList();

            var fremtidigePerioder = new LocalDateTimeline<>(fremtidigeOpphold,
                StandardCombinators::alwaysTrueForMatch).compress();

           return fremtidigePerioder.getLocalDateIntervals()
               .stream()
               .anyMatch(this::periodeLengreEnn12M);
        }
        return false;
    }

    private boolean periodeLengreEnn12M(LocalDateInterval localDateInterval) {
        return localDateInterval.days() >= ANTALL_DAGER_I_ÅRET;
    }

    private LocalDateSegment<Boolean> finnSegment(LocalDate skjæringsdato, LocalDate fom, LocalDate tom) {
        if (skjæringsdato.isAfter(fom) && skjæringsdato.isBefore(tom)) {
            return new LocalDateSegment<>(skjæringsdato, tom, true);
        }
        return new LocalDateSegment<>(fom, tom, true);
    }

    private Utfall harBrukerUtenlandskPostadresse(BehandlingReferanse ref, PersonopplysningerAggregat personopplysninger, LocalDate vurderingsdato) {
        if (personopplysninger.getAdresserFor(ref.aktørId(), SimpleLocalDateInterval.enDag(vurderingsdato)).stream()
            .anyMatch(adresse -> AdresseType.POSTADRESSE_UTLAND.equals(adresse.getAdresseType()) || !Landkoder.erNorge(adresse.getLand()))) {
            return JA;
        }
        return NEI;
    }

    //TODO(OJR) må denne endres?
    private Utfall harBrukerTilknytningHjemland(BehandlingReferanse ref) {
        var medlemskapAggregat = medlemskapRepository.hentMedlemskap(ref.behandlingId());
        var oppgittTilknytning = medlemskapAggregat.flatMap(MedlemskapAggregat::getOppgittTilknytning).orElseThrow(IllegalStateException::new);

        var antallNei = 0;
        if (!oppgittTilknytning.isOppholdINorgeSistePeriode()) {
            antallNei++;
        }
        if (!oppgittTilknytning.isOppholdNå()) {
            antallNei++;
        }
        if (!oppgittTilknytning.isOppholdINorgeNestePeriode()) {
            antallNei++;
        }

        if (antallNei >= 2) {
            return NEI;
        }
        return JA;
    }

    private Utfall erFrivilligMedlemEllerIkkeMedlem(BehandlingReferanse ref, LocalDate vurderingsdato) {


        var medlemskap = medlemskapRepository.hentMedlemskap(ref.behandlingId());

        Collection<MedlemskapPerioderEntitet> medlemskapsPerioder = medlemskap.isPresent()
            ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptyList();
        var medlemskapDekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapsPerioder, vurderingsdato);

        var erRegistrertSomIkkeMedlem = medlemskapPerioderTjeneste.erRegistrertSomIkkeMedlem(medlemskapDekningTyper);
        var erRegistrertSomFrivilligMedlem = medlemskapPerioderTjeneste.erRegistrertSomFrivilligMedlem(medlemskapDekningTyper);
        return erRegistrertSomIkkeMedlem || erRegistrertSomFrivilligMedlem ? JA : NEI;
    }
}
