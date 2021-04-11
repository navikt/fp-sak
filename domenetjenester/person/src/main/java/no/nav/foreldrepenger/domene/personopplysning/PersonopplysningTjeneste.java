package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class PersonopplysningTjeneste implements StandardPersonopplysningTjeneste {

    private static final LocalDate OPPHOLD_AKSEPTER_FOM_ETTER = LocalDate.of(2020, 1,1);
    private static final LocalDate OPPHOLD_BRUK_UDEFINERT_FOM = LocalDate.of(2020, 10,1);

    private PersonopplysningRepository personopplysningRepository;

    PersonopplysningTjeneste() {
        // CDI
    }

    @Inject
    public PersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref) {
        final LocalDate localDate = ref.getUtledetSkjæringstidspunkt();
        return hentGjeldendePersoninformasjonPåTidspunkt(ref.getBehandlingId(), ref.getAktørId(), localDate);
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(ref.getBehandlingId());
        if (grunnlagOpt.isPresent()) {
            final LocalDate localDate = ref.getUtledetSkjæringstidspunkt();
            return Optional.of(mapTilAggregat(ref.getAktørId(), localDate, grunnlagOpt.get()));
        }
        return Optional.empty();
    }

    @Override
    public PersonopplysningerAggregat hentGjeldendePersoninformasjonPåTidspunkt(Long behandlingId, AktørId aktørId, LocalDate tidspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)));
        }
        throw new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda.");
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(Long behandlingId, AktørId aktørId, LocalDate tidspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1))));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(Long behandlingId, AktørId aktørId, DatoIntervallEntitet forPeriode) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, forPeriode));
        }
        return Optional.empty();
    }

    private PersonopplysningerAggregat mapTilAggregat(AktørId aktørId, LocalDate tidspunkt, PersonopplysningGrunnlagEntitet grunnlag) {
        tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
        return new PersonopplysningerAggregat(grunnlag, aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)));
    }

    private PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId).map(PersonopplysningGrunnlagEntitet::getId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(PersonInformasjonEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(PersonInformasjonEntitet.class));
    }

    public PersonopplysningGrunnlagEntitet hentGrunnlagPåId(Long grunnlagId) {
        return personopplysningRepository.hentGrunnlagPåId(grunnlagId);
    }

    public List<OppholdstillatelseEntitet> hentOppholdstillatelser(Long behandlingId) {
         return getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId).flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getOppholdstillatelser).orElse(List.of());
    }

    public boolean harOppholdstillatelsePåDato(Long behandlingId, LocalDate dato) {
        return getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId).flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getOppholdstillatelser).orElse(List.of()).stream()
            .filter(ot -> OppholdstillatelseType.PERMANENT.equals(ot.getTillatelse()) || OppholdstillatelseType.MIDLERTIDIG.equals(ot.getTillatelse()))
            .map(OppholdstillatelseEntitet::getPeriode)
            .filter(p -> p.getTomDato().isAfter(dato) && (p.getFomDato().isAfter(OPPHOLD_AKSEPTER_FOM_ETTER) || OPPHOLD_BRUK_UDEFINERT_FOM.isBefore(p.getTomDato())))
            .map(p -> new LocalDateInterval(p.getFomDato().isAfter(OPPHOLD_AKSEPTER_FOM_ETTER) ? p.getFomDato() : OPPHOLD_BRUK_UDEFINERT_FOM, p.getTomDato()))
            .anyMatch(i -> i.encloses(dato));
    }

    public boolean harOppholdstillatelseForPeriode(Long behandlingId, LocalDateInterval datointervall) {
        return hentOppholdstillatelseTidslinje(behandlingId).getLocalDateIntervals().stream().anyMatch(i -> i.contains(datointervall)) ;
    }

    public LocalDateTimeline<Boolean> hentOppholdstillatelseTidslinje(Long behandlingId) {
        var segmenter = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId).flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getOppholdstillatelser).orElse(List.of()).stream()
            .filter(ot -> OppholdstillatelseType.PERMANENT.equals(ot.getTillatelse()) || OppholdstillatelseType.MIDLERTIDIG.equals(ot.getTillatelse()))
            .map(OppholdstillatelseEntitet::getPeriode)
            .filter(p -> p.getFomDato().isAfter(OPPHOLD_AKSEPTER_FOM_ETTER) || OPPHOLD_BRUK_UDEFINERT_FOM.isBefore(p.getTomDato()))
            .map(p -> new LocalDateInterval(p.getFomDato().isAfter(OPPHOLD_AKSEPTER_FOM_ETTER) ? p.getFomDato() : OPPHOLD_BRUK_UDEFINERT_FOM, p.getTomDato()))
            .map(ot -> new LocalDateSegment<>(ot, Boolean.TRUE))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress();
    }

    public Optional<AktørId> hentOppgittAnnenPartAktørId(Long behandlingId) {
        return getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart).map(OppgittAnnenPartEntitet::getAktørId);
    }

    public Optional<OppgittAnnenPartEntitet> hentOppgittAnnenPart(Long behandlingId) {
        return getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
    }
}
