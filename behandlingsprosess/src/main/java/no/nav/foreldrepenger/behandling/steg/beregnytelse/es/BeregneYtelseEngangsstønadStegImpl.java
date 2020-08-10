package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

/** Steg for å beregne tilkjent ytelse (for Engangsstønad). */
@BehandlingStegRef(kode = "BERYT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class BeregneYtelseEngangsstønadStegImpl implements BeregneYtelseSteg {

    private int maksStønadsalderAdopsjon;

    private BehandlingRepositoryProvider repositoryProvider;
    private LegacyESBeregningRepository beregningRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository resultatRepository;

    BeregneYtelseEngangsstønadStegImpl() {
        // for CDI proxy
    }

    /**
     * @param maksStønadsalder - Maks stønadsalder ved adopsjon
     */
    @Inject
    BeregneYtelseEngangsstønadStegImpl(BehandlingRepositoryProvider repositoryProvider, LegacyESBeregningRepository beregningRepository,
                                       @KonfigVerdi(value = "es.maks.stønadsalder.adopsjon", defaultVerdi = "15") int maksStønadsalder,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.beregningRepository = beregningRepository;
        this.maksStønadsalderAdopsjon = maksStønadsalder;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.resultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        LegacyESBeregning sisteBeregning = finnSisteBeregning(behandlingId);
        if (sisteBeregning == null || !sisteBeregning.isOverstyrt()) {
            long antallBarn = new BarnFinner(repositoryProvider).finnAntallBarn(behandlingId, maksStønadsalderAdopsjon);
            LocalDate satsDato = getSatsDato(behandlingId);
            BeregningSats sats = beregningRepository.finnEksaktSats(BeregningSatsType.ENGANG, satsDato);
            long beregnetYtelse = sats.getVerdi() * antallBarn;
            LegacyESBeregning beregning = new LegacyESBeregning(sats.getVerdi(), antallBarn, beregnetYtelse, LocalDateTime.now());

            Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
            var behResultat = resultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
            LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
                .medBeregning(beregning)
                .buildFor(behandling, behResultat);
            beregningRepository.lagre(beregningResultat, kontekst.getSkriveLås());
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDate getSatsDato(Long behandlingId) {
        LocalDate idag = LocalDate.now();
        LocalDate satsDato = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        // La stå: For å håndtere at man bruker dagens sats fram til ny trer i kraft
        return satsDato.isBefore(idag) ? satsDato : idag;
    }

    private LegacyESBeregning finnSisteBeregning(Long behandlingId) {
        return beregningRepository.getSisteBeregning(behandlingId).orElse(null);
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType fraSteg, BehandlingStegType tilSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var behResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId()).orElse(null);
        RyddBeregninger ryddBeregninger = new RyddBeregninger(repositoryProvider.getBehandlingRepository(), kontekst);
        ryddBeregninger.ryddBeregninger(behandling, behResultat);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var behResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId()).orElse(null);
        RyddBeregninger ryddBeregninger = new RyddBeregninger(repositoryProvider.getBehandlingRepository(), kontekst);
        ryddBeregninger.ryddBeregningerHvisIkkeOverstyrt(behandling, behResultat);
    }

}
