package no.nav.foreldrepenger.domene.uttak;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;

@ApplicationScoped
public class SvangerskapspengerUttakTjeneste {

    private SvangerskapspengerUttakResultatRepository repository;

    @Inject
    public SvangerskapspengerUttakTjeneste(SvangerskapspengerUttakResultatRepository repository) {
        this.repository = repository;
    }

    SvangerskapspengerUttakTjeneste() {
        //CDI
    }

    public Optional<SvangerskapspengerUttak> hentHvisEksisterer(long behandlingId) {
        return repository.hentHvisEksisterer(behandlingId).map(SvangerskapspengerUttak::new);
    }
}
