package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;

class YtelsesFordelingRepositoryForTest extends YtelsesFordelingRepository {

    private final Map<Long, YtelseFordelingAggregat> ytelseFordelingAggregatMap = new ConcurrentHashMap<>();

    @Override
    public YtelseFordelingAggregat hentAggregat(Long behandlingId) {
        return hentAggregatHvisEksisterer(behandlingId).orElseThrow();
    }

    @Override
    public YtelseFordelingAggregat hentYtelsesFordelingPåId(Long aggregatId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<YtelseFordelingAggregat> hentAggregatHvisEksisterer(Long behandlingId) {
        var yf = ytelseFordelingAggregatMap.get(behandlingId);
        if (yf == null) {
            return Optional.empty();
        }
        return Optional.of(yf);
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
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

    @Override
    public DiffResult diffResultat(Long grunnlagId1, Long grunnlagId2, boolean onlyCheckTrackedFields) {
        throw new IkkeImplementertForTestException();
    }
}
