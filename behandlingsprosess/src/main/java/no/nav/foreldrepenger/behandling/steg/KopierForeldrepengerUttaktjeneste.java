package no.nav.foreldrepenger.behandling.steg;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderRevurderingUtil;

@ApplicationScoped
public class KopierForeldrepengerUttaktjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(KopierForeldrepengerUttaktjeneste.class);

    private FpUttakRepository fpUttakRepository;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public KopierForeldrepengerUttaktjeneste(FpUttakRepository fpUttakRepository,
                                             BehandlingRepository behandlingRepository,
                                             YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.fpUttakRepository = fpUttakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    KopierForeldrepengerUttaktjeneste() {
        //CDI
    }

    public void kopierUttakFraOriginalBehandling(Long behandlingId) {
        kopierUttaksgrunnlagSøknadsfristResultatFraOriginalBehandling(behandlingId);
        kopierUttaksresultatFraOriginalBehandling(behandlingId);
    }

    public void kopierUttaksgrunnlagSøknadsfristResultatFraOriginalBehandling(Long behandlingId) {
        var originalBehandling = finnOriginalBehandling(behandlingId);
        LOG.info("Kopierer yfgrunnlag fra behandling {}, til behandling {}", originalBehandling, behandlingId);
        ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandling, behandlingId);
    }

    public void kopierUttaksresultatFraOriginalBehandling(Long behandlingId) {
        var originalBehandling = finnOriginalBehandling(behandlingId);
        fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling)
            .ifPresent(uttak -> kopierUttaksresultat(behandlingId, uttak));
    }

    private Long finnOriginalBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId)
            .getOriginalBehandlingId()
            .orElseThrow(
                () -> new IllegalArgumentException("Finner ikke original behandling for behandling " + behandlingId));
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
