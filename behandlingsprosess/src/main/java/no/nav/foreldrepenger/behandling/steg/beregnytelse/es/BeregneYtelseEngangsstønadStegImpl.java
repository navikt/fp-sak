package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/** Steg for å beregne tilkjent ytelse (for Engangsstønad). */
@BehandlingStegRef(BehandlingStegType.BEREGN_YTELSE)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class BeregneYtelseEngangsstønadStegImpl implements BeregneYtelseSteg {

    private int maksStønadsalderAdopsjon;

    private LegacyESBeregningRepository beregningRepository;
    private SatsRepository satsRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository resultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    BeregneYtelseEngangsstønadStegImpl() {
        // for CDI proxy
    }

    /**
     * @param maksStønadsalder - Maks stønadsalder ved adopsjon
     */
    @Inject
    BeregneYtelseEngangsstønadStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                       LegacyESBeregningRepository beregningRepository,
                                       @KonfigVerdi(value = "es.maks.stønadsalder.adopsjon", defaultVerdi = "15") int maksStønadsalder,
                                       SatsRepository satsRepository,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.beregningRepository = beregningRepository;
        this.maksStønadsalderAdopsjon = maksStønadsalder;
        this.satsRepository = satsRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.resultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        var sisteBeregning = finnSisteBeregning(behandlingId);
        if (sisteBeregning == null || !sisteBeregning.isOverstyrt()) {
            var barnFinner = new BarnFinner(familieHendelseRepository);
            long antallBarn = barnFinner.finnAntallBarn(behandlingId, maksStønadsalderAdopsjon);
            var satsDato = getSatsDato(behandlingId);
            var sats = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, satsDato);
            var beregnetYtelse = sats.getVerdi() * antallBarn;
            var beregning = new LegacyESBeregning(behandlingId, sats.getVerdi(), antallBarn, beregnetYtelse, LocalDateTime.now());

            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var behResultat = resultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
            var beregningResultat = LegacyESBeregningsresultat.builder()
                    .medBeregning(beregning)
                    .buildFor(behandling, behResultat);
            beregningRepository.lagre(beregningResultat, kontekst.getSkriveLås());
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDate getSatsDato(Long behandlingId) {
        var idag = LocalDate.now();
        var satsDato = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        // La stå: For å håndtere at man bruker dagens sats fram til ny trer i kraft
        return satsDato.isBefore(idag) ? satsDato : idag;
    }

    private LegacyESBeregning finnSisteBeregning(Long behandlingId) {
        return beregningRepository.getSisteBeregning(behandlingId).orElse(null);
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType fraSteg,
            BehandlingStegType tilSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var behResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        if (behResultat.isPresent()) {
            var ryddBeregninger = new RyddBeregninger(behandlingRepository, kontekst);
            ryddBeregninger.ryddBeregninger(behandling, behResultat.get());
        }
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var behResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        if (behResultat.isPresent()) {
            var ryddBeregninger = new RyddBeregninger(behandlingRepository, kontekst);
            ryddBeregninger.ryddBeregningerHvisIkkeOverstyrt(behandling, behResultat.get());
        }
    }

}
