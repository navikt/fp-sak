package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

class YtelsesFordelingRepositoryStub extends YtelsesFordelingRepository {

    private final Map<Long, YtelseFordelingAggregat> ytelseFordelingAggregatMap = new ConcurrentHashMap<>();

    @Override
    public YtelseFordelingAggregat hentAggregat(Long behandlingId) {
        return hentAggregatHvisEksisterer(behandlingId).orElseThrow();
    }

    @Override
    public Optional<YtelseFordelingAggregat> hentAggregatHvisEksisterer(Long behandlingId) {
        return Optional.ofNullable(ytelseFordelingAggregatMap.get(behandlingId));
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Behandling nyBehandling) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<Long> hentIdPåAktivYtelsesFordeling(Long behandlingId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void lagre(Long behandlingId, YtelseFordelingAggregat aggregat) {
        ytelseFordelingAggregatMap.put(behandlingId, aggregat);
    }
}
