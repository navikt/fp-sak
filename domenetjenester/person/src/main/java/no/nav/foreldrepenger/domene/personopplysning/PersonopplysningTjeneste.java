package no.nav.foreldrepenger.domene.personopplysning;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
@ApplicationScoped
public class PersonopplysningTjeneste extends AbstractPersonopplysningTjenesteImpl {

    PersonopplysningTjeneste() {
        super();
        // CDI
    }

    @Inject
    public PersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository) {
        super(personopplysningRepository);
    }

    public void lagreAvklartPersonstatus(Long behandlingId, AktørId aktørId, PersonstatusType personstatusType, DatoIntervallEntitet intervall) {
        if (!PersonstatusType.personstatusTyperFortsattBehandling().contains(personstatusType)) {
            throw new IllegalArgumentException("har ikke avklart personsstatus");
        }
        PersonInformasjonBuilder builder = personopplysningRepository.opprettBuilderForOverstyring(behandlingId);
        PersonInformasjonBuilder.PersonstatusBuilder medPersonstatus = builder.getPersonstatusBuilder(aktørId, intervall)
            .medAktørId(aktørId)
            .medPeriode(intervall)
            .medPersonstatus(personstatusType);
        builder.leggTil(medPersonstatus);
        personopplysningRepository.lagre(behandlingId, builder);
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
}
