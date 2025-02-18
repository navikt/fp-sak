package no.nav.foreldrepenger.domene.personopplysning;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class PersonopplysningTjeneste implements StandardPersonopplysningTjeneste {

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
        return hentPersonopplysningerHvisEksisterer(ref.behandlingId(), ref.aktørId())
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda."));
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref) {
        return hentPersonopplysningerHvisEksisterer(ref.behandlingId(), ref.aktørId());
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(Long behandlingId, AktørId aktørId) {
        return getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId)
            .map(grunnlag -> new PersonopplysningerAggregat(grunnlag, aktørId));
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

    public Optional<AktørId> hentOppgittAnnenPartAktørId(Long behandlingId) {
        return hentOppgittAnnenPart(behandlingId).map(OppgittAnnenPartEntitet::getAktørId);
    }

    public Optional<OppgittAnnenPartEntitet> hentOppgittAnnenPart(Long behandlingId) {
        return getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
    }
}
