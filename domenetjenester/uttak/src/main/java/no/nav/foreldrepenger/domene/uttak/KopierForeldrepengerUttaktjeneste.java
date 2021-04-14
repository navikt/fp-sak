package no.nav.foreldrepenger.domene.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderRevurderingUtil;

@ApplicationScoped
public class KopierForeldrepengerUttaktjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(KopierForeldrepengerUttaktjeneste.class);

    private FpUttakRepository fpUttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public KopierForeldrepengerUttaktjeneste(FpUttakRepository fpUttakRepository,
                                             YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.fpUttakRepository = fpUttakRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    KopierForeldrepengerUttaktjeneste() {
        //CDI
    }

    public void kopierUttakFraOriginalBehandling(BehandlingReferanse referanse) {
        kopierUttaksgrunnlagSøknadsfristResultatFraOriginalBehandling(referanse);
        kopierUttaksresultatFraOriginalBehandling(referanse);
    }

    public void kopierUttaksgrunnlagSøknadsfristResultatFraOriginalBehandling(BehandlingReferanse ref) {
        LOG.info("Kopierer yfgrunnlag fra behandling {}, til behandling {}", ref.getOriginalBehandlingId(), ref.getBehandlingId());
        ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(ref.getOriginalBehandlingId().orElseThrow(), ref.getBehandlingId());
    }

    public void kopierUttaksresultatFraOriginalBehandling(BehandlingReferanse referanse) {
        fpUttakRepository.hentUttakResultatHvisEksisterer(referanse.getOriginalBehandlingId().orElseThrow())
            .ifPresent(uttak -> kopierUttaksresultat(referanse.getBehandlingId(), uttak));
    }

    private void kopierUttaksresultat(Long behandlingId, UttakResultatEntitet uttak) {
        LOG.info("Kopierer uttaksresultat id {}, til behandling {}", uttak.getId(), behandlingId);
        var kopiertOpprinneligPerioder = FastsettePerioderRevurderingUtil.kopier(uttak.getOpprinneligPerioder());
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, kopiertOpprinneligPerioder);
        if (uttak.getOverstyrtPerioder() != null) {
            var kopiertOverstyrtPerioder = FastsettePerioderRevurderingUtil.kopier(uttak.getOverstyrtPerioder());
            fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, kopiertOverstyrtPerioder);
        }
    }
}
