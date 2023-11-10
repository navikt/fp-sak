package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        var stp = ref.getUtledetSkjæringstidspunkt();
        return hentGjeldendePersoninformasjonPåTidspunkt(ref, stp);
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref) {
        var stp = ref.getUtledetSkjæringstidspunktHvisUtledet().orElseGet(LocalDate::now);
        return hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(ref, stp);
    }

    @Override
    public PersonopplysningerAggregat hentGjeldendePersoninformasjonPåTidspunkt(BehandlingReferanse ref, LocalDate tidspunkt) {
        var grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(ref.behandlingId());
        if (grunnlagOpt.isPresent()) {
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return new PersonopplysningerAggregat(grunnlagOpt.get(), ref.aktørId(), tidspunkt, ref.getUtledetSkjæringstidspunkt());
        }
        throw new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda.");
    }

    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(Long behandlingId, AktørId aktørId, LocalDate tidspunkt) {
        var grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, tidspunkt, tidspunkt));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(BehandlingReferanse ref, LocalDate tidspunkt) {
        var grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(ref.behandlingId());
        if (grunnlagOpt.isPresent()) {
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), ref.aktørId(), tidspunkt, ref.getUtledetSkjæringstidspunkt()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(BehandlingReferanse ref, DatoIntervallEntitet forPeriode) {
        var grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(ref.behandlingId());
        if (grunnlagOpt.isPresent()) {
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), ref.aktørId(), forPeriode, ref.getUtledetSkjæringstidspunkt()));
        }
        return Optional.empty();
    }

    private PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        var funnetId = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId).map(PersonopplysningGrunnlagEntitet::getId);
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
            .toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress();
    }

    public Optional<AktørId> hentOppgittAnnenPartAktørId(Long behandlingId) {
        return hentOppgittAnnenPart(behandlingId).map(OppgittAnnenPartEntitet::getAktørId);
    }

    public Optional<OppgittAnnenPartEntitet> hentOppgittAnnenPart(Long behandlingId) {
        return getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
    }
}
