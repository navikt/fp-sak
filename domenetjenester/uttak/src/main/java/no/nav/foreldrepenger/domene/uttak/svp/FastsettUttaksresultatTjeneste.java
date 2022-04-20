package no.nav.foreldrepenger.domene.uttak.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.svangerskapspenger.tjeneste.fastsettuttak.FastsettPerioderTjeneste;

@ApplicationScoped
public class FastsettUttaksresultatTjeneste {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private AvklarteDatoerTjeneste avklarteDatoerTjeneste;
    private UttaksresultatMapper uttaksresultatMapper;
    private RegelmodellSøknaderMapper regelmodellSøknaderMapper;
    private final FastsettPerioderTjeneste fastsettPerioderTjeneste = new FastsettPerioderTjeneste();

    public FastsettUttaksresultatTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettUttaksresultatTjeneste(UttakRepositoryProvider behandlingRepositoryProvider,
                                          AvklarteDatoerTjeneste avklarteDatoerTjeneste,
                                          UttaksresultatMapper uttaksresultatMapper,
                                          RegelmodellSøknaderMapper regelmodellSøknaderMapper) {
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.svangerskapspengerUttakResultatRepository = behandlingRepositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.avklarteDatoerTjeneste = avklarteDatoerTjeneste;
        this.uttaksresultatMapper = uttaksresultatMapper;
        this.regelmodellSøknaderMapper = regelmodellSøknaderMapper;
    }

    public SvangerskapspengerUttakResultatEntitet fastsettUttaksresultat(UttakInput input) {
        var nyeSøknader = regelmodellSøknaderMapper.hentSøknader(input);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        var uttaksperioder = fastsettPerioderTjeneste.fastsettePerioder(nyeSøknader, avklarteDatoer);

        var behandlingId = input.getBehandlingReferanse().behandlingId();
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var svangerskapspengerUttakResultatEntitet = uttaksresultatMapper.tilEntiteter(behandlingsresultat, uttaksperioder);
        svangerskapspengerUttakResultatRepository.lagre(behandlingId, svangerskapspengerUttakResultatEntitet);
        return svangerskapspengerUttakResultatEntitet;
    }

}
