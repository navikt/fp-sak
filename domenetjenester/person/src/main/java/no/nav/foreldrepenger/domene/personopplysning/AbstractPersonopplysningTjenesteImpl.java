package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public abstract class AbstractPersonopplysningTjenesteImpl implements StandardPersonopplysningTjeneste {

    protected PersonopplysningRepository personopplysningRepository;

    AbstractPersonopplysningTjenesteImpl() {
        // CDI
    }

    public AbstractPersonopplysningTjenesteImpl(PersonopplysningRepository personopplysningRepository) {
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

    protected PersonopplysningerAggregat mapTilAggregat(AktørId aktørId, LocalDate tidspunkt, PersonopplysningGrunnlagEntitet grunnlag) {
        tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
        return new PersonopplysningerAggregat(grunnlag, aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)));
    }

    protected PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }


}
