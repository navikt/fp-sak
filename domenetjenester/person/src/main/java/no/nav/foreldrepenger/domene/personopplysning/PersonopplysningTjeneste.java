package no.nav.foreldrepenger.domene.personopplysning;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingsgrunnlagKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.domene.person.tps.TpsAdapter;
import no.nav.foreldrepenger.domene.person.verge.VergeOppdatererAksjonspunkt;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeAksjonpunktDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
@ApplicationScoped
public class PersonopplysningTjeneste extends AbstractPersonopplysningTjenesteImpl {

    private TpsAdapter tpsAdapter;
    private NavBrukerRepository navBrukerRepository;
    private VergeRepository vergeRepository;

    PersonopplysningTjeneste() {
        super();
        // CDI
    }

    @Inject
    public PersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository,
                                        BehandlingsgrunnlagKodeverkRepository behandlingsgrunnlagKodeverkRepository,
                                        TpsAdapter tpsAdapter,
                                        VergeRepository vergeRepository,
                                        NavBrukerRepository navBrukerRepository) {
        super(personopplysningRepository, behandlingsgrunnlagKodeverkRepository);
        this.tpsAdapter = tpsAdapter;
        this.navBrukerRepository = navBrukerRepository;
        this.vergeRepository = vergeRepository;
    }

    public void aksjonspunktVergeOppdaterer(Long behandlingId, VergeAksjonpunktDto adapter) {
        new VergeOppdatererAksjonspunkt(vergeRepository, tpsAdapter, navBrukerRepository).oppdater(behandlingId, adapter);
    }

    public void aksjonspunktAvklarSaksopplysninger(Long behandlingId, AktørId aktørId, PersonopplysningAksjonspunktDto adapter) {
        new AvklarSaksopplysningerAksjonspunkt(getPersonopplysningRepository()).oppdater(behandlingId, aktørId, adapter);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId).map(PersonopplysningGrunnlagEntitet::getId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(PersonInformasjonEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(PersonInformasjonEntitet.class));
    }

    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        PersonopplysningGrunnlagEntitet grunnlag1 = getPersonopplysningRepository().hentPersonopplysningerPåId((Long) idDiff.getGrunnlagId1());
        PersonopplysningGrunnlagEntitet grunnlag2 = getPersonopplysningRepository().hentPersonopplysningerPåId((Long) idDiff.getGrunnlagId2());
        return getPersonopplysningRepository().diffResultat(grunnlag1, grunnlag2, kunSporedeEndringer);
    }

}
