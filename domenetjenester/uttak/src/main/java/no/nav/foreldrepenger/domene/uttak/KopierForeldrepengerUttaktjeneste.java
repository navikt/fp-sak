package no.nav.foreldrepenger.domene.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void kopierUttakFraOriginalBehandling(Long originalBehandlingId, Long behandlingId) {
        kopierUttaksgrunnlagFraOriginalBehandling(originalBehandlingId, behandlingId);
        kopierUttaksresultatFraOriginalBehandling(originalBehandlingId, behandlingId);
    }

    public void lagreTomtUttakResultat(Long behandlingId) {
        /* TODO: evaluer testresultat og vurdere dette alternativet
            var tomtUttak = new UttakResultatPerioderEntitet();
            fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, tomtUttak);
         */
        fpUttakRepository.deaktivterAktivtResultat(behandlingId);
    }

    public void kopierUttaksgrunnlagFraOriginalBehandling(Long originalBehandlingId, Long behandlingId) {
        LOG.info("Kopierer yfgrunnlag fra behandling {}, til behandling {}", originalBehandlingId, behandlingId);
        ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandlingForOverhoppUttak(originalBehandlingId, behandlingId);
    }

    public void kopierUttaksresultatFraOriginalBehandling(Long originalBehandlingId, Long behandlingId) {
        fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandlingId)
            .ifPresent(uttak -> kopierUttaksresultat(behandlingId, uttak));
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
