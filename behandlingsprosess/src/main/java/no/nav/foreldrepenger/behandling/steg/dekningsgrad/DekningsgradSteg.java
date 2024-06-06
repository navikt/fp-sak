package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_DEKNINGSGRAD;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@BehandlingStegRef(BehandlingStegType.DEKNINGSGRAD)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class DekningsgradSteg implements BehandlingSteg {

    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private final FamilieHendelseTjeneste familieHendelseTjeneste;
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final HistorikkRepository historikkRepository;

    @Inject
    public DekningsgradSteg(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                            FamilieHendelseTjeneste familieHendelseTjeneste,
                            YtelseFordelingTjeneste ytelseFordelingTjeneste,
                            BehandlingRepository behandlingRepository,
                            HistorikkRepository historikkRepository) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        var fagsakRelasjonDekningsgrad = hentFagsakRelasjon(kontekst).getGjeldendeDekningsgrad();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var eksisterendeSakskompleksDekningsgrad = ytelseFordelingAggregat.getSakskompleksDekningsgrad();
        var annenPartsOppgittDekningsgrad = finnAnnenPartsOppgittDekningsgrad(kontekst.getFagsakId()).orElse(null);
        var fh = familieHendelseTjeneste.hentAggregat(behandlingId).getGjeldendeVersjon();
        return SakskompleksDekningsgradUtleder.utledFor(fagsakRelasjonDekningsgrad, eksisterendeSakskompleksDekningsgrad,
            ytelseFordelingAggregat.getOppgittDekningsgrad(), annenPartsOppgittDekningsgrad, fh).map(utledingResultat -> {
                if (!Objects.equals(ytelseFordelingAggregat.getGjeldendeDekningsgrad(), utledingResultat.dekningsgrad())) {
                    lagHistorikkinnslag(behandlingId, ytelseFordelingAggregat.getGjeldendeDekningsgrad(), utledingResultat);
                }
            ytelseFordelingTjeneste.lagreSakskompleksDekningsgrad(behandlingId, utledingResultat.dekningsgrad());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }).orElseGet(() -> BehandleStegResultat.utførtMedAksjonspunkt(AVKLAR_DEKNINGSGRAD));
    }

    private void lagHistorikkinnslag(Long behandlingId, Dekningsgrad gjeldendeDekningsgrad, DekningsgradUtledingResultat nyUtleding) {
        var nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.ENDRET_DEKNINGSGRAD);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandlingId);
        var historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.ENDRET_DEKNINGSGRAD)
            .medBegrunnelse(String.format("Dekningsgraden er endret fra %s%% til %s%% grunnet %s", gjeldendeDekningsgrad, nyUtleding.dekningsgrad(),
                switch (nyUtleding.kilde()) {
                    case FAGSAK_RELASJON -> "annen parts sak";
                    case DØDSFALL -> "opplysninger om død";
                    case OPPGITT, ALLEREDE_FASTSATT -> throw new IllegalStateException("Unexpected value: " + nyUtleding.kilde());
                }));
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    private Optional<Dekningsgrad> finnAnnenPartsOppgittDekningsgrad(long fagsakId) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsakId)
            .flatMap(fr -> fr.getRelatertFagsakFraId(fagsakId))
            .flatMap(annenPartsFagsak -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(annenPartsFagsak.getId())
                .flatMap(b -> ytelseFordelingTjeneste.hentAggregatHvisEksisterer(b.getId()).map(YtelseFordelingAggregat::getOppgittDekningsgrad)));
    }

    private FagsakRelasjon hentFagsakRelasjon(BehandlingskontrollKontekst kontekst) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(kontekst.getFagsakId()).orElseThrow();
    }
}
