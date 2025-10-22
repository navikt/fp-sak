package no.nav.foreldrepenger.domene.personopplysning;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class PersonopplysningTjeneste  {

    private PersonopplysningRepository personopplysningRepository;
    private BehandlingEventPubliserer behandlingEventPubliserer;

    PersonopplysningTjeneste() {
        // CDI
    }

    @Inject
    public PersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository, BehandlingEventPubliserer behandlingEventPubliserer) {
        this.personopplysningRepository = personopplysningRepository;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
    }

    public PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref) {
        return hentPersonopplysningerHvisEksisterer(ref.behandlingId(), ref.aktørId())
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda."));
    }

    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref) {
        return hentPersonopplysningerHvisEksisterer(ref.behandlingId(), ref.aktørId());
    }

    private Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(Long behandlingId, AktørId aktørId) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandlingId)
            .map(grunnlag -> new PersonopplysningerAggregat(grunnlag, aktørId));
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        var funnetId = personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandlingId).map(PersonopplysningGrunnlagEntitet::getId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(PersonInformasjonEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(PersonInformasjonEntitet.class));
    }

    public PersonopplysningGrunnlagEntitet hentGrunnlagPåId(Long grunnlagId) {
        return personopplysningRepository.hentGrunnlagPåId(grunnlagId);
    }

    public Optional<AktørId> hentOppgittAnnenPartAktørId(BehandlingReferanse ref) {
        return hentOppgittAnnenPart(ref).map(OppgittAnnenPartEntitet::getAktørId);
    }

    public Optional<OppgittAnnenPartEntitet> hentOppgittAnnenPart(BehandlingReferanse ref) {
        return personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(ref.behandlingId());
    }

    public void kopierAnnenPartFraOriginalBehandling(BehandlingReferanse ref) {
        ref.getOriginalBehandlingId()
            .flatMap(obid -> personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(obid))
            .map(OppgittAnnenPartBuilder::new)
            .ifPresent(oap -> personopplysningRepository.lagre(ref.behandlingId(), oap.build()));
    }

    public void lagreOppgittAnnenPart(BehandlingReferanse ref, OppgittAnnenPartEntitet annenPart) {
        var personerFør = personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(ref.saksnummer());
        personopplysningRepository.lagre(ref.behandlingId(), annenPart);
        var personerEtter = personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(ref.saksnummer());
        if (personerFør.size() != personerEtter.size() || !personerFør.containsAll(personerEtter)) {
            behandlingEventPubliserer.publiserBehandlingEvent(new SakensPersonerEndretEvent(ref.fagsakId(), ref.saksnummer(), ref.behandlingId(), "Søknad"));
        }
    }

    public PersonInformasjonBuilder opprettBuilderForRegisterdata(BehandlingReferanse ref) {
        return personopplysningRepository.opprettBuilderForRegisterdata(ref.behandlingId());
    }

    public void lagreRegisterdata(BehandlingReferanse ref, PersonInformasjonBuilder builder) {
        var personerFør = personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(ref.saksnummer());
        personopplysningRepository.lagre(ref.behandlingId(), builder);
        var personerEtter = personopplysningRepository.hentAktørIdKnyttetTilSaksnummer(ref.saksnummer());
        if (personerFør.size() != personerEtter.size() || !personerFør.containsAll(personerEtter)) {
            behandlingEventPubliserer.publiserBehandlingEvent(new SakensPersonerEndretEvent(ref.fagsakId(), ref.saksnummer(), ref.behandlingId(), "Registerdata"));
        }
    }
}
