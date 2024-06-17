package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;

public class UttakRepositoryStubProvider extends UttakRepositoryProvider {

    private final FagsakRepository fagsakRepository;
    private final FagsakRelasjonRepository fagsakRelasjonRepository;
    private final FpUttakRepository fpUttakRepository;
    private final UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;

    public UttakRepositoryStubProvider() {
        this.ytelsesFordelingRepository = new YtelsesFordelingRepositoryStub();
        this.fagsakRepository = new FagsakRepositoryStub();
        this.fagsakRelasjonRepository = new FagsakRelasjonRepositoryStub();
        this.behandlingsresultatRepository = new BehandlingsresultatRepositoryStub();
        this.fpUttakRepository = new FpUttakRepositoryStub((BehandlingsresultatRepositoryStub) behandlingsresultatRepository);
        this.uttaksperiodegrenseRepository = new UttaksperiodegrenseRepositoryStub();
        this.svangerskapspengerUttakResultatRepository = new SvangerskapspengerUttakResultatRepositoryStub();
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
