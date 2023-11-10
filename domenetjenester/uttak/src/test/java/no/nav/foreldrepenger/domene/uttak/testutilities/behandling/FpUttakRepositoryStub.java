package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;

class FpUttakRepositoryStub extends FpUttakRepository {

    private final Map<Long, UttakResultatPerioderEntitet> opprinnelig = new ConcurrentHashMap<>();
    private final Map<Long, UttakResultatPerioderEntitet> overstyrt = new ConcurrentHashMap<>();

    private final BehandlingsresultatRepositoryStub behandlingsresultatRepository;

    FpUttakRepositoryStub(BehandlingsresultatRepositoryStub behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    public void lagreOpprinneligUttakResultatPerioder(Long behandlingId,
                                                      UttakResultatPerioderEntitet opprinneligPerioder) {
        opprinnelig.put(behandlingId, opprinneligPerioder);
    }

    @Override
    public void lagreOverstyrtUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet overstyrtPerioder) {
        overstyrt.put(behandlingId, overstyrtPerioder);
    }

    @Override
    public Optional<UttakResultatEntitet> hentUttakResultatHvisEksisterer(Long behandlingId) {
        var opprinneligUttak = opprinnelig.get(behandlingId);
        if (opprinneligUttak == null) {
            return Optional.empty();
        }
        return Optional.of(new UttakResultatEntitet.Builder(behandlingsresultatRepository.hent(behandlingId))
            .medOpprinneligPerioder(opprinneligUttak)
            .medOverstyrtPerioder(overstyrt.get(behandlingId))
            .build());
    }

    @Override
    public UttakResultatEntitet hentUttakResultat(Long behandlingId) {
        return hentUttakResultatHvisEksisterer(behandlingId).orElseThrow();
    }

    @Override
    public Optional<UttakResultatEntitet> hentUttakResultatPåId(Long behandlingId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void deaktivterAktivtResultat(Long behandlingId) {
        overstyrt.remove(behandlingId);
        opprinnelig.remove(behandlingId);
    }
}

