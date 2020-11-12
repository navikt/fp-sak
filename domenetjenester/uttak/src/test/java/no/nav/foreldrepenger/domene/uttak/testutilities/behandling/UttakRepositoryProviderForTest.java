package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;

public class UttakRepositoryProviderForTest extends UttakRepositoryProvider {

    private final FagsakRepository fagsakRepository;
    private final FagsakRelasjonRepository fagsakRelasjonRepository;
    private final FpUttakRepository fpUttakRepository;
    private final UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;

    public UttakRepositoryProviderForTest() {
        this.ytelsesFordelingRepository = new YtelsesFordelingRepositoryForTest();
        this.fagsakRepository = new FagsakRepositoryForTest();
        this.fagsakRelasjonRepository = new FagsakRelasjonRepositoryForTest();
        this.behandlingsresultatRepository = new BehandlingsresultatRepositoryForTest();
        this.fpUttakRepository = new FpUttakRepositoryForTest(
            (BehandlingsresultatRepositoryForTest) behandlingsresultatRepository);
        this.uttaksperiodegrenseRepository = new UttaksperiodegrenseRepositoryForTest();
        this.svangerskapspengerUttakResultatRepository = new SvangerskapspengerUttakResultatRepositoryForTest();
    }

    @Override
    public FagsakRepository getFagsakRepository() {
        return fagsakRepository;
    }

    @Override
    public FagsakRelasjonRepository getFagsakRelasjonRepository() {
        return fagsakRelasjonRepository;
    }

    @Override
    public FpUttakRepository getFpUttakRepository() {
        return fpUttakRepository;
    }

    @Override
    public YtelsesFordelingRepository getYtelsesFordelingRepository() {
        return ytelsesFordelingRepository;
    }

    @Override
    public BehandlingsresultatRepository getBehandlingsresultatRepository() {
        return behandlingsresultatRepository;
    }

    @Override
    public SvangerskapspengerUttakResultatRepository getSvangerskapspengerUttakResultatRepository() {
        return svangerskapspengerUttakResultatRepository;
    }

    @Override
    public UttaksperiodegrenseRepository getUttaksperiodegrenseRepository() {
        return uttaksperiodegrenseRepository;
    }
}
